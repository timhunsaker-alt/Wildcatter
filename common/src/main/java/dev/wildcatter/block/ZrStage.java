package dev.wildcatter.block;

/**
 * A stage of the zirconium plant's line (docs/multiblocks/zirconium.md) that holds metal for the next stage touching it: zircon out
 * of the separator, ZrCl4 out of the chlorinator and the hafnium column, sponge out of the Kroll reactor and the vacuum distillation,
 * ingot out of the VAR furnace, hollows out of the extrusion press. Counted as kg of metal.
 */
public interface ZrStage {
	double zirconium();

	/** Takes up to {@code kg} of metal; returns what it gave. */
	double takeZirconium(double kg);

	/** For the game test: metal put straight into it. */
	void addZirconium(double kg);
}
