package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A 60 MVA transformer at true size, 6 m across, 5 m deep and 6 m to its bushing tops (docs/multiblocks/aeroderivative.md): the
 * peaker's generator step-up (13.8 kV delta / 138 kV grounded wye) or a substation transformer (138 kV delta / 12.47 kV grounded
 * wye). Facing is its 138 kV side, away from whoever places it: the three 138 kV bushings stand on the cover over the front, and
 * the other winding comes in low on the back of the main cell.
 */
public class Gsu60TransformerBlock extends BigTransformerBlock {
	/** 6 across, 6 up, 5 deep, the main cell at the back of the bottom, just east of the middle. */
	public static final Footprint FOOTPRINT = Footprint.box(6, 6, 5, 3, 4);

	private final boolean stepDown;

	/** {@code stepDown}: a substation transformer, its 138 kV the delta and its other winding the low side; otherwise a GSU. */
	public Gsu60TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ, boolean stepDown, String shapeName) {
		super(properties, FOOTPRINT, shapeName, low, high, ratedVa, percentZ, !stepDown, stepDown);
		this.stepDown = stepDown;
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		BlockPos bushings = BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 5, -4, 0));   // the top of the front row, over the main cell
		return List.of(
				new Terminal(main, front.getOpposite(), false),
				new Terminal(bushings, Direction.UP, true),
				new Terminal(bushings, front, true));
	}
}
