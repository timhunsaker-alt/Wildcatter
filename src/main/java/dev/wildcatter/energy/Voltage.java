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
	HIGH;

	public String label() {
		return ElectricalStandard.active().level(this).label();
	}

	/** The volts a source at this tier is meant to hold. */
	public double nominal() {
		return ElectricalStandard.active().level(this).volts();
	}
}
