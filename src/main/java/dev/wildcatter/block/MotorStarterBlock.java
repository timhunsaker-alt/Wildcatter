package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A combination motor starter: a motor circuit protector, a contactor and an overload relay in one box, in front of one 480 V
 * three-phase motor. Right-click (or a redstone signal) starts and stops it; right-click after a trip resets it; sneak and right-click
 * steps the overload dial (set it to the motor's full-load amps).
 *
 * The same box is a bucket of a motor control center when {@code bucket} is set: it then takes its line from the MCC's bus and sends
 * the motor's supply out of its back or top.
 */
public class MotorStarterBlock extends BaseEntityBlock {
	public enum Run implements StringRepresentable {
		STOPPED("stopped"), RUNNING("running"), TRIPPED("tripped");

		private final String name;

		Run(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	/** The way the front faces (toward whoever placed it). */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Run> RUN = EnumProperty.create("run", Run.class);

	private final boolean bucket;

	public MotorStarterBlock(Properties properties, boolean bucket) {
		super(properties);
		this.bucket = bucket;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUN, Run.STOPPED));
	}

	public boolean bucket() {
		return bucket;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RUN);
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
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof MotorStarterBlockEntity starter)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			starter.nextOverloadSetting();
		} else {
			starter.pushButton();
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		player.sendOverlayMessage(starter.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	/** A redstone signal starts it; losing the signal stops it. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MotorStarterBlockEntity starter) {
			starter.redstone(level.hasNeighborSignal(pos));
		}
		CircuitManager.markDirty(level);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MotorStarterBlockEntity(pos, state);
	}
}
