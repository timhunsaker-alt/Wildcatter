package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One combination starter: three contactor poles between the line and the motor, closed while it runs. The motor circuit
 * protector opens at once on a short (13 times the overload setting). The overload relay heats on the square of the current above
 * 115 % of its setting and trips class 10: within 10 s at 6 times the setting, a couple of minutes at twice it.
 * <p>
 * A standalone {@link MotorStarterBlock} holds one; an MCC starter section ({@link MccSectionBlockEntity}) one per bucket space.
 * Its owner says what changed: a new run state to show, a trip to sound.
 */
public final class Starter {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final int[] load = new int[3];
	private int[] poles = new int[0];
	private MotorStarterBlock.Run run = MotorStarterBlock.Run.STOPPED;
	private int setting = Tuning.OVERLOAD_DEFAULT;
	/** 0 to 1: how close the overload relay is to tripping. */
	private double heat;
	private double amps;
	private boolean powered;
	private String lastTrip = "";

	public MotorStarterBlock.Run run() {
		return run;
	}

	/** Sets the run state; true if it changed. */
	public boolean setRun(MotorStarterBlock.Run run) {
		boolean changed = this.run != run;
		this.run = run;
		return changed;
	}

	public double overloadAmps() {
		return Tuning.OVERLOAD_SETTINGS[Math.max(0, Math.min(Tuning.OVERLOAD_SETTINGS.length - 1, setting))];
	}

	public void nextOverloadSetting() {
		setting = (setting + 1) % Tuning.OVERLOAD_SETTINGS.length;
	}

	/** The start/stop button: start when stopped, stop when running, reset (to stopped) after a trip. True if the run state changed. */
	public boolean pushButton() {
		return setRun(run == MotorStarterBlock.Run.STOPPED ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
	}

	/** Redstone control: a rising signal starts it, a falling one stops it. A tripped starter waits for a reset by hand. */
	public boolean redstone(boolean signal) {
		if (signal == powered) {
			return false;
		}
		powered = signal;
		return run != MotorStarterBlock.Run.TRIPPED && setRun(signal ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
	}

	/** Its contactor from {@code line} (one node per phase) to its motor side, closed while it runs and {@code fed}. */
	public void build(Circuit circuit, int[] line, boolean fed) {
		boolean closed = run == MotorStarterBlock.Run.RUNNING && fed;
		poles = closed ? new int[3] : new int[0];
		for (int k = 0; k < 3; k++) {
			load[k] = circuit.node();
			if (closed) {
				poles[k] = circuit.resistor(line[k], load[k], Tuning.BREAKER_CLOSED_OHMS);
			}
		}
	}

	/** The motor leads (A, B, C and the ground) on one face of one cell. */
	public void motorPort(Circuit circuit, BlockPos cell, Direction face, int ground) {
		for (int k = 0; k < 3; k++) {
			circuit.port(cell, face, PHASES[k], Voltage.INDUSTRIAL, load[k], Tuning.CONTACT_OHMS / 2, false);
		}
		circuit.port(cell, face, Conductor.G, Voltage.INDUSTRIAL, ground, Tuning.CONTACT_OHMS / 2, false);
	}

	/** The line side (A, B, C and the ground) on one face of one cell, for a standalone starter. */
	public static void linePort(Circuit circuit, BlockPos cell, Direction face, int[] line, int ground) {
		for (int k = 0; k < 3; k++) {
			circuit.port(cell, face, PHASES[k], Voltage.INDUSTRIAL, line[k], Tuning.CONTACT_OHMS / 2, false);
		}
		circuit.port(cell, face, Conductor.G, Voltage.INDUSTRIAL, ground, Tuning.CONTACT_OHMS / 2, false);
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int pole : poles) {
			most = Math.max(most, circuit.current(pole));
		}
		return most;
	}

	/** The motor circuit protector: a short opens it at once, before anything else sees the fault. True if it tripped. */
	public boolean protect(Circuit circuit) {
		if (poles.length == 0) {
			return false;
		}
		double now = mostAmps(circuit);
		if (now > overloadAmps() * Tuning.MCP_MULTIPLE) {
			trip(String.format("short circuit: %.0f A, the protector opens at %.0f A", now, overloadAmps() * Tuning.MCP_MULTIPLE));
			return true;
		}
		return false;
	}

	/** The overload relay. True if it tripped. */
	public boolean apply(Circuit circuit, int ticks) {
		amps = mostAmps(circuit);
		double ratio = amps / overloadAmps();
		double dt = ticks / 20.0;
		double pickup = Tuning.OVERLOAD_PICKUP * Tuning.OVERLOAD_PICKUP;
		if (ratio > Tuning.OVERLOAD_PICKUP) {
			// I-squared-t: at 6 times the setting the relay heats from cold to tripped in the class time
			heat += (ratio * ratio - pickup) / ((36.0 - pickup) * Tuning.OVERLOAD_CLASS_SECONDS) * dt;
		} else {
			heat = Math.max(0, heat - dt / Tuning.OVERLOAD_COOL_SECONDS);
		}
		if (heat >= 1.0 && run == MotorStarterBlock.Run.RUNNING) {
			trip(String.format("overload: %.1f A on the %.1f A setting", amps, overloadAmps()));
			return true;
		}
		return false;
	}

	private void trip(String why) {
		heat = 0;
		amps = 0;
		lastTrip = why;
		run = MotorStarterBlock.Run.TRIPPED;
	}

	/** A short line for the MCC's readout. */
	public String brief() {
		return switch (run) {
			case STOPPED -> "stopped";
			case RUNNING -> String.format("running %.1f A", amps);
			case TRIPPED -> "TRIPPED (" + lastTrip + ")";
		};
	}

	/** Its state for a readout. */
	public String state() {
		String state = switch (run) {
			case STOPPED -> "STOPPED: right-click (or redstone) to start";
			case RUNNING -> String.format("RUNNING, %.1f A", amps);
			case TRIPPED -> "TRIPPED (" + lastTrip + "): fix the motor, then right-click to reset";
		};
		return state + String.format(", overload set to %.1f A", overloadAmps()) + (heat > 0.05 ? ", relay heating " + Math.round(heat * 100) + "%" : "")
				+ "; sneak and right-click steps the overload dial";
	}

	public void save(ValueOutput output, String prefix) {
		output.putString(prefix + "run", run.getSerializedName());
		output.putInt(prefix + "setting", setting);
		output.putDouble(prefix + "heat", heat);
		output.putString(prefix + "lastTrip", lastTrip);
		output.putBoolean(prefix + "powered", powered);
	}

	public void load(ValueInput input, String prefix) {
		String name = input.getStringOr(prefix + "run", "");
		for (MotorStarterBlock.Run r : MotorStarterBlock.Run.values()) {
			if (r.getSerializedName().equals(name)) {
				run = r;
			}
		}
		setting = input.getIntOr(prefix + "setting", Tuning.OVERLOAD_DEFAULT);
		heat = input.getDoubleOr(prefix + "heat", 0);
		lastTrip = input.getStringOr(prefix + "lastTrip", "");
		powered = input.getBooleanOr(prefix + "powered", false);
	}
}
