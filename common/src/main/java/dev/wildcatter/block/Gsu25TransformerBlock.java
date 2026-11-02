package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 25 MVA generator step-up at true size (docs/multiblocks/gas-turbine.md): 13.8 kV delta to 138 kV grounded wye, 8 %Z, ONAN/ONAF,
 * 5 m across, 4 m deep and 5 m to its bushing tops. Facing is the 138 kV side, away from whoever places it.
 * <ul>
 * <li>The 13.8 kV bus duct comes in low on the back: the back face of the bottom cell in the middle of the back row.</li>
 * <li>The three 138 kV bushings stand on the cover: their terminals are the top and front faces of the top cell in the middle of the
 * front row.</li>
 * </ul>
 */
public class Gsu25TransformerBlock extends BigTransformerBlock {
	/** 5 across, 5 up, 4 deep, the main cell at the back of the bottom middle. */
	public static final Footprint FOOTPRINT = Footprint.box(5, 5, 4, 2, 3);

	public Gsu25TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		super(properties, FOOTPRINT, "gsu_25", low, high, ratedVa, percentZ, true);
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		BlockPos bushings = BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 4, -3, 0));   // the top of the front row's middle
		return List.of(
				new Terminal(main, front.getOpposite(), false),
				new Terminal(bushings, Direction.UP, true),
				new Terminal(bushings, front, true));
	}
}
