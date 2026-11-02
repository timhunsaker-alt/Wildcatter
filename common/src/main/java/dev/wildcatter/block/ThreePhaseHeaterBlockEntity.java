package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The three delta elements of a {@link ThreePhaseHeaterBlock}: each is rated a third of the heater at line-to-line volts. */
public class ThreePhaseHeaterBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	private final int[] hot = new int[3];
	private final int[] element = new int[3];
	private final double[] lineVolts = new double[3];
	private final double[] elementAmps = new double[3];
	private double watts;

	public ThreePhaseHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.THREE_PHASE_HEATER_ENTITY, pos, state);
	}

	private ThreePhaseHeaterBlock block() {
		return (ThreePhaseHeaterBlock) getBlockState().getBlock();
	}

	public boolean isLive() {
		return Math.max(lineVolts[0], Math.max(lineVolts[1], lineVolts[2])) > Tuning.SHOCK_MIN_VOLTS;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == block().voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		Voltage voltage = block().voltage();
		double lineToLine = voltage.lineToLine();
		double ohms = lineToLine * lineToLine / (block().watts() / 3.0);   // each element: a third of the power at line-to-line volts
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
		}
		for (int k = 0; k < 3; k++) {
			element[k] = circuit.resistor(hot[k], hot[(k + 1) % 3], ohms);   // A-B, B-C, C-A
		}
		for (Direction face : Direction.values()) {
			for (int k = 0; k < 3; k++) {
				circuit.port(worldPosition, face, PHASES[k], voltage, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		watts = 0;
		for (int k = 0; k < 3; k++) {
			lineVolts[k] = circuit.across(hot[k], hot[(k + 1) % 3]);
			elementAmps[k] = circuit.current(element[k]);
			watts += lineVolts[k] * elementAmps[k];
		}
		boolean working = watts > block().watts() * Tuning.BRIGHT_ABOVE * Tuning.BRIGHT_ABOVE;
		BlockState state = getBlockState();
		if (level instanceof ServerLevel server && state.getValue(LoadBlock.LIT) != working) {
			server.setBlock(worldPosition, state.setValue(LoadBlock.LIT, working), 3);
		}
	}

	public double watts() {
		return watts;
	}

	@Override
	public Component inspect() {
		Voltage voltage = block().voltage();
		String state = watts < 1.0 ? "NO POWER: it needs phases A, B and C"
				: watts < block().watts() * 0.75 ? "UNDER-POWERED: a phase is missing or the voltage is low" : "running";
		return Component.literal(String.format("Three-phase heater (rated %.0f kW at %s): A-B %.0f V, B-C %.0f V, C-A %.0f V, %.0f W, %s; %s",
				block().watts() / 1000.0, voltage.label(), lineVolts[0], lineVolts[1], lineVolts[2], watts, state,
				level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
