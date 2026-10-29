package dev.wildcatter.circuit;

/** Which of the two conductors of a circuit a wire is. Power goes out on the hot one and comes back on the neutral one. */
public enum Role {
	HOT("Hot", "red"),
	NEUTRAL("Neutral", "black");

	private final String label;
	private final String color;

	Role(String label, String color) {
		this.label = label;
		this.color = color;
	}

	public String label() {
		return label;
	}

	public String color() {
		return color;
	}
}
