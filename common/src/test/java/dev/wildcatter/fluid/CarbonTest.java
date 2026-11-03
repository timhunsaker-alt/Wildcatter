package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 10b3a: the coker, the coke ovens and the anodes (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md). */
class CarbonTest {
	@Test
	void theCokerYieldsAddUp() {
		assertEquals(1.0, Tuning.COKER_COKE_YIELD + Tuning.COKER_GAS_OIL_YIELD + Tuning.COKER_NAPHTHA_YIELD + Tuning.COKER_GAS_YIELD, 1e-9);
		assertEquals(2.06, Tuning.COKER_RESIDUE_KGS / Tuning.RESIDUE_KG_PER_L, 0.01);              // L/s of residue
		assertEquals(25_350, Tuning.COKER_RESIDUE_KGS * 86_400 * Tuning.COKER_COKE_YIELD, 10);       // kg of green coke a day
		assertEquals(2.2e6, Tuning.COKER_RESIDUE_KGS * Tuning.COKER_HEATER_J_PER_KG, 0.05e6);        // the heater's duty, W
	}

	@Test
	void theCalcinedCokeFeedsTheAnodes() {
		assertEquals(19_770, Carbon.calcinedCokePerDay(), 20);
		assertEquals(20_720, Carbon.anodeCokePerDay(), 20);
		// the coker at its rate, with the fifth more it can catch up, covers the anodes
		assertTrue(Carbon.calcinedCokePerDay() * Tuning.CARBON_CATCH_UP > Carbon.anodeCokePerDay());
	}

	@Test
	void theCokeOvensMakeThePitch() {
		assertEquals(0.24, Carbon.netCokeOvenGas(), 1e-9);
		assertEquals(0.667, Tuning.COKE_OVEN_COAL_KGS * Carbon.netCokeOvenGas(), 0.001);            // m3/s of clean gas
		assertEquals(12.0e6, Tuning.COKE_OVEN_COAL_KGS * Carbon.netCokeOvenGas() * Tuning.COKE_OVEN_GAS_JOULES_PER_M3, 0.1e6);
		assertEquals(4_620, Carbon.pitchPerDay(), 5);
		assertEquals(4_780, Carbon.anodePitchPerDay(), 5);
		assertTrue(Carbon.pitchPerDay() * Tuning.CARBON_CATCH_UP > Carbon.anodePitchPerDay());
		assertEquals(112.5, Tuning.MET_COKE_KG, 1e-9);
		assertEquals(3.26e9, Tuning.MET_COKE_JOULES, 0.01e9);
		// tar travels with the raw gas: what the by-product plant takes out is what the coal gave
		assertEquals(Tuning.COKE_OVEN_TAR_PER_KG, Carbon.tarPerRawGas() * Tuning.COKE_OVEN_GAS_PER_KG, 1e-12);
	}

	@Test
	void theAnodeRecipe() {
		assertEquals(1.0, Tuning.PASTE_COKE + Tuning.PASTE_BUTTS + Tuning.PASTE_PITCH, 1e-9);
		assertEquals(829, Carbon.cokePerAnode(), 1);
		assertEquals(255, Carbon.buttsPerAnode(), 1);
		assertEquals(191, Carbon.pitchPerAnode(), 1);
		assertEquals(303, Tuning.BUTT_KG, 1);
		assertEquals(2.91e9, Carbon.bakingJoules(), 0.01e9);
		assertEquals(0.84e6, Carbon.bakingJoules() * Tuning.ANODES_PER_SECOND, 0.01e6);              // the furnace's gas, W
		// a returned butt covers the next anode's butts
		assertTrue(Tuning.BUTT_KG >= Carbon.buttsPerAnode());
	}
}
