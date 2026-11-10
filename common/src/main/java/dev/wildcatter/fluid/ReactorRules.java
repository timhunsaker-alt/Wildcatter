package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;
import java.util.Properties;

/**
 * Phase 19's reactor rules that change what an existing reactor does (docs/multiblocks/reactor-extensions.md). Each is a key in
 * {@code config/wildcatter.properties}, read when the server starts (as {@code standard} and {@code wattsPerFe} are), and each is OFF
 * unless the key says {@code true}: a world built before Phase 19 runs exactly as it did. Parts that only add (the containment, the
 * relief tank, casks, the inspection stand) need no rule. Plain Java, so the reactor's unit tests can switch a rule on and back.
 */
public final class ReactorRules {
	private ReactorRules() {
	}

	/** 19a: the console tracks the heat-up rate in plant time, alarms HEAT-UP RATE over 55 C/h and counts the excursions. */
	public static volatile boolean heatupLimit;
	/** 19a: the 80x heat-up compression is removed (cold to hot standby takes the real 8-12 hours). */
	public static volatile boolean realHeatup;
	/** 19a: the P-T limit curve, LTOP, the P-T LIMIT and LTOP NOT ARMED alarms, and the vessel's brittle failure. */
	public static volatile boolean ltop;
	/** 19a: the full overtemperature and overpower delta-T trips, ahead of the simplified 343 C check. */
	public static volatile boolean deltaTTrips;
	/** 19b: coolant activity, contamination where primary water gets out, its dose, and its pick-up by players. */
	public static volatile boolean contamination;
	/** 19b: a fuel assembly moved by hopper or crane may get a bent grid; loaded uninspected, it becomes a leaking rod. */
	public static volatile boolean fuelHandlingDamage;

	/** The keys, in the order of {@link #snapshot()}. */
	public static final String[] KEYS = {"reactorHeatupLimit", "reactorRealHeatup", "reactorLtop", "reactorDeltaTTrips", "reactorContamination", "fuelHandlingDamage"};

	/** Reads the keys from the config's properties; a missing or unreadable key is off. */
	public static void load(Properties props) {
		boolean[] v = new boolean[KEYS.length];
		for (int i = 0; i < KEYS.length; i++) {
			v[i] = Boolean.parseBoolean(props.getProperty(KEYS[i], "false").trim());
		}
		restore(v);
	}

	/** The rules now, for a test to hand back with {@link #restore}. */
	public static boolean[] snapshot() {
		return new boolean[] {heatupLimit, realHeatup, ltop, deltaTTrips, contamination, fuelHandlingDamage};
	}

	public static void restore(boolean[] v) {
		heatupLimit = v[0];
		realHeatup = v[1];
		ltop = v[2];
		deltaTTrips = v[3];
		contamination = v[4];
		fuelHandlingDamage = v[5];
	}

	/** The rules that are on, for the server log. */
	public static String describe() {
		StringBuilder out = new StringBuilder();
		boolean[] v = snapshot();
		for (int i = 0; i < KEYS.length; i++) {
			if (v[i]) {
				out.append(out.isEmpty() ? "" : ", ").append(KEYS[i]);
			}
		}
		return out.isEmpty() ? "none (as before Phase 19)" : out.toString();
	}

	/** How much the heat-up below no-load temperature is sped up: 80, or 1 with {@link #realHeatup}. */
	public static double speedup() {
		return realHeatup ? 1 : Tuning.HEATUP_SPEEDUP;
	}
}
