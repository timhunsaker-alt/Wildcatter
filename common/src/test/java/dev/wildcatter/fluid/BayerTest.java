package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 10b2: the Bayer refinery, lime kiln and AlF3 reactor (docs/multiblocks/bayer-refinery.md, alumina-plant.md). */
class BayerTest {
	@Test
	void aBauxiteBlockGivesATonneOfAlumina() {
		assertEquals(1021, Bayer.aluminaPerBauxiteBlock(), 1);
		assertEquals(2.057, Bayer.bauxite(), 0.002);
		assertEquals(14, Bayer.aluminaPerBauxiteBlock() / Tuning.BAYER_ALUMINA_KGS / 60, 0.1);   // minutes a block at the full rate
		assertEquals(1.530, Bayer.hydrate(), 0.001);
	}

	@Test
	void theStreamsMatchTheSpec() {
		assertEquals(16.25, Bayer.strongLiquor(), 0.01);
		assertEquals(18.31, Bayer.bauxiteSlurry(), 0.01);
		assertEquals(0.526, Bayer.mud(), 0.002);
		assertEquals(1.504, Bayer.mudSlurry(), 0.003);
		assertEquals(16.80, Bayer.greenLiquor(), 0.01);
		assertEquals(18.80, Bayer.polishedLiquor(), 0.01);
		assertEquals(17.27, Bayer.spentLiquor(), 0.01);
		assertEquals(0.1875, Bayer.caustic(), 1e-4);
		assertEquals(1.207, Bayer.evaporated(), 0.003);
		assertEquals(1.98, Bayer.steam(), 0.01);
		assertEquals(0.752, Bayer.cake(), 0.002);
		assertEquals(2394, Bayer.aluminaPerRedMudBlock(), 5);
	}

	@Test
	void theLoopBalances() {
		// the liquor the mill takes comes back: spent liquor plus make-up, less what the evaporator boils off
		assertEquals(Bayer.strongLiquor(), Bayer.spentLiquor() + Bayer.caustic() - Bayer.evaporated(), 1e-9);
		// what comes in leaves: bauxite, wash water and caustic as hydrate, wet mud and the water boiled off
		double in = Bayer.bauxite() + Bayer.washWater() + Bayer.caustic();
		double out = Bayer.hydrate() + Bayer.mudSlurry() + Bayer.evaporated();
		assertEquals(in, out, 1e-9);
	}

	@Test
	void theFullRateNeedsAboutElevenChlorAlkaliCells() {
		double causticKgh = Bayer.caustic() * Tuning.BAYER_ALUMINA_KGS * 3600;
		assertEquals(820, causticKgh, 5);
		double perCell = Zirconium.chlorAlkaliKgs() * 3600 * Bayer.causticPerCl2();
		assertEquals(75, perCell, 1);
		assertEquals(11, Math.ceil(causticKgh / perCell), 0);
		assertEquals(2.4, Bayer.steam() * Tuning.BAYER_ALUMINA_KGS, 0.05);
		assertTrue(Tuning.PROCESS_BOILER_KGS > Bayer.steam() * Tuning.BAYER_ALUMINA_KGS, "one boiler carries the plant");
		assertEquals(2.75e6, Bayer.boilerJoulesPerSteam(), 0.01e6);
	}

	@Test
	void limeAndAlf3() {
		assertEquals(1383, Bayer.limePerLimestoneBlock(), 2);
		assertTrue(Tuning.LIME_KILN_KGS > Bayer.lime() * Tuning.BAYER_ALUMINA_KGS * 2, "the kiln covers the refinery twice over");
		assertEquals(0.715, Tuning.HF_PER_ALF3, 0.001);
		assertEquals(0.929, Tuning.HYDRATE_PER_ALF3, 0.001);
		assertEquals(0.644, Bayer.waterPerAlf3(), 0.002);
		assertTrue(Tuning.ALF3_KGS * Tuning.HF_PER_ALF3 < Tuning.HF_KILN_KGS, "the HF kiln can feed it");
	}
}
