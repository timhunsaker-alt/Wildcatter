package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.BranchBreakerItem;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.solver.Complex;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * One branch space of a panel (docs/multiblocks/panelboards.md): its share of the panel's bus, which it passes on to the gutters beside
 * it, and the branch breaker clipped into it.
 * <ul>
 * <li>Its place in the chain sets its bus stab: A, B, A, B on a split-phase load centre, A, B, C on a panelboard.</li>
 * <li>The breaker's hot pole(s), the neutral and the ground leave the front face.</li>
 * <li>Thermal-magnetic, as the one-block breaker; a GFCI also trips on the difference between its hot and neutral currents.</li>
 * </ul>
 */
public class PanelGutterBlockEntity extends BlockEntity implements CircuitPart, Protective, Inspectable, BigPartEntity {
	private static final Conductor[] ROLES = {Conductor.A, Conductor.B, Conductor.C};

	private boolean grown;
	private @Nullable BranchBreakerItem breaker;
	private boolean closed;
	private boolean tripped;
	private double heat;
	private String lastTrip = "";
	private double amps;
	private double imbalance;
	private double volts;
	private int[] links = new int[0];
	private int neutralLink = -1;
	private int[] hot = new int[0];

	public PanelGutterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PANEL_GUTTER_ENTITY, pos, state);
	}

	private Direction facing() {
		return getBlockState().getValue(PanelGutterBlock.FACING);
	}

	public @Nullable BranchBreakerItem breaker() {
		return breaker;
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

	public double imbalance() {
		return imbalance;
	}

	/** Volts across a 2-pole breaker's poles: 240 V on a split-phase load centre. */
	public double volts() {
		return volts;
	}

	public String lastTrip() {
		return lastTrip;
	}

	// ---- its place in the chain ------------------------------------------------------------------------------------------------
	/** Where it stands: its panel, its space number (from 0) and whether its breaker fits in the panel's spaces. */
	public record Place(@Nullable PanelBlockEntity panel, int index, boolean inSpaces, String problem) {
	}

	public Place place() {
		if (level == null) {
			return new Place(null, -1, false, "not placed");
		}
		java.util.Set<BlockPos> panels = PanelChain.panels(level, worldPosition);
		if (panels.isEmpty()) {
			return new Place(null, -1, false, "NO PANEL: set it beside a load centre or panelboard, or beside a gutter of one, facing the same way");
		}
		if (panels.size() > 1) {
			return new Place(null, -1, false, "TWO PANELS: its chain reaches more than one panel");
		}
		if (!(level.getBlockEntity(panels.iterator().next()) instanceof PanelBlockEntity panel)) {
			return new Place(null, -1, false, "NO PANEL");
		}
		List<BlockPos> order = PanelChain.gutters(level, panel.getBlockPos());
		int index = order.indexOf(worldPosition);
		if (index < 0) {
			return new Place(panel, -1, false, "NOT IN THE CHAIN");
		}
		int poles = 0;
		for (int k = 0; k <= index; k++) {
			if (level.getBlockEntity(order.get(k)) instanceof PanelGutterBlockEntity g && g.breaker != null) {
				poles += g.breaker.poles();
			}
		}
		boolean fits = poles <= panel.spaces();
		return new Place(panel, index, fits, fits ? "" : "PANEL FULL: its breakers' poles would pass its " + panel.spaces() + " spaces");
	}

	/** The breaker's phases on the bus: its space's stab and the next ones. */
	private Conductor[] phases(PanelBlockEntity panel, int index) {
		Conductor[] stabs = panel.busPhases();
		Conductor[] out = new Conductor[breaker == null ? 0 : stabs.length == 1 ? 1 : breaker.poles()];
		for (int k = 0; k < out.length; k++) {
			out[k] = stabs[(index + k) % stabs.length];
		}
		return out;
	}

	/** Under IEC a load centre's 2-pole breaker switches the line and the neutral: there is no second leg. */
	private boolean switchedNeutral() {
		return breaker != null && breaker.poles() == 2 && breaker.tier() == Voltage.LOW && !ElectricalStandard.active().splitPhase();
	}

	// ---- by hand -------------------------------------------------------------------------------------------------------------------
	/** Clips a breaker in: null if it went in, otherwise why not. */
	public @Nullable String install(BranchBreakerItem item) {
		if (breaker != null) {
			return "This space already has a breaker";
		}
		Place place = place();
		if (place.panel() == null) {
			return place.problem();
		}
		if (item.tier() != place.panel().voltage()) {
			return "That breaker doesn't fit a " + place.panel().name().toLowerCase(java.util.Locale.ROOT) + ": it is a different frame";
		}
		if (place.panel().used()[0] + item.poles() > place.panel().spaces()) {
			return "The panel is full: its " + place.panel().spaces() + " spaces are taken";
		}
		breaker = item;
		closed = false;
		tripped = false;
		heat = 0;
		lastTrip = "";
		show();
		return null;
	}

	/** Pulls an open breaker out as its item; null if there is none or it is closed. */
	public @Nullable ItemStack pull() {
		if (breaker == null || closed) {
			return null;
		}
		ItemStack out = new ItemStack(breaker);
		breaker = null;
		tripped = false;
		lastTrip = "";
		show();
		return out;
	}

	/** Toggles the breaker; closing resets a trip. False if there is none. */
	public boolean operate() {
		if (breaker == null) {
			return false;
		}
		closed = !closed;
		if (closed) {
			tripped = false;
			heat = 0;
		}
		show();
		return true;
	}

	/** Test hook: set the breaker on or off. */
	public void setClosed(boolean on) {
		if (breaker != null && closed != on) {
			operate();
		}
	}

	private void show() {
		setChanged();
		if (level != null && getBlockState().getBlock() instanceof PanelGutterBlock) {
			PanelGutterBlock.Slot want = breaker == null ? PanelGutterBlock.Slot.EMPTY : tripped ? PanelGutterBlock.Slot.TRIPPED
					: closed ? PanelGutterBlock.Slot.ON : PanelGutterBlock.Slot.OFF;
			if (getBlockState().getValue(PanelGutterBlock.SLOT) != want) {
				level.setBlock(worldPosition, getBlockState().setValue(PanelGutterBlock.SLOT, want), 3);
			}
		}
	}

	// ---- the circuit -------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		if (breaker == null || face != facing() || voltage != breaker.tier()) {
			return false;
		}
		if (role == Conductor.N || role == Conductor.G) {
			return true;
		}
		return breaker.poles() == 1 || switchedNeutral() || role.ordinal() < breaker.poles();
	}

	@Override
	public void build(Circuit circuit) {
		Place place = place();
		links = new int[0];
		neutralLink = -1;
		hot = new int[0];
		if (place.panel() == null) {
			return;   // no panel, or two: it passes no bus and carries nothing
		}
		int[] bus = new int[3];
		for (int k = 0; k < 3; k++) {
			bus[k] = circuit.node();
		}
		int neutral = circuit.node();
		int ground = circuit.node();
		for (Direction side : PanelChain.inPlane(facing())) {
			for (int k = 0; k < 3; k++) {
				circuit.panelBusPort(worldPosition, side, ROLES[k], bus[k]);
			}
			circuit.panelBusPort(worldPosition, side, Conductor.N, neutral);
			circuit.panelBusPort(worldPosition, side, Conductor.G, ground);
		}
		if (breaker == null || !place.inSpaces() || breaker.tier() != place.panel().voltage()) {
			return;
		}
		Voltage tier = breaker.tier();
		Conductor[] phases = phases(place.panel(), place.index());
		hot = new int[phases.length];
		links = closed ? new int[phases.length] : new int[0];
		for (int k = 0; k < phases.length; k++) {
			hot[k] = circuit.node();
			if (closed) {
				links[k] = circuit.resistor(bus[phases[k].ordinal()], hot[k], Tuning.BREAKER_CLOSED_OHMS);
			}
			if (phases.length == 1) {
				circuit.hotPort(worldPosition, facing(), tier, hot[k], Tuning.CONTACT_OHMS / 2, false);
			} else {
				circuit.port(worldPosition, facing(), ROLES[k], tier, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
		// the branch neutral comes back through the breaker (a GFCI's pigtail), so the breaker can compare it with the hot
		int branchNeutral = circuit.node();
		if (closed || !switchedNeutral()) {
			neutralLink = circuit.resistor(branchNeutral, neutral, Tuning.BREAKER_CLOSED_OHMS);
		}
		circuit.port(worldPosition, facing(), Conductor.N, tier, branchNeutral, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(worldPosition, facing(), Conductor.G, tier, ground, Tuning.CONTACT_OHMS / 2, false);
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		return most;
	}

	/** Out on the hot pole(s) less back on the neutral: what leaks to ground. */
	private double imbalance(Circuit circuit) {
		if (links.length == 0 || neutralLink < 0) {
			return 0;
		}
		Complex out = Complex.ZERO;
		for (int link : links) {
			out = out.plus(circuit.currentPhasor(link));
		}
		return out.minus(circuit.currentPhasor(neutralLink)).abs();
	}

	/** The ground-fault trip (a GFCI), then the magnetic trip. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (links.length == 0 || breaker == null) {
			return false;
		}
		if (breaker.gfci()) {
			double leak = imbalance(circuit);
			if (leak > ElectricalStandard.active().groundFaultTripAmps()) {
				trip(String.format("ground fault: %,.1f A leaking to ground, over its %.0f mA", leak, ElectricalStandard.active().groundFaultTripAmps() * 1000));
				return true;
			}
		}
		double now = mostAmps(circuit);
		if (now > breaker.amps() * Tuning.BREAKER_MAGNETIC_MULTIPLE) {
			trip(String.format("short circuit: %,.0f A on a %d A breaker", now, breaker.amps()));
			return true;
		}
		return false;
	}

	// ---- the protection race and the coordination study ----------------------------------------------------------------------------
	/** A GFCI on a ground fault opens in about 1.5 cycles; the magnetic trip in about one. */
	@Override
	public double clearingSeconds(Circuit circuit) {
		if (links.length == 0 || breaker == null) {
			return Double.POSITIVE_INFINITY;
		}
		double t = Double.POSITIVE_INFINITY;
		if (breaker.gfci() && imbalance(circuit) > ElectricalStandard.active().groundFaultTripAmps()) {
			t = Tuning.GFCI_CLEAR_CYCLES / circuit.hertz();
		}
		if (mostAmps(circuit) > breaker.amps() * Tuning.BREAKER_MAGNETIC_MULTIPLE) {
			t = Math.min(t, Tuning.MAGNETIC_CLEAR_CYCLES / circuit.hertz());
		}
		return t;
	}

	@Override
	public double instantPickupAmps() {
		return breaker == null ? Double.POSITIVE_INFINITY : breaker.amps() * Tuning.BREAKER_MAGNETIC_MULTIPLE;
	}

	@Override
	public String curveName() {
		return breaker == null ? "empty space" : String.format("%d A branch breaker", breaker.amps());
	}

	@Override
	public double clearingSeconds(double amps) {
		return breaker == null ? Double.POSITIVE_INFINITY : dev.wildcatter.circuit.TimeCurrent.thermalMagnetic(curveName(), breaker.amps(), Tuning.BREAKER_TRIP_SECONDS_AT_2X,
				Tuning.BREAKER_MAGNETIC_MULTIPLE, ElectricalStandard.active().hertz()).clearingSeconds(amps);
	}

	@Override
	public double ratingAmps() {
		return breaker == null ? 0 : breaker.amps();
	}

	@Override
	public double interruptingAmps() {
		return breaker != null && breaker.tier() == Voltage.INDUSTRIAL ? Tuning.INTERRUPT_PANELBOARD : Tuning.INTERRUPT_LOAD_CENTRE;
	}

	/** On the panel's bus: its upstream is the panel's main ({@link Coordination#upstream}). */
	@Override
	public List<Terminal> lineSide() {
		return List.of();
	}

	/** The thermal trip. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = mostAmps(circuit);
		imbalance = breaker != null && breaker.gfci() ? imbalance(circuit) : 0;
		volts = hot.length >= 2 ? circuit.across(hot[0], hot[1]) : hot.length == 1 ? circuit.voltage(hot[0]) : 0;
		if (breaker == null) {
			return;
		}
		double ratio = amps / breaker.amps();
		if (ratio > 1.0) {
			heat += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * Tuning.BREAKER_TRIP_SECONDS_AT_2X);
		} else {
			heat = Math.max(0, heat - ticks / (20.0 * Tuning.BREAKER_COOL_SECONDS));
		}
		if (heat >= 1.0 && closed) {
			trip(String.format("overload: %,.1f A on a %d A breaker", amps, breaker.amps()));
		}
	}

	private void trip(String why) {
		closed = false;
		tripped = true;
		heat = 0;
		amps = 0;
		lastTrip = why;
		show();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.1F);
			CircuitManager.markDirty(server);
		}
	}

	// ---- the readout ------------------------------------------------------------------------------------------------------------
	@Override
	public Component inspect() {
		Place place = place();
		String where = place.index() >= 0 && place.panel() != null
				? "Space " + (place.index() + 1) + " (" + stabs(place) + ")" : "Panel gutter";
		if (place.panel() == null) {
			return Component.literal(where + ": " + place.problem());
		}
		if (breaker == null) {
			return Component.literal(where + ": EMPTY: clip a branch breaker in");
		}
		String kind = String.format("%d A %d-pole%s", breaker.amps(), breaker.poles(), breaker.gfci() ? (ElectricalStandard.active().splitPhase() ? " GFCI" : " RCBO") : "");
		String state;
		if (!place.inSpaces()) {
			state = place.problem();
		} else if (tripped) {
			state = "TRIPPED (" + lastTrip + "); right-click to reset after you fix the fault";
		} else if (closed) {
			state = String.format("ON, %,.1f A", amps);
			if (breaker.gfci()) {
				state += String.format("; imbalance %.1f mA", imbalance * 1000);
			}
			if (breaker.poles() >= 2 && !switchedNeutral()) {
				state += String.format("; %,.0f V across its poles", volts);
			}
			if (heat > 0.05) {
				state += ", getting warm " + Math.round(heat * 100) + "%";
			}
		} else {
			state = "OFF; sneak and right-click to pull it out";
		}
		return Component.literal(where + ", " + kind + ": " + state + (place.inSpaces() ? Coordination.readout(level, this) : ""));
	}

	private String stabs(Place place) {
		StringBuilder text = new StringBuilder("phase ");
		Conductor[] phases = breaker == null ? new Conductor[] {place.panel().busPhases()[place.index() % place.panel().busPhases().length]} : phases(place.panel(), place.index());
		for (int k = 0; k < phases.length; k++) {
			text.append(k == 0 ? "" : "-").append(phases[k].name());
		}
		return text.toString();
	}

	/** Breaking the gutter drops its breaker. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && breaker != null) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(breaker));
		}
	}

	// ---- saving --------------------------------------------------------------------------------------------------------------------
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
		output.putBoolean("has", breaker != null);
		if (breaker != null) {
			output.putString("tier", breaker.tier().name());
			output.putInt("amps", breaker.amps());
			output.putInt("poles", breaker.poles());
			output.putBoolean("gfci", breaker.gfci());
		}
		output.putBoolean("closed", closed);
		output.putBoolean("tripped", tripped);
		output.putDouble("heat", heat);
		output.putString("lastTrip", lastTrip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		breaker = null;
		if (input.getBooleanOr("has", false)) {
			Voltage tier = Voltage.LOW;
			for (Voltage v : Voltage.values()) {
				if (v.name().equals(input.getStringOr("tier", ""))) {
					tier = v;
				}
			}
			breaker = WildcatterItems.branchBreaker(tier, input.getIntOr("amps", 20), input.getIntOr("poles", 1), input.getBooleanOr("gfci", false));
		}
		closed = input.getBooleanOr("closed", false);
		tripped = input.getBooleanOr("tripped", false);
		heat = input.getDoubleOr("heat", 0);
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
