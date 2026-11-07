package dev.wildcatter.control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A field junction box: a steel box of terminals where a multi-core run fans out. Any number of cables may meet here. */
public class JunctionBoxBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);

	public JunctionBoxBlock(Properties properties) {
		super(properties.noOcclusion());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
