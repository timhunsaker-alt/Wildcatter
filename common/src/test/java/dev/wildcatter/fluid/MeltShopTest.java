package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.MeltShop.Charge;
import dev.wildcatter.fluid.MeltShop.Grade;
import org.junit.jupiter.api.Test;

/** The alloy melt shop's numbers (docs/multiblocks/nickel-smelter.md, 17b2). */
class MeltShopTest {
	@Test
	void everyGradesChargeIsATonne() {
		for (Grade g : Grade.values()) {
			double sum = 0;
			for (Charge c : Charge.values()) {
				sum += g.kgPerTonne(c);
			}
			assertEquals(1000, sum, 1e-9, g.label);
		}
	}

	@Test
	void chemistryFromTheCharge() {
		// HC ferrochrome 65 % Cr, ferromolybdenum 65 % Mo (ferroalloys.md)
		assertEquals(0.182, Grade.S304.kgPerTonne(Charge.FERROCHROME) * 0.65 / 1000, 0.001);
		assertEquals(0.08, Grade.S304.kgPerTonne(Charge.NICKEL) / 1000, 1e-9);
		assertEquals(0.12, Grade.S316.kgPerTonne(Charge.NICKEL) / 1000, 1e-9);
		assertEquals(0.026, Grade.S316.kgPerTonne(Charge.FERROMOLYBDENUM) * 0.65 / 1000, 0.001);
		assertEquals(0.156, Grade.NICR.kgPerTonne(Charge.FERROCHROME) * 0.65 / 1000, 0.001);
		assertEquals(0.72, Grade.NICR.kgPerTonne(Charge.NICKEL) / 1000, 1e-9);
	}

	@Test
	void inductionFurnaceMeltsTwoAndAHalfTonnesAnHour() {
		assertEquals(2.5, MeltShop.meltTph(), 1e-9);
		assertEquals(1.2e6 * 3600, MeltShop.heatJoules(), 1);   // 1.2 MWh a heat
	}

	@Test
	void theAodBlowsTheCarbonOut() {
		assertEquals(42.0, Grade.S304.heatCarbon(), 1e-9);    // 560 kg of FeCr and 40 of FeMn at 7 % C: 2.1 %
		double o2 = MeltShop.heatO2(Grade.S304);
		assertTrue(o2 > 55 && o2 < 70, "about 64 Nm3 of oxygen a 304 heat: " + o2);
		double left = Grade.S304.heatCarbon() - o2 * MeltShop.carbonPerO2();
		assertEquals(Tuning.INDUCTION_HEAT_KG * Tuning.AOD_FINAL_CARBON, left, 1e-6);
		assertTrue(left / Tuning.INDUCTION_HEAT_KG < 0.0008, "under 304's 0.08 % carbon");
		assertEquals(20, MeltShop.ingotsPerHeat());
	}
}
