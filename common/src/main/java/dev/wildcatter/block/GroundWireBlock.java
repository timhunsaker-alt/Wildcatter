package dev.wildcatter.block;

import dev.wildcatter.safety.Bondable;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Green wire that carries no power: it only ties machines to a ground rod. */
public class GroundWireBlock extends Block {
	public GroundWireBlock(Properties properties) {
		super(properties);
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

	private static BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			BlockPos next = pos.relative(side);
			boolean joins = level.getBlockState(next).getBlock() instanceof GroundWireBlock
					|| level.getBlockState(next).getBlock() instanceof GroundRodBlock
					|| level.getBlockEntity(next) instanceof Bondable;
			state = state.setValue(WireShape.ARMS.get(side), joins);
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return WireShape.of(state);
	}
}
