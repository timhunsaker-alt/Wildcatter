package dev.wildcatter.safety;

import dev.wildcatter.Tuning;
import dev.wildcatter.block.GroundRodBlock;
import dev.wildcatter.block.GroundWireBlock;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine is grounded if a ground rod touches it, or a chain of ground wire leads from it to a ground rod.
 * The rod has to actually be in the earth (something solid under it). A machine of several cells (a big part: the rig, the pumpjack, a
 * transformer) is one metal frame, so a rod or ground wire against any of its cells grounds it, whichever cell is asked about.
 */
public final class Grounding {
	private Grounding() {
	}

	public static boolean isGrounded(Level level, BlockPos machine) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		java.util.List<BlockPos> cells = BigParts.cellsAt(level, machine);
		seen.addAll(cells);
		// every face of every cell first, outside the wire search's limit: a big part has more faces than a one-block machine
		for (BlockPos cell : cells) {
			if (rodOrWire(level, cell, seen, queue)) {
				return true;
			}
		}
		// then along ground wire, as far as a one-block machine's search reaches (its own cell and six faces count towards the limit)
		int offset = seen.size() - 1 - Direction.values().length;
		while (!queue.isEmpty() && seen.size() - offset <= Tuning.GROUND_SEARCH_LIMIT) {
			if (rodOrWire(level, queue.poll(), seen, queue)) {
				return true;
			}
		}
		return false;
	}

	/** Looks at the six faces of {@code here}: true at a rod in the earth; ground wire and rods (out of the earth) are queued to follow. */
	private static boolean rodOrWire(Level level, BlockPos here, Set<BlockPos> seen, ArrayDeque<BlockPos> queue) {
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
		return false;
	}

	/** A rod works if it stands on (or hangs in) solid ground. */
	private static boolean inEarth(Level level, BlockPos rod) {
		BlockState below = level.getBlockState(rod.below());
		return !below.isAir() && below.getFluidState().isEmpty() && below.isSolid();
	}
}
