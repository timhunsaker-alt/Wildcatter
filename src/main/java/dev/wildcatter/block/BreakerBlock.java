package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
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
 * A circuit breaker. It sits in a hot wire, with the wire coming in at the back and going out at the front.
 * Right-click to switch it off and on (and to reset it after it trips); sneak and right-click to change its rating.
 */
public class BreakerBlock extends BaseEntityBlock {
	public enum Position implements StringRepresentable {
		ON("on"), OFF("off"), TRIPPED("tripped");

		private final String name;

		Position(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Position> POSITION = EnumProperty.create("position", Position.class);

	private final Voltage voltage;
	private final boolean plainSwitch;
	private final boolean relay;

	public BreakerBlock(Properties properties, Voltage voltage) {
		this(properties, voltage, false);
	}

	/** A plain switch has no protection: it only opens and closes by hand. */
	public BreakerBlock(Properties properties, Voltage voltage, boolean plainSwitch) {
		this(properties, voltage, plainSwitch, false);
	}

	/** An under-frequency relay opens when the island's frequency falls too far (load shedding) and protects nothing else. */
	public BreakerBlock(Properties properties, Voltage voltage, boolean plainSwitch, boolean relay) {
		super(properties);
		this.relay = relay;
		this.voltage = voltage;
		this.plainSwitch = plainSwitch;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POSITION, Position.ON));
	}

	public Voltage voltage() {
		return voltage;
	}

	public boolean isSwitch() {
		return plainSwitch;
	}

	public boolean isRelay() {
		return relay;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POSITION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// The wire runs along the direction you are facing: in at the near side, out at the far side.
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BreakerBlockEntity breaker)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown() && !plainSwitch && !relay) {
			breaker.nextRating();
			player.sendOverlayMessage(breaker.inspect());
		} else {
			boolean closing = state.getValue(POSITION) != Position.ON;
			breaker.setSwitch(closing);
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, closing ? 1.0F : 0.85F);
			player.sendOverlayMessage(breaker.inspect());
		}
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BreakerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return null;
	}
}
