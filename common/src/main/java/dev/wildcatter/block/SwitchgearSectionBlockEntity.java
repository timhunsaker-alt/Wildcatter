package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.BusProtection;
import dev.wildcatter.circuit.RelaySettings;
import dev.wildcatter.circuit.TripCurve;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
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
 * The inside of a 4.16 kV switchgear section (docs/multiblocks/mv-switchgear.md): its share of the lineup's main bus, a three-pole
 * draw-out vacuum breaker from that bus to the section's cable terminal, and the relay in its door.
 * <ul>
 * <li>The bus joins the neighbouring sections through the side faces of its bottom middle cell, on a key only switchgear uses.</li>
 * <li>The cable terminal is the back face of the bottom back cell on a MAIN, the top face of the top back cell on a FEEDER.</li>
 * <li>The breaker carries current only closed and CONNECTED, in a lineup with exactly one main.</li>
 * <li>50 trips it on the solve the fault appears in; 51 runs its inverse-time curve after it. Pickup, curve, dial and 50 are set from
 * the relay door with the Power Meter ({@link RelaySettings}).</li>
 * <li>Racking takes time ({@link Tuning#RACK_SECONDS} a position) and can be done remotely from a PLC (RACK IN / RACK OUT).</li>
 * <li>50BF: a breaker that fails to open (above its interrupting rating, or a stuck mechanism) has its failure relay trip every other
 * breaker in the lineup after {@link Tuning#BF_SECONDS}, and 86BF locks them out.</li>
 * <li>87B: the main section carries the lineup's bus differential; grounds on any section's bus trip the whole lineup and 86B locks it
 * out until the main is reset.</li>
 * </ul>
 * Protection gap wave: docs/questions/protection-gaps.md.
 */
public class SwitchgearSectionBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, dev.wildcatter.control.ControlDevice, dev.wildcatter.control.Measurable,
		TripCurve, dev.wildcatter.safety.Groundable {
	/** Points 0, 1 (CLOSE, TRIP COIL) and 6, 7 (RACK IN, RACK OUT) are commands. */
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(8);

	public boolean remote() {
		return remote.auto();
	}

	@Override
	public String deviceName() {
		return "Switchgear section";
	}

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("CLOSE", true), new Point("TRIP COIL", true), new Point("CLOSED", false), new Point("TRIPPED", false), new Point("REMOTE", false),
				new Point("AMPS 4-20 mA, 150 %", false, Signal.MA), new Point("RACK IN", true), new Point("RACK OUT", true), new Point("CONNECTED", false), new Point("RACKING", false));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case 2 -> closed();
			case 3 -> tripped();
			case 4 -> remote.auto();
			case 8 -> rack == Rack.CONNECTED && rackTarget == null;
			case 9 -> rackTarget != null;
			default -> false;
		};
	}

	/** A relay's trip stays until a hand resets it: a close coil will not clear it. RACK IN and RACK OUT drive the remote racking motor. */
	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 6 || point == 7) {
			if (remote.command(point, on, gameTime)) {
				rackToward(point == 6 ? Rack.CONNECTED : Rack.DISCONNECTED);
			}
			return;
		}
		if (point > 1 || !remote.command(point, on, gameTime)) {
			return;
		}
		if ((point == 0 && !closed() && !tripped()) || (point == 1 && closed())) {
			operate();
			if (level != null) {
				CircuitManager.markDirty(level);
			}
		}
	}

	@Override
	public double readMa(int point) {
		return 4 + 16 * Math.min(1.25, amps() / (frame() * 1.5));
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle().replace("HAND", "LOCAL").replace("AUTO", "REMOTE");
	}

	@Override
	public double measure(Quantity quantity) {
		return quantity == Quantity.AMPS ? amps() : Double.NaN;
	}

	public enum Rack { CONNECTED, TEST, DISCONNECTED }

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private boolean grown;
	private boolean closed;
	private boolean tripped;
	private Rack rack = Rack.CONNECTED;
	/** The position the truck is being racked to, or null while it stands still. */
	private Rack rackTarget;
	private double rackLeft;
	private int frameIndex;
	private final RelaySettings settings = new RelaySettings();
	private String lastTrip = "";
	private final RelaySettings.Element element = new RelaySettings.Element();
	private int[] links = new int[0];
	private double amps;
	// 50BF
	private boolean stuck;
	private boolean failed;
	private double failedFor;
	// 87B on the bus (the main carries the relay; any section can carry grounds on its bus)
	private boolean grounded;
	private int[] clamps = new int[0];
	private boolean lockedOut;
	private String lockout = "";
	private double busVolts;

	public SwitchgearSectionBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SWITCHGEAR_ENTITY, pos, state);
	}

	public boolean isMain() {
		return getBlockState().getBlock() instanceof SwitchgearSectionBlock block && block.isMain();
	}

	private Direction facing() {
		return getBlockState().getValue(SwitchgearSectionBlock.FACING);
	}

	public int frame() {
		return Tuning.SWITCHGEAR_FRAME_AMPS[Math.min(frameIndex, Tuning.SWITCHGEAR_FRAME_AMPS.length - 1)];
	}

	public int pickup() {
		return settings.pickup(frame());
	}

	public RelaySettings settings() {
		return settings;
	}

	public boolean closed() {
		return closed;
	}

	public boolean tripped() {
		return tripped;
	}

	public Rack racked() {
		return rack;
	}

	/** True while the truck is moving between positions. */
	public boolean racking() {
		return rackTarget != null;
	}

	public double amps() {
		return amps;
	}

	public String lastTrip() {
		return lastTrip;
	}

	public void nextFrame() {
		frameIndex = (frameIndex + 1) % Tuning.SWITCHGEAR_FRAME_AMPS.length;
		setChanged();
	}

	/** Sneak and right-click with the Power Meter: steps the setting the relay's cursor is on. */
	public void nextPickup() {
		settings.step(frame());
		element.reset();
		setChanged();
	}

	/** Right-click with the Power Meter on the relay door: the cursor moves to the next setting. */
	public void nextSetting() {
		settings.nextField();
		setChanged();
	}

	/** Sets the 51 pickup to the step nearest {@code target} amps (the game test). */
	public void setPickup(int target) {
		settings.setPickup(target);
		element.reset();
		setChanged();
	}

	/** A test hook: the breaker's mechanism sticks, so its next trip fails to open it (a real cause of breaker failure). */
	public void stickMechanism(boolean on) {
		stuck = on;
		setChanged();
	}

	/** The interrupting rating of a 5 kV, 350 MVA class vacuum breaker. */
	public static double interruptingAmps() {
		return Tuning.SWITCHGEAR_INTERRUPTING_AMPS;
	}

	/** Racks the truck one position on (CONNECTED, TEST, DISCONNECTED, round again): only while the breaker is open and the truck still. */
	public boolean rack() {
		if (closed || rackTarget != null) {
			return false;
		}
		return startRacking(Rack.values()[(rack.ordinal() + 1) % Rack.values().length]);
	}

	/** The remote racking motor: one position toward {@code end}. */
	private void rackToward(Rack end) {
		if (closed || rackTarget != null || rack == end) {
			return;
		}
		startRacking(Rack.values()[rack.ordinal() + (end.ordinal() > rack.ordinal() ? 1 : -1)]);
	}

	private boolean startRacking(Rack to) {
		rackTarget = to;
		rackLeft = Tuning.RACK_SECONDS;
		setChanged();
		return true;
	}

	/** A test hook: the truck reaches the position it is being racked to now. */
	public void finishRacking() {
		if (rackTarget != null) {
			rack = rackTarget;
			rackTarget = null;
			rackLeft = 0;
			setChanged();
		}
	}

	/** Right-click: open a closed breaker; close an open one (after a trip, reset the relay too). False if it cannot close. */
	public boolean operate() {
		if (closed) {
			closed = false;
			failed = false;   // opened by hand: the manual trip on a stuck mechanism
			stuck = false;
			setChanged();
			return true;
		}
		if (rack == Rack.DISCONNECTED || rackTarget != null) {
			return false;
		}
		SwitchgearSectionBlockEntity main = main();
		if (isMain() && lockedOut) {
			lockedOut = false;   // the main's reset releases 86B / 86BF
			lockout = "";
		} else if (main != null && main.lockedOut) {
			return false;
		}
		tripped = false;
		element.reset();
		closed = true;
		setChanged();
		return true;
	}

	/** A relay outside the lineup (87T on the transformer feeding it) opens this breaker. */
	public void tripByRelay(String why) {
		if (closed) {
			trip(why);
		}
	}

	/** That relay's reset closes it again, if it is connected and was opened. */
	public void closeByRelay() {
		if (!closed && rack == Rack.CONNECTED && rackTarget == null) {
			tripped = false;
			element.reset();
			closed = true;
			setChanged();
			if (level != null) {
				CircuitManager.markDirty(level);
			}
		}
	}

	public boolean lockedOut() {
		SwitchgearSectionBlockEntity main = main();
		return main != null && main.lockedOut;
	}

	// ---- the lineup -------------------------------------------------------------------------------------------------------------
	/** The sections joined to this one side by side, facing the same way, numbered from the left as you face the front. */
	public List<SwitchgearSectionBlockEntity> lineup() {
		List<SwitchgearSectionBlockEntity> sections = new ArrayList<>();
		if (level == null) {
			sections.add(this);
			return sections;
		}
		Direction left = facing().getClockWise();
		BlockPos at = worldPosition;
		while (sections.size() < 64 && level.getBlockEntity(at.relative(left)) instanceof SwitchgearSectionBlockEntity other && other.facing() == facing()) {
			at = at.relative(left);
		}
		while (sections.size() < 64 && level.getBlockEntity(at) instanceof SwitchgearSectionBlockEntity section && section.facing() == facing()) {
			sections.add(section);
			at = at.relative(left.getOpposite());
		}
		return sections;
	}

	private static int mains(List<SwitchgearSectionBlockEntity> lineup) {
		int mains = 0;
		for (SwitchgearSectionBlockEntity section : lineup) {
			mains += section.isMain() ? 1 : 0;
		}
		return mains;
	}

	/** The lineup's one main, or null with none or two. */
	private SwitchgearSectionBlockEntity main() {
		List<SwitchgearSectionBlockEntity> lineup = lineup();
		if (mains(lineup) != 1) {
			return null;
		}
		for (SwitchgearSectionBlockEntity section : lineup) {
			if (section.isMain()) {
				return section;
			}
		}
		return null;
	}

	private BlockPos cell(int dy, int dz) {
		return BigParts.cell(worldPosition, getBlockState(), SwitchgearSectionBlock.FOOTPRINT.pieceAt(0, dy, dz, 0));
	}

	/** The cable terminal: the back of the bottom back cell on a main, the top of the top back cell on a feeder. */
	private BlockPos terminalCell() {
		return isMain() ? cell(0, 2) : cell(2, 2);
	}

	private Direction terminalFace() {
		return isMain() ? facing().getOpposite() : Direction.UP;
	}

	// ---- grounds on the bus ---------------------------------------------------------------------------------------------------------
	@Override
	public boolean grounded() {
		return grounded;
	}

	@Override
	public void setGrounded(boolean on) {
		if (on && !grounded && level instanceof ServerLevel server && busVolts > 50) {
			server.playSound(null, worldPosition, WildcatterSounds.FLASHOVER, SoundSource.BLOCKS, 1.5F, 1.0F);
			BlockPos bus = cell(0, 1);
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, bus.getX() + 0.5, bus.getY() + 0.5, bus.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.3);
		}
		grounded = on;
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	@Override
	public String groundedMessage() {
		return "Grounding set: grounds applied to the lineup's main bus, A, B and C to ground";
	}

	private double groundAmps(Circuit circuit) {
		double most = 0;
		for (int clamp : clamps) {
			most = Math.max(most, circuit.current(clamp));
		}
		return most;
	}

	// ---- the circuit --------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && face == terminalFace() && cell.equals(terminalCell());
	}

	private int busA = -1;

	@Override
	public void build(Circuit circuit) {
		boolean carries = closed && rack == Rack.CONNECTED && rackTarget == null && mains(lineup()) == 1;
		links = carries ? new int[3] : new int[0];
		BlockPos busCell = cell(0, 1);
		Direction left = facing().getClockWise();
		int busNeutral = circuit.node();
		int busGround = circuit.node();
		int[] bus = new int[3];
		for (int k = 0; k < 3; k++) {
			bus[k] = circuit.node();
			int line = circuit.node();
			for (Direction side : new Direction[] {left, left.getOpposite()}) {
				circuit.switchgearBusPort(busCell, side, PHASES[k], bus[k]);
			}
			circuit.port(terminalCell(), terminalFace(), PHASES[k], Voltage.MEDIUM, line, Tuning.CONTACT_OHMS / 2, false);
			if (carries) {
				links[k] = circuit.resistor(bus[k], line, Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		busA = bus[0];
		// the neutral and ground run through the lineup and out of the cable compartment, unswitched
		for (Direction side : new Direction[] {left, left.getOpposite()}) {
			circuit.switchgearBusPort(busCell, side, Conductor.N, busNeutral);
			circuit.switchgearBusPort(busCell, side, Conductor.G, busGround);
		}
		circuit.port(terminalCell(), terminalFace(), Conductor.N, Voltage.MEDIUM, busNeutral, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(terminalCell(), terminalFace(), Conductor.G, Voltage.MEDIUM, busGround, Tuning.CONTACT_OHMS / 2, false);
		clamps = new int[0];
		if (grounded) {
			clamps = new int[] {circuit.resistor(bus[0], busGround, Tuning.GROUNDING_SET_OHMS), circuit.resistor(bus[1], busGround, Tuning.GROUNDING_SET_OHMS),
					circuit.resistor(bus[2], busGround, Tuning.GROUNDING_SET_OHMS)};
		}
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		return most;
	}

	/** 87B (on the main), 50BF, then 50: all on the solve the fault appears in. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (isMain() && !lockedOut && busDifferential(circuit)) {
			return true;
		}
		if (links.length == 0) {
			return false;
		}
		double now = mostAmps(circuit);
		if (failed) {
			// 50BF: its current detector still sees current after the failure timer: 86BF trips every other breaker in the lineup
			if (now > Tuning.BF_DETECTOR_AMPS && failedFor >= Tuning.BF_SECONDS) {
				return breakerFailure(now);
			}
			return false;
		}
		if (settings.instantaneous() > 0 && now > settings.instantaneous() * pickup()) {
			trip(String.format("50 instantaneous, %,.0f A", now), now);
			return !failed;
		}
		return false;
	}

	/** The main's 87B: the current into grounds on the lineup's bus against its percentage restraint. */
	private boolean busDifferential(Circuit circuit) {
		List<SwitchgearSectionBlockEntity> lineup = lineup();
		if (mains(lineup) != 1) {
			return false;
		}
		double fault = 0;
		double boundary = 0;
		double largest = 0;
		for (SwitchgearSectionBlockEntity section : lineup) {
			fault = Math.max(fault, section.groundAmps(circuit));
			boundary += section.mostAmps(circuit);
			largest = Math.max(largest, section.frame());
		}
		if (!BusProtection.differentialOperates(fault, boundary, largest)) {
			return false;
		}
		String why = String.format("87B BUS DIFFERENTIAL TRIP: %,.0f A in the bus (pickup %,.0f A)", fault, BusProtection.differentialPickup(boundary, largest));
		for (SwitchgearSectionBlockEntity section : lineup) {
			if (section.closed) {
				section.trip("87B bus differential, 86B lockout");
			}
		}
		lockedOut = true;
		lockout = why;
		setChanged();
		return true;
	}

	/** 50BF: this breaker failed to open; every other breaker on the bus opens and 86BF holds them. */
	private boolean breakerFailure(double now) {
		List<SwitchgearSectionBlockEntity> lineup = lineup();
		SwitchgearSectionBlockEntity main = main();
		for (SwitchgearSectionBlockEntity section : lineup) {
			if (section != this && section.closed) {
				section.trip("50BF: section " + (lineup.indexOf(this) + 1) + " failed to open, 86BF lockout");
			}
		}
		if (main != null) {
			main.lockedOut = true;
			main.lockout = String.format("50BF BREAKER FAILURE: section %d failed to open on %,.0f A", lineup.indexOf(this) + 1, now);
			main.setChanged();
		}
		failedFor = 0;
		lastTrip = lastTrip + "; FAILED TO OPEN, cleared by 50BF";
		setChanged();
		return true;
	}

	/** 51 after the solve, and the racking motor or crank. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		double dt = ticks / 20.0;
		if (rackTarget != null) {
			rackLeft -= dt;
			if (rackLeft <= 0) {
				finishRacking();
			}
		}
		busVolts = busA >= 0 ? circuit.voltage(busA) : 0;
		amps = mostAmps(circuit);
		if (failed) {
			failedFor += dt;
			if (amps <= Tuning.BF_DETECTOR_AMPS) {
				failedFor = 0;   // the current went away (the source opened): the failure relay drops out
			}
			return;
		}
		if (links.length == 0) {
			element.reset();
			return;
		}
		if (element.step(amps, pickup(), settings.curve(), settings.dial(), dt)) {
			trip(String.format("51 time overcurrent, %,.0f A on a %,d A pickup", amps, pickup()), amps);
			if (level != null) {
				CircuitManager.markDirty(level);
			}
		}
	}

	private void trip(String why) {
		trip(why, 0);
	}

	/** The trip coil: the breaker opens, unless its mechanism is stuck or {@code interrupting} is beyond its rating (then 50BF starts). */
	private void trip(String why, double interrupting) {
		tripped = true;
		lastTrip = why;
		element.reset();
		if (stuck || interrupting > Tuning.SWITCHGEAR_INTERRUPTING_AMPS) {
			if (!failed) {
				failed = true;
				failedFor = 0;
				lastTrip = why + (stuck ? "; the mechanism stuck" : String.format("; beyond its %,.0f A interrupting rating", Tuning.SWITCHGEAR_INTERRUPTING_AMPS));
			}
			setChanged();
			return;
		}
		closed = false;
		amps = 0;
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.HV_BREAKER, SoundSource.BLOCKS, 1.2F, 1.1F);
			CircuitManager.markDirty(server);
		}
	}

	// ---- the coordination curve -------------------------------------------------------------------------------------------------------
	@Override
	public String curveName() {
		return String.format("%s switchgear %s relay (%s, TD %s, %,d A)", Voltage.MEDIUM.label(), isMain() ? "main" : "feeder", settings.curve().name(),
				settings.dial() == Math.rint(settings.dial()) ? String.format("%.0f", settings.dial()) : String.valueOf(settings.dial()), pickup());
	}

	@Override
	public double clearingSeconds(double current) {
		return settings.secondsAt(current, frame()) + Tuning.BREAKER_INTERRUPT_SECONDS;
	}

	// ---- the readout --------------------------------------------------------------------------------------------------------------
	@Override
	public Component inspect() {
		List<SwitchgearSectionBlockEntity> lineup = lineup();
		int mains = mains(lineup);
		String head = String.format("%s switchgear, %s %d of %d (%,d A frame, vacuum): ", Voltage.MEDIUM.label(), isMain() ? "MAIN" : "FEEDER",
				lineup.indexOf(this) + 1, lineup.size(), frame());
		String state;
		SwitchgearSectionBlockEntity main = main();
		if (mains != 1) {
			state = (mains == 0 ? "NO MAIN: a lineup needs one main section" : "TWO MAINS: a lineup takes only one main section") + "; the breaker carries nothing";
		} else if (failed && closed) {
			state = "FAILED TO OPEN (" + lastTrip + "); right-click to open it by hand";
		} else if (tripped) {
			boolean held = main != null && main.lockedOut;
			state = "TRIPPED (" + lastTrip + ")" + (rack == Rack.CONNECTED ? (held && !isMain() ? "; LOCKED OUT: reset the main first" : "; right-click to reset and close")
					: rack == Rack.DISCONNECTED ? ", DISCONNECTED (racked out: it cannot close)" : ", TEST (the primaries are parted: it carries nothing)");
		} else if (closed && rack == Rack.CONNECTED && rackTarget == null) {
			state = String.format("CLOSED, CONNECTED, %,.0f A", amps);
		} else if (rackTarget != null) {
			state = String.format("OPEN, RACKING from %s to %s, %.0f s", rack.name(), rackTarget.name(), Math.max(0, rackLeft));
		} else if (rack == Rack.DISCONNECTED) {
			state = "OPEN, DISCONNECTED (racked out: it cannot close)";
		} else {
			state = (closed ? "CLOSED, " : "OPEN, ") + rack.name() + (rack == Rack.TEST ? " (the primaries are parted: it carries nothing)" : "");
		}
		StringBuilder text = new StringBuilder(head).append(state).append("; ").append(settings.describe(frame(), true));
		if (isMain()) {
			text.append(String.format("; lineup: %d main%s, %d feeder%s", mains, mains == 1 ? "" : "s", lineup.size() - mains, lineup.size() - mains == 1 ? "" : "s"));
			text.append(lockedOut ? "; " + lockout + ", LOCKED OUT; right-click the main to reset once the fault is cleared" : "; 87B ready");
		}
		if (grounded) {
			text.append("; bus GROUNDED (protective grounds applied)");
		}
		if (!tripped && !lastTrip.isEmpty()) {
			text.append("; last trip: ").append(lastTrip);
		}
		return Component.literal(text.toString());
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------
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
		output.putBoolean("closed", closed);
		output.putBoolean("tripped", tripped);
		output.putString("rack", rack.name());
		output.putString("rackTarget", rackTarget == null ? "" : rackTarget.name());
		output.putDouble("rackLeft", rackLeft);
		output.putInt("frame", frameIndex);
		int[] relay = settings.save();
		for (int k = 0; k < relay.length; k++) {
			output.putInt("relay" + k, relay[k]);
		}
		output.putString("lastTrip", lastTrip);
		output.putBoolean("failed", failed);
		output.putBoolean("groundedBus", grounded);
		output.putBoolean("lockedOut", lockedOut);
		output.putString("lockout", lockout);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		grown = input.getBooleanOr("grown", false);
		closed = input.getBooleanOr("closed", false);
		tripped = input.getBooleanOr("tripped", false);
		rack = Rack.CONNECTED;
		rackTarget = null;
		for (Rack r : Rack.values()) {
			if (r.name().equals(input.getStringOr("rack", ""))) {
				rack = r;
			}
			if (r.name().equals(input.getStringOr("rackTarget", ""))) {
				rackTarget = r;
			}
		}
		rackLeft = input.getDoubleOr("rackLeft", 0);
		frameIndex = Math.max(0, input.getIntOr("frame", 0));
		int[] relay = new int[6];
		int[] defaults = new RelaySettings().save();
		for (int k = 0; k < relay.length; k++) {
			relay[k] = input.getIntOr("relay" + k, defaults[k]);
		}
		relay[0] = input.getIntOr("relay0", input.getIntOr("pickup", Tuning.RELAY_PICKUP_START));   // worlds before the gap wave saved only the pickup
		settings.load(relay);
		lastTrip = input.getStringOr("lastTrip", "");
		failed = input.getBooleanOr("failed", false);
		grounded = input.getBooleanOr("groundedBus", false);
		lockedOut = input.getBooleanOr("lockedOut", false);
		lockout = input.getStringOr("lockout", "");
	}
}
