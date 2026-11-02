package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 8a: the nuclear island (docs/multiblocks/reactor.md). */
class ReactorTest {
	@Test
	void theCoreMakes155Megawatts() {
		assertEquals(17.2e6, Reactor.assemblyWatts(), 0.05e6);
		assertEquals(155e6, Reactor.ratedWatts(), 0.5e6);
	}

	@Test
	void eachLoopCarries431KilogramsAndMakes43OfSteam() {
		assertEquals(431, Reactor.loopFlow(), 2);
		assertEquals(43.1, Reactor.steamPerGenerator(), 0.5);
		assertEquals(7.6, Steam.psat(Tuning.TAVG_NO_LOAD), 0.05);
	}

	@Test
	void theShutdownMarginIsAtLeastTheRequired() {
		assertTrue(Reactor.shutdownMargin() >= Tuning.SHUTDOWN_MARGIN, "margin " + Reactor.shutdownMargin());
	}

	@Test
	void aCycleIsAbout107Hours() {
		assertEquals(107, Reactor.cycleHours(), 2);
	}

	@Test
	void theRodsWorthFollowsTheSCurve() {
		assertEquals(1.0, Reactor.inserted(0), 1e-9);
		assertEquals(0.5, Reactor.inserted(Tuning.ROD_STEPS / 2.0), 1e-9);
		assertEquals(0.0, Reactor.inserted(Tuning.ROD_STEPS), 1e-9);
	}

	/** A full core at hot standby with both pumps, the aux feed and the dump valves: the operator's view of a startup to 2 %. */
	@Test
	void itGoesCriticalHoldsTwoPercentAndTrips() {
		Reactor r = new Reactor();
		for (int i = 0; i < 9; i++) {
			r.loadAssembly(0);
		}
		for (int i = 0; i < 5; i++) {
			r.loadRcca();
		}
		r.setHotStandbyForTest(1500);
		Reactor.Inputs in = inputs();
		double dt = 0.2;
		run(r, in, 60, dt);
		assertTrue(r.tripped(), "it starts tripped");
		r.setSetpoint(0.02);
		boolean critical = false;
		double vented = 0;
		for (double t = 0; t < 3600; t += dt) {
			step(r, in, dt);
			critical |= r.startupRate() > 0.2 && r.power() > 1e-6;
			vented += r.vented(0) + r.vented(1);
		}
		assertTrue(critical, "it should go critical");
		assertTrue(!r.tripped(), "it should not trip: " + r.trip());
		assertEquals(Tuning.ROD_STEPS, r.shutdownBank(), 1e-6);
		assertEquals(0.02, r.power(), 0.002);
		assertTrue(vented > 1000, "the dump valves vent: " + vented);
		assertTrue(r.sgLevel(0) > 0.9, "the aux feed holds the level: " + r.sgLevel(0));
		assertEquals(Tuning.RCS_MPA, r.pressure(), 0.3);

		// one pump stopped below P-8: it runs on one loop
		in.pump[1] = 0;
		in.pumpPower[1] = 0;
		run(r, in, 120, dt);
		assertTrue(!r.tripped(), "one loop below P-8 runs on: " + r.trip());

		r.nextSetpoint();
		while (r.setpoint() >= 0) {
			r.nextSetpoint();
		}
		run(r, in, 10, dt);
		assertTrue(r.tripped());
		assertEquals("MANUAL TRIP", r.trip());
		assertTrue(r.power() < 0.001, "the power falls on the delayed neutrons: " + r.power());
		assertEquals(0, r.bankD(), 1e-9);
		assertEquals(0, r.shutdownBank(), 1e-9);
	}

	@Test
	void losingTheRodDrivesTrips() {
		Reactor r = new Reactor();
		for (int i = 0; i < 9; i++) {
			r.loadAssembly(0);
		}
		for (int i = 0; i < 5; i++) {
			r.loadRcca();
		}
		r.setHotStandbyForTest(1500);
		Reactor.Inputs in = inputs();
		r.setSetpoint(0.02);
		run(r, in, 600, 0.2);
		in.crdm = false;
		run(r, in, 5, 0.2);
		assertEquals("CRDM POWER LOST", r.trip());
	}

	@Test
	void fillingBlendsToTheTargetAndHeatUpTakesAboutHalfAnHour() {
		Reactor r = new Reactor();
		Reactor.Inputs in = inputs();
		in.pump[0] = in.pump[1] = 0;
		in.pumpPower[0] = in.pumpPower[1] = 0;
		double t = 0;
		while (!r.full() && t < 7200) {
			step(r, in, 1);
			t++;
		}
		assertEquals(3000, t, 100);   // 30 t at 10 kg/s
		assertEquals(1500, r.ppm(), 1);
		while (r.pressure() < Tuning.RCP_SEAL_MPA && t < 20_000) {
			step(r, in, 1);
			t++;
		}
		double heaters = t;
		in.pump[0] = in.pump[1] = 1;
		in.pumpPower[0] = in.pumpPower[1] = 1;
		while (r.tavg() < Tuning.TAVG_NO_LOAD - 1 && t < 20_000) {
			step(r, in, 1);
			t++;
		}
		double heatup = t - heaters;
		assertTrue(heatup > 1200 && heatup < 2700, "heat-up on the pumps " + heatup + " s");
		run(r, in, 1800, 1);
		assertEquals(Tuning.RCS_MPA, r.pressure(), 0.3);
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
		double afw = Tuning.AFW_KGS * dt;
		for (int l = 0; l < 2; l++) {
			in.feed[l] = Math.min(r.feedWanted(l, dt), afw / 2);
		}
		r.step(in, dt);
	}
}
