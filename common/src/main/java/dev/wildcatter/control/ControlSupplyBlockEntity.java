package dev.wildcatter.control;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A standard DIN-rail supply: a small fixed load on the mains, 24 V DC out while the mains holds up (docs/multiblocks/plc.md, proposed). */
public class ControlSupplyBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	/** What it draws from the mains. */
	public static final double WATTS = 100;
	/** Below this share of nominal it makes no 24 V. */
	public static final double DROPOUT = 0.85;
	/** Its rating: 24 V, 10 A, a 240 W DIN-rail supply. */
	public static final double RATED_AMPS = 10;
	/**
	 * Over its rating it shuts its output off for this long and tries again (hiccup mode), as a real supply protects itself, so an overload
	 * flickers every PLC on it rather than burning the supply.
	 */
	public static final int HICCUP_TICKS = 20;
	/** A cabinet's report of what it draws holds this long. */
	private static final int REPORT_TICKS = 20;
	private final java.util.Map<BlockPos, double[]> loads = new java.util.HashMap<>();
	private long offUntil;
	private long overloads;
	private int hot;
	private int neutral;
	private double volts;

	public ControlSupplyBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CONTROL_SUPPLY_ENTITY, pos, state);
	}

	public boolean live() {
		return mainsOk() && (level == null || level.getGameTime() >= offUntil);
	}

	private boolean mainsOk() {
		return volts >= Voltage.LOW.nominal() * DROPOUT;
	}

	/**
	 * A cabinet or drop's 24 V load on this supply, reported each scan. Over {@link #RATED_AMPS} in all, the output goes off for {@link #HICCUP_TICKS}.
	 */
	public void report(BlockPos from, double amps, long now) {
		loads.put(from.immutable(), new double[] {amps, now});
		double total = amps(now);
		if (total > RATED_AMPS && now >= offUntil) {
			offUntil = now + HICCUP_TICKS;
			overloads++;
			loads.clear();   // what it fed has dropped out
		}
	}

	/** The 24 V load on it now, from its cabinets' latest reports. */
	public double amps(long now) {
		loads.values().removeIf(l -> now - (long) l[1] > REPORT_TICKS);
		return loads.values().stream().mapToDouble(l -> l[0]).sum();
	}

	public boolean overloaded() {
		return level != null && level.getGameTime() < offUntil;
	}

	public long overloads() {
		return overloads;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		hot = circuit.node();
		neutral = circuit.node();
		double nominal = Voltage.LOW.nominal();
		circuit.resistor(hot, neutral, nominal * nominal / WATTS);
		for (Direction face : Direction.values()) {
			circuit.hotPort(worldPosition, face, Voltage.LOW, hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Conductor.N, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = Math.abs(circuit.across(hot, neutral));
		if (level != null && getBlockState().getValue(ControlSupplyBlock.ON) != live()) {
			level.setBlock(worldPosition, getBlockState().setValue(ControlSupplyBlock.ON, live()), 3);
		}
	}

	@Override
	public Component inspect() {
		long now = level == null ? 0 : level.getGameTime();
		String state = overloaded() ? String.format("OVERLOAD: over its %.0f A, off and trying again", RATED_AMPS)
				: live() ? String.format("making 24 V, %.2f A of %.0f A", amps(now), RATED_AMPS) : "NO 24 V: it needs " + Voltage.LOW.label() + " hot and neutral";
		return Component.literal("24 V DC control supply, 10 A: " + state + String.format(", mains %.0f V", volts) + (overloads > 0 ? ", " + overloads + " overload(s)" : ""));
	}
}
