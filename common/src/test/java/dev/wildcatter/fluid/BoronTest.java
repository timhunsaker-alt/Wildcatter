package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 8a: the boron plant (docs/multiblocks/boron.md). */
class BoronTest {
	@Test
	void boraxAndAcidMakeBoricAcid() {
		assertEquals(1.54, Boron.boraxPerAcid(), 0.01);
		assertEquals(0.40, Boron.sulfuricPerAcid(), 0.01);
		assertEquals(0.22, Boron.sulfuricLitresPerAcid(), 0.01);
		assertEquals(0.57, Boron.sulfatePerAcid(), 0.01);
	}

	@Test
	void boricAcidAndCarbonMakeBoronCarbide() {
		assertEquals(4.48, Boron.acidPerB4c(), 0.01);
		assertEquals(2.52, Boron.b2o3PerB4c(), 0.01);
		assertEquals(1.52, Boron.carbonPerB4c(), 0.01);
		assertEquals(3.04, Boron.coPerB4c(), 0.01);
	}

	@Test
	void anRccaHoldsNineKilograms() {
		assertEquals(0.37, Boron.rodletKg(), 0.005);
		assertEquals(9.0, 24 * Boron.rodletKg(), 0.15);
	}

	@Test
	void fifteenHundredPpmIsTenBags() {
		assertEquals(257, Boron.acidForPpm(1500, 30_000), 2);
	}
}
