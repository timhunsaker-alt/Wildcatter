package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 8c: the natural-draft cooling tower (docs/multiblocks/natural-draft-tower.md). */
class CoolingTowerTest {
	@Test
	void theShellIsAHyperbola() {
		assertEquals(30.0, CoolingTower.radius(8), 0.1);
		assertEquals(17.0, CoolingTower.radius(62), 1e-9);
		assertEquals(20.8, CoolingTower.radius(88), 0.1);
		assertEquals(54, CoolingTower.lifts());
	}

	@Test
	void itTakesAbout430ConcreteAnd44Steel() {
		assertEquals(430, CoolingTower.totalConcrete(), 25);
		assertEquals(44, CoolingTower.totalSteelIngots(), 4);
	}

	@Test
	void stullMatchesItsCheckValue() {
		assertEquals(13.7, CoolingTower.wetBulb(20, 50), 0.1);
	}

	@Test
	void theApproachAndTheWater() {
		assertEquals(8.0, CoolingTower.approach(104e6), 1e-9);
		assertEquals(5.28, CoolingTower.approach(52e6), 0.05);
		assertEquals(42.4, CoolingTower.evaporation(104e6), 0.2);
		assertEquals(53.1, CoolingTower.makeUp(104e6), 0.2);
	}
}
