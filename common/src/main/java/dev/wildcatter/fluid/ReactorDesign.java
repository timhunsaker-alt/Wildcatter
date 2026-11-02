package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The core's design numbers, from {@link Tuning}: ratings, flows, rod worth curves and coefficients. {@link Reactor} exposes them. */
final class ReactorDesign {
	private ReactorDesign() {
	}

	/** W of heat an assembly makes at full power: its rods' length at the average linear heat rate. */
	static double assemblyWatts() {
		return Tuning.ASSEMBLY_RODS * Tuning.ROD_STACK_M * Tuning.LINEAR_HEAT_KW_M * 1000;
	}

	/** W of heat the core makes at full power. */
	static double ratedWatts() {
		return Tuning.CORE_ASSEMBLIES * assemblyWatts();
	}

	/** GWd/t a second at full power, with the burnup speed-up. */
	static double burnupPerSecond() {
		return assemblyWatts() / 1e6 / (Fuel.assemblyU() / 1000) / 86400 / 1000 * Tuning.BURNUP_SPEEDUP;
	}

	/** Hours at full power of one cycle, a third of the discharge burnup. */
	static double cycleHours() {
		return Tuning.DISCHARGE_GWD_T / 3 / burnupPerSecond() / 3600;
	}

	/** kg/s of steam a steam generator makes at full power. */
	static double steamPerGenerator() {
		return ratedWatts() / 2 / (Steam.hg(Steam.psat(Tuning.TAVG_FULL_LOAD - ratedWatts() / 2 / Tuning.SG_UA_W_PER_C)) - Steam.hf(Tuning.FEEDWATER_C));
	}

	/** kg/s of coolant a loop carries for the core's rated delta-T. */
	static double loopFlow() {
		return ratedWatts() / 2 / (Steam.CP_COOLANT * Tuning.CORE_DT);
	}

	/** The rods' worth with the most reactive one (the centre) stuck out, less the power defect at full power: the shutdown margin. */
	static double shutdownMargin() {
		double rods = 4 * Tuning.ROD_WORTH_EDGE;
		double defect = -Tuning.DOPPLER_PER_C * Tuning.FUEL_RISE_C - Tuning.MTC_AT_0_PPM * (Tuning.TAVG_FULL_LOAD - Tuning.TAVG_NO_LOAD);
		return rods - defect;
	}

	/** The fraction of a rod's worth still in at {@code steps} withdrawn: the S-curve. */
	static double inserted(double steps) {
		double x = Math.max(0, Math.min(1, steps / Tuning.ROD_STEPS));
		return 1 - (x - Math.sin(2 * Math.PI * x) / (2 * Math.PI));
	}

	/** The moderator temperature coefficient at {@code ppm}, per C: linear between its two points, never positive. */
	static double mtc(double ppm) {
		return Math.min(0, Tuning.MTC_AT_0_PPM + (Tuning.MTC_AT_1500_PPM - Tuning.MTC_AT_0_PPM) * ppm / 1500);
	}

	/** Xenon at equilibrium at {@code power}, of its full-power level. */
	static double xenonEquilibrium(double power) {
		return (Tuning.XENON_LAMBDA + Tuning.XENON_BURNOUT) * power / (Tuning.XENON_LAMBDA + Tuning.XENON_BURNOUT * power);
	}
}
