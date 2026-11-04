package dev.wildcatter.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 10c1: what a crane lifts (docs/blueprints.md, "Masses"). */
class MassesTest {
	@Test
	void theHeavyPartsTakeTheirOwnFigure() {
		assertEquals(180, Masses.part("reactor_vessel", 3, 10, 3), 1e-9);
		assertEquals(90, Masses.part("gsu_60", 6, 6, 5), 1e-9);
		assertEquals(60, Masses.part("pot", 6, 6, 12), 1e-9);
	}

	@Test
	void theOthersGoByKind() {
		assertEquals(0.5 * 4 * 4 * 4, Masses.part("unit_auxiliary_transformer", 4, 4, 4), 1e-9);   // a transformer
		assertEquals(0.4 * 2 * 2 * 5, Masses.part("main_feed_pump", 2, 2, 5), 1e-9);               // a rotating machine
		assertEquals(0.3 * 1 * 3 * 1, Masses.part("soft_starter_mv", 1, 3, 1), 1e-9);              // a cabinet
		assertEquals(0.05 * 5 * 20 * 5, Masses.part("coke_quench_tower", 5, 20, 5), 1e-9);         // a structure
		assertEquals(0.15 * 3 * 18 * 3, Masses.part("coker_fractionator", 3, 18, 3), 1e-9);        // a vessel
	}

	@Test
	void theRiggingIsCounted() {
		assertEquals(183.6, Masses.onHook(180), 1e-9);
		assertEquals(1.5, Masses.onHook(1), 1e-9);   // at least half a tonne of hook block
		assertEquals(2.4, Masses.Material.STONE.tonnes, 1e-9);
	}
}
