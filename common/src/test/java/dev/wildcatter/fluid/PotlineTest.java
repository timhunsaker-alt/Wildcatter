package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 10b3b: the potline's balance for 40 pots (docs/multiblocks/potline.md). */
class PotlineTest {
	private static final double AL_KGS = Potline.aluminiumKgs(Tuning.POT_AMPS, Tuning.POT_CURRENT_EFFICIENCY);

	@Test
	void aPotMakesItsMetal() {
		assertEquals(56.78, AL_KGS * 3600, 0.01);                                     // kg/h
		assertEquals(54_510, AL_KGS * 86_400 * 40, 10);                               // 40 pots, a day
		assertEquals(109.6, AL_KGS * 3600 * Tuning.POT_ALUMINA_PER_AL, 0.1);          // alumina, kg/h
		assertEquals(105_200, AL_KGS * 86_400 * 40 * Tuning.POT_ALUMINA_PER_AL, 100); // matches 10b2's 105 t a day
		assertEquals(1.14, AL_KGS * 3600 * Potline.alf3PerAl(true), 0.01);
		assertEquals(2.56, AL_KGS * 3600 * Potline.alf3PerAl(false), 0.01);
		assertEquals(22_350, AL_KGS * 86_400 * 40 * Tuning.POT_CARBON_PER_AL, 10);   // net carbon, 40 pots, a day
		// two days of metal over its pad
		assertEquals(2.0, Tuning.POT_METAL_HOLD_KG / (AL_KGS * 86_400), 0.01);
	}

	@Test
	void theAnodesLast29Days() {
		assertEquals(909.75, Potline.carbonPerAnode(), 1e-9);
		assertEquals(29.3, Potline.anodeLifeDays(), 0.1);
		// 40 pots change about 25 a day, what the anode plant makes
		assertEquals(Tuning.ANODES_PER_SECOND * 86_400, 40 * Tuning.POT_ANODES / Potline.anodeLifeDays(), 0.5);
	}

	@Test
	void theCurrentEfficiencyFalls() {
		assertEquals(0.94, Potline.currentEfficiency(0, false), 1e-12);
		assertEquals(0.90, Potline.currentEfficiency(2, false), 1e-12);
		assertEquals(0.90, Potline.currentEfficiency(0, true), 1e-12);
		assertEquals(0.54, Potline.currentEfficiency(99, true), 1e-12);   // at most 18 overdue
	}

	@Test
	void theVoltagesFitTheRectiformers() {
		assertEquals(4.3, Potline.potVolts(), 1e-9);
		assertEquals(27.65, Potline.anodeEffectBackEmf(), 1e-9);
		double line = 40 * Potline.potVolts() + (Tuning.POT_AE_VOLTS - Tuning.POT_CELL_VOLTS);
		assertTrue(line < Tuning.RECTIFORMER_MAX_DC_VOLTS, "40 pots and an anode effect: " + line);
		assertEquals(0.195, Tuning.POT_AE_CF4_KG_PER_MIN, 0.001);
		// 40 pots at 4.3 V: about 31 MW of DC, 33.6 MVA on the AC side through 98.5 % at PF 0.95
		double dc = 40 * Potline.potVolts() * Tuning.POT_AMPS;
		assertEquals(31.0e6, dc, 0.1e6);
		assertEquals(33.1e6, dc / Tuning.RECTIFORMER_EFFICIENCY / Tuning.RECTIFIER_POWER_FACTOR, 0.1e6);
	}

	@Test
	void theUnitsShareTheLine() {
		assertEquals(45_000, Potline.unitShare(180_000, 4), 1e-9);
		assertEquals(60_000, Potline.unitShare(180_000, 3), 1e-9);
		assertEquals(60_000, Potline.unitShare(180_000, 2), 1e-9);   // the line falls to 120 kA
		assertEquals(0, Potline.unitShare(180_000, 0), 1e-9);
	}

	@Test
	void theFumeTreatmentCentreKeepsUp() {
		// 40 pots' gas on two fans; a reacted bag for each bag's worth of the alumina the pots use
		assertEquals(40 * Tuning.POT_GAS_M3H, Tuning.FTC_FANS * Tuning.FTC_FAN_M3H, 1e-9);
		assertEquals(45_620, Tuning.FTC_GAS_PER_BAG, 10);
		assertEquals(4.38, 40 * Tuning.POT_GAS_M3H / Tuning.FTC_GAS_PER_BAG, 0.01);   // bags an hour
		assertEquals(352, Math.floor(Tuning.CRUCIBLE_KG / Tuning.AL_INGOT_KG), 1e-9);  // ingots a crucible
	}
}
