package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wood utility pole at true size (docs/multiblocks/poles.md): one item that stands its real height above ground, 8 m for a
 * 30 ft pole, 10 m for a 40 ft and 20 m for a 75 ft. Its top cell carries a pole-top pin; a crossarm or an H-frame crossbeam is
 * clicked onto it. Climbable, like the one-block sections it replaces.
 */
public class WoodPoleBlock extends BigPartBlock {
	private final int height;

	public WoodPoleBlock(Properties properties, int height, String shapeName) {
		super(properties, Footprint.box(1, height, 1, 0, 0), shapeName);
		this.height = height;
	}

	public int height() {
		return height;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BigPartBlockEntity(pos, state);
	}

	/** A new pole, unlike the parts of a machine whose layout changed: one set by a command grows into its cells. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	/** Any pole cell: a true-size pole's, or an older world's one-block section. */
	public static boolean isPole(BlockState state) {
		return state.is(WildcatterBlocks.POLE) || state.getBlock() instanceof WoodPoleBlock;
	}

	/** The top cell of a true-size pole: a pole-top pin goes on top of it, and a crossarm beside it. */
	public static boolean isTop(BlockState state) {
		return state.getBlock() instanceof WoodPoleBlock pole && state.getValue(pole.piece()) == pole.height - 1;
	}

	/** The top cell of the pole that has a cell at {@code pos} (an older one-block section is its own top). */
	public static BlockPos top(BlockPos pos, BlockState state) {
		return state.getBlock() instanceof WoodPoleBlock pole ? BigParts.main(pos, state).above(pole.height - 1) : pos;
	}
}
