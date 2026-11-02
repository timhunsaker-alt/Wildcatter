package dev.wildcatter.block;

import net.minecraft.world.level.block.Block;

/**
 * The steel pipe a drilling rig leaves in the hole, cemented into the rock. It holds the rock back and carries the oil or gas up.
 * It cannot be broken, like bedrock: a miner who tunnels into one has found a well (the field itself is not in the rock to take).
 */
public class CasingBlock extends Block {
	public CasingBlock(Properties properties) {
		super(properties);
	}
}
