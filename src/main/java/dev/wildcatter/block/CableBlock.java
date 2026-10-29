package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Voltage;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A thin copper cable. Grows an arm toward every neighbour that is part of the grid. */
public class CableBlock extends BaseEntityBlock {

	public static final Map<Direction, BooleanProperty> ARMS = new EnumMap<>(Direction.class);
	static {
		for (Direction side : Direction.values()) {
			ARMS.put(side, BooleanProperty.create(side.getName()));
		}
	}

	private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
	private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);
	static {
		ARM_SHAPES.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
		ARM_SHAPES.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
		ARM_SHAPES.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
		ARM_SHAPES.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
		ARM_SHAPES.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
		ARM_SHAPES.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));
	}

	private final Voltage voltage;

	public CableBlock(Properties properties, Voltage voltage) {
		super(properties);
		this.voltage = voltage;
		BlockState state = stateDefinition.any();
		for (BooleanProperty arm : ARMS.values()) {
			state = state.setValue(arm, false);
		}
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		ARMS.values().forEach(builder::add);
	}

	public Voltage voltage() {
		return voltage;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withArms(defaultBlockState(), context.getLevel(), context.getClickedPos(), voltage);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		BlockState updated = withArms(state, level, pos, voltage);
		if (updated != state) {
			level.setBlock(pos, updated, 2);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		neighborChanged(state, level, pos, this, null, movedByPiston);
	}

	private static BlockState withArms(BlockState state, BlockGetter level, BlockPos pos, Voltage voltage) {
		for (Direction side : Direction.values()) {
			boolean joins = level.getBlockEntity(pos.relative(side)) instanceof EnergyNode node && node.connectsTo(side.getOpposite(), voltage);
			state = state.setValue(ARMS.get(side), joins);
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = CORE;
		for (Direction side : Direction.values()) {
			if (state.getValue(ARMS.get(side))) {
				shape = Shapes.or(shape, ARM_SHAPES.get(side));
			}
		}
		return shape;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.CABLE_ENTITY, CableBlockEntity::tick);
	}
}
