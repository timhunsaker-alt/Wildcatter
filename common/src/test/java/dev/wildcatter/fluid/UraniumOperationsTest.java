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

	// ---- 16c1: product assay, spin-up and cooling (docs/multiblocks/enrichment-operations.md) ---------------------------------------
	@Test
	void theSpecsAssayTable() {
		double[][] rows = {{0.0495, 0.0025, 10.20, 7.81, 11}, {0.040, 0.0025, 8.13, 5.83, 11}, {0.030, 0.0025, 5.97, 3.81, 10}, {0.0495, 0.0020, 9.30, 8.73, 12}};
		for (double[] r : rows) {
			assertEquals(r[2], Enrichment.feedPerProduct(r[0], r[1]), 0.01);
			assertEquals(r[3], Enrichment.swuPerProduct(r[0], r[1]), 0.02);
			assertEquals((int) r[4], Enrichment.stages(r[0], r[1]));
		}
		assertEquals(Enrichment.swuPerProduct(), Enrichment.swuPerProduct(Tuning.ASSAY_PRODUCT, Tuning.ASSAY_TAILS), 1e-12);
		assertEquals(Enrichment.stages(), Enrichment.stages(Tuning.ASSAY_PRODUCT, Tuning.ASSAY_TAILS));
	}

	@Test
	void lowerAssayFuelStartsLower() {
		assertEquals(Tuning.ASSEMBLY_RHO_FRESH, Enrichment.freshRho(Tuning.ASSAY_PRODUCT), 1e-12);   // 4.95 % fuel as before
		assertEquals(0.082, Enrichment.freshRho(0.030), 1e-9);                                       // 3 %: about half the cycle
	}

	@Test
	void aMachineTakes13Point6KgOfWaterASecond() {
		assertEquals(13.6, Enrichment.machineWatts() / (Tuning.WATER_SPECIFIC_HEAT * Tuning.CASING_RISE_C), 0.2);
	}
}
