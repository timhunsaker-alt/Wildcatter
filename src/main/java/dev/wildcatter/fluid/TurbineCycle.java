package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The turbine island's numbers (docs/multiblocks/turbine-island.md): the steam rate, the condenser's vacuum from its circulating
 * water, the heat rejected and the water it needs. Plain Java, so it is unit-tested without Minecraft.
 */
public final class TurbineCycle {
	private TurbineCycle() {
	}

	private static final double WATER_CP = 4186.0;

	/** W of electricity {@code steamKgs} of main steam makes at the design vacuum. */
	public static double designWatts(double steamKgs) {
		return steamKgs * 3600 / Tuning.STEAM_KG_PER_KWH_PWR * 1000;
	}

	/** The condensing temperature with {@code cwFraction} of the design circulating water flowing, C. */
	public static double condensingC(double cwFraction) {
		return Tuning.CW_INLET_C + Tuning.CW_RISE_C / Math.max(cwFraction, 0.05) + Tuning.CONDENSER_TTD_C;
	}

	/** The condenser's absolute pressure, kPa. */
	public static double backPressureKpa(double cwFraction) {
		return Steam.psat(Math.min(condensingC(cwFraction), 370)) * 1000;
	}

	/** The condenser's absolute pressure, kPa, rejecting {@code watts} into {@code cwKgs} of circulating water coming in at 20 C. */
	public static double backPressureKpa(double watts, double cwKgs) {
		return backPressureKpa(watts, cwKgs, Tuning.CW_INLET_C);
	}

	/** The same with the water coming in at {@code inC} (a cooling tower's cold water, 8c). */
	public static double backPressureKpa(double watts, double cwKgs, double inC) {
		return Steam.psat(Math.min(inC + rise(watts, cwKgs) + Tuning.CONDENSER_TTD_C, 370)) * 1000;
	}

	/** C the circulating water warms through the condenser. */
	public static double rise(double watts, double cwKgs) {
		return watts / (WATER_CP * Math.max(cwKgs, 1));
	}

	/** The share of the design output at {@code kpa} back pressure. */
	public static double vacuumFactor(double kpa) {
		return Math.max(0, Math.min(1, 1 - Tuning.VACUUM_LOSS_PER_KPA * (kpa - backPressureKpa(1))));
	}

	/** J/kg the steam carries in over the feedwater it comes back as. */
	public static double steamHeat() {
		return Steam.hg(Tuning.STEAM_HEADER_MPA) - Steam.hf(Tuning.FEEDWATER_C);
	}

	/** W the condenser must reject for {@code steamKgs} turned into {@code electricWatts}. */
	public static double rejectedWatts(double steamKgs, double electricWatts) {
		return Math.max(0, steamKgs * steamHeat() - electricWatts);
	}

	/** kg/s of circulating water that carries {@code watts} at the design rise. */
	public static double cwNeeded(double watts) {
		return watts / (WATER_CP * Tuning.CW_RISE_C);
	}

	/** kW of hydraulic power a pump gives {@code kgs} of water through {@code mpa}, at 80 % efficiency. */
	public static double pumpKw(double kgs, double mpa) {
		return kgs / 1000 * mpa * 1000 / 0.8;
	}
}
