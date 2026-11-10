package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The residence-time holds (plant-crane-gaps #11, the world rule realResidenceTimes). */
class HoldupTest {
	@Test
	void nothingComesOutBeforeItsTime() {
		Holdup h = new Holdup(1800);
		h.add(0, 18 * 3600, 100);
		assertEquals(0, h.due(18 * 3600 - 1), 1e-9);
		assertEquals(100, h.due(18 * 3600), 1e-9);
		assertEquals(18 * 3600, h.untilNext(0), 1e-9);
	}

	@Test
	void oldestFirstAndPartTakes() {
		Holdup h = new Holdup(60);
		h.add(0, 3600, 10);
		h.add(120, 3600, 5);
		assertEquals(4, h.take(3720, 4), 1e-9);
		assertEquals(11, h.inProcess(), 1e-9);
		assertEquals(11, h.take(3720, 100), 1e-9);
		assertTrue(h.isEmpty());
	}

	@Test
	void steadyFeedGivesTheSameRateAfterTheHold() {
		Holdup h = new Holdup(60);
		double out = 0;
		for (int t = 0; t < 7200; t += 5) {
			h.add(t, 3600, 1);   // 0.2 a second in
			out += h.take(t, 1e9);
		}
		// after the first hour, everything fed in the first hour has come out
		assertEquals(720, out, 13);
		assertEquals(720, h.inProcess(), 13);
	}

	@Test
	void savesAgesAndTurnsOff() {
		Holdup h = new Holdup(1800);
		h.add(0, 36 * 3600, 7);
		Holdup back = new Holdup(1800);
		back.load(h.save());
		assertEquals(7, back.inProcess(), 1e-9);
		back.age(36 * 3600);
		assertEquals(7, back.due(0), 1e-9);
		h.add(0, 36 * 3600, 3);
		assertEquals(10, h.takeAny(1e9), 1e-9);
	}
}
