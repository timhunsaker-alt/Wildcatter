package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The generator step-up at true size (docs/multiblocks/gsu.md): one oil-filled tank 3 m across, 2 m deep and 3 m to the bushing
 * tops, with its radiators on the two sides. Facing is the 12.47 kV side, away from whoever places it.
 * <ul>
 * <li>The 4.16 kV delta comes in through the cable box low on the back, at the main cell's back face.</li>
 * <li>The three 12.47 kV bushings stand on the cover over the front half: their terminals are the top and front faces of the
 * top cell in the middle of the front row.</li>
 * </ul>
 */
public class GsuTransformerBlock extends BigTransformerBlock {
	/** 3 across, 3 up, 2 deep, the main cell at the back of the bottom middle. */
	public static final Footprint FOOTPRINT = Footprint.box(3, 3, 2, 1, 1);

	public GsuTransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		super(properties, FOOTPRINT, "gsu_transformer", low, high, ratedVa, percentZ, true);
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		BlockPos bushings = BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 2, -1, 0));   // the top of the front row's middle
		return List.of(
				new Terminal(main, front.getOpposite(), false),
				new Terminal(bushings, Direction.UP, true),
				new Terminal(bushings, front, true));
	}
}
