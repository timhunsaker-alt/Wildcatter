package dev.wildcatter.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A block that is one real component at true scale (docs/multiblocks/README.md, "Big parts"): placed, it fills every cell of
 * its {@link Footprint}. Each cell is the same block with its own {@link #piece} number; piece 0 is the main cell and holds the
 * block entity. {@link BigParts} does the placing, breaking, shapes and click forwarding, so every big part behaves the same.
 */
public interface BigPart {
	Footprint footprint();

	/** The block-state property that numbers the pieces, 0 to {@code footprint().pieces() - 1}. */
	IntegerProperty piece();

	/** Which way the part faces in this state (its footprint is drawn facing north). */
	Direction facing(BlockState state);

	/** The state the part would be placed in, whether or not it fits (the ghost outline shows where it would go). */
	BlockState placementState(net.minecraft.world.item.context.BlockPlaceContext context);

	/** The model tool's name for this part: its collision boxes are in {@code assets/wildcatter/shapes/<name>.json}. */
	String shapeName();
}
