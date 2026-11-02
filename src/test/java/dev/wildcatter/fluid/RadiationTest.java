package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 8e: radiation, the dosimeter's numbers and spent fuel (docs/multiblocks/fuel-handling.md). */
class RadiationTest {
	@Test
	void aDayOldAssemblyIsLethalBareAndHarmlessUnderWater() {
		double at1m = Radiation.assemblyAt1m(0.1e6);
		assertEquals(100, at1m, 1e-9);
		assertEquals(4.0, Radiation.rate(at1m, 5, 0, 0), 1e-9);
		assertTrue(Radiation.rate(at1m, 4, 3, 0) < 1e-5, "under 3 m of water: " + Radiation.rate(at1m, 4, 3, 0));
	}

	@Test
	void theEffectsFollowTheRealThresholds() {
		assertEquals(0, Radiation.stage(0.5));
		assertEquals(1, Radiation.stage(1.2));
		assertEquals(2, Radiation.stage(4));
		assertEquals(3, Radiation.stage(7));
		assertEquals(4, Radiation.stage(8));
		assertEquals(1.0, Radiation.repaired(2.0, 24 * 3600), 1e-9);
	}

	@Test
	void anAssemblyCoolsInThePool() {
		double[] eq = Tuning.DECAY_SHARE.clone();
		SpentFuel.Assembly a = new SpentFuel.Assembly(50, eq, false);
		a.decay(86400);
		assertEquals(0.0064 * Reactor.assemblyWatts(), a.watts(), 0.0064 * Reactor.assemblyWatts() * 0.15);
	}

	@Test
	void anUncooledPoolTakesADayOrTwoToBoil() {
		double watts = 9 * 0.005 * Reactor.assemblyWatts();
		double hours = SpentFuel.hoursToBoil(watts);
		assertTrue(hours > 24 && hours < 72, hours + " h");
		assertEquals(0.34, SpentFuel.boilOff(watts), 0.05);
	}
}
