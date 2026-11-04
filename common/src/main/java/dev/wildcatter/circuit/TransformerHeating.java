package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * A rated transformer's short-time heating (docs/ELECTRICAL_MODEL.md, "Transformers"; IEEE C57.109 through-fault duration): it
 * fails when (I² − 1)·t reaches {@value Tuning#TRANSFORMER_I2T_PU2S} pu²·s, I being the most loaded winding's current over its
 * rated current. 150 % lasts about 17 minutes, 200 % about 7, a 6 x motor inrush 36 s and 25 x 2 s. At or below its rating it
 * cools back from its limit in {@value Tuning#TRANSFORMER_COOL_SECONDS} s. Plain Java, so it is unit-tested without Minecraft.
 */
public final class TransformerHeating {
	private double heat;
	private double ratio;

	/** Heats or cools for {@code seconds} with the most loaded winding at {@code perUnit} of its rated current; true once it fails. */
	public boolean step(double perUnit, double seconds) {
		ratio = perUnit;
		if (perUnit > 1.0) {
			heat += (perUnit * perUnit - 1.0) * seconds / Tuning.TRANSFORMER_I2T_PU2S;
		} else {
			heat = Math.max(0, heat - seconds / Tuning.TRANSFORMER_COOL_SECONDS);
		}
		return heat >= 1.0;
	}

	/** 0 to 1: how far it has gone towards failing. */
	public double level() {
		return heat;
	}

	/** The last step's current as a multiple of rating. */
	public double ratio() {
		return ratio;
	}

	/** Seconds left before it fails at the last step's current, or infinity at or below its rating. */
	public double secondsToFail() {
		return secondsToFail(ratio, heat);
	}

	/** Seconds from {@code heat} to failing at {@code perUnit} of rated current; infinity at or below its rating. */
	public static double secondsToFail(double perUnit, double heat) {
		if (perUnit <= 1.0) {
			return Double.POSITIVE_INFINITY;
		}
		return Math.max(0, 1.0 - heat) * Tuning.TRANSFORMER_I2T_PU2S / (perUnit * perUnit - 1.0);
	}

	public void reset() {
		heat = 0;
	}
}
