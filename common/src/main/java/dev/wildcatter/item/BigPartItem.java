package dev.wildcatter.item;

import dev.wildcatter.multiblock.BigPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** The item of a true-size part: before placing, it asks the part where a click really puts it (see {@link BigPart#snap}). */
public class BigPartItem extends BlockItem {
	public BigPartItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** Snaps first: vanilla checks that the clicked spot is free before it asks {@link #updatePlacementContext}, and the slot is what matters. */
	@Override
	public net.minecraft.world.InteractionResult place(BlockPlaceContext context) {
		return super.place(((BigPart) getBlock()).snap(context));
	}

	@Override
	public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
		return ((BigPart) getBlock()).snap(context);
	}
}
