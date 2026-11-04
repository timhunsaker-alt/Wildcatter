package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 10 MVA power transformer at true size (docs/multiblocks/power-transformer-10.md): 4.16 kV delta to 138 kV grounded wye, 7.5 %Z,
 * ONAN, 5 m across, 4 m deep and 5 m to its bushing tops. It steps a plant's 4.16 kV up to a line, or a yard's 138 kV down to a
 * plant. Facing is the 138 kV side, away from whoever places it; the footprint and terminals are the 25 MVA GSU's.
 * <ul>
 * <li>The 4.16 kV cable comes in low on the back: the back face of the bottom cell in the middle of the back row.</li>
 * <li>The three 138 kV bushings stand on the cover: their terminals are the top and front faces of the top cell in the middle of the
 * front row.</li>
 * </ul>
 */
public class PowerTransformer10Block extends BigTransformerBlock {
	public static final Footprint FOOTPRINT = Gsu25TransformerBlock.FOOTPRINT;

	public PowerTransformer10Block(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		super(properties, FOOTPRINT, "substation_transformer_10", low, high, ratedVa, percentZ, true);
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
