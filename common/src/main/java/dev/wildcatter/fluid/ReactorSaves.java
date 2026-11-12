package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** Saving and loading {@link Reactor}'s state through its {@link Reactor.Store}. The keys are the saved format: do not rename them. */
final class ReactorSaves {
	private ReactorSaves() {
	}

	/** Writes the state. */
	static void save(Reactor r, Reactor.Store out) {
		for (int i = 0; i < r.burnup.length; i++) {
			out.put("burnup" + i, r.burnup[i]);
			out.put("assay" + i, r.assay[i]);
		}
		out.put("assemblies", r.assemblies);
		out.put("rccas", r.rccas);
		for (int i = 0; i < r.rccaKind.length; i++) {   // 19d
			if (r.rccaKind[i] != 0 || r.rccaB10[i] != 0) {
				out.put("rccaKind" + i, r.rccaKind[i]);
				out.put("rccaB10" + i, r.rccaB10[i]);
			}
		}
		out.put("shutdownBank", r.shutdownBank);
		out.put("bankD", r.bankD);
		out.put("setpoint", r.setpoint);
		out.put("tripped", r.tripped ? 1 : 0);
		out.put("waterKg", r.waterKg);
		out.put("ppm", r.ppm);
		out.put("targetPpm", r.targetPpm);
		out.put("tavg", r.tavg);
		out.put("pzrC", r.pzrC);
		for (int l = 0; l < 2; l++) {
			out.put("sgC" + l, r.sgC[l]);
			out.put("sgKg" + l, r.sgKg[l]);
		}
		out.put("power", r.kinetics.power());
		for (int i = 0; i < r.decay.length; i++) {
			out.put("decay" + i, r.decay[i]);
		}
		out.put("iodine", r.iodine);
		out.put("xenon", r.xenon);
		out.put("fuelC", r.fuelC);
		out.put("zrOxidized", r.zrOxidized);
		out.put("h2", r.h2);
		out.put("coreDamaged", r.coreDamaged ? 1 : 0);
		out.put("exploded", r.exploded ? 1 : 0);
		for (int i = 0; i < 6; i++) {
			out.put("precursor" + i, r.kinetics.precursors()[i]);
		}
		// 19a
		out.put("fatigue", r.fatigue);
		out.put("ltopArmed", r.ltopArmed ? 1 : 0);
		out.put("ptViolations", r.ptViolations);
		out.put("vesselFailed", r.vesselFailed ? 1 : 0);
		// 19b
		for (int i = 0; i < r.bent.length; i++) {
			out.put("bent" + i, r.bent[i] ? 1 : 0);
			out.put("leaking" + i, r.leaking[i] ? 1 : 0);
		}
		out.put("activity", r.activity);
	}

	/** Reads the state back, with the defaults for older saves. */
	static void load(Reactor r, Reactor.Store in) {
		for (int i = 0; i < r.burnup.length; i++) {
			r.burnup[i] = in.get("burnup" + i, 0);
			r.assay[i] = in.get("assay" + i, Tuning.ASSAY_PRODUCT);
		}
		r.assemblies = (int) in.get("assemblies", 0);
		r.rccas = (int) in.get("rccas", 0);
		for (int i = 0; i < r.rccaKind.length; i++) {
			r.rccaKind[i] = (int) in.get("rccaKind" + i, 0);
			r.rccaB10[i] = in.get("rccaB10" + i, 0);
		}
		r.shutdownBank = in.get("shutdownBank", 0);
		r.bankD = in.get("bankD", 0);
		r.setpoint = (int) in.get("setpoint", 0);
		r.tripped = in.get("tripped", 1) > 0.5;
		r.trip = r.tripped ? "shut down" : "";
		r.waterKg = in.get("waterKg", 0);
		r.ppm = in.get("ppm", 0);
		r.targetPpm = in.get("targetPpm", 1500);
		r.tavg = r.thot = r.tcold = in.get("tavg", 20);
		r.pzrC = in.get("pzrC", 20);
		for (int l = 0; l < 2; l++) {
			r.sgC[l] = in.get("sgC" + l, 20);
			r.sgKg[l] = in.get("sgKg" + l, Tuning.SG_WATER_KG);
		}
		double[] c = new double[6];
		for (int i = 0; i < 6; i++) {
			c[i] = in.get("precursor" + i, 0);
		}
		r.kinetics.load(in.get("power", 0), c);
		for (int i = 0; i < r.decay.length; i++) {
			r.decay[i] = in.get("decay" + i, 0);
		}
		r.iodine = in.get("iodine", 0);
		r.xenon = in.get("xenon", 0);
		r.fuelC = in.get("fuelC", r.tavg);
		r.zrOxidized = in.get("zrOxidized", 0);
		r.h2 = in.get("h2", 0);
		r.coreDamaged = in.get("coreDamaged", 0) > 0.5;
		r.exploded = in.get("exploded", 0) > 0.5;
		r.fatigue = (int) in.get("fatigue", 0);
		r.ltopArmed = in.get("ltopArmed", 0) > 0.5;
		r.ptViolations = (int) in.get("ptViolations", 0);
		r.vesselFailed = in.get("vesselFailed", 0) > 0.5;
		for (int i = 0; i < r.bent.length; i++) {
			r.bent[i] = in.get("bent" + i, 0) > 0.5;
			r.leaking[i] = in.get("leaking" + i, 0) > 0.5;
		}
		r.activity = in.get("activity", 0);
		if (r.coreDamaged) {
			r.trip = "CORE DAMAGE";
		} else if (r.vesselFailed) {
			r.trip = "VESSEL FAILURE";
		}
		if (in.get("precursor0", -1) < 0) {
			r.kinetics.hold(Tuning.NEUTRON_SOURCE / 0.05);
		}
	}
}
