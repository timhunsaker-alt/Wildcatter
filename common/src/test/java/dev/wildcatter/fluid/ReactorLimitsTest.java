package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 19a's limits and trips (docs/multiblocks/reactor-extensions.md), each with its rule switched on and handed back. */
class ReactorLimitsTest {
	private boolean[] rules;

	@BeforeEach
	void keep() {
		rules = dev.wildcatter.Rules.snapshot();
	}

	@AfterEach
	void handBack() {
		dev.wildcatter.Rules.restore(rules);
	}

	@Test
	void newWorldsStartWithTheRulesOnAndExistingOnesOff() {
		for (dev.wildcatter.Rules.Rule r : dev.wildcatter.Rules.Rule.values()) {
			assertFalse(dev.wildcatter.Rules.defaultFor(r, false), r.key);
		}
		assertTrue(dev.wildcatter.Rules.defaultFor(dev.wildcatter.Rules.Rule.REACTOR_LTOP, true));
		assertTrue(dev.wildcatter.Rules.defaultFor(dev.wildcatter.Rules.Rule.REACTOR_REAL_HEATUP, true));
		assertFalse(dev.wildcatter.Rules.defaultFor(dev.wildcatter.Rules.Rule.REACTOR_CONTAMINATION, true));
		assertFalse(dev.wildcatter.Rules.defaultFor(dev.wildcatter.Rules.Rule.CASCADE_SPIN_UP, true));
		assertFalse(dev.wildcatter.Rules.defaultFor(dev.wildcatter.Rules.Rule.REAL_RESIDENCE_TIMES, true));
		assertEquals(dev.wildcatter.Rules.Rule.MUD_GEL, dev.wildcatter.Rules.Rule.byKey("mudgel"));
		dev.wildcatter.Rules.restore(new boolean[dev.wildcatter.Rules.Rule.values().length]);
		assertEquals(Tuning.HEATUP_SPEEDUP, dev.wildcatter.Rules.heatupSpeedup());
	}

	@Test
	void thePtCurveRisesFrom100To290() {
		assertEquals(3.4, Reactor.ptLimit(30), 1e-9);
		assertEquals(3.4, Reactor.ptLimit(100), 1e-9);
		assertEquals(9.45, Reactor.ptLimit(195), 1e-9);
		assertEquals(Double.POSITIVE_INFINITY, Reactor.ptLimit(291.7));
	}

	/** Without the rule a cold RCS at 7 MPa is what it was before Phase 19: no alarm, no failure. */
	@Test
	void withoutTheRuleNothingChanges() {
		dev.wildcatter.Rules.restore(new boolean[dev.wildcatter.Rules.Rule.values().length]);
		Reactor r = cold();
		r.setPressureForTest(7.0);
		run(r, inputs(), 5, 0.2);
		assertEquals("", r.alarms());
		assertFalse(r.vesselFailed());
		assertEquals(Tuning.RCS_WATER_KG, r.waterKg(), 1);
	}

	@Test
	void ltopHoldsThePressureAndTheCurveAlarms() {
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_LTOP, true);
		Reactor r = cold();
		Reactor.Inputs in = inputs();
		r.setPressureForTest(3.0);
		run(r, in, 1, 0.2);
		assertTrue(r.alarms().contains("LTOP NOT ARMED"), r.alarms());
		assertTrue(r.wantsHeaters(), "under 90 % of the curve the heaters run");
		r.setPressureForTest(3.2);
		assertFalse(r.wantsHeaters(), "the heaters hold 90 % of the curve");
		r.setPressureForTest(3.6);
		run(r, in, 1, 0.2);
		assertTrue(r.alarms().contains("P-T LIMIT"), r.alarms());
		assertEquals(1, r.ptViolations());
		r.toggleLtop();
		r.setPressureForTest(3.6);
		step(r, in, 0.2);
		assertEquals(Tuning.LTOP_MPA, r.pressure(), 0.01);
		assertTrue(r.reliefKg() > 0, "the PORVs let steam go");
		assertFalse(r.alarms().contains("LTOP"), r.alarms());
		// over twice the curve, unarmed: the vessel fails and empties
		r.toggleLtop();
		r.setPressureForTest(7.0);
		step(r, in, 0.2);
		assertTrue(r.vesselFailed() && r.trip().contains("VESSEL FAILURE"), r.trip());
		run(r, in, 200, 0.2);
		assertTrue(r.waterKg() < 1, "a failed vessel empties: " + r.waterKg());
	}

	@Test
	void aFastHeatUpAlarmsAndCounts() {
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_HEATUP_LIMIT, true);
		Reactor r = cold();
		r.setTemperatureForTest(150);
		r.setPressureForTest(7.0);   // subcooled: 286 C to boil
		Reactor.Inputs in = inputs();
		in.loop[0] = in.loop[1] = false;
		run(r, in, 120, 0.2);
		assertFalse(r.heatupAlarm(), "the pumps' heat alone is slow: " + r.heatupRate());
		for (int i = 0; i < 4; i++) {
			r.setHistoryForTest(1.0);   // kept at 6 % decay heat into the RCS: about 110 C/h in plant time
			run(r, in, 10, 0.2);
		}
		assertTrue(r.heatupAlarm() && r.alarms().contains("HEAT-UP RATE"), r.heatupRate() + " C/h");
		assertEquals(1, r.fatigue());
	}

	@Test
	void theRealHeatUpIs80TimesSlower() {
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_REAL_HEATUP, true);
		assertEquals(1, dev.wildcatter.Rules.heatupSpeedup());
		Reactor slow = cold();
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_REAL_HEATUP, false);
		Reactor fast = cold();
		Reactor.Inputs in = inputs();
		in.loop[0] = in.loop[1] = false;
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_REAL_HEATUP, true);
		run(slow, in, 60, 0.2);
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_REAL_HEATUP, false);
		run(fast, in, 60, 0.2);
		assertEquals(80, (fast.tavg() - 30) / (slow.tavg() - 30), 2);
	}

	@Test
	void aStepInTavgTripsOnOtDeltaT() {
		Reactor r = atPower();
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_DELTA_T_TRIPS, true);
		Reactor.Inputs in = inputs();
		for (double t = 0; t < 5; t += 0.2) {
			full(r, in, 0.2);
		}
		assertFalse(r.tripped(), r.trip() + " | Tavg " + r.tavg() + ", P " + r.power());
		assertTrue(r.otSetpoint() > r.deltaT() && r.opSetpoint() > r.deltaT(), r.otSetpoint() + " / " + r.opSetpoint() + " over " + r.deltaT());
		r.setTemperatureForTest(320);
		for (double t = 0; t < 2; t += 0.2) {
			full(r, in, 0.2);
		}
		assertTrue(r.tripped() && r.trip().startsWith("OT-DELTA-T"), r.trip());
	}

	@Test
	void withoutTheRuleTheSameStepDoesNotTrip() {
		Reactor r = atPower();
		Reactor.Inputs in = inputs();
		assertFalse(r.tripped(), r.trip());
		r.setTemperatureForTest(320);
		for (double t = 0; t < 2; t += 0.2) {
			full(r, in, 0.2);
		}
		assertFalse(r.tripped(), r.trip());
	}

	private static Reactor full() {
		Reactor r = new Reactor();
		for (int i = 0; i < 9; i++) {
			r.loadAssembly(0);
		}
		for (int i = 0; i < 5; i++) {
			r.loadRcca();
		}
		return r;
	}

	private static Reactor cold() {
		Reactor r = full();
		r.setHotStandbyForTest(1500);
		r.setColdForTest();
		return r;
	}

	/** Steady at full power, its rods out and its trip reset. */
	private static Reactor atPower() {
		Reactor r = full();
		r.setHotStandbyForTest(1500);
		r.setTemperatureForTest(Tuning.TAVG_FULL_LOAD);
		r.setHistoryForTest(1.0);
		r.setRodsForTest(Tuning.ROD_STEPS, 200);
		r.setSetpoint(1.0);
		r.holdPowerForTest(1.0);
		boolean was = dev.wildcatter.Rules.reactorDeltaTTrips();
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_DELTA_T_TRIPS, false);
		Reactor.Inputs in = inputs();
		for (double t = 0; t < 120; t += 0.2) {
			full(r, in, 0.2);
		}
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_DELTA_T_TRIPS, was);
		return r;
	}

	/** A step taking each steam generator's full-power steam and feeding it back. */
	private static void full(Reactor r, Reactor.Inputs in, double dt) {
		in.heaters = r.wantsHeaters() ? 1 : 0;
		for (int l = 0; l < 2; l++) {
			in.steamTaken[l] = Reactor.steamPerGenerator() * dt;
			in.feed[l] = r.feedWanted(l, dt);
		}
		r.step(in, dt);
	}

	private static Reactor.Inputs inputs() {
		Reactor.Inputs in = new Reactor.Inputs();
		in.crdm = true;
		in.pressurizer = true;
		in.loop[0] = in.loop[1] = true;
		in.pump[0] = in.pump[1] = 1;
		in.pumpPower[0] = in.pumpPower[1] = 1;
		in.cvcs = true;
		in.charging = 1;
		in.boricAcid = 1000;
		in.water = 1e9;
		return in;
	}

	private static void run(Reactor r, Reactor.Inputs in, double seconds, double dt) {
		for (double t = 0; t < seconds; t += dt) {
			step(r, in, dt);
		}
	}

	private static void step(Reactor r, Reactor.Inputs in, double dt) {
		in.heaters = r.wantsHeaters() ? 1 : 0;
		for (int l = 0; l < 2; l++) {
			in.feed[l] = Math.min(r.feedWanted(l, dt), Tuning.AFW_KGS * dt / 2);
		}
		r.step(in, dt);
	}
}
