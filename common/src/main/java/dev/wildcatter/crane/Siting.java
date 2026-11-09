package dev.wildcatter.crane;

/**
 * Where a crane may work (docs/multiblocks/tower-crane.md, crawler-crane.md, ring-crane.md, "Wind", "Ground bearing", "Collision",
 * "Relocation"): the wind at its jib or boom head against its limit, the ground under its crawlers or ring against its pressure, and
 * the ring crane's travel on its rails. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Siting {
	private Siting() {
	}

	// ---- wind (docs/questions/plant-crane-gaps.md, 1) --------------------------------------------------------------------------
	/** Mean wind 10 m up in fair weather, in rain and in a thunderstorm, m/s (Beaufort 3, 5 and 8; proposed). */
	public static final double CALM_M_S = 4.0, RAIN_M_S = 8.0, STORM_M_S = 18.0;
	/** The power-law exponent of the wind's rise with height over open ground (the 1/7 law). */
	public static final double WIND_EXPONENT = 1.0 / 7.0;

	/** The wind 10 m up, m/s, with the world's rain and thunder levels, each 0 to 1 (thunder is already scaled by rain). */
	public static double windAt10(double rain, double thunder) {
		double r = Math.clamp(rain, 0, 1), t = Math.clamp(thunder, 0, 1);
		return CALM_M_S + (RAIN_M_S - CALM_M_S) * r + (STORM_M_S - RAIN_M_S) * t;
	}

	/** The wind {@code height} m above the ground, from the wind 10 m up, m/s. */
	public static double windAt(double at10, double height) {
		return at10 * Math.pow(Math.max(height, 10) / 10, WIND_EXPONENT);
	}

	// ---- ground bearing (docs/questions/plant-crane-gaps.md, 2) -----------------------------------------------------------------
	/**
	 * What a crane's crawlers or ring stand on, with its allowable bearing (IBC Table 1806.2's presumptive load-bearing values,
	 * psf to t/m2 at 4.882 kg/m2 a psf).
	 */
	public enum Ground {
		/** Crystalline rock, concrete, a built pad or a crane rail on its beam: 12,000 psf. */
		ROCK("rock or concrete", 58.6),
		/** Sedimentary rock and compacted fill: 4,000 psf. */
		SEDIMENTARY("sedimentary rock or fill", 19.5),
		/** Sandy gravel and gravel: 3,000 psf. */
		GRAVEL("gravel", 14.6),
		/** Sand, silty sand, clayey sand: 2,000 psf. */
		SAND("sand", 9.8),
		/** Clay, silt and topsoil: 1,500 psf. */
		CLAY("dirt or clay", 7.3),
		/** Air, water or a block with no top to bear on. */
		NONE("nothing", 0.0);

		public final String label;
		public final double tonnesPerM2;

		Ground(String label, double tonnesPerM2) {
			this.label = label;
			this.tonnesPerM2 = tonnesPerM2;
		}

		public boolean bears(double tonnesPerM2) {
			return this.tonnesPerM2 + 1e-9 >= tonnesPerM2;
		}
	}

	// ---- collision (docs/questions/plant-crane-gaps.md, 3) ----------------------------------------------------------------------
	/** The tower crane's jib: its bottom chords this far over the hook's highest point, and its top chord this far above them, m. */
	public static final double TOWER_JIB_OVER_HOOK = 1.5, TOWER_JIB_DEPTH = 1.9;
	/** The tower crane's jib to its tip and its counter-jib, m (as drawn: tower-crane.md). */
	public static final double TOWER_JIB = 76.8, TOWER_COUNTER_JIB = 22.0;
	/** The tower crane's mast, half its width, m. */
	public static final double TOWER_MAST_HALF = 1.15;
	/** Room kept clear of a boom's or jib's centre line, m: half a lattice section and a margin. */
	public static final double CLEARANCE = 1.0;

	/**
	 * The hook positions a move passes through, as the crane draws them: slewing and moving out together, both in proportion to
	 * the time. {@code samples} points from {@code from} to {@code to}, ends included; each is {degrees, radius}.
	 */
	public static double[][] sweep(LiftCycle.Hook from, LiftCycle.Hook to, double reach) {
		double turn = LiftCycle.turn(from.degrees(), to.degrees());
		double arc = Math.abs(Math.toRadians(turn)) * Math.max(reach, 1);
		int n = (int) Math.ceil(Math.max(arc, Math.abs(to.radius() - from.radius()))) + 1;
		double[][] out = new double[n + 1][];
		for (int i = 0; i <= n; i++) {
			double f = (double) i / n;
			out[i] = new double[] {from.degrees() + turn * f, from.radius() + (to.radius() - from.radius()) * f};
		}
		return out;
	}

	// ---- the ring crane's travel (docs/questions/plant-crane-gaps.md, 4) -----------------------------------------------------------
	/** How fast the ring crane skids along its rails, m/h (proposed: a hydraulic skid system's pace). */
	public static final double RING_TRAVEL_M_H = 10.0;
	/** What its skid jacks draw from its power packs while it travels, W (proposed: as its slewing drives). */
	public static final double RING_TRAVEL_WATTS = CraneKind.RING_SLEW_WATTS;
	/** It aims to stand with the site this far beyond its chart's least radius, m: close in, where the chart is highest. */
	public static final double RING_TRAVEL_MARGIN = 10.0;
	/** It travels only if that brings the site at least this much nearer to its aim, m. */
	public static final double RING_TRAVEL_GAIN = 2.0;

	/**
	 * The best stop on the rails, as a signed number of metres from where it stands: the one that puts {@code site} (x, z) nearest
	 * {@code aim} m from the slewing centre, within {@code reach}; the nearer stop when two are as good. {@code centre} is its slewing
	 * centre now, {@code dir} the rails' unit direction (x, z), and it may go {@code back} m one way and {@code ahead} m the other.
	 * Zero if no stop is better by {@link #RING_TRAVEL_GAIN} than where it is.
	 */
	public static int bestStop(double[] centre, int[] dir, int back, int ahead, double[] site, double aim, double reach) {
		double here = Math.abs(Math.hypot(site[0] - centre[0], site[1] - centre[1]) - aim);
		int best = 0;
		double bestScore = here;
		for (int k = -back; k <= ahead; k++) {
			double x = centre[0] + dir[0] * k, z = centre[1] + dir[1] * k;
			double d = Math.hypot(site[0] - x, site[1] - z);
			if (d > reach) {
				continue;
			}
			double score = Math.abs(d - aim);
			if (score < bestScore - 1e-9 || (Math.abs(score - bestScore) < 1e-9 && Math.abs(k) < Math.abs(best))) {
				best = k;
				bestScore = score;
			}
		}
		return here - bestScore >= RING_TRAVEL_GAIN ? best : 0;
	}

	/** Seconds to travel {@code metres} on the rails. */
	public static double travelSeconds(double metres) {
		return Math.abs(metres) / RING_TRAVEL_M_H * 3600;
	}
}
