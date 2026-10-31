package dev.wildcatter.kinetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeamLinkageTest {
	@Test
	void strokesAbout54Inches() {
		double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
		for (int d = 0; d < 360; d++) {
			double rod = BeamLinkage.rod(Math.toRadians(d));
			low = Math.min(low, rod);
			high = Math.max(high, rod);
		}
		assertEquals(1.37, high - low, 0.05, "API 80-119-54: a 54 in (1.37 m) stroke");
	}

	@Test
	void theLinkageHoldsTogetherAllTheWayRound() {
		for (int d = 0; d < 360; d++) {
			double crank = Math.toRadians(d);
			double[] pin = BeamLinkage.pin(crank), tail = BeamLinkage.tail(crank);
			assertEquals(BeamLinkage.PITMAN, Math.hypot(tail[0] - pin[0], tail[1] - pin[1]), 1e-6, "the pitman keeps its length");
			assertEquals(BeamLinkage.BACK_ARM, Math.hypot(tail[0] - BeamLinkage.SADDLE_Z, tail[1] - BeamLinkage.SADDLE_Y), 1e-6, "the beam keeps its length");
			assertTrue(Math.abs(BeamLinkage.beam(crank)) < Math.toRadians(20), "the beam swings within 20 degrees of level");
			assertTrue(tail[0] > BeamLinkage.SADDLE_Z, "the tail stays behind the saddle");
		}
	}
}
