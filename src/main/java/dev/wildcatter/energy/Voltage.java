package dev.wildcatter.energy;

/**
 * The three levels of the grid, as in the real one: houses and small machines run on 110 V, bigger generators and
 * local lines run at medium voltage, and long hauls run at high voltage because it loses less on the way.
 * Energy only ever moves between two things on the same level; a transformer is what joins two of them.
 */
public enum Voltage {
	LOW("110 V"),
	MEDIUM("4.16 kV"),
	HIGH("138 kV");

	private final String label;

	Voltage(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
