package dev.wildcatter;

/**
 * Opt-in operating rules (Phase 16, docs/questions/phase16-uranium.md): rules that would change or slow a plant an existing world
 * already runs. Each is off unless {@code config/wildcatter.properties} turns it on, read as the world's standard and {@code wattsPerFe}
 * are read from the same file; the file is read once, when a server starts ({@link #load}). Off, every plant runs as it did before
 * Phase 16.
 * <ul>
 * <li>{@code fluorinatorAshBed=true}: a fluorinator stops at BED FULL when its bed holds 2 t of ash, until it is retired (16b1).</li>
 * <li>{@code cascadeSpinUp=true}: centrifuges take 10 minutes to reach speed and coast for an hour when their drive goes (16c1).</li>
 * </ul>
 */
public final class Rules {
	private static boolean fluorinatorAshBed;
	private static boolean cascadeSpinUp;

	private Rules() {
	}

	public static boolean fluorinatorAshBed() {
		return fluorinatorAshBed;
	}

	public static boolean cascadeSpinUp() {
		return cascadeSpinUp;
	}

	/** For the game tests: turns a rule on or off for the running server. */
	public static void setFluorinatorAshBed(boolean on) {
		fluorinatorAshBed = on;
	}

	public static void setCascadeSpinUp(boolean on) {
		cascadeSpinUp = on;
	}

	/** Reads the rules from the config file; a missing file or key leaves a rule off. */
	public static void load() {
		java.nio.file.Path file = dev.wildcatter.platform.Platform.INSTANCE.configDir().resolve("wildcatter.properties");
		java.util.Properties props = new java.util.Properties();
		try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
			props.load(in);
		} catch (java.io.IOException e) {
			// no file yet: every rule off
		}
		fluorinatorAshBed = Boolean.parseBoolean(props.getProperty("fluorinatorAshBed", "false").trim());
		cascadeSpinUp = Boolean.parseBoolean(props.getProperty("cascadeSpinUp", "false").trim());
		Wildcatter.LOGGER.info("Operating rules: fluorinatorAshBed={}, cascadeSpinUp={}", fluorinatorAshBed, cascadeSpinUp);
	}
}
