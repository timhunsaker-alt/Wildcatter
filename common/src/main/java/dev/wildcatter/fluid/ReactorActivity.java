package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * 19b (docs/multiblocks/reactor-extensions.md): a bent grid loaded uninspected becomes a leaking rod after a third of its cycle; with
 * the contamination rule the coolant carries an activity (µCi/g dose-equivalent I-131) from activated corrosion products and the
 * leakers, which the CVCS's demineralizer takes out.
 */
final class ReactorActivity {
	private ReactorActivity() {
	}

	/** GWd/t a fresh assembly gains in a third of a cycle at full power. */
	static double thirdOfCycle() {
		return Reactor.burnupPerSecond() * Reactor.cycleHours() * 3600 / 3;
	}

	static void step(Reactor r, Reactor.Inputs in, double p, double dt) {
		for (int i = 0; i < r.assemblies; i++) {
			if (r.bent[i] && !r.leaking[i] && r.burnup[i] >= thirdOfCycle()) {
				r.leaking[i] = true;   // fretting at the bent grid wears through a rod's cladding
			}
		}
		if (!ReactorRules.contamination || r.waterKg < 1) {
			return;
		}
		double power = Math.max(0, Math.min(1.2, p));
		double source = (Tuning.CORROSION_UCI_G_S + Tuning.LEAKER_UCI_G_S * r.leakers()) * power
				+ Tuning.LEAKER_UCI_G_S * Tuning.LEAKER_SPIKE * r.leakers() * Math.min(1, r.decayHeat() / 0.01);   // the iodine spike after a trip
		double removal = Tuning.IODINE_131_LAMBDA + (in.demineralizer ? Tuning.CVCS_KGS / Tuning.RCS_WATER_KG * Tuning.DEMIN_EFFICIENCY : 0);
		// the exact step of dA/dt = S - lambda A
		double eq = source / removal;
		r.activity = eq + (r.activity - eq) * Math.exp(-removal * dt);
	}
}
