package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The chemical and volume control system: filling the RCS, borating and diluting toward the boron target. */
final class ReactorCvcs {
	private ReactorCvcs() {
	}

	/** What the CVCS does this step. */
	static void step(Reactor r, Reactor.Inputs in, double dt) {
		if (!in.cvcs) {
			r.cvcsState = "NOT BUILT";
			return;
		}
		if (in.charging <= 0) {
			r.cvcsState = "charging pump stopped";
			return;
		}
		double baPerPpmKg = 1e-6 / Tuning.B_IN_BORIC_ACID;   // kg of boric acid a r.ppm a kg of water
		if (!r.full()) {
			double kg = Math.min(Math.min(Tuning.RCS_FILL_KGS * in.charging * dt, in.water), Tuning.RCS_WATER_KG - r.waterKg);
			double acid = r.targetPpm * kg * baPerPpmKg;
			if (acid > in.boricAcid) {
				r.cvcsState = "NO BORIC ACID: filling waits for bags in its hopper";
				return;
			}
			if (kg <= 1e-9) {
				r.cvcsState = "NO WATER: filling waits for its makeup pump";
				return;
			}
			r.ppm = (r.ppm * r.waterKg + r.targetPpm * kg) / (r.waterKg + kg);
			r.waterKg += kg;
			r.boricAcidUsed = acid;
			r.waterUsed = kg;
			r.cvcsState = String.format("filling the RCS at %.0f kg/s, %.0f ppm: %.0f of %.0f t", kg / dt, r.targetPpm, r.waterKg / 1000, Tuning.RCS_WATER_KG / 1000);
			return;
		}
		double kg = Tuning.CVCS_KGS * in.charging * dt;
		if (r.ppm < r.targetPpm - 0.5) {
			double gain = Math.min((Tuning.BORIC_ACID_TANK_PPM - r.ppm) * kg / r.waterKg, r.targetPpm - r.ppm);
			double acid = gain * r.waterKg * baPerPpmKg;
			if (acid > in.boricAcid) {
				r.cvcsState = "NO BORIC ACID: put bags in its hopper";
				return;
			}
			r.ppm += gain;
			r.boricAcidUsed = acid;
			r.cvcsState = String.format("borating toward %.0f ppm", r.targetPpm);
		} else if (r.ppm > r.targetPpm + 0.5) {
			if (in.water < kg) {
				r.cvcsState = "NO WATER: diluting waits for its makeup pump";
				return;
			}
			double loss = Math.min(r.ppm * kg / r.waterKg, r.ppm - r.targetPpm);
			r.ppm -= loss;
			r.boricAcidUsed = -loss * r.waterKg * baPerPpmKg;   // recovered from the letdown
			r.waterUsed = kg;
			r.cvcsState = String.format("diluting toward %.0f ppm", r.targetPpm);
		} else {
			r.cvcsState = String.format("holding %.0f ppm", r.ppm);
		}
	}
}
