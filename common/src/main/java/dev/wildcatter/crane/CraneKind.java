package dev.wildcatter.crane;

/**
 * What each crane can do (docs/multiblocks/tower-crane.md, crawler-crane.md): its chart, its height under the hook, its speeds and
 * what its drives draw. Plain Java, so it is unit-tested without Minecraft.
 */
public enum CraneKind {
	/** Liebherr 380 EC-B 16 Litronic, 75 m jib, free-standing. */
	TOWER,
	/** Liebherr LR 1300, main boom, 104 t counterweight. */
	CRAWLER;

	public static final double G = 9.81;

	// ---- tower crane (BKL data sheet, driving units Litronic) ---------------------------------------------------------------------
	public static final double TOWER_HOOK_HEIGHT = 59.7;
	/** Each hoist gear: its rated tonnes and m/min there, and its light tonnes and m/min there. */
	private static final double[][] TOWER_GEARS = {{16.0, 36, 7.5, 71}, {8.8, 61, 4.0, 123}, {4.9, 102, 1.9, 205}};
	public static final double TOWER_TROLLEY_M_MIN = 95;
	public static final double TOWER_SLEW_RPM = 0.8;
	public static final double TOWER_HOIST_WATTS = 110_000, TOWER_TROLLEY_WATTS = 10_500, TOWER_SLEW_WATTS = 15_000;
	public static final double TOWER_STANDBY_WATTS = 3_000;           // the cab and controls (proposed)
	public static final double TOWER_HOIST_EFFICIENCY = 0.85;          // drive, motor and gearing (proposed)

	// ---- crawler crane (LR 1300 technical description) ------------------------------------------------------------------------------
	public static final double CRAWLER_ROPE_M_MIN = 138;
	public static final double CRAWLER_LINE_PULL_KN = 150;
	public static final int CRAWLER_MAX_PARTS = 22;
	public static final double CRAWLER_SLEW_RPM = 1.8;
	public static final double CRAWLER_LUFF_DEG_S = 71.0 / 127.0;      // 127 s from 15 to 86 degrees
	public static final double CRAWLER_ENGINE_WATTS = 390_000;
	public static final double CRAWLER_HYDRAULIC_EFFICIENCY = 0.75;     // engine to hook (proposed)
	public static final double CRAWLER_SLEW_WATTS = 40_000, CRAWLER_LUFF_WATTS = 60_000;   // (proposed)
	public static final double CRAWLER_HEAD_DROP = 4.0;                 // the boom head and hook block below the boom tip, m (proposed)
	public static final double CRAWLER_FULL_LOAD_LPH = 102;            // 220 g/kWh at 390 kW (proposed)
	public static final double CRAWLER_TANK_LITRES = 750;

	/** Rigging a load at the pick or at its place, and landing and unhooking it, s (proposed). */
	public static final double RIG_SECONDS = 30;

	public LoadChart chart(int boom) {
		return this == TOWER ? LoadChart.tower() : LoadChart.crawler(boom);
	}

	public String title() {
		return this == TOWER ? "Tower crane (16 t, 75 m jib)" : "Crawler crane (300 t)";
	}

	/** The crawler's boom angle at {@code radius} on boom {@code boom}, degrees. */
	public static double boomDegrees(int boom, double radius) {
		double length = LoadChart.CRAWLER_BOOMS[boom];
		double c = Math.clamp((radius - LoadChart.BOOM_FOOT_OUT) / length, 0, 1);
		return Math.toDegrees(Math.acos(c));
	}

	/** Height of the hook's highest point above the crane's base at {@code radius} m, m. */
	public double hookHeight(int boom, double radius) {
		if (this == TOWER) {
			return TOWER_HOOK_HEIGHT;
		}
		double length = LoadChart.CRAWLER_BOOMS[boom];
		return LoadChart.BOOM_FOOT_UP + length * Math.sin(Math.toRadians(boomDegrees(boom, radius))) - CRAWLER_HEAD_DROP;
	}

	/** The parts of line the crawler's hook block is reeved with for {@code tonnes} on the hook. */
	public static int partsOfLine(double tonnes) {
		return (int) Math.clamp(Math.ceil(tonnes * G / CRAWLER_LINE_PULL_KN), 1, CRAWLER_MAX_PARTS);
	}

	/** Hook speed with {@code tonnes} on it, m/min. */
	public double hoistMetresPerMinute(double tonnes) {
		if (this == TOWER) {
			double best = 0;
			for (double[] gear : TOWER_GEARS) {
				double rated = gear[0], atRated = gear[1], light = gear[2], atLight = gear[3];
				if (tonnes > rated + 1e-9) {
					continue;
				}
				double v = tonnes <= light ? atLight : atRated + (atLight - atRated) * (rated - tonnes) / (rated - light);
				best = Math.max(best, v);
			}
			return best;
		}
		double reeved = CRAWLER_ROPE_M_MIN / partsOfLine(tonnes);
		double powered = CRAWLER_HYDRAULIC_EFFICIENCY * CRAWLER_ENGINE_WATTS / (Math.max(tonnes, 0.5) * 1000 * G) * 60;
		return Math.min(reeved, powered);
	}

	/** Seconds to slew {@code degrees}. */
	public double slewSeconds(double degrees) {
		double rpm = this == TOWER ? TOWER_SLEW_RPM : CRAWLER_SLEW_RPM;
		return Math.abs(degrees) / (rpm * 6.0);
	}

	/** Seconds to move the hook out or in from {@code from} to {@code to} m: the trolley, or luffing the boom. */
	public double outSeconds(int boom, double from, double to) {
		if (this == TOWER) {
			return Math.abs(to - from) / TOWER_TROLLEY_M_MIN * 60;
		}
		return Math.abs(boomDegrees(boom, to) - boomDegrees(boom, from)) / CRAWLER_LUFF_DEG_S;
	}

	/** Watts the hoist takes lifting {@code tonnes} at {@code metresPerMinute}. */
	public double hoistWatts(double tonnes, double metresPerMinute) {
		double lift = tonnes * 1000 * G * metresPerMinute / 60;
		return this == TOWER ? Math.min(TOWER_HOIST_WATTS, lift / TOWER_HOIST_EFFICIENCY) : Math.min(CRAWLER_ENGINE_WATTS, lift / CRAWLER_HYDRAULIC_EFFICIENCY);
	}

	public double slewWatts() {
		return this == TOWER ? TOWER_SLEW_WATTS : CRAWLER_SLEW_WATTS;
	}

	public double outWatts() {
		return this == TOWER ? TOWER_TROLLEY_WATTS : CRAWLER_LUFF_WATTS;
	}

	/** The crawler's diesel a second with its drives taking {@code watts}, litres. */
	public static double litresPerSecond(double watts) {
		double share = Math.clamp(watts / CRAWLER_ENGINE_WATTS, 0, 1);
		return CRAWLER_FULL_LOAD_LPH / 3600.0 * (0.15 + 0.85 * share);
	}
}
