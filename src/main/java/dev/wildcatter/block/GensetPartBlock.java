package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * One part of a genset skid (docs/multiblocks/genset.md): engine, alternator, radiator, sub-base fuel tank or exhaust stack. Parts
 * do nothing alone; the control panel finds them, turns them to its own facing, and runs them. The fuel tank holds diesel (pour
 * in a bucket, or run a line from a refinery).
 */
public class GensetPartBlock extends BaseEntityBlock {
	public enum Part {
		DIESEL_ENGINE, GAS_ENGINE, ALTERNATOR, RADIATOR, FUEL_TANK, EXHAUST
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Part part;

	public GensetPartBlock(Properties properties, Part part) {
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
		if (part == Part.FUEL_TANK && stack.is(WildcatterItems.DIESEL_BUCKET) && level.getBlockEntity(pos) instanceof GensetPartBlockEntity tank) {
			if (!level.isClientSide()) {
				if (tank.addFuel(Tuning.DIESEL_BUCKET_UNITS)) {
					stack.shrink(1);
					ItemStack empty = new ItemStack(Items.BUCKET);
					if (stack.isEmpty()) {
						player.setItemInHand(hand, empty);
					} else if (!player.getInventory().add(empty)) {
						net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), empty);
					}
				}
				player.sendOverlayMessage(tank.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GensetPartBlockEntity p) {
			player.sendOverlayMessage(p.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GensetPartBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? createTickerHelper(type, WildcatterBlocks.GENSET_PART_ENTITY, GensetPartBlockEntity::clientTick) : null;
	}
}
