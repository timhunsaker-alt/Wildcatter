package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Tim's answer 20: a dry cask's limits count an assembly's cooling 100x, as burnup is; its real decay heat is unchanged. */
class CaskCoolingTest {
	private static SpentFuel.Assembly discharged() {
		double[] d = new double[Tuning.DECAY_LAMBDA.length];
		java.util.Arrays.fill(d, 0.001);
		return new SpentFuel.Assembly(45, d, false);
	}

	@Test
	void eligibilityCountsCoolingAHundredTimesOver() {
		SpentFuel.Assembly a = discharged(), b = discharged();
		a.decay(3600);
		b.decay(3600 * Tuning.BURNUP_SPEEDUP);
		assertEquals(b.watts(), a.eligibilityWatts(), b.watts() * 1e-9);
		assertTrue(a.watts() > a.eligibilityWatts());
		assertEquals(3600, a.cooled, 1e-9);
	}

	@Test
	void aJustDischargedAssemblyCountsItsRealHeat() {
		SpentFuel.Assembly a = discharged();
		assertEquals(a.watts(), a.eligibilityWatts(), 1e-9);
	}
}
