package dev.wildcatter.well;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 23c (docs/multiblocks/oilfield-follow-ons.md): the spec's pressure window and gas in place. */
class CavernTest {
	@Test
	void theWindowAsIfAt1000Metres() {
		assertEquals(68, Cavern.minBar(), 1.0, "0.3 psi/ft at 1,000 m");
		assertEquals(193, Cavern.maxBar(), 1.5, "0.85 psi/ft at 1,000 m");
	}

	@Test
	void gasInPlace() {
		assertEquals(125, Cavern.sm3PerM3(Cavern.maxBar()) - Cavern.sm3PerM3(Cavern.minBar()), 3, "about 125 Sm3 of working gas a m3");
		assertEquals(70, Cavern.sm3PerM3(Cavern.minBar()), 3, "about 70 Sm3 of cushion a m3");
		double v = 2000;
		double working = Cavern.capacity(v) - Cavern.cushion(v);
		assertEquals(250_000, working, 8_000, "a 2,000 m3 cavern holds about 250,000 Sm3 of working gas");
		assertEquals(9.5, Cavern.gj(working) / 1000, 0.4, "about 9.5 TJ");
		assertEquals(Cavern.maxBar(), Cavern.bar(Cavern.capacity(v), v), 1e-6);
		assertEquals(1.0, Cavern.withdrawShare(Cavern.capacity(v), v), 1e-9, "full rate above half the window");
		assertEquals(0.0, Cavern.withdrawShare(Cavern.cushion(v), v), 1e-9, "none at the minimum");
		assertTrue(Cavern.withdrawShare(Cavern.cushion(v) * 1.2, v) > 0 && Cavern.withdrawShare(Cavern.cushion(v) * 1.2, v) < 1);
	}

	@Test
	void leaching() {
		double kgs = Tuning.INJECTION_PUMP_LPS / 1000 * Tuning.SALT_PER_M3_BRINE;
		assertEquals(3.1, kgs, 0.05, "3.1 kg/s of salt at the injection pump's 10 L/s");
		assertEquals(5.3, kgs * 3600 / 2117, 0.2, "about 5 rock salt blocks an hour");
	}
}
