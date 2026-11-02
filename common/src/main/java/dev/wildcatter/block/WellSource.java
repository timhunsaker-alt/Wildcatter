package dev.wildcatter.block;

import dev.wildcatter.fluid.Stream;

/**
 * A well on a run of steel pipe (docs/multiblocks/lease.md): what a separator or a firebox draws from it comes as a split stream,
 * so the oil, water and gas in it stay apart. A pumpjack's amount is litres of liquid; a gas well's is m3 of gas.
 */
public interface WellSource extends FluidNode {
	/** Draws up to {@code amount} of its stream and returns what was in it. */
	Stream.Split drain(double amount);
}
