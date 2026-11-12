package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.multiblock.BigPart;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 16b2 (docs/multiblocks/conversion-extensions.md, "A full-size plant"): the main line's second, larger size. A part whose id ends in
 * {@code _large} is rated four times the 7d part's 0.075 kg of U a second; everything it takes and makes goes with its uranium.
 */
public final class ConversionSize {
	private ConversionSize() {
	}

	/** The part's id without its size: {@code fluorinator_large} is a fluorinator. */
	public static String base(BlockEntity part) {
		String id = part.getBlockState().getBlock() instanceof BigPart big ? big.shapeName() : "";
		return id.endsWith("_large") ? id.substring(0, id.length() - "_large".length()) : id;
	}

	public static boolean large(BlockEntity part) {
		return part.getBlockState().getBlock() instanceof BigPart big && big.shapeName().endsWith("_large");
	}

	/** Its rating against the 7d part's: 4 for the large size, else 1. */
	public static double scale(BlockEntity part) {
		return large(part) ? Tuning.CONVERSION_LARGE_SCALE : 1;
	}
}
