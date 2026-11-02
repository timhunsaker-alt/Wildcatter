package dev.wildcatter.block;

import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * The X-brace of a 138 kV H-frame (docs/multiblocks/poles.md): two crossed timbers between the poles, 3 m wide and 3 m high, set
 * below the middle string. Clicked on one of the poles it centres between them, its top 2 m under the crossbeam (the string
 * hangs in the cell between).
 */
public class XBraceBlock extends PoleHardwareBlock {
	/** Three across and three up, the main cell in the middle of the bottom row. */
	public static final Footprint FOOTPRINT = Footprint.box(3, 3, 1, 1, 0);

	public XBraceBlock(Properties properties) {
		super(properties, FOOTPRINT, "x_brace_138");
	}

	@Override
	protected List<BlockPos> poles(BlockPos main, Direction facing) {
		int half = CrossbeamBlock.POLE_SPACING / 2;
		return List.of(main.relative(facing.getClockWise(), half), main.relative(facing.getCounterClockWise(), half),
				main.above(2).relative(facing.getClockWise(), half), main.above(2).relative(facing.getCounterClockWise(), half));
	}

	@Override
	protected BlockPos mainFor(BlockGetter level, BlockPos pole, Direction facing) {
		return CrossbeamBlock.middle(level, pole, facing).below(4);   // the beam's cell, the string's, then the brace's three
	}

	@Override
	protected String needs() {
		return "An X-brace goes between the two poles of an H-frame: click one of them";
	}
}
