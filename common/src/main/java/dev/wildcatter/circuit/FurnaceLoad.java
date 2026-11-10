package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Complex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A submerged arc furnace seen from its transformer's primary (docs/multiblocks/ferroalloys.md): a balanced three-phase load in a
 * floating star at the furnace's power factor, 0.80 lagging. Its electrode regulators move the electrodes to hold the power the tap and
 * its set point ask for, so it takes constant power from the line: its impedance is sized from the volts it measured last, as a drive's
 * is (DriveLoad), and a sagging bus draws more current. The electrodes go down into the charge gently ({@link Tuning#SAF_RAMP_PER_SECOND}) and
 * come up at once; below {@link Tuning#SAF_UNDERVOLTAGE} of its tier the breaker opens until the volts come back.
 */
public final class FurnaceLoad {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final Voltage voltage;
	private final double powerFactor;
	private double demand;
	private double limit;
	private boolean undervoltage;

	private final int[] hot = new int[3];
	private int star;
	private final int[] branch = {-1, -1, -1};

	private double volts;
	private double amps;
	private double watts;
	private double vars;

	public FurnaceLoad(Voltage voltage, double powerFactor) {
		this.voltage = voltage;
		this.powerFactor = powerFactor;
	}

	public Voltage voltage() {
		return voltage;
	}

	/** What the furnace wants, watts. */
	public void setDemand(double wattsWanted) {
		demand = Math.max(0, wattsWanted);
		if (demand <= 0) {
			limit = 0;   // the electrodes raised
		}
	}

	public double demand() {
		return demand;
	}

	public void build(Circuit circuit, BlockPos pos, java.util.List<Direction> faces) {
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
		}
		java.util.Arrays.fill(branch, -1);
		double take = demand * limit;
		if (take > 1 && !undervoltage) {
			double v = volts > voltage.lineToLine() * 0.5 ? volts : voltage.lineToLine();   // constant power: from the volts it sees
			double z = v * v / (take / powerFactor);
			double r = z * powerFactor;
			double x = z * Math.sqrt(1 - powerFactor * powerFactor);
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
		watts = 0;
		vars = 0;
		amps = 0;
		for (int k = 0; k < 3; k++) {
			if (branch[k] >= 0) {
				Complex i = circuit.currentPhasor(branch[k]);
				Complex s = circuit.acrossPhasor(hot[k], star).times(i.conjugate());
				watts += s.re();
				vars += s.im();
				amps = Math.max(amps, i.abs());
			}
		}
		if (volts < voltage.lineToLine() * Tuning.SAF_UNDERVOLTAGE) {
			undervoltage = true;
			limit = 0;
			return;
		}
		undervoltage = false;
		if (demand > 0) {
			limit = Math.min(1.0, limit + Tuning.SAF_RAMP_PER_SECOND * ticks / 20.0);
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

	/** Reactive power it draws, var (positive: lagging). */
	public double vars() {
		return vars;
	}

	public boolean undervoltage() {
		return undervoltage;
	}

	/** True while the electrodes are still going down to their set point. */
	public boolean ramping() {
		return demand > 0 && limit < 0.999;
	}

	public String describe() {
		if (undervoltage) {
			return String.format("NO SUPPLY: %.0f V, its breaker is open", volts);
		}
		return String.format("%.0f V, %.0f A", volts, amps);
	}

	public void save(net.minecraft.world.level.storage.ValueOutput output) {
		output.putDouble("furnaceLimit", limit);
	}

	public void load(net.minecraft.world.level.storage.ValueInput input) {
		limit = input.getDoubleOr("furnaceLimit", 0);
	}
}
