package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Role;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A thin wire. Red wires are the hot conductor and black wires are the neutral; a circuit needs both, running out
 * to the load and back. Grows an arm toward every neighbour with a terminal of its own role and voltage.
 *
 * Bare wire is cheap and live to the touch. Wool-wrapped wire is safe until the wool wears through (WEAR 3),
 * and then it is bare wire again.
 */
public class CableBlock extends BaseEntityBlock {
	/** 0 fresh, up to 3 worn out. Only wool-insulated wire ever leaves 0. */
	public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, 3);

	private final Voltage voltage;
	private final Role role;
	private final boolean wool;

	public CableBlock(Properties properties, Voltage voltage, Role role, boolean wool) {
		super(properties);
		this.voltage = voltage;
		this.role = role;
		this.wool = wool;
		registerDefaultState(WireShape.noArms(stateDefinition.any()).setValue(WEAR, 0));
	}

	public Voltage voltage() {
		return voltage;
	}

	public Role role() {
		return role;
	}

	public boolean wool() {
		return wool;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		WireShape.ARMS.values().forEach(builder::add);
		builder.add(WEAR);
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

	private BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			boolean joins = level.getBlockEntity(pos.relative(side)) instanceof CircuitPart part
					&& part.hasPort(side.getOpposite(), role, voltage);
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
		return new WireBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.WIRE_ENTITY, WireBlockEntity::tick);
	}
}
