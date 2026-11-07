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
	private int hot;
	private int neutral;
	private double volts;

	public ControlSupplyBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CONTROL_SUPPLY_ENTITY, pos, state);
	}

	public boolean live() {
		return volts >= Voltage.LOW.nominal() * DROPOUT;
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
		return Component.literal("24 V DC control supply, 10 A: " + (live() ? "making 24 V" : "NO 24 V: it needs " + Voltage.LOW.label() + " hot and neutral")
				+ String.format(", mains %.0f V", volts));
	}
}
