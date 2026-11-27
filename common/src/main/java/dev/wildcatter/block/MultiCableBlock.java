package dev.wildcatter.block;

import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A three-phase cable: one insulated block carrying all five conductors, A, B, C, N and G, as the cable in a cable tray or a
 * conduit run does (docs/ELECTRICAL_MODEL.md). It joins its neighbours conductor by conductor: another cable or a three-phase
 * machine joins on all five, a single wire joins only on the one it carries. The insulation never wears and it never shorts. The same block
 * with only hot (A), neutral and ground is single-phase cable, the sheathed cable of house wiring.
 */
public class MultiCableBlock extends BaseEntityBlock {
	/** The five conductors of three-phase cable. */
	public static final java.util.Set<Conductor> THREE_PHASE = java.util.EnumSet.allOf(Conductor.class);
	/** Hot, neutral and ground: single-phase cable, as sheathed house wiring is. */
	public static final java.util.Set<Conductor> SINGLE_PHASE = java.util.EnumSet.of(Conductor.A, Conductor.N, Conductor.G);
	/** Width in pixels at each gauge: three-phase cable keeps the old 4 pixels for 12 and 10 AWG; single-phase cable is a size thinner. */
	private static final double[] CABLE_WIDTH = {4, 5, 6, 7};
	private static final double[] SINGLE_WIDTH = {3, 4, 5, 6};
	private static final VoxelShape[][] CABLE = new VoxelShape[4][64];
	private static final VoxelShape[][] SINGLE = new VoxelShape[4][64];

	static {
		for (int g = 0; g < 4; g++) {
			for (int mask = 0; mask < 64; mask++) {
				CABLE[g][mask] = WireShape.build(CABLE_WIDTH[g], mask);
				SINGLE[g][mask] = WireShape.build(SINGLE_WIDTH[g], mask);
			}
		}
	}

	private final Voltage voltage;
	private final boolean busway;
	private final java.util.Set<Conductor> conductors;

	public MultiCableBlock(Properties properties, Voltage voltage, boolean busway) {
		this(properties, voltage, busway, THREE_PHASE);
	}

	public MultiCableBlock(Properties properties, Voltage voltage, boolean busway, java.util.Set<Conductor> conductors) {
		super(properties);
		this.voltage = voltage;
		this.busway = busway;
		this.conductors = conductors;
		registerDefaultState(WireShape.noArms(stateDefinition.any()).setValue(WireShape.GAUGE, dev.wildcatter.energy.ConductorSize.Gauge.SMALL));
	}

	public Voltage voltage() {
		return voltage;
	}

	/** An 800 A busway rather than a cable: the same five conductors as bars in a steel housing. */
	public boolean busway() {
		return busway;
	}

	/** The conductors it carries: all five, or hot, neutral and ground for single-phase cable. */
	public java.util.Set<Conductor> conductors() {
		return conductors;
	}

	public boolean singlePhase() {
		return conductors.size() == SINGLE_PHASE.size();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		WireShape.ARMS.values().forEach(builder::add);
		builder.add(WireShape.GAUGE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		dev.wildcatter.energy.ConductorSize size = dev.wildcatter.item.ConductorItem.size(context.getItemInHand(), voltage,
				busway ? dev.wildcatter.energy.ConductorSize.Kind.BUSWAY : dev.wildcatter.energy.ConductorSize.Kind.CABLE);
		return withArms(defaultBlockState().setValue(WireShape.GAUGE, busway ? dev.wildcatter.energy.ConductorSize.Gauge.SMALL : size.gauge()), context.getLevel(), context.getClickedPos());
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
		// set by a command or a blueprint (not from its item), its arms are found here; set them with a neighbour update, since vanilla
		// skips the one for this placement once the state has changed under it, and the older neighbours would never reach back
		BlockState updated = withArms(state, level, pos);
		if (updated != state) {
			level.setBlock(pos, updated, Block.UPDATE_ALL);
		}
		CircuitManager.markDirty(level);
	}

	/** Whether it has terminals on {@code face}: a cable on every face, a branch cable only at its ends. */
	public boolean joins(BlockState state, Direction face) {
		return true;
	}

	/** An arm toward every neighbour with a terminal of any conductor at this voltage. */
	private BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			boolean joins = false;
			if (!joins(state, side)) {
				state = state.setValue(WireShape.ARMS.get(side), false);
				continue;
			}
			for (Conductor conductor : conductors) {
				joins |= CircuitPart.portAt(level, pos.relative(side), side.getOpposite(), conductor, voltage);
			}
			state = state.setValue(WireShape.ARMS.get(side), joins);
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int gauge = busway ? 0 : WireShape.gauge(state).ordinal();
		return (singlePhase() ? SINGLE : CABLE)[gauge][WireShape.mask(state)];
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
