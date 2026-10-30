package dev.wildcatter.energy;

/**
 * The electrical standard of a world: North American (60 Hz) or IEC (50 Hz). Chosen when the world is created and fixed
 * afterwards. Every voltage and the frequency in the mod come from here; nothing else hardcodes them.
 * See docs/ELECTRICAL_MODEL.md for the table. Plain Java, no Minecraft.
 *
 * The mod's three voltage tiers ({@link Voltage}) read their volts and label from the active standard. The other
 * levels of the table (distribution, generator terminals, industrial and commercial low voltage) are data for the
 * phases that add their equipment.
 */
public enum ElectricalStandard {
	NORTH_AMERICAN("North American", 60.0,
			new Level("120 V", 120.0), new Level("4.16 kV", 4160.0), new Level("138 kV", 138000.0),
			new Level("12.47 kV", 12470.0), new Level("13.8 kV", 13800.0), new Level("480 V", 480.0), new Level("208 V", 208.0)),
	IEC("IEC", 50.0,
			new Level("230 V", 230.0), new Level("3.3 kV", 3300.0), new Level("132 kV", 132000.0),
			new Level("11 kV", 11000.0), new Level("11 kV", 11000.0), new Level("400 V", 400.0), new Level("400 V", 400.0));

	/** A voltage level: its label and its nominal volts (line to neutral for the residential level, line to line above it). */
	public record Level(String label, double volts) {
	}

	private final String displayName;
	private final double hertz;
	private final Level low;
	private final Level medium;
	private final Level high;
	private final Level distribution;
	private final Level generator;
	private final Level industrialLow;
	private final Level commercialLow;

	ElectricalStandard(String displayName, double hertz, Level low, Level medium, Level high, Level distribution,
			Level generator, Level industrialLow, Level commercialLow) {
		this.displayName = displayName;
		this.hertz = hertz;
		this.low = low;
		this.medium = medium;
		this.high = high;
		this.distribution = distribution;
		this.generator = generator;
		this.industrialLow = industrialLow;
		this.commercialLow = commercialLow;
	}

	/** What the world is using. The server sets it when a world loads. */
	private static volatile ElectricalStandard active = NORTH_AMERICAN;
	/** A choice made on the Create World screen, waiting for the new world to read it. Null otherwise. */
	private static volatile ElectricalStandard pending;

	public static ElectricalStandard active() {
		return active;
	}

	public static void setActive(ElectricalStandard standard) {
		active = standard;
	}

	public static ElectricalStandard pending() {
		return pending;
	}

	public static void setPending(ElectricalStandard standard) {
		pending = standard;
	}

	/** North American residential service is split-phase (two legs 180 degrees apart); IEC is a single phase and neutral. */
	public boolean splitPhase() {
		return this == NORTH_AMERICAN;
	}

	/** Under-frequency load shedding, stage 1: relays on feeders open below this. */
	public double loadShedHertz() {
		return this == NORTH_AMERICAN ? 59.3 : 49.0;
	}

	/** A generator trips if the frequency stays below this. */
	public double generatorLowHertz() {
		return this == NORTH_AMERICAN ? 57.0 : 47.5;
	}

	/** A generator trips if the frequency stays above this. */
	public double generatorHighHertz() {
		return this == NORTH_AMERICAN ? 61.5 : 51.5;
	}

	public String displayName() {
		return displayName;
	}

	public double hertz() {
		return hertz;
	}

	public Level level(Voltage tier) {
		return switch (tier) {
			case LOW -> low;
			case MEDIUM -> medium;
			case DISTRIBUTION -> distribution;
			case HIGH -> high;
		};
	}

	public Level distribution() {
		return distribution;
	}

	public Level generatorTerminal() {
		return generator;
	}

	public Level industrialLow() {
		return industrialLow;
	}

	public Level commercialLow() {
		return commercialLow;
	}
}
