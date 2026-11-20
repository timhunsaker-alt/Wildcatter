package dev.wildcatter.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Grid batteries' cells (docs/multiblocks/grid-batteries.md, 22b): state of charge, losses, taper, fade and new modules. */
class BessTest {
	private static BessCells container() {
		BessCells c = new BessCells(Tuning.CONTAINER_KWH, Tuning.CELL_HEAT_CAPACITY, Tuning.CHILLER_WATTS);
		return c;
	}

	@Test
	void aContainerHoldsFiveMegawattHours() {
		assertEquals(5011, Tuning.CONTAINER_KWH, 1);
		assertEquals(261, Tuning.CABINET_MODULES * Tuning.LFP_MODULE_KWH, 1);
	}

	@Test
	void roundTripAtThePcsTerminalsIsAboutNinetyOnePercent() {
		// 98.5 % x 97 % each way; the MV transformer's losses (the solver's) and the chillers' draw bring a day's cycle to about 87 %
		BessCells c = container();
		c.setSoc(0.05);
		double inKwh = 0;
		while (c.soc() < 0.95) {
			c.step(-1.25e6, 60, true);   // 0.25C, a minute at a time
			inKwh += 1.25e6 * 60 / 3.6e6;
		}
		double outKwh = 0;
		while (c.soc() > 0.05) {
			c.step(1.25e6, 60, true);
			outKwh += 1.25e6 * 60 / 3.6e6;
		}
		double rte = outKwh / inKwh;
		assertTrue(rte > 0.90 && rte < 0.925, "round trip " + rte);
	}

	@Test
	void powerTapersAtTheEdgesOfTheWindow() {
		BessCells c = container();
		c.setSoc(0.50);
		assertEquals(1.25e6, c.dischargeLimit(1.25e6, Tuning.SOC_MIN), 1);
		c.setSoc(0.065);
		assertEquals(0.5 * 1.25e6, c.dischargeLimit(1.25e6, Tuning.SOC_MIN), 1e3);
		c.setSoc(0.05);
		assertEquals(0, c.dischargeLimit(1.25e6, Tuning.SOC_MIN), 1);
		c.setSoc(0.95);
		assertEquals(0, c.chargeLimit(1.25e6, Tuning.SOC_MAX), 1);
	}

	@Test
	void fadeIsTwentyPercentOverSixThousandCyclesAndFasterHot() {
		BessCells c = container();
		c.testCycles(6000, 25);
		assertEquals(0.80, c.soh(), 1e-9);
		BessCells hot = container();
		hot.testCycles(100, 35);
		BessCells cool = container();
		cool.testCycles(100, 25);
		assertEquals(2.0, (1 - hot.soh()) / (1 - cool.soh()), 1e-6);
	}

	@Test
	void theBmsOpensOverFiftyFiveDegrees() {
		BessCells c = container();
		c.setSoc(0.5);
		c.setTemp(56);
		c.step(0, 1, false);
		assertTrue(c.overtemp());
		assertEquals(0, c.dischargeLimit(1e6, 0.05), 0);
	}

	@Test
	void newModulesRaiseHealthByTheirShare() {
		BessCells c = new BessCells(0, Tuning.CELL_HEAT_CAPACITY, Tuning.CHILLER_WATTS);
		c.addModules(48 * Tuning.LFP_MODULE_KWH);
		c.testCycles(3000, 25);   // the first half worn to 90 %
		c.addModules(48 * Tuning.LFP_MODULE_KWH);
		assertEquals(0.95, c.soh(), 1e-6);
		assertEquals(Tuning.CONTAINER_KWH, c.ratedKwh(), 1e-6);
	}
}
