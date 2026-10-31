package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Complex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The electrical side of an induction motor (docs/phase3-three-phase.md). Running, each phase is an impedance drawing the
 * full-load current at a power factor of 0.8; starting, the impedance drops to the locked-rotor value (6 x the current at
 * PF 0.35) for the start time. Stopped, it draws nothing. A three-phase motor is wired in a floating star across A, B and C;
 * a single-phase motor sits between whichever phase reaches it and the neutral.
 *
 * Three-phase motors also read the phase rotation (A, B, C or the reverse: a reversed motor runs backward) and notice
 * single-phasing: with one phase lost a running motor stalls and draws locked-rotor current until something trips.
 */
public final class Motor {
	public enum State { STOPPED, STARTING, RUNNING }

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final boolean threePhase;
	private final Voltage voltage;
	private final double ratedWatts;

	private State state = State.STOPPED;
	private double startSeconds;
	private boolean stalled;
	private boolean reversed;
	/** Two leads swapped at the terminal box (B and C): the way an electrician reverses a three-phase motor. */
	private boolean leadsSwapped;

	private final int[] hot = new int[3];
	private int neutral;
	private int star;
	private final int[] branch = {-1, -1, -1};

	private double volts;
	private double amps;
	private double watts;
	private boolean supplied;

	public Motor(boolean threePhase, Voltage voltage, double ratedWatts) {
		this.threePhase = threePhase;
		this.voltage = voltage;
		this.ratedWatts = ratedWatts;
	}

	public Voltage voltage() {
		return voltage;
	}

	public boolean threePhase() {
		return threePhase;
	}

	/** The full-load current of each line. */
	public double fullLoadAmps() {
		return ratedWatts / ((threePhase ? 3 : 1) * voltage.nominal() * Tuning.MOTOR_POWER_FACTOR);
	}

	/** Terminals on every face, and the windings if it is turning or starting. */
	public void build(Circuit circuit, BlockPos pos) {
		int phases = threePhase ? 3 : 1;
		for (int k = 0; k < phases; k++) {
			hot[k] = circuit.node();
		}
		neutral = circuit.node();
		java.util.Arrays.fill(branch, -1);
		if (state != State.STOPPED) {
			boolean locked = state == State.STARTING || stalled;
			double amps = fullLoadAmps() * (locked ? Tuning.MOTOR_LOCKED_ROTOR_MULTIPLE : 1.0);
			double pf = locked ? Tuning.MOTOR_LOCKED_ROTOR_POWER_FACTOR : Tuning.MOTOR_POWER_FACTOR;
			double z = voltage.nominal() / amps;
			double r = z * pf;
			double x = z * Math.sqrt(1 - pf * pf);
			if (threePhase) {
				star = circuit.node();   // a floating star: the windings meet here, not at the neutral
				for (int k = 0; k < 3; k++) {
					branch[k] = circuit.impedance(hot[k], star, r, x);
				}
			} else {
				branch[0] = circuit.impedance(hot[0], neutral, r, x);
			}
		}
		for (Direction face : Direction.values()) {
			if (threePhase) {
				for (int k = 0; k < 3; k++) {
					int lead = leadsSwapped && k > 0 ? 3 - k : k;   // swapped: B goes to the winding C was on, and C to B's
					circuit.port(pos, face, PHASES[k], voltage, hot[lead], Tuning.CONTACT_OHMS / 2, false);
				}
			} else {
				circuit.hotPort(pos, face, voltage, hot[0], Tuning.CONTACT_OHMS / 2, false);
				circuit.port(pos, face, Conductor.N, voltage, neutral, Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	/** Reads the circuit and moves the motor on (stopped, starting, running): {@code wanted} is whether its machine has work for it. */
	public void apply(Circuit circuit, int ticks, boolean wanted) {
		watts = 0;
		amps = 0;
		if (threePhase) {
			Complex ab = circuit.acrossPhasor(hot[0], hot[1]);
			Complex bc = circuit.acrossPhasor(hot[1], hot[2]);
			Complex ca = circuit.acrossPhasor(hot[2], hot[0]);
			volts = (ab.abs() + bc.abs() + ca.abs()) / 3.0;
			double floor = voltage.lineToLine() * Tuning.BRIGHT_ABOVE;
			supplied = ab.abs() > floor && bc.abs() > floor && ca.abs() > floor;
			// ABC rotation: B-C lags A-B by 120 degrees
			double lag = ab.degrees() - bc.degrees();
			lag = ((lag % 360) + 540) % 360 - 180;
			reversed = lag < 0;
		} else {
			volts = circuit.across(hot[0], neutral);
			supplied = volts > voltage.nominal() * Tuning.BRIGHT_ABOVE;
		}
		double most = 0;
		double least = Double.MAX_VALUE;
		for (int k = 0; k < (threePhase ? 3 : 1); k++) {
			if (branch[k] >= 0) {
				Complex i = circuit.currentPhasor(branch[k]);
				Complex v = circuit.acrossPhasor(hot[k], threePhase ? star : neutral);
				most = Math.max(most, i.abs());
				least = Math.min(least, i.abs());
				watts += v.times(i.conjugate()).re();
			}
		}
		amps = most;
		double dt = ticks / 20.0;

		if (!wanted) {
			state = State.STOPPED;
			stalled = false;
		} else {
			switch (state) {
				case STOPPED -> {
					if (supplied) {
						state = State.STARTING;
						startSeconds = 0;
					}
				}
				case STARTING -> {
					startSeconds += dt;
					if (volts < 1.0) {
						state = State.STOPPED;
					} else if (startSeconds >= Tuning.MOTOR_START_SECONDS) {
						state = State.RUNNING;
					}
				}
				case RUNNING -> {
					if (volts < 1.0) {
						state = State.STOPPED;
						stalled = false;
					}
				}
			}
			// one line carrying almost nothing while the others carry current: a phase is lost, and the motor stalls
			if (threePhase && state != State.STOPPED && most > 0) {
				stalled = least < most * 0.2;
			}
		}
	}

	/** How much of its rated work it does: 0 unless running the right way round on a full supply. */
	public double output() {
		if (state != State.RUNNING || stalled || reversed) {
			return 0;
		}
		double nominal = threePhase ? voltage.lineToLine() : voltage.nominal();
		return volts < nominal * Tuning.BRIGHT_ABOVE ? 0 : Math.min(1.0, Math.pow(volts / nominal, 2));
	}

	public boolean leadsSwapped() {
		return leadsSwapped;
	}

	public void setLeadsSwapped(boolean swapped) {
		leadsSwapped = swapped;
	}

	public State state() {
		return state;
	}

	public boolean stalled() {
		return stalled;
	}

	public boolean reversed() {
		return reversed && state == State.RUNNING;
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

	/** One line for the machine's readout. */
	public String describe() {
		String supply = threePhase ? voltage.label() + " three-phase" : voltage.label();
		if (state == State.STOPPED) {
			return supplied ? "motor stopped (no work)" : volts < 1.0 ? "NO POWER: it needs " + supply : "too little voltage to start (" + Math.round(volts) + " V)";
		}
		String reading = String.format("%.0f V, %.1f A, %.0f W", volts, amps, watts);
		if (stalled) {
			return "STALLED: a phase is missing, drawing locked-rotor current (" + reading + ")";
		}
		if (state == State.STARTING) {
			return "starting, inrush " + reading;
		}
		if (reversed) {
			return "RUNNING BACKWARD: swap two phases (" + reading + ")";
		}
		return "running, " + reading;
	}
}
