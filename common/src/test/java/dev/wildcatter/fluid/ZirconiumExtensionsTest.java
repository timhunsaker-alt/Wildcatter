package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.TimeScale;
import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 17c's numbers (docs/multiblocks/zirconium-extensions.md). */
class ZirconiumExtensionsTest {
	// ---- 17c1: batches, scrap and hafnium --------------------------------------------------------------------------------------
	@Test
	void eightyFourPercentOfTheMetalReachesATube() {
		assertEquals(0.92, Zirconium.extrusionYield(), 1e-9);
		assertEquals(0.84, Zirconium.lineYield(), 1e-9);
		assertEquals(8.4, Zirconium.tubeKgh(10, false), 1e-9);
		assertEquals(10.0, Zirconium.tubeKgh(10, true), 1e-9);
	}

	@Test
	void twoRetortsKeepTheLineAtTenKgAnHour() {
		// one retort reduces while the other distils: a 50 kg batch every 5 hours
		assertEquals(10.0, Tuning.KROLL_BATCH_KG / (Tuning.KROLL_REDUCTION_S / 3600), 1e-9);
		// the distillation's 4 hours fit inside the next retort's 5
		assertEquals(true, Tuning.KROLL_DISTILLATION_S < Tuning.KROLL_REDUCTION_S);
		// a 100 kg ingot of sponge every 10 hours, melted for 2
		assertEquals(10.0, Tuning.VAR_INGOT_KG / (Tuning.ZR_LINE_KGS * 3600), 1e-9);
	}

	@Test
	void batchWaitsFollowTheTimeScale() {
		int before = TimeScale.scale();
		try {
			TimeScale.set(50);
			assertEquals(360, TimeScale.scaled(Tuning.KROLL_REDUCTION_S), 1e-9);
			assertEquals(72, TimeScale.scaled(Tuning.VAR_MELT_S), 1e-9);
		} finally {
			TimeScale.set(before);
		}
	}

	@Test
	void aHafniumRodletWeighsAbout3_6Kg() {
		assertEquals(3.6, Zirconium.hafniumRodletKg(), 0.05);
	}

	@Test
	void theLinesHafniumMakesFiveRccasInAbout2100Hours() {
		assertEquals(0.2, Zirconium.hafniumKgh(), 0.01);
		assertEquals(2100, Zirconium.hoursForHafniumRccas(5), 50);
		// a HfCl4 drum's 50 kg is about 245 hours of the line
		assertEquals(245, Tuning.HFCL4_DRUM_HF_KG / Zirconium.hafniumKgh(), 2);
	}

	@Test
	void hafniumTakesLessMagnesiumThanZirconium() {
		assertEquals(0.272, Zirconium.magnesiumPerHf(), 0.001);
		assertEquals(1.067, Zirconium.mgCl2PerHf(), 0.001);
	}
}
