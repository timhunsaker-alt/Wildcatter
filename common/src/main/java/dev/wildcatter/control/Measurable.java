package dev.wildcatter.control;

/** A machine a transmitter can clamp to: real numbers, where most machines otherwise report only text (docs/multiblocks/plc.md). */
public interface Measurable {
	enum Quantity {
		LEVEL("%", "LT"), PRESSURE("bar", "PT"), FLOW("L/s", "FT"), TEMPERATURE("C", "TT"), SPEED("%", "ST"), POSITION("%", "ZT"), VOLTS("V", "ET"), AMPS("A", "IT"), WATTS("W", "JT"), HERTZ("Hz", "SIT"), POWER_FACTOR("PF", "PFT"),
		WEIGHT("kg/s", "WT"), DENSITY("kg/L", "DT"), PH("pH", "AT"), ASSAY("%", "AIT");

		public final String unit;
		/** The instrument letters ISA-5.1 gives it. */
		public final String letters;

		Quantity(String unit, String letters) {
			this.unit = unit;
			this.letters = letters;
		}
	}

	/** The reading, or NaN if this machine has no such reading. */
	double measure(Quantity quantity);
}
