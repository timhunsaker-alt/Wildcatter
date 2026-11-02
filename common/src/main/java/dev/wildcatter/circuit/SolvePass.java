package dev.wildcatter.circuit;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.ObjIntConsumer;
import java.util.function.ToLongFunction;

/**
 * One pass over a level's circuit groups. A pass normally finishes in the tick it starts; when solving its groups takes longer
 * than the per-tick budget ({@link dev.wildcatter.Tuning#SOLVE_BUDGET_NANOS}) the rest wait for the next tick, so a huge grid
 * slows its own updates instead of stalling the server. Plain Java, so it is unit-tested without Minecraft.
 */
public final class SolvePass<G> {
	private final ArrayDeque<G> groups;
	private final long startedTick;
	private final int ticks;
	private final ToLongFunction<G> key;
	private final Map<Long, Long> previous;
	private final Map<Long, Long> solvedAt = new HashMap<>();
	/** The islands still running, gathered over the whole pass. */
	final Set<Long> live = new HashSet<>();

	/**
	 * @param ticks game ticks since the previous pass started
	 * @param key a group's identity from pass to pass (the circuit uses its lowest block position)
	 * @param previous when each group was solved in the previous pass ({@link #solvedAt()} of it), or empty
	 */
	public SolvePass(Collection<G> groups, long startedTick, int ticks, ToLongFunction<G> key, Map<Long, Long> previous) {
		this.groups = new ArrayDeque<>(groups);
		this.startedTick = startedTick;
		this.ticks = ticks;
		this.key = key;
		this.previous = previous;
	}

	/** The tick each group was solved at in this pass: hand it to the next pass. */
	public Map<Long, Long> solvedAt() {
		return solvedAt;
	}

	/** Whether a new pass should start: something changed, or the regular cadence for loads that move on their own. */
	public static boolean due(boolean dirty, long now, int interval) {
		return dirty || now % interval == 0;
	}

	public boolean done() {
		return groups.isEmpty();
	}

	public int remaining() {
		return groups.size();
	}

	/**
	 * Ticks of simulated time for {@code group} solved at {@code now}: since its own last solve, so a group deferred in either
	 * pass is neither shorted nor counted twice. A group solved in the tick its pass started, after one solved the same way,
	 * gets exactly the pass's {@code ticks}.
	 */
	public int ticksFor(G group, long now) {
		Long last = previous.get(key.applyAsLong(group));
		return last != null ? (int) Math.max(1, now - last) : (int) (ticks + Math.max(0, now - startedTick));
	}

	/**
	 * Solves groups in order until the pass is done or {@code budgetNanos} has gone by; at least one group always runs, so every
	 * pass finishes. Returns how many were solved.
	 */
	public int run(long now, long budgetNanos, LongSupplier clock, ObjIntConsumer<G> solve) {
		long start = clock.getAsLong();
		int solved = 0;
		while (!groups.isEmpty()) {
			if (solved > 0 && clock.getAsLong() - start >= budgetNanos) {
				break;
			}
			G group = groups.poll();
			int t = ticksFor(group, now);
			solvedAt.put(key.applyAsLong(group), now);
			solve.accept(group, t);
			solved++;
		}
		return solved;
	}
}
