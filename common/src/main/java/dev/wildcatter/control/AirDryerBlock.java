package dev.wildcatter.control;

import net.minecraft.world.level.block.Block;

/** A desiccant air dryer. Instrument air must pass through one: wet air fouls positioners, so a compressor feeds tubing only through this. */
public class AirDryerBlock extends Block {
	public AirDryerBlock(Properties properties) {
		super(properties);
	}
}
