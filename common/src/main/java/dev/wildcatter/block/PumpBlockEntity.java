package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A 110 V motor that lifts water from the water touching it and sends it down the pipes to every boiler on the run.
 * A level switch runs its motor only while the boilers need water: it starts below 80 % and stops when they are full.
 * Water pressure follows the motor: it needs most of its voltage, and delivers with the square of it, like a real pump.
 */
public class PumpBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, dev.wildcatter.kinetic.Sounding, dev.wildcatter.kinetic.Kinetic {
	private final dev.wildcatter.kinetic.Spin spin = new dev.wildcatter.kinetic.Spin();
	/** Its induction motor (single-phase); it turns only while there is work. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(false, Voltage.LOW, Tuning.PUMP_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	/** Set by the machine's own logic: whether it has work for the motor. */
	private boolean wanted;
	private double volts;
	private double amps;
	private String status = "idle";

	public PumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PUMP_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == motor.voltage() && (motor.threePhase() ? role != Conductor.N : true);
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
		volts = motor.volts();
		amps = motor.amps();
	}

	private boolean touchesWater(Level level) {
		for (Direction side : Direction.values()) {
			if (level.getFluidState(worldPosition.relative(side)).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
		pump.running.update(pump, pump.motor.output() > 0);   // the client plays its loop while it runs
		pump.spin.setRpm(pump.motor.output() > 0 ? (float) Tuning.STARTER_PUMP_RPM : 0);
		if (pump.spin.needsSync()) {
			pump.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.PLUMBING_INTERVAL != 0) {
			return;
		}
		if (!pump.touchesWater(level)) {
			pump.wanted = false;
			pump.status = "DRY: put it against water";
			return;
		}
		List<BlockPos> cell = List.of(pos);
		Set<BlockPos> pipes = Plumbing.network(level, cell);
		List<BoilerBlockEntity> boilers = Plumbing.boilers(level, cell, pipes, true);
		// steam plants take their makeup water at the condenser's hotwell (1 unit of water is 1 kg)
		List<TurbineGeneratorBlockEntity> plants = Plumbing.machines(level, cell, pipes, true, SteamPlantPartBlockEntity.class).stream()
				.filter(p -> p.part() == SteamPlantPartBlock.Part.CONDENSER).map(SteamPlantPartBlockEntity::controller).filter(java.util.Objects::nonNull).distinct().toList();
		// a land rig's mud tanks take their water too (docs/multiblocks/land-rig.md)
		List<RigSubstructureBlockEntity> rigs = Plumbing.machines(level, cell, pipes, true, RigPartBlockEntity.class).stream()
				.filter(p -> p.part() == RigPartBlock.Part.MUD_TANKS).map(RigPartBlockEntity::rig).filter(java.util.Objects::nonNull).distinct().toList();
		// a cracking furnace makes its dilution steam from water (docs/multiblocks/cracker.md); an acid plant's boiler and absorber,
		// a mill's SAG mill and thickener take theirs (acid-plant.md, uranium-mill.md)
		List<WaterUser> furnaces = Plumbing.machines(level, cell, pipes, true, net.minecraft.world.level.block.entity.BlockEntity.class).stream()
				.filter(e -> e instanceof WaterUser).map(e -> (WaterUser) e).toList();
		if (boilers.isEmpty() && plants.isEmpty() && rigs.isEmpty() && furnaces.isEmpty()) {
			pump.wanted = false;
			pump.status = "no boiler, plant condenser, mud tanks or other water user on its pipes";
			return;
		}
		// a level switch: start when a boiler, hotwell or mud tank falls below the start level, stop when they are all full
		double lowest = Math.min(Math.min(boilers.stream().mapToDouble(BoilerBlockEntity::waterLevel).min().orElse(1.0),
				plants.stream().mapToDouble(TurbineGeneratorBlockEntity::hotwellLevel).min().orElse(1.0)),
				Math.min(rigs.stream().mapToDouble(RigSubstructureBlockEntity::waterLevel).min().orElse(1.0),
						furnaces.stream().mapToDouble(WaterUser::waterLevel).min().orElse(1.0)));
		pump.wanted = pump.wanted ? lowest < 1.0 : lowest < Tuning.PUMP_START_LEVEL;
		double strength = pump.motor.output();
		if (strength == 0) {
			pump.status = pump.wanted ? pump.motor.describe() : "boilers full, " + pump.motor.describe();
			return;
		}
		int budget = (int) Math.round(Tuning.PUMP_WATER_PER_TICK * Tuning.PLUMBING_INTERVAL * strength);
		int moved = Plumbing.deliver(boilers, budget);
		for (TurbineGeneratorBlockEntity plant : plants) {
			moved += (int) Math.round(plant.addMakeup((budget - moved) / (double) plants.size()));
		}
		for (RigSubstructureBlockEntity rig : rigs) {
			moved += rig.addWater(budget - moved);
		}
		for (WaterUser furnace : furnaces) {
			moved += furnace.addWater(budget - moved);
		}
		pump.status = (moved > 0 ? "pumping " + Math.round(moved * 20.0 / Tuning.PLUMBING_INTERVAL) + " water/s to " + boilers.size() + " boiler(s), " + plants.size() + " plant(s)"
				+ (rigs.isEmpty() ? "" : " and " + rigs.size() + " rig's mud tanks")
				: "boilers are full") + ", " + pump.motor.describe();
	}

	@Override
	public Component inspect() {
		return Component.literal("Water pump (" + Math.round(Tuning.PUMP_WATTS) + " W motor, " + Voltage.LOW.label() + "): " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.MOTOR_HUM : null;
	}

	@Override
	public float soundPitch() {
		return 1.0F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.4F : 0;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = running.tag();
		tag.putFloat("rpm", spin.rpm());
		return tag;
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		spin.setRpm(input.getFloatOr("rpm", 0));
	}

	// ---- moving parts: the jaw coupling between the motor and the bearing frame, under its open guard -------------------------
	@Override
	public List<Rotor> rotors() {
		dev.wildcatter.kinetic.RotorBlock rotor = (dev.wildcatter.kinetic.RotorBlock) WildcatterBlocks.ROTOR;
		return List.of(new Rotor(rotor.part(dev.wildcatter.kinetic.RotorBlock.Part.STARTER_PUMP_COUPLING), new org.joml.Vector3f(0.5F, 0.2F, 0.42F), Direction.Axis.Z, 1.0F));
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(PumpBlock.FACING);
	}

	@Override
	public dev.wildcatter.kinetic.Spin spin() {
		return spin;
	}
}
