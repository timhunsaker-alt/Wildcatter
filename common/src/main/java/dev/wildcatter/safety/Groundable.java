package dev.wildcatter.safety;

/**
 * Something a grounding clamp set (an ASTM F855 cluster) can be put on: a cable, busway or bus duct cell, or a part with a bus or
 * terminals of its own (a switchgear section's main bus). Right-clicking any cell of it with the set toggles the grounds.
 */
public interface Groundable {
	boolean grounded();

	/** Puts the clamps on or takes them off; on a live conductor putting them on is a bolted fault. */
	void setGrounded(boolean on);

	/** Said to the player when the clamps go on. */
	default String groundedMessage() {
		return "Grounding set: grounds applied, A, B and C to ground";
	}
}
