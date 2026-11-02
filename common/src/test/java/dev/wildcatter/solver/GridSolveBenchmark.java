package dev.wildcatter.solver;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Times {@link PhasorSolver#solve()} on synthetic grids of growing size. Off by default (it takes a while); run it with
 * {@code ./gradlew test --tests '*GridSolveBenchmark' -Dwildcatter.bench=true}. The numbers in docs/grid-performance.md come from it.
 */
class GridSolveBenchmark {
	/** Three-phase feeders: a chain of {@code n / 3} poles per phase, a load at every pole, a generator every 50, a transformer every 100. */
	static PhasorSolver feeder(int n) {
		PhasorSolver s = new PhasorSolver();
		int poles = Math.max(2, n / 3);
		for (int ph = 0; ph < 3; ph++) {
			int prev = s.node();
			s.source(prev, PhasorSolver.GROUND, 7200, ph * 120, 0.5);
			for (int i = 1; i < poles; i++) {
				int here = s.node();
				s.impedance(prev, here, new Complex(0.05, 0.08));
				if (i % 100 == 0) {
					int low = s.node();
					s.transformer(here, PhasorSolver.GROUND, low, PhasorSolver.GROUND, new Complex(30, 0), new Complex(0.5, 1.5));
					s.resistor(low, PhasorSolver.GROUND, 2.0);
				} else {
					s.resistor(here, PhasorSolver.GROUND, 500 + i % 37);
				}
				if (i % 50 == 0) {
					s.source(here, PhasorSolver.GROUND, 7200, ph * 120 - 2, 1.0);
				}
				prev = here;
			}
		}
		return s;
	}

	/** A meshed network: a square lattice of buses, every tie an impedance, loads everywhere and generators on a sparse grid. */
	static PhasorSolver mesh(int n) {
		PhasorSolver s = new PhasorSolver();
		int w = (int) Math.max(2, Math.sqrt(n));
		int[][] id = new int[w][w];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < w; y++) {
				id[x][y] = s.node();
				s.resistor(id[x][y], PhasorSolver.GROUND, 300);
				if (x > 0) s.impedance(id[x - 1][y], id[x][y], new Complex(0.02, 0.05));
				if (y > 0) s.impedance(id[x][y - 1], id[x][y], new Complex(0.02, 0.05));
				if (x % 8 == 0 && y % 8 == 0) s.source(id[x][y], PhasorSolver.GROUND, 7200, 0, 0.3);
			}
		}
		return s;
	}

	static double millis(java.util.function.IntFunction<PhasorSolver> make, int n) {
		for (int i = 0; i < 3; i++) make.apply(n).solve();   // warm up
		int runs = 7;
		long best = Long.MAX_VALUE;
		for (int i = 0; i < runs; i++) {
			PhasorSolver s = make.apply(n);
			long t = System.nanoTime();
			assertTrue(s.solve());
			best = Math.min(best, System.nanoTime() - t);
		}
		return best / 1e6;
	}

	@Test
	void timings() {
		Assumptions.assumeTrue(Boolean.getBoolean("wildcatter.bench"), "benchmark off; -Dwildcatter.bench=true");
		System.out.println("nodes  feeder_ms  mesh_ms");
		for (int n : new int[] {100, 250, 500, 1000, 2000, 4000, 8000}) {
			System.out.printf("%5d  %9.3f  %7.3f  (%d mesh nodes)%n", feeder(n).nodes(), millis(GridSolveBenchmark::feeder, n), millis(GridSolveBenchmark::mesh, n), mesh(n).nodes());
		}
	}

	/** Twenty 2,000-node meshes on one level: the whole pass in one tick (before) against the pass under the tick budget (after). */
	@Test
	void budgetedPass() {
		Assumptions.assumeTrue(Boolean.getBoolean("wildcatter.bench"), "benchmark off; -Dwildcatter.bench=true");
		java.util.List<Integer> groups = java.util.Collections.nCopies(20, 2000);
		for (int i = 0; i < 3; i++) mesh(2000).solve();
		long t = System.nanoTime();
		for (int n : groups) mesh(n).solve();
		double whole = (System.nanoTime() - t) / 1e6;
		dev.wildcatter.circuit.SolvePass<Integer> pass = new dev.wildcatter.circuit.SolvePass<>(groups, 0, 4, g -> 0, java.util.Map.of());
		double worst = 0;
		int ticks = 0;
		while (!pass.done()) {
			long s = System.nanoTime();
			pass.run(ticks, dev.wildcatter.Tuning.SOLVE_BUDGET_NANOS, System::nanoTime, (n, k) -> mesh(n).solve());
			worst = Math.max(worst, (System.nanoTime() - s) / 1e6);
			ticks++;
		}
		System.out.printf("20 x 2000-node mesh: one tick %.1f ms before; after: worst tick %.1f ms, pass spread over %d ticks%n", whole, worst, ticks);
	}
}
