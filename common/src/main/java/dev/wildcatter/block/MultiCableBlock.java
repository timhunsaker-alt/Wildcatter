package dev.wildcatter.block;

import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A three-phase cable: one insulated block carrying all five conductors, A, B, C, N and G, as the cable in a cable tray or a
 * conduit run does (docs/ELECTRICAL_MODEL.md). It joins its neighbours conductor by conductor: another cable or a three-phase
 * machine joins on all five, a single wire joins only on the one it carries. The insulation never wears and it never shorts.
 */
public class MultiCableBlock extends BaseEntityBlock {
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
	private final boolean busway;

	public MultiCableBlock(Properties properties, Voltage voltage, boolean busway) {
		super(properties);
		this.voltage = voltage;
		this.busway = busway;
		registerDefaultState(WireShape.noArms(stateDefinition.any()));
	}

	public Voltage voltage() {
		return voltage;
	}

	/** An 800 A busway rather than a cable: the same five conductors as bars in a steel housing. */
	public boolean busway() {
		return busway;
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
		CircuitManager.markDirty(level);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		neighborChanged(state, level, pos, this, null, movedByPiston);
	}

	/** An arm toward every neighbour with a terminal of any conductor at this voltage. */
	private BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			boolean joins = false;
			for (Conductor conductor : Conductor.values()) {
				joins |= CircuitPart.portAt(level, pos.relative(side), side.getOpposite(), conductor, voltage);
			}
			state = state.setValue(WireShape.ARMS.get(side), joins);
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

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MultiCableBlockEntity(pos, state);
	}
	/** A broken conductor drops with its size (the size lives in the block entity, not the block state). */
	@Override
	protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		java.util.List<net.minecraft.world.item.ItemStack> drops = super.getDrops(state, params);
		net.minecraft.world.level.block.entity.BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
		dev.wildcatter.energy.ConductorSize size = be instanceof WireBlockEntity wire ? wire.size() : be instanceof MultiCableBlockEntity cable ? cable.size() : null;
		if (size != null) {
			for (net.minecraft.world.item.ItemStack drop : drops) {
				if (drop.is(asItem())) {
					drop.set(dev.wildcatter.WildcatterComponents.CONDUCTOR_SIZE, size);
				}
			}
		}
		return drops;
	}
}
