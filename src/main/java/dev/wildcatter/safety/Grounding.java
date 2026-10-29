package dev.wildcatter.safety;

import dev.wildcatter.Tuning;
import dev.wildcatter.block.GroundRodBlock;
import dev.wildcatter.block.GroundWireBlock;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine is grounded if a ground rod touches it, or a chain of ground wire leads from it to a ground rod.
 * The rod has to actually be in the earth (something solid under it).
 */
public final class Grounding {
	private Grounding() {
	}

	public static boolean isGrounded(Level level, BlockPos machine) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(machine);
		seen.add(machine);
		while (!queue.isEmpty() && seen.size() <= Tuning.GROUND_SEARCH_LIMIT) {
			BlockPos here = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = here.relative(side);
				if (!seen.add(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof GroundRodBlock && inEarth(level, next)) {
					return true;
				}
				if (state.getBlock() instanceof GroundWireBlock || state.getBlock() instanceof GroundRodBlock) {
					queue.add(next);
				}
			}
		}
		return false;
	}

	/** A rod works if it stands on (or hangs in) solid ground. */
	private static boolean inEarth(Level level, BlockPos rod) {
		BlockState below = level.getBlockState(rod.below());
		return !below.isAir() && below.getFluidState().isEmpty() && below.isSolid();
	}
}
