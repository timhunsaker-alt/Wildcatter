package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * Time-current curves of the low-voltage and fused gear, for the protection race and the coordination study (docs/questions/
 * protection-gaps.md, C-entries). Plain Java, so it is unit-tested without Minecraft.
 * <ul>
 * <li>A thermal-magnetic breaker or a fuse heats on I²t over its rating: at a steady {@code r} times its rating it opens after
 * {@code 3 x T2x / (r² - 1)} seconds, {@code T2x} being its time at twice its rating (the curve every part already runs).</li>
 * <li>Above its instant multiple a breaker's magnetic trip or a fuse's melt clears in {@link Tuning#MAGNETIC_CLEAR_CYCLES} cycles.</li>
 * <li>A current-limiting fuse (class J, E-rated) above its threshold clears inside the first half cycle and lets through only a peak
 * that grows with the cube root of the prospective current, the shape of every maker's let-through chart.</li>
 * </ul>
 */
public final class TimeCurrent {
	private TimeCurrent() {
	}

	/** Seconds of one cycle at {@code hertz}. */
	public static double cycle(double hertz) {
		return 1.0 / hertz;
	}

	/** The thermal part: seconds to open at a steady {@code amps} on a {@code rating} with {@code secondsAt2x}; infinite at or under 1 x. */
	public static double thermalSeconds(double amps, double rating, double secondsAt2x) {
		double r = amps / rating;
		return r <= 1.0 ? Double.POSITIVE_INFINITY : 3.0 * secondsAt2x / (r * r - 1.0);
	}

	/** A thermal-magnetic breaker, or a fuse that is not current-limiting (an expulsion link, a plug fuse). */
	public static TripCurve thermalMagnetic(String name, double rating, double secondsAt2x, double instantMultiple, double hertz) {
		return new TripCurve() {
			@Override
			public String curveName() {
				return name;
			}

			@Override
			public double clearingSeconds(double amps) {
				if (amps > instantMultiple * rating) {
					return Tuning.MAGNETIC_CLEAR_CYCLES * cycle(hertz);
				}
				return thermalSeconds(amps, rating, secondsAt2x);
			}
		};
	}

	/** A fuse that limits the current above its threshold (UL 248-8 class J; ANSI C37.46 E-rated current-limiting). */
	public static final class CurrentLimiting implements TripCurve {
		private final String name;
		private final double rating;
		private final double secondsAt2x;
		private final double threshold;
		private final double hertz;

		public CurrentLimiting(String name, double rating, double secondsAt2x, double instantMultiple, double hertz) {
			this.name = name;
			this.rating = rating;
			this.secondsAt2x = secondsAt2x;
			this.threshold = instantMultiple * rating;
			this.hertz = hertz;
		}

		@Override
		public String curveName() {
			return name;
		}

		/** The prospective current (rms) above which it limits. */
		public double thresholdAmps() {
			return threshold;
		}

		public boolean limiting(double amps) {
			return amps > threshold;
		}

		/** Inside the first half cycle above its threshold, sooner the heavier the fault; the I²t curve below it. */
		@Override
		public double clearingSeconds(double amps) {
			if (limiting(amps)) {
				return Tuning.CL_CLEAR_CYCLES * cycle(hertz) * Math.cbrt(threshold / amps);
			}
			return thermalSeconds(amps, rating, secondsAt2x);
		}

		/**
		 * The peak current it lets through on a fault of {@code prospective} amps rms: the fault's own peak (√2 x, symmetrical) up to the
		 * threshold, then √2 x threshold x (prospective / threshold)^(1/3). For a 60 A class J this gives 7.7 kA at 200 kA (UL 248-8
		 * allows 8 kA), for 600 A 35.8 kA (35 kA).
		 */
		public double letThroughPeak(double prospective) {
			if (!limiting(prospective)) {
				return Math.sqrt(2) * prospective;
			}
			return Math.sqrt(2) * threshold * Math.cbrt(prospective / threshold);
		}

		/** The let-through I²t (A²s) of a fault it limits: a triangular pulse of the let-through peak over its clearing time. */
		public double letThroughI2t(double prospective) {
			if (!limiting(prospective)) {
				double t = clearingSeconds(prospective);
				return prospective * prospective * t;
			}
			double peak = letThroughPeak(prospective);
			return peak * peak * clearingSeconds(prospective) / 3.0;
		}
	}

	/** The result of comparing a device with the one upstream of it. */
	public record Study(boolean selectiveToRating, double notSelectiveAbove, String why) {
	}

	/**
	 * The coordination study of {@code down} under {@code up}, from just over the downstream's pickup to {@code maxAmps} (its
	 * interrupting rating). Selective at a current if the upstream clears more than {@link Tuning#COORDINATION_MARGIN_CYCLES} later than
	 * the downstream, the same rule the protection race uses. Under a current-limiting fuse in its limiting range the upstream sees only
	 * the let-through peak's rms equivalent (peak / √2), and is not selective if that is in its instant range.
	 */
	public static Study study(TripCurve down, TripCurve up, double downRating, double maxAmps, double hertz) {
		double margin = Tuning.COORDINATION_MARGIN_CYCLES * cycle(hertz);
		double start = Math.max(1.0, downRating * 1.05);
		int steps = 400;
		double factor = Math.pow(maxAmps / start, 1.0 / steps);
		double amps = start;
		for (int k = 0; k <= steps; k++, amps *= factor) {
			double tDown = down.clearingSeconds(amps);
			if (Double.isInfinite(tDown)) {
				continue;
			}
			boolean limited = down instanceof CurrentLimiting cl0 && cl0.limiting(amps);
			double seen = limited ? Math.min(amps, ((CurrentLimiting) down).letThroughPeak(amps) / Math.sqrt(2)) : amps;
			double tUp = up.clearingSeconds(seen);
			// under a limiting fuse the upstream device trips if the let-through reaches its instant range: it unlatches on the peak
			boolean trips = limited ? tUp <= Tuning.MAGNETIC_CLEAR_CYCLES * cycle(hertz) : tUp <= tDown + margin;
			if (trips) {
				String why = down instanceof CurrentLimiting cl && cl.limiting(amps)
						? String.format("its let-through (%,.0f A peak) trips the %s at once", cl.letThroughPeak(amps), up.curveName())
						: Double.isInfinite(tUp) ? "" : String.format("the %s clears as fast (%.3f s against %.3f s)", up.curveName(), tUp, tDown);
				return new Study(false, amps, why);
			}
		}
		return new Study(true, maxAmps, "");
	}

	/** "selective up to its 10 kA rating" or "selective up to 1,600 A; above that ...". */
	public static String describe(Study study, double maxAmps) {
		if (study.selectiveToRating()) {
			return String.format("selective up to its %s rating", kiloamps(maxAmps));
		}
		return String.format("selective only up to %,.0f A: above that %s", study.notSelectiveAbove(), study.why());
	}

	private static String kiloamps(double amps) {
		return amps >= 1000 ? String.format("%,.0f kA", amps / 1000) : String.format("%,.0f A", amps);
	}
}
