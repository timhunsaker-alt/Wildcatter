package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Complex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A house of variable-frequency drives on a three-phase supply (docs/multiblocks/land-rig.md): a balanced load in a floating
 * star at the drives' front-end power factor, drawing what its machine asks for. Its power management limits the draw so the
 * island's frequency stays at or above {@link Tuning#LAND_RIG_FREQUENCY_FLOOR} of the standard's: it picks load up gently
 * (10 % a second) and sheds it at once when the frequency sags. Below {@link Tuning#LAND_RIG_UNDERVOLTAGE} of its rated voltage
 * the drives stop, and they start again when the voltage is back.
 */
public final class DriveLoad {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	/** Share of the demand picked up each second while the frequency is healthy. */
	private static final double RAMP_PER_SECOND = 0.1;

	private final Voltage voltage;
	private double demand;
	/** The share of the demand the power management lets the drives take. */
	private double limit;
	private boolean undervoltage;

	private final int[] hot = new int[3];
	private int star;
	private final int[] branch = {-1, -1, -1};

	private double volts;
	private double amps;
	private double watts;
	private double hertz;

	public DriveLoad(Voltage voltage) {
		this.voltage = voltage;
	}

	public Voltage voltage() {
		return voltage;
	}

	/** What the machine wants, in watts. */
	public void setDemand(double wattsWanted) {
		demand = Math.max(0, wattsWanted);
	}

	public double demand() {
		return demand;
	}

	/** The share of its demand it is getting, 0 to 1. */
	public double delivered() {
		return demand <= 0 ? 0 : Math.min(1.0, watts / demand);
	}

	public void build(Circuit circuit, BlockPos pos, java.util.List<Direction> faces) {
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
		}
		java.util.Arrays.fill(branch, -1);
		double take = demand * limit;
		if (take > 1 && !undervoltage) {
			// each phase of the star draws a third of it at the line-to-neutral voltage
			double z = voltage.lineToLine() * voltage.lineToLine() / (take / Tuning.LAND_RIG_POWER_FACTOR);
			double r = z * Tuning.LAND_RIG_POWER_FACTOR;
			double x = z * Math.sqrt(1 - Tuning.LAND_RIG_POWER_FACTOR * Tuning.LAND_RIG_POWER_FACTOR);
			star = circuit.node();
			for (int k = 0; k < 3; k++) {
				branch[k] = circuit.impedance(hot[k], star, r, x);
			}
		}
		for (Direction face : faces) {
			for (int k = 0; k < 3; k++) {
				circuit.port(pos, face, PHASES[k], voltage, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	public void apply(Circuit circuit, int ticks) {
		Complex ab = circuit.acrossPhasor(hot[0], hot[1]);
		Complex bc = circuit.acrossPhasor(hot[1], hot[2]);
		Complex ca = circuit.acrossPhasor(hot[2], hot[0]);
		volts = (ab.abs() + bc.abs() + ca.abs()) / 3.0;
		hertz = circuit.hertz();
		watts = 0;
		amps = 0;
		for (int k = 0; k < 3; k++) {
			if (branch[k] >= 0) {
				Complex i = circuit.currentPhasor(branch[k]);
				watts += circuit.acrossPhasor(hot[k], star).times(i.conjugate()).re();
				amps = Math.max(amps, i.abs());
			}
		}
		double seconds = ticks / 20.0;
		boolean healthy = volts >= voltage.lineToLine() * Tuning.LAND_RIG_UNDERVOLTAGE;
		if (!healthy) {
			undervoltage = true;
			limit = 0;
			return;
		}
		undervoltage = false;
		double floor = ElectricalStandard.active().hertz() * Tuning.LAND_RIG_FREQUENCY_FLOOR;
		if (hertz < floor) {
			limit = Math.max(0, limit * 0.85 - 0.02);   // shed at once
		} else if (hertz >= floor + ElectricalStandard.active().hertz() * 0.003) {
			limit = Math.min(1.0, limit + RAMP_PER_SECOND * seconds);
		}
	}

	public double volts() {
		return volts;
	}

	public double amps() {
		return amps;
	}

	public double watts() {
		return watts;
	}

	public boolean undervoltage() {
		return undervoltage;
	}

	/** True while the power management is holding it below what it wants. */
	public boolean limited() {
		return demand > 0 && limit < 0.999;
	}

	public String describe() {
		if (undervoltage) {
			return String.format("UNDERVOLTAGE: %.0f V, the drives are stopped", volts);
		}
		return String.format("%.0f V, %.0f A, %.0f kW of %.0f kW wanted, %.2f Hz%s", volts, amps, watts / 1000, demand / 1000, hertz,
				limited() ? ", POWER LIMITED" : "");
	}
}
