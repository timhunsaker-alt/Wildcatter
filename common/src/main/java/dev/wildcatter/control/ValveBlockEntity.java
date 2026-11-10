package dev.wildcatter.control;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.Starter;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A valve's actuator and position. An air valve moves on air and its spring takes it to its fail position when air is lost. A motor-operated
 * valve moves only on power, and stays put without it. Every valve starts in HAND, shut.
 */
public class ValveBlockEntity extends BlockEntity implements ControlDevice, CircuitPart, Inspectable, Measurable {
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.VALVE_ACTUATOR_WATTS);
	private double position;
	/** Where a hand has set it. */
	private double handTarget;
	private Hoa hoa = Hoa.HAND;
	private boolean failOpen;
	private boolean air;
	private double signalMa;
	private long signalTick = -100;
	private boolean commandOpen;
	private boolean commandClose;
	private long commandTick = -100;
	private boolean moving;

	public ValveBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.VALVE_ENTITY, pos, state);
	}

	public ValveBlock.Type type() {
		return ((ValveBlock) getBlockState().getBlock()).type();
	}

	/** 0 shut to 1 full open. */
	public double position() {
		return position;
	}

	public boolean hasAir() {
		return air;
	}

	public void setHoaForTest(Hoa next) {
		hoa = next;
	}

	private boolean electric() {
		return type() == ValveBlock.Type.MOTOR_OPERATED;
	}

	/** A hand on it, in HAND: a control valve's handwheel goes by quarters, the others open and shut. */
	public void hand() {
		if (hoa != Hoa.HAND) {
			return;
		}
		handTarget = type() == ValveBlock.Type.CONTROL ? (handTarget >= 1 ? 0 : handTarget + 0.25) : handTarget > 0 ? 0 : 1;
		setChanged();
	}

	public void toggleFailMode() {
		if (!electric()) {
			failOpen = !failOpen;
			setChanged();
		}
	}

	private double stroke() {
		return switch (type()) {
			case CONTROL -> Tuning.CONTROL_VALVE_STROKE_SECONDS;
			case ON_OFF -> Tuning.ON_OFF_VALVE_STROKE_SECONDS;
			case MOTOR_OPERATED -> Tuning.MOV_STROKE_SECONDS;
		};
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ValveBlockEntity valve) {
		long now = level.getGameTime();
		if ((now + pos.hashCode()) % 20 == 0 && !valve.electric()) {
			valve.air = AirCompressorBlockEntity.pressureAt(level, pos) >= Tuning.AIR_MIN_BAR;
		}
		boolean fresh = now - valve.commandTick <= Starter.COMMAND_STALE_TICKS;
		boolean signal = now - valve.signalTick <= Starter.COMMAND_STALE_TICKS && valve.signalMa >= 3.6;
		double fail = valve.failOpen ? 1 : 0;
		double target = switch (valve.type()) {
			case CONTROL -> !valve.air ? fail : valve.hoa == Hoa.HAND ? valve.handTarget : valve.hoa == Hoa.AUTO && signal ? Math.max(0, Math.min(1, (valve.signalMa - 4) / 16)) : fail;
			case ON_OFF -> !valve.air ? fail : valve.hoa == Hoa.HAND ? valve.handTarget : valve.hoa == Hoa.AUTO && fresh && valve.commandOpen ? (valve.failOpen ? 0 : 1) : fail;
			case MOTOR_OPERATED -> valve.hoa == Hoa.HAND ? valve.handTarget
					: valve.hoa == Hoa.AUTO && fresh ? valve.commandOpen && !valve.commandClose ? 1 : valve.commandClose && !valve.commandOpen ? 0 : valve.position : valve.position;
		};
		double step = 1.0 / (20 * valve.stroke());
		boolean wantsMove = Math.abs(target - valve.position) > step / 2;
		boolean wasMoving = valve.moving;
		valve.moving = wantsMove;
		if (valve.electric() && wasMoving != wantsMove && level instanceof net.minecraft.server.level.ServerLevel server) {
			dev.wildcatter.circuit.CircuitManager.markDirty(server);
		}
		if (wantsMove && (!valve.electric() || valve.motor.output() > 0)) {
			double before = valve.position;
			valve.position = target > valve.position ? Math.min(target, valve.position + step) : Math.max(target, valve.position - step);
			if ((before <= 0) != (valve.position <= 0)) {
				level.updateNeighborsAt(pos, state.getBlock());
			}
			valve.setChanged();
		}
	}

	// ---- signals ---------------------------------------------------------------------------------------------------------------------

	@Override
	public String deviceName() {
		return switch (type()) {
			case CONTROL -> "Control valve";
			case ON_OFF -> "On-off valve";
			case MOTOR_OPERATED -> "Motor-operated valve";
		};
	}

	@Override
	public List<Point> points() {
		Point opened = new Point("OPENED", false);
		Point closed = new Point("CLOSED", false);
		Point auto = new Point("AUTO", false);
		return switch (type()) {
			case CONTROL -> List.of(new Point("SIGNAL 4-20 mA", true, Signal.MA), new Point("POSITION 4-20 mA", false, Signal.MA), opened, closed, auto);
			case ON_OFF -> List.of(new Point("SOLENOID", true), opened, closed, auto);
			case MOTOR_OPERATED -> List.of(new Point("OPEN", true), new Point("CLOSE", true), opened, closed, auto);
		};
	}

	/** An on-off valve's 24 V solenoid pilot, 4.8 W; a control valve's positioner lives on its loop, and a motor-operated valve's motor on the mains. */
	@Override
	public double outputAmps(int point) {
		return type() == ValveBlock.Type.ON_OFF && point == 0 ? 0.2 : 0;
	}

	@Override
	public boolean readPoint(int point) {
		String name = point < points().size() ? points().get(point).name() : "";
		return switch (name) {
			case "OPENED" -> position >= 0.98;
			case "CLOSED" -> position <= 0.02;
			case "AUTO" -> hoa == Hoa.AUTO;
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (type() == ValveBlock.Type.CONTROL) {
			return;
		}
		if (point == 0) {
			commandOpen = on;
			commandTick = gameTime;
		} else if (point == 1 && electric()) {
			commandClose = on;
			commandTick = gameTime;
		}
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (type() == ValveBlock.Type.CONTROL && point == 0) {
			signalMa = milliamps;
			signalTick = gameTime;
		}
	}

	@Override
	public double readMa(int point) {
		return 4 + 16 * position;
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		hoa = hoa.next();
		setChanged();
		return hoa.name();
	}

	@Override
	public double measure(Quantity quantity) {
		return quantity == Quantity.POSITION ? position * 100 : Double.NaN;
	}

	// ---- the actuator's motor --------------------------------------------------------------------------------------------------------

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return electric() && voltage == motor.voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		if (electric()) {
			motor.build(circuit, worldPosition);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (electric()) {
			motor.apply(circuit, ticks, moving);
		}
	}

	@Override
	public Component inspect() {
		String drive = electric() ? (motor.output() > 0 ? "actuator powered" : moving ? "NO POWER, staying put" : "actuator idle")
				: (air ? "air OK" : "NO AIR, at its fail position") + ", fails " + (failOpen ? "open" : "shut") + " (sneak-click to change)";
		return Component.literal(String.format(Locale.ROOT, "%s [%s]: %.0f %% open, %s", deviceName(), hoa, position * 100, drive));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("position", position);
		output.putDouble("handTarget", handTarget);
		output.putString("hoa", hoa.name());
		output.putBoolean("failOpen", failOpen);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		position = input.getDoubleOr("position", 0);
		handTarget = input.getDoubleOr("handTarget", 0);
		hoa = Hoa.valueOf(input.getStringOr("hoa", Hoa.HAND.name()));
		failOpen = input.getBooleanOr("failOpen", false);
	}
}
