package dev.wildcatter.block;

import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * A machine that puts something on steel pipe (docs/multiblocks/lease.md): a well, a separator nozzle, a tank, a refinery's diesel.
 * A port is one face of one of its cells; {@code side} points from that cell to the pipe. Machines that only take (engines, fireboxes)
 * need not be nodes: they pull through {@link Lines}.
 */
public interface FluidNode {
	/** Who is pulling: a tank takes only from process outlets, never from another tank; a flare takes only what is in excess. */
	enum Pull {
		MACHINE, TANK, FLARE
	}

	/** What this port puts on its pipe, or null for nothing (an inlet, or an empty tank). */
	@Nullable Product offers(BlockPos cell, Direction side);

	/** How much of {@code product} this port could give to {@code pull} now. */
	double available(BlockPos cell, Direction side, Product product, Pull pull);

	/** Gives up to {@code max} of {@code product} out of this port; returns how much. */
	double take(BlockPos cell, Direction side, Product product, double max, Pull pull);

	/** Tanks are drawn after process outlets, and do not fill from each other. */
	default boolean tank() {
		return false;
	}
}
