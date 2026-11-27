package dev.wildcatter.block;

import dev.wildcatter.energy.ConductorSize;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The wire look shared by wires, cables and ground wire: a core with an arm toward each joined neighbour. Sized conductors are drawn
 * thicker for bigger sizes in four steps ({@link #GAUGE}, set from the conductor size when it is placed).
 */
public final class WireShape {
	public static final Map<Direction, BooleanProperty> ARMS = new EnumMap<>(Direction.class);
	/** How thick a sized wire or cable is drawn. */
	public static final EnumProperty<ConductorSize.Gauge> GAUGE = EnumProperty.create("gauge", ConductorSize.Gauge.class);
	/** A wire's width in pixels at each gauge: 12 and 10 AWG keep the old 2-pixel wire. */
	private static final double[] WIRE_WIDTH = {2, 3, 4, 5};
	private static final VoxelShape[][] WIRE = new VoxelShape[4][64];

	static {
		for (Direction side : Direction.values()) {
			ARMS.put(side, BooleanProperty.create(side.getName()));
		}
		for (int g = 0; g < 4; g++) {
			for (int mask = 0; mask < 64; mask++) {
				WIRE[g][mask] = build(WIRE_WIDTH[g], mask);
			}
		}
	}

	private WireShape() {
	}

	public static BlockState noArms(BlockState state) {
		for (BooleanProperty arm : ARMS.values()) {
			state = state.setValue(arm, false);
		}
		return state;
	}

	/** Which sides have an arm, one bit per {@link Direction#ordinal()}. */
	public static int mask(BlockState state) {
		int mask = 0;
		for (Direction side : Direction.values()) {
			BooleanProperty arm = ARMS.get(side);
			if (state.hasProperty(arm) && state.getValue(arm)) {
				mask |= 1 << side.ordinal();
			}
		}
		return mask;
	}

	public static ConductorSize.Gauge gauge(BlockState state) {
		return state.hasProperty(GAUGE) ? state.getValue(GAUGE) : ConductorSize.Gauge.SMALL;
	}

	/** A wire's shape at its gauge. */
	public static VoxelShape of(BlockState state) {
		return WIRE[gauge(state).ordinal()][mask(state)];
	}

	/**
	 * The sides where this wire or cable meets a machine's terminal rather than more wire: each arm whose neighbour is not a wire,
	 * cable or insulator. A terminal lug (wire) or cable gland (cable) is drawn there (client/TerminalRenderer). One bit per side.
	 */
	public static int terminals(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, BlockState state) {
		int arms = mask(state);
		int out = 0;
		for (Direction side : Direction.values()) {
			if ((arms & 1 << side.ordinal()) != 0 && !conductorBlock(level.getBlockState(pos.relative(side)).getBlock())) {
				out |= 1 << side.ordinal();
			}
		}
		return out;
	}

	/** Wire, cable, busway, ground wire and insulators: a run carries on through them, with no terminal at the joint. */
	public static boolean conductorBlock(Block block) {
		return block instanceof CableBlock || block instanceof MultiCableBlock || block instanceof GroundWireBlock || block instanceof InsulatorBlock;
	}

	/** A core {@code width} pixels across with an arm of that width toward each side in {@code mask}. */
	public static VoxelShape build(double width, int mask) {
		double a = 8 - width / 2, b = 8 + width / 2;
		VoxelShape shape = Block.box(a, a, a, b, b, b);
		for (Direction side : Direction.values()) {
			if ((mask & 1 << side.ordinal()) != 0) {
				shape = Shapes.or(shape, switch (side) {
					case NORTH -> Block.box(a, a, 0, b, b, a);
					case SOUTH -> Block.box(a, a, b, b, b, 16);
					case WEST -> Block.box(0, a, a, a, b, b);
					case EAST -> Block.box(b, a, a, 16, b, b);
					case DOWN -> Block.box(a, 0, a, b, a, b);
					case UP -> Block.box(a, b, a, b, 16, b);
				});
			}
		}
		return shape;
	}
}
