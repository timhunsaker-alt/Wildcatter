package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
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
 * The inside of a load centre or panelboard (docs/multiblocks/panelboards.md): the service terminals on its top and back faces, the
 * thermal-magnetic main breaker, and the bus behind it, which leaves the panel's faces in the wall's plane on the panel bus key for
 * its gutters. The neutral and ground run through the main unswitched.
 */
public class PanelBlockEntity extends BlockEntity implements CircuitPart, Protective, Inspectable, BigPartEntity {
	static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private boolean grown;
	private boolean closed = true;
	private boolean tripped;
	private double heat;
	private double amps;
	private String lastTrip = "";
	private int[] links = new int[0];

	public PanelBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PANEL_ENTITY, pos, state);
	}

	private PanelBlock block() {
		return (PanelBlock) getBlockState().getBlock();
	}

	public Voltage voltage() {
		return block().voltage();
	}

	public int spaces() {
		return block().spaces();
	}

	public int mainAmps() {
		return block().mainAmps();
	}

	private Direction facing() {
		return getBlockState().getValue(PanelBlock.FACING);
	}

	public boolean closed() {
		return closed;
	}

	public boolean tripped() {
		return tripped;
	}

	public double amps() {
		return amps;
	}

	public String lastTrip() {
		return lastTrip;
	}

	/** Works the main by hand; closing also resets a trip. */
	public void setMain(boolean on) {
		closed = on;
		if (on) {
			tripped = false;
			heat = 0;
		}
		show();
	}

	/** Draws the main's handle by its state on every cell: up ON, down OFF, centre TRIPPED (as the gutters' breakers). */
	private void show() {
		setChanged();
		if (level == null || !(getBlockState().getBlock() instanceof PanelBlock)) {
			return;
		}
		PanelGutterBlock.Slot want = tripped ? PanelGutterBlock.Slot.TRIPPED : closed ? PanelGutterBlock.Slot.ON : PanelGutterBlock.Slot.OFF;
		for (BlockPos cell : cells()) {
			BlockState state = level.getBlockState(cell);
			if (state.getBlock() instanceof PanelBlock && state.getValue(PanelBlock.MAIN) != want) {
				level.setBlock(cell, state.setValue(PanelBlock.MAIN, want), 3);
			}
		}
	}

	/** The main handle's drawn state: ON, OFF or TRIPPED. */
	public PanelGutterBlock.Slot handle() {
		return getBlockState().getValue(PanelBlock.MAIN);
	}

	/** The phases its bus stabs alternate over: A and B on a split-phase load centre, L alone under IEC, A, B and C on a panelboard. */
	public Conductor[] busPhases() {
		if (voltage() == Voltage.LOW) {
			return dev.wildcatter.energy.ElectricalStandard.active().splitPhase() ? new Conductor[] {Conductor.A, Conductor.B} : new Conductor[] {Conductor.A};
		}
		return PHASES;
	}

	private List<BlockPos> cells() {
		return BigParts.cells(worldPosition, getBlockState());
	}

	private BlockPos topCell() {
		List<BlockPos> cells = cells();
		return cells.get(cells.size() - 1);
	}

	/** The service lands on the top face of its top cell or the back face of its main cell. */
	private boolean feedFace(BlockPos cell, Direction face) {
		return cell.equals(topCell()) && face == Direction.UP || cell.equals(worldPosition) && face == facing().getOpposite();
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage() && feedFace(cell, face) && (role != Conductor.C || voltage() == Voltage.INDUSTRIAL);
	}

	/** True while a ground rod (the grounding electrode) touches the panel or is wired to it: it is then the service, bonded (NEC 250.24). */
	private boolean service;
	private int electrode = -1;
	private double electrodeAmps;

	public boolean service() {
		return service;
	}

	@Override
	public void build(Circuit circuit) {
		links = closed ? new int[3] : new int[0];
		int neutral = circuit.node();
		int ground = circuit.node();
		// the service: a grounding electrode at the panel makes it the service disconnect; its main bonding jumper ties the neutral bar
		// to the ground bar, and the grounding electrode conductor ties both to the rod. A panel with no rod is a sub-panel: N and G apart.
		service = level != null && cells().stream().anyMatch(cell -> dev.wildcatter.safety.Grounding.isGrounded(level, cell));
		electrode = -1;
		if (service) {
			electrode = circuit.resistor(neutral, ground, Tuning.CONTACT_OHMS);   // read: the bonding jumper's current
			circuit.earth(neutral, Tuning.GROUND_ELECTRODE_OHMS);
		}
		int[] bus = new int[3];
		for (int k = 0; k < 3; k++) {
			int feed = circuit.node();
			bus[k] = circuit.node();
			circuit.port(topCell(), Direction.UP, PHASES[k], voltage(), feed, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, facing().getOpposite(), PHASES[k], voltage(), feed, Tuning.CONTACT_OHMS / 2, false);
			if (closed) {
				links[k] = circuit.resistor(feed, bus[k], Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		for (Conductor role : new Conductor[] {Conductor.N, Conductor.G}) {
			int node = role == Conductor.N ? neutral : ground;
			circuit.port(topCell(), Direction.UP, role, voltage(), node, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, facing().getOpposite(), role, voltage(), node, Tuning.CONTACT_OHMS / 2, false);
		}
		// the bus leaves every face in the wall's plane that is not the panel's own
		List<BlockPos> cells = cells();
		for (BlockPos cell : cells) {
			for (Direction side : PanelChain.inPlane(facing())) {
				if (cells.contains(cell.relative(side))) {
					continue;
				}
				for (int k = 0; k < 3; k++) {
					circuit.panelBusPort(cell, side, PHASES[k], bus[k]);
				}
				circuit.panelBusPort(cell, side, Conductor.N, neutral);
				circuit.panelBusPort(cell, side, Conductor.G, ground);
			}
		}
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		return most;
	}

	/** The magnetic trip. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (links.length == 0) {
			return false;
		}
		double now = mostAmps(circuit);
		if (now > mainAmps() * Tuning.BREAKER_MAGNETIC_MULTIPLE) {
			trip(now, "short circuit");
			return true;
		}
		return false;
	}

	// ---- the protection race and the coordination study ----------------------------------------------------------------------------
	@Override
	public double clearingSeconds(Circuit circuit) {
		return links.length > 0 && mostAmps(circuit) > mainAmps() * Tuning.BREAKER_MAGNETIC_MULTIPLE ? Tuning.MAGNETIC_CLEAR_CYCLES / circuit.hertz() : Double.POSITIVE_INFINITY;
	}

	@Override
	public double instantPickupAmps() {
		return mainAmps() * Tuning.BREAKER_MAGNETIC_MULTIPLE;
	}

	@Override
	public String curveName() {
		return String.format("%d A main", mainAmps());
	}

	@Override
	public double clearingSeconds(double amps) {
		return dev.wildcatter.circuit.TimeCurrent.thermalMagnetic(curveName(), mainAmps(), Tuning.BREAKER_TRIP_SECONDS_AT_2X, Tuning.BREAKER_MAGNETIC_MULTIPLE,
				dev.wildcatter.energy.ElectricalStandard.active().hertz()).clearingSeconds(amps);
	}

	@Override
	public double ratingAmps() {
		return mainAmps();
	}

	@Override
	public double interruptingAmps() {
		return voltage() == Voltage.LOW ? Tuning.INTERRUPT_LOAD_CENTRE : Tuning.INTERRUPT_PANELBOARD;
	}

	@Override
	public List<Terminal> lineSide() {
		return List.of(new Terminal(topCell(), Direction.UP), new Terminal(worldPosition, facing().getOpposite()));
	}

	/** The thermal trip. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		electrodeAmps = electrode >= 0 ? circuit.current(electrode) : 0;
		amps = mostAmps(circuit);
		double ratio = amps / mainAmps();
		if (ratio > 1.0) {
			heat += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * Tuning.BREAKER_TRIP_SECONDS_AT_2X);
		} else {
			heat = Math.max(0, heat - ticks / (20.0 * Tuning.BREAKER_COOL_SECONDS));
		}
		if (heat >= 1.0 && closed) {
			trip(amps, "overload");
		}
	}

	/** Test hook: the main trips as on a fault. */
	public void tripForTest() {
		trip(amps, "test trip");
	}

	private void trip(double tripAmps, String reason) {
		closed = false;
		tripped = true;
		heat = 0;
		amps = 0;
		lastTrip = String.format("%s: %,.0f A on the %d A main", reason, tripAmps, mainAmps());
		show();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 0.8F);
			CircuitManager.markDirty(server);
		}
	}

	/** The poles of the breakers in its gutters, and how many breakers. */
	public int[] used() {
		int poles = 0;
		int breakers = 0;
		if (level != null) {
			for (BlockPos gutter : PanelChain.gutters(level, worldPosition)) {
				if (level.getBlockEntity(gutter) instanceof PanelGutterBlockEntity g && g.breaker() != null) {
					poles += g.breaker().poles();
					breakers++;
				}
			}
		}
		return new int[] {poles, breakers};
	}

	public String name() {
		return voltage() == Voltage.LOW ? "Load centre" : "Panelboard";
	}

	@Override
	public Component inspect() {
		String state = tripped ? "TRIPPED (" + lastTrip + "); right-click to reset after you fix the fault"
				: closed ? String.format("ON, %,.0f A", amps) + (heat > 0.05 ? ", getting warm " + Math.round(heat * 100) + "%" : "") : "OFF";
		int[] used = used();
		return Component.literal(String.format("%s %s, main %d A: %s; %d space%s of %d used (%d breaker%s)", name(), voltageLabel(), mainAmps(), state,
				used[0], used[0] == 1 ? "" : "s", spaces(), used[1], used[1] == 1 ? "" : "s") + grounding() + Coordination.readout(level, this));
	}

	/** The service's grounding: bonded to a rod, or not. */
	public String grounding() {
		return service ? String.format("; SERVICE: neutral bonded to ground and earthed by its ground rod (%.0f ohm), %.2f A on the bonding jumper", Tuning.GROUND_ELECTRODE_OHMS, electrodeAmps)
				: "; neutral not earthed here (no ground rod: a sub-panel, or an unearthed service)";
	}

	/** Amps through the main bonding jumper, neutral to ground: what comes home by the earth (the game test). */
	public double electrodeAmps() {
		return electrodeAmps;
	}

	/** "120/240 V" split-phase, "230 V" under IEC, "480Y/277 V" or "400Y/230 V". */
	public String voltageLabel() {
		if (voltage() == Voltage.LOW) {
			return dev.wildcatter.energy.ElectricalStandard.active().splitPhase()
					? String.format("%.0f/%.0f V", voltage().nominal(), 2 * voltage().nominal()) : voltage().label();
		}
		return String.format("%.0fY/%.0f V", voltage().lineToLine(), voltage().nominal());
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
		output.putBoolean("grown", grown);
		output.putBoolean("closed", closed);
		output.putBoolean("tripped", tripped);
		output.putDouble("heat", heat);
		output.putString("lastTrip", lastTrip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		closed = input.getBooleanOr("closed", true);
		tripped = input.getBooleanOr("tripped", false);
		heat = input.getDoubleOr("heat", 0);
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
