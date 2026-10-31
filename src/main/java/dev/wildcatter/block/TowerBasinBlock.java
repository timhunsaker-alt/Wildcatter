package dev.wildcatter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A cooling tower's cold-water basin: the foot of the column, where the water pipes join. */
public class TowerBasinBlock extends BaseEntityBlock {
	public TowerBasinBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TowerBasinBlockEntity(pos, state);
	}
}
