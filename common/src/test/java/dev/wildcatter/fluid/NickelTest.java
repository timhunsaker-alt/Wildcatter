package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** The nickel smelter's and refinery's numbers (docs/multiblocks/nickel-smelter.md, "Size"). */
class NickelTest {
	@Test
	void roasterSendsAboutAFifthOfAnAcidPlantsSulfur() {
		// 2 t/h of feed at 30 % S, 70 % to the acid plant: 0.42 t/h of S
		assertEquals(420, Tuning.NI_ROASTER_FEED_KGH * Nickel.roasterSulfurPerFeed(), 1);
		assertTrue(Nickel.burnerGasPerFeed() > Nickel.roasterSulfurPerFeed());
	}

	@Test
	void furnaceSmeltsTwoTonnesAnHourAtOneMegawatt() {
		assertEquals(2000, Nickel.furnaceKgh(Tuning.NI_FURNACE_WATTS), 1);
	}

	@Test
	void bessemerMatteIsAboutHalfATonneAnHour() {
		double niKgh = Tuning.NI_ROASTER_FEED_KGH * Tuning.NICKEL_FEED_NI;   // 240 kg/h
		assertEquals(480, niKgh * Nickel.bessemerPerNi(), 1);                  // the spec's "about 0.46 t/h"
		assertEquals(0.35, Nickel.converterSulfurPerNi(), 1e-9);              // 84 kg/h of S from the converter
	}

	@Test
	void tankhouseIsSixtyThreeVoltsAndWinsTheLinesNickel() {
		assertEquals(63, Nickel.tankhouseVolts(), 1e-9);
		assertEquals(0.775e6, Nickel.tankhouseWatts(), 0.01e6);
		double kgh = Nickel.cathodeKgs(Tuning.NI_CELL_KA * 1000) * 3600;
		assertEquals(230, kgh, 2);
		assertEquals(228, Nickel.lineNiKgh(), 1);
	}

	@Test
	void anodeChlorineCoversMostOfTheLeach() {
		double back = Nickel.anodeChlorinePerNi() * Tuning.NI_RECOVERY;
		double used = Nickel.leachChlorinePerNi();
		assertEquals(1.208, Nickel.anodeChlorinePerNi(), 0.001);
		assertTrue(back / used > 0.65 && back / used < 0.75, "make-up is the copper's, cobalt's and lost nickel's chlorine: " + back / used);
	}
}
