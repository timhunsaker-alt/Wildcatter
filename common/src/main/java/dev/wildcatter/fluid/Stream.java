package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * What comes up a well (docs/multiblocks/lease.md): an oil well's water cut and gas, and a gas well's condensate and water. Plain Java,
 * so it is unit-tested without Minecraft.
 */
public final class Stream {
	private Stream() {
	}

	/** An amount of well stream after separation: litres of oil and of water, m3 of gas. */
	public record Split(double oil, double water, double gas) {
		public static final Split NONE = new Split(0, 0, 0);

		public Split plus(Split other) {
			return new Split(oil + other.oil, water + other.water, gas + other.gas);
		}

		public Split times(double k) {
			return new Split(oil * k, water * k, gas * k);
		}

		public double liquid() {
			return oil + water;
		}
	}

	/** An oil field's water cut, {@code left} of {@code start} units still in it: 10 % new, rising in a straight line to 80 % drained. */
	public static double waterCut(double left, double start) {
		double drained = start <= 0 ? 1 : Math.max(0, Math.min(1, 1 - left / start));
		return Tuning.WATER_CUT_NEW + (Tuning.WATER_CUT_DEPLETED - Tuning.WATER_CUT_NEW) * drained;
	}

	/** {@code liquid} litres lifted at this water cut, split into oil, water and the gas that comes out of the oil. */
	public static Split oilWell(double liquid, double waterCut) {
		double oil = liquid * (1 - waterCut);
		return new Split(oil, liquid - oil, oil * Tuning.GAS_OIL_RATIO);
	}

	/** {@code gas} m3 from a gas well, with its condensate (as oil) and water. */
	public static Split gasWell(double gas) {
		return new Split(gas * Tuning.CONDENSATE_PER_M3, gas * Tuning.GAS_WELL_WATER_PER_M3, gas);
	}
}
