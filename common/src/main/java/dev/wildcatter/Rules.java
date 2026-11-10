package dev.wildcatter;

/**
 * The world's operating rules (Tim, 2026-11-10, answer 23): rules that would change or slow what an existing world already does. They
 * are saved with the world ({@link RulesData}), not in the config file. A NEW world starts with every rule on except the few marked off
 * below; a world saved before this version (or before a rule existed) loads with that rule off, so nothing an existing world does
 * changes on update. An operator changes them with {@code /wildcatter rule <name> [true|false]}, which also lists them.
 * <p>
 * The values are held here, static, for the running server (they are read in plain-Java models and on worldgen threads); {@link RulesData}
 * fills them when the overworld loads and keeps them when they change. Plain Java, so unit tests can switch a rule and hand it back.
 * {@code standard} and {@code wattsPerFe} stay in {@code config/wildcatter.properties}: they are not rules.
 */
public final class Rules {
	private Rules() {
	}

	/** Each rule: its command name, whether a new world starts with it on, and what it does (for the list). */
	public enum Rule {
		/** 16b1: a fluorinator stops at BED FULL when its bed holds 2 t of ash, until it is retired. */
		FLUORINATOR_ASH_BED("fluorinatorAshBed", true, "a fluorinator stops at BED FULL with 2 t of ash in its bed"),
		/** 16c1: centrifuges take 10 minutes to reach speed and coast for an hour when their drive goes. Off even in new worlds. */
		CASCADE_SPIN_UP("cascadeSpinUp", false, "centrifuges spin up for 10 minutes and coast down for an hour"),
		/** 19a: the P-T limit curve, LTOP, the P-T LIMIT and LTOP NOT ARMED alarms, and the vessel's brittle failure. */
		REACTOR_LTOP("reactorLtop", true, "the P-T limit curve, LTOP and the vessel's brittle failure"),
		/** 19a: the console tracks the heat-up rate in plant time, alarms HEAT-UP RATE over 55 C/h and counts the excursions. */
		REACTOR_HEATUP_LIMIT("reactorHeatupLimit", true, "the HEAT-UP RATE alarm over 55 C/h and its fatigue count"),
		/** 19a: the 80x heat-up compression is removed (cold to hot standby takes the real 8-12 hours). */
		REACTOR_REAL_HEATUP("reactorRealHeatup", true, "a reactor heats up in real hours (no 80x compression)"),
		/** 19a: the full overtemperature and overpower delta-T trips, ahead of the simplified 343 C check. */
		REACTOR_DELTA_T_TRIPS("reactorDeltaTTrips", true, "the full OT and OP delta-T trips"),
		/** 19b: coolant activity, contamination where primary water gets out, its dose, and its pick-up by players. Off even in new worlds. */
		REACTOR_CONTAMINATION("reactorContamination", false, "coolant activity and contamination where primary water gets out"),
		/** 19b: a fuel assembly moved by hopper or crane may get a bent grid; loaded uninspected, it becomes a leaking rod. */
		FUEL_HANDLING_DAMAGE("fuelHandlingDamage", true, "a 1 % chance of a bent grid on an assembly's first move"),
		/** 15a: salt domes on the 192-block seed grid with flank fields and caprock pools; off, domes are placed as before Phase 15. */
		SALT_DOME_GRID_192("saltDomeGrid192", true, "salt domes on a 192-block grid, with flank fields and caprock pools"),
		/** 15b1: mud under 15 lb/bbl of bentonite (plain water too) is LOW GEL and drills at half rate. */
		MUD_GEL("mudGel", true, "plain water and thin mud are LOW GEL and drill at half rate"),
		/** 15b2 and the washout: a deep field kicks under light mud and needs a BOP (NO BOP); unsaturated mud washes out in rock salt. */
		WELL_CONTROL("wellControl", true, "kicks, NO BOP into a deep field, blowouts, and washouts in rock salt"),
		/** Plant residence times as real holds: coking 18 h, digestion 1 h, precipitation 36 h. Off even in new worlds. */
		REAL_RESIDENCE_TIMES("realResidenceTimes", false, "coking 18 h, digestion 1 h and precipitation 36 h as real holds");

		public final String key;
		public final boolean newWorld;
		public final String what;

		Rule(String key, boolean newWorld, String what) {
			this.key = key;
			this.newWorld = newWorld;
			this.what = what;
		}

		public static Rule byKey(String key) {
			for (Rule r : values()) {
				if (r.key.equalsIgnoreCase(key)) {
					return r;
				}
			}
			return null;
		}
	}

	private static final boolean[] ON = new boolean[Rule.values().length];
	/** Told when a rule changes, so the world's saved copy follows (set by {@link RulesData}); null with no world loaded. */
	private static volatile java.util.function.BiConsumer<Rule, Boolean> listener;

	public static boolean on(Rule rule) {
		return ON[rule.ordinal()];
	}

	/** Turns a rule on or off for the running world (and its saved copy, if one is loaded). */
	public static void set(Rule rule, boolean value) {
		ON[rule.ordinal()] = value;
		java.util.function.BiConsumer<Rule, Boolean> l = listener;
		if (l != null) {
			l.accept(rule, value);
		}
	}

	static void listen(java.util.function.BiConsumer<Rule, Boolean> l) {
		listener = l;
	}

	/** The rules a world starts with: a new world's defaults, or every rule off for a world saved before the rules existed. */
	public static boolean defaultFor(Rule rule, boolean newWorld) {
		return newWorld && rule.newWorld;
	}

	/** Every rule off and no world: when a server stops, so the next world opened in this session starts clean. */
	public static void clear() {
		listener = null;
		java.util.Arrays.fill(ON, false);
	}

	/** The rules now, for a test to hand back with {@link #restore}. */
	public static boolean[] snapshot() {
		return ON.clone();
	}

	public static void restore(boolean[] v) {
		for (Rule r : Rule.values()) {
			if (r.ordinal() < v.length && ON[r.ordinal()] != v[r.ordinal()]) {
				set(r, v[r.ordinal()]);
			}
		}
	}

	/** The rules that are on, for the server log. */
	public static String describe() {
		StringBuilder out = new StringBuilder();
		for (Rule r : Rule.values()) {
			if (on(r)) {
				out.append(out.isEmpty() ? "" : ", ").append(r.key);
			}
		}
		return out.isEmpty() ? "none" : out.toString();
	}

	// ---- the short names the code reads ----

	public static boolean fluorinatorAshBed() {
		return on(Rule.FLUORINATOR_ASH_BED);
	}

	public static boolean cascadeSpinUp() {
		return on(Rule.CASCADE_SPIN_UP);
	}

	public static boolean reactorLtop() {
		return on(Rule.REACTOR_LTOP);
	}

	public static boolean reactorHeatupLimit() {
		return on(Rule.REACTOR_HEATUP_LIMIT);
	}

	public static boolean reactorRealHeatup() {
		return on(Rule.REACTOR_REAL_HEATUP);
	}

	public static boolean reactorDeltaTTrips() {
		return on(Rule.REACTOR_DELTA_T_TRIPS);
	}

	public static boolean reactorContamination() {
		return on(Rule.REACTOR_CONTAMINATION);
	}

	public static boolean fuelHandlingDamage() {
		return on(Rule.FUEL_HANDLING_DAMAGE);
	}

	public static boolean saltDomeGrid() {
		return on(Rule.SALT_DOME_GRID_192);
	}

	public static boolean mudGel() {
		return on(Rule.MUD_GEL);
	}

	public static boolean wellControl() {
		return on(Rule.WELL_CONTROL);
	}

	public static boolean realResidenceTimes() {
		return on(Rule.REAL_RESIDENCE_TIMES);
	}

	/** How much the reactor's heat-up below no-load temperature is sped up: 80, or 1 with {@code reactorRealHeatup}. */
	public static double heatupSpeedup() {
		return reactorRealHeatup() ? 1 : Tuning.HEATUP_SPEEDUP;
	}
}
