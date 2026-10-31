package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * One part of the 1 MW steam plant (docs/multiblocks/steam-plant.md): burner, drum, superheater, turbine casing, condenser or
 * feed pump. The turbine generator finds them and runs the plant. The burner takes coal, charcoal or coal blocks (by hand or
 * hopper) or pipeline gas; the feed pump is a 480 V three-phase motor that needs power before the plant can run.
 */
public class SteamPlantPartBlock extends BaseEntityBlock {
	public enum Part {
		BURNER, DRUM, SUPERHEATER, TURBINE, CONDENSER, FEED_PUMP
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Part part;

	public SteamPlantPartBlock(Properties properties, Part part) {
		super(properties);
		this.part = part;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public Part part() {
		return part;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (part == Part.BURNER && SteamPlantPartBlockEntity.joulesOf(stack) > 0 && level.getBlockEntity(pos) instanceof SteamPlantPartBlockEntity burner) {
			if (!level.isClientSide()) {
				ItemStack rest = burner.insertFuel(stack.copy());
				if (!player.getAbilities().instabuild) {
					player.setItemInHand(hand, rest);
				}
				player.sendOverlayMessage(burner.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamPlantPartBlockEntity p) {
			player.sendOverlayMessage(p.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SteamPlantPartBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? createTickerHelper(type, WildcatterBlocks.STEAM_PLANT_PART_ENTITY, SteamPlantPartBlockEntity::clientTick) : null;
	}
}
