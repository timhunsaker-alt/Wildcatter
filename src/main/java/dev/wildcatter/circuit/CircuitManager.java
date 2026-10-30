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
	/** The frequency of each island of generators, keyed by the lowest block position in it. */
	private static final Map<ResourceKey<Level>, Map<Long, Island>> ISLANDS = new HashMap<>();
	private static dev.wildcatter.energy.ElectricalStandard lastStandard;

	private static final class Island {
		final dev.wildcatter.solver.FrequencyIsland frequency;
		double outsideSeconds;

		Island(double nominal) {
			this.frequency = new dev.wildcatter.solver.FrequencyIsland(nominal, Tuning.GOVERNOR_DROOP, Tuning.AGC_SECONDS);
		}
	}

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
		Set<Long> live = new HashSet<>();
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
				if (level.getBlockEntity(here) instanceof CircuitPart part) {
					for (BlockPos far : part.links()) {
						if (registered.contains(far) && seen.add(far)) {
							queue.add(far);
						}
					}
				}
			}
			solveGroup(level, group, ticks, live);
		}
		ISLANDS.computeIfAbsent(level.dimension(), k -> new HashMap<>()).keySet().retainAll(live);
	}

	private static void solveGroup(ServerLevel level, List<BlockPos> group, int ticks, Set<Long> live) {
		long key = group.stream().mapToLong(BlockPos::asLong).min().orElse(0L);
		dev.wildcatter.energy.ElectricalStandard standard = dev.wildcatter.energy.ElectricalStandard.active();
		if (standard != lastStandard) {
			ISLANDS.clear();   // a different standard (another world, in the same session) has a different nominal frequency: start over
			lastStandard = standard;
		}
		Map<Long, Island> islands = ISLANDS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
		Island island = islands.get(key);
		double hertz = island != null ? island.frequency.hertz() : standard.hertz();
		Circuit circuit = null;
		List<CircuitPart> parts = new ArrayList<>();
		// If a breaker trips the group is solved again without it, so nothing downstream reacts to the fault current.
		for (int attempt = 0; attempt < 4; attempt++) {
			circuit = new Circuit();
			circuit.setHertz(hertz);
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
		stepFrequency(level, key, islands, island, standard, parts, ticks, live);
	}

	/** Moves the island's frequency on by the time since the last solve, and trips generators that stay outside the safe band. */
	private static void stepFrequency(ServerLevel level, long key, Map<Long, Island> islands, Island island,
			dev.wildcatter.energy.ElectricalStandard standard, List<CircuitPart> parts, int ticks, Set<Long> live) {
		List<Generator> online = new ArrayList<>();
		for (CircuitPart part : parts) {
			if (part instanceof Generator generator && generator.online()) {
				online.add(generator);
			}
		}
		if (online.isEmpty()) {
			islands.remove(key);
			return;
		}
		if (island == null) {
			island = new Island(standard.hertz());
			islands.put(key, island);
		}
		double rated = 0;
		double inertia = 0;
		double delivered = 0;
		for (Generator generator : online) {
			rated += generator.ratedWatts();
			inertia += generator.ratedWatts() * generator.inertiaSeconds();
			delivered += Math.max(0, generator.deliveredWatts());
		}
		double dt = ticks / 20.0;
		island.frequency.step(dt, rated, inertia / rated, delivered);
		double hertz = island.frequency.hertz();
		if (hertz < standard.generatorLowHertz() || hertz > standard.generatorHighHertz()) {
			island.outsideSeconds += dt;
		} else {
			island.outsideSeconds = 0;
		}
		if (island.outsideSeconds >= Tuning.GENERATOR_TRIP_SECONDS) {
			for (Generator generator : online) {
				generator.tripOnFrequency(hertz);
			}
			islands.remove(key);
			markDirty(level);
			return;
		}
		live.add(key);
	}
}
