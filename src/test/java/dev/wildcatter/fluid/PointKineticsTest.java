package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 8a: six-group point kinetics (docs/multiblocks/reactor.md). */
class PointKineticsTest {
	private static double periodAfter(double rho, double settle) {
		PointKinetics k = new PointKinetics();
		k.hold(0.01);
		for (double t = 0; t < settle; t += 0.2) {
			k.step(rho, 0.2);
		}
		double before = k.power();
		for (double t = 0; t < 20; t += 0.2) {
			k.step(rho, 0.2);
		}
		return 20 / Math.log(k.power() / before);
	}

	@Test
	void aHundredPcmGivesTheInhourPeriod() {
		assertEquals(54.9, periodAfter(0.001, 300), 2.0);   // the inhour equation's 54.9 s
		assertEquals(136.8, periodAfter(0.0005, 600), 4.0);
	}

	@Test
	void atZeroReactivityThePowerHolds() {
		PointKinetics k = new PointKinetics();
		k.hold(0.02);
		for (int i = 0; i < 3000; i++) {
			k.step(0, 0.2);
		}
		assertEquals(0.02, k.power(), 0.0005);
	}

	@Test
	void aTripDropsPromptlyThenOnTheLongestGroup() {
		PointKinetics k = new PointKinetics();
		k.hold(1.0);
		k.step(-0.06, 0.2);
		assertTrue(k.power() < 0.15, "the prompt drop: " + k.power());
		for (double t = 0; t < 300; t += 0.2) {
			k.step(-0.06, 0.2);
		}
		double before = k.power();
		for (double t = 0; t < 60; t += 0.2) {
			k.step(-0.06, 0.2);
		}
		assertEquals(-80.6, 60 / Math.log(k.power() / before), 3.0);   // 1 / 0.0124 s
	}
}
