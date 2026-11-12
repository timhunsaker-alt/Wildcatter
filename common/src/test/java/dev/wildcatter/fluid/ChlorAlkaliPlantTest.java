package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 20a (docs/multiblocks/chlor-alkali-plant.md): the spec's numbers follow from Faraday and the zirconium spec's ratios. */
class ChlorAlkaliPlantTest {
	@Test
	void theCellRoomMakesATonneAnHourAt161Volts() {
		assertEquals(1.27, ChlorAlkaliPlant.cl2PerKah(), 0.005, "1.27 kg of chlorine a kA·h at 96 %");
		assertEquals(1000, ChlorAlkaliPlant.cl2Kgs() * 3600, 15, "about a tonne an hour");
		assertEquals(161, ChlorAlkaliPlant.dcVolts(), 0.5);
		assertEquals(2.4e6, ChlorAlkaliPlant.dcWatts(), 0.05e6);
		assertEquals(2.4, ChlorAlkaliPlant.kwhPerCl2(), 0.1, "2.4 kWh a kg");
	}

	@Test
	void itsSaltCausticHydrogenAndWater() {
		double kgh = ChlorAlkaliPlant.cl2Kgs() * 3600;
		assertEquals(1.65, kgh * ChlorAlkaliPlant.saltPerCl2() / 1000, 0.03, "1.65 t/h of salt");
		assertEquals(3.5, kgh * ChlorAlkaliPlant.caustic32PerCl2() / 1000, 0.06, "3.5 t/h of 32 % caustic");
		assertEquals(2.3, kgh * ChlorAlkaliPlant.caustic32PerCl2() * ChlorAlkaliPlant.caustic50Per32() / 1000, 0.08, "or 2.3 t/h of 50 %");
		assertEquals(315, kgh * ChlorAlkaliPlant.hydrogenM3PerCl2(), 6, "315 Nm3/h of hydrogen");
		assertEquals(3.0, kgh * Tuning.CA_WATER_PER_CL2 / 1000, 0.05, "3 m3/h of water");
		assertEquals(15, Tuning.DRYING_ACID_PER_CL2 * 1000, 1e-9, "15 kg of acid a tonne");
		assertEquals(77, 2117 / (kgh * ChlorAlkaliPlant.saltPerCl2()) * 60, 2, "a rock salt block every 77 minutes");
	}

	@Test
	void theHclBurnerAndTheCreep() {
		assertEquals(0.972, ChlorAlkaliPlant.cl2PerHcl(), 0.001);
		assertEquals(2.125, ChlorAlkaliPlant.waterPerHcl(), 1e-9);
		assertEquals(0.3075, ChlorAlkaliPlant.h2M3PerHcl(), 0.002);
		assertEquals(0.1, ChlorAlkaliPlant.creep(86_400), 1e-9, "0.1 V an element a day");
		assertEquals(Tuning.CAP_CREEP_MAX_VOLTS, ChlorAlkaliPlant.creep(1e9), 1e-9);
		assertEquals(3.85, ChlorAlkaliPlant.brinePerSalt(), 0.01, "26 % brine");
	}
}
