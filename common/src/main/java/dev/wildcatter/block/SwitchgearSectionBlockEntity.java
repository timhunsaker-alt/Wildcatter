package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.BusProtection;
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
 * <li>50 trips it on the solve the fault appears in; 51 runs the IEEE moderately inverse curve after it.</li>
 * </ul>
 */
public class SwitchgearSectionBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	public enum Rack { CONNECTED, TEST, DISCONNECTED }

	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private boolean grown;
	private boolean closed;
	private boolean tripped;
	private Rack rack = Rack.CONNECTED;
	private int frameIndex;
	private int pickupIndex = Tuning.RELAY_PICKUP_START;
	private String lastTrip = "";
	private final BusProtection.TimeElement element = new BusProtection.TimeElement(false);
	private int[] links = new int[0];
	private double amps;

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
		return Math.min(Tuning.RELAY_PICKUP_STEPS[Math.min(pickupIndex, Tuning.RELAY_PICKUP_STEPS.length - 1)], frame());
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

	public void nextPickup() {
		do {
			pickupIndex = (pickupIndex + 1) % Tuning.RELAY_PICKUP_STEPS.length;
		} while (Tuning.RELAY_PICKUP_STEPS[pickupIndex] > frame());
		element.reset();
		setChanged();
	}

	/** Sets the 51 pickup to the step nearest {@code target} amps (the game test). */
	public void setPickup(int target) {
		for (int k = 0; k < Tuning.RELAY_PICKUP_STEPS.length; k++) {
			if (Tuning.RELAY_PICKUP_STEPS[k] <= target) {
				pickupIndex = k;
			}
		}
		element.reset();
		setChanged();
	}

	/** Racks the truck one position on: only while the breaker is open. False if it is closed. */
	public boolean rack() {
		if (closed) {
			return false;
		}
		rack = Rack.values()[(rack.ordinal() + 1) % Rack.values().length];
		setChanged();
		return true;
	}

	/** Right-click: open a closed breaker; close an open one (after a trip, reset the relay too). False if it cannot close. */
	public boolean operate() {
		if (closed) {
			closed = false;
			setChanged();
			return true;
		}
		if (rack == Rack.DISCONNECTED) {
			return false;
		}
		tripped = false;
		element.reset();
		closed = true;
		setChanged();
		return true;
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

	// ---- the circuit --------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && face == terminalFace() && cell.equals(terminalCell());
	}

	@Override
	public void build(Circuit circuit) {
		boolean carries = closed && rack == Rack.CONNECTED && mains(lineup()) == 1;
		links = carries ? new int[3] : new int[0];
		BlockPos busCell = cell(0, 1);
		Direction left = facing().getClockWise();
		int busNeutral = circuit.node();
		int busGround = circuit.node();
		for (int k = 0; k < 3; k++) {
			int bus = circuit.node();
			int line = circuit.node();
			for (Direction side : new Direction[] {left, left.getOpposite()}) {
				circuit.switchgearBusPort(busCell, side, PHASES[k], bus);
			}
			circuit.port(terminalCell(), terminalFace(), PHASES[k], Voltage.MEDIUM, line, Tuning.CONTACT_OHMS / 2, false);
			if (carries) {
				links[k] = circuit.resistor(bus, line, Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		// the neutral and ground run through the lineup and out of the cable compartment, unswitched
		for (Direction side : new Direction[] {left, left.getOpposite()}) {
			circuit.switchgearBusPort(busCell, side, Conductor.N, busNeutral);
			circuit.switchgearBusPort(busCell, side, Conductor.G, busGround);
		}
		circuit.port(terminalCell(), terminalFace(), Conductor.N, Voltage.MEDIUM, busNeutral, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(terminalCell(), terminalFace(), Conductor.G, Voltage.MEDIUM, busGround, Tuning.CONTACT_OHMS / 2, false);
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		return most;
	}

	/** 50: instantaneous, at 8 x the pickup. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (links.length == 0) {
			return false;
		}
		double now = mostAmps(circuit);
		if (now > Tuning.RELAY_50_OF_PICKUP * pickup()) {
			trip(String.format("50 instantaneous, %,.0f A", now));
			return true;
		}
		return false;
	}

	/** 51: moderately inverse, time dial 1. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = mostAmps(circuit);
		if (links.length == 0) {
			element.reset();
			return;
		}
		if (element.step(amps, pickup(), Tuning.RELAY_TIME_DIAL, ticks / 20.0)) {
			trip(String.format("51 time overcurrent, %,.0f A on a %,d A pickup", amps, pickup()));
			if (level != null) {
				CircuitManager.markDirty(level);
			}
		}
	}

	private void trip(String why) {
		closed = false;
		tripped = true;
		amps = 0;
		lastTrip = why;
		element.reset();
		setChanged();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.HV_BREAKER, SoundSource.BLOCKS, 1.2F, 1.1F);
			CircuitManager.markDirty(server);
		}
	}

	// ---- the readout --------------------------------------------------------------------------------------------------------------
	@Override
	public Component inspect() {
		List<SwitchgearSectionBlockEntity> lineup = lineup();
		int mains = mains(lineup);
		String head = String.format("%s switchgear, %s %d of %d (%,d A frame, vacuum): ", Voltage.MEDIUM.label(), isMain() ? "MAIN" : "FEEDER",
				lineup.indexOf(this) + 1, lineup.size(), frame());
		String state;
		if (mains != 1) {
			state = (mains == 0 ? "NO MAIN: a lineup needs one main section" : "TWO MAINS: a lineup takes only one main section") + "; the breaker carries nothing";
		} else if (tripped) {
			state = "TRIPPED (" + lastTrip + ")" + (rack == Rack.CONNECTED ? "; right-click to reset and close"
					: rack == Rack.DISCONNECTED ? ", DISCONNECTED (racked out: it cannot close)" : ", TEST (the primaries are parted: it carries nothing)");
		} else if (closed && rack == Rack.CONNECTED) {
			state = String.format("CLOSED, CONNECTED, %,.0f A", amps);
		} else if (rack == Rack.DISCONNECTED) {
			state = "OPEN, DISCONNECTED (racked out: it cannot close)";
		} else {
			state = (closed ? "CLOSED, " : "OPEN, ") + rack.name() + (rack == Rack.TEST ? " (the primaries are parted: it carries nothing)" : "");
		}
		StringBuilder text = new StringBuilder(head).append(state)
				.append(String.format("; 51 pickup %,d A (MI, TD %.0f), 50 at %,.0f A", pickup(), Tuning.RELAY_TIME_DIAL, Tuning.RELAY_50_OF_PICKUP * pickup()));
		if (isMain()) {
			text.append(String.format("; lineup: %d main%s, %d feeder%s", mains, mains == 1 ? "" : "s", lineup.size() - mains, lineup.size() - mains == 1 ? "" : "s"));
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
		output.putBoolean("grown", grown);
		output.putBoolean("closed", closed);
		output.putBoolean("tripped", tripped);
		output.putString("rack", rack.name());
		output.putInt("frame", frameIndex);
		output.putInt("pickup", pickupIndex);
		output.putString("lastTrip", lastTrip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		closed = input.getBooleanOr("closed", false);
		tripped = input.getBooleanOr("tripped", false);
		rack = Rack.CONNECTED;
		for (Rack r : Rack.values()) {
			if (r.name().equals(input.getStringOr("rack", ""))) {
				rack = r;
			}
		}
		frameIndex = Math.max(0, input.getIntOr("frame", 0));
		pickupIndex = Math.max(0, input.getIntOr("pickup", Tuning.RELAY_PICKUP_START));
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
