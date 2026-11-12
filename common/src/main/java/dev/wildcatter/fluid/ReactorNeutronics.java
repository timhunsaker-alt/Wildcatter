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
		double rods = worth(r, 0) * Reactor.inserted(r.bankD);
		for (int i = 1; i < r.rccas; i++) {
			rods += worth(r, i) * Reactor.inserted(r.shutdownBank);
		}
		double rho = fuel - rods + free(r);
		if (!r.full()) {
			rho = Math.min(rho, -0.05);   // no moderator
		}
		r.kinetics.step(rho, dt);
		return r.power();
	}

	/** 19d: RCCA {@code i}'s worth: the centre one (bank D) or an edge one (the shutdown bank), times its material's share. */
	static double worth(Reactor r, int i) {
		return i >= r.rccas ? 0 : (i == 0 ? Tuning.ROD_WORTH_CENTRE : Tuning.ROD_WORTH_EDGE) * r.rccaWorthShare(i);
	}

	/** Reactivity from boron, Doppler, moderator and xenon. */
	private static double free(Reactor r) {
		double tfuel = r.tavg + Tuning.FUEL_RISE_C * r.power();
		return Tuning.BORON_PER_PPM * r.ppm + Tuning.DOPPLER_PER_C * (tfuel - Tuning.TAVG_NO_LOAD) + Reactor.mtc(r.ppm) * (r.tavg - Tuning.TAVG_NO_LOAD)
				- Tuning.XENON_WORTH * r.xenon;
	}

	/**
	 * 19d: the shutdown margin of the RCCAs loaded, as ReactorDesign checks the design's: every RCCA but the one of highest worth, less the
	 * power defect from full power (Doppler and moderator), so it reads the same at any power. 1,300 pcm is the limit.
	 */
	static double rodShutdownMargin(Reactor r) {
		double all = 0;
		double stuck = 0;
		for (int i = 0; i < r.rccas; i++) {
			all += worth(r, i);
			stuck = Math.max(stuck, worth(r, i));
		}
		return all - stuck - ReactorDesign.powerDefect();
	}

	/** 19d: each B4C RCCA burns its B-10 at the power and the share of it that is inserted (the rule reactorRodWear). */
	static void burnRods(Reactor r, double p, double dt) {
		if (!dev.wildcatter.Rules.reactorRodWear() || p <= 0) {
			return;
		}
		for (int i = 0; i < r.rccas; i++) {
			if (r.rccaKind[i] == ControlRods.B4C) {
				double in = Reactor.inserted(i == 0 ? r.bankD : r.shutdownBank);
				r.rccaB10[i] = Math.min(1, r.rccaB10[i] + p * in * ControlRods.b10PerSecond() * dt);
			}
		}
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
		burnRods(r, p, dt);
	}
}
