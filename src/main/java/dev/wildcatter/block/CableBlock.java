package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Voltage;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A thin wire. Grows an arm toward every neighbour that is part of the grid at its voltage.
 * A bare wire shocks anything that comes near it while it is live; an insulated one is safe to touch.
 */
public class CableBlock extends BaseEntityBlock {
	private final Voltage voltage;
	private final boolean insulated;

	public CableBlock(Properties properties, Voltage voltage, boolean insulated) {
		super(properties);
		this.voltage = voltage;
		this.insulated = insulated;
		registerDefaultState(WireShape.noArms(stateDefinition.any()));
	}

	/** How many cables of this voltage touch {@code pos}. Devices need two: one to feed them and one to return. */
	public static int cablesAround(BlockGetter level, BlockPos pos, Voltage voltage) {
		int count = 0;
		for (Direction side : Direction.values()) {
			if (level.getBlockState(pos.relative(side)).getBlock() instanceof CableBlock cable && cable.voltage == voltage) {
				count++;
			}
		}
		return count;
	}

	public Voltage voltage() {
		return voltage;
	}

	public boolean insulated() {
		return insulated;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		WireShape.ARMS.values().forEach(builder::add);
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
			state = state.setValue(WireShape.ARMS.get(side), joins);
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return WireShape.of(state);
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
