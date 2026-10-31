package dev.wildcatter.block;

import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A cooling tower's concrete cold-water basin, 4 x 4 m: the foot of the cell, where the water pipes join (any cell of it). */
public class TowerBasinBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(4, 1, 4, 0, 0);

	public TowerBasinBlock(Properties properties) {
		super(properties, FOOTPRINT, "tower_basin");
	}

	@Override
	protected PartLayout layout() {
		return TowerColumn.layout();
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new TowerBasinBlockEntity(pos, state);
	}
}
