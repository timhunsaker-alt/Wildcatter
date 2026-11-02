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
}
