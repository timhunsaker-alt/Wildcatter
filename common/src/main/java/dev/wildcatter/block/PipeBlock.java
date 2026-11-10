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
	private static final Map<Direction, VoxelShape> ARM_SHAPES = arms(6, 10);
	/** The 600 mm slurry pipe and air header: clicked and walked into at their size. */
	private static final VoxelShape BIG_CORE = Block.box(3, 3, 3, 13, 13, 13);
	private static final Map<Direction, VoxelShape> BIG_ARM_SHAPES = arms(3, 13);

	private static Map<Direction, VoxelShape> arms(int a, int b) {
		Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
		out.put(Direction.NORTH, Block.box(a, a, 0, b, b, a));
		out.put(Direction.SOUTH, Block.box(a, a, b, b, b, 16));
		out.put(Direction.WEST, Block.box(0, a, a, a, b, b));
		out.put(Direction.EAST, Block.box(b, a, a, 16, b, b));
		out.put(Direction.DOWN, Block.box(a, 0, a, b, a, b));
		out.put(Direction.UP, Block.box(a, b, a, b, 16, b));
		return out;
	}

	/**
	 * Water pipes link boilers, turbines and pumps; gas lines link wells and fireboxes. The two never join. SLURRY is the 600 mm
	 * large-bore slurry pipe: a run of its own, as steel pipe's, rated about 1 m3/s ({@link Lines#capacityPerTick}). AIR_HEADER is the
	 * 600 mm flotation air header: blower air for another mod's cells, a network of its own ({@link Plumbing#network}).
	 */
	public enum Kind {
		WATER, GAS, AIR, NETWORK, SLURRY, AIR_HEADER
	}

	/** Another mod's block that draws an arm toward a pipe of these kinds (a slurry pump's nozzle, a blower's outlet). */
	public interface Joins {
		boolean joinsPipe(BlockState state, Kind kind);
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
			if (kind == Kind.NETWORK) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Network cable: " + dev.wildcatter.control.NetworkNet.plcs(level, java.util.List.of(pos)).size() + " PLC(s) with a COMM module on it"));
			} else if (kind == Kind.AIR) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT, "Instrument air tubing: %.1f bar", dev.wildcatter.control.AirCompressorBlockEntity.pressureAt(level, pos))));
			} else if (kind == Kind.AIR_HEADER) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT, "Air header (600 mm, about %.1f m3/s of blower air): %d lengths joined",
						dev.wildcatter.Tuning.AIR_HEADER_M3_S, Plumbing.network(level, java.util.List.of(pos), Kind.AIR_HEADER).size() + 1)));
			} else if (kind == Kind.SLURRY) {
				Lines.Line line = Lines.line(level, pos);
				String assay = Slurry.describe(line);
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT, "Slurry pipe (600 mm, rated %.0f m3/s): ", dev.wildcatter.Tuning.SLURRY_PIPE_M3_S)
						+ line.pipes().size() + " lengths joined, " + line.run().describe() + (assay == null ? "" : " (" + assay + ")")));
			} else if (kind == Kind.WATER) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Water pipe (2 in): " + (Plumbing.network(level, java.util.List.of(pos)).size() + 1)
						+ " lengths joined, carrying fresh water from a pump"));
			} else {
				Lines.Line line = Lines.line(level, pos);
				String assay = Slurry.describe(line);
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Steel pipe (4 in): " + line.pipes().size() + " lengths joined, "
						+ line.run().describe() + (assay == null ? "" : " (" + assay + ")")));
			}
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	public Kind kind() {
		return kind;
	}

	/** False at a shut valve, where a run of pipe stops. */
	public static boolean conducts(net.minecraft.world.level.Level level, BlockPos pos) {
		return !(level.getBlockEntity(pos) instanceof dev.wildcatter.control.ValveBlockEntity valve) || valve.position() > 0.001;
	}

	private boolean joins(BlockState neighbour) {
		Block block = neighbour.getBlock();
		if (block instanceof PipeBlock pipe) {
			return pipe.kind == kind;
		}
		if (block instanceof Joins other && other.joinsPipe(neighbour, kind)) {
			return true;
		}
		if (kind == Kind.AIR_HEADER) {
			return false;
		}
		if (kind == Kind.SLURRY && block instanceof DecantTowerBlock) {
			return true;
		}
		if (kind == Kind.NETWORK) {
			return block instanceof dev.wildcatter.control.PlcCabinetBlock || block instanceof dev.wildcatter.control.HmiPanelBlock || block instanceof dev.wildcatter.control.RemoteIoDropBlock;
		}
		if (kind == Kind.AIR) {
			return block instanceof dev.wildcatter.control.AirDryerBlock || block instanceof dev.wildcatter.control.ValveBlock v && v.type() != dev.wildcatter.control.ValveBlock.Type.MOTOR_OPERATED;
		}
		return kind == Kind.WATER
				? block instanceof DayTankBlock || block instanceof BoilerBlock || block instanceof SteamTurbineBlock || block instanceof PumpBlock || block instanceof TowerBasinBlock
						|| block instanceof RigPartBlock rig && rig.part() == RigPartBlock.Part.MUD_TANKS
						|| block instanceof SteamPlantPartBlock plant && plant.part() == SteamPlantPartBlock.Part.CONDENSER
						|| block instanceof PetrochemPartBlock furnace && furnace.part() == PetrochemPartBlock.Part.CRACKING_FURNACE
				: block instanceof GasWellheadBlock gas && neighbour.getValue(gas.piece()) == 0 || block instanceof OilWellheadBlock || block instanceof FireboxBlock
						|| block instanceof LeasePartBlock || block instanceof DisposalWellheadBlock
						|| block instanceof GasPlantPartBlock gasPlant && gasPlant.part() != GasPlantPartBlock.Part.REFRIGERATION_COMPRESSOR
						|| block instanceof RefineryPartBlock refinery && refinery.part() != RefineryPartBlock.Part.CRUDE_CHARGE_PUMP
						|| block instanceof TurbinePartBlock turbine && (turbine.part() == TurbinePartBlock.Part.GAS_TURBINE || turbine.part() == TurbinePartBlock.Part.AERO_TURBINE)
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
		boolean big = kind == Kind.SLURRY || kind == Kind.AIR_HEADER;
		VoxelShape shape = big ? BIG_CORE : CORE;
		for (Direction side : Direction.values()) {
			if (state.getValue(WireShape.ARMS.get(side))) {
				shape = Shapes.or(shape, (big ? BIG_ARM_SHAPES : ARM_SHAPES).get(side));
			}
		}
		return shape;
	}
}
