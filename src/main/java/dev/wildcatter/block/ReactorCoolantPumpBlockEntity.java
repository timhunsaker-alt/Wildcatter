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
	private boolean on = true;
	private double flow;
	private double power;
	private String held = "";

	public ReactorCoolantPumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REACTOR_COOLANT_PUMP_ENTITY, pos, state);
	}

	/** The vessel's call each interval: runs it if switched on and {@code permit}; returns nothing, read {@link #flow()}. */
	public void operate(Level level, boolean permit, String why, double dt) {
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
