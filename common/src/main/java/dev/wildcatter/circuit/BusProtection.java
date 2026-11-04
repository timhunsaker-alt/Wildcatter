package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * The generator bus's relay elements (docs/multiblocks/gen-bus-protection.md): the 87B bus differential, the 51V voltage-restrained
 * overcurrent on each generator breaker (IEEE C37.102) and the 50/51 on each feeder breaker, with the IEEE C37.112 inverse-time
 * curves. Plain Java, so it is unit-tested without Minecraft.
 */
public final class BusProtection {
	private BusProtection() {
	}

	/**
	 * 87B: true if {@code differenceAmps} (the current that enters the bus and does not leave it at a breaker, which by Kirchhoff
	 * flows into a fault on it) is over its percentage-restrained pickup: the larger of a floor at a share of the largest breaker's
	 * rating and a slope of the current through the bus's boundary.
	 */
	public static boolean differentialOperates(double differenceAmps, double boundaryAmps, double largestRating) {
		return differenceAmps > differentialPickup(boundaryAmps, largestRating);
	}

	public static double differentialPickup(double boundaryAmps, double largestRating) {
		return Math.max(Tuning.BUS_87_MIN_PICKUP * largestRating, Tuning.BUS_87_SLOPE * boundaryAmps);
	}

	/** 51V: the pickup for a generator of {@code fullLoadAmps} at {@code voltsPerUnit} of its rated voltage, restrained linearly. */
	public static double restrainedPickup(double fullLoadAmps, double voltsPerUnit) {
		return Tuning.GEN_51V_PICKUP * fullLoadAmps * Math.clamp(voltsPerUnit, Tuning.GEN_51V_FLOOR, 1.0);
	}

	/** IEEE C37.112 very inverse: seconds to trip at {@code m} times the pickup (infinite at or below it). */
	public static double veryInverseSeconds(double m, double timeDial) {
		return m <= 1.0 ? Double.POSITIVE_INFINITY : timeDial * (3.922 / (m * m - 1.0) + 0.0982);
	}

	/** IEEE C37.112 moderately inverse: seconds to trip at {@code m} times the pickup (infinite at or below it). */
	public static double moderatelyInverseSeconds(double m, double timeDial) {
		return m <= 1.0 ? Double.POSITIVE_INFINITY : timeDial * (0.0515 / (Math.pow(m, 0.02) - 1.0) + 0.114);
	}

	/** One inverse-time element: it runs towards its trip while over pickup and resets at once below it, as a digital relay does. */
	public static final class TimeElement {
		private final boolean veryInverse;
		private double timer;

		public TimeElement(boolean veryInverse) {
			this.veryInverse = veryInverse;
		}

		/** Runs for {@code seconds} at {@code amps} against {@code pickup}; true once it trips. */
		public boolean step(double amps, double pickup, double timeDial, double seconds) {
			double m = pickup <= 0 ? 0 : amps / pickup;
			if (m <= 1.0) {
				timer = 0;
				return false;
			}
			double toTrip = veryInverse ? veryInverseSeconds(m, timeDial) : moderatelyInverseSeconds(m, timeDial);
			timer += seconds / toTrip;
			return timer >= 1.0;
		}

		/** 0 to 1: how far it has run towards its trip. */
		public double progress() {
			return timer;
		}

		public void reset() {
			timer = 0;
		}
	}
}
