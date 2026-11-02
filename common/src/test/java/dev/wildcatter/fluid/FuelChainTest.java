package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** The 7b acid plant and uranium mill (docs/multiblocks/acid-plant.md, uranium-mill.md). */
class FuelChainTest {
	@Test
	void aTonneOfAcidTakesAThirdOfATonneOfSulfur() {
		assertEquals(0.327, AcidPlant.S / AcidPlant.H2SO4, 0.001);
		assertEquals(0.57, AcidPlant.ratedSulfurKgs(), 0.01);
	}

	@Test
	void theBlowerMovesAbout13000Nm3AnHour() {
		double nm3h = AcidPlant.ratedSulfurKgs() * AcidPlant.airM3PerSulfur() * 3600;
		assertEquals(13_000, nm3h, 600);
		assertEquals(2_100, AcidPlant.ratedSulfurKgs() * AcidPlant.airM3PerSulfur() / AcidPlant.ratedAcidKgs() * 1000, 100);   // Nm3 a tonne of acid
	}

	@Test
	void theConvertedSulfurAllEndsUpInTheAcid() {
		double sulfur = 1.0;
		double so2 = sulfur * AcidPlant.gasPerSulfur() * AcidPlant.so2InGas();
		assertEquals(AcidPlant.SO2 / AcidPlant.S, so2, 1e-9);
		double acid = so2 * AcidPlant.conversion(true) * AcidPlant.acidPerSo2();
		assertEquals(AcidPlant.H2SO4 / AcidPlant.S * Tuning.ACID_CONVERSION_DOUBLE, acid, 1e-9);
		assertEquals(0.196, AcidPlant.waterPerProduct(), 0.002);
	}

	@Test
	void singleAbsorptionPutsTwentyFiveTimesTheSo2UpTheStack() {
		double ratio = (1 - AcidPlant.conversion(false)) / (1 - AcidPlant.conversion(true));
		assertTrue(ratio >= 10, "ratio " + ratio);
	}

	@Test
	void theAcidPlantCoversTheLeach() {
		double leach = Tuning.MILL_ORE_KGS * Tuning.LEACH_ACID_KG_PER_T / 1000;
		assertTrue(AcidPlant.ratedAcidKgs() > leach, "plant " + AcidPlant.ratedAcidKgs() + " leach " + leach);
	}

	@Test
	void pitchblendeIsHeldToTheSxLimit() {
		assertEquals(Tuning.MILL_ORE_KGS, Mill.oreKgs(0.02), 1e-9);
		assertEquals(Tuning.MILL_U3O8_KGS / 0.60, Mill.oreKgs(0.60), 1e-9);
		assertTrue(Mill.oreKgs(0.60) * 0.60 <= Tuning.MILL_U3O8_KGS + 1e-9);
	}

	@Test
	void aboutSevenAndAHalfBlocksOfOreFillADrum() {
		double perBlock = 2300 * 0.02 * Tuning.MILL_RECOVERY;
		assertEquals(43, perBlock, 0.5);
		assertEquals(7.5, Mill.u3o8PerDrum() / perBlock, 0.1);
		assertEquals(320, Mill.u3o8PerDrum(), 1e-9);
	}

	@Test
	void theMillFillsAboutFourDrumsAnHour() {
		double u3o8 = Tuning.MILL_ORE_KGS * 0.02 * Tuning.MILL_RECOVERY;
		assertEquals(4.4, u3o8 * 3600 / Mill.u3o8PerDrum(), 0.1);
	}

	@Test
	void stripLiquorAndSalt() {
		assertEquals(40, Mill.stripPerU3o8(), 1e-9);   // litres a kg at 25 g/L
		double saltKgs = Tuning.MILL_U3O8_KGS * Tuning.MILL_RECOVERY * Mill.saltPerU3o8();
		assertEquals(0.14, saltKgs, 0.01);
	}

	@Test
	void theThickenerWashesWithTwoAndAHalfTimesTheSolids() {
		double perSecond = Tuning.MILL_ORE_KGS * Mill.washPerSolids();
		assertEquals(52, perSecond, 0.5);
		assertTrue(Mill.liquorPerSlurry(Tuning.MILL_SLURRY_SOLIDS) > 0);
	}
}
