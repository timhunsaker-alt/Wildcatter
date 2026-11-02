package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The unit auxiliary transformer at true size, 4 m each way (docs/multiblocks/turbine-island.md): 13.8 kV delta to 4.16 kV wye
 * (11/3.3 kV IEC), 7.5 MVA, the plant's own station service off its generator bus. Facing is its 13.8 kV side: the three bushings
 * stand on the cover over the front row, and the 4.16 kV comes out low on the back of the main cell.
 */
public class UatTransformerBlock extends BigTransformerBlock {
	/** 4 across, 4 up, 4 deep, the main cell at the back of the bottom, just west of the middle. */
	public static final Footprint FOOTPRINT = Footprint.box(4, 4, 4, 1, 3);

	public UatTransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		super(properties, FOOTPRINT, "unit_auxiliary_transformer", low, high, ratedVa, percentZ, false, true);
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		BlockPos bushings = BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 3, -3, 0));   // the top of the front row, over the main cell
		return List.of(
				new Terminal(main, front.getOpposite(), false),
				new Terminal(bushings, Direction.UP, true),
				new Terminal(bushings, front, true));
	}
}
