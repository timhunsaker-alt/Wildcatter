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

	@Test
	void aSpanAtTwiceItsRatingPartsInAboutElevenMinutes() {
		// an overhead span: 10 minute time constant, it parts at 3 x its rated rise (docs/multiblocks/poles.md)
		double rise = 1.0;
		double seconds = 0;
		while (rise < dev.wildcatter.Tuning.SPAN_BURN_RISE && seconds < 3600) {
			rise = WireHeat.step(rise, 2.0, 1.0, dev.wildcatter.Tuning.SPAN_HEAT_SECONDS);
			seconds++;
		}
		assertTrue(seconds > 600 && seconds < 720, "twice its rating should part a span in about 11 minutes: " + seconds);
		assertFalse(WireHeat.step(1.0, 1.0, 3600, dev.wildcatter.Tuning.SPAN_HEAT_SECONDS) >= dev.wildcatter.Tuning.SPAN_BURN_RISE, "at its rating it holds");
	}
}
