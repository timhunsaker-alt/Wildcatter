package dev.wildcatter.control;

import dev.wildcatter.WildcatterSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A wall-mounted operator device on 24 V: a pushbutton, selector, e-stop, pilot light, stack light or horn (docs/multiblocks/plc.md). */
public class OperatorDeviceBlock extends BaseEntityBlock {
	public enum Kind {
		PUSHBUTTON, SELECTOR, ESTOP, PULL_CORD, PILOT_LIGHT, STACK_LIGHT, HORN
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** What it shows: bit 0 lit, pressed or latched; a stack light's three lamps, red, amber and green, are bits 0, 1 and 2. */
	public static final IntegerProperty LAMPS = IntegerProperty.create("lamps", 0, 7);
	private static final VoxelShape NORTH = Block.box(4, 4, 12, 12, 12, 16);
	private static final VoxelShape SOUTH = Block.box(4, 4, 0, 12, 12, 4);
	private static final VoxelShape WEST = Block.box(12, 4, 4, 16, 12, 12);
	private static final VoxelShape EAST = Block.box(0, 4, 4, 4, 12, 12);
	private final Kind kind;

	public OperatorDeviceBlock(Properties properties, Kind kind) {
		super(properties.noOcclusion());
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LAMPS, 0));
	}

	public Kind kind() {
		return kind;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LAMPS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
			default -> NORTH;
		};
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof OperatorDeviceBlockEntity device) {
			if (device.operate()) {
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.5F, 1.6F);
			}
			player.sendOverlayMessage(device.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new OperatorDeviceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, dev.wildcatter.WildcatterBlocks.OPERATOR_DEVICE_ENTITY, OperatorDeviceBlockEntity::tick);
	}
}
