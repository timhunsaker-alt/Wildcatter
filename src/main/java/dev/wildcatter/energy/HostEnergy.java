package dev.wildcatter.energy;

/**
 * Something outside the grid that takes power as FE or Team Reborn E (another mod's machine or cable). Only plug adapters use it,
 * and only through {@link EnergyBridge}: the grid itself works in volts, amps and watts.
 */
public interface HostEnergy {
	/** The most it would take in one tick right now. */
	long demand();

	/** Gives it energy; returns how much it took. */
	long insert(long amount);

	/** The host's unit, for the plug's readout. */
	default String unit() {
		return "FE";
	}

	/** Tick {@code tick} of {@code ticks}' even share of {@code total}: the remainder goes to the first ticks, so they sum to it. */
	static long perTick(long total, int ticks, int tick) {
		return total / ticks + (tick < total % ticks ? 1 : 0);
	}
}
