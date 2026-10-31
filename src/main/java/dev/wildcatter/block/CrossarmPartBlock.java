package dev.wildcatter.block;

import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * An 8 ft distribution crossarm (docs/multiblocks/poles.md): a fir arm 2.44 m long on two braces, bolted across a pole 0.3 m
 * below its top. It fills the cells either side of the pole, square to the line (facing north it runs west to east), and a
 * pin insulator stands on each. Clicked within 2 m of a true-size pole's top it goes to the standard place, beside the top cell;
 * lower, where it was clicked, for a second arm.
 */
public class CrossarmPartBlock extends PoleHardwareBlock {
	/** The main cell west of the pole, the other east of it: the pole's cell is not the arm's. */
	public static final Footprint FOOTPRINT = Footprint.of(new Footprint.Cell(0, 0, 0), new Footprint.Cell(2, 0, 0));

	public CrossarmPartBlock(Properties properties) {
		super(properties, FOOTPRINT, "crossarm_dist");
	}

	@Override
	protected List<BlockPos> poles(BlockPos main, Direction facing) {
		return List.of(main.relative(facing.getClockWise()));
	}

	@Override
	protected BlockPos mainFor(BlockGetter level, BlockPos pole, Direction facing) {
		BlockPos top = WoodPoleBlock.top(pole, level.getBlockState(pole));
		BlockPos at = top.getY() - pole.getY() <= 1 ? top : pole;
		return at.relative(facing.getCounterClockWise());
	}

	@Override
	protected String needs() {
		return "A crossarm bolts across a pole: click the pole's side near its top";
	}
}
