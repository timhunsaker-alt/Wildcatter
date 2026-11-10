package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The reactor's neutronics: reactivity, point kinetics, decay heat, iodine and xenon, and burnup. Steps {@link Reactor}'s state. */
final class ReactorNeutronics {
	private ReactorNeutronics() {
	}

	/** Reactivity from fuel, rods, boron, Doppler, moderator and xenon; steps the kinetics and returns the new power. */
	static double kinetics(Reactor r, double dt) {
		// reactivity and r.kinetics
		double fuel = 0;
		for (int i = 0; i < r.assemblies; i++) {
			fuel += Enrichment.freshRho(r.assay[i]) - Tuning.ASSEMBLY_RHO_PER_GWD * r.burnup[i];   // 16c1: its assay
		}
		fuel = r.assemblies == 0 ? -1 : fuel / Tuning.CORE_ASSEMBLIES - (Tuning.CORE_ASSEMBLIES - r.assemblies) * 0.05;
		double centre = r.rccas > 0 ? Tuning.ROD_WORTH_CENTRE : 0;
		double edges = Math.max(0, r.rccas - 1) * Tuning.ROD_WORTH_EDGE;
		double rods = centre * Reactor.inserted(r.bankD) + edges * Reactor.inserted(r.shutdownBank);
		double tfuel = r.tavg + Tuning.FUEL_RISE_C * r.power();
		double rho = fuel - rods + Tuning.BORON_PER_PPM * r.ppm + Tuning.DOPPLER_PER_C * (tfuel - Tuning.TAVG_NO_LOAD)
				+ Reactor.mtc(r.ppm) * (r.tavg - Tuning.TAVG_NO_LOAD);
		rho -= Tuning.XENON_WORTH * r.xenon;
		if (!r.full()) {
			rho = Math.min(rho, -0.05);   // no moderator
		}
		r.kinetics.step(rho, dt);
		return r.power();
	}

	/** Decay heat groups, iodine and xenon follow the fissions; returns the prompt share of the decay heat. */
	static double decayAndXenon(Reactor r, double p, double dt) {
		// r.decay heat, r.iodine and r.xenon follow the fissions (at real speed: the r.burnup speed-up is not theirs)
		double prompt = 0;
		for (int i = 0; i < r.decay.length; i++) {
			double e = Math.exp(-Tuning.DECAY_LAMBDA[i] * dt);
			r.decay[i] = r.decay[i] * e + Tuning.DECAY_SHARE[i] * p * (1 - e);
			prompt += Tuning.DECAY_SHARE[i];
		}
		double made = Tuning.XENON_LAMBDA + Tuning.XENON_BURNOUT;   // r.xenon made a second at full power, of its equilibrium
		r.iodine += (Tuning.IODINE_LAMBDA * p - Tuning.IODINE_LAMBDA * r.iodine) * dt;
		r.xenon += (made * Tuning.XENON_DIRECT * p + made * (1 - Tuning.XENON_DIRECT) * r.iodine - (Tuning.XENON_LAMBDA + Tuning.XENON_BURNOUT * p) * r.xenon) * dt;
		return prompt;
	}

	/** Burnup of each loaded assembly. */
	static void burn(Reactor r, double p, double dt) {
		// r.burnup
		for (int i = 0; i < r.assemblies; i++) {
			r.burnup[i] += p * Reactor.burnupPerSecond() * dt;
		}
	}
}
