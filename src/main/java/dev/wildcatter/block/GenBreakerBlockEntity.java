package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The generator circuit breaker (docs/multiblocks/gas-turbine.md): a 15 kV vacuum breaker, 1,200 A, 40 kA, in its enclosure against
 * a generator's terminal end. It is the generator's synchronizing breaker: the generator closes it once in step and opens it when it
 * trips or stops. Its front (north) faces take the generator; the bus duct leaves its back (south).
 */
public class GenBreakerBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private static final Conductor[] CONDUCTORS = {Conductor.A, Conductor.B, Conductor.C, Conductor.N};
	private final int[] node = new int[4];
	private boolean grown;
	private double amps;

	public GenBreakerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GEN_BREAKER_ENTITY, pos, state);
	}

	private List<Lines.Face> ends() {
		List<Lines.Face> out = new ArrayList<>(LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == 0 && at[1] == 0, Direction.NORTH));
		out.addAll(LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == 1 && at[1] == 0, Direction.SOUTH));
		return out;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.GENERATOR && role != Conductor.G && ends().contains(new Lines.Face(cell, face));
	}

	/** Its bus voltage transformers, wye-grounded: a megohm from each conductor to earth. They also give the 13.8 kV bus its ground
	 * reference while the generator is off (the GSU's delta and the bus duct would float). */
	private static final double VT_OHMS = 1.0e6;

	@Override
	public void build(Circuit circuit) {
		int earth = circuit.node();
		circuit.ground(earth);
		for (int k = 0; k < 4; k++) {
			node[k] = circuit.node();
			circuit.resistor(node[k], earth, VT_OHMS);
			for (Lines.Face f : ends()) {
				circuit.port(f.cell(), f.side(), CONDUCTORS[k], Voltage.GENERATOR, node[k], Tuning.CONTACT_OHMS, false);
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		amps = circuit.through(node[0]);
	}

	@Override
	public Component inspect() {
		GeneratorPartBlockEntity gen = level == null ? null : Touching.find(level, worldPosition, GeneratorPartBlockEntity.class, g -> true);
		String state = gen == null ? "no generator against it" : gen.breakerClosed() ? String.format("CLOSED, carrying %.0f A", amps) : "OPEN (" + gen.status() + ")";
		return Component.literal("Generator circuit breaker (15 kV vacuum, 1,200 A, 40 kA): " + state);
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
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
	}
}
