package dev.wildcatter.block;

/**
 * A stage of the conversion plant's main line (docs/multiblocks/conversion.md) that holds uranium for the next stage touching it:
 * UO2 out of the reducer, UF4 out of the hydrofluorinator, UF6 out of the fluorinator, the cold traps and the columns. Counted as
 * kg of U.
 */
public interface UraniumStage {
	double uranium();

	/** Takes up to {@code kg} of U; returns what it gave. */
	double takeUranium(double kg);

	/**
	 * The share of the uranium it holds that came from 96 % U3O8 drums (the ADU route, docs/multiblocks/adu-yellowcake.md), 0 to 1. What
	 * it hands on carries the same share. A stage that does not track it holds magnesia route uranium only.
	 */
	default double aduShare() {
		return 0;
	}

	/** The share after {@code kg} of U at {@code share} joins {@code held} kg at {@code was}. */
	static double blend(double held, double was, double kg, double share) {
		return held + kg <= 1e-12 ? 0 : (held * was + kg * share) / (held + kg);
	}
}
