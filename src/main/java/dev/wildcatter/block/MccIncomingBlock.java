package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The incoming section of a motor control center (docs/multiblocks/motor-control-center.md): the controller. The feeder comes in
 * on its back or top, through the main three-pole breaker, onto the bus that runs to the buckets on its right. Right-click switches
 * the main breaker (and resets it after a trip); sneak and right-click changes its rating.
 */
public class MccIncomingBlock extends BaseEntityBlock {
	/** The way the front faces (toward whoever placed it). */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<BreakerBlock.Position> POSITION = BreakerBlock.POSITION;

	public MccIncomingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POSITION, BreakerBlock.Position.ON));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POSITION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof MccIncomingBlockEntity mcc)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			mcc.nextRating();
		} else {
			mcc.setSwitch(state.getValue(POSITION) != BreakerBlock.Position.ON);
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 0.9F);
		}
		player.sendOverlayMessage(mcc.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MccIncomingBlockEntity(pos, state);
	}
}
