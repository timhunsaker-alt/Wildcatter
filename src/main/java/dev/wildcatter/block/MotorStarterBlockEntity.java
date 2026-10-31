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
 * The inside of a {@link MotorStarterBlock}: three contactor poles between the line and the motor, closed while it runs. The motor
 * circuit protector opens at once on a short (13 times the overload setting). The overload relay heats on the square of the current
 * above 115 % of its setting and trips class 10: within 10 s at 6 times the setting, a couple of minutes at twice it.
 */
public class MotorStarterBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	private final int[] line = new int[3];
	private final int[] load = new int[3];
	private int[] poles = new int[0];
	private int setting = Tuning.OVERLOAD_DEFAULT;
	/** 0 to 1: how close the overload relay is to tripping. */
	private double heat;
	private double amps;
	private boolean powered;
	private String lastTrip = "";

	public MotorStarterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MOTOR_STARTER_ENTITY, pos, state);
	}

	private MotorStarterBlock block() {
		return (MotorStarterBlock) getBlockState().getBlock();
	}

	private Direction facing() {
		return getBlockState().getValue(MotorStarterBlock.FACING);
	}

	public MotorStarterBlock.Run run() {
		return getBlockState().getValue(MotorStarterBlock.RUN);
	}

	public double overloadAmps() {
		return Tuning.OVERLOAD_SETTINGS[Math.max(0, Math.min(Tuning.OVERLOAD_SETTINGS.length - 1, setting))];
	}

	public void nextOverloadSetting() {
		setting = (setting + 1) % Tuning.OVERLOAD_SETTINGS.length;
		setChanged();
	}

	/** The start/stop button: start when stopped, stop when running, reset (to stopped) after a trip. */
	public void pushButton() {
		setRun(run() == MotorStarterBlock.Run.STOPPED ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
	}

	/** Redstone control: a rising signal starts it, a falling one stops it. A tripped starter waits for a reset by hand. */
	public void redstone(boolean signal) {
		if (signal != powered) {
			powered = signal;
			if (run() != MotorStarterBlock.Run.TRIPPED) {
				setRun(signal ? MotorStarterBlock.Run.RUNNING : MotorStarterBlock.Run.STOPPED);
			}
		}
	}

	private void setRun(MotorStarterBlock.Run run) {
		if (level != null && run() != run) {
			level.setBlock(worldPosition, getBlockState().setValue(MotorStarterBlock.RUN, run), 3);
			setChanged();
		}
	}

	/** The section to its left (seen from the front) toward the controller: a bucket is on the bus only in a formed lineup. */
	public boolean onBus() {
		if (!block().bucket() || level == null) {
			return false;
		}
		Direction left = facing().getClockWise();
		BlockPos at = worldPosition;
		for (int step = 1; step <= Tuning.MCC_MAX_BUCKETS; step++) {
			at = at.relative(left);
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof MccIncomingBlock && state.getValue(MccIncomingBlock.FACING) == facing()) {
				return true;
			}
			if (!(state.getBlock() instanceof MotorStarterBlock starter && starter.bucket() && state.getValue(MotorStarterBlock.FACING) == facing())) {
				return false;
			}
		}
		return false;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		if (voltage != Voltage.INDUSTRIAL) {
			return false;
		}
		Direction back = facing().getOpposite();
		return block().bucket() ? face == back || face == Direction.UP : face == back || face == facing();
	}

	@Override
	public void build(Circuit circuit) {
		boolean closed = run() == MotorStarterBlock.Run.RUNNING && (!block().bucket() || onBus());
		poles = closed ? new int[3] : new int[0];
		int ground = circuit.node();
		for (int k = 0; k < 3; k++) {
			line[k] = circuit.node();
			load[k] = circuit.node();
			if (closed) {
				poles[k] = circuit.resistor(line[k], load[k], Tuning.BREAKER_CLOSED_OHMS);
			}
		}
		Direction back = facing().getOpposite();
		if (block().bucket()) {
			if (onBus()) {
				Direction left = facing().getClockWise();
				for (Direction side : new Direction[] {left, left.getOpposite()}) {
					for (int k = 0; k < 3; k++) {
						circuit.busPort(worldPosition, side, PHASES[k], line[k]);
					}
					circuit.busPort(worldPosition, side, Conductor.G, ground);
				}
			}
			for (Direction out : new Direction[] {back, Direction.UP}) {
				motorSide(circuit, out, ground);
			}
		} else {
			for (int k = 0; k < 3; k++) {
				circuit.port(worldPosition, back, PHASES[k], Voltage.INDUSTRIAL, line[k], Tuning.CONTACT_OHMS / 2, false);
			}
			circuit.port(worldPosition, back, Conductor.G, Voltage.INDUSTRIAL, ground, Tuning.CONTACT_OHMS / 2, false);
			motorSide(circuit, facing(), ground);
		}
	}

	private void motorSide(Circuit circuit, Direction face, int ground) {
		for (int k = 0; k < 3; k++) {
			circuit.port(worldPosition, face, PHASES[k], Voltage.INDUSTRIAL, load[k], Tuning.CONTACT_OHMS / 2, false);
		}
		circuit.port(worldPosition, face, Conductor.G, Voltage.INDUSTRIAL, ground, Tuning.CONTACT_OHMS / 2, false);
	}

	private double mostAmps(Circuit circuit) {
		double most = 0;
		for (int pole : poles) {
			most = Math.max(most, circuit.current(pole));
		}
		return most;
	}

	/** The motor circuit protector: a short opens it at once, before anything else sees the fault. */
	@Override
	public boolean protect(Circuit circuit, int ticks) {
		if (poles.length == 0) {
			return false;
		}
		double now = mostAmps(circuit);
		if (now > overloadAmps() * Tuning.MCP_MULTIPLE) {
			trip(String.format("short circuit: %.0f A, the protector opens at %.0f A", now, overloadAmps() * Tuning.MCP_MULTIPLE));
			return true;
		}
		return false;
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = mostAmps(circuit);
		double ratio = amps / overloadAmps();
		double dt = ticks / 20.0;
		double pickup = Tuning.OVERLOAD_PICKUP * Tuning.OVERLOAD_PICKUP;
		if (ratio > Tuning.OVERLOAD_PICKUP) {
			// I-squared-t: at 6 times the setting the relay heats from cold to tripped in the class time
			heat += (ratio * ratio - pickup) / ((36.0 - pickup) * Tuning.OVERLOAD_CLASS_SECONDS) * dt;
		} else {
			heat = Math.max(0, heat - dt / Tuning.OVERLOAD_COOL_SECONDS);
		}
		if (heat >= 1.0 && run() == MotorStarterBlock.Run.RUNNING) {
			trip(String.format("overload: %.1f A on the %.1f A setting", amps, overloadAmps()));
		}
	}

	private void trip(String why) {
		heat = 0;
		amps = 0;
		lastTrip = why;
		setRun(MotorStarterBlock.Run.TRIPPED);
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 1.2F);
			CircuitManager.markDirty(server);
		}
	}

	/** A short line for the MCC's readout. */
	public String brief() {
		return switch (run()) {
			case STOPPED -> "stopped";
			case RUNNING -> String.format("running %.1f A", amps);
			case TRIPPED -> "TRIPPED (" + lastTrip + ")";
		};
	}

	@Override
	public Component inspect() {
		String what = block().bucket() ? "MCC bucket" : "Motor starter";
		String bus = block().bucket() && !onBus() ? "NOT ON AN MCC BUS: put it in a line to the right of an incoming section; " : "";
		String state = switch (run()) {
			case STOPPED -> "STOPPED: right-click (or redstone) to start";
			case RUNNING -> String.format("RUNNING, %.1f A", amps);
			case TRIPPED -> "TRIPPED (" + lastTrip + "): fix the motor, then right-click to reset";
		};
		return Component.literal(what + " (" + Voltage.INDUSTRIAL.label() + " three-phase): " + bus + state + String.format(", overload set to %.1f A", overloadAmps())
				+ (heat > 0.05 ? ", relay heating " + Math.round(heat * 100) + "%" : "") + "; sneak and right-click steps the overload dial");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("setting", setting);
		output.putDouble("heat", heat);
		output.putString("lastTrip", lastTrip);
		output.putBoolean("powered", powered);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		setting = input.getIntOr("setting", Tuning.OVERLOAD_DEFAULT);
		heat = input.getDoubleOr("heat", 0);
		lastTrip = input.getStringOr("lastTrip", "");
		powered = input.getBooleanOr("powered", false);
	}
}
