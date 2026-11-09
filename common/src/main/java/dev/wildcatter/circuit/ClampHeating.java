package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * A grounding clamp set's heating (ASTM F855, grade 3: 2/0 copper): the I²t a fault drives through the most loaded clamp, against the
 * cluster's ultimate rating. A fault cleared on the solve it appeared in still flows for the relay's and breaker's clearing time;
 * one left on flows for as long as it lasts. Below the cable's continuous rating it cools back. Plain Java, so it is unit-tested.
 */
public final class ClampHeating {
	/** The game test's lowered rating in A²s, or null for the real one. */
	public static volatile Double testLimit;

	private double used;
	private double peak;

	public static double limit() {
		return testLimit != null ? testLimit : Tuning.CLAMP_ULTIMATE_I2T;
	}

	/** Every solve, faulted or not: the clamp current seen, so a fault a breaker clears before the final solve is still counted. */
	public void seen(double amps) {
		peak = Math.max(peak, amps);
	}

	/**
	 * After the final solve: {@code amps} still flowing, for {@code seconds}; {@code clearingSeconds} is how long a fault cleared in
	 * this solve flowed before its breaker opened. True once the clamps have burned off.
	 */
	public boolean step(double amps, double seconds, double clearingSeconds) {
		double continuous = Tuning.CLAMP_CONTINUOUS_AMPS;
		if (peak > continuous && peak > 1.5 * amps) {
			used += peak * peak * clearingSeconds;   // cleared in this solve: it flowed for the clearing time
		}
		peak = 0;
		if (amps > continuous) {
			used += (amps * amps - continuous * continuous) * seconds;
		} else {
			used = Math.max(0, used - limit() * seconds / Tuning.CLAMP_COOL_SECONDS);
		}
		return used >= limit();
	}

	/** 0 to 1 and over: the share of the rating used. */
	public double share() {
		return used / limit();
	}

	public void reset() {
		used = 0;
		peak = 0;
	}
}
