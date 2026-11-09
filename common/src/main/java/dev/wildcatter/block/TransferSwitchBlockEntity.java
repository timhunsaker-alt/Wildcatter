package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The inside of a 480 V open-transition automatic transfer switch (docs/multiblocks/ats.md): two sets of three-pole contacts, NORMAL to
 * LOAD and EMERGENCY to LOAD, never closed together, and the controller that runs them.
 * <ul>
 * <li>Normal fails below 85 % of nominal on any phase (or under a test); after TDES it calls for the genset (redstone).</li>
 * <li>Once emergency is at 90 % of voltage and 95 % of frequency, after TDNE, it opens normal, waits in the neutral position, and closes
 * emergency.</li>
 * <li>Normal restored at 90 %: after TDEN it opens emergency, waits, closes normal, and after TDEC it drops the call.</li>
 * </ul>
 * The neutral is solid and the ground runs through.
 */
public class TransferSwitchBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, dev.wildcatter.control.ControlDevice {
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(2);
	private boolean plcTest;
	private boolean inhibit;

	@Override
	public String deviceName() {
		return "Automatic transfer switch";
	}

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("TEST", true), new Point("INHIBIT TRANSFER", true), new Point("ON NORMAL", false), new Point("ON EMERGENCY", false), new Point("NORMAL OK", false), new Point("EMERGENCY OK", false), new Point("ENGINE START", false), new Point("AUTO", false));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case 2 -> stage.normalClosed();
			case 3 -> stage.emergencyClosed();
			case 4 -> normalVolts >= Voltage.INDUSTRIAL.lineToLine() * Tuning.ATS_PICKUP;
			case 5 -> emergencyVolts >= Voltage.INDUSTRIAL.lineToLine() * Tuning.ATS_PICKUP;
			case 6 -> engineStart();
			case 7 -> remote.auto();
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point > 1) {
			return;
		}
		remote.command(point, on, gameTime);
		plcTest = remote.on(0, gameTime);
		inhibit = remote.on(1, gameTime);
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	public enum Stage {
		NORMAL, TDES, START, TDNE, TO_EMERGENCY, CLOSED_TO_EMERGENCY, EMERGENCY, TDEN, TO_NORMAL, CLOSED_TO_NORMAL, TDEC;

		boolean normalClosed() {
			return this == NORMAL || this == TDES || this == START || this == TDNE || this == TDEC || overlap();
		}

		boolean emergencyClosed() {
			return this == EMERGENCY || this == TDEN || overlap();
		}

		/** A closed transition's make-before-break: both sets of contacts closed for a moment. */
		boolean overlap() {
			return this == CLOSED_TO_EMERGENCY || this == CLOSED_TO_NORMAL;
		}

		boolean engineStart() {
			return this != NORMAL && this != TDES;
		}
	}

	/**
	 * How it moves the load between two live sources (a test, a retransfer). OPEN: break, a programmed neutral position, make. IN_PHASE:
	 * open, but only once the in-phase monitor sees the two sources within its window, with no programmed delay. CLOSED: make before
	 * break, both closed at most 100 ms, only when the sync check passes. Leaving a dead source is always an open transition.
	 */
	public enum Transition {
		OPEN("open transition"), IN_PHASE("open transition, in-phase monitor"), CLOSED("closed transition");

		public final String label;

		Transition(String label) {
			this.label = label;
		}
	}

	/** The controller's timers: FACTORY, the real defaults (TDEN 30 min, TDEC 5 min); SHORT, the scaled set built in 10d3. */
	public enum Timers {
		FACTORY, SHORT;

		double tdes() {
			return this == FACTORY ? Tuning.ATS_FACTORY_TDES : Tuning.ATS_TDES_SECONDS;
		}

		double tdne() {
			return this == FACTORY ? Tuning.ATS_FACTORY_TDNE : Tuning.ATS_TDNE_SECONDS;
		}

		double tden() {
			return this == FACTORY ? Tuning.ATS_FACTORY_TDEN : Tuning.ATS_TDEN_SECONDS;
		}

		double tdec() {
			return this == FACTORY ? Tuning.ATS_FACTORY_TDEC : Tuning.ATS_TDEC_SECONDS;
		}
	}

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private Transition transition = Transition.OPEN;
	private Timers timers = Timers.FACTORY;
	/** The neutral position's length for the transfer under way. */
	private double neutralSeconds = Tuning.ATS_NEUTRAL_SECONDS;
	/** Normal to emergency, in degrees (phase A to phase A), as the in-phase monitor and sync check see it. */
	private double angle;
	/** A slip between the two sources the solver cannot make (it gives a connected group one frequency): a test hook. */
	private double slipHz;
	private double slipDegrees;
	/** The longest make-before-break overlap so far, in seconds. */
	private double longestOverlap;
	private String lastTransfer = "";
	private boolean grown;
	private Stage stage = Stage.NORMAL;
	private double timer;
	private boolean test;
	private final int[] normal = new int[3];
	private final int[] emergency = new int[3];
	private int[] links = new int[0];
	private double amps;
	private double normalVolts;
	private double emergencyVolts;
	private double hertz;
	/** Set if the two sets of contacts were ever closed together: never, by construction; the game test reads it. */
	private boolean paralleled;

	public TransferSwitchBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TRANSFER_SWITCH_ENTITY, pos, state);
	}

	private Direction facing() {
		return getBlockState().getValue(TransferSwitchBlock.FACING);
	}

	public Stage stage() {
		return stage;
	}

	public boolean engineStart() {
		return stage.engineStart();
	}

	public boolean test() {
		return test;
	}

	public void setTest(boolean value) {
		test = value;
		setChanged();
	}

	/** True if the two sets of contacts were ever closed together outside a closed transition: never, by construction. */
	public boolean paralleled() {
		return paralleled;
	}

	public Transition transition() {
		return transition;
	}

	public void setTransition(Transition value) {
		transition = value;
		setChanged();
	}

	public void nextTransition() {
		setTransition(Transition.values()[(transition.ordinal() + 1) % Transition.values().length]);
	}

	public Timers timers() {
		return timers;
	}

	public void setTimers(Timers value) {
		timers = value;
		setChanged();
	}

	public void nextTimers() {
		setTimers(timers == Timers.FACTORY ? Timers.SHORT : Timers.FACTORY);
	}

	/** Test hook: the two sources drift apart at {@code hz}, as two separate machines would; the solver gives them one frequency. */
	public void setSlipForTest(double hz) {
		slipHz = hz;
		slipDegrees = 0;
	}

	public double angle() {
		return angle;
	}

	public double longestOverlap() {
		return longestOverlap;
	}

	public String lastTransfer() {
		return lastTransfer;
	}

	public double amps() {
		return amps;
	}

	private BlockPos topCell() {
		return BigParts.cell(worldPosition, getBlockState(), 1);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && (cell.equals(topCell()) && (face == Direction.UP || face == facing().getOpposite())
				|| cell.equals(worldPosition) && face == Direction.DOWN);
	}

	@Override
	public void build(Circuit circuit) {
		boolean n = stage.normalClosed();
		boolean e = stage.emergencyClosed();
		paralleled |= n && e && !stage.overlap();
		links = new int[(n ? 3 : 0) + (e ? 3 : 0)];
		int used = 0;
		for (int k = 0; k < 3; k++) {
			normal[k] = circuit.node();
			emergency[k] = circuit.node();
			int load = circuit.node();
			circuit.port(topCell(), Direction.UP, PHASES[k], Voltage.INDUSTRIAL, normal[k], Tuning.CONTACT_OHMS / 2, false);
			circuit.port(topCell(), facing().getOpposite(), PHASES[k], Voltage.INDUSTRIAL, emergency[k], Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, Direction.DOWN, PHASES[k], Voltage.INDUSTRIAL, load, Tuning.CONTACT_OHMS / 2, false);
			if (n) {
				links[used++] = circuit.resistor(normal[k], load, Tuning.BREAKER_CLOSED_OHMS);
			}
			if (e) {
				links[used++] = circuit.resistor(emergency[k], load, Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		// a solid neutral: normal, emergency and load neutrals are one; the ground runs through
		for (Conductor role : new Conductor[] {Conductor.N, Conductor.G}) {
			int node = circuit.node();
			circuit.port(topCell(), Direction.UP, role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(topCell(), facing().getOpposite(), role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, Direction.DOWN, role, Voltage.INDUSTRIAL, node, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	/** The lowest phase voltage of a source, as a share of nominal. */
	private static double lowest(Circuit circuit, int[] nodes) {
		double low = Double.MAX_VALUE;
		for (int node : nodes) {
			low = Math.min(low, circuit.voltage(node));
		}
		return low / Voltage.INDUSTRIAL.nominal();
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		amps = most;
		double n = lowest(circuit, normal);
		double e = lowest(circuit, emergency);
		normalVolts = n * Voltage.INDUSTRIAL.lineToLine();
		emergencyVolts = e * Voltage.INDUSTRIAL.lineToLine();
		hertz = circuit.hertz();
		boolean testing = test || remote.auto() && plcTest;
		boolean normalFailed = testing || n < Tuning.ATS_DROPOUT;
		boolean normalRestored = !testing && n >= Tuning.ATS_PICKUP;
		boolean emergencyOk = e >= Tuning.ATS_PICKUP && hertz >= Tuning.ATS_EMERGENCY_HERTZ * ElectricalStandard.active().hertz();
		boolean normalLive = n >= Tuning.ATS_PICKUP;   // live, whether or not a test treats it as failed
		double dt = ticks / 20.0;
		slipDegrees = wrap(slipDegrees + 360.0 * slipHz * dt);
		angle = wrap(circuit.voltagePhasor(emergency[0]).degrees() - circuit.voltagePhasor(normal[0]).degrees() + slipDegrees);
		boolean bothLive = normalLive && emergencyOk;
		timer += dt;
		Stage was = stage;
		switch (stage) {
			case NORMAL -> {
				if (normalFailed) {
					go(Stage.TDES);
				}
			}
			case TDES -> {
				if (!normalFailed) {
					go(Stage.NORMAL);
				} else if (timer >= timers.tdes()) {
					go(Stage.START);
				}
			}
			case START -> {
				if (!normalFailed) {
					go(Stage.TDEC);   // normal came back before the transfer: cool the engine down
				} else if (emergencyOk) {
					go(Stage.TDNE);
				}
			}
			case TDNE -> {
				if (!emergencyOk) {
					go(Stage.START);
				} else if (timer >= timers.tdne() && !(remote.auto() && inhibit)) {
					transfer(bothLive, timer - timers.tdne(), Stage.TO_EMERGENCY, Stage.CLOSED_TO_EMERGENCY, "to emergency");
				}
			}
			case TO_EMERGENCY -> {
				if (timer >= neutralSeconds) {
					go(Stage.EMERGENCY);
				}
			}
			case CLOSED_TO_EMERGENCY -> {
				// the overlap was the solve just made with both closed: open normal now
				longestOverlap = Math.max(longestOverlap, dt);
				go(Stage.EMERGENCY);
			}
			case EMERGENCY -> {
				if (!emergencyOk && normalRestored) {
					neutralSeconds = Tuning.ATS_NEUTRAL_SECONDS;
				go(Stage.TO_NORMAL);   // the genset failed and normal is there: back at once
				} else if (normalRestored) {
					go(Stage.TDEN);
				}
			}
			case TDEN -> {
				if (!emergencyOk) {
					neutralSeconds = Tuning.ATS_NEUTRAL_SECONDS;
					go(Stage.TO_NORMAL);
				} else if (!normalRestored) {
					go(Stage.EMERGENCY);
				} else if (timer >= timers.tden()) {
					transfer(true, timer - timers.tden(), Stage.TO_NORMAL, Stage.CLOSED_TO_NORMAL, "to normal");
				}
			}
			case TO_NORMAL -> {
				if (timer >= neutralSeconds) {
					go(Stage.TDEC);
				}
			}
			case CLOSED_TO_NORMAL -> {
				longestOverlap = Math.max(longestOverlap, dt);
				go(Stage.TDEC);
			}
			case TDEC -> {
				if (normalFailed) {
					go(Stage.START);   // normal failed again while the engine ran on: it is still running
				} else if (timer >= timers.tdec()) {
					go(Stage.NORMAL);
				}
			}
		}
		if (stage != was && level instanceof ServerLevel server) {
			if (stage.normalClosed() != was.normalClosed() || stage.emergencyClosed() != was.emergencyClosed()) {
				server.playSound(null, worldPosition, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.2F, 0.6F);
				CircuitManager.markDirty(server);
			}
			if (stage.engineStart() != was.engineStart()) {
				for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
					server.updateNeighborsAt(cell, getBlockState().getBlock());
				}
			}
		}
	}

	/**
	 * Starts a transfer once its delay has run: between two live sources by the set transition (waiting for the in-phase window or the
	 * sync check, and falling back to open after {@link Tuning#ATS_SYNC_WAIT_SECONDS}); away from a dead source always open.
	 */
	private void transfer(boolean bothLive, double waited, Stage open, Stage closed, String where) {
		boolean timedOut = waited >= Tuning.ATS_SYNC_WAIT_SECONDS;
		if (!bothLive || transition == Transition.OPEN || timedOut) {
			neutralSeconds = Tuning.ATS_NEUTRAL_SECONDS;
			lastTransfer = where + ", open transition" + (bothLive && transition != Transition.OPEN ? " (no sync in " + Math.round(Tuning.ATS_SYNC_WAIT_SECONDS) + " s)" : "");
			go(open);
		} else if (transition == Transition.IN_PHASE && Math.abs(angle) <= Tuning.ATS_IN_PHASE_DEGREES && Math.abs(slipHz) <= Tuning.ATS_IN_PHASE_MAX_HZ) {
			neutralSeconds = Tuning.ATS_FAST_NEUTRAL_SECONDS;
			lastTransfer = String.format("%s, in phase (%.0f degrees apart)", where, angle);
			go(open);
		} else if (transition == Transition.CLOSED && Math.abs(angle) <= Tuning.ATS_CLOSED_DEGREES && Math.abs(slipHz) <= Tuning.ATS_CLOSED_HZ
				&& Math.abs(normalVolts - emergencyVolts) <= Tuning.ATS_CLOSED_VOLTS * Voltage.INDUSTRIAL.lineToLine()) {
			lastTransfer = String.format("%s, closed transition (%.0f degrees apart)", where, angle);
			go(closed);
		}
	}

	private static double wrap(double degrees) {
		return ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
	}

	private void go(Stage next) {
		stage = next;
		timer = 0;
		setChanged();
	}

	@Override
	public Component inspect() {
		String on = switch (stage) {
			case NORMAL, TDES, START, TDNE, TDEC -> String.format("on NORMAL, %,.0f A", amps);
			case EMERGENCY, TDEN -> String.format("on EMERGENCY, %,.0f A", amps);
			case TO_EMERGENCY, TO_NORMAL -> "in the NEUTRAL position (open transition)";
			case CLOSED_TO_EMERGENCY, CLOSED_TO_NORMAL -> "on BOTH (closed transition, make before break)";
		};
		String timing = switch (stage) {
			case TDES -> String.format("; normal failed: engine start in %.0f s", Math.max(0, Tuning.ATS_TDES_SECONDS - timer));
			case START -> "; waiting for emergency to be acceptable";
			case TDNE -> timer >= timers.tdne() ? waitingFor() : String.format("; transfer to emergency in %s", clock(timers.tdne() - timer));
			case TDEN -> timer >= timers.tden() ? waitingFor() : String.format("; normal restored: retransfer in %s", clock(timers.tden() - timer));
			case TDEC -> String.format("; engine cooldown, %s left", clock(timers.tdec() - timer));
			default -> "";
		};
		return Component.literal(String.format("Transfer switch %s, %d A, %s, %s timers: %s%s; normal %,.0f V, emergency %,.0f V, %.1f Hz%s; engine start %s%s%s",
				Voltage.INDUSTRIAL.label(), Tuning.ATS_AMPS, transition.label, timers.name().toLowerCase(java.util.Locale.ROOT), on, timing, normalVolts, emergencyVolts, hertz,
				normalVolts > 0.5 * Voltage.INDUSTRIAL.lineToLine() && emergencyVolts > 0.5 * Voltage.INDUSTRIAL.lineToLine() ? String.format(", %.0f degrees apart", angle) : "", engineStart() ? "ON" : "off", lastTransfer.isEmpty() ? "" : "; last transfer " + lastTransfer,
				test ? "; TEST: normal treated as failed (sneak and right-click to end it)" : ""));
	}

	private String waitingFor() {
		return transition == Transition.CLOSED ? String.format("; waiting for sync (within %.0f degrees, %.0f%% voltage)", Tuning.ATS_CLOSED_DEGREES, Tuning.ATS_CLOSED_VOLTS * 100)
				: transition == Transition.IN_PHASE ? String.format("; in-phase monitor: waiting for the sources within %.0f degrees", Tuning.ATS_IN_PHASE_DEGREES) : "";
	}

	/** "22 s", "4 min 10 s". */
	private static String clock(double seconds) {
		long s = Math.max(0, Math.round(seconds));
		return s >= 60 ? String.format("%d min %d s", s / 60, s % 60) : s + " s";
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		remote.save(output);
		output.putBoolean("grown", grown);
		output.putString("stage", stage.name());
		output.putDouble("timer", timer);
		output.putBoolean("test", test);
		output.putString("transition", transition.name());
		output.putString("timers", timers.name());
		output.putDouble("neutral", neutralSeconds);
		output.putString("lastTransfer", lastTransfer);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		grown = input.getBooleanOr("grown", false);
		stage = Stage.NORMAL;
		for (Stage s : Stage.values()) {
			if (s.name().equals(input.getStringOr("stage", ""))) {
				stage = s;
			}
		}
		timer = input.getDoubleOr("timer", 0);
		test = input.getBooleanOr("test", false);
		transition = Transition.OPEN;
		for (Transition t : Transition.values()) {
			if (t.name().equals(input.getStringOr("transition", ""))) {
				transition = t;
			}
		}
		// a switch saved before the timer sets keeps the scaled timers it was built with
		timers = "FACTORY".equals(input.getStringOr("timers", "SHORT")) ? Timers.FACTORY : Timers.SHORT;
		neutralSeconds = input.getDoubleOr("neutral", Tuning.ATS_NEUTRAL_SECONDS);
		lastTransfer = input.getStringOr("lastTransfer", "");
	}
}
