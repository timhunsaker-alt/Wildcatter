package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 19b's leaking rods and coolant activity (docs/multiblocks/reactor-extensions.md), the rule switched on and handed back. */
class ReactorActivityTest {
	private boolean[] rules;

	@BeforeEach
	void keep() {
		rules = dev.wildcatter.Rules.snapshot();
	}

	@AfterEach
	void handBack() {
		dev.wildcatter.Rules.restore(rules);
	}

	private static Reactor atPower(boolean bentFirst) {
		Reactor r = new Reactor();
		r.loadAssembly(0, bentFirst);
		for (int i = 1; i < 9; i++) {
			r.loadAssembly(0);
		}
		for (int i = 0; i < 5; i++) {
			r.loadRcca();
		}
		r.setHotStandbyForTest(1500);
		r.setTemperatureForTest(Tuning.TAVG_FULL_LOAD);
		r.setRodsForTest(Tuning.ROD_STEPS, 200);
		r.setSetpoint(1.0);
		r.pinPowerForTest(1.0);
		return r;
	}

	private static Reactor.Inputs inputs(boolean demin) {
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
		in.demineralizer = demin;
		return in;
	}

	private static void run(Reactor r, Reactor.Inputs in, double seconds) {
		for (double t = 0; t < seconds; t += 1) {
			for (int l = 0; l < 2; l++) {
				in.steamTaken[l] = Reactor.steamPerGenerator();
				in.feed[l] = r.feedWanted(l, 1);
			}
			r.step(in, 1);
		}
	}

	@Test
	void aBentGridLeaksAfterAThirdOfItsCycle() {
		Reactor r = atPower(true);
		assertTrue(r.bent(0) && !r.leaking(0));
		r.setBurnupsForTest(ReactorActivity.thirdOfCycle() * 0.99);
		run(r, inputs(true), 2);
		assertFalse(r.leaking(0), "not before a third of its cycle");
		r.setBurnupsForTest(ReactorActivity.thirdOfCycle() * 1.01);
		run(r, inputs(true), 2);
		assertTrue(r.leaking(0) && r.leakers() == 1);
	}

	@Test
	void withoutTheRuleTheCoolantCarriesNoActivity() {
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_CONTAMINATION, false);
		Reactor r = atPower(false);
		r.setLeakingForTest(0);
		run(r, inputs(true), 600);
		assertEquals(0, r.activity());
		assertEquals("", r.alarms());
	}

	@Test
	void aLeakerRaisesTheActivityAndTheResinTakesItOut() {
		dev.wildcatter.Rules.set(dev.wildcatter.Rules.Rule.REACTOR_CONTAMINATION, true);
		Reactor clean = atPower(false);
		Reactor leaky = atPower(false);
		leaky.setLeakingForTest(0);
		run(clean, inputs(true), 3600);
		run(leaky, inputs(true), 3600);
		assertTrue(leaky.activity() > 3 * clean.activity() && clean.activity() > 0, clean.activity() + " / " + leaky.activity());
		// without the demineralizer the leaker's activity keeps climbing past the limit
		Reactor noResin = atPower(false);
		noResin.setLeakingForTest(0);
		noResin.setActivityForTest(0.98);
		run(noResin, inputs(false), 3 * 3600);
		assertTrue(noResin.activity() > Tuning.ACTIVITY_LIMIT && noResin.alarms().contains("COOLANT ACTIVITY"), noResin.activity() + " " + noResin.alarms());
	}
}
