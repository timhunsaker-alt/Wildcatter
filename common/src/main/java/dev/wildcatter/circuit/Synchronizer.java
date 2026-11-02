package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A generator's synchronizer (ELECTRICAL_MODEL: generators must synchronize before closing onto a live island). With the output
 * breaker open it reads the bus through potential transformers (a very high impedance from each phase to earth) and tracks the
 * machine's slip angle against it: theta += 360 x (f machine - f bus) x dt. AUTO trims the machine a hair fast and closes inside the
 * sync-check window; MANUAL leaves the speed trim and the close to the player. Used by the genset panel and the steam plant.
 */
public final class Synchronizer {
	public enum Mode {
		AUTO, MANUAL
	}

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private Mode mode = Mode.AUTO;
	private int trim = 2;   // index into SYNC_TRIM_HZ: no trim
	private double slipDegrees;
	private double ownHertz;
	private double busVolts;
	private double busHertz;
	private final int[] pt = new int[3];
	private boolean ptBuilt;
	private boolean busRead;
	private String lastClose = "";

	public Mode mode() {
		return mode;
	}

	public void setMode(Mode value) {
		mode = value;
	}

	public void toggleMode() {
		mode = mode == Mode.AUTO ? Mode.MANUAL : Mode.AUTO;
	}

	/** The manual raise/lower switch: steps through SYNC_TRIM_HZ. */
	public void stepTrim() {
		trim = (trim + 1) % Tuning.SYNC_TRIM_HZ.length;
	}

	public double ownHertz() {
		return ownHertz;
	}

	public double slipDegrees() {
		return slipDegrees;
	}

	public void setSlipForTest(double degrees) {
		slipDegrees = degrees;
	}

	public boolean busRead() {
		return busRead;
	}

	public boolean busLive(Voltage tier) {
		return busVolts > tier.nominal() * 0.1;
	}

	public String lastClose() {
		return lastClose;
	}

	public double slipHertz() {
		return ownHertz - busHertz;
	}

	/** The breaker closed or the machine stopped: start afresh next time. */
	public void reset(double nominalHertz) {
		busRead = false;
		slipDegrees = 0;
		ownHertz = nominalHertz;
	}

	/** Breaker open: the potential transformers on every face that {@code faces} allows. */
	public void buildTransformers(Circuit circuit, BlockPos pos, Voltage tier, Predicate<Direction> faces) {
		java.util.List<Direction> on = new java.util.ArrayList<>();
		for (Direction face : Direction.values()) {
			if (faces.test(face)) {
				on.add(face);
			}
		}
		buildTransformers(circuit, java.util.List.of(pos), on, tier);
	}

	/** The same, on {@code faces} of each of {@code cells} (a big part's terminal box). */
	public void buildTransformers(Circuit circuit, java.util.List<BlockPos> cells, java.util.List<Direction> faces, Voltage tier) {
		int earth = circuit.node();
		circuit.ground(earth);
		for (int k = 0; k < 3; k++) {
			pt[k] = circuit.node();
			circuit.resistor(pt[k], earth, Tuning.PT_OHMS);
			for (BlockPos pos : cells) {
				for (Direction face : faces) {
					circuit.port(pos, face, PHASES[k], tier, pt[k], Tuning.CONTACT_OHMS / 2, false);
				}
			}
		}
		ptBuilt = true;
	}

	/** After a solve: read the bus, if the transformers were built this solve. */
	public void read(Circuit circuit) {
		if (ptBuilt) {
			busVolts = Math.max(circuit.voltage(pt[0]), Math.max(circuit.voltage(pt[1]), circuit.voltage(pt[2])));
			busHertz = circuit.hertz();
			busRead = true;
		} else {
			busVolts = 0;
		}
	}

	/** Called at the start of each build: the transformers exist only while the machine runs with its breaker open. */
	public void beginBuild() {
		ptBuilt = false;
	}

	/**
	 * One tick with the breaker open. Returns a reason to close now ("onto a dead bus", "in sync ...") in AUTO, or null.
	 * The machine runs at {@link #ownHertz()} meanwhile.
	 */
	public String step(double nominalHertz, Voltage tier) {
		if (!busRead) {
			return null;
		}
		if (!busLive(tier)) {
			ownHertz = nominalHertz + (mode == Mode.MANUAL ? Tuning.SYNC_TRIM_HZ[trim] : 0);
			slipDegrees = 0;
			return mode == Mode.AUTO ? "onto a dead bus" : null;
		}
		ownHertz = mode == Mode.AUTO ? busHertz + Tuning.AUTO_SYNC_SLIP_HZ : nominalHertz + Tuning.SYNC_TRIM_HZ[trim];
		slipDegrees += 360.0 * slipHertz() / 20.0;
		slipDegrees = ((slipDegrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
		return mode == Mode.AUTO && inWindow(tier) ? String.format("in sync (%.0f degrees, %.2f Hz slip)", slipDegrees, slipHertz()) : null;
	}

	public boolean inWindow(Voltage tier) {
		double volts = Math.abs(tier.nominal() - busVolts) / tier.nominal();
		return Math.abs(slipDegrees) <= Tuning.SYNC_MAX_DEGREES && Math.abs(slipHertz()) <= Tuning.SYNC_MAX_HZ && volts <= Tuning.SYNC_MAX_VOLTS;
	}

	/** The fault of closing now, in per unit of the machine's rating: 2 sin(theta / 2) / X''d. */
	public double closingFaultPu() {
		return 2.0 * Math.sin(Math.toRadians(Math.abs(slipDegrees)) / 2.0) / Tuning.ALTERNATOR_XDPP;
	}

	public void closed(String how) {
		lastClose = how;
		busRead = false;
		slipDegrees = 0;
	}

	/** The readout while the breaker is open on a live bus. */
	public String scope() {
		return String.format("synchroscope: %.2f Hz %s, %.0f degrees %s", Math.abs(slipHertz()), slipHertz() >= 0 ? "fast" : "slow",
				Math.abs(slipDegrees), slipDegrees >= 0 ? "ahead" : "behind")
				+ (mode == Mode.MANUAL ? String.format(" (speed trim %+.1f Hz): right-click to close", Tuning.SYNC_TRIM_HZ[trim]) : "");
	}

	public void save(ValueOutput output) {
		output.putString("sync", mode.name());
		output.putInt("trim", trim);
		output.putString("lastClose", lastClose);
	}

	public void load(ValueInput input) {
		mode = "MANUAL".equals(input.getStringOr("sync", "AUTO")) ? Mode.MANUAL : Mode.AUTO;
		trim = Math.max(0, Math.min(Tuning.SYNC_TRIM_HZ.length - 1, input.getIntOr("trim", 2)));
		lastClose = input.getStringOr("lastClose", "");
	}
}
