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
 * A water pipe or a steel pipe. It grows an arm toward every pipe of its kind and every machine of its kind next to it. The pipes carry
 * nothing themselves: a pump pushes water through the whole connected run to the boilers touching it, and a turbine's
 * drain sends its condensate back the same way (see {@link Plumbing}). Each joined run of steel pipe carries the one product its
 * sources put on it, or is BLOCKED where two meet (see {@link Lines}); the Power Meter on any pipe says which.
 */
public class PipeBlock extends Block {
	/** Drawn at its real diameter (docs/multiblocks/well-site.md), but clicked and walked into as 0.25 m so it can be hit. */
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

	@Override
	protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
			net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (!stack.is(dev.wildcatter.WildcatterItems.POWER_METER)) {
			return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (kind == Kind.WATER) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Water pipe (2 in): " + (Plumbing.network(level, java.util.List.of(pos)).size() + 1)
						+ " lengths joined, carrying fresh water from a pump"));
			} else {
				Lines.Line line = Lines.line(level, pos);
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Steel pipe (4 in): " + line.pipes().size() + " lengths joined, "
						+ line.run().describe()));
			}
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
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
				? block instanceof BoilerBlock || block instanceof SteamTurbineBlock || block instanceof PumpBlock || block instanceof TowerBasinBlock
						|| block instanceof RigPartBlock rig && rig.part() == RigPartBlock.Part.MUD_TANKS
						|| block instanceof SteamPlantPartBlock plant && plant.part() == SteamPlantPartBlock.Part.CONDENSER
						|| block instanceof PetrochemPartBlock furnace && furnace.part() == PetrochemPartBlock.Part.CRACKING_FURNACE
				: block instanceof GasWellheadBlock gas && neighbour.getValue(gas.piece()) == 0 || block instanceof OilWellheadBlock || block instanceof FireboxBlock
						|| block instanceof LeasePartBlock || block instanceof DisposalWellheadBlock
						|| block instanceof GasPlantPartBlock gasPlant && gasPlant.part() != GasPlantPartBlock.Part.REFRIGERATION_COMPRESSOR
						|| block instanceof RefineryPartBlock refinery && refinery.part() != RefineryPartBlock.Part.CRUDE_CHARGE_PUMP
						|| block instanceof PetrochemPartBlock petrochem && petrochem.part() != PetrochemPartBlock.Part.PELLETIZER
						|| block instanceof GensetPartBlock part && (part.part() == GensetPartBlock.Part.FUEL_TANK || part.part() == GensetPartBlock.Part.GAS_ENGINE)
						|| block instanceof SteamPlantPartBlock burner && burner.part() == SteamPlantPartBlock.Part.BURNER;
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
