package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 7d: conversion to UF6 (docs/multiblocks/conversion.md). */
class ConversionTest {
	@Test
	void twelveCellsMakeFortyEightKilogramsOfFluorineAnHour() {
		assertEquals(48.5, Conversion.fluorineKgs() * 3600, 0.1);
		assertEquals(0.075, Conversion.uraniumKgs(), 0.001);
	}

	@Test
	void theCellsRunAt108VoltsAnd650Kilowatts() {
		assertEquals(108, Conversion.dcVolts(), 1e-9);
		assertEquals(648_000, Conversion.dcWatts(), 1e-6);
		assertEquals(668_000, Conversion.acWatts(), 1_000);
		assertEquals(13.4, Conversion.dcWatts() / 1000 / (Conversion.fluorineKgs() * 3600), 0.1);   // kWh a kg of F2
		assertTrue(Conversion.acWatts() / Tuning.RECTIFIER_POWER_FACTOR < Tuning.UNIT_SUBSTATION_VA, "a 1 MVA unit substation carries it");
	}

	@Test
	void theReagentsFollowTheStoichiometry() {
		assertEquals(0.094, Conversion.hydrogenM3PerU(), 0.001);
		assertEquals(0.70, Conversion.hfPerU(), 0.01);
		assertEquals(352.0 / 238.03, Tuning.UF6_PER_U, 0.001);
		// the reformer covers the reducer, the kiln the hydrofluorinator and the cells
		assertTrue(Tuning.SMR_M3H / 3600 > Conversion.uraniumKgs() * Conversion.hydrogenM3PerU());
		assertTrue(Tuning.HF_KILN_KGS > Conversion.uraniumKgs() * Conversion.hfPerU());
	}

	@Test
	void aCylinderTakesAboutThirtyOneHours() {
		assertEquals(31, Conversion.hoursPerCylinder(), 1);
		assertEquals(8_453, Tuning.CYLINDER_48Y_KG / Tuning.UF6_PER_U, 5);
	}

	@Test
	void aDrumLastsAnHour() {
		double uPerDrum = Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8 * dev.wildcatter.ore.Ore.U_IN_U3O8;
		assertEquals(1.0, uPerDrum / Conversion.uraniumKgs() / 3600, 0.05);
	}
}
