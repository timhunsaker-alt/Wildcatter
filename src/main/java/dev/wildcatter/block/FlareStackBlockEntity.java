package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A 10 m elevated flare (docs/multiblocks/lease.md): the knockout pot at its foot, the riser and the tip with its pilot. Gas comes in
 * at the bottom cell. It burns only what a separator's or a gas plant part's back-pressure valve lets go when it is three quarters
 * full: the gas no burner, engine or plant is taking. It never takes a gas well's own gas.
 */
public class FlareStackBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, dev.wildcatter.kinetic.Sounding {
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private double burned;
	private int quiet;
	private boolean grown;

	public FlareStackBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FLARE_STACK_ENTITY, pos, state);
	}

	public boolean burning() {
		return running.on();
	}

	private List<Lines.Face> inlet() {
		return Lines.faces(List.of(worldPosition)).stream().filter(f -> f.side().getAxis().isHorizontal()).toList();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FlareStackBlockEntity flare) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		flare.burned = 0;
		for (Lines.Line line : Lines.lines(level, flare.inlet())) {   // whatever gas its line carries: a separator's, or a gas plant's
			Product gas = line.run().product();
			if (gas != null && gas.gas()) {
				flare.burned += Lines.pull(line, gas, Tuning.SEPARATOR_GAS_M3S * Tuning.WELL_INTERVAL / 20.0 * 4, FluidNode.Pull.FLARE, flare);
			}
		}
		flare.quiet = flare.burned > 1e-6 ? 0 : flare.quiet + 1;
		flare.running.update(flare, flare.quiet < 10);   // the flame dies back two seconds after the gas stops
	}

	/** Client side: the flame at the tip while it burns, and its roar. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, FlareStackBlockEntity flare) {
		dev.wildcatter.kinetic.RunningSound.clientTick(flare);
		if (!flare.burning()) {
			return;
		}
		double x = pos.getX() + 0.5, y = pos.getY() + 10.1, z = pos.getZ() + 0.5;
		for (int i = 0; i < 3; i++) {
			level.addParticle(ParticleTypes.FLAME, x + (level.getRandom().nextDouble() - 0.5) * 0.4, y + level.getRandom().nextDouble() * 0.6,
					z + (level.getRandom().nextDouble() - 0.5) * 0.4, 0, 0.08 + level.getRandom().nextDouble() * 0.06, 0);
		}
		if (level.getRandom().nextInt(4) == 0) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 1.2, z, 0, 0.06, 0);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Flare stack (10 m): " + (burning()
				? String.format("BURNING %.2f m3/s of gas the lease cannot use", burned * 20.0 / Tuning.WELL_INTERVAL)
				: "pilot lit, nothing to burn (it takes only the excess gas of a separator or a gas plant, through the bottom cell)"));
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.BURNER_ROAR : null;
	}

	@Override
	public float soundPitch() {
		return 0.7F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.8F : 0;
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

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		grown = input.getBooleanOr("grown", grown);
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return running.tag();
	}
}
