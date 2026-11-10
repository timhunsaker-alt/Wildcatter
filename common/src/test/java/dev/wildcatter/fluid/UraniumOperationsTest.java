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

	// ---- 16b1: cathode hydrogen, the ash bed, KOH (docs/multiblocks/conversion-extensions.md) ---------------------------------------
	private static double cathodeNm3h() {
		return Conversion.fluorineKgs() * Conversion.cathodeM3PerF2() * 3600;
	}

	@Test
	void theCellsHydrogenCoversTheReducer() {
		assertEquals(28.6, cathodeNm3h(), 0.3);
		double magnesia = Conversion.uraniumKgs() * Conversion.hydrogenM3PerU() * 3600;
		double adu = Conversion.uraniumKgs() * Tuning.CONV_H2_PER_U_ADU / Tuning.H2_KG_PER_M3 * 3600;
		assertEquals(25.4, magnesia, 0.3);
		assertEquals(17.0, adu, 0.3);
	}

	@Test
	void theKohScrubbersBalance() {
		double hfKgh = Conversion.uraniumKgs() * Tuning.HF_EXCESS_PER_U * 3600;
		assertEquals(25.4, hfKgh, 0.3);
		assertEquals(35.6, hfKgh * Tuning.LIME_PER_HF, 0.5);
		assertEquals(49.5, hfKgh * Tuning.CAF2_PER_HF, 0.5);
		assertEquals(28, Tuning.KOH_CHARGE_KG / (hfKgh * Tuning.KOH_PER_HF), 1);   // hours without lime
	}

	@Test
	void anAshDrumHolds12KgOfU() {
		assertEquals(12, Tuning.ASH_DRUM_KG * Tuning.ASH_U_SHARE, 0.01);
	}
}
