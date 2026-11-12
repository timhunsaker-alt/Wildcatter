package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 16b2 (docs/multiblocks/conversion-extensions.md, "A full-size plant"): four cell houses in series and the large main line. */
class ConversionLargeTest {
	@Test
	void fourHousesInSeries() {
		int cells = Tuning.F2_HOUSES_IN_SERIES * Tuning.F2_CELLS;
		assertEquals(48, cells);
		assertEquals(432, cells * Tuning.F2_CELL_VOLTS, 1e-9, "432 V DC");
		assertEquals(2.6e6, cells * Tuning.F2_CELL_VOLTS * Tuning.F2_CELL_KA * 1000, 0.05e6, "about 2.6 MW");
		assertEquals(4 * 48.5, Tuning.F2_HOUSES_IN_SERIES * Conversion.fluorineKgs() * 3600, 2, "four houses' fluorine");
	}

	@Test
	void theLargeLineFillsA48yEveryEightHours() {
		double u = Conversion.uraniumKgs() * Tuning.CONVERSION_LARGE_SCALE;
		assertEquals(0.3, u, 0.01, "0.3 kg of U a second");
		assertEquals(8, Conversion.hoursPerCylinder() / Tuning.CONVERSION_LARGE_SCALE, 0.5, "a 48Y about every 8 hours");
		assertEquals(9_400, u * 3600 * 24 * 365 / 1000, 200, "about 9,400 t of U a year");
		assertEquals(Tuning.F2_HOUSES_IN_SERIES * Conversion.fluorineKgs() / Tuning.CONV_F2_PER_U, u, 1e-9, "the four houses' fluorine runs it");
	}
}
