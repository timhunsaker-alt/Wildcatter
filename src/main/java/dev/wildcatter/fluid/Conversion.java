package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The rates of the quarter-size conversion plant (docs/multiblocks/conversion.md). Plain Java, so it is unit-tested without Minecraft. */
public final class Conversion {
	private Conversion() {
	}

	/** kg of F2 a second from the cell house at full current. */
	public static double fluorineKgs() {
		return Tuning.F2_CELLS * Tuning.F2_CELL_KA * Tuning.F2_KG_PER_KAH * Tuning.F2_CURRENT_EFFICIENCY / 3600;
	}

	/** kg of U a second the plant converts: what its fluorine allows. */
	public static double uraniumKgs() {
		return fluorineKgs() / Tuning.CONV_F2_PER_U;
	}

	/** The cell house's DC volts and watts at full current. */
	public static double dcVolts() {
		return Tuning.F2_CELLS * Tuning.F2_CELL_VOLTS;
	}

	public static double dcWatts() {
		return dcVolts() * Tuning.F2_CELL_KA * 1000;
	}

	/** What the rectifier takes from the AC side at full current. */
	public static double acWatts() {
		return dcWatts() / Tuning.RECTIFIER_EFFICIENCY;
	}

	/** m3 of H2 a kg of U. */
	public static double hydrogenM3PerU() {
		return Tuning.CONV_H2_PER_U / Tuning.H2_KG_PER_M3;
	}

	/** kg of anhydrous HF a kg of U, the hydrofluorinator's and the cells' together. */
	public static double hfPerU() {
		return Tuning.CONV_HF_PER_U + Tuning.CONV_F2_PER_U * Tuning.F2_HF_PER_F2;
	}

	/** Hours to fill a 48Y cylinder at the plant's rate. */
	public static double hoursPerCylinder() {
		return Tuning.CYLINDER_48Y_KG / Tuning.UF6_PER_U / uraniumKgs() / 3600;
	}

	/** Joules of fuel a m3 of H2 the reformer burns and reforms (natural gas, or LPG at the same heat). */
	public static double reformerJoulesPerM3() {
		return Tuning.SMR_GAS_PER_H2 * Tuning.GAS_JOULES_PER_M3;
	}
}
