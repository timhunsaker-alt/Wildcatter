package dev.wildcatter.block;

import net.minecraft.world.level.block.IronBarsBlock;

/**
 * Decoration for a substation yard (docs/multiblocks/substation.md): chain-link fence fabric, or the barbed-wire top that
 * finishes a 2.1 m substation fence (NESC). Each is a block that joins its neighbours like iron bars; stack the top on the fence.
 */
public class ChainLinkFenceBlock extends IronBarsBlock {
	public ChainLinkFenceBlock(Properties properties) {
		super(properties);
	}
}
