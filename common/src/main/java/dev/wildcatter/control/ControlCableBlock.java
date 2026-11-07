package dev.wildcatter.control;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Multi-core instrument cable, about 25 mm across. A cable makes one run: runs fan out at a junction box (docs/multiblocks/plc.md). */
public class ControlCableBlock extends Block {
	private static final Map<Direction, BooleanProperty> SIDES = new EnumMap<>(Direction.class);
	private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
	private static final VoxelShape CORE = Block.box(7, 7, 7, 9, 9, 9);

	static {
		SIDES.put(Direction.NORTH, BlockStateProperties.NORTH);
		SIDES.put(Direction.SOUTH, BlockStateProperties.SOUTH);
		SIDES.put(Direction.EAST, BlockStateProperties.EAST);
		SIDES.put(Direction.WEST, BlockStateProperties.WEST);
		SIDES.put(Direction.UP, BlockStateProperties.UP);
		SIDES.put(Direction.DOWN, BlockStateProperties.DOWN);
		ARMS.put(Direction.NORTH, Block.box(7, 7, 0, 9, 9, 7));
		ARMS.put(Direction.SOUTH, Block.box(7, 7, 9, 9, 9, 16));
		ARMS.put(Direction.WEST, Block.box(0, 7, 7, 7, 9, 9));
		ARMS.put(Direction.EAST, Block.box(9, 7, 7, 16, 9, 9));
		ARMS.put(Direction.DOWN, Block.box(7, 0, 7, 9, 7, 9));
		ARMS.put(Direction.UP, Block.box(7, 9, 7, 9, 16, 9));
	}

	public ControlCableBlock(Properties properties) {
		super(properties.noOcclusion());
		BlockState state = stateDefinition.any();
		for (BooleanProperty side : SIDES.values()) {
			state = state.setValue(side, false);
		}
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.EAST, BlockStateProperties.WEST, BlockStateProperties.UP, BlockStateProperties.DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		for (Direction side : Direction.values()) {
			state = state.setValue(SIDES.get(side), ControlNet.joins(context.getLevel(), context.getClickedPos().relative(side)));
		}
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction side, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return state.setValue(SIDES.get(side), ControlNet.joins(level, neighborPos));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = CORE;
		for (Direction side : Direction.values()) {
			if (state.getValue(SIDES.get(side))) {
				shape = Shapes.or(shape, ARMS.get(side));
			}
		}
		return shape;
	}
}
