package dev.wildcatter.energy;

import net.minecraft.core.Direction;

/**
 * Anything that can be part of the grid: a generator, a cable, a machine. Block entities implement this.
 * A node joins a neighbour on a side only if both report the same voltage there.
 */
public interface EnergyNode {
	/** The main buffer: what the Power Meter reads, and what is used for every side that has no buffer of its own. */
	EnergyBuffer energy();

	/** The buffer the neighbour on {@code side} fills or drains. Most nodes have just one. */
	default EnergyBuffer energyAt(Direction side) {
		return energy();
	}

	/** The voltage on {@code side}, or null if nothing joins there. */
	default Voltage voltageAt(Direction side) {
		return Voltage.LOW;
	}

	/** True if this node joins to a neighbour of {@code voltage} on {@code side} (cables use it to draw their arms). */
	default boolean connectsTo(Direction side, Voltage voltage) {
		return voltageAt(side) == voltage;
	}
}
