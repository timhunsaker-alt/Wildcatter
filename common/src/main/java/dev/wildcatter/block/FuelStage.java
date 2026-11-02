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
}
