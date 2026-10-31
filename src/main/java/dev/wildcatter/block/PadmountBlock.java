package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A three-phase pad-mount transformer, or a plant's unit substation, at true size (docs/multiblocks/padmount.md). Facing is its
 * front (the doors), toward whoever places it; the main cell is the bottom front cell on the right as you face the doors.
 * <ul>
 * <li>Pad-mount, 2 x 2 x 2: the cables come up from below into the two front compartments. The low side is the main cell's
 * outer side and underside, the high side the other front cell's outer side and underside.</li>
 * <li>Unit substation, 4 x 3 x 2: the lineup runs from its medium-voltage switch section at the main cell's end to its 480 V
 * breaker section at the far end; each terminal is that end cell's end face and underside.</li>
 * </ul>
 */
public class PadmountBlock extends BigTransformerBlock {
	public static final Footprint PADMOUNT = Footprint.box(2, 2, 2, 0, 0);
	public static final Footprint UNIT_SUBSTATION = Footprint.box(4, 3, 2, 0, 0);

	/** The far end cell along the front, across from the main cell: 1 for a pad-mount, 3 for a unit substation. */
	private final int across;
	/** Which end carries the high side: the far cell for a pad-mount (on the left as you face it), the main cell for a unit substation. */
	private final boolean highAtMain;

	private PadmountBlock(Properties properties, Footprint footprint, String shapeName, Voltage low, Voltage high, double ratedVa, double percentZ, int across, boolean highAtMain) {
		super(properties, footprint, shapeName, low, high, ratedVa, percentZ, false);
		this.across = across;
		this.highAtMain = highAtMain;
	}

	public static PadmountBlock padmount(Properties properties, String shapeName, Voltage low, Voltage high, double ratedVa, double percentZ) {
		return new PadmountBlock(properties, PADMOUNT, shapeName, low, high, ratedVa, percentZ, 1, false);
	}

	public static PadmountBlock unitSubstation(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		return new PadmountBlock(properties, UNIT_SUBSTATION, "unit_substation", low, high, ratedVa, percentZ, 3, true);
	}

	@Override
	public BlockState placementState(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return BigParts.placement(context, placementState(context));
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		BlockPos far = BigParts.cell(main, state, footprint().pieceAt(across, 0, 0, 0));
		Direction mainSide = front.getCounterClockWise();
		Direction farSide = front.getClockWise();
		return List.of(
				new Terminal(main, mainSide, highAtMain),
				new Terminal(main, Direction.DOWN, highAtMain),
				new Terminal(far, farSide, !highAtMain),
				new Terminal(far, Direction.DOWN, !highAtMain));
	}
}
