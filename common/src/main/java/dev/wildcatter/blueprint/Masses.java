package dev.wildcatter.blueprint;

import java.util.Map;

/**
 * What a crane lifts (docs/blueprints.md, "Masses", all proposed): a part's own mass where one is known, else its footprint box
 * times the density of its kind; an ordinary block by what it is made of; and the hook block and slings on top. Plain Java, so it is
 * unit-tested without Minecraft.
 */
public final class Masses {
	private Masses() {
	}

	/** Tonnes of the parts with their own figure. */
	private static final Map<String, Double> KNOWN = Map.ofEntries(
			Map.entry("reactor_vessel", 180.0),
			Map.entry("steam_generator", 150.0),
			Map.entry("pressurizer", 50.0),
			Map.entry("reactor_coolant_pump", 40.0),
			Map.entry("hp_turbine", 40.0),
			Map.entry("lp_turbine", 120.0),
			Map.entry("tg_generator_60", 150.0),
			Map.entry("gsu_60", 90.0),
			Map.entry("substation_transformer_60", 90.0),
			Map.entry("rectiformer", 85.0),
			Map.entry("pot", 60.0),
			Map.entry("aero_turbine", 20.0),
			Map.entry("aero_generator", 80.0),
			Map.entry("gt_generator", 80.0));

	/** t/m3 of footprint box, by kind. */
	public static double density(String id) {
		if (has(id, "transformer", "gsu_", "padmount_", "unit_substation")) {
			return 0.5;
		}
		if (has(id, "turbine", "generator", "compressor", "pump", "mill", "fan", "blower", "engine", "alternator")) {
			return 0.4;
		}
		if (has(id, "breaker", "switchgear", "cabinet", "starter", "mcc", "drive_", "control", "panel", "relay", "load_centre", "gutter", "safety_switch", "lis_", "ats_")) {
			return 0.3;
		}
		if (has(id, "tower", "stack", "derrick", "rig", "house", "filter", "quench")) {
			return 0.05;
		}
		return 0.15;
	}

	private static boolean has(String id, String... words) {
		for (String w : words) {
			if (id.contains(w)) {
				return true;
			}
		}
		return false;
	}

	/** Tonnes of a part {@code w x h x l} m. */
	public static double part(String id, int w, int h, int l) {
		Double known = KNOWN.get(id);
		return known != null ? known : w * h * l * density(id);
	}

	/** What an ordinary block is made of, from its sound type and its id. */
	public enum Material {
		STONE(2.4), METAL(7.8), COPPER(8.9), GLASS(2.5), WOOD(0.6), LIGHT(0.05), WILDCATTER(0.1), OTHER(1.0);

		public final double tonnes;

		Material(double tonnes) {
			this.tonnes = tonnes;
		}
	}

	/** The hook block and slings: 2 % of the load, at least half a tonne. */
	public static double rigging(double tonnes) {
		return Math.max(0.5, tonnes * 0.02);
	}

	/** What the crane carries for a load of {@code tonnes}. */
	public static double onHook(double tonnes) {
		return tonnes + rigging(tonnes);
	}
}
