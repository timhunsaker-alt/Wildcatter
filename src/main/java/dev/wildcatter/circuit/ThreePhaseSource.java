package dev.wildcatter.circuit;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The electrical side of a generator: three phases A, B and C at 0, -120 and +120 degrees (ABC rotation), each behind the
 * machine's internal resistance, and a neutral bonded to ground. Every face has all four terminals; a wire takes the one
 * conductor it carries, and a single-phase load touching the machine takes phase A.
 */
public final class ThreePhaseSource {
	public static final double[] DEGREES = {0, -120, 120};
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final int[] hot = new int[3];
	private final int[] source = new int[3];
	private int neutral;

	/** Adds the machine to the circuit: {@code volts} line to neutral on each phase, behind {@code ohms}. */
	public void build(Circuit circuit, BlockPos pos, Voltage voltage, double volts, double ohms) {
		neutral = circuit.node();
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
			int internal = circuit.node();
			source[k] = circuit.source(internal, neutral, volts, DEGREES[k]);
			circuit.resistor(internal, hot[k], ohms);
		}
		circuit.ground(neutral);
		for (Direction face : Direction.values()) {
			for (int k = 0; k < 3; k++) {
				circuit.port(pos, face, PHASES[k], voltage, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
			circuit.port(pos, face, Conductor.N, voltage, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	/** Real power out of the terminals on all three phases (what the machine delivers, not counting its own losses). */
	public double watts(Circuit circuit) {
		double sum = 0;
		for (int k = 0; k < 3; k++) {
			sum += circuit.acrossPhasor(hot[k], neutral).times(circuit.sourceCurrentPhasor(source[k]).conjugate()).re();
		}
		return sum;
	}

	/** Phase A to neutral at the terminals. */
	public double volts(Circuit circuit) {
		return circuit.across(hot[0], neutral);
	}

	/** Each phase's current, A, B, C. */
	public double[] phaseAmps(Circuit circuit) {
		double[] out = new double[3];
		for (int k = 0; k < 3; k++) {
			out[k] = circuit.sourceCurrent(source[k]);
		}
		return out;
	}

	/** The largest of the three phase currents. */
	public double amps(Circuit circuit) {
		double most = 0;
		for (int k = 0; k < 3; k++) {
			most = Math.max(most, circuit.sourceCurrent(source[k]));
		}
		return most;
	}
}
