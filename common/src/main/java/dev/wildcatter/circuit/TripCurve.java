package dev.wildcatter.circuit;

/**
 * A protective device's time-current characteristic, for the coordination check: how long it takes to clear a given current. A
 * breaker's thermal-magnetic trip, a fuse's melting and clearing curve and a relay's 50/51 each give one. Plain Java.
 */
public interface TripCurve {
	/** A short name for the readout, e.g. "feeder 2 relay (MI, TD 1, 100 A)" or "class J 60 A fuse". */
	String curveName();

	/** Seconds to clear {@code amps} (the largest phase current through the device); infinite if it never trips at that current. */
	double clearingSeconds(double amps);
}
