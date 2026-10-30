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
				if (!cells.contains(next) && level.getBlockEntity(next) instanceof BoilerBlockEntity boiler) {
					found.putIfAbsent(boiler.rootPos(), boiler);
				}
			}
		}
		return new ArrayList<>(found.values());
	}

	/** The fireboxes that touch the gas lines (and, if {@code direct}, the cells). A big firebox shows up once. */
	public static List<FireboxBlockEntity> fireboxes(Level level, Collection<BlockPos> cells, Set<BlockPos> pipes, boolean direct) {
		Map<BlockPos, FireboxBlockEntity> found = new LinkedHashMap<>();
		List<BlockPos> sources = new ArrayList<>(pipes);
		if (direct) {
			sources.addAll(cells);
		}
		for (BlockPos source : sources) {
			for (Direction side : Direction.values()) {
				BlockPos next = source.relative(side);
				if (!cells.contains(next) && level.getBlockEntity(next) instanceof FireboxBlockEntity firebox) {
					found.putIfAbsent(firebox.rootPos(), firebox);
				}
			}
		}
		return new ArrayList<>(found.values());
	}

	/** Hands up to {@code available} ticks of fire to the fireboxes that are hungry; returns how many were taken. */
	public static double feedFireboxes(List<FireboxBlockEntity> fireboxes, double available) {
		double left = available;
		for (FireboxBlockEntity firebox : fireboxes) {
			if (left <= 0) {
				break;
			}
			left -= firebox.feed(left);
		}
		return available - left;
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
