package dev.wildcatter.block;

/**
 * A stage of the fuel plant's line (docs/multiblocks/fuel.md) that holds uranium for the next stage touching it: UF6 vapour out of
 * the autoclave, powder out of the kiln and the granulator, pellets out of the press, the furnace and the grinder. Counted as kg of U.
 */
public interface FuelStage {
	double fuelU();

	/** Takes up to {@code kg} of U; returns what it gave. */
	double takeFuelU(double kg);

	/** For the game test: uranium put straight into it. */
	void addFuelU(double kg);

	/** 16c1: the U-235 assay of the uranium it holds, from the 30B it came in (docs/multiblocks/enrichment-operations.md). */
	default double fuelAssay() {
		return dev.wildcatter.Tuning.ASSAY_PRODUCT;
	}

	/** The assay after {@code kg} at {@code a} joins {@code held} kg at {@code was}. */
	static double blend(double held, double was, double kg, double a) {
		return held + kg <= 1e-12 ? a : (held * was + kg * a) / (held + kg);
	}
}
