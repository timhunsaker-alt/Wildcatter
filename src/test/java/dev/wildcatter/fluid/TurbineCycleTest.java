package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 8b: the turbine island (docs/multiblocks/turbine-island.md). */
class TurbineCycleTest {
	@Test
	void fullSteamMakes51Megawatts() {
		double steam = 2 * Reactor.steamPerGenerator();
		assertEquals(86, steam, 1);
		assertEquals(51e6, TurbineCycle.designWatts(steam), 1e6);
	}

	@Test
	void itRejects104MegawattsInto2500KilogramsOfWater() {
		double steam = 2 * Reactor.steamPerGenerator();
		double rejected = TurbineCycle.rejectedWatts(steam, TurbineCycle.designWatts(steam));
		assertEquals(104e6, rejected, 2e6);
		assertEquals(2500, TurbineCycle.cwNeeded(rejected), 100);
	}

	@Test
	void theVacuumFollowsTheWater() {
		assertEquals(5.6, TurbineCycle.backPressureKpa(1), 0.1);
		assertEquals(1.0, TurbineCycle.vacuumFactor(TurbineCycle.backPressureKpa(1)), 1e-9);
		assertTrue(TurbineCycle.backPressureKpa(0.5) > 9 && TurbineCycle.vacuumFactor(TurbineCycle.backPressureKpa(0.5)) > 0.94);
		assertTrue(TurbineCycle.backPressureKpa(0.2) > Tuning.LOW_VACUUM_TRIP_KPA, "a fifth of the water trips it on low vacuum");
	}

	@Test
	void thePumpsFitTheirMotors() {
		assertTrue(TurbineCycle.pumpKw(Tuning.MAIN_FEED_PUMP_KGS, 7.6) < Tuning.MAIN_FEED_PUMP_WATTS / 1000);
		assertTrue(TurbineCycle.pumpKw(Tuning.CONDENSATE_PUMP_KGS, 2.5) < Tuning.CONDENSATE_PUMP_WATTS / 1000);
		assertTrue(TurbineCycle.pumpKw(Tuning.CW_PUMP_KGS, 0.147) < Tuning.CW_PUMP_WATTS / 1000);
	}
}
