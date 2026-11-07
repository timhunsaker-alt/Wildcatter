package dev.wildcatter.control;

import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What a cabinet's control cables reach. Signals are not put through the circuit solver, whose groups they would swell: this is continuity
 * only, walked over loaded blocks (docs/multiblocks/plc.md).
 */
public final class ControlNet {
	private static final int MOST_CELLS = 8192;

	private ControlNet() {
	}

	/** What a walk found. {@code badJoint} is a cable cell that touches more than two things, or null. */
	public record Found(List<BlockPos> devices, boolean supplyLive, @Nullable BlockPos badJoint) {
	}

	/** The device at {@code pos}, looking through a big part's cell to its main cell. */
	public static @Nullable ControlDevice deviceAt(BlockGetter level, BlockPos pos) {
		return BigParts.entity(level, pos) instanceof ControlDevice device && !device.points().isEmpty() ? device : null;   // one with no signals is no device: an MCC bucket, an unpowered part
	}

	public static BlockPos mainOf(BlockGetter level, BlockPos pos) {
		var entity = BigParts.entity(level, pos);   // any kind of true-size part, as not all are a BigPartBlock
		return entity != null ? entity.getBlockPos() : pos;
	}

	/** True if a control cable next to {@code pos} would join it. */
	public static boolean joins(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof ControlCableBlock || state.getBlock() instanceof JunctionBoxBlock || state.getBlock() instanceof ControlSupplyBlock
				|| state.getBlock() instanceof PlcCabinetBlock || deviceAt(level, pos) != null;
	}

	private static int touching(Level level, BlockPos cable) {
		int n = 0;
		for (Direction side : Direction.values()) {
			if (joins(level, cable.relative(side))) {
				n++;
			}
		}
		return n;
	}

	/** Walks out from a cabinet's cells. */
	public static Found walk(Level level, List<BlockPos> cabinet) {
		Set<BlockPos> seen = new HashSet<>(cabinet);
		Deque<BlockPos> queue = new ArrayDeque<>(cabinet);
		Set<BlockPos> devices = new java.util.LinkedHashSet<>();
		boolean live = false;
		BlockPos bad = null;
		while (!queue.isEmpty() && seen.size() < MOST_CELLS) {
			BlockPos at = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (!level.isLoaded(next) || !seen.add(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof ControlCableBlock) {
					if (touching(level, next) > 2) {
						bad = bad == null ? next : bad;
					} else {
						queue.add(next);
					}
				} else if (state.getBlock() instanceof JunctionBoxBlock) {
					queue.add(next);
				} else if (level.getBlockEntity(next) instanceof ControlSupplyBlockEntity supply) {
					live |= supply.live();
				} else if (!(state.getBlock() instanceof PlcCabinetBlock) && deviceAt(level, next) != null) {
					devices.add(mainOf(level, next));
				}
			}
		}
		return new Found(new ArrayList<>(devices), live, bad);
	}
}
