package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A surveyor's stake at a blueprint's origin (docs/blueprints.md, "Site marker"). The blueprint item places it; right-click reads the
 * site's progress and its next lift.
 */
public class SiteMarkerBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 14, 10);

	public SiteMarkerBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SiteMarkerBlockEntity site) {
			site.recount();
			player.sendOverlayMessage(site.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SiteMarkerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? createTickerHelper(type, WildcatterBlocks.SITE_MARKER_ENTITY, SiteMarkerBlockEntity::clientTick)
				: createTickerHelper(type, WildcatterBlocks.SITE_MARKER_ENTITY, SiteMarkerBlockEntity::serverTick);
	}
}
