package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 20b (docs/multiblocks/titanium-pigment.md): the spec's sizes follow from the chloride process's ratios. */
class TitaniumPigmentTest {
	@Test
	void theChlorineLoop() {
		double tph = TitaniumPigment.tio2Kgs() * 3.6;
		assertEquals(0.25, tph, 1e-9);
		assertEquals(0.59, tph * TitaniumPigment.ticl4PerTio2(), 0.01, "0.59 t/h of TiCl4");
		assertEquals(0.44, tph * TitaniumPigment.cl2BackPerTio2(), 0.01, "0.44 t/h of chlorine back");
		assertEquals(0.1, tph * TitaniumPigment.cl2MakeupPerTio2(), 1e-9, "0.1 t/h of make-up");
		assertEquals(93, tph * 1000 * TitaniumPigment.cokePerTio2(), 1, "93 kg/h of coke");
		assertEquals(88, tph * 1000 * TitaniumPigment.o2M3PerTio2(), 1, "88 Nm3/h of oxygen");
		assertEquals(280, 1000 * TitaniumPigment.o2M3PerTio2Ideal(), 2, "the stoichiometric 280 Nm3 a tonne");
	}

	@Test
	void theSlagSmelter() {
		Ferroalloys.Recipe ti = Ferroalloys.Recipe.TI_SLAG;
		assertEquals(2.2, ti.mwh, 1e-9);
		assertEquals(1700, ti.kgPerTonne(Ferroalloys.Charge.ILMENITE), 1, "1.7 t of ilmenite a tonne of slag");
		assertEquals(0.31, 0.18 * ti.kgPerTonne(Ferroalloys.Charge.ILMENITE) / 1000, 0.01, "0.31 t/h of ilmenite for 0.18 t/h of slag");
		assertEquals(0.12, 0.18 * Ferroalloys.pigIronPerSlag(), 0.005, "0.12 t/h of pig iron");
		assertEquals(0.4, 0.18 * ti.mwh, 0.01, "about 0.4 MW for the slag");
		assertEquals(0.0, Ferroalloys.Recipe.FECR.kgPerTonne(Ferroalloys.Charge.ILMENITE), 1e-9, "the 17a recipes take no ilmenite");
		assertEquals(0.6 / Tuning.TI_SLAG_TIO2 + 0.4 / Tuning.RUTILE_TIO2, 0.6 * TitaniumPigment.feedPerTio2(0.85) + 0.4 * TitaniumPigment.feedPerTio2(0.95), 1e-9);
	}
}
