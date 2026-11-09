package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import java.util.List;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class TimeCurrentTest {
	private static final double HZ = 60;

	private static TimeCurrent.CurrentLimiting classJ(double amps) {
		return new TimeCurrent.CurrentLimiting("class J " + amps, amps, Tuning.CLASS_J_SECONDS_AT_2X, Tuning.CLASS_J_INSTANT_MULTIPLE, HZ);
	}

	@Test
	void classJLetThroughIsWithinTheUl248_8Limits() {
		// UL 248-8 class J maximum peak let-through at 200 kA: 30 A 6 kA, 60 A 8 kA, 100 A 12 kA, 200 A 16 kA, 400 A 25 kA, 600 A 35 kA
		double[][] limits = {{30, 6000}, {60, 8000}, {100, 12000}, {200, 16000}, {400, 25000}, {600, 35000}};
		for (double[] l : limits) {
			double peak = classJ(l[0]).letThroughPeak(200_000);
			assertTrue(peak <= l[1] * 1.1 && peak >= l[1] * 0.75, l[0] + " A: " + peak + " against " + l[1]);   // the chart's shape, within a tenth over
		}
	}

	@Test
	void aCurrentLimitingFuseClearsInsideHalfACycle() {
		TimeCurrent.CurrentLimiting j = classJ(60);
		assertFalse(j.limiting(800));
		assertTrue(j.clearingSeconds(1000) <= 0.5 / HZ);
		assertTrue(j.clearingSeconds(50_000) < j.clearingSeconds(1000));
		assertEquals(3 * Tuning.CLASS_J_SECONDS_AT_2X / 3, j.clearingSeconds(120), 1e-9);   // 2 x: 80 s
		// a limited fault's let-through I²t is far below the prospective's over a cycle
		assertTrue(j.letThroughI2t(50_000) < 50_000.0 * 50_000.0 / HZ / 50);
	}

	@Test
	void aBranchBreakerUnderAMainIsSelectiveOnlyBelowTheMainsMagneticPickup() {
		TripCurve branch = TimeCurrent.thermalMagnetic("20 A branch breaker", 20, Tuning.BREAKER_TRIP_SECONDS_AT_2X, Tuning.BREAKER_MAGNETIC_MULTIPLE, HZ);
		TripCurve main = TimeCurrent.thermalMagnetic("200 A main", 200, Tuning.BREAKER_TRIP_SECONDS_AT_2X, Tuning.BREAKER_MAGNETIC_MULTIPLE, HZ);
		TimeCurrent.Study s = TimeCurrent.study(branch, main, 20, 10_000, HZ);
		assertFalse(s.selectiveToRating());
		assertEquals(200 * Tuning.BREAKER_MAGNETIC_MULTIPLE, s.notSelectiveAbove(), 200 * Tuning.BREAKER_MAGNETIC_MULTIPLE * 0.03);
	}

	@Test
	void aSmallClassJUnderABigBreakerIsSelectiveToItsRatingWhileItsLetThroughStaysUnderThePickup() {
		TripCurve main = TimeCurrent.thermalMagnetic("400 A breaker", 400, Tuning.BREAKER_TRIP_SECONDS_AT_2X, Tuning.BREAKER_MAGNETIC_MULTIPLE, HZ);
		// a 30 A class J lets through 3.1 kA peak at 50 kA: 2.2 kA rms equivalent, under the 400 A breaker's 3.2 kA magnetic pickup
		TimeCurrent.Study s30 = TimeCurrent.study(classJ(30), main, 30, 50_000, HZ);
		assertTrue(s30.selectiveToRating(), s30.toString());
		// at 200 kA its let-through, 4.9 kA peak (3.4 kA rms), is over the breaker's 3.2 kA pickup: not selective up there
		TimeCurrent.Study s30full = TimeCurrent.study(classJ(30), main, 30, 200_000, HZ);
		assertFalse(s30full.selectiveToRating());
		assertTrue(s30full.notSelectiveAbove() > 100_000 && s30full.why().contains("let-through"), s30full.toString());
		TimeCurrent.Study s200 = TimeCurrent.study(classJ(200), main, 200, 200_000, HZ);
		assertFalse(s200.selectiveToRating());   // a 200 A time-delay fuse is slower than the breaker's thermal trip: never selective
	}

	/** A part with a fixed race time, for the race. */
	private record Timed(double seconds, double peak, double pickup, boolean[] acted) implements CircuitPart {
		@Override
		public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
			return false;
		}

		@Override
		public void build(Circuit circuit) {
		}

		@Override
		public void apply(Circuit circuit, int ticks) {
		}

		@Override
		public double clearingSeconds(Circuit circuit) {
			return seconds;
		}

		@Override
		public double letThroughPeak(Circuit circuit) {
			return peak;
		}

		@Override
		public double instantPickupAmps() {
			return pickup;
		}
	}

	@Test
	void onlyTheFastestDevicesActOnASolve() {
		Circuit circuit = new Circuit();
		double cycle = 1.0 / circuit.hertz();
		CircuitPart fuse = new Timed(0.3 * cycle, 9000, Double.POSITIVE_INFINITY, null);
		CircuitPart smallBreaker = new Timed(cycle, 0, 160, null);         // its 160 A pickup is under the fuse's 6.4 kA rms let-through
		CircuitPart bigBreaker = new Timed(cycle, 0, 16_000, null);        // over it: it holds
		CircuitPart idle = new Timed(Double.POSITIVE_INFINITY, 0, 160, null);
		CircuitPart untimed = new Timed(Double.NaN, 0, Double.POSITIVE_INFINITY, null);
		List<CircuitPart> acting = ProtectionRace.acting(List.of(fuse, smallBreaker, bigBreaker, idle, untimed), circuit);
		assertEquals(List.of(fuse, smallBreaker, untimed), acting);
		// with no fault anywhere every part runs its protect as before
		assertEquals(2, ProtectionRace.acting(List.of(idle, untimed), circuit).size());
	}
}
