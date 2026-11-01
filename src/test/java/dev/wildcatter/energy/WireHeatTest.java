package dev.wildcatter.energy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WireHeatTest {
	@Test
	void aMotorStartOnItsCablePasses() {
		double rise = WireHeat.step(1.0, 6.0, 2.0);   // running at its rating, then a six-times start for two seconds
		assertFalse(WireHeat.burnt(rise), "a start should not burn its cable: " + rise);
	}

	@Test
	void aDeadShortBurnsAtOnceAndTwiceTheRatingInAboutHalfAMinute() {
		assertTrue(WireHeat.secondsToBurn(20) < 1, "a short burns in under a second");
		double twice = WireHeat.secondsToBurn(2);
		assertTrue(twice > 20 && twice < 120, "twice the rating: " + twice);
		assertTrue(WireHeat.secondsToBurn(3) < 30, "three times the rating: " + WireHeat.secondsToBurn(3));
	}

	@Test
	void atItsRatingItNeverBurns() {
		double rise = 0;
		for (int i = 0; i < 1000; i++) {
			rise = WireHeat.step(rise, 1.0, 10);
		}
		assertFalse(WireHeat.burnt(rise));
		assertTrue(Double.isInfinite(WireHeat.secondsToBurn(1.2)), "a fifth over its rating runs hot but holds");
	}
}
