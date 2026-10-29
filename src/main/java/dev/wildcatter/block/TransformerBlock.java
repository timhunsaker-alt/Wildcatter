package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
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

	public TransformerBlock(Properties properties, Voltage low, Voltage high) {
		super(properties);
		this.low = low;
		this.high = high;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public Voltage low() {
		return low;
	}

	public Voltage high() {
		return high;
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
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.TRANSFORMER_ENTITY, TransformerBlockEntity::tick);
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
