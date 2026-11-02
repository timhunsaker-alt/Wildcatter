package dev.wildcatter.energy;

import dev.wildcatter.Tuning;

/**
 * How a conductor heats (approved by Tim, 2026-10-31): its temperature rise, as a multiple of its rated rise, follows the square of
 * its current as a multiple of its rating, with a time constant of {@link Tuning#WIRE_HEAT_SECONDS}. At its rating it settles at 1;
 * it burns out at {@link Tuning#WIRE_BURN_RISE}. So a motor's start (six times its current for two seconds) passes, a dead short
 * burns a wire in a fraction of a second, and twice the rating takes about half a minute. Plain Java, so it is unit-tested.
 */
public final class WireHeat {
	private WireHeat() {
	}

	/** The rise after {@code seconds} more at {@code ratio} times the rating, from {@code rise}. */
	public static double step(double rise, double ratio, double seconds) {
		double target = ratio * ratio;
		return target + (rise - target) * Math.exp(-seconds / Tuning.WIRE_HEAT_SECONDS);
	}

	public static boolean burnt(double rise) {
		return rise >= Tuning.WIRE_BURN_RISE;
	}

	/** Seconds a conductor starting at its rated rise lasts at {@code ratio} times its rating; infinite if it never burns. */
	public static double secondsToBurn(double ratio) {
		double target = ratio * ratio;
		if (target <= Tuning.WIRE_BURN_RISE) {
			return Double.POSITIVE_INFINITY;
		}
		return Tuning.WIRE_HEAT_SECONDS * Math.log((target - 1.0) / (target - Tuning.WIRE_BURN_RISE));
	}

	/** For a readout: how far past its rated temperature toward burning out, once it is past it. */
	public static String readout(double rise) {
		if (rise <= 1.02) {
			return "";
		}
		return ", HEATING " + Math.round(Math.min(1, (rise - 1) / (Tuning.WIRE_BURN_RISE - 1)) * 100) + "%";
	}
}
