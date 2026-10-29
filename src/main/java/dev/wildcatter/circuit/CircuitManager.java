package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Keeps track of every circuit part in loaded chunks, groups the ones that touch, and solves each group every few ticks
 * (and straight away when something changes).
 */
public final class CircuitManager {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PARTS = new HashMap<>();
	private static final Map<ResourceKey<Level>, Long> LAST_SOLVE = new HashMap<>();
	private static final Set<ResourceKey<Level>> DIRTY = new HashSet<>();

	private CircuitManager() {
	}

	public static void register(BlockEntity be, ServerLevel level) {
		if (be instanceof CircuitPart) {
			PARTS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(be.getBlockPos().immutable());
			DIRTY.add(level.dimension());
		}
	}

	public static void unregister(BlockEntity be, ServerLevel level) {
		Set<BlockPos> set = PARTS.get(level.dimension());
		if (set != null && set.remove(be.getBlockPos())) {
			DIRTY.add(level.dimension());
		}
	}

	/** Something changed (a switch, a breaker, a wire): solve again on the next tick instead of waiting. */
	public static void markDirty(Level level) {
		DIRTY.add(level.dimension());
	}

	public static void tick(ServerLevel level) {
		Set<BlockPos> registered = PARTS.get(level.dimension());
		if (registered == null || registered.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		boolean due = DIRTY.remove(level.dimension()) || now % Tuning.SOLVE_INTERVAL == 0;
		if (!due) {
			return;
		}
		int ticks = (int) Math.max(1, now - LAST_SOLVE.getOrDefault(level.dimension(), now - Tuning.SOLVE_INTERVAL));
		LAST_SOLVE.put(level.dimension(), now);

		Set<BlockPos> seen = new HashSet<>();
		for (BlockPos start : new ArrayList<>(registered)) {
			if (seen.contains(start)) {
				continue;
			}
			List<BlockPos> group = new ArrayList<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			seen.add(start);
			while (!queue.isEmpty()) {
				BlockPos here = queue.poll();
				group.add(here);
				for (Direction side : Direction.values()) {
					BlockPos next = here.relative(side);
					if (registered.contains(next) && seen.add(next)) {
						queue.add(next);
					}
				}
			}
			solveGroup(level, group, ticks);
		}
	}

	private static void solveGroup(ServerLevel level, List<BlockPos> group, int ticks) {
		Circuit circuit = null;
		List<CircuitPart> parts = new ArrayList<>();
		// If a breaker trips the group is solved again without it, so nothing downstream reacts to the fault current.
		for (int attempt = 0; attempt < 4; attempt++) {
			circuit = new Circuit();
			parts.clear();
			for (BlockPos pos : group) {
				BlockEntity be = level.getBlockEntity(pos);
				if (be instanceof CircuitPart part && !be.isRemoved()) {
					parts.add(part);
					part.build(circuit);
				}
			}
			if (circuit.nodes() > Tuning.MAX_CIRCUIT_NODES) {
				Wildcatter.LOGGER.warn("A circuit near {} has {} nodes, more than the {} allowed; it is not simulated", group.get(0), circuit.nodes(), Tuning.MAX_CIRCUIT_NODES);
				return;
			}
			if (!circuit.solve()) {
				Wildcatter.LOGGER.warn("Could not solve the circuit near {}", group.get(0));
				return;
			}
			boolean tripped = false;
			for (CircuitPart part : parts) {
				tripped |= part.protect(circuit, ticks);
			}
			if (!tripped) {
				break;
			}
		}
		for (CircuitPart part : parts) {
			part.apply(circuit, ticks);
		}
	}
}
