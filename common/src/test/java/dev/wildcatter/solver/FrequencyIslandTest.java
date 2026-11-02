package dev.wildcatter.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FrequencyIslandTest {
	private static final double RATED = 100_000;

	private static FrequencyIsland island() {
		return new FrequencyIsland(60.0, 0.04, 30.0);
	}

	private static void run(FrequencyIsland f, double seconds, double load, double inertia) {
		for (double t = 0; t < seconds; t += 0.2) {
			f.step(0.2, RATED, inertia, load);
		}
	}

	@Test
	void steadyLoadHoldsNominal() {
		FrequencyIsland f = island();
		run(f, 60, 40_000, 3.0);
		assertEquals(60.0, f.hertz(), 1e-6);
	}

	@Test
	void loadStepDipsByTheDroopThenRecovers() {
		FrequencyIsland f = island();
		run(f, 10, 40_000, 3.0);
		run(f, 3, 50_000, 3.0);                          // +10% of rating
		// droop 4%: a 10% load step settles 0.4% low (0.24 Hz) before the secondary control acts
		assertEquals(60.0 - 0.24, f.hertz(), 0.06);
		run(f, 200, 50_000, 3.0);
		assertEquals(60.0, f.hertz(), 0.02);             // the secondary control has brought it back
	}

	@Test
	void lightMachinesDipFasterThanHeavyOnes() {
		FrequencyIsland light = island();
		FrequencyIsland heavy = island();
		run(light, 10, 40_000, 1.0);
		run(heavy, 10, 40_000, 5.0);
		for (FrequencyIsland f : new FrequencyIsland[] {light, heavy}) {
			f.step(0.05, RATED, f == light ? 1.0 : 5.0, 90_000);   // a sudden big step, looked at 50 ms later
		}
		assertTrue(light.hertz() < heavy.hertz(), "the lighter set should fall faster: " + light.hertz() + " vs " + heavy.hertz());
	}

	@Test
	void overloadBeyondTheRatingCollapsesTheFrequency() {
		FrequencyIsland f = island();
		run(f, 10, 40_000, 3.0);
		run(f, 3, 150_000, 3.0);                         // the load wants half as much again as the generators can give
		assertTrue(f.hertz() < 57.0, "an overload should drag the island below the generator trip point: " + f.hertz());
	}

	@Test
	void lossOfLoadLetsTheFrequencyRise() {
		FrequencyIsland f = island();
		run(f, 10, 80_000, 3.0);
		run(f, 3, 20_000, 3.0);
		assertTrue(f.hertz() > 60.0, "dropping load should raise the frequency: " + f.hertz());
	}
}
