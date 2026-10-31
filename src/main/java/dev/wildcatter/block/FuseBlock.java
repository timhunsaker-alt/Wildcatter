package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A fuse: a thin strip of metal that melts when too much current runs through it. It sits in a hot wire like a breaker
 * (in at the back, out at the front) but cannot be reset: once it blows, replace its element with a copper ingot.
 */
public class FuseBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty BLOWN = BooleanProperty.create("blown");

	private final dev.wildcatter.energy.Voltage voltage;

	public FuseBlock(Properties properties) {
		this(properties, dev.wildcatter.energy.Voltage.LOW);
	}

	/** A fuse for a tier; the distribution one is a fused cutout. */
	public FuseBlock(Properties properties, dev.wildcatter.energy.Voltage voltage) {
		super(properties);
		this.voltage = voltage;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BLOWN, false));
	}

	public dev.wildcatter.energy.Voltage voltage() {
		return voltage;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BLOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (stack.is(Items.COPPER_INGOT) && state.getValue(BLOWN)) {
			if (bites(level, pos, player)) {
				return InteractionResult.SUCCESS;
			}
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FuseBlockEntity fuse) {
				fuse.replaceElement();
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				player.sendOverlayMessage(fuse.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (bites(level, pos, player)) {
			return InteractionResult.SUCCESS;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FuseBlockEntity fuse) {
			if (player.isShiftKeyDown() && !state.getValue(BLOWN)) {
				fuse.nextRating();
			}
			player.sendOverlayMessage(fuse.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	/** Hitting it to knock it down is touching it too. */
	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		bites(level, pos, player);
	}

	/**
	 * A fused cutout's fuse tube and contacts are bare and at line voltage (linemen work them with a hot stick): a hand on a live
	 * one is a shock, grounded or not. A low-voltage fuse is enclosed and safe to touch. Returns true if it bit.
	 */
	private boolean bites(Level level, BlockPos pos, Player player) {
		if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof FuseBlockEntity fuse && fuse.cutout() && fuse.isLive()
				&& !player.isCreative() && !player.isSpectator()) {
			Shock.zap(server, player, voltage, pos, fuse.volts());
			player.sendOverlayMessage(fuse.inspect());
			return true;
		}
		return false;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FuseBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return null;
	}
}
