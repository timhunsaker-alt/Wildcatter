package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Sounding;
import dev.wildcatter.kinetic.Spin;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A genset part. It remembers which control panel runs it (sent to the client, which reads the shaft speed from the panel to turn
 * the fan, pulley and coupling and to play the engine). The fuel tank also holds the diesel, in litres.
 */
public class GensetPartBlockEntity extends BlockEntity implements Kinetic, Sounding, Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	private static final Spin STILL = new Spin();

	private @Nullable BlockPos controller;
	private double liters;
	/** An alternator wrecked by an out-of-sync close: it must be replaced. */
	private boolean damaged;
	/** Grown into its true-size cells (see BigParts). */
	private boolean grown;

	public GensetPartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GENSET_PART_ENTITY, pos, state);
	}

	public GensetPartBlock.Part part() {
		return ((GensetPartBlock) getBlockState().getBlock()).part();
	}

	public @Nullable GensetControlPanelBlockEntity controller() {
		return controller != null && level != null && level.getBlockEntity(controller) instanceof GensetControlPanelBlockEntity panel ? panel : null;
	}

	/** The panel has formed a set with this part in it. */
	void bind(BlockPos panel) {
		if (!panel.equals(controller)) {
			controller = panel.immutable();
			changed();
		}
	}

	void unbind() {
		if (controller != null) {
			controller = null;
			changed();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	public boolean damaged() {
		return damaged;
	}

	void damage() {
		damaged = true;
		changed();
	}

	// ---- the fuel tank ---------------------------------------------------------------------------------------------
	public double liters() {
		return liters;
	}

	public boolean addFuel(double amount) {
		if (part() != GensetPartBlock.Part.FUEL_TANK || liters + amount > Tuning.GENSET_TANK_LITERS) {
			return false;
		}
		liters += amount;
		setChanged();
		return true;
	}

	/** Takes up to {@code amount} litres; returns what it gave. */
	double take(double amount) {
		double got = Math.min(liters, amount);
		liters -= got;
		setChanged();
		return got;
	}

	/** Test hook: sets the tank's contents. */
	public void setFuel(double value) {
		liters = Math.max(0, Math.min(Tuning.GENSET_TANK_LITERS, value));
		setChanged();
	}

	// ---- turning parts and sound ------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		// in the main cell's frame for a set facing north (tools/models/rotors.py): the shaft runs west (radiator) to east
		// (panel) along the middle of the skid, 0.55 m above the base, which is z = 1 and y = 0.55 from these main cells
		return switch (part()) {
			case RADIATOR -> List.of(new Rotor(rotor.part(RotorBlock.Part.RADIATOR_FAN), new Vector3f(0.78F, 1.0F, 1.0F), Direction.Axis.X, 1.2F));
			case DIESEL_ENGINE, GAS_ENGINE -> List.of(new Rotor(rotor.part(RotorBlock.Part.CRANK_PULLEY), new Vector3f(0.06F, 0.55F, 1.0F), Direction.Axis.X, 1.0F));
			case ALTERNATOR -> List.of(
					new Rotor(rotor.part(RotorBlock.Part.COUPLING), new Vector3f(0.1F, 0.55F, 1.0F), Direction.Axis.X, 1.0F),
					new Rotor(rotor.part(RotorBlock.Part.ALTERNATOR_FAN), new Vector3f(0.3F, 0.55F, 1.0F), Direction.Axis.X, 1.0F));
			default -> List.of();
		};
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(GensetPartBlock.FACING);
	}

	@Override
	public Spin spin() {
		GensetControlPanelBlockEntity panel = controller();
		return panel != null ? panel.spin() : STILL;
	}

	@Override
	public @Nullable SoundEvent loop() {
		GensetControlPanelBlockEntity panel = controller();
		if (panel == null || panel.spin().rpm() <= 0) {
			return null;
		}
		return switch (part()) {
			case DIESEL_ENGINE -> panel.loadShare() > 0.3 ? WildcatterSounds.DIESEL_LOAD : WildcatterSounds.DIESEL_IDLE;
			case GAS_ENGINE -> WildcatterSounds.GAS_ENGINE;
			case ALTERNATOR -> WildcatterSounds.ALTERNATOR_WHINE;
			case RADIATOR -> WildcatterSounds.FAN;
			default -> null;
		};
	}

	@Override
	public float soundPitch() {
		GensetControlPanelBlockEntity panel = controller();
		return panel == null ? 1 : (float) (panel.spin().rpm() / panel.ratedRpm());
	}

	@Override
	public float soundVolume() {
		GensetControlPanelBlockEntity panel = controller();
		if (panel == null || panel.spin().rpm() <= 0) {
			return 0;
		}
		double speed = panel.spin().rpm() / panel.ratedRpm();
		return (float) switch (part()) {
			case DIESEL_ENGINE, GAS_ENGINE -> 0.6 + 0.6 * panel.loadShare();
			case ALTERNATOR -> 0.35 * speed;
			default -> 0.4 * speed;
		};
	}

	/** Server side: the main cell checks itself now and then, which is how an older world's one-block part is found. */
	public static void serverTick(Level level, BlockPos pos, BlockState state, GensetPartBlockEntity part) {
		dev.wildcatter.multiblock.BigParts.checkNow(level, pos, state, 100);
	}

	/** Client side: keeps the running sound going and puffs exhaust from the silencer's outlet, following the load. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, GensetPartBlockEntity part) {
		if (level.getGameTime() % 10 == 0 && part.loop() != null) {
			Sounding.Hook.start.accept(part);
		}
		GensetControlPanelBlockEntity panel = part.controller();
		if (part.part().isSilencer() && panel != null && panel.spin().rpm() > 0
				&& level.getRandom().nextFloat() < 0.3F + 0.7F * (float) panel.loadShare()) {
			// the tail pipe turns up at the radiator end of the silencer (tools/models/silencer.py)
			double along = 0.1;
			double[] at = turned(along, 0.5, part.facing());
			level.addParticle(panel.loadShare() > 0.7 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
					pos.getX() + at[0], pos.getY() + 1.0, pos.getZ() + at[1], 0, 0.08 + 0.1 * panel.loadShare(), 0);
		}
	}

	/** A point (x east, z south, in metres from the main cell's north-west corner) drawn for a part facing north, turned to {@code facing}. */
	static double[] turned(double x, double z, Direction facing) {
		double dx = x - 0.5, dz = z - 0.5;
		for (int i = 0; i < dev.wildcatter.multiblock.Multiblock.turns(facing); i++) {
			double t = dx;
			dx = -dz;
			dz = t;
		}
		return new double[] {dx + 0.5, dz + 0.5};
	}

	@Override
	public Component inspect() {
		GensetControlPanelBlockEntity panel = controller();
		String name = switch (part()) {
			case DIESEL_ENGINE -> "Diesel engine";
			case GAS_ENGINE -> "Gas engine";
			case ALTERNATOR -> damaged ? "Alternator, DAMAGED by an out-of-sync close: break it and fit a new one" : "Alternator";
			case RADIATOR -> "Radiator";
			case FUEL_TANK -> String.format("Sub-base fuel tank: %.0f of %.0f L of diesel", liters, Tuning.GENSET_TANK_LITERS);
			case BASE_FRAME -> "Base frame";
			case EXHAUST, SILENCER_LARGE -> "Exhaust silencer";
		};
		return Component.literal(name + (panel == null ? " (not part of a genset: build the set round a control panel)" : " of a genset; the control panel reads: "))
				.append(panel == null ? Component.empty() : panel.inspect());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("liters", liters);
		output.putBoolean("damaged", damaged);
		output.putBoolean("grown", grown);
		if (controller != null) {
			output.putLong("controller", controller.asLong());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		liters = input.getDoubleOr("liters", 0);
		damaged = input.getBooleanOr("damaged", false);
		grown = input.getBooleanOr("grown", false);
		controller = input.getLong("controller").map(BlockPos::of).orElse(null);
	}
}
