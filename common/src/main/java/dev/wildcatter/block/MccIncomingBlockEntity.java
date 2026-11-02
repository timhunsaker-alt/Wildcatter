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
 * The main breaker and the bus of a motor control center (docs/multiblocks/motor-control-center.md). The main breaker behaves like
 * a three-pole breaker (thermal and magnetic trip on the largest phase current). The feed is on the top of the section's top cell
 * or the back of its bottom cell; the bus leaves the bottom cell's right-hand side. Its readout lists the buckets on the bus.
 */
public class MccIncomingBlockEntity extends BlockEntity implements CircuitPart, Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	private final int[] feed = new int[3];
	private final int[] bus = new int[3];
	private int[] poles = new int[0];
	private int ratingIndex = 3;   // 100 A
	private double heat;
	private double amps;
	private double volts;
	private String lastTrip = "";
	private boolean grown;

	public MccIncomingBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MCC_INCOMING_ENTITY, pos, state);
	}

	private Direction facing() {
		return getBlockState().getValue(MccIncomingBlock.FACING);
	}

	private boolean closed() {
		return getBlockState().getValue(MccIncomingBlock.POSITION) == BreakerBlock.Position.ON;
	}

	public int rating() {
		return Tuning.BREAKER_AMPS_INDUSTRIAL[Math.max(0, Math.min(Tuning.BREAKER_AMPS_INDUSTRIAL.length - 1, ratingIndex))];
	}

	public void nextRating() {
		ratingIndex = (ratingIndex + 1) % Tuning.BREAKER_AMPS_INDUSTRIAL.length;
		setChanged();
	}

	public void setSwitch(boolean on) {
		heat = 0;
		setPosition(on ? BreakerBlock.Position.ON : BreakerBlock.Position.OFF);
	}

	private void setPosition(BreakerBlock.Position position) {
		if (level != null && getBlockState().getValue(MccIncomingBlock.POSITION) != position) {
			level.setBlock(worldPosition, getBlockState().setValue(MccIncomingBlock.POSITION, position), 3);
			setChanged();
		}
	}

	/** The buckets on the bus, section by section left to right as seen from the front, each bottom to top. */
	public List<Starter> buckets() {
		List<Starter> found = new ArrayList<>();
		if (level == null) {
			return found;
		}
		Direction right = facing().getCounterClockWise();
		BlockPos at = worldPosition;
		for (int step = 1; step <= Tuning.MCC_MAX_BUCKETS / MccSectionBlockEntity.SPACES; step++) {
			at = at.relative(right);
			if (!(level.getBlockEntity(at) instanceof MccSectionBlockEntity section && section.onBus())) {
				break;
			}
			for (Starter bucket : section.buckets()) {
				if (bucket != null) {
					found.add(bucket);
				}
			}
		}
		return found;
	}

	/** The feed's two terminals: the back of the bottom cell, the top of the top cell. */
	private boolean feedAt(BlockPos cell, Direction face) {
		return cell.equals(worldPosition) && face == facing().getOpposite() || cell.equals(worldPosition.above(2)) && face == Direction.UP;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return hasPortAt(worldPosition, face, role, voltage);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && feedAt(cell, face);
	}

	@Override
	public void build(Circuit circuit) {
		poles = closed() ? new int[3] : new int[0];
		int neutral = circuit.node();
		int ground = circuit.node();
		for (int k = 0; k < 3; k++) {
			feed[k] = circuit.node();
			bus[k] = circuit.node();
			if (closed()) {
				poles[k] = circuit.resistor(feed[k], bus[k], Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		for (Object[] in : new Object[][] {{worldPosition, facing().getOpposite()}, {worldPosition.above(2), Direction.UP}}) {
			BlockPos cell = (BlockPos) in[0];
			Direction face = (Direction) in[1];
			for (int k = 0; k < 3; k++) {
				circuit.port(cell, face, PHASES[k], Voltage.INDUSTRIAL, feed[k], Tuning.CONTACT_OHMS / 2, false);
			}
			circuit.port(cell, face, Conductor.N, Voltage.INDUSTRIAL, neutral, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(cell, face, Conductor.G, Voltage.INDUSTRIAL, ground, Tuning.CONTACT_OHMS / 2, false);
		}
		Direction right = facing().getCounterClockWise();
		for (int k = 0; k < 3; k++) {
			circuit.busPort(worldPosition, right, PHASES[k], bus[k]);
		}
		circuit.busPort(worldPosition, right, Conductor.G, ground);
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int pole : poles) {
			most = Math.max(most, circuit.current(pole));
		}
		return most;
	}

	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (poles.length == 0) {
			return false;
		}
		double now = mostAmps(circuit);
		if (now > rating() * Tuning.BREAKER_MAGNETIC_MULTIPLE) {
			trip(now, "short circuit");
			return true;
		}
		return false;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = mostAmps(circuit);
		volts = circuit.across(feed[0], feed[1]);
		double ratio = amps / rating();
		if (ratio > 1.0) {
			heat += (ratio * ratio - 1.0) / 3.0 * ticks / (20.0 * Tuning.BREAKER_TRIP_SECONDS_AT_2X);
		} else {
			heat = Math.max(0, heat - ticks / (20.0 * Tuning.BREAKER_COOL_SECONDS));
		}
		if (heat >= 1.0 && closed()) {
			trip(amps, "overload");
		}
	}

	private void trip(double tripAmps, String reason) {
		heat = 0;
		amps = 0;
		lastTrip = String.format("%s: %.0f A on a %d A main", reason, tripAmps, rating());
		setPosition(BreakerBlock.Position.TRIPPED);
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 0.9F);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String main = switch (getBlockState().getValue(MccIncomingBlock.POSITION)) {
			case ON -> String.format("main %d A ON, %.0f V, %.1f A", rating(), volts, amps);
			case OFF -> "main " + rating() + " A OFF";
			case TRIPPED -> "main " + rating() + " A TRIPPED (" + lastTrip + "): right-click to reset";
		};
		StringBuilder text = new StringBuilder("Motor control center (" + Voltage.INDUSTRIAL.label() + " three-phase): " + main);
		List<Starter> buckets = buckets();
		text.append("; ").append(buckets.size()).append(" bucket(s)");
		for (int i = 0; i < buckets.size(); i++) {
			text.append(i == 0 ? ": " : ", ").append(i + 1).append(" ").append(buckets.get(i).brief());
		}
		return Component.literal(text.toString());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("rating", ratingIndex);
		output.putDouble("heat", heat);
		output.putString("lastTrip", lastTrip);
		output.putBoolean("grown", grown);
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
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ratingIndex = input.getIntOr("rating", 3);
		heat = input.getDoubleOr("heat", 0);
		lastTrip = input.getStringOr("lastTrip", "");
		grown = input.getBooleanOr("grown", false);
	}
}
