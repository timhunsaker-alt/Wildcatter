package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.BusProtection;
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
 */
public class BreakerRelayCabinetBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private boolean grown;
	private @Nullable BlockPos breaker;
	private int touching;
	private int pickupIndex = Tuning.RELAY_PICKUP_START;
	private boolean tripped;
	private String lastTrip = "";
	private double hertz = ElectricalStandard.active().hertz();
	private double amps;
	private final BusProtection.TimeElement element = new BusProtection.TimeElement(false);

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
		int step = Tuning.RELAY_PICKUP_STEPS[Math.min(pickupIndex, Tuning.RELAY_PICKUP_STEPS.length - 1)];
		return b == null ? step : Math.min(step, b.rating());
	}

	public void nextPickup() {
		findBreaker();
		BreakerBlockEntity b = breaker();
		do {
			pickupIndex = (pickupIndex + 1) % Tuning.RELAY_PICKUP_STEPS.length;
		} while (b != null && Tuning.RELAY_PICKUP_STEPS[pickupIndex] > b.rating() && pickupIndex != 0);
		element.reset();
		setChanged();
	}

	/** Sets the 51 pickup to the step nearest under {@code target} amps (the game test). */
	public void setPickup(int target) {
		for (int k = 0; k < Tuning.RELAY_PICKUP_STEPS.length; k++) {
			if (Tuning.RELAY_PICKUP_STEPS[k] <= target) {
				pickupIndex = k;
			}
		}
		element.reset();
		setChanged();
	}

	// ---- the circuit ----------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public void build(Circuit circuit) {
		findBreaker();
	}

	/** 50: at 8 x the pickup, on the solve the fault appears in. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		BreakerBlockEntity b = breaker();
		if (b == null || !b.closed()) {
			return false;
		}
		double now = b.mostAmps(circuit);
		if (now > Tuning.RELAY_50_OF_PICKUP * pickup()) {
			trip(b, String.format("50 instantaneous, %,.0f A", now));
			return true;
		}
		return false;
	}

	/** 81U on a 12.47 kV breaker, then 51. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		hertz = circuit.hertz();
		BreakerBlockEntity b = breaker();
		if (b == null || !b.closed()) {
			amps = 0;
			element.reset();
			return;
		}
		amps = b.mostAmps(circuit);
		double shed = ElectricalStandard.active().loadShedHertz();
		if (distribution(b) && hertz < shed) {
			trip(b, String.format("81U under-frequency, the island fell to %.1f Hz, below %.1f Hz", hertz, shed));
		} else if (element.step(amps, pickup(), Tuning.RELAY_TIME_DIAL, ticks / 20.0)) {
			trip(b, String.format("51 time overcurrent, %,.0f A on a %,d A pickup", amps, pickup()));
		}
	}

	private void trip(BreakerBlockEntity b, String why) {
		b.tripByRelay(why);
		tripped = true;
		lastTrip = why;
		element.reset();
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	/** The reset: the relay's targets cleared and the breaker it opened closed again. */
	public void reset() {
		tripped = false;
		element.reset();
		BreakerBlockEntity b = breaker();
		if (b != null) {
			b.closeByRelay();
		}
		setChanged();
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
		if (tripped) {
			text.append("TRIPPED: ").append(lastTrip).append(lastTrip.startsWith("81U") ? "; right-click to reclose once the frequency has recovered" : "; right-click to reset and reclose");
		} else {
			text.append(b.closed() ? String.format("CLOSED, %,.0f A", amps) : "OPEN");
		}
		text.append(String.format("; 51 pickup %,d A (MI, TD %.0f), 50 at %,.0f A", pickup(), Tuning.RELAY_TIME_DIAL, Tuning.RELAY_50_OF_PICKUP * pickup()));
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
		output.putInt("pickup", pickupIndex);
		output.putBoolean("tripped", tripped);
		output.putString("lastTrip", lastTrip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		pickupIndex = Math.max(0, input.getIntOr("pickup", Tuning.RELAY_PICKUP_START));
		tripped = input.getBooleanOr("tripped", false);
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
