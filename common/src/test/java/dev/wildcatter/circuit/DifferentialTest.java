package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

class DifferentialTest {
	@Test
	void eightySevenTOperatesOnAnInternalFaultAndNotOnThroughLoad() {
		assertFalse(Differential.transformer(0.0, 1.0));       // full load passing through: no difference
		assertFalse(Differential.transformer(0.2, 0.5));       // under the 0.3 pu floor
		assertTrue(Differential.transformer(0.4, 0.5));
		assertFalse(Differential.transformer(2.0, 10.0));      // a heavy through fault with CT error under the 25 % slope
		assertTrue(Differential.transformer(8.0, 8.0));        // a fault in the zone: everything in goes to it
	}

	@Test
	void eightySevenGIsMoreSensitive() {
		assertTrue(Differential.generator(0.15, 1.0));
		assertFalse(Differential.transformer(0.15, 1.0));
	}

	@Test
	void clampsSurviveAFaultClearedInFiveCyclesAndBurnOnALongOne() {
		double clearing = Tuning.FAULT_CLEARING_CYCLES / 60.0;
		ClampHeating set = new ClampHeating();
		set.seen(40_000);                                       // 40 kA, cleared by 87B on the solve it appeared in
		assertFalse(set.step(0, 0.5, clearing));
		assertTrue(set.share() < 1);
		ClampHeating left = new ClampHeating();
		boolean burnt = false;
		double t = 0;
		while (!burnt && t < 60) {                              // 10 kA that nothing clears: about 3 s
			left.seen(10_000);
			burnt = left.step(10_000, 0.5, clearing);
			t += 0.5;
		}
		assertTrue(burnt && t > 2 && t < 4.5, "burned at " + t);
		ClampHeating small = new ClampHeating();
		for (int k = 0; k < 1000; k++) {                        // the cable's continuous current never burns it
			small.seen(250);
			assertFalse(small.step(250, 1, clearing));
		}
	}

	@Test
	void aHeatedSetCoolsBack() {
		ClampHeating set = new ClampHeating();
		set.seen(20_000);
		set.step(20_000, 0.5, 0.08);
		double hot = set.share();
		set.step(0, 60, 0.08);
		assertTrue(set.share() < hot);
	}
}
