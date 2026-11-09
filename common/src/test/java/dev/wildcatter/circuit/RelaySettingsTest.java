package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RelaySettingsTest {
	@Test
	void theDefaultsAreTheOldFixedSettings() {
		RelaySettings s = new RelaySettings();
		assertEquals(100, s.pickup(0));
		assertEquals(RelaySettings.Curve.MI, s.curve());
		assertEquals(1.0, s.dial());
		assertEquals(8.0, s.instantaneous());
		assertEquals(BusProtection.moderatelyInverseSeconds(3, 1), s.secondsAt(300, 0), 1e-9);
		assertEquals("51 pickup 100 A (MI, TD 1), 50 at 800 A", s.describe(0, false));
	}

	@Test
	void theCurvesAreIeeeC37_112() {
		assertEquals(19.61 / 3 + 0.491, RelaySettings.Curve.VI.seconds(2, 1), 1e-9);
		assertEquals(28.2 / 3 + 0.1217, RelaySettings.Curve.EI.seconds(2, 1), 1e-9);
		// at 10 x pickup, EI is the fastest, MI the slowest
		assertTrue(RelaySettings.Curve.EI.seconds(10, 1) < RelaySettings.Curve.VI.seconds(10, 1));
		assertTrue(RelaySettings.Curve.VI.seconds(10, 1) < RelaySettings.Curve.MI.seconds(10, 1));
	}

	@Test
	void theCursorStepsOneSettingAtATime() {
		RelaySettings s = new RelaySettings();
		s.step(600);                                  // pickup 100 -> 200
		assertEquals(200, s.pickup(600));
		assertEquals(RelaySettings.Field.CURVE, s.nextField());
		s.step(600);
		assertEquals(RelaySettings.Curve.VI, s.curve());
		s.nextField();
		s.step(600);
		assertEquals(2.0, s.dial());
		assertTrue(s.describe(600, true).contains("[TD 2]"));
		s.nextField();
		s.step(600);
		assertEquals(10.0, s.instantaneous());
		int[] saved = s.save();
		RelaySettings t = new RelaySettings();
		t.load(saved[0], saved[1], saved[2], saved[3], saved[4]);
		assertEquals(s.describe(600, true), t.describe(600, true));
	}

	@Test
	void aPickupAboveTheCeilingIsSkipped() {
		RelaySettings s = new RelaySettings();
		s.setPickup(600);
		s.step(600);                                  // 1,200 is above a 600 A breaker: wraps to 5 A
		assertEquals(5, s.pickup(600));
	}
}
