package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Role;
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
	private int back;
	private int front;
	private int link = -1;
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
			case HIGH -> Tuning.BREAKER_AMPS_HIGH;
		};
	}

	private boolean plainSwitch() {
		return ((BreakerBlock) getBlockState().getBlock()).isSwitch();
	}

	public int rating() {
		return ratings()[Math.min(ratingIndex, ratings().length - 1)];
	}

	private boolean closed() {
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
	public boolean hasPort(Direction face, Role role, Voltage voltage) {
		return role == Role.HOT && voltage == voltage() && (face == facing() || face == facing().getOpposite());
	}

	@Override
	public void build(Circuit circuit) {
		back = circuit.node();
		front = circuit.node();
		link = closed() ? circuit.resistor(back, front, Tuning.BREAKER_CLOSED_OHMS) : -1;
		circuit.port(worldPosition, facing().getOpposite(), Role.HOT, voltage(), back, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(worldPosition, facing(), Role.HOT, voltage(), front, Tuning.CONTACT_OHMS / 2, false);
	}

	/** The magnetic trip: a current far over the rating opens it at once, before anything else sees the fault. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (link < 0 || plainSwitch()) {
			return false;
		}
		double now = Math.abs(circuit.current(link));
		if (now > rating() * Tuning.BREAKER_MAGNETIC_MULTIPLE) {
			trip(now, "short circuit");
			return true;
		}
		return false;
	}

	/** The thermal trip: a moderate overload heats it up until it opens. */
	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = link >= 0 ? Math.abs(circuit.current(link)) : 0;
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
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
			CircuitManager.markDirty(server);
		}
		lastTrip = String.format("%s: %.0f A on a %d A breaker", reason, tripAmps, rating());
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
		if (plainSwitch()) {
			return Component.literal("Switch (" + voltage().label() + "): " + (closed() ? String.format("CLOSED, carrying %.1f A", amps) : "OPEN")
					+ "; right-click to flip it. A switch has NO protection: it never trips, so put a breaker or fuse before it");
		}
		return Component.literal("Breaker " + rating() + " A (" + voltage().label() + "): " + status + "; sneak and right-click changes the rating"
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
		ratingIndex = Math.max(0, Math.min(3, input.getIntOr("rating", 0)));
		heat = input.getDoubleOr("heat", 0);
		lastTrip = input.getStringOr("lastTrip", "");
	}
}
