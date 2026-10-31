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
	IEC_60309_BLUE("iec_60309_blue", "IEC 60309 blue", 230.0, 32.0, false),
	// three-phase: A, B, C, N and G; the volts are line to line
	NEMA_L21_30("nema_l21_30", "NEMA L21-30", 208.0, 30.0, Voltage.LOW),
	NEMA_L22_30("nema_l22_30", "NEMA L22-30", 480.0, 30.0, Voltage.INDUSTRIAL),
	IEC_60309_RED_32("iec_60309_red_32", "IEC 60309 red 32 A", 400.0, 32.0, Voltage.INDUSTRIAL),
	IEC_60309_RED_63("iec_60309_red_63", "IEC 60309 red 63 A", 400.0, 63.0, Voltage.INDUSTRIAL);

	private final String id;
	private final String label;
	private final double volts;
	private final double amps;
	private final boolean betweenLegs;
	private final Voltage tier;
	private final boolean threePhase;

	PlugType(String id, String label, double volts, double amps, boolean betweenLegs) {
		this(id, label, volts, amps, betweenLegs, Voltage.LOW, false);
	}

	PlugType(String id, String label, double volts, double amps, Voltage tier) {
		this(id, label, volts, amps, false, tier, true);
	}

	PlugType(String id, String label, double volts, double amps, boolean betweenLegs, Voltage tier, boolean threePhase) {
		this.id = id;
		this.label = label;
		this.volts = volts;
		this.amps = amps;
		this.betweenLegs = betweenLegs;
		this.tier = tier;
		this.threePhase = threePhase;
	}

	/** The wiring tier the receptacle connects to. */
	public Voltage tier() {
		return tier;
	}

	/** A three-phase plug takes A, B and C (and N); its volts are line to line. */
	public boolean threePhase() {
		return threePhase;
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
