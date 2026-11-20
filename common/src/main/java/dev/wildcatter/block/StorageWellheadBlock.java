package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A gas storage wellhead with its subsurface safety valve (docs/multiblocks/oilfield-follow-ons.md, 23c), set on a brine well's hole when
 * leaching ends. The storage compressor puts gas in by pipe; debrining pushes the brine up its inner string and out of the side it
 * faces to a disposal wellhead; the glycol dehydrator draws it out. A click cycles INJECT / WITHDRAW / SHUT IN.
 */
public class StorageWellheadBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

	public StorageWellheadBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!context.getLevel().getBlockState(context.getClickedPos().below()).is(WildcatterBlocks.CASING)) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.literal("A storage wellhead goes on a brine well's casing, its cavern leached"));
			}
			return null;
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageWellheadBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, dev.wildcatter.WildcatterBlocks.STORAGE_WELLHEAD_ENTITY, StorageWellheadBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageWellheadBlockEntity well) {
			player.sendOverlayMessage(player.isShiftKeyDown() ? well.inspect() : Component.literal(well.cycleMode()));
		}
		return InteractionResult.SUCCESS;
	}
}
