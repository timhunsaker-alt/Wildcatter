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
 * (and straight away when something changes). A pass that takes longer than {@link Tuning#SOLVE_BUDGET_NANOS} finishes over
 * the next ticks rather than stalling the server.
 */
public final class CircuitManager {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PARTS = new HashMap<>();
	private static final Map<ResourceKey<Level>, Long> LAST_SOLVE = new HashMap<>();
	private static final Set<ResourceKey<Level>> DIRTY = new HashSet<>();
	/** A pass that ran out of its tick's time budget, finishing over the next ticks. */
	private static final Map<ResourceKey<Level>, SolvePass<List<BlockPos>>> PASSES = new HashMap<>();
	/** When each group was solved in the last finished pass, by its key. */
	private static final Map<ResourceKey<Level>, Map<Long, Long>> GROUP_SOLVED = new HashMap<>();
	/** The frequency of each island of generators, keyed by the lowest block position in it. */
	private static final Map<ResourceKey<Level>, Map<Long, Island>> ISLANDS = new HashMap<>();
	private static dev.wildcatter.energy.ElectricalStandard lastStandard;

	/** Forgets every part, island and solve time; called when the server stops so a new world starts clean. */
	public static void clearAll() {
		PARTS.clear();
		LAST_SOLVE.clear();
		DIRTY.clear();
		ISLANDS.clear();
		PASSES.clear();
		GROUP_SOLVED.clear();
	}

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
		ResourceKey<Level> dim = level.dimension();
		SolvePass<List<BlockPos>> pass = PASSES.get(dim);
		if (pass == null) {
			// a pass already under way finishes first; a change during it starts the next one
			if (!SolvePass.due(DIRTY.remove(dim), now, Tuning.SOLVE_INTERVAL)) {
				return;
			}
			int ticks = (int) Math.max(1, now - LAST_SOLVE.getOrDefault(dim, now - Tuning.SOLVE_INTERVAL));
			LAST_SOLVE.put(dim, now);
			pass = new SolvePass<>(groups(level, registered), now, ticks, CircuitManager::groupKey, GROUP_SOLVED.getOrDefault(dim, Map.of()));
			PASSES.put(dim, pass);
		}
		SolvePass<List<BlockPos>> running = pass;
		running.run(now, Tuning.SOLVE_BUDGET_NANOS, System::nanoTime, (group, ticks) -> solveGroup(level, group, ticks, running.live));
		if (running.done()) {
			PASSES.remove(dim);
			GROUP_SOLVED.put(dim, running.solvedAt());
			ISLANDS.computeIfAbsent(dim, k -> new HashMap<>()).keySet().retainAll(running.live);
		} else {
			deferredTicks++;
		}
	}

	/** Ticks on which a pass ran out of its time budget and left groups for the next tick: the game test can read it. */
	public static volatile long deferredTicks;

	/** The connected groups of parts: each is solved on its own. */
	private static List<List<BlockPos>> groups(ServerLevel level, Set<BlockPos> registered) {
		Set<BlockPos> seen = new HashSet<>();
		List<List<BlockPos>> groups = new ArrayList<>();
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
				// a big part touches its neighbours through every one of its cells, and a neighbour that is a piece of one means its main cell
				for (BlockPos cell : dev.wildcatter.multiblock.BigParts.cellsAt(level, here)) {
					for (Direction side : Direction.values()) {
						BlockPos next = dev.wildcatter.multiblock.BigParts.owner(level, cell.relative(side));
						if (registered.contains(next) && seen.add(next)) {
							queue.add(next);
						}
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
			groups.add(group);
		}
		return groups;
	}

	/** The biggest circuit solved since the last {@link #resetSolveStats} and how long its solve took: the game test reads these. */
	public static volatile int largestNodes;
	public static volatile long largestNanos;

	public static void resetSolveStats() {
		largestNodes = 0;
		largestNanos = 0;
	}

	/** A group's identity: its lowest block position (also its frequency island's key). */
	private static long groupKey(List<BlockPos> group) {
		return group.stream().mapToLong(BlockPos::asLong).min().orElse(0L);
	}

	private static void solveGroup(ServerLevel level, List<BlockPos> group, int ticks, Set<Long> live) {
		long key = groupKey(group);
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
				// a group deferred to a later tick may reach into a chunk that has since unloaded: skip it rather than load it again
				BlockEntity be = level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
				if (be instanceof CircuitPart part && !be.isRemoved()) {
					parts.add(part);
					part.build(circuit);
				}
			}
			if (circuit.nodes() > Tuning.MAX_CIRCUIT_NODES) {
				Wildcatter.LOGGER.warn("A circuit near {} has {} nodes, more than the {} allowed; it is not simulated", group.get(0), circuit.nodes(), Tuning.MAX_CIRCUIT_NODES);
				return;
			}
			long started = System.nanoTime();
			boolean solved = circuit.solve();
			long took = System.nanoTime() - started;
			if (circuit.nodes() >= largestNodes) {
				largestNodes = circuit.nodes();
				largestNanos = took;
			}
			if (!solved) {
				Wildcatter.LOGGER.warn("Could not solve the circuit near {}", group.get(0));
				return;
			}
			boolean tripped = false;
			for (CircuitPart part : ProtectionRace.acting(parts, circuit)) {
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
			delivered += generator.deliveredWatts();
		}
		// net over the island: current circulating between machines (one driving another) is not load
		delivered = Math.max(0, delivered);
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
