package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A resistive load bank at 13.8 kV (docs/multiblocks/gas-turbine.md): 0 to 10 MW in 1 MW steps, three resistor elements in delta.
 * Plants are commissioned against one. Its terminals are the north face of its front bottom cells.
 */
public class LoadBankBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	private final int[] hot = new int[3];
	private final int[] element = {-1, -1, -1};
	private int steps;
	private double watts;
	private boolean grown;

	public LoadBankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LOAD_BANK_ENTITY, pos, state);
	}

	/** Right-click: a step up; sneak and right-click: a step down. */
	public Component step(boolean up) {
		steps = Math.max(0, Math.min(Tuning.LOAD_BANK_STEPS, steps + (up ? 1 : -1)));
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
		return Component.literal(String.format("Load bank set to %d MW", steps));
	}

	public void setSteps(int value) {
		steps = Math.max(0, Math.min(Tuning.LOAD_BANK_STEPS, value));
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	private List<Lines.Face> terminals() {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.GENERATOR && role != Conductor.N && role != Conductor.G && terminals().contains(new Lines.Face(cell, face));
	}

	@Override
	public void build(Circuit circuit) {
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
			element[k] = -1;
		}
		if (steps > 0) {
			double lineToLine = Voltage.GENERATOR.lineToLine();
			double ohms = lineToLine * lineToLine / (steps * Tuning.LOAD_BANK_STEP_WATTS / 3.0);
			for (int k = 0; k < 3; k++) {
				element[k] = circuit.resistor(hot[k], hot[(k + 1) % 3], ohms);
			}
		}
		for (Lines.Face f : terminals()) {
			for (int k = 0; k < 3; k++) {
				circuit.port(f.cell(), f.side(), PHASES[k], Voltage.GENERATOR, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		watts = 0;
		for (int k = 0; k < 3; k++) {
			if (element[k] >= 0) {
				watts += circuit.across(hot[k], hot[(k + 1) % 3]) * circuit.current(element[k]);
			}
		}
	}

	public double watts() {
		return watts;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Load bank (%s, resistive): set to %d MW, taking %.2f MW (right-click a step up, sneak and right-click a step down)",
				Voltage.GENERATOR.label(), steps, watts / 1e6));
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
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putInt("steps", steps);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
		steps = input.getIntOr("steps", 0);
	}
}
