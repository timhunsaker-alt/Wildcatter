package dev.wildcatter.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Moves energy between neighbours. There is no network object: every generator and cable just pushes into whatever
 * touches it at the same voltage. Between two things that can both give energy (cables, batteries) it only flows from
 * the fuller one to the emptier one, so a long line of cable levels out instead of bouncing back and forth. A
 * machine that only takes energy is always topped up first.
 */
public final class EnergyNet {
	private EnergyNet() {
	}

	/** Pushes out of {@code from} on every side. Returns how much left. */
	public static int push(Level level, BlockPos pos, EnergyBuffer from, Voltage voltage) {
		int sent = 0;
		for (Direction side : Direction.values()) {
			sent += push(level, pos, from, voltage, side);
		}
		return sent;
	}

	/** Pushes out of {@code from} into the neighbour on one side only (a transformer's two sides use different buffers). */
	public static int push(Level level, BlockPos pos, EnergyBuffer from, Voltage voltage, Direction side) {
		if (from.available() <= 0) {
			return 0;
		}
		BlockEntity be = level.getBlockEntity(pos.relative(side));
		if (!(be instanceof EnergyNode node) || !node.connectsTo(side.getOpposite(), voltage)) {
			return 0;
		}
		EnergyBuffer to = node.energyAt(side.getOpposite());
		if (to == from || (!to.isSink() && to.fraction() >= from.fraction())) {
			return 0; // only flow downhill between two things that can both give
		}
		int amount = Math.min(from.available(), to.room());
		if (!to.isSink()) {
			// Stop at the halfway point so two equal neighbours don't swap the same energy back and forth.
			int gap = from.stored() - to.stored();
			amount = Math.min(amount, Math.max(1, gap / 2));
		}
		return amount > 0 ? to.insert(from.extract(amount)) : 0;
	}
}
