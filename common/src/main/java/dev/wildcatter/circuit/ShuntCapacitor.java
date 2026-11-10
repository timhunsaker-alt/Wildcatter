package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Ferroalloys;
import dev.wildcatter.solver.Complex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A shunt capacitor bank in an ungrounded wye (docs/multiblocks/ferroalloys.md): three legs of capacitance from the phases to a floating
 * star, sized to give its rated vars at its tier's nominal volts and the standard's frequency. A capacitor's reactance falls as the
 * frequency rises, and the vars it gives go as the square of the volts. Its breaker takes the legs out; the terminals stay.
 */
public final class ShuntCapacitor {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final Voltage voltage;
	private final double ratedVar;
	private final int[] hot = new int[3];
	private int star;
	private final int[] branch = {-1, -1, -1};
	private double volts;
	private double amps;
	private double watts;
	private double vars;

	public ShuntCapacitor(Voltage voltage, double ratedVar) {
		this.voltage = voltage;
		this.ratedVar = ratedVar;
	}

	public Voltage voltage() {
		return voltage;
	}

	public double ratedVar() {
		return ratedVar;
	}

	public void build(Circuit circuit, BlockPos pos, java.util.List<Direction> faces, boolean closed) {
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
		}
		java.util.Arrays.fill(branch, -1);
		if (closed) {
			double x = Ferroalloys.capacitorOhms(voltage.lineToLine(), ratedVar) * ElectricalStandard.active().hertz() / Math.max(1, circuit.hertz());
			star = circuit.node();
			for (int k = 0; k < 3; k++) {
				branch[k] = circuit.impedance(hot[k], star, x * Tuning.CAP_BANK_LOSS, -x);
			}
		}
		for (Direction face : faces) {
			for (int k = 0; k < 3; k++) {
				circuit.port(pos, face, PHASES[k], voltage, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	public void apply(Circuit circuit) {
		Complex ab = circuit.acrossPhasor(hot[0], hot[1]);
		Complex bc = circuit.acrossPhasor(hot[1], hot[2]);
		Complex ca = circuit.acrossPhasor(hot[2], hot[0]);
		volts = (ab.abs() + bc.abs() + ca.abs()) / 3.0;
		watts = 0;
		vars = 0;
		amps = 0;
		for (int k = 0; k < 3; k++) {
			if (branch[k] >= 0) {
				Complex i = circuit.currentPhasor(branch[k]);
				Complex s = circuit.acrossPhasor(hot[k], star).times(i.conjugate());
				watts += s.re();
				vars -= s.im();   // a capacitor's S is negative imaginary: it gives vars
				amps = Math.max(amps, i.abs());
			}
		}
	}

	public double volts() {
		return volts;
	}

	public double amps() {
		return amps;
	}

	/** Its losses, W. */
	public double watts() {
		return watts;
	}

	/** Vars it gives the bus (positive). */
	public double vars() {
		return vars;
	}
}
