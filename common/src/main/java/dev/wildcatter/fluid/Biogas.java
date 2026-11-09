package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * Biogas in a gas set (docs/biogas.md): 60 % methane, 23 MJ/m3; the engine derated to 850 kW at a heat rate of 11.0 MJ/kWh, burning
 * 15 % of its full-load fuel at no load, as on natural gas. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Biogas {
	private Biogas() {
	}

	/** m3/h of biogas at the derated full load: 850 kW x 11.0 MJ/kWh / 23 MJ/m3, about 407. */
	public static double fullLoadM3h() {
		return Tuning.GENSET_BIOGAS_WATTS / 1000 * Tuning.BIOGAS_HEAT_RATE_MJ_KWH * 1e6 / Tuning.BIOGAS_JOULES_PER_M3;
	}

	/** m3/h at {@code share} of the derated rating, on genset.md's part-load line. */
	public static double fuelM3h(double share) {
		double s = Math.max(0, Math.min(1, share));
		return fullLoadM3h() * (Tuning.GENSET_NO_LOAD_FUEL + (1 - Tuning.GENSET_NO_LOAD_FUEL) * s);
	}

	/** kWh of electricity a m3 of biogas makes at full load. */
	public static double kwhPerM3() {
		return Tuning.BIOGAS_JOULES_PER_M3 / 1e6 / Tuning.BIOGAS_HEAT_RATE_MJ_KWH;
	}

	/** The derate: the biogas rating as a share of the natural gas one. */
	public static double derate() {
		return Tuning.GENSET_BIOGAS_WATTS / Tuning.GENSET_GAS_WATTS;
	}

	/** A firebox's ticks of fire in a m3 of biogas: a natural gas m3's, by heating value. */
	public static double fireboxTicksPerM3() {
		return Tuning.GAS_FUEL_TICKS_PER_UNIT * Tuning.BIOGAS_JOULES_PER_M3 / Tuning.GAS_JOULES_PER_M3;
	}
}
