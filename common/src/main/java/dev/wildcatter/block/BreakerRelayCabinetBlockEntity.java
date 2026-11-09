package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A standalone breaker's relay (docs/multiblocks/breaker-relay-cabinet.md). Each time the circuit is built it finds the 12.47 kV or
 * 138 kV yard breaker it touches; then on each solve 50 trips it at once on a heavy fault, and after the solve 51 runs its moderately
 * inverse curve and, on a 12.47 kV breaker, 81U sheds it below the standard's load-shed frequency. It has no terminals: it reads the
 * breaker's CTs. Its reset recloses what it opened.
 * <p>Protection gap wave (docs/questions/protection-gaps.md): pickup, curve, dial and 50 are set from the relay door with the Power
 * Meter ({@link dev.wildcatter.circuit.RelaySettings}), and 79 recloses the breaker after a 50/51 trip: up to three shots after the
 * open intervals of {@link Tuning#RECLOSE_INTERVALS}, then lockout; held closed for {@link Tuning#RECLOSE_RESET_SECONDS} it resets. An 81U
 * trip is never reclosed.
 */
public class BreakerRelayCabinetBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, dev.wildcatter.circuit.TripCurve {
	private boolean grown;
	private @Nullable BlockPos breaker;
	private int touching;
	private final dev.wildcatter.circuit.RelaySettings settings = new dev.wildcatter.circuit.RelaySettings().withReclosing(-1);
	/** False until the 79's shots are set for the breaker's voltage (or by hand): a 12.47 kV feeder three shots, a 138 kV line one. */
	private boolean shotsSet;
	private int shotsUsed;
	private double reclosingIn = -1;   // seconds to the next shot while a 79 sequence is open; -1 when none
	private double closedFor;
	private boolean lockedOut;
	private boolean tripped;
	private String lastTrip = "";
	private double hertz = ElectricalStandard.active().hertz();
	private double amps;
	private final dev.wildcatter.circuit.RelaySettings.Element element = new dev.wildcatter.circuit.RelaySettings.Element();

	public BreakerRelayCabinetBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BREAKER_RELAY_CABINET_ENTITY, pos, state);
	}

	public boolean hasTripped() {
		return tripped;
	}

	public String lastTrip() {
		return lastTrip;
	}

	private void findBreaker() {
		breaker = null;
		touching = 0;
		if (level == null) {
			return;
		}
		java.util.Set<BlockPos> found = new java.util.LinkedHashSet<>();
		for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
			for (Direction d : Direction.values()) {
				if (BigParts.entity(level, cell.relative(d)) instanceof BreakerBlockEntity b && b.getBlockState().getBlock() instanceof BigBreakerBlock big
						&& (big.voltage() == Voltage.DISTRIBUTION || big.voltage() == Voltage.HIGH)) {
					found.add(b.getBlockPos());
				}
			}
		}
		touching = found.size();
		breaker = found.isEmpty() ? null : found.iterator().next();
	}

	private @Nullable BreakerBlockEntity breaker() {
		return breaker != null && level != null && level.getBlockEntity(breaker) instanceof BreakerBlockEntity b ? b : null;
	}

	private boolean distribution(BreakerBlockEntity b) {
		return ((BreakerBlock) b.getBlockState().getBlock()).voltage() == Voltage.DISTRIBUTION;
	}

	/** The 51 pickup: a step of {@link Tuning#RELAY_PICKUP_STEPS}, no higher than the breaker's rating. */
	public int pickup() {
		BreakerBlockEntity b = breaker();
		return settings.pickup(b == null ? 0 : b.rating());
	}

	public dev.wildcatter.circuit.RelaySettings settings() {
		defaultShots();
		return settings;
	}

	private void defaultShots() {
		BreakerBlockEntity b = breaker();
		if (!shotsSet && b != null) {
			settings.setShots(distribution(b) ? Tuning.RECLOSE_SHOTS_DISTRIBUTION : Tuning.RECLOSE_SHOTS_HIGH);
			shotsSet = true;
		}
	}

	/** Sneak and right-click with the Power Meter: steps the setting the relay's cursor is on (the pickup on a new cabinet). */
	public void nextPickup() {
		findBreaker();
		defaultShots();
		BreakerBlockEntity b = breaker();
		settings.step(b == null ? 0 : b.rating());
		shotsSet = true;
		element.reset();
		setChanged();
	}

	/** Right-click with the Power Meter on the relay (the upper cell): the cursor moves to the next setting. */
	public void nextSetting() {
		settings.nextField();
		setChanged();
	}

	/** Sets the 51 pickup to the step nearest under {@code target} amps (the game test). */
	public void setPickup(int target) {
		settings.setPickup(target);
		element.reset();
		setChanged();
	}

	/** Sets the 79's shots (0 turns reclosing off). */
	public void setShots(int shots) {
		settings.setShots(shots);
		shotsSet = true;
		setChanged();
	}

	public boolean lockedOut() {
		return lockedOut;
	}

	public int shotsUsed() {
		return shotsUsed;
	}

	// ---- the circuit ----------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public void build(Circuit circuit) {
		findBreaker();
		defaultShots();
	}

	/** 50: at 8 x the pickup, on the solve the fault appears in. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		BreakerBlockEntity b = breaker();
		if (b == null || !b.closed()) {
			return false;
		}
		double now = b.mostAmps(circuit);
		if (settings.instantaneous() > 0 && now > settings.instantaneous() * pickup()) {
			trip(b, String.format("50 instantaneous, %,.0f A", now), true);
			return true;
		}
		return false;
	}

	/** 81U on a 12.47 kV breaker, then 51. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		hertz = circuit.hertz();
		double dt = ticks / 20.0;
		BreakerBlockEntity b = breaker();
		if (b != null && !b.closed() && reclosingIn >= 0) {
			// 79: the open interval runs, then the shot recloses the breaker
			reclosingIn -= dt;
			if (reclosingIn <= 0) {
				reclosingIn = -1;
				shotsUsed++;
				closedFor = 0;
				tripped = false;
				lastTrip = lastTrip + String.format("; reclosed by 79 (shot %d of %d)", shotsUsed, settings.shots());
				b.closeByRelay();
				setChanged();
			}
		}
		if (b == null || !b.closed()) {
			amps = 0;
			element.reset();
			return;
		}
		if (shotsUsed > 0) {
			closedFor += dt;
			if (closedFor >= Tuning.RECLOSE_RESET_SECONDS) {
				shotsUsed = 0;   // held closed through the reset time: the fault was temporary, and the sequence starts over
				setChanged();
			}
		}
		amps = b.mostAmps(circuit);
		double shed = ElectricalStandard.active().loadShedHertz();
		if (distribution(b) && hertz < shed) {
			trip(b, String.format("81U under-frequency, the island fell to %.1f Hz, below %.1f Hz", hertz, shed), false);
		} else if (element.step(amps, pickup(), settings.curve(), settings.dial(), dt)) {
			trip(b, String.format("51 time overcurrent, %,.0f A on a %,d A pickup", amps, pickup()), true);
		}
	}

	/** Opens the breaker; after a fault trip ({@code reclose}) 79 starts its next open interval, or locks out with no shots left. */
	private void trip(BreakerBlockEntity b, String why, boolean reclose) {
		b.tripByRelay(why);
		tripped = true;
		lastTrip = why;
		element.reset();
		if (reclose && shotsUsed < settings.shots()) {
			reclosingIn = Tuning.RECLOSE_INTERVALS[shotsUsed];
		} else {
			reclosingIn = -1;
			lockedOut = reclose && settings.shots() > 0;
			if (lockedOut) {
				lastTrip = why + String.format("; 79 LOCKED OUT after %d shot%s", shotsUsed, shotsUsed == 1 ? "" : "s");
			}
		}
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	/** The reset: the relay's targets cleared and the breaker it opened closed again. */
	public void reset() {
		tripped = false;
		lockedOut = false;
		shotsUsed = 0;
		reclosingIn = -1;
		element.reset();
		BreakerBlockEntity b = breaker();
		if (b != null) {
			b.closeByRelay();
		}
		setChanged();
	}

	// ---- the coordination curve -------------------------------------------------------------------------------------------------------
	@Override
	public String curveName() {
		BreakerBlockEntity b = breaker();
		return String.format("%s breaker relay (%s, TD %s, %,d A)", b == null ? "" : ((BreakerBlock) b.getBlockState().getBlock()).voltage().label(), settings.curve().name(),
				settings.dial() == Math.rint(settings.dial()) ? String.format("%.0f", settings.dial()) : String.valueOf(settings.dial()), pickup());
	}

	@Override
	public double clearingSeconds(double current) {
		BreakerBlockEntity b = breaker();
		return settings.secondsAt(current, b == null ? 0 : b.rating()) + Tuning.BREAKER_INTERRUPT_SECONDS;
	}

	// ---- the readout --------------------------------------------------------------------------------------------------------------
	@Override
	public Component inspect() {
		if (level != null && !level.isClientSide()) {
			findBreaker();
		}
		BreakerBlockEntity b = breaker();
		if (b == null) {
			return Component.literal("Breaker relay cabinet: NO BREAKER: set it against a " + Voltage.DISTRIBUTION.label() + " or " + Voltage.HIGH.label() + " breaker");
		}
		double shed = ElectricalStandard.active().loadShedHertz();
		StringBuilder text = new StringBuilder(String.format("Breaker relay cabinet: %s breaker (%,d A), ", ((BreakerBlock) b.getBlockState().getBlock()).voltage().label(), b.rating()));
		if (tripped && reclosingIn >= 0) {
			text.append("TRIPPED: ").append(lastTrip).append(String.format("; 79 recloses in %.0f s (shot %d of %d)", reclosingIn, shotsUsed + 1, settings.shots()));
		} else if (tripped) {
			text.append("TRIPPED: ").append(lastTrip).append(lastTrip.startsWith("81U") ? "; right-click to reclose once the frequency has recovered" : "; right-click to reset and reclose");
		} else {
			text.append(b.closed() ? String.format("CLOSED, %,.0f A", amps) : "OPEN");
		}
		text.append("; ").append(settings.describe(b.rating(), true));
		if (distribution(b)) {
			text.append(String.format("; 81U at %.1f Hz, island %.1f Hz", shed, hertz));
		}
		if (touching > 1) {
			text.append("; it touches ").append(touching).append(" breakers and works the first");
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
		int[] relay = settings.save();
		for (int k = 0; k < relay.length; k++) {
			output.putInt("relay" + k, relay[k]);
		}
		output.putBoolean("shotsSet", shotsSet);
		output.putInt("shotsUsed", shotsUsed);
		output.putDouble("reclosingIn", reclosingIn);
		output.putDouble("closedFor", closedFor);
		output.putBoolean("lockedOut", lockedOut);
		output.putBoolean("tripped", tripped);
		output.putString("lastTrip", lastTrip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		int[] relay = new int[6];
		int[] defaults = new dev.wildcatter.circuit.RelaySettings().save();
		for (int k = 0; k < relay.length; k++) {
			relay[k] = input.getIntOr("relay" + k, defaults[k]);
		}
		relay[0] = input.getIntOr("relay0", input.getIntOr("pickup", Tuning.RELAY_PICKUP_START));   // worlds before the gap wave saved only the pickup
		settings.load(relay);
		shotsSet = input.getBooleanOr("shotsSet", false);
		shotsUsed = input.getIntOr("shotsUsed", 0);
		reclosingIn = input.getDoubleOr("reclosingIn", -1);
		closedFor = input.getDoubleOr("closedFor", 0);
		lockedOut = input.getBooleanOr("lockedOut", false);
		tripped = input.getBooleanOr("tripped", false);
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
