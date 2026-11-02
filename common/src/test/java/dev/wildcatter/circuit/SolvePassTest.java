package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SolvePassTest {
	/** A fake clock: each group "takes" {@code cost} ns to solve. */
	static final class Clock {
		long t;
	}

	@Test
	void solvesOnlyWhenDirtyOrOnTheCadence() {
		int interval = Tuning.SOLVE_INTERVAL;
		int solves = 0;
		for (long now = 1; now <= 100; now++) {
			if (SolvePass.due(false, now, interval)) {
				solves++;
			}
		}
		assertEquals(100 / interval, solves, "a quiet grid solves only on the cadence");
		assertFalse(SolvePass.due(false, 1, interval));
		assertTrue(SolvePass.due(true, 1, interval), "a change solves at once");
		assertTrue(SolvePass.due(false, interval * 7L, interval));
	}

	@Test
	void smallGridFinishesInOneTickWithTheSameTicks() {
		Clock clock = new Clock();
		List<String> order = new ArrayList<>();
		List<Integer> ticks = new ArrayList<>();
		SolvePass<Long> pass = new SolvePass<>(List.of(1L, 2L, 3L), 100, 4, g -> g, Map.of(1L, 96L, 2L, 96L));
		int n = pass.run(100, Tuning.SOLVE_BUDGET_NANOS, () -> clock.t, (g, t) -> { clock.t += 1_000_000; order.add("g" + g); ticks.add(t); });
		assertEquals(3, n);
		assertTrue(pass.done());
		assertEquals(List.of("g1", "g2", "g3"), order);
		assertEquals(List.of(4, 4, 4), ticks, "every group gets the time since the last pass, as before");
		assertEquals(Map.of(1L, 100L, 2L, 100L, 3L, 100L), pass.solvedAt());
	}

	@Test
	void overBudgetDefersTheRestToLaterTicks() {
		Clock clock = new Clock();
		long budget = 10;
		List<Long> solvedTick = new ArrayList<>();
		List<Integer> ticks = new ArrayList<>();
		SolvePass<Long> pass = new SolvePass<>(List.of(1L, 2L, 3L, 4L, 5L), 200, 4, g -> g, Map.of());
		long now = 200;
		int tickCount = 0;
		while (!pass.done()) {
			long at = now;
			int n = pass.run(now, budget, () -> clock.t, (g, t) -> { clock.t += 6; solvedTick.add(at); ticks.add(t); });
			assertTrue(n >= 1, "every tick makes progress");
			assertTrue(n <= 2, "the budget stops the tick after it is spent");
			now++;
			tickCount++;
		}
		assertEquals(3, tickCount);
		assertEquals(List.of(200L, 200L, 201L, 201L, 202L), solvedTick);
		assertEquals(List.of(4, 4, 5, 5, 6), ticks, "a deferred group also covers the ticks it waited");
	}

	@Test
	void aHugeGroupStillRunsAlone() {
		Clock clock = new Clock();
		SolvePass<Long> pass = new SolvePass<>(List.of(1L, 2L), 0, 4, g -> g, Map.of());
		assertEquals(1, pass.run(0, 5, () -> clock.t, (g, t) -> clock.t += 1_000));
		assertEquals(1, pass.remaining());
	}

	@Test
	void deferredTimeIsNotCountedTwice() {
		// group 9 was solved two ticks late in the last pass (at 102, pass started at 100); this pass starts at 104
		SolvePass<Long> pass = new SolvePass<>(List.of(9L), 104, 4, g -> g, Map.of(9L, 102L));
		List<Integer> ticks = new ArrayList<>();
		pass.run(104, 1, () -> 0, (g, t) -> ticks.add(t));
		assertEquals(List.of(2), ticks);
	}
}
