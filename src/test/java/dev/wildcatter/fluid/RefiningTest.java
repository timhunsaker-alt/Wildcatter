package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

class RefiningTest {
	@Test
	void eachAssayAddsUp() {
		for (Product feed : new Product[] {Product.CRUDE, Product.CONDENSATE}) {
			assertEquals(1.0, Assay.of(feed).values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9, feed.label());
		}
	}

	@Test
	void eachCrackerFeedAddsUp() {
		assertEquals(1.0, Cracker.LPG.total(), 1e-9);
		assertEquals(1.0, Cracker.GASOLINE.total(), 1e-9);
	}

	@Test
	void theToppingUnitsOwnLpgNearlyFuelsItsHeater() {
		double lpgWatts = Tuning.TOPPING_LPS * Assay.of(Product.CRUDE).get(Product.LPG) * Tuning.LPG_JOULES_PER_L;
		assertTrue(lpgWatts > 0.8 * Tuning.TOPPING_HEATER_WATTS && lpgWatts < 1.2 * Tuning.TOPPING_HEATER_WATTS, "LPG " + lpgWatts + " W");
	}

	@Test
	void anLpgCrackerMakesAboutTwelveSheetsAMinute() {
		double ethyleneKgs = Tuning.CRACKER_FEED_LPS * Tuning.LPG_KG_PER_L * Cracker.LPG.ethylene();
		double sheetsPerMinute = ethyleneKgs * Tuning.PE_CONVERSION / Tuning.PE_KG_PER_SHEET * 60;
		assertEquals(12.6, sheetsPerMinute, 0.5);
	}

	@Test
	void theFuelGasRoughlyFuelsTheFurnace() {
		double fuelKgs = Tuning.CRACKER_FEED_LPS * Tuning.LPG_KG_PER_L * Cracker.LPG.fuelGas();
		double watts = fuelKgs / Tuning.FUEL_GAS_KG_PER_M3 * Tuning.GAS_JOULES_PER_M3;
		assertTrue(watts > 0.8 * Tuning.CRACKER_FUEL_WATTS_LPG && watts < 1.4 * Tuning.CRACKER_FUEL_WATTS_LPG, "fuel gas " + watts + " W");
	}
}
