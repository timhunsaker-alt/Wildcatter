package dev.wildcatter.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A water pipe or a gas line. It grows an arm toward every pipe of its kind and every machine of its kind next to it. The pipes carry nothing
 * themselves: a pump pushes water through the whole connected run to the boilers touching it, and a turbine's
 * drain sends its condensate back the same way. See {@link Plumbing}.
 */
public class PipeBlock extends Block {
	private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
	private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);

	static {
		ARM_SHAPES.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
		ARM_SHAPES.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
		ARM_SHAPES.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
		ARM_SHAPES.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
		ARM_SHAPES.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
		ARM_SHAPES.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
	}

	/** Water pipes link boilers, turbines and pumps; gas lines link wells and fireboxes. The two never join. */
	public enum Kind {
		WATER, GAS
	}

	private final Kind kind;

	public PipeBlock(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
		registerDefaultState(WireShape.noArms(stateDefinition.any()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		WireShape.ARMS.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withArms(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		BlockState updated = withArms(state, level, pos);
		if (updated != state) {
			level.setBlock(pos, updated, 2);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		neighborChanged(state, level, pos, this, null, movedByPiston);
	}

	public Kind kind() {
		return kind;
	}

	private boolean joins(BlockState neighbour) {
		Block block = neighbour.getBlock();
		if (block instanceof PipeBlock pipe) {
			return pipe.kind == kind;
		}
		return kind == Kind.WATER
				? block instanceof BoilerBlock || block instanceof SteamTurbineBlock || block instanceof PumpBlock
				: block instanceof GasWellheadBlock || block instanceof PumpjackBlock || block instanceof FireboxBlock;
	}

	private BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			state = state.setValue(WireShape.ARMS.get(side), joins(level.getBlockState(pos.relative(side))));
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = CORE;
		for (Direction side : Direction.values()) {
			if (state.getValue(WireShape.ARMS.get(side))) {
				shape = Shapes.or(shape, ARM_SHAPES.get(side));
			}
		}
		return shape;
	}
}
