package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The reactor protection system: the trips, checked after each step, and their reset. */
final class ReactorProtection {
	private ReactorProtection() {
	}

	/** Trips the reactor on the first condition met, or resets a cleared trip. */
	static void trips(Reactor r, Reactor.Inputs in, double p, double dt) {
		boolean[] dT = deltaT(r, dt);
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
		} else if (dT[0] && !r.tripped) {
			r.tripNow(String.format("OT-DELTA-T (%.1f C over its %.1f C setpoint)", r.deltaT(), r.otSetpoint));
		} else if (dT[1] && !r.tripped) {
			r.tripNow(String.format("OP-DELTA-T (%.1f C over its %.1f C setpoint)", r.deltaT(), r.opSetpoint));
		} else if (r.full() && (r.thot > Tuning.TRIP_THOT_C || r.thot > Steam.tsat(mpa) - 3) && (r.shutdownBank > 0 || !r.tripped)) {
			r.tripNow("OVERTEMPERATURE");
		} else if ((in.loop[0] && r.sgLevel(0) < Tuning.SG_LOW_LOW) || (in.loop[1] && r.sgLevel(1) < Tuning.SG_LOW_LOW)) {
			r.tripNow("STEAM GENERATOR LOW-LOW LEVEL");
		} else if (r.tripped && Reactor.SETPOINTS[r.setpoint] >= 0 && !r.coreDamaged && !r.vesselFailed) {
			r.tripped = false;
			r.trip = "";
		}
	}

	/**
	 * 19a, with its rule: the overtemperature and overpower delta-T setpoints (NUREG-1431 Table 3.3.1-1 Notes 1 and 2, the constants of
	 * H. B. Robinson 2), worked out each step with their lead-lag and rate filters. Returns whether each is exceeded. The axial flux
	 * term f(delta-I) is left out: the model has no axial shape. Without the rule the setpoints are NaN and nothing trips.
	 */
	static boolean[] deltaT(Reactor r, double dt) {
		if (!ReactorRules.deltaTTrips || !r.full() || dt <= 0) {
			r.otLag = r.opLag = r.otSetpoint = r.opSetpoint = Double.NaN;
			return new boolean[2];
		}
		double x = r.tavg - Tuning.OT_TPRIME_C;
		if (Double.isNaN(r.otLag)) {
			r.otLag = x;   // a step after loading or switching the rule on is not a transient
			r.opLag = r.tavg;
		}
		// the lead-lag (1 + t1 s) / (1 + t2 s) = t1/t2 + (1 - t1/t2) / (1 + t2 s), exact over a step of any length
		r.otLag += (x - r.otLag) * (1 - Math.exp(-dt / Tuning.OT_TAU2_S));
		double lead = Tuning.OT_TAU1_S / Tuning.OT_TAU2_S;
		double leadLag = lead * x + (1 - lead) * r.otLag;
		r.otSetpoint = Tuning.OT_DT0 * (Tuning.OT_K1 - Tuning.OT_K2_PER_C * leadLag + Tuning.OT_K3_PER_MPA * (r.pressure() - Tuning.OT_PPRIME_MPA));
		// the rate term t3 s / (1 + t3 s) Tavg: Tavg less its lag, in C (counted only while Tavg rises)
		r.opLag += (r.tavg - r.opLag) * (1 - Math.exp(-dt / Tuning.OP_TAU3_S));
		double rate = Math.max(0, r.tavg - r.opLag);
		r.opSetpoint = Tuning.OT_DT0 * (Tuning.OP_K4 - Tuning.OP_K5_PER_C * rate - Tuning.OP_K6_PER_C * Math.max(0, x));
		double dT = r.deltaT();
		return new boolean[] {dT > r.otSetpoint, dT > r.opSetpoint};
	}
}
