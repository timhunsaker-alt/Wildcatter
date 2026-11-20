package dev.wildcatter.circuit;

/**
 * A circuit part that makes power: it sets the frequency of its island together with the others. The
 * {@link CircuitManager} adds up the ratings and the power delivered, steps the island's frequency, and trips
 * generators whose frequency has been outside the safe band for too long.
 */
public interface Generator {
	/** What it can supply, in watts. */
	double ratedWatts();

	/** Its inertia constant H in seconds: how long its spinning mass could run it at rating. */
	double inertiaSeconds();

	/** What it is delivering right now, in watts: negative while the rest of the island drives it. */
	double deliveredWatts();

	/** False once it has tripped. */
	boolean online();

	/** True for a battery (22c): its output is not a machine's, so a peak shaver does not count it. */
	default boolean storage() {
		return false;
	}

	/** Frequency protection: shut down. */
	void tripOnFrequency(double hertz);
}
