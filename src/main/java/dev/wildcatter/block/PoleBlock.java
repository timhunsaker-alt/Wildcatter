package dev.wildcatter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A utility pole section. Stack them as tall as the line needs: a bare 138 kV line arcs to anything within
 * {@link dev.wildcatter.Tuning#BARE_REACH_HIGH} blocks, so it has to be hung higher than that above where people walk.
 * You can climb a pole, which is exactly how you end up next to the line.
 */
public class PoleBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

	public PoleBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
