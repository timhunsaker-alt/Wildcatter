package dev.wildcatter.crane;

/**
 * What each crane can do (docs/multiblocks/tower-crane.md, crawler-crane.md, ring-crane.md): its chart, its height under the hook,
 * its speeds and what its drives draw. Plain Java, so it is unit-tested without Minecraft.
 */
public enum CraneKind {
	/** Liebherr 380 EC-B 16 Litronic, 75 m jib, free-standing. */
	TOWER,
	/** Liebherr LR 1300, main boom, 104 t counterweight. */
	CRAWLER,
	/** Sarens SGC-250, 118 m or 160.5 m main boom, 5,200 t counterweight. */
	RING;

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

	// ---- ring crane (Sarens SGC brochure; Sarens, Cat and Bosch Rexroth releases) ----------------------------------------------------
	public static final double RING_LINE_M_MIN = 20;                    // each winch, layer 1
	public static final int RING_WINCHES = 6;
	public static final double RING_LINE_PULL_KN = 536;
	public static final int RING_MAX_PARTS = 60;
	public static final double RING_SLEW_DEG_MIN = 6;
	public static final double RING_ENGINE_WATTS = 12 * 310_000;
	public static final double RING_HOOK_BLOCK_TONNES = 104.6;
	public static final double RING_LUFF_DEG_S = 0.1;                   // (proposed)
	public static final double RING_SLEW_WATTS = 600_000, RING_LUFF_WATTS = 750_000;   // (proposed)
	public static final double RING_BOOM_FOOT_OUT = 18, RING_BOOM_FOOT_UP = 6;      // (proposed)
	public static final double RING_HEAD_DROP = 14.5 + 4;               // the hook block and the boom head (proposed)
	public static final double RING_FULL_LOAD_LPH = 975;               // 220 g/kWh at 3.72 MW (proposed)
	public static final double RING_TANK_LITRES = 6_000;               // 1,000 L a power pack (proposed)

	/** Rigging a load at the pick or at its place, and landing and unhooking it, s (proposed). */
	public static final double RIG_SECONDS = 30;

	// ---- wind and ground (docs/questions/plant-crane-gaps.md, 1 and 2) -------------------------------------------------------------
	/** The tower crane stops above about 72 km/h at its jib (tower-crane.md). */
	public static final double TOWER_WIND_LIMIT = 20.0;
	/** The LR 1300's main boom charts hold to 9 m/s at the boom head (EN 13000's reference wind; proposed). */
	public static final double CRAWLER_WIND_LIMIT = 9.0;
	/** The SGC-250's 14 m/s (ring-crane.md). */
	public static final double RING_WIND_LIMIT = 14.0;
	/** The LR 1300 on its mats: 1.45 kg/cm2 (crawler-crane.md). */
	public static final double CRAWLER_GROUND_T_M2 = 14.5;
	/** The SGC-250's ring mats: 30 t/m2 (ring-crane.md). */
	public static final double RING_GROUND_T_M2 = 30.0;

	/** The most wind it works in, at its jib or boom head, m/s. */
	public double windLimit() {
		return switch (this) {
			case TOWER -> TOWER_WIND_LIMIT;
			case CRAWLER -> CRAWLER_WIND_LIMIT;
			case RING -> RING_WIND_LIMIT;
		};
	}

	/** What the ground under its crawlers or ring must bear, t/m2; 0 for the tower crane, whose cross base is on its own ballast slabs. */
	public double groundTonnesPerM2() {
		return switch (this) {
			case TOWER -> 0;
			case CRAWLER -> CRAWLER_GROUND_T_M2;
			case RING -> RING_GROUND_T_M2;
		};
	}

	/** Height of its jib's top or its boom head above its base with the hook at {@code radius}, m: where its wind is read. */
	public double headHeight(int boom, double radius) {
		if (this == TOWER) {
			return TOWER_HOOK_HEIGHT + Siting.TOWER_JIB_OVER_HOOK + Siting.TOWER_JIB_DEPTH;
		}
		return footUp() + boomLength(boom) * Math.sin(Math.toRadians(boomAngle(boom, radius)));
	}

	public LoadChart chart(int boom) {
		return switch (this) {
			case TOWER -> LoadChart.tower();
			case CRAWLER -> LoadChart.crawler(boom);
			case RING -> LoadChart.ring(boom);
		};
	}

	public String title() {
		return switch (this) {
			case TOWER -> "Tower crane (16 t, 75 m jib)";
			case CRAWLER -> "Crawler crane (300 t)";
			case RING -> "Ring crane (5,000 t)";
		};
	}

	/** True for the cranes with a luffing boom the player sets. */
	public boolean hasBoom() {
		return this != TOWER;
	}

	/** True for the cranes on their own diesel. */
	public boolean diesel() {
		return this != TOWER;
	}

	public int booms() {
		return switch (this) {
			case TOWER -> 1;
			case CRAWLER -> LoadChart.CRAWLER_BOOMS.length;
			case RING -> LoadChart.RING_BOOMS.length;
		};
	}

	/** Boom {@code boom}'s length, m; the jib's for the tower crane. */
	public double boomLength(int boom) {
		return switch (this) {
			case TOWER -> 75;
			case CRAWLER -> LoadChart.CRAWLER_BOOMS[boom];
			case RING -> LoadChart.RING_BOOMS[boom];
		};
	}

	/** The boom to start on: the crawler's 32 m, the ring crane's 118 m. */
	public int firstBoom() {
		return this == CRAWLER ? 3 : 0;
	}

	public String boomName(int boom) {
		double l = boomLength(boom);
		return l == Math.rint(l) ? String.format("%.0f m", l) : String.format("%.1f m", l);
	}

	private double footOut() {
		return this == RING ? RING_BOOM_FOOT_OUT : LoadChart.BOOM_FOOT_OUT;
	}

	private double footUp() {
		return this == RING ? RING_BOOM_FOOT_UP : LoadChart.BOOM_FOOT_UP;
	}

	/** The boom's foot: how far ahead of the slewing centre and how high, m. */
	public double[] boomFoot() {
		return new double[] {footOut(), footUp()};
	}

	public double tankLitres() {
		return this == RING ? RING_TANK_LITRES : CRAWLER_TANK_LITRES;
	}

	/** The hook block's own mass where it is not in the 2 % rigging rule, t: the ring crane's 104.6 t main hook. */
	public double hookBlockTonnes() {
		return this == RING ? RING_HOOK_BLOCK_TONNES : 0;
	}

	/** The crawler's boom angle at {@code radius} on boom {@code boom}, degrees. */
	public static double boomDegrees(int boom, double radius) {
		return CRAWLER.boomAngle(boom, radius);
	}

	/** The boom's angle at {@code radius} on boom {@code boom}, degrees. */
	public double boomAngle(int boom, double radius) {
		double c = Math.clamp((radius - footOut()) / boomLength(boom), 0, 1);
		return Math.toDegrees(Math.acos(c));
	}

	/** Height of the hook's highest point above the crane's base at {@code radius} m, m. */
	public double hookHeight(int boom, double radius) {
		if (this == TOWER) {
			return TOWER_HOOK_HEIGHT;
		}
		double drop = this == RING ? RING_HEAD_DROP : CRAWLER_HEAD_DROP;
		return footUp() + boomLength(boom) * Math.sin(Math.toRadians(boomAngle(boom, radius))) - drop;
	}

	/** The parts of line the crawler's hook block is reeved with for {@code tonnes} on the hook. */
	public static int partsOfLine(double tonnes) {
		return (int) Math.clamp(Math.ceil(tonnes * G / CRAWLER_LINE_PULL_KN), 1, CRAWLER_MAX_PARTS);
	}

	/** The parts of line the ring crane's main hook is reeved with for {@code tonnes}: in sixes, one share for each winch. */
	public static int ringPartsOfLine(double tonnes) {
		int parts = (int) Math.ceil(tonnes * G / RING_LINE_PULL_KN);
		parts = (parts + RING_WINCHES - 1) / RING_WINCHES * RING_WINCHES;
		return Math.clamp(parts, RING_WINCHES, RING_MAX_PARTS);
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
		double reeved = this == RING ? RING_WINCHES * RING_LINE_M_MIN / ringPartsOfLine(tonnes) : CRAWLER_ROPE_M_MIN / partsOfLine(tonnes);
		double powered = CRAWLER_HYDRAULIC_EFFICIENCY * engineWatts() / (Math.max(tonnes, 0.5) * 1000 * G) * 60;
		return Math.min(reeved, powered);
	}

	private double engineWatts() {
		return this == RING ? RING_ENGINE_WATTS : CRAWLER_ENGINE_WATTS;
	}

	/** Seconds to slew {@code degrees}. */
	public double slewSeconds(double degrees) {
		double degreesPerSecond = switch (this) {
			case TOWER -> TOWER_SLEW_RPM * 6.0;
			case CRAWLER -> CRAWLER_SLEW_RPM * 6.0;
			case RING -> RING_SLEW_DEG_MIN / 60.0;
		};
		return Math.abs(degrees) / degreesPerSecond;
	}

	/** Seconds to move the hook out or in from {@code from} to {@code to} m: the trolley, or luffing the boom. */
	public double outSeconds(int boom, double from, double to) {
		if (this == TOWER) {
			return Math.abs(to - from) / TOWER_TROLLEY_M_MIN * 60;
		}
		return Math.abs(boomAngle(boom, to) - boomAngle(boom, from)) / (this == RING ? RING_LUFF_DEG_S : CRAWLER_LUFF_DEG_S);
	}

	/** Watts the hoist takes lifting {@code tonnes} at {@code metresPerMinute}. */
	public double hoistWatts(double tonnes, double metresPerMinute) {
		double lift = tonnes * 1000 * G * metresPerMinute / 60;
		return this == TOWER ? Math.min(TOWER_HOIST_WATTS, lift / TOWER_HOIST_EFFICIENCY) : Math.min(engineWatts(), lift / CRAWLER_HYDRAULIC_EFFICIENCY);
	}

	public double slewWatts() {
		return switch (this) {
			case TOWER -> TOWER_SLEW_WATTS;
			case CRAWLER -> CRAWLER_SLEW_WATTS;
			case RING -> RING_SLEW_WATTS;
		};
	}

	public double outWatts() {
		return switch (this) {
			case TOWER -> TOWER_TROLLEY_WATTS;
			case CRAWLER -> CRAWLER_LUFF_WATTS;
			case RING -> RING_LUFF_WATTS;
		};
	}

	/** The crawler's diesel a second with its drives taking {@code watts}, litres. */
	public static double litresPerSecond(double watts) {
		return CRAWLER.dieselPerSecond(watts);
	}

	/** This crane's diesel a second with its drives taking {@code watts}, litres: 15 % idling, the rest with the load. */
	public double dieselPerSecond(double watts) {
		double share = Math.clamp(watts / engineWatts(), 0, 1);
		double full = this == RING ? RING_FULL_LOAD_LPH : CRAWLER_FULL_LOAD_LPH;
		return full / 3600.0 * (0.15 + 0.85 * share);
	}
}
