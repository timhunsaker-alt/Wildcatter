package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 10d1: the generator bus's relay elements (docs/multiblocks/gen-bus-protection.md). */
class BusProtectionTest {
	@Test
	void theBusDifferentialSeesAFaultOnTheBusButNotThroughIt() {
		// a bolted fault on the bus fed by one 20.6 MVA generator: 17 kA in, none out at a breaker
		assertTrue(BusProtection.differentialOperates(17_000, 17_000, 3000));
		// the same current passing through the bus to a fault past a feeder breaker: in at one breaker, out at another
		assertFalse(BusProtection.differentialOperates(0, 34_000, 3000));
		// a small mismatch at full load stays under the floor of 20 % of the largest breaker
		assertFalse(BusProtection.differentialOperates(500, 1_700, 3000));
		assertEquals(600, BusProtection.differentialPickup(1_700, 3000), 1e-9);
		assertEquals(0.3 * 34_000, BusProtection.differentialPickup(34_000, 3000), 1e-9);
	}

	@Test
	void voltageRestraintLowersThePickupInAFault() {
		assertEquals(1.5 * 862, BusProtection.restrainedPickup(862, 1.0), 1e-9);
		assertEquals(1.5 * 862 * 0.5, BusProtection.restrainedPickup(862, 0.5), 1e-9);
		assertEquals(1.5 * 862 * 0.25, BusProtection.restrainedPickup(862, 0.05), 1e-9);   // a floor at a quarter
		assertEquals(1.5 * 862, BusProtection.restrainedPickup(862, 1.08), 1e-9);
	}

	@Test
	void theInverseCurvesAreIeeeC37112() {
		assertEquals(3.922 / 3 + 0.0982, BusProtection.veryInverseSeconds(2, 1), 1e-9);
		assertEquals(0.0515 / (Math.pow(2, 0.02) - 1) + 0.114, BusProtection.moderatelyInverseSeconds(2, 1), 1e-9);
		assertTrue(BusProtection.veryInverseSeconds(10, 1) < BusProtection.veryInverseSeconds(2, 1));
		assertEquals(Double.POSITIVE_INFINITY, BusProtection.veryInverseSeconds(1.0, 1));
	}

	@Test
	void aTimeElementRunsOverPickupAndResetsBelowIt() {
		BusProtection.TimeElement e = new BusProtection.TimeElement(false);
		double toTrip = BusProtection.moderatelyInverseSeconds(4, 1);
		assertFalse(e.step(4000, 1000, 1, toTrip * 0.6));
		assertFalse(e.step(500, 1000, 1, 0.05));          // the overload passes: it resets
		assertEquals(0, e.progress(), 1e-9);
		assertFalse(e.step(4000, 1000, 1, toTrip * 0.6));
		assertTrue(e.step(4000, 1000, 1, toTrip * 0.5));
	}
}
