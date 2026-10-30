package dev.wildcatter.energy;

/**
 * The three tiers of the grid, as in the real one: houses and small machines run on low voltage, bigger generators and
 * local lines run at medium voltage, and long hauls run at high voltage because it loses less on the way.
 * Things only connect at the same tier; a transformer is what joins two of them.
 *
 * What a tier means in volts depends on the world's {@link ElectricalStandard}: low is 120 V or 230 V, medium is 4.16 kV
 * or 3.3 kV, high is 138 kV or 132 kV. Never write a voltage in the code: ask the tier.
 */
public enum Voltage {
	LOW,
	MEDIUM,
	/** 12.47 kV (11 kV IEC): feeders from the substation to the pole transformers. */
	DISTRIBUTION,
	HIGH;

	public String label() {
		return ElectricalStandard.active().level(this).label();
	}

	/** The volts a source at this tier is meant to hold. */
	public double nominal() {
		double volts = ElectricalStandard.active().level(this).volts();
		// the single-phase taps of a distribution feeder run line to neutral: 12.47 kV / sqrt 3 = 7.2 kV
		return this == DISTRIBUTION ? volts / Math.sqrt(3.0) : volts;
	}
}
