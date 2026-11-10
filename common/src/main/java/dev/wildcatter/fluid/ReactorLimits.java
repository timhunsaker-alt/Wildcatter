package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * 19a's operating limits (docs/multiblocks/reactor-extensions.md), each with its rule (dev.wildcatter.Rules): the heat-up rate in plant time
 * and its excursions; the P-T curve's alarm, its violations and the vessel's brittle failure, with the coolant then pouring out.
 */
final class ReactorLimits {
	private ReactorLimits() {
	}

	static void step(Reactor r, Reactor.Inputs in, double dt, double before) {
		// the heat-up rate: below no-load temperature the game's heat-up is compressed, so its plant-time rate is the game's over that
		if (dev.wildcatter.Rules.reactorHeatupLimit() && r.full() && dt > 0) {
			double speed = before < Tuning.TAVG_NO_LOAD ? dev.wildcatter.Rules.heatupSpeedup() : 1;
			double plant = (r.tavg - before) / dt * 3600 / speed;
			r.heatupRate += (plant - r.heatupRate) * (1 - Math.exp(-dt / Tuning.HEATUP_FILTER_S));
			boolean over = r.tavg < Tuning.TAVG_NO_LOAD && r.heatupRate > Tuning.HEATUP_LIMIT_C_PER_H;
			if (over && !r.heatupAlarm) {
				r.fatigue++;
			}
			r.heatupAlarm = over;
		} else {
			r.heatupRate = 0;
			r.heatupAlarm = false;
		}

		// the P-T curve: an alarm over it, a count of each time, and the vessel failing over twice it
		if (dev.wildcatter.Rules.reactorLtop() && r.full() && !r.vesselFailed) {
			double limit = Reactor.ptLimit(r.tcold);
			boolean over = r.pressure() > limit;
			if (over && !r.ptAlarm) {
				r.ptViolations++;
			}
			r.ptAlarm = over;
			if (r.pressure() > Tuning.PT_FAIL_FACTOR * limit) {
				r.vesselFailed = true;
				r.ptAlarm = false;
				r.tripped = false;
				r.tripNow(String.format("VESSEL FAILURE (%.1f MPa at %.0f C, over twice the P-T curve)", r.pressure(), r.tcold));
			}
		} else if (!dev.wildcatter.Rules.reactorLtop()) {
			r.ptAlarm = false;
		}
		// a failed vessel empties into its building, whatever the rules are now
		if (r.vesselFailed && r.waterKg > 0) {
			double out = Math.min(r.waterKg, Tuning.LOCA_KGS * dt);
			r.waterKg -= out;
			r.locaKg += out;
			if (r.waterKg < 1) {
				r.pzrC = Math.min(r.pzrC, 100);
			}
		}
	}
}
