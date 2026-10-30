package dev.wildcatter.energy;

/**
 * The receptacle and plug types (docs/ELECTRICAL_MODEL.md). A plug adapter only fits a receptacle of its own type. The nominal
 * volts and amps are the standard's: a plug on a circuit at the wrong voltage delivers nothing. Plain Java, no Minecraft.
 */
public enum PlugType {
	NEMA_5_15("nema_5_15", "NEMA 5-15", 120.0, 15.0, false),
	NEMA_5_20("nema_5_20", "NEMA 5-20", 120.0, 20.0, false),
	NEMA_14_50("nema_14_50", "NEMA 14-50", 240.0, 50.0, true),
	CEE_7("cee_7", "CEE 7 (Schuko)", 230.0, 16.0, false),
	IEC_60309_BLUE("iec_60309_blue", "IEC 60309 blue", 230.0, 32.0, false);

	private final String id;
	private final String label;
	private final double volts;
	private final double amps;
	private final boolean betweenLegs;

	PlugType(String id, String label, double volts, double amps, boolean betweenLegs) {
		this.id = id;
		this.label = label;
		this.volts = volts;
		this.amps = amps;
		this.betweenLegs = betweenLegs;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	/** The volts across the plug when it is on a healthy circuit of its standard. */
	public double volts() {
		return volts;
	}

	/** What the receptacle and plug are rated to carry. */
	public double amps() {
		return amps;
	}

	/** True for a 240 V plug that takes both legs (A and B); the others take one line (A or B) and the neutral. */
	public boolean betweenLegs() {
		return betweenLegs;
	}
}
