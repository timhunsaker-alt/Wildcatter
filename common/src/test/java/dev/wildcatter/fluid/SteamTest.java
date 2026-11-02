package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 8a: IAPWS-IF97 saturation and the table enthalpies (docs/multiblocks/reactor.md). */
class SteamTest {
	@Test
	void theSaturationLineMatchesTheTables() {
		assertEquals(0.1014, Steam.psat(100), 0.0005);
		assertEquals(7.63, Steam.psat(291.7), 0.02);     // no-load steam pressure
		assertEquals(344.8, Steam.tsat(15.51), 0.1);     // the RCS's own saturation
		assertEquals(284.9, Steam.tsat(6.9), 0.1);
		assertEquals(99.97, Steam.tsat(0.101325), 0.05);
	}

	@Test
	void theEnthalpiesMatchTheTables() {
		assertEquals(976e3, Steam.hf(227), 2e3);
		assertEquals(1344.8e3, Steam.hf(300), 1e3);
		assertEquals(2773e3, Steam.hg(6.9), 2e3);
		assertEquals(2610.9e3, Steam.hg(15), 1e3);
	}
}
