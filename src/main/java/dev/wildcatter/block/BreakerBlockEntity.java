package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A thermal-magnetic breaker, like the one in a house panel. A big sudden current (a short circuit) trips it
 * at once. A current only a little over the rating heats it slowly: it trips sooner the further over it is, and
 * never if the current stays at or under the rating.
 */
public class BreakerBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private int ratingIndex;
	/** 0 to 1: how close the heat has brought it to tripping. */
	private double heat;
	private final int[] back = new int[3];
	private final int[] front = new int[3];
	/** The closed contacts, one per pole; empty when it is open. */
	private int[] links = new int[0];
	/** The largest of the pole currents. */
	private double amps;

	public BreakerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BREAKER_ENTITY, pos, state);
	}

	private Voltage voltage() {
		return ((BreakerBlock) getBlockState().getBlock()).voltage();
	}

	private int[] ratings() {
		return switch (voltage()) {
			case LOW -> Tuning.BREAKER_AMPS;
			case MEDIUM -> Tuning.BREAKER_AMPS_MEDIUM;
			case DISTRIBUTION -> Tuning.BREAKER_AMPS_DISTRIBUTION;
			case HIGH -> Tuning.BREAKER_AMPS_HIGH;
			case INDUSTRIAL -> Tuning.BREAKER_AMPS_INDUSTRIAL;
		};
	}

	private boolean plainSwitch() {
		BreakerBlock block = (BreakerBlock) getBlockState().getBlock();
		return block.isSwitch() || block.isRelay() || block.isRelayOperated();   // none of these trips on its own current
	}

	private boolean relay() {
		return ((BreakerBlock) getBlockState().getBlock()).isRelay();
	}

	private double hertz = dev.wildcatter.energy.ElectricalStandard.active().hertz();

	public int rating() {
		return ratings()[Math.min(ratingIndex, ratings().length - 1)];
	}

	/** The relay panel opens it: a protective trip. */
	public void tripByRelay(String reason) {
		if (closed()) {
			trip(amps, reason);
		}
	}

	/** The relay panel closes it after a reset. */
	public void closeByRelay() {
		setSwitch(true);
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	public double amps() {
		return amps;
	}

	public boolean closed() {
		return getBlockState().getValue(BreakerBlock.POSITION) == BreakerBlock.Position.ON;
	}

	private Direction facing() {
		return getBlockState().getValue(BreakerBlock.FACING);
	}

	public void nextRating() {
		ratingIndex = (ratingIndex + 1) % ratings().length;
		setChanged();
	}

	/** Flips the switch by hand. Closing it onto a fault will just trip it again. */
	public void setSwitch(boolean on) {
		heat = 0;
		setPosition(on ? BreakerBlock.Position.ON : BreakerBlock.Position.OFF);
	}

	private void setPosition(BreakerBlock.Position position) {
		if (level != null && getBlockState().getValue(BreakerBlock.POSITION) != position) {
			level.setBlock(worldPosition, getBlockState().setValue(BreakerBlock.POSITION, position), 3);
			setChanged();
		}
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return (role != Conductor.G && role != Conductor.N || poles() == 3) && voltage == voltage() && (face == facing() || face == facing().getOpposite());
	}

	private int poles() {
		return ((BreakerBlock) getBlockState().getBlock()).poles();
	}

	/** The biggest current through any pole. */
	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int link : links) {
			most = Math.max(most, circuit.current(link));
		}
		return most;
	}

	@Override
	public void build(Circuit circuit) {
		int poles = poles();
		links = closed() ? new int[poles] : new int[0];
		Conductor[] phases = {Conductor.A, Conductor.B, Conductor.C};
		for (int k = 0; k < poles; k++) {
			back[k] = circuit.node();
			front[k] = circuit.node();
			if (closed()) {
				links[k] = circuit.resistor(back[k], front[k], Tuning.BREAKER_CLOSED_OHMS);
			}
			if (poles == 1) {
				// one pole: it switches whichever phase runs through it
				circuit.hotPort(worldPosition, facing().getOpposite(), voltage(), back[k], Tuning.CONTACT_OHMS / 2, false);
				circuit.hotPort(worldPosition, facing(), voltage(), front[k], Tuning.CONTACT_OHMS / 2, false);
			} else {
				circuit.port(worldPosition, facing().getOpposite(), phases[k], voltage(), back[k], Tuning.CONTACT_OHMS / 2, false);
				circuit.port(worldPosition, facing(), phases[k], voltage(), front[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
		if (poles == 3) {
			// the neutral and ground run straight through a three-pole breaker, unswitched
			int neutral = circuit.node();
			int ground = circuit.node();
			circuit.port(worldPosition, facing().getOpposite(), Conductor.N, voltage(), neutral, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, facing(), Conductor.N, voltage(), neutral, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, facing().getOpposite(), Conductor.G, voltage(), ground, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, facing(), Conductor.G, voltage(), ground, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	/** The magnetic trip: a current far over the rating opens it at once, before anything else sees the fault. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (links.length == 0 || plainSwitch()) {
			return false;
		}
		double now = mostAmps(circuit);
		if (now > rating() * Tuning.BREAKER_MAGNETIC_MULTIPLE) {
			trip(now, "short circuit");
			return true;
		}
		return false;
	}

	/** The thermal trip: a moderate overload heats it up until it opens. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = mostAmps(circuit);
		hertz = circuit.hertz();
		if (relay()) {
			double shed = dev.wildcatter.energy.ElectricalStandard.active().loadShedHertz();
			if (closed() && hertz < shed) {
				trip(amps, "under-frequency");
				lastTrip = String.format("under-frequency: the island fell to %.1f Hz, below %.1f Hz", hertz, shed);
			}
			return;
		}
		double ratio = amps / rating();
		if (ratio > 1.0) {
			heat += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * Tuning.BREAKER_TRIP_SECONDS_AT_2X);
		} else {
			heat = Math.max(0, heat - ticks / (20.0 * Tuning.BREAKER_COOL_SECONDS));
		}
		if (plainSwitch()) {
			return;
		}
		if (heat >= 1.0 && closed()) {
			trip(amps, "overload");
		}
	}

	private void trip(double tripAmps, String reason) {
		heat = 0;
		amps = 0;
		setPosition(BreakerBlock.Position.TRIPPED);
		if (level instanceof ServerLevel server) {
			boolean big = voltage() == Voltage.HIGH || voltage() == Voltage.DISTRIBUTION || voltage() == Voltage.MEDIUM;   // power breakers bang, panel breakers snap
			server.playSound(null, worldPosition, big ? WildcatterSounds.HV_BREAKER : WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, big ? 1.5F : 1.0F, 1.0F);
			CircuitManager.markDirty(server);
		}
		lastTrip = ((BreakerBlock) getBlockState().getBlock()).isRelayOperated() ? reason   // the relay panel says why
				: String.format("%s: %.0f A on a %d A breaker", reason, tripAmps, rating());
	}

	private String lastTrip = "";

	@Override
	public Component inspect() {
		BreakerBlock.Position position = getBlockState().getValue(BreakerBlock.POSITION);
		String status = switch (position) {
			case ON -> String.format("ON, carrying %.1f A", amps);
			case OFF -> "OFF";
			case TRIPPED -> "TRIPPED (" + lastTrip + "); right-click to reset after you fix the fault";
		};
		if (relay()) {
			double shed = dev.wildcatter.energy.ElectricalStandard.active().loadShedHertz();
			String state = switch (position) {
				case ON -> String.format("CLOSED, carrying %.1f A", amps);
				case OFF -> "OPEN";
				case TRIPPED -> "TRIPPED (" + lastTrip + "); right-click to close it again once the frequency has recovered";
			};
			return Component.literal("Under-frequency relay (" + voltage().label() + "): " + state + String.format(", island at %.1f Hz, opens below %.1f Hz", hertz, shed)
					+ "; it sheds this feeder to save the generators and protects nothing else");
		}
		if (((BreakerBlock) getBlockState().getBlock()).isRelayOperated()) {
			String state = switch (position) {
				case ON -> String.format("CLOSED, carrying %.0f A", amps);
				case OFF -> "OPEN";
				case TRIPPED -> "TRIPPED by the relay panel (" + lastTrip + "); reset it at the relay panel";
			};
			return Component.literal("Substation breaker (" + voltage().label() + ", three-pole, relay operated): " + state);
		}
		if (plainSwitch()) {
			return Component.literal("Switch (" + voltage().label() + "): " + (closed() ? String.format("CLOSED, carrying %.1f A", amps) : "OPEN")
					+ "; right-click to flip it. A switch has NO protection: it never trips, so put a breaker or fuse before it");
		}
		return Component.literal((poles() == 3 ? "Three-pole breaker " : "Breaker ") + rating() + " A (" + voltage().label() + "): " + status + "; sneak and right-click changes the rating"
				+ (heat > 0.05 && closed() ? ", getting warm " + Math.round(heat * 100) + "%" : ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("rating", ratingIndex);
		output.putDouble("heat", heat);
		output.putString("lastTrip", lastTrip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ratingIndex = Math.max(0, input.getIntOr("rating", 0));
		heat = input.getDoubleOr("heat", 0);
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
