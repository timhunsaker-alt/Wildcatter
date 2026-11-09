package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 14b-14d: the sales line's account, the acid plant's steam in the 5 MW turbine and biogas in a gas set. */
class GasPowerTest {
	// ---- 14b: the sales line (docs/multiblocks/sales-gas.md) --------------------------------------------------------------------
	@Test
	void onlyPipelineGasIsOnSpec() {
		assertEquals(SalesGas.Spec.ON_SPEC, SalesGas.check(Product.PIPELINE_GAS));
		assertEquals(SalesGas.Spec.RICH, SalesGas.check(Product.GAS));
		assertEquals(SalesGas.Spec.RICH, SalesGas.check(Product.COMPRESSED_GAS));
		assertEquals(SalesGas.Spec.RICH, SalesGas.check(Product.DRIED_GAS));
		assertEquals(SalesGas.Spec.WATER, SalesGas.check(Product.WET_GAS));
		assertEquals(SalesGas.Spec.CO2, SalesGas.check(Product.BIOGAS));
		assertEquals(SalesGas.Spec.NOT_GAS, SalesGas.check(Product.CRUDE));
		assertEquals(SalesGas.Spec.NONE, SalesGas.check(null));
		assertTrue(SalesGas.Spec.RICH.reading().startsWith("OFF SPEC: RICH"));
	}

	@Test
	void pipelineGasIsAboutAThousandBtuAndTheLineKeepsTwoPercent() {
		assertEquals(1_020, SalesGas.btuPerScf(Tuning.GAS_JOULES_PER_M3), 5);
		double gj = SalesGas.gj(1_000);
		assertEquals(38.0, gj, 1e-9);
		assertEquals(36.0, SalesGas.mmbtu(gj), 0.1);
		assertEquals(gj * 0.98, SalesGas.credited(gj), 1e-9);
		assertEquals(gj, SalesGas.credited(gj) + SalesGas.retained(gj), 1e-9);
		// a full gas plant's 1.0 m3/s is 38 MW of fuel, about 37 MW delivered after retention and the line heater
		double delivered = SalesGas.credited(SalesGas.gj(1.0)) / SalesGas.debit(1.0) * Tuning.GAS_JOULES_PER_M3;
		assertEquals(37.2e6, delivered, 0.1e6);
		assertEquals(5.0, SalesGas.rating(1.0), 1e-9, "a station's rating");
	}

	@Test
	void aDeliveryNeverTakesMoreThanTheAccountHolds() {
		assertEquals(0, SalesGas.deliverable(0, 5), 1e-12);
		assertEquals(5, SalesGas.deliverable(100, 5), 1e-12);
		double balance = SalesGas.debit(2.0);
		assertEquals(2.0, SalesGas.deliverable(balance, 5), 1e-9, "it gives out exactly what the account pays for");
		assertEquals(0.2 / 100, SalesGas.debit(1) / SalesGas.gj(1) - 1, 1e-12, "the line heater burns 0.2 %");
	}

	// ---- 14c: the acid plant's steam (docs/multiblocks/acid-plant-steam.md) -------------------------------------------------------
	@Test
	void theAcidPlantsSteamMakesAboutOnePointSixMegawatts() {
		assertEquals(2.08, AcidSteam.boilerKgs(), 0.01);
		assertEquals(2.02, AcidSteam.turbineKgs(), 0.01);
		assertEquals(0.867e6, AcidSteam.designWattsPerKgs(), 0.001e6);
		assertEquals(0.10, AcidSteam.throttleLoss(Tuning.ST_DESIGN_STEAM_KGS / 3), 1e-9, "a tenth at a third of its flow");
		assertEquals(0, AcidSteam.throttleLoss(Tuning.ST_DESIGN_STEAM_KGS), 1e-12);
		double watts = AcidSteam.turbineWatts(AcidSteam.turbineKgs());
		assertTrue(watts > 1.5e6 && watts < 1.7e6, "about 1.6 MW: " + watts);
		assertEquals(Math.min(Tuning.ST_WATTS, Tuning.ST_DESIGN_WATTS), AcidSteam.turbineWatts(Tuning.ST_DESIGN_STEAM_KGS), 1, "held to the generator's rating");
		assertEquals(0.83e6, AcidSteam.superheatWatts(2.0), 0.01e6, "the superheater takes about 0.83 MW at 2 kg/s");
		// about 1.3 MW exported over the plant's own 0.30 MW
		assertEquals(1.3e6, watts - 0.30e6, 0.05e6);
	}

	@Test
	void theCondensateReturnCutsTheMakeUpToATenth() {
		double boiler = AcidSteam.boilerKgs();
		double toTurbine = AcidSteam.turbineKgs();
		double makeUp = AcidSteam.makeUpKgs(boiler, toTurbine);
		assertEquals(Tuning.MELTER_STEAM_KGS + toTurbine * 0.10, makeUp, 1e-9);
		assertTrue(makeUp < boiler * 0.15, "a closed loop: " + makeUp);
	}

	// ---- 14d: biogas (docs/biogas.md) -------------------------------------------------------------------------------------------
	@Test
	void aGasSetOnBiogasBurnsFourHundredCubicMetresAnHourAt850Kilowatts() {
		assertEquals(406.5, Biogas.fullLoadM3h(), 0.1);
		assertEquals(61.0, Biogas.fuelM3h(0), 0.1, "15 % of full load at no load");
		assertEquals(Biogas.fullLoadM3h(), Biogas.fuelM3h(1), 1e-9);
		assertEquals(0.85, Biogas.derate(), 1e-9);
		assertEquals(48, 23 * Biogas.kwhPerM3(), 0.1, "23 m3 of biogas makes Farmings' 48 kWh");
		assertEquals(Tuning.GAS_FUEL_TICKS_PER_UNIT * 23.0 / 38.0, Biogas.fireboxTicksPerM3(), 1e-9);
		assertTrue(Product.BIOGAS.gas() && !Product.BIOGAS.tanked());
		assertEquals("m3", Product.BIOGAS.unit());
		assertEquals("biogas, 60 % CH4", Product.BIOGAS.label());
	}
}
