package dev.wildcatter.kinetic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Whether a machine's motor is running, kept in step with the client so the client can play its loop (docs/kinetics-and-sound.md).
 * The server calls {@link #update}; the block entity puts {@link #tag()} in its update tag and reads it back with {@link #load}.
 */
public final class RunningSound {
	private boolean on;

	public boolean on() {
		return on;
	}

	/** Server side: tells the client when the machine starts or stops. */
	public void update(BlockEntity machine, boolean running) {
		if (running != on && machine.getLevel() != null) {
			on = running;
			machine.getLevel().sendBlockUpdated(machine.getBlockPos(), machine.getBlockState(), machine.getBlockState(), 3);
		}
	}

	public CompoundTag tag() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("running", on);
		return tag;
	}

	public void load(ValueInput input) {
		on = input.getBooleanOr("running", on);
	}

	/** Client side, every few ticks: keeps the loop going while it runs. */
	public static void clientTick(BlockEntity machine) {
		if (machine.getLevel() != null && machine.getLevel().getGameTime() % 10 == 0 && machine instanceof Sounding s && s.loop() != null) {
			Sounding.Hook.start.accept(machine);
		}
	}
}
