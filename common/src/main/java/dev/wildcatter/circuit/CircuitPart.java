package dev.wildcatter.circuit;

import dev.wildcatter.energy.Voltage;
import net.minecraft.core.Direction;

/**
 * A block entity that is part of an electrical circuit: a wire, a generator, a load, a breaker, a transformer.
 *
 * Every few ticks the {@link CircuitManager} asks each part in a connected group to {@link #build} itself into a
 * fresh {@link Circuit} (its nodes, resistors and terminals), solves the whole group at once, and then tells each
 * part what happened with {@link #apply}.
 */
public interface CircuitPart {
	/** True if there is a terminal of this role and voltage on {@code face}. Wires use it to know where to grow arms. */
	boolean hasPort(Direction face, Conductor role, Voltage voltage);

	/** For a big part (docs/multiblocks/README.md): a terminal on {@code face} of one of its cells. Others have only their own cell. */
	default boolean hasPortAt(net.minecraft.core.BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return hasPort(face, role, voltage);
	}

	/** True if whatever stands at {@code pos} (an ordinary part, or any piece of a big part) has that terminal on {@code face}. */
	static boolean portAt(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, Direction face, Conductor role, Voltage voltage) {
		return dev.wildcatter.multiblock.BigParts.entity(level, pos) instanceof CircuitPart part && part.hasPortAt(pos, face, role, voltage);
	}

	/** Adds this part's nodes, components and terminals to the circuit. */
	void build(Circuit circuit);

	/**
	 * Protective devices act here, before anything else gets to react to the numbers.
	 * Return true if you changed the circuit (a breaker tripping), and it will be solved again.
	 */
	default boolean protect(Circuit circuit, int ticks) {
		return false;
	}

	/**
	 * The protection race (protection gap wave): how soon this part's {@link #protect} would open on this solve, in seconds, or
	 * infinity if it would not. Only the fastest parts act on a solve, and the circuit is solved again without the fault, so a
	 * current-limiting fuse clears a fault before a breaker's magnetic trip sees it. NaN (the default): untimed, it always acts, as
	 * before.
	 */
	default double clearingSeconds(Circuit circuit) {
		return Double.NaN;
	}

	/** The peak current a current-limiting fuse lets through on this solve, when it wins the race in its limiting range; 0 if none. */
	default double letThroughPeak(Circuit circuit) {
		return 0;
	}

	/** The rms current at which this part trips at once (its magnetic pickup); a let-through peak over √2 times it still trips it. */
	default double instantPickupAmps() {
		return Double.POSITIVE_INFINITY;
	}

	/** Other parts this one is linked to without touching them (an overhead span): they are solved together as one circuit. */
	default java.util.Collection<net.minecraft.core.BlockPos> links() {
		return java.util.List.of();
	}

	/** Reads the results. {@code ticks} is how long it has been since the last solve. */
	void apply(Circuit circuit, int ticks);
}
