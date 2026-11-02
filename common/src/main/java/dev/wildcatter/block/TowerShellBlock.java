package dev.wildcatter.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What makes the natural-draft tower solid (docs/multiblocks/natural-draft-tower.md): an invisible, unbreakable block the tower's
 * controller places along its rim, raker columns and each finished lift. The tower is drawn by the controller's renderer.
 */
public class TowerShellBlock extends Block {
	public TowerShellBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}
}
