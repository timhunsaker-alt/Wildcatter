package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A reactor coolant pump (docs/multiblocks/reactor.md): a shaft-seal pump with its 500 kW, 4.16 kV motor and flywheel, against its
 * steam generator. The vessel runs it while the RCS is full and holds 2.3 MPa for its seals. Its flow comes up over 10 s; with its
 * power lost the flywheel carries it down, halving the flow in about 10 s. A sneak-click switches it to stop or to start.
 */
public class ReactorCoolantPumpBlockEntity extends UnitMotorBlockEntity {
	private final dev.wildcatter.control.Remote pumpRemote = new dev.wildcatter.control.Remote(1);

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("START", true), new Point("RUNNING", false), new Point("AUTO", false), new Point("FLOW 4-20 mA", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 1 ? power > 0.3 : point == 2 && pumpRemote.auto();
	}

	/** In AUTO its switch follows the PLC. Its seal permit still rules: {@link #operate} holds it however it is told. */
	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			pumpRemote.command(0, on, gameTime);
			if (pumpRemote.auto()) {
				this.on = on;
			}
		}
	}

	@Override
	public double readMa(int point) {
		return 4 + 16 * flow;
	}

	@Override
	public String cycleHoa(net.minecraft.core.BlockPos cell) {
		setChanged();
		return pumpRemote.cycle();
	}

	@Override
	protected boolean plcHold(Level level) {
		return false;   // a coolant pump is not a unit to be held: it runs from its switch and its permit
	}

	private boolean on = true;
	private double flow;
	private double power;
	private String held = "";

	public ReactorCoolantPumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REACTOR_COOLANT_PUMP_ENTITY, pos, state);
	}

	/** The vessel's call each interval: runs it if switched on and {@code permit}; returns nothing, read {@link #flow()}. */
	public void operate(Level level, boolean permit, String why, double dt) {
		if (!pumpRemote.hand() && !pumpRemote.on(0, level.getGameTime())) {
			on = false;   // OFF, or a PLC gone quiet: it stops, and the reactor's own low-flow trip does the rest
		}
		held = on && !permit ? why : "";
		power = on && permit ? drive(level) : 0;
		if (power > 0.3) {   // running, a voltage sag or not: only a stopped motor lets the flywheel carry it down
			flow = Math.min(1, flow + dt / 10);
		} else {
			flow = flow / (1 + flow * dt / Tuning.RCP_COASTDOWN_S);
			if (flow < 0.005) {
				flow = 0;
			}
		}
		setChanged();
	}

	/** 0 to 1 of the loop's full flow. */
	public double flow() {
		return flow;
	}

	/** 0 to 1: how hard its motor runs. */
	public double power() {
		return power;
	}

	public boolean on() {
		return on;
	}

	public String toggle() {
		on = !on;
		setChanged();
		return "Reactor coolant pump: switched to " + (on ? "START" : "STOP");
	}

	public void setOnForTest(boolean on) {
		this.on = on;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Reactor coolant pump (%.0f kW, %s): switch %s, flow %.0f %%%s; %s", Tuning.RCP_WATTS / 1000, motor().voltage().label(),
				on ? "START" : "STOP", flow * 100, held.isEmpty() ? "" : ", HELD: " + held, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("on", on);
		output.putDouble("flow", flow);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		on = input.getBooleanOr("on", true);
		flow = input.getDoubleOr("flow", 0);
	}
}
