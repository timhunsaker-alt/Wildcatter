package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Finds the boilers a pump or a drain can reach through a run of pipes. */
public final class Plumbing {
	private Plumbing() {
	}

	/** Every pipe joined to a pipe that touches one of {@code cells}. */
	public static Set<BlockPos> network(Level level, Collection<BlockPos> cells) {
		return network(level, cells, PipeBlock.Kind.WATER);
	}

	private static boolean isPipe(Level level, BlockPos pos, PipeBlock.Kind kind) {
		return level.getBlockState(pos).getBlock() instanceof PipeBlock pipe && pipe.kind() == kind;
	}

	/** Every pipe of this kind joined to a pipe that touches one of {@code cells}. */
	public static Set<BlockPos> network(Level level, Collection<BlockPos> cells, PipeBlock.Kind kind) {
		Set<BlockPos> pipes = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos cell : cells) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (!cells.contains(next) && isPipe(level, next, kind) && pipes.add(next)) {
					queue.add(next);
				}
			}
		}
		while (!queue.isEmpty() && pipes.size() < Tuning.PIPE_NETWORK_LIMIT) {
			BlockPos at = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (isPipe(level, next, kind) && pipes.add(next)) {
					queue.add(next);
				}
			}
		}
		return pipes;
	}

	/**
	 * The boilers that touch the pipes of the network (and, if {@code direct}, the ones touching {@code cells} themselves).
	 * A coil boiler shows up once, however many of its blocks touch.
	 */
	public static List<BoilerBlockEntity> boilers(Level level, Collection<BlockPos> cells, Set<BlockPos> pipes, boolean direct) {
		Map<BlockPos, BoilerBlockEntity> found = new LinkedHashMap<>();
		List<BlockPos> sources = new ArrayList<>(pipes);
		if (direct) {
			sources.addAll(cells);
		}
		for (BlockPos source : sources) {
			for (Direction side : Direction.values()) {
				BlockPos next = source.relative(side);
				if (!cells.contains(next) && dev.wildcatter.multiblock.BigParts.entity(level, next) instanceof BoilerBlockEntity boiler) {
					found.putIfAbsent(boiler.rootPos(), boiler);
				}
			}
		}
		return new ArrayList<>(found.values());
	}

	/** The block entities of one kind touching the gas lines (and, if {@code direct}, the cells). */
	public static <T extends net.minecraft.world.level.block.entity.BlockEntity> List<T> machines(Level level, Collection<BlockPos> cells,
			Set<BlockPos> pipes, boolean direct, Class<T> kind) {
		Map<BlockPos, T> found = new LinkedHashMap<>();
		List<BlockPos> sources = new ArrayList<>(pipes);
		if (direct) {
			sources.addAll(cells);
		}
		for (BlockPos source : sources) {
			for (Direction side : Direction.values()) {
				BlockPos next = source.relative(side);
				// a true-size machine shows up once, through whichever of its cells the pipe touches
				net.minecraft.world.level.block.entity.BlockEntity entity = dev.wildcatter.multiblock.BigParts.entity(level, next);
				if (level.getBlockState(next).getBlock() instanceof OilWellheadBlock) {   // a pumpjack's crude comes out of its wellhead
					entity = OilWellheadBlock.pumpjack(level, next);
				}
				if (!cells.contains(next) && kind.isInstance(entity)) {
					found.putIfAbsent(entity.getBlockPos(), kind.cast(entity));
				}
			}
		}
		return new ArrayList<>(found.values());
	}

	/** Pours water into the boilers, returns how much fitted. */
	public static int deliver(List<BoilerBlockEntity> boilers, int amount) {
		int left = amount;
		for (BoilerBlockEntity boiler : boilers) {
			if (left <= 0) {
				break;
			}
			left -= boiler.addWater(left);
		}
		return amount - left;
	}
}
