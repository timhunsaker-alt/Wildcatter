package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 7f: the zirconium plant (docs/multiblocks/zirconium.md). */
class ZirconiumTest {
	@Test
	void aKilogramOfZirconiumTakesTwoOfZirconAndThreeOfChlorine() {
		assertEquals(2.01, Zirconium.zirconPerZr(), 0.01);
		assertEquals(0.58, Zirconium.carbonPerZr(), 0.01);
		assertEquals(3.11, Zirconium.chlorinePerZr(), 0.01);
		assertEquals(1.555, Zirconium.chlorinePerZr() / 2, 0.005);
	}

	@Test
	void theKrollLoopReturnsItsMagnesiumAndChlorine() {
		assertEquals(0.533, Zirconium.magnesiumPerZr(), 0.002);
		assertEquals(2.09, Zirconium.mgCl2PerZr(), 0.01);
		assertEquals(Zirconium.magnesiumPerZr(), Zirconium.mgCl2PerZr() * Zirconium.magnesiumInMgCl2(), 1e-9);
		assertEquals(1.555, Zirconium.mgCl2PerZr() * Zirconium.chlorineInMgCl2(), 0.005);
	}

	@Test
	void theCellsMakeWhatTheLineNeeds() {
		assertEquals(5.7, Zirconium.mgCellKgs() * 3600, 0.05);
		assertEquals(12.3, Zirconium.mgCellWatts() / 1000 / (Zirconium.mgCellKgs() * 3600), 0.2);   // kWh a kg of Mg
		double zrKgh = Tuning.ZR_LINE_KGS * 3600;
		assertTrue(Zirconium.mgCellKgs() * 3600 > zrKgh * Zirconium.magnesiumPerZr(), "the cell keeps up with the Kroll reactor");
		assertEquals(21.3, Zirconium.chlorAlkaliKgs() * 3600, 0.2);
		assertEquals(52_080, Zirconium.chlorAlkaliWatts(), 1);
		assertEquals(2.44, Zirconium.chlorAlkaliWatts() / 1000 / (Zirconium.chlorAlkaliKgs() * 3600), 0.03);   // kWh a kg of Cl2
		assertTrue(Zirconium.chlorAlkaliKgs() * 3600 > zrKgh * Zirconium.chlorinePerZr() / 2, "it makes up the chlorine the SiCl4 takes");
	}

	@Test
	void aCladdingTubeWeighs404Grams() {
		assertEquals(0.404, Zirconium.claddingKg(), 0.002);
		assertEquals(0.40, Zirconium.thimbleKg(), 0.01);
		assertEquals(11.7, Zirconium.hoursPerAssembly(), 0.3);
	}
}
