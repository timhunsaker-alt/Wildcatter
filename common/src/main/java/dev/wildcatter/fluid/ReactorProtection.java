package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The reactor protection system: the trips, checked after each step, and their reset. */
final class ReactorProtection {
	private ReactorProtection() {
	}

	/** Trips the reactor on the first condition met, or resets a cleared trip. */
	static void trips(Reactor r, Reactor.Inputs in, double p) {
		// the trips
		double mpa = r.pressure();
		if (p > Tuning.P7) {
			r.lowRangeBlocked = true;
		} else if (p < Tuning.P7 * 0.9) {
			r.lowRangeBlocked = false;
		}
		if (!in.crdm && (r.shutdownBank > 0 || r.bankD > 0 || !r.tripped)) {
			r.tripNow("CRDM POWER LOST");
		} else if (p > Tuning.TRIP_FLUX) {
			r.tripNow("HIGH FLUX (109 %)");
		} else if (!r.lowRangeBlocked && p > Tuning.TRIP_FLUX_LOW) {
			r.tripNow("HIGH FLUX, LOW SETPOINT (25 %)");
		} else if (p > Tuning.P7 && r.flow < Tuning.TRIP_LOW_FLOW) {
			r.tripNow("LOW RCS FLOW");
		} else if (p > Tuning.P9 && in.turbineTripped) {
			r.tripNow("TURBINE TRIP (above P-9)");
		} else if (p > Tuning.P8 && (in.pump[0] < Tuning.TRIP_LOW_FLOW || in.pump[1] < Tuning.TRIP_LOW_FLOW)) {
			r.tripNow("REACTOR COOLANT PUMP LOST");
		} else if (p > Tuning.P7 && mpa < Tuning.TRIP_LOW_MPA) {
			r.tripNow("LOW PRESSURIZER PRESSURE");
		} else if (mpa > Tuning.TRIP_HIGH_MPA) {
			r.tripNow("HIGH PRESSURIZER PRESSURE");
		} else if (r.full() && (r.thot > Tuning.TRIP_THOT_C || r.thot > Steam.tsat(mpa) - 3) && (r.shutdownBank > 0 || !r.tripped)) {
			r.tripNow("OVERTEMPERATURE");
		} else if ((in.loop[0] && r.sgLevel(0) < Tuning.SG_LOW_LOW) || (in.loop[1] && r.sgLevel(1) < Tuning.SG_LOW_LOW)) {
			r.tripNow("STEAM GENERATOR LOW-LOW LEVEL");
		} else if (r.tripped && Reactor.SETPOINTS[r.setpoint] >= 0 && !r.coreDamaged) {
			r.tripped = false;
			r.trip = "";
		}
	}
}
