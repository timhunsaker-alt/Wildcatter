package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** The ammonia plant (docs/multiblocks/ammonia-plant.md): the spec's rates follow from one reformer's hydrogen. */
class AmmoniaPlantTest {
	@Test
	void oneReformerMakesFifteenKilogramsOfAmmoniaAnHour() {
		assertEquals(20.0, AmmoniaPlant.ammoniaNm3h(), 1e-9);
		assertEquals(15.2, AmmoniaPlant.ammoniaKgh(), 0.05);
		assertEquals(0.36, AmmoniaPlant.ammoniaKgh() * 24 / 1000, 0.01);   // t/day
		assertEquals(Tuning.N2_GENERATOR_NM3H, AmmoniaPlant.nitrogenNm3h(), 1e-9, "the generator covers the loop's nitrogen exactly");
		assertEquals(25, AmmoniaPlant.ammoniaKgh() / Tuning.AMMONIA_KG_PER_L, 0.5);   // L/h of liquid
	}

	@Test
	void theSplitBalancesTheAcidAgainstTheAmmonia() {
		assertEquals(0.52, AmmoniaPlant.nitricShare(), 0.01);
		double toNeutraliser = AmmoniaPlant.ammoniaKgh() - AmmoniaPlant.nitricAmmoniaKgh();
		assertEquals(toNeutraliser, AmmoniaPlant.hno3Kgh() * AmmoniaPlant.ammoniaPerHno3(), 1e-9, "the acid neutralises the rest exactly");
		assertEquals(27.3, AmmoniaPlant.hno3Kgh(), 0.1);
		assertEquals(45.4, AmmoniaPlant.acidKgh(), 0.1);
	}

	@Test
	void thePlantMakesThirtyFiveKilogramsOfAnAndFortySixOfMatrix() {
		assertEquals(34.6, AmmoniaPlant.anKgh(), 0.05);
		assertEquals(0.83, AmmoniaPlant.anKgh() * 24 / 1000, 0.01);
		assertEquals(46.1, AmmoniaPlant.matrixKgh(), 0.1);
		assertEquals(1.1, AmmoniaPlant.matrixKgh() * 24 / 1000, 0.01);
		assertEquals(1.0, Tuning.MATRIX_AN + Tuning.MATRIX_WATER + Tuning.MATRIX_DIESEL + Tuning.MATRIX_EMULSIFIER, 1e-9);
		assertTrue(AmmoniaPlant.rockTonnesPerDay() > 2_900 && AmmoniaPlant.rockTonnesPerDay() < 3_100, "about 3,000 t of rock a day: " + AmmoniaPlant.rockTonnesPerDay());
	}

	@Test
	void theWaterBalancesCloseWithWaterToSpare() {
		assertTrue(AmmoniaPlant.processWaterPerHno3() > 0, "the absorption tower needs process water");
		assertEquals(0.35, AmmoniaPlant.processWaterPerHno3(), 0.01);
		assertTrue(AmmoniaPlant.evaporatedPerHno3() > 0, "the neutraliser boils water off");
		assertTrue(AmmoniaPlant.blenderWaterPerMatrix() > 0, "the blender adds a little water");
		// the matrix's water: what the solution brings plus what the blender adds
		double matrix = AmmoniaPlant.matrixKgh();
		double solution = AmmoniaPlant.anSolutionKgh();
		assertEquals(matrix * Tuning.MATRIX_WATER, solution * (1 - Tuning.AN_STRENGTH) + matrix * AmmoniaPlant.blenderWaterPerMatrix(), 1e-9);
		assertEquals(solution, matrix * AmmoniaPlant.solutionPerMatrix(), 1e-9);
	}

	@Test
	void theGauzeAndThePailLastLong() {
		assertEquals(0.0041, AmmoniaPlant.gauzeLossGh(), 0.0002);
		assertTrue(AmmoniaPlant.gauzeHours() > 1_000, "a pack runs for weeks: " + AmmoniaPlant.gauzeHours());
		assertEquals(43, AmmoniaPlant.hoursPerPail(), 1);
	}
}
