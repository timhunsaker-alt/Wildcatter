package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SteamTurbineBlock extends BaseEntityBlock {
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");
	/** Part of a medium voltage generator (a 3x3x3 cube of turbines). */

	public SteamTurbineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(RUNNING, false));
	}

	/** Its shaft line at true size (docs/multiblocks/building-gear.md): the turbine at the back, the generator at the front. */
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RUNNING, FACING);
	}

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SteamTurbineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.STEAM_TURBINE_ENTITY, level.isClientSide() ? (l, p, st, be) -> dev.wildcatter.kinetic.RunningSound.clientTick(be) : SteamTurbineBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamTurbineBlockEntity turbine) {
			if (turbine.isTripped()) {
				turbine.resetTrip();
			}
			if (!Shock.touch((ServerLevel) level, pos, player, turbine.voltage(), turbine.isLive())) {
				player.sendOverlayMessage(turbine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
