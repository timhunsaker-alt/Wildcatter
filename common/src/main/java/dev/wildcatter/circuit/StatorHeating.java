package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * A generator stator's per-phase heating. Each phase winding is rated for a third of the machine's kVA; above that it heats on
 * the IEEE C50.13 short-time curve, (I² - 1)·t = 37.5 s in per unit (130 % for about a minute, 226 % for about 10 s), and the
 * machine trips when the hottest phase reaches it. Below rating it cools.
 */
public final class StatorHeating {
	private final double[] heat = new double[3];
	private int hottest;
	private double hottestRatio;

	/** The game test shrinks every rating by this factor so a few heaters can overload a 100 kW machine. Null in play. */
	public static volatile Double testRatingScale;

	/** The current one phase may carry: kVA (the rated kW at the nameplate power factor) over three phases, at the line-to-neutral voltage. */
	public static double ratedPhaseAmps(double ratedWatts, double voltsToNeutral) {
		return ratedWatts / Tuning.GENERATOR_RATED_PF / (3.0 * voltsToNeutral) * (testRatingScale != null ? testRatingScale : 1.0);
	}

	/** Heats or cools each phase for {@code seconds}; true once a phase has reached its limit. */
	public boolean step(double[] phaseAmps, double ratedPhaseAmps, double seconds) {
		boolean over = false;
		hottestRatio = 0;
		for (int k = 0; k < 3; k++) {
			double r = phaseAmps[k] / ratedPhaseAmps;
			if (r > 1.0) {
				heat[k] += (r * r - 1.0) * seconds / Tuning.STATOR_I2T_SECONDS;
			} else {
				heat[k] = Math.max(0, heat[k] - seconds / Tuning.STATOR_COOL_SECONDS);
			}
			if (r > hottestRatio) {
				hottestRatio = r;
			}
			if (heat[k] > heat[hottest]) {
				hottest = k;
			}
			over |= heat[k] >= 1.0;
		}
		return over;
	}

	/** 0 to 1: how close the hottest phase is to tripping. */
	public double level() {
		return heat[hottest];
	}

	/** The hottest phase: 0, 1, 2 for A, B, C. */
	public int hottestPhase() {
		return hottest;
	}

	/** The largest phase current as a multiple of rating, from the last step. */
	public double worstRatio() {
		return hottestRatio;
	}

	public void reset() {
		java.util.Arrays.fill(heat, 0);
	}
}
