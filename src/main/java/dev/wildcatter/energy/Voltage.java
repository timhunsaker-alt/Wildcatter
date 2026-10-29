package dev.wildcatter.energy;

/**
 * The three levels of the grid, as in the real one: houses and small machines run on 110 V, bigger generators and
 * local lines run at medium voltage, and long hauls run at high voltage because it loses less on the way.
 * Things only connect at the same level; a transformer is what joins two of them.
 */
public enum Voltage {
	LOW("110 V", 110.0),
	MEDIUM("4.16 kV", 4160.0),
	HIGH("138 kV", 138000.0);

	private final String label;
	private final double nominal;

	Voltage(String label, double nominal) {
		this.label = label;
		this.nominal = nominal;
	}

	public String label() {
		return label;
	}

	/** The volts a source at this level is meant to hold. */
	public double nominal() {
		return nominal;
	}
}
