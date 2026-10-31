package dev.wildcatter.block;

import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * The crossbeam of a 138 kV H-frame (docs/multiblocks/poles.md): two timbers about 9 m long through-bolted across the tops of two
 * 75 ft poles 4 m apart, 0.6 m below the pole tops. The three suspension strings hang under it, 4 m apart: the middle one under
 * its main cell between the poles, the outer two at its ends. Facing north it runs west to east.
 */
public class CrossbeamBlock extends PoleHardwareBlock {
	/** Nine cells across, less the two the poles stand in (2 m either side of the middle). */
	public static final Footprint FOOTPRINT = Footprint.of(new Footprint.Cell(0, 0, 0), new Footprint.Cell(-4, 0, 0), new Footprint.Cell(-3, 0, 0),
			new Footprint.Cell(-1, 0, 0), new Footprint.Cell(1, 0, 0), new Footprint.Cell(3, 0, 0), new Footprint.Cell(4, 0, 0));
	/** How far the poles of an H-frame stand apart. */
	public static final int POLE_SPACING = 4;

	public CrossbeamBlock(Properties properties) {
		super(properties, FOOTPRINT, "crossbeam_138");
	}

	@Override
	protected List<BlockPos> poles(BlockPos main, Direction facing) {
		return List.of(main.relative(facing.getClockWise(), POLE_SPACING / 2), main.relative(facing.getCounterClockWise(), POLE_SPACING / 2));
	}

	/** The middle of an H-frame for a click on one of its poles: toward the other pole, 4 m off; at the clicked pole's top. */
	static BlockPos middle(BlockGetter level, BlockPos pole, Direction facing) {
		BlockPos at = WoodPoleBlock.top(pole, level.getBlockState(pole));
		Direction across = facing.getClockWise();
		if (!WoodPoleBlock.isPole(level.getBlockState(at.relative(across, POLE_SPACING)))
				&& WoodPoleBlock.isPole(level.getBlockState(at.relative(across.getOpposite(), POLE_SPACING)))) {
			across = across.getOpposite();
		}
		return at.relative(across, POLE_SPACING / 2);
	}

	@Override
	protected BlockPos mainFor(BlockGetter level, BlockPos pole, Direction facing) {
		return middle(level, pole, facing);
	}

	@Override
	protected String needs() {
		return "A crossbeam goes across the tops of two poles 4 m apart: click one of them";
	}
}
