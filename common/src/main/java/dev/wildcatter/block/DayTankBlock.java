package dev.wildcatter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A 1 m³ water day tank, an intermediate bulk tank's size: filled by a pump on its pipes, emptied through a valve touching it. */
public class DayTankBlock extends BaseEntityBlock {
	public DayTankBlock(Properties properties) {
		super(properties.noOcclusion());   // its model is smaller than a cube: it must not hide its neighbours' faces
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DayTankBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, dev.wildcatter.WildcatterBlocks.DAY_TANK_ENTITY, DayTankBlockEntity::tick);
	}
}
