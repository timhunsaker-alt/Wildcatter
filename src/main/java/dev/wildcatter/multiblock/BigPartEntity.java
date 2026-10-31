package dev.wildcatter.multiblock;

/**
 * The block entity in a {@link BigPart}'s main cell remembers, saved with the world, whether the part has grown into its
 * cells. A grown part that has lost a piece is broken; a main cell that never grew is an older world's one-block part.
 */
public interface BigPartEntity {
	boolean grown();

	void markGrown();
}
