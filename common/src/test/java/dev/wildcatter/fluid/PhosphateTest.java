package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 18 (docs/multiblocks/fertilizer.md): the specs' numbers follow from the sizes. */
class PhosphateTest {
	@Test
	void theReactorAttacksSeventyTonnesOfRockADay() {
		assertEquals(0.296, Phosphate.rockP2o5(), 0.001);
		assertEquals(0.82, Phosphate.reactorRockKgs(), 0.01);
		assertEquals(70, Phosphate.reactorRockKgs() * 86.4, 2);
		assertEquals(54, Phosphate.sulfuricLps() * Tuning.ACID_KG_PER_L * 86.4, 0.5, "2.7 t of acid a tonne of P2O5");
		assertEquals(90, Phosphate.reactorRockKgs() * Phosphate.gypsumPerRock() * 86.4, 0.5, "4.5 t of gypsum a tonne of P2O5");
		assertEquals(0.82, Phosphate.filterAcidLps() * Tuning.FILTER_ACID_KG_PER_L, 0.01, "the spec's 0.82 kg/s of filter acid");
		assertEquals(Tuning.P2O5_TPD, Phosphate.reactorRockKgs() * Phosphate.filterAcidPerRock() * Tuning.FILTER_ACID_P2O5 * 86.4, 1e-9);
	}

	@Test
	void theEvaporatorMakesFiftyFourPercentAcid() {
		double filterKgs = Phosphate.filterAcidLps() * Tuning.FILTER_ACID_KG_PER_L;
		assertEquals(0.43, filterKgs * Phosphate.merchantPerFilterAcid(), 0.01, "the spec's 0.43 kg/s of merchant acid");
		assertEquals(Phosphate.merchantLps() * Tuning.MERCHANT_ACID_KG_PER_L, filterKgs * Phosphate.merchantPerFilterAcid(), 1e-9);
		assertEquals(40, filterKgs * Phosphate.steamPerFilterAcid() * 86.4, 0.5, "2 t of steam a tonne of P2O5");
		assertTrue(filterKgs * Phosphate.steamPerFilterAcid() < Tuning.PROCESS_BOILER_KGS, "one package boiler carries it");
	}

	@Test
	void tspIsTheMainProductUntilThereIsMoreAmmonia() {
		assertEquals(2.6, Phosphate.tspKgs() * 3.6, 0.05, "t/h of TSP from all the acid");
		assertEquals(Phosphate.p2o5Kgs(), Phosphate.tspKgs() * Phosphate.merchantPerTsp() * Tuning.MERCHANT_ACID_P2O5, 1e-9, "the granulator takes all the acid on TSP");
		assertEquals(0.22, Phosphate.ammoniaPerDap(), 0.005);
		assertEquals(69, Phosphate.dapFromAmmoniaKgh(), 1, "the ammonia plant makes only 69 kg of DAP an hour");
		assertEquals(1.16, Phosphate.millRockKgs(), 0.01, "the mill grinds for the reactor and the granulator");
		assertEquals(0.61, Phosphate.millWaterPerRock(), 0.01);
	}

	@Test
	void potashMakesFiveTonnesOfMopAnHour() {
		assertEquals(6.25, Phosphate.mopKgs() * Phosphate.orePerMop() * 3.6, 0.01, "t/h of ore");
		assertEquals(0.25, Phosphate.saltPerMop(), 0.001);
		assertTrue(Phosphate.mopKgs() * (Tuning.POTASH_DISSOLVER_STEAM + Tuning.POTASH_EJECTOR_STEAM) < Tuning.PROCESS_BOILER_KGS, "one package boiler carries it");
		assertEquals(Tuning.POTASH_DRYER_GAS_WATTS, Phosphate.dryerJPerKg() * Phosphate.mopKgs(), 1e-6);
	}

}
