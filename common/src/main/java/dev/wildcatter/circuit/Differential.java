package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * The 87T transformer differential (SEL-787 class) and the 87G generator differential (SEL-700G class), in per unit of the
 * protected machine's rated current. By Kirchhoff, the difference between the currents entering and leaving the zone at its
 * breakers' CTs is the current into a fault inside it. A percentage-restrained characteristic: it operates when that difference is
 * over the larger of a minimum pickup and a slope times the current through the zone. Plain Java, so it is unit-tested.
 */
public final class Differential {
	private Differential() {
	}

	public static boolean operates(double differencePu, double throughPu, double minimumPu, double slope) {
		return differencePu > pickup(throughPu, minimumPu, slope);
	}

	public static double pickup(double throughPu, double minimumPu, double slope) {
		return Math.max(minimumPu, slope * throughPu);
	}

	/** 87T, with the transformer's settings. */
	public static boolean transformer(double differencePu, double throughPu) {
		return operates(differencePu, throughPu, Tuning.XFMR_87_MIN_PU, Tuning.XFMR_87_SLOPE);
	}

	/** 87G, with the generator's settings. */
	public static boolean generator(double differencePu, double throughPu) {
		return operates(differencePu, throughPu, Tuning.GEN_87_MIN_PU, Tuning.GEN_87_SLOPE);
	}
}
