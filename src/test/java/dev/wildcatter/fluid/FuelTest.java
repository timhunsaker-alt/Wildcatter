package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 7g: the fuel plant (docs/multiblocks/fuel.md). */
class FuelTest {
	@Test
	void aPelletIsFiveGramsAndARodHolds372() {
		assertEquals(5.31, Fuel.pelletGrams(), 0.01);
		assertEquals(372, Fuel.pelletsPerRod());
		assertEquals(1.741, Fuel.rodU(), 0.005);
	}

	@Test
	void anAssemblyHolds460KilogramsOfUranium() {
		assertEquals(459.6, Fuel.assemblyU(), 1.0);
		assertEquals(667, Fuel.assemblyKg(), 5);   // textbook about 665
	}

	@Test
	void theKilnTakesHydrogenAndSteamAndGivesHf() {
		assertEquals(0.141, Fuel.hydrogenPerU(), 0.002);
		assertEquals(0.45, Fuel.waterPerU(), 0.01);
		assertEquals(0.504, Fuel.hfPerU(), 0.002);
	}

	@Test
	void theLineRunsAt3PelletsASecond() {
		assertEquals(3.0, Fuel.pelletsPerSecond(), 0.1);
		assertEquals(31, Fuel.hoursPer30b(), 0.5);
		assertEquals(9.2, Fuel.hoursPerAssembly(), 0.1);
		assertEquals(21_250, Fuel.rodsPerCylinder(), 1);
		assertEquals(3.3, Tuning.CYLINDER_30B_KG / Tuning.UF6_PER_U / Fuel.assemblyU(), 0.05);
	}
}
