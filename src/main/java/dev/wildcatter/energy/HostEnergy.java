package dev.wildcatter.energy;

/**
 * Something outside the grid that takes power as FE (another mod's machine or cable). Only plug adapters use it, and only through
 * {@link EnergyBridge}: the grid itself works in volts, amps and watts.
 */
public interface HostEnergy {
	/** The most FE it would take in one tick right now. */
	long demand();

	/** Gives it FE; returns how much it took. */
	long insert(long fe);
}
