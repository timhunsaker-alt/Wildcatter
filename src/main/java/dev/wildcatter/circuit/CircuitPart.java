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

	/** Adds this part's nodes, components and terminals to the circuit. */
	void build(Circuit circuit);

	/**
	 * Protective devices act here, before anything else gets to react to the numbers.
	 * Return true if you changed the circuit (a breaker tripping), and it will be solved again.
	 */
	default boolean protect(Circuit circuit, int ticks) {
		return false;
	}

	/** Reads the results. {@code ticks} is how long it has been since the last solve. */
	void apply(Circuit circuit, int ticks);
}
