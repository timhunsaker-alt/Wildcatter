package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The receptacle is only a junction: it carries its conductors to the plug adapter set against it. A single-line type takes leg A or B and
 * the neutral; the 240 V type takes both legs and the neutral.
 */
public class ReceptacleBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private int hotA;
	private int hotB;
	private int neutral;
	private double volts;

	public ReceptacleBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RECEPTACLE_ENTITY, pos, state);
	}

	public PlugType type() {
		return ((ReceptacleBlock) getBlockState().getBlock()).type();
	}

	@Override
	public boolean hasPort(Direction face, Conductor conductor, Voltage voltage) {
		return voltage == Voltage.LOW && conductor != Conductor.C && conductor != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		hotA = circuit.node();
		hotB = type().betweenLegs() ? circuit.node() : hotA;   // a single-line receptacle takes either leg on its one line terminal
		neutral = circuit.node();
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Conductor.A, Voltage.LOW, hotA, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Conductor.B, Voltage.LOW, hotB, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Conductor.N, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = Math.abs(circuit.across(hotA, neutral));
	}

	@Override
	public Component inspect() {
		return Component.literal(type().label() + " receptacle (" + Math.round(type().volts()) + " V, " + Math.round(type().amps()) + " A): "
				+ String.format("%.0f V on its line", volts) + "; set a matching plug adapter against it");
	}
}
