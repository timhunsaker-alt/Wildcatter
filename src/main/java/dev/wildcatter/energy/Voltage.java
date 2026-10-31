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
	HIGH,
	/** 480Y/277 V (400Y/230 V IEC): industrial low voltage, for motors and plants. Last in the list so saved tiers keep their numbers. */
	INDUSTRIAL;

	public String label() {
		return ElectricalStandard.active().level(this).label();
	}

	/**
	 * The volts a source at this tier holds from each phase to neutral. Three-phase voltages are quoted line to line
	 * (4.16 kV, 12.47 kV, 138 kV), and line to neutral is that divided by sqrt 3 (2.40 kV, 7.2 kV, 79.7 kV). The low tier
	 * is quoted line to neutral already (120 V, 230 V).
	 */
	public double nominal() {
		double volts = ElectricalStandard.active().level(this).volts();
		return this == LOW ? volts : volts / Math.sqrt(3.0);
	}

	/** Higher tiers rank above lower ones, whatever their order in the list. */
	public boolean above(Voltage other) {
		return lineToLine() > other.lineToLine() * 1.01;
	}

	/** Line to line: the voltage between two phases, sqrt 3 times line to neutral. */
	public double lineToLine() {
		return nominal() * Math.sqrt(3.0);
	}
}
