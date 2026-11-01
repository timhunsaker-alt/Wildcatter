package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import dev.wildcatter.well.RollFront;
import org.junit.jupiter.api.Test;

/** 7c: roll-front deposits and the ISR wellfield (docs/multiblocks/isr-wellfield.md). */
class IsrTest {
	private static RollFront someFront(long seed) {
		for (int cx = 0; cx < 20; cx++) {
			RollFront front = RollFront.inCell(seed, cx, 0);
			if (front != null) {
				return front;
			}
		}
		return null;
	}

	@Test
	void aFrontDependsOnlyOnTheSeed() {
		RollFront a = someFront(42), b = someFront(42);
		assertNotNull(a);
		assertEquals(a, b);
	}

	@Test
	void theBandIsRichestAlongItsMiddle() {
		RollFront front = someFront(7);
		int[] mid = front.middle();
		assertTrue(front.covers(mid[0], mid[1]));
		assertEquals(Tuning.ROLL_PEAK_GRADE, front.grade(mid[0], mid[1]), 0.001);
		assertTrue(front.topY() <= Tuning.ROLL_MAX_TOP_Y && front.bottomY() >= Tuning.ROLL_MIN_TOP_Y - Tuning.ROLL_MAX_THICKNESS);
		// the arc's centre lies off the band
		assertEquals(0, front.grade((int) Math.floor(front.centerX()), (int) Math.floor(front.centerZ())), 1e-9);
	}

	@Test
	void aFullFiveSpotGivesFiveHundredMilligramsALitre() {
		assertEquals(500, Isr.headGrade(4, 1, Tuning.ROLL_PEAK_GRADE, 1), 1e-9);
		assertEquals(250, Isr.headGrade(2, 1, Tuning.ROLL_PEAK_GRADE, 1), 1e-9);
		assertEquals(500, Isr.headGrade(6, 1, Tuning.ROLL_PEAK_GRADE, 1), 1e-9);   // a fifth injector adds nothing
		assertEquals(0, Isr.headGrade(4, 0, Tuning.ROLL_PEAK_GRADE, 1), 1e-9);     // no oxygen
		assertEquals(0, Isr.headGrade(0, 1, Tuning.ROLL_PEAK_GRADE, 1), 1e-9);
		assertEquals(250, Isr.headGrade(4, 1, Tuning.ROLL_PEAK_GRADE, 0.5), 1e-9);   // half the front won
	}

	@Test
	void aPatternMakesFourAndAHalfKilogramsAnHour() {
		double kgPerHour = Tuning.ISR_PRODUCER_LPS * Tuning.ISR_HEAD_GRADE_MG_L * 1e-6 * 3600;
		assertEquals(4.5, kgPerHour, 0.01);
		double hoursPerDrum = Mill.u3o8PerDrum() / (6 * kgPerHour * Tuning.IX_CAPTURE);
		assertEquals(12, hoursPerDrum, 1.0);
	}

	@Test
	void oneOxygenGeneratorServesAHeaderHouse() {
		double kgPerHour = Tuning.HEADER_HOUSE_PRODUCERS * Tuning.ISR_PRODUCER_LPS * Tuning.O2_DOSE_KG_PER_L * 3600;
		assertTrue(kgPerHour / Tuning.O2_KG_PER_NM3 <= Tuning.O2_GENERATOR_NM3H, kgPerHour / Tuning.O2_KG_PER_NM3 + " Nm3/h");
		assertTrue(Tuning.HEADER_HOUSE_PRODUCERS * Tuning.ISR_PRODUCER_LPS <= Tuning.IX_MAX_LPS);
	}

	@Test
	void aFrontHoldsTonsOfUranium() {
		RollFront front = someFront(99);
		assertTrue(front.recoverableKg() > 20_000, front.recoverableKg() + " kg");
	}
}
