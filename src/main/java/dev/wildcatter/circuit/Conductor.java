package dev.wildcatter.circuit;

/**
 * Which conductor of a circuit a wire is: the phases A, B and C, the neutral N and the ground G (docs/ELECTRICAL_MODEL.md).
 * A is the "hot" of a single-phase circuit. On a split-phase service (North American 120/240 V) A and B are the two legs,
 * 180 degrees apart, and N is the centre tap. C is the third phase of a three-phase circuit.
 * A wire carries one conductor, chosen by right-clicking it; its coloured band shows which.
 */
public enum Conductor implements net.minecraft.util.StringRepresentable {
	A("Phase A", "red"),
	B("Phase B", "orange"),
	C("Phase C", "blue"),
	N("Neutral", "black"),
	G("Ground", "green");

	private final String label;
	private final String color;

	Conductor(String label, String color) {
		this.label = label;
		this.color = color;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	public String label() {
		return label;
	}

	public String color() {
		return color;
	}

	/** The next one in the cycle a player steps through by right-clicking a wire. */
	public Conductor next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
