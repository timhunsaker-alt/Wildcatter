package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A plug adapter: it sets against a receptacle of its own type (it will not stay beside any other) and gives the machines or cables
 * beside it power as FE, drawing the matching watts from the grid.
 */
public class PlugAdapterBlock extends BaseEntityBlock {

	private final PlugType type;

	public PlugAdapterBlock(Properties properties, PlugType type) {
		super(properties);
		this.type = type;
	}

	public PlugType type() {
		return type;
	}

	/** True if a receptacle of this plug's type is beside the position. */
	public static boolean fits(LevelReader level, BlockPos pos, PlugType type) {
		for (Direction side : Direction.values()) {
			if (level.getBlockState(pos.relative(side)).getBlock() instanceof ReceptacleBlock receptacle && receptacle.type() == type) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return fits(level, pos, type);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		if (!canSurvive(state, level, pos)) {
			level.destroyBlock(pos, true);
		}
		CircuitManager.markDirty(level);
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AttachedGear.ATTACH);
	}

	/** Plugged into the receptacle beside it (the one of its type), its cord running away from it. */
	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		for (Direction side : Direction.values()) {
			if (context.getLevel().getBlockState(context.getClickedPos().relative(side)).getBlock() instanceof ReceptacleBlock receptacle && receptacle.type() == type) {
				return defaultBlockState().setValue(AttachedGear.ATTACH, side);
			}
		}
		return AttachedGear.place(defaultBlockState(), context);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AttachedGear.shape(state, 5, 2);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PlugAdapterBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.PLUG_ADAPTER_ENTITY, PlugAdapterBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlugAdapterBlockEntity plug) {
			player.sendOverlayMessage(plug.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
