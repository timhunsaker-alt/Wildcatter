package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * Radiation dose (docs/multiblocks/fuel-handling.md): the strength of each kind of source at 1 m, inverse square, and the shielding
 * of the blocks along the line; the acute dose's repair and its effects. Doses in sieverts. Plain Java, so it is unit-tested without
 * Minecraft.
 */
public final class Radiation {
	private Radiation() {
	}

	/** Sv/h at 1 m of an irradiated assembly making {@code watts} of decay heat. */
	public static double assemblyAt1m(double watts) {
		return Tuning.SV_H_PER_MW_AT_1M * watts / 1e6;
	}

	/** Sv/h at {@code metres} from a source of {@code at1m}, through {@code water} blocks of water and {@code solid} other full blocks. */
	public static double rate(double at1m, double metres, int water, int solid) {
		double r = Math.max(1, metres);
		return at1m / (r * r) * Math.pow(Tuning.WATER_BLOCK_SHIELD, water) * Math.pow(Tuning.SOLID_BLOCK_SHIELD, solid);
	}

	/** The acute dose after {@code seconds} of repair. */
	public static double repaired(double acute, double seconds) {
		return acute * Math.pow(0.5, seconds / Tuning.ACUTE_HALF_LIFE_S);
	}

	/** The effects of an acute dose: 0 none, 1 nausea and weakness, 2 also hunger and slowness, 3 also wither, 4 lethal. */
	public static int stage(double acuteSv) {
		return acuteSv >= Tuning.SV_LETHAL ? 4 : acuteSv >= Tuning.SV_WITHER ? 3 : acuteSv >= Tuning.SV_SICK ? 2 : acuteSv >= Tuning.SV_NAUSEA ? 1 : 0;
	}

	/** A dose rate for a readout: µSv/h, mSv/h or Sv/h. */
	public static String rateText(double svh) {
		return svh >= 1 ? String.format("%.2f Sv/h", svh) : svh >= 1e-3 ? String.format("%.2f mSv/h", svh * 1e3) : String.format("%.2f µSv/h", svh * 1e6);
	}

	public static String doseText(double sv) {
		return sv >= 1 ? String.format("%.2f Sv", sv) : sv >= 1e-3 ? String.format("%.2f mSv", sv * 1e3) : String.format("%.1f µSv", sv * 1e6);
	}
}
