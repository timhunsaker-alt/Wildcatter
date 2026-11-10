package dev.wildcatter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the player chose on the Create World screen's World Rules screen, for the world about to be made (docs/time-scale.md): the time
 * scale and each world rule, Wildcatter's and any other mod's that {@linkplain #register registers} its rules here. Held until the new
 * world's rules are made ({@link RulesData#load}, and the other mods' own), then cleared; cancelling the screen clears it too. With no
 * choice made (a dedicated server, a test, or a player who did not open the screen) each rule takes its new-world default and the scale
 * is 1:1, or what {@code config/wildcatter.properties} says ({@code newWorld.timeScale}, {@code newWorld.<rule>}).
 */
public final class NewWorldSetup {
	private NewWorldSetup() {
	}

	/** A rule shown on the screen: its mod, its key (as its command takes it), what it does, and its new-world default. */
	public record Entry(String mod, String key, String what, boolean newWorld) {
		public String id() {
			return mod + ":" + key;
		}
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final Map<String, Boolean> CHOSEN = new LinkedHashMap<>();
	private static volatile Integer chosenScale;

	static {
		for (Rules.Rule r : Rules.Rule.values()) {
			ENTRIES.add(new Entry(Wildcatter.MOD_ID, r.key, r.what, r.newWorld));
		}
	}

	/** Another mod's world rule, shown on the World Rules screen after Wildcatter's. Call once, at mod setup. */
	public static synchronized void register(String mod, String key, String what, boolean newWorld) {
		ENTRIES.add(new Entry(mod, key, what, newWorld));
	}

	public static synchronized List<Entry> entries() {
		return List.copyOf(ENTRIES);
	}

	/** The rule as chosen for the new world, or its default (the config file's, else the rule's own). */
	public static synchronized boolean rule(String mod, String key, boolean newWorld) {
		Boolean chosen = CHOSEN.get(mod + ":" + key);
		if (chosen != null) {
			return chosen;
		}
		String config = configValue("newWorld." + (mod.equals(Wildcatter.MOD_ID) ? "" : mod + ".") + key);
		return config != null ? Boolean.parseBoolean(config) : newWorld;
	}

	public static synchronized void choose(String mod, String key, boolean on) {
		CHOSEN.put(mod + ":" + key, on);
	}

	/** The scale chosen for the new world, or the config file's, or 1. */
	public static int scale() {
		Integer chosen = chosenScale;
		if (chosen != null) {
			return chosen;
		}
		String config = configValue("newWorld.timeScale");
		try {
			return config == null ? 1 : TimeScale.preset(Integer.parseInt(config));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	public static void chooseScale(int scale) {
		chosenScale = TimeScale.preset(scale);
	}

	/** Has the player chosen anything on the screen? */
	public static synchronized boolean chosen() {
		return chosenScale != null || !CHOSEN.isEmpty();
	}

	/** Forgets the choices: once a new world has taken them, or when world creation is left. */
	public static synchronized void clear() {
		CHOSEN.clear();
		chosenScale = null;
	}

	/** Set by tests that run without a loader (no config folder). */
	static volatile boolean noConfig;

	private static String configValue(String key) {
		if (noConfig) {
			return null;
		}
		try {
			java.nio.file.Path file = dev.wildcatter.platform.Platform.INSTANCE.configDir().resolve("wildcatter.properties");
			if (!java.nio.file.Files.exists(file)) {
				return null;
			}
			java.util.Properties props = new java.util.Properties();
			try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
				props.load(in);
			}
			String v = props.getProperty(key);
			return v == null ? null : v.trim();
		} catch (Exception | LinkageError e) {
			return null;
		}
	}
}
