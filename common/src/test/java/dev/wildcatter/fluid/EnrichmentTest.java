package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 7e: the centrifuge cascade (docs/multiblocks/enrichment.md). */
class EnrichmentTest {
	@Test
	void aKilogramOfLeuTakesTenKilogramsOfFeedAndEightSwu() {
		assertEquals(10.20, Enrichment.feedPerProduct(), 0.01);
		assertEquals(9.20, Enrichment.tailsPerProduct(), 0.01);
		assertEquals(7.81, Enrichment.swuPerProduct(), 0.02);
	}

	@Test
	void theTc21NeedsElevenStages() {
		assertEquals(11, Enrichment.stages());
	}

	@Test
	void aMachineDoesElevenSwuAnHourOn571Kilowatts() {
		assertEquals(11.4, Enrichment.machineSwuPerSecond() * 3600, 0.05);
		assertEquals(571_000, Enrichment.machineWatts(), 1_000);
		// a converter's dozen machines fit a 400 A feeder breaker at 12.47 kV
		assertTrue(Tuning.CONVERTER_MACHINES * Enrichment.machineWatts() / Tuning.CONVERTER_POWER_FACTOR / (Math.sqrt(3) * 12_470) < 400);
	}

	@Test
	void theMotorsRunAt1225Hertz() {
		assertEquals(1_225, Tuning.CENTRIFUGE_HERTZ, 2);
		assertTrue(Tuning.CENTRIFUGE_HERTZ > 600 && Tuning.CENTRIFUGE_HERTZ < 2_000, "inside the export-control band");
	}

	@Test
	void elevenMachinesFillA30bInAboutFourDays() {
		assertEquals(1_539.6, Enrichment.uPer30b(), 0.5);
		assertEquals(96, Enrichment.hoursPer30b(11), 1.5);
		// and take about 60 % of one conversion plant's uranium
		double feed = 11 * Enrichment.machineSwuPerSecond() / Enrichment.swuPerProduct() * Enrichment.feedPerProduct();
		assertEquals(0.61, feed / Conversion.uraniumKgs(), 0.03);
	}
}
