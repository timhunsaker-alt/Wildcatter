package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Where the fuel goes. Right-click to open it and add coal or wood; a hopper can feed it too. It only burns while the
 * boiler above it wants heat. Nine of them in a 3x3 floor join into one big firebox that shares a single fuel store.
 */
public class FireboxBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public FireboxBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT, FACING);
	}

	/** Its fire door at true size (docs/multiblocks/building-gear.md): toward whoever placed it. */
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FireboxBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.FIREBOX_ENTITY, FireboxBlockEntity::tick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox) {
			player.openMenu(firebox);
			player.sendOverlayMessage(firebox.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
