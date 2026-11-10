package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import dev.wildcatter.ore.Ore;
import org.junit.jupiter.api.Test;

/** Phase 16's numbers (docs/ideas-plan.md): uranium routes and plant operations. */
class UraniumOperationsTest {
	// ---- 16a: ADU yellowcake (docs/multiblocks/adu-yellowcake.md) ------------------------------------------------------------------
	@Test
	void u3o8TakesTwoThirdsOfAnH2AU() {
		// U3O8 + 2 H2 -> 3 UO2: two thirds of a mole of H2 a mole of U
		double derived = 2.0 / 3 * 2.016 / 238.03;
		assertEquals(derived, Tuning.CONV_H2_PER_U_ADU, 0.01e-3);
	}

	@Test
	void anAduDrumHolds326KgOfU() {
		assertEquals(326, Tuning.DRUM_NET_KG * Tuning.ADU_U3O8 * Ore.U_IN_U3O8, 1);
		assertEquals(271, Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8 * Ore.U_IN_U3O8, 1);
	}

	@Test
	void oneAmmoniaPlantFeedsAbout76KgOfU3o8AnHour() {
		assertEquals(76, AmmoniaPlant.ammoniaKgh() / Tuning.ADU_NH3_PER_U3O8, 2);
	}
}
