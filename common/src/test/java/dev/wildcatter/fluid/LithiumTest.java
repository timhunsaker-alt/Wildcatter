package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 20c (docs/multiblocks/lithium.md): the sulfate route's numbers at 0.5 t of LiOH.H2O an hour. */
class LithiumTest {
	@Test
	void theRatios() {
		assertEquals(2.81, Lithium.liohPerLi2o(), 0.01);
		assertEquals(3.28, Lithium.H2SO4 / Lithium.LI2O, 0.01);
		assertEquals(2.68, Lithium.naohPerLi2o(), 0.01);
		assertEquals(4.75, Lithium.na2so4PerLi2o(), 0.01);
	}

	@Test
	void thePlantAtItsRating() {
		double li2oKgh = Lithium.li2oKgs() * 3600;
		assertEquals(3.3, Lithium.oreKgs() * 3.6, 0.05, "3.3 t/h of ore");
		assertEquals(0.84, Lithium.oreKgs() * Lithium.acidPerOre() * 3.6, 0.03, "0.82 t/h of acid (the spec, on the Li2O recovered: 0.84 on all of it)");
		assertEquals(0.48, li2oKgh * Lithium.naohPerLi2o() / 1000, 0.03, "0.46 t/h of NaOH");
		assertEquals(1.49, li2oKgh * Lithium.naohPerLi2o() / Tuning.CAUSTIC_STRENGTH / 1000, 0.06, "1.44 t/h of 32 % caustic");
		assertEquals(0.85, li2oKgh * Lithium.na2so4PerLi2o() / 1000, 0.04, "0.82 t/h of sodium sulfate");
		assertEquals(0.9, Lithium.decrepitationWatts() / 1e6, 0.03, "0.9 MW of gas");
		assertEquals(3.1, Lithium.oreKgs() * Lithium.residuePerOre() * 3.6, 0.1, "about 3 t/h of residue");
	}

	@Test
	void theCarbonator() {
		assertEquals(1.136, Lithium.liohPerLi2co3(), 0.002);
		assertEquals(0.596, Lithium.co2PerLi2co3(), 0.002);
	}
}
