package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Joins two voltage levels. The side it faces is the higher voltage, the opposite side is the lower one, and the
 * four other sides connect to nothing. Power can flow either way.
 */
public class TransformerBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Voltage low;
	private final Voltage high;
	/** A pole-mount unit has a kVA rating (it heats on overload and fails); the others are not rated. 0 means unrated. */
	private final double ratedVa;

	public TransformerBlock(Properties properties, Voltage low, Voltage high) {
		this(properties, low, high, 0);
	}

	public TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa) {
		super(properties);
		this.low = low;
		this.high = high;
		this.ratedVa = ratedVa;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public Voltage low() {
		return low;
	}

	public Voltage high() {
		return high;
	}

	public double ratedVa() {
		return ratedVa;
	}

	/** A pole-mount transformer hangs on a pole: one must stand beside it. */
	@Override
	protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
		if (ratedVa <= 0) {
			return true;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(side)).is(WildcatterBlocks.POLE)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// The high side faces away from the player, so the near side (where you stand) is the lower voltage.
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TransformerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || ratedVa <= 0 ? null : createTickerHelper(type, WildcatterBlocks.TRANSFORMER_ENTITY, TransformerBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TransformerBlockEntity transformer) {
			if (!Shock.touch((ServerLevel) level, pos, player, transformer.topVoltage(), transformer.isLive())) {
				player.sendOverlayMessage(transformer.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
