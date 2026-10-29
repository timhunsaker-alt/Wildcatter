package dev.wildcatter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A copper rod driven into the earth. Put it against a machine (or run ground wire to it) to ground the machine. */
public class GroundRodBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(7, 0, 7, 9, 16, 9);

	public GroundRodBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
