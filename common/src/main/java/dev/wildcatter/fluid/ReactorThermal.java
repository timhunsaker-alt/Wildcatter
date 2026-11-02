package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The reactor's thermal-hydraulics: the primary's heat balance, the RHR, the steam generators' secondary sides, the pressurizer, and an uncovered core's fuel heat-up, cladding failure and hydrogen. */
final class ReactorThermal {
	private ReactorThermal() {
	}

	/** Steps the heat balance over {@code dt} seconds at power {@code p}. */
	static void step(Reactor r, Reactor.Inputs in, double dt, double p, double prompt, double before, boolean anyPump) {
		// the primary's heat
		// each steam generator's heat over the step (J), worked out as the exponential approach of the two sides so a long step is stable
		double[] f = new double[2];
		double uncovered = r.assemblies == 0 ? 0 : Math.max(0, Math.min(1, (Tuning.CORE_UNCOVERED - r.waterKg / Tuning.RCS_WATER_KG) / 0.4));
		double heat = (p * (1 - prompt) + r.decayHeat() * (1 - uncovered)) * Reactor.ratedWatts();   // an uncovered core's share heats the fuel
		double sgHeat = 0;
		double[] q = new double[2];
		double speed = r.tavg < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1;
		int loops = (in.loop[0] ? 1 : 0) + (in.loop[1] ? 1 : 0);
		for (int l = 0; l < 2; l++) {
			f[l] = in.loop[l] ? Math.max(in.pump[l], Tuning.NATURAL_CIRCULATION) : 0;
			heat += in.pumpPower[l] * Tuning.RCP_WATTS * Tuning.RCP_HEAT;
			if (in.loop[l]) {
				double wet = Math.min(1, r.sgKg[l] / (0.3 * Tuning.SG_WATER_KG));
				double ua = Tuning.SG_UA_W_PER_C * Math.pow(f[l], 0.8) * wet;
				double primary = Tuning.RCS_HEAT_J_PER_C / speed / loops;
				double secondary = Tuning.SG_HEAT_J_PER_C / (r.sgC[l] < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1);
				double both = 1 / primary + 1 / secondary;
				q[l] = (r.tavg - r.sgC[l]) * (1 - Math.exp(-ua * both * dt)) / both;
				sgHeat += q[l];
			}
		}
		r.flow = (f[0] + f[1]) / 2;
		double loss = Tuning.RCS_LOSS_W * (r.tavg - 20) / (Tuning.TAVG_NO_LOAD - 20);
		// the RHR: its exchanger on the circulating water, holding cold shutdown
		r.rhrHeat = 0;
		if (in.rhrPump > 0 && r.rhrPermit() && r.tavg > Tuning.RHR_HOLD_C && in.rhrWaterKgs > 0) {
			double dT = Math.max(0, r.tavg - in.rhrWaterC);
			r.rhrHeat = Math.min(Tuning.RHR_UA_W_PER_C * dT, in.rhrWaterKgs * 4186 * dT * 0.8) * in.rhrPump;
		}
		boolean controlled = r.coolingDown();
		if (r.waterKg > 1) {
			double joules = (heat - loss) * dt - sgHeat;
			// the heat-up is compressed; so is the operators' cooldown (and the RHR's); a cool-down on its own is not
			r.tavg += joules * (joules > 0 || controlled ? speed : 1) / Tuning.RCS_HEAT_J_PER_C - r.rhrHeat * dt * speed / Tuning.RCS_HEAT_J_PER_C;
			// with more heat than goes out, the coolant boils off through the PORVs
			double tsat = Steam.tsat(r.pressure());
			if (r.tavg > tsat) {
				double boil = (r.tavg - tsat) * Tuning.RCS_HEAT_J_PER_C * Math.min(1, r.waterKg / Tuning.RCS_WATER_KG) / Tuning.RCS_LATENT_J_PER_KG;
				r.waterKg = Math.max(0, r.waterKg - boil);
				r.tavg = tsat;
			}
		}
		double dt33 = p * Reactor.ratedWatts() / (Math.max(r.flow, Tuning.NATURAL_CIRCULATION) * 2 * Reactor.loopFlow() * Steam.CP_COOLANT);
		r.thot = r.tavg + dt33 / 2;
		r.tcold = r.tavg - dt33 / 2;
		if (controlled) {
			if (Double.isNaN(r.cooldownC)) {
				r.cooldownC = Math.min(r.sgC[0], r.sgC[1]);
			}
			r.cooldownC = Math.max(Tuning.COOLDOWN_FLOOR_C, r.cooldownC - Tuning.COOLDOWN_C_PER_H / 3600 * Tuning.HEATUP_SPEEDUP * dt);
		} else {
			r.cooldownC = Double.NaN;
		}

		// the steam generators' secondary sides
		for (int l = 0; l < 2; l++) {
			r.vented[l] = 0;
			if (!in.loop[l]) {
				continue;
			}
			double mpa = Steam.psat(r.sgC[l]);
			double adv = controlled ? Steam.psat(r.cooldownC) : Tuning.SG_ADV_MPA;
			double vent = mpa > adv ? Math.min(Tuning.SG_ADV_KGS, Tuning.SG_ADV_KGS * (mpa - adv) / 0.1) * dt : 0;
			vent = Math.min(vent, r.sgKg[l]);
			r.vented[l] = vent;
			double out = Math.min(in.steamTaken[l] + vent, r.sgKg[l]);
			r.lastSteam[l] = out;
			double latent = Steam.hg(mpa) - Steam.hf(r.sgC[l]);
			double sub = Steam.hf(r.sgC[l]) - Steam.hf(Tuning.FEEDWATER_C);
			double sgSpeed = r.sgC[l] < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1;
			double joules = q[l] - out * latent - in.feed[l] * sub;
			r.sgC[l] += joules * (joules > 0 || controlled ? sgSpeed : 1) / Tuning.SG_HEAT_J_PER_C;
			r.sgKg[l] = Math.max(0, r.sgKg[l] + in.feed[l] - out);
		}

		// the pressurizer
		if (!in.pressurizer) {
			r.pzrC = Math.max(Math.min(r.pzrC, r.tavg), r.pzrC - 2 * dt);
		} else if (r.full()) {
			double pzrSpeed = r.tavg < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1;
			double gain = in.heaters * Tuning.PZR_HEATER_WATTS - Tuning.PZR_LOSS_W * (r.pzrC - 20) / (344.8 - 20);
			r.pzrC += gain * dt * (gain > 0 ? pzrSpeed : 1) / Tuning.PZR_HEAT_J_PER_C + Tuning.PZR_SURGE * (r.tavg - before);
			if (controlled && r.pressure() > r.cooldownMpa() + 0.2) {
				r.pzrC = Math.max(Steam.tsat(r.cooldownMpa()), r.pzrC - Tuning.PZR_SPRAY_C_PER_S * pzrSpeed * dt);   // auxiliary spray off the charging pump
			} else if (r.pressure() > Tuning.PZR_SPRAY_MPA && anyPump) {
				r.pzrC = Math.max(r.tcold, r.pzrC - Tuning.PZR_SPRAY_C_PER_S * dt);
			}
			r.pzrC = Math.min(r.pzrC, Steam.tsat(Tuning.PORV_MPA));
		} else {
			r.pzrC = r.tavg;
		}

		// an uncovered core: its share of the r.decay heat goes into the fuel, and over 1,200 C the cladding fails and burns in the steam
		if (uncovered > 0) {
			r.fuelC = Math.max(r.fuelC, r.tavg);
			double rate = r.fuelC > 900 ? Math.min(0.05, 1e-3 * Math.exp((r.fuelC - Tuning.CLAD_FAIL_C) / 100)) : 0;
			double zr = rate * (Tuning.CORE_ZR_KG - r.zrOxidized) * dt * uncovered;
			r.zrOxidized += zr;
			r.h2 += zr * Tuning.H2_PER_ZR;
			r.fuelC += (uncovered * r.decayHeat() * Reactor.ratedWatts() * dt + zr * Tuning.ZR_OXIDATION_J_PER_KG
					- Tuning.FUEL_STEAM_COOLING_W_PER_C * (r.fuelC - r.tavg) * dt) / Tuning.FUEL_HEAT_J_PER_C;
		} else {
			r.fuelC = r.tavg + Tuning.FUEL_RISE_C * p;
		}
		if (!r.coreDamaged && r.fuelC >= Tuning.CLAD_FAIL_C) {
			r.coreDamaged = true;
			r.tripped = false;
			r.tripNow("CORE DAMAGE (the cladding failed at 1,200 C)");
		}
		if (!r.exploded && r.h2 >= Tuning.H2_IGNITE_KG) {
			r.exploded = true;
			r.blast = true;
		}
	}
}
