package dev.wildcatter.block;

import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * How a panel's gutters chain (docs/multiblocks/panelboards.md): gutters touching the panel in the wall's plane, facing the same way,
 * and gutters touching those. Spaces are numbered in the order the chain reaches them from the panel, nearest first; at each step
 * down, up, left, then right as you face the front.
 */
final class PanelChain {
	private static final int MOST = 64;

	private PanelChain() {
	}

	/** The four faces in the wall's plane, in the order the chain is walked: down, up, left, right as you face the front. */
	static Direction[] inPlane(Direction facing) {
		return new Direction[] {Direction.DOWN, Direction.UP, facing.getClockWise(), facing.getCounterClockWise()};
	}

	static boolean isGutter(BlockState state, Direction facing) {
		return state.getBlock() instanceof PanelGutterBlock && state.getValue(BigPartBlock.FACING) == facing;
	}

	static boolean isPanel(BlockState state, Direction facing) {
		return state.getBlock() instanceof PanelBlock && state.getValue(BigPartBlock.FACING) == facing;
	}

	/** The gutters of the panel whose main cell is {@code panel}, in space order. */
	static List<BlockPos> gutters(BlockGetter level, BlockPos panel) {
		BlockState state = level.getBlockState(panel);
		List<BlockPos> order = new ArrayList<>();
		if (!(state.getBlock() instanceof PanelBlock)) {
			return order;
		}
		Direction facing = state.getValue(BigPartBlock.FACING);
		Set<BlockPos> seen = new LinkedHashSet<>(BigParts.cells(panel, state));
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(seen);
		while (!queue.isEmpty() && order.size() < MOST) {
			BlockPos here = queue.poll();
			for (Direction side : inPlane(facing)) {
				BlockPos next = here.relative(side);
				if (!seen.contains(next) && isGutter(level.getBlockState(next), facing)) {
					seen.add(next);
					order.add(next);
					queue.add(next);
				}
			}
		}
		return order;
	}

	/** The main cells of every panel this gutter's chain reaches. */
	static Set<BlockPos> panels(BlockGetter level, BlockPos gutter) {
		Set<BlockPos> panels = new LinkedHashSet<>();
		BlockState start = level.getBlockState(gutter);
		if (!(start.getBlock() instanceof PanelGutterBlock)) {
			return panels;
		}
		Direction facing = start.getValue(BigPartBlock.FACING);
		Set<BlockPos> seen = new LinkedHashSet<>();
		seen.add(gutter);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(seen);
		while (!queue.isEmpty() && seen.size() < MOST) {
			BlockPos here = queue.poll();
			for (Direction side : inPlane(facing)) {
				BlockPos next = here.relative(side);
				BlockState state = level.getBlockState(next);
				if (isPanel(state, facing)) {
					panels.add(BigParts.main(next, state));
				} else if (!seen.contains(next) && isGutter(state, facing)) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		return panels;
	}

	/** The one panel this gutter's chain reaches, or null for none or more than one. */
	static @Nullable BlockPos panel(BlockGetter level, BlockPos gutter) {
		Set<BlockPos> panels = panels(level, gutter);
		return panels.size() == 1 ? panels.iterator().next() : null;
	}
}
