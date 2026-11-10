package dev.wildcatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The world's time scale (docs/time-scale.md): presets only, waits divided, the reactor's real heat-up sped by it, the new-world choices. */
class TimeScaleTest {
	@BeforeEach
	void setUp() {
		NewWorldSetup.noConfig = true;
		NewWorldSetup.clear();
		Rules.clear();
	}

	@AfterEach
	void tearDown() {
		NewWorldSetup.clear();
		Rules.clear();
		NewWorldSetup.noConfig = false;
	}

	@Test
	void presetsOnly() {
		for (int p : new int[] {1, 5, 10, 20, 50}) {
			assertTrue(TimeScale.valid(p));
		}
		assertFalse(TimeScale.valid(7));
		assertFalse(TimeScale.valid(0));
		assertThrows(IllegalArgumentException.class, () -> TimeScale.set(7));
		assertEquals(1, TimeScale.preset(7));
		assertEquals("20:1", TimeScale.label(20));
	}

	@Test
	void waitsDivided() {
		assertEquals(3600, TimeScale.scaled(3600));
		TimeScale.set(10);
		assertEquals(18 * 360.0, TimeScale.scaled(Tuning.COKING_HOLD_S), 1e-9);   // 18 h at 10:1 is 1.8 h
		TimeScale.set(50);
		assertEquals(72, TimeScale.scaled(3600), 1e-9);
	}

	@Test
	void heatupFollowsTheScaleOnlyWhenReal() {
		TimeScale.set(20);
		assertEquals(Tuning.HEATUP_SPEEDUP, Rules.heatupSpeedup());   // the old compression stands without the rule
		Rules.set(Rules.Rule.REACTOR_REAL_HEATUP, true);
		assertEquals(20, Rules.heatupSpeedup());
		TimeScale.set(1);
		assertEquals(1, Rules.heatupSpeedup());
	}

	@Test
	void clearGoesBackToRealTime() {
		TimeScale.set(50);
		Rules.clear();
		assertEquals(1, TimeScale.scale());
	}

	@Test
	void newWorldChoices() {
		assertEquals(1, NewWorldSetup.scale());
		assertTrue(NewWorldSetup.rule(Wildcatter.MOD_ID, "mudGel", true));
		assertFalse(NewWorldSetup.chosen());
		NewWorldSetup.chooseScale(20);
		NewWorldSetup.choose(Wildcatter.MOD_ID, "mudGel", false);
		assertEquals(20, NewWorldSetup.scale());
		assertFalse(NewWorldSetup.rule(Wildcatter.MOD_ID, "mudGel", true));
		NewWorldSetup.register("othermod", "someRule", "does something", false);
		assertTrue(NewWorldSetup.entries().stream().anyMatch(e -> e.id().equals("othermod:someRule")));
		assertEquals(Rules.Rule.values().length, NewWorldSetup.entries().stream().filter(e -> e.mod().equals(Wildcatter.MOD_ID)).count());
		NewWorldSetup.clear();
		assertEquals(1, NewWorldSetup.scale());
		assertTrue(NewWorldSetup.rule(Wildcatter.MOD_ID, "mudGel", true));
	}
}
