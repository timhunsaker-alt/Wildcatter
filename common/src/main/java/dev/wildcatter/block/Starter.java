package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.ThreePhaseSource;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Complex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One motor starter between the line and one three-phase motor, of one of three kinds:
 * <ul>
 * <li>{@link Kind#DIRECT}: a combination starter. Three contactor poles, closed while it runs; the motor starts direct on line.</li>
 * <li>{@link Kind#SOFT}: a soft starter (docs/multiblocks/soft-starter.md). While ramping, each pole is a series impedance it
 * adjusts every solve to hold the current at its limit dial (a multiple of the full-load amps); once the current falls to
 * {@link Tuning#SOFT_START_BYPASS} the bypass contactor closes.</li>
 * <li>{@link Kind#DRIVE}: a variable-frequency drive (docs/multiblocks/vfd.md). Its rectifier takes constant power from the line
 * at {@link Tuning#DRIVE_LINE_POWER_FACTOR}; its inverter is a three-phase source of its own on the motor side, marked as a drive
 * output, ramping from nothing to full voltage over {@link Tuning#DRIVE_RAMP_SECONDS}. Above its current limit the ramp holds,
 * and held too long it trips. Below {@link Tuning#LAND_RIG_UNDERVOLTAGE} of the line voltage it stops, and starts again when the
 * voltage is back.</li>
 * </ul>
 * Every kind has the same overload relay: it heats on the square of the current above 115 % of its setting and trips class 10,
 * within 10 s at 6 times the setting, a couple of minutes at twice it. The direct and soft starters' short-circuit protection opens
 * at once at 13 times the setting.
 * <p>
 * A standalone {@link MotorStarterBlock} holds a direct one, an MCC starter section ({@link MccSectionBlockEntity}) one per bucket
 * space, a starter cabinet ({@link StarterCabinetBlockEntity}) one of its kind. Its owner says what changed: a new run state to
 * show, a trip to sound.
 */
public final class Starter {
	public enum Kind { DIRECT, SOFT, DRIVE }

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final Kind kind;
	private final Voltage voltage;
	private final double[] dial;

	private final int[] load = new int[3];
	private int[] line = new int[3];
	private int[] poles = new int[0];
	private MotorStarterBlock.Run run = MotorStarterBlock.Run.STOPPED;
	private int setting;
	/** 0 to 1: how close the overload relay is to tripping. */
	private double heat;
	private double amps;
	private boolean powered;
	private String lastTrip = "";

	// the soft starter
	private int limitSetting = Tuning.SOFT_START_LIMIT_DEFAULT;
	private boolean bypassed;
	private double seriesOhms = -1;
	private double rampSeconds;
	private boolean sawInrush;

	// the drive
	private double ramp;
	private double outWatts;
	private double outAmps;
	private double lineVolts;
	private double lineWatts;
	private double overSeconds;
	private boolean undervoltage;
	private int star;
	private int outNeutral;
	private int[] outBranch = new int[0];

	/** A combination starter at 480 V, on the starter's overload dial. */
	public Starter() {
		this(Kind.DIRECT, Voltage.INDUSTRIAL, Tuning.OVERLOAD_SETTINGS);
	}

	public Starter(Kind kind, Voltage voltage, double[] dial) {
		this.kind = kind;
		this.voltage = voltage;
		this.dial = dial;
		this.setting = voltage == Voltage.MEDIUM ? 3 : Tuning.OVERLOAD_DEFAULT;
	}

	public Kind kind() {
		return kind;
	}

	// ---- HAND / OFF / AUTO, and the PLC's coil (docs/multiblocks/plc.md) --------------------------------------------------------

	/** A device in AUTO stops when it has heard no command for this long: a cut cable, a dead PLC and STOP all look the same. */
	public static final int COMMAND_STALE_TICKS = 10;
	public static final java.util.List<dev.wildcatter.control.ControlDevice.Point> POINTS = java.util.List.of(
			new dev.wildcatter.control.ControlDevice.Point("RUN", true), new dev.wildcatter.control.ControlDevice.Point("RUNNING", false),
			new dev.wildcatter.control.ControlDevice.Point("TRIPPED", false), new dev.wildcatter.control.ControlDevice.Point("AUTO", false));
	/** A drive's: the four above, and its speed reference and two readings. */
	public static final java.util.List<dev.wildcatter.control.ControlDevice.Point> DRIVE_POINTS = java.util.List.of(POINTS.get(0), POINTS.get(1), POINTS.get(2), POINTS.get(3),
			new dev.wildcatter.control.ControlDevice.Point("SPEED REF 4-20 mA", true, dev.wildcatter.control.ControlDevice.Signal.MA),
			new dev.wildcatter.control.ControlDevice.Point("SPEED 4-20 mA", false, dev.wildcatter.control.ControlDevice.Signal.MA),
			new dev.wildcatter.control.ControlDevice.Point("AMPS 4-20 mA, 150 %", false, dev.wildcatter.control.ControlDevice.Signal.MA));
	private double referenceMa;
	private long referenceTick = -COMMAND_STALE_TICKS - 1;
	private long gameTime;
	private double speedTarget = 1.0;

	private dev.wildcatter.control.Hoa hoa = dev.wildcatter.control.Hoa.HAND;
	private boolean commanded;
	private long commandTick = -COMMAND_STALE_TICKS - 1;

	public dev.wildcatter.control.Hoa hoa() {
		return hoa;
	}

	/** Steps the selector; true if the run state changed. Only HAND keeps it running: leaving it stops the motor until AUTO's command comes. */
	public boolean cycleHoa() {
		hoa = hoa.next();
		return run != MotorStarterBlock.Run.TRIPPED && hoa != dev.wildcatter.control.Hoa.HAND && setRun(MotorStarterBlock.Run.STOPPED);
	}

	/** The PLC's output, each scan; it counts only in AUTO. */
	public void command(boolean on, long now) {
		commanded = on;
		commandTick = now;
	}

	/** A contactor whose contacts have welded: it stays closed when its coil drops (a test hook; nothing in play welds one yet). */
	private boolean welded;

	public void setWeldedForTest(boolean welded) {
		this.welded = welded;
	}

	/** Follows the PLC, in AUTO. True if the run state changed. A trip stays until its hand reset. */
	public boolean followCommand(long now) {
		if (hoa != dev.wildcatter.control.Hoa.AUTO || run == MotorStarterBlock.Run.TRIPPED || welded && run == MotorStarterBlock.Run.RUNNING) {
			return false;
		}
		return setRun(commanded && now - commandTick <= COMMAND_STALE_TICKS ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
	}

	public java.util.List<dev.wildcatter.control.ControlDevice.Point> points() {
		return kind == Kind.DRIVE ? DRIVE_POINTS : POINTS;
	}

	/** The speed reference, from the PLC's analog module, each scan; it counts only in AUTO. */
	public void commandSpeed(double milliamps, long now) {
		referenceMa = milliamps;
		referenceTick = now;
		gameTime = now;
	}

	/** Where the drive is heading, 0 to 1: its reference in AUTO if one is coming, otherwise full speed as ever. */
	private double target() {
		boolean fresh = hoa == dev.wildcatter.control.Hoa.AUTO && gameTime - referenceTick <= COMMAND_STALE_TICKS && referenceMa >= 3.6;
		return fresh ? Math.max(Tuning.DRIVE_MIN_SPEED, Math.min(1.0, (referenceMa - 4) / 16)) : 1.0;
	}

	public void noteTime(long now) {
		gameTime = now;
	}

	/** SPEED and AMPS, in milliamps. */
	public double readMa(int point) {
		return switch (point) {
			case 5 -> 4 + 16 * ramp;
			case 6 -> 4 + 16 * Math.min(1.25, outAmps / (overloadAmps() * 1.5));
			default -> 0;
		};
	}

	/** What a transmitter clamped on it reads. */
	public double measure(dev.wildcatter.control.Measurable.Quantity quantity) {
		double running = run == MotorStarterBlock.Run.RUNNING ? 1 : 0;
		return switch (quantity) {
			case SPEED -> (kind == Kind.DRIVE ? ramp : running) * 100;
			case AMPS -> kind == Kind.DRIVE ? outAmps : amps;
			case VOLTS -> lineVolts;
			case WATTS -> kind == Kind.DRIVE ? lineWatts : Double.NaN;
			case HERTZ -> (kind == Kind.DRIVE ? ramp : running) * dev.wildcatter.energy.ElectricalStandard.active().hertz();
			case POWER_FACTOR -> kind == Kind.DRIVE ? linePowerFactor() : Double.NaN;
			default -> Double.NaN;
		};
	}

	/** RUNNING, TRIPPED and AUTO, as {@link #POINTS} numbers them. */
	public boolean readPoint(int point) {
		return switch (point) {
			case 1 -> run == MotorStarterBlock.Run.RUNNING;
			case 2 -> run == MotorStarterBlock.Run.TRIPPED;
			case 3 -> hoa == dev.wildcatter.control.Hoa.AUTO;
			default -> false;
		};
	}

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
		return dial[Math.max(0, Math.min(dial.length - 1, setting))];
	}

	public void nextOverloadSetting() {
		setting = (setting + 1) % dial.length;
	}

	/** The soft starter's current limit, in multiples of the full-load amps. */
	public double limitMultiple() {
		return Tuning.SOFT_START_LIMITS[Math.max(0, Math.min(Tuning.SOFT_START_LIMITS.length - 1, limitSetting))];
	}

	public void nextLimitSetting() {
		limitSetting = (limitSetting + 1) % Tuning.SOFT_START_LIMITS.length;
	}

	public boolean bypassed() {
		return bypassed;
	}

	/** The drive's ramp, 0 to 1 of full frequency and voltage. */
	public double ramp() {
		return ramp;
	}

	public double lineAmps() {
		return amps;
	}

	public double lineWatts() {
		return lineWatts;
	}

	/** The drive's line-side power factor, as measured (0 while it draws nothing). */
	public double linePowerFactor() {
		double apparent = Math.sqrt(3) * lineVolts * amps;
		return apparent > 1 ? lineWatts / apparent : 0;
	}

	public double outputAmps() {
		return outAmps;
	}

	/** The start/stop button: start when stopped, stop when running, reset (to stopped) after a trip. True if the run state changed. */
	public boolean pushButton() {
		if (hoa != dev.wildcatter.control.Hoa.HAND && run != MotorStarterBlock.Run.TRIPPED) {
			return false;
		}
		return setRun(run == MotorStarterBlock.Run.STOPPED ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
	}

	/** Redstone control: a rising signal starts it, a falling one stops it. A tripped starter waits for a reset by hand. */
	public boolean redstone(boolean signal) {
		if (signal == powered) {
			return false;
		}
		powered = signal;
		return hoa == dev.wildcatter.control.Hoa.HAND && run != MotorStarterBlock.Run.TRIPPED && setRun(signal ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
	}

	/** Its poles from {@code line} (one node per phase) to its motor side, closed while it runs and {@code fed}. */
	public void build(Circuit circuit, int[] line, boolean fed) {
		this.line = line;
		boolean closed = run == MotorStarterBlock.Run.RUNNING && fed;
		poles = new int[0];
		outBranch = new int[0];
		for (int k = 0; k < 3; k++) {
			load[k] = circuit.node();
		}
		if (!closed) {
			return;
		}
		switch (kind) {
			case DIRECT -> {
				poles = new int[3];
				for (int k = 0; k < 3; k++) {
					poles[k] = circuit.resistor(line[k], load[k], Tuning.BREAKER_CLOSED_OHMS);
				}
			}
			case SOFT -> {
				poles = new int[3];
				// before the first measurement, enough series impedance to hold the limit with the motor shorted
				double x = seriesOhms >= 0 ? seriesOhms : voltage.nominal() / (limitMultiple() * overloadAmps());
				for (int k = 0; k < 3; k++) {
					poles[k] = bypassed ? circuit.resistor(line[k], load[k], Tuning.BREAKER_CLOSED_OHMS)
							: circuit.impedance(line[k], load[k], Tuning.BREAKER_CLOSED_OHMS, x);
					if (!bypassed) {
						circuit.markSoftStart(load[k]);
					}
				}
			}
			case DRIVE -> {
				if (undervoltage) {
					return;
				}
				// the rectifier: a balanced star at its front end's power factor, drawing what the inverter gave out last solve
				double take = outWatts / Tuning.DRIVE_EFFICIENCY;
				if (take > 1) {
					double at = lineVolts > voltage.lineToLine() * 0.5 ? lineVolts : voltage.lineToLine();
					double pf = Tuning.DRIVE_LINE_POWER_FACTOR;
					double z = at * at / (take / pf);
					star = circuit.node();
					poles = new int[3];
					for (int k = 0; k < 3; k++) {
						poles[k] = circuit.impedance(line[k], star, z * pf, z * Math.sqrt(1 - pf * pf));
					}
				}
				// the inverter: its own supply on the motor side
				outNeutral = circuit.node();
				circuit.ground(outNeutral);
				outBranch = new int[3];
				for (int k = 0; k < 3; k++) {
					int internal = circuit.node();
					circuit.source(internal, outNeutral, voltage.nominal() * ramp, ThreePhaseSource.DEGREES[k]);
					outBranch[k] = circuit.resistor(internal, load[k], Tuning.DRIVE_OUTPUT_OHMS);
					circuit.markDriveOutput(load[k]);
					circuit.markDriveReference(load[k], speedTarget);
				}
			}
		}
	}

	/** The motor leads (A, B, C and the ground) on one face of one cell. */
	public void motorPort(Circuit circuit, BlockPos cell, Direction face, int ground) {
		for (int k = 0; k < 3; k++) {
			circuit.port(cell, face, PHASES[k], voltage, load[k], Tuning.CONTACT_OHMS / 2, false);
		}
		circuit.port(cell, face, Conductor.G, voltage, ground, Tuning.CONTACT_OHMS / 2, false);
	}

	/** The line side (A, B, C and the ground) on one face of one cell, for a standalone starter. */
	public static void linePort(Circuit circuit, BlockPos cell, Direction face, int[] line, int ground) {
		linePort(circuit, cell, face, Voltage.INDUSTRIAL, line, ground);
	}

	/** The same at {@code voltage}. */
	public static void linePort(Circuit circuit, BlockPos cell, Direction face, Voltage voltage, int[] line, int ground) {
		for (int k = 0; k < 3; k++) {
			circuit.port(cell, face, PHASES[k], voltage, line[k], Tuning.CONTACT_OHMS / 2, false);
		}
		circuit.port(cell, face, Conductor.G, voltage, ground, Tuning.CONTACT_OHMS / 2, false);
	}

	private double mostAmps(Circuit circuit, int[] branches) {
		double most = 0;
		for (int branch : branches) {
			most = Math.max(most, circuit.current(branch));
		}
		return most;
	}

	/** The short-circuit protection: a short opens it at once, before anything else sees the fault. True if it tripped. */
	public boolean protect(Circuit circuit) {
		if (poles.length == 0 || kind == Kind.DRIVE) {
			return false;   // a drive limits its own output current
		}
		double now = mostAmps(circuit, poles);
		if (now > overloadAmps() * Tuning.MCP_MULTIPLE) {
			trip(String.format("short circuit: %.0f A, the protector opens at %.0f A", now, overloadAmps() * Tuning.MCP_MULTIPLE));
			return true;
		}
		return false;
	}

	/** Reads the solve: the soft starter's ramp, the drive's ramp and limits, and the overload relay. True if it tripped. */
	public boolean apply(Circuit circuit, int ticks) {
		double dt = ticks / 20.0;
		amps = mostAmps(circuit, poles);
		boolean running = run == MotorStarterBlock.Run.RUNNING;
		double fla = overloadAmps();
		double motorAmps = amps;
		switch (kind) {
			case DIRECT -> {
			}
			case SOFT -> {
				if (running && poles.length > 0 && !bypassed) {
					rampSeconds += dt;
					if (amps > fla * Tuning.SOFT_START_BYPASS * 1.2) {
						sawInrush = true;
					}
					if ((sawInrush && amps < fla * Tuning.SOFT_START_BYPASS) || (!sawInrush && rampSeconds >= Tuning.SOFT_START_NO_MOTOR_SECONDS)) {
						bypassed = true;
					} else {
						// the motor's impedance as it stands, and the series reactance that holds the current at the limit
						Complex i = circuit.currentPhasor(poles[0]);
						if (i.abs() > 0.01) {
							Complex motor = circuit.voltagePhasor(load[0]).dividedBy(i);
							double total = circuit.voltage(line[0]) / (limitMultiple() * fla);
							double r = Math.abs(motor.re()) + Tuning.BREAKER_CLOSED_OHMS;
							seriesOhms = total > r ? Math.max(0, Math.sqrt(total * total - r * r) - motor.im()) : 0;
						}
					}
				} else if (!running) {
					bypassed = false;
					seriesOhms = -1;
					rampSeconds = 0;
					sawInrush = false;
				}
			}
			case DRIVE -> {
				Complex ab = circuit.acrossPhasor(line[0], line[1]);
				Complex bc = circuit.acrossPhasor(line[1], line[2]);
				Complex ca = circuit.acrossPhasor(line[2], line[0]);
				lineVolts = (ab.abs() + bc.abs() + ca.abs()) / 3.0;
				lineWatts = 0;
				for (int k = 0; k < poles.length; k++) {
					lineWatts += circuit.acrossPhasor(line[k], star).times(circuit.currentPhasor(poles[k]).conjugate()).re();
				}
				outWatts = 0;
				outAmps = 0;
				for (int k = 0; k < outBranch.length; k++) {
					Complex i = circuit.currentPhasor(outBranch[k]);
					outWatts += circuit.acrossPhasor(load[k], outNeutral).times(i.conjugate()).re();
					outAmps = Math.max(outAmps, i.abs());
				}
				outWatts = Math.max(0, outWatts);
				motorAmps = outAmps;
				if (running) {
					undervoltage = lineVolts < voltage.lineToLine() * Tuning.LAND_RIG_UNDERVOLTAGE;
					if (undervoltage) {
						ramp = 0;
						overSeconds = 0;
					} else if (outAmps > fla * Tuning.DRIVE_CURRENT_LIMIT) {
						overSeconds += dt;   // the ramp holds where it is
						if (overSeconds >= Tuning.DRIVE_LIMIT_SECONDS) {
							trip(String.format("overcurrent: %.0f A for %.0f s, over its %.0f A limit", outAmps, overSeconds, fla * Tuning.DRIVE_CURRENT_LIMIT));
							return true;
						}
					} else {
						overSeconds = Math.max(0, overSeconds - dt);
						speedTarget = target();
						ramp = ramp < speedTarget ? Math.min(speedTarget, ramp + dt / Tuning.DRIVE_RAMP_SECONDS) : Math.max(speedTarget, ramp - dt / Tuning.DRIVE_RAMP_SECONDS);
					}
				} else {
					ramp = 0;
					outWatts = 0;
					overSeconds = 0;
					undervoltage = false;
				}
			}
		}
		double ratio = motorAmps / fla;
		double pickup = Tuning.OVERLOAD_PICKUP * Tuning.OVERLOAD_PICKUP;
		if (ratio > Tuning.OVERLOAD_PICKUP) {
			// I-squared-t: at 6 times the setting the relay heats from cold to tripped in the class time
			heat += (ratio * ratio - pickup) / ((36.0 - pickup) * Tuning.OVERLOAD_CLASS_SECONDS) * dt;
		} else {
			heat = Math.max(0, heat - dt / Tuning.OVERLOAD_COOL_SECONDS);
		}
		if (heat >= 1.0 && running) {
			trip(String.format("overload: %.1f A on the %.1f A setting", motorAmps, fla));
			return true;
		}
		return false;
	}

	private void trip(String why) {
		heat = 0;
		amps = 0;
		lastTrip = why;
		run = MotorStarterBlock.Run.TRIPPED;
		bypassed = false;
		seriesOhms = -1;
		rampSeconds = 0;
		sawInrush = false;
		ramp = 0;
		outWatts = 0;
		overSeconds = 0;
	}

	/** What it is doing while it runs. */
	private String running() {
		return switch (kind) {
			case DIRECT -> String.format("RUNNING, %.1f A", amps);
			case SOFT -> bypassed ? String.format("BYPASSED, running %.1f A", amps)
					: String.format("RAMPING, limit %.1f x %.0f A = %.0f A, %.0f A", limitMultiple(), overloadAmps(), limitMultiple() * overloadAmps(), amps);
			case DRIVE -> undervoltage ? String.format("UNDERVOLTAGE: stopped, %.0f V", lineVolts)
					: String.format("%s %.1f Hz (%.0f %%), motor %.1f A, line %.1f A at %.2f PF, %.0f kW", ramp >= speedTarget * 0.995 ? "RUNNING" : "RAMPING",
							ramp * dev.wildcatter.energy.ElectricalStandard.active().hertz(), ramp * 100, outAmps, amps,
							linePowerFactor(), lineWatts / 1000);
		};
	}

	/** A short line for the MCC's readout. */
	public String brief() {
		String name = switch (kind) {
			case DIRECT -> "";
			case SOFT -> "soft starter ";
			case DRIVE -> "drive ";
		};
		return name + switch (run) {
			case STOPPED -> "stopped";
			case RUNNING -> {
				String now = running();   // "RUNNING, 3.1 A" reads "running, 3.1 A" in a list
				int word = now.indexOf(' ');
				yield now.substring(0, word).toLowerCase(java.util.Locale.ROOT) + now.substring(word);
			}
			case TRIPPED -> "TRIPPED (" + lastTrip + ")";
		};
	}

	/** Its state for a readout. */
	public String state() {
		String state = switch (run) {
			case STOPPED -> "STOPPED: right-click (or redstone) to start";
			case RUNNING -> running();
			case TRIPPED -> "TRIPPED (" + lastTrip + "): fix the motor, then right-click to reset";
		};
		String dials = kind == Kind.SOFT
				? String.format(", full-load amps set to %.1f A, limit %.1f x; sneak and right-click steps the amps, with the Power Meter the limit", overloadAmps(), limitMultiple())
				: String.format(", %s set to %.1f A; sneak and right-click steps the dial", kind == Kind.DRIVE ? "motor full-load amps" : "overload", overloadAmps());
		state = (hoa == dev.wildcatter.control.Hoa.HAND ? "" : "[" + hoa + "] ") + state;
		return state + (heat > 0.05 ? ", overload relay heating " + Math.round(heat * 100) + "%" : "") + dials;
	}

	public void save(ValueOutput output, String prefix) {
		output.putString(prefix + "run", run.getSerializedName());
		output.putString(prefix + "hoa", hoa.name());
		output.putInt(prefix + "setting", setting);
		output.putDouble(prefix + "heat", heat);
		output.putString(prefix + "lastTrip", lastTrip);
		output.putBoolean(prefix + "powered", powered);
		output.putInt(prefix + "limit", limitSetting);
		output.putBoolean(prefix + "bypassed", bypassed);
		output.putDouble(prefix + "ramp", ramp);
	}

	public void load(ValueInput input, String prefix) {
		String name = input.getStringOr(prefix + "run", "");
		for (MotorStarterBlock.Run r : MotorStarterBlock.Run.values()) {
			if (r.getSerializedName().equals(name)) {
				run = r;
			}
		}
		hoa = dev.wildcatter.control.Hoa.valueOf(input.getStringOr(prefix + "hoa", "HAND"));
		setting = input.getIntOr(prefix + "setting", setting);
		heat = input.getDoubleOr(prefix + "heat", 0);
		lastTrip = input.getStringOr(prefix + "lastTrip", "");
		powered = input.getBooleanOr(prefix + "powered", false);
		limitSetting = input.getIntOr(prefix + "limit", Tuning.SOFT_START_LIMIT_DEFAULT);
		bypassed = input.getBooleanOr(prefix + "bypassed", false);
		ramp = input.getDoubleOr(prefix + "ramp", 0);
	}
}
