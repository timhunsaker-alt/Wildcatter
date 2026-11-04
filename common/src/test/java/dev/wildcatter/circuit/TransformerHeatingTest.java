package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 10d1: the IEEE C57.109 short-time rule for rated transformers (docs/ELECTRICAL_MODEL.md, "Transformers"). */
class TransformerHeatingTest {
	@Test
	void itLastsAsLongAsItsThroughFaultCurveAllows() {
		assertEquals(1000, TransformerHeating.secondsToFail(1.5, 0), 1e-9);    // 150 %: about 17 minutes
		assertEquals(1250.0 / 3, TransformerHeating.secondsToFail(2.0, 0), 1e-9);
		assertEquals(1250.0 / 35, TransformerHeating.secondsToFail(6.0, 0), 1e-9);   // a motor's inrush: 36 s
		assertEquals(1250.0 / 624, TransformerHeating.secondsToFail(25.0, 0), 1e-9); // 25 x: 2 s
		assertEquals(Double.POSITIVE_INFINITY, TransformerHeating.secondsToFail(1.0, 0));
	}

	@Test
	void itFailsWhenItsCurveIsUsedUpAndCoolsBelowRating() {
		TransformerHeating h = new TransformerHeating();
		for (int i = 0; i < 39; i++) {
			assertFalse(h.step(25.0, 0.05), "not yet at " + i * 0.05 + " s");   // 25 x fails just after 2 s
		}
		assertTrue(h.step(25.0, 0.1));
		TransformerHeating starts = new TransformerHeating();
		assertFalse(starts.step(6.0, 10));                                         // a 10 s start at 6 x uses 28 % of it
		assertEquals(350.0 / 1250, starts.level(), 1e-9);
		starts.step(0.8, 100);                                                     // below rating it cools 1/600 of its limit a second
		assertEquals(350.0 / 1250 - 100.0 / 600, starts.level(), 1e-9);
		starts.step(0.8, 300);
		assertEquals(0, starts.level(), 1e-9);
	}
}
