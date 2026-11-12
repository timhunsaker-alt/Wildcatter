package dev.wildcatter.block;

/**
 * An electric furnace fed by a furnace transformer set against it (docs/multiblocks/ferroalloys.md, nickel-smelter.md): the submerged
 * arc furnace and the nickel smelter's six-in-line furnace. The transformer asks it what it wants of its tap's power each solve.
 */
public interface TransformerFurnace {
	/** Watts it asks for, given what the transformer's tap gives at full electrode current. */
	double wantedWatts(double tapWatts);
}
