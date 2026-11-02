package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The conductors of a station bus: A, B, C, N and G from its west terminal to its east one, each the bus's length of 2,000 A
 * station bus (ConductorSize.STATION_BUS_2000); the ground is tied to earth, as the yard's ground grid is.
 */
public class StationBusBlockEntity extends BlockEntity implements CircuitPart, Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	private static final Conductor[] CONDUCTORS = Conductor.values();
	private static final ConductorSize SIZE = ConductorSize.STATION_BUS_2000;
	private final int[] west = new int[CONDUCTORS.length];
	private final int[] east = new int[CONDUCTORS.length];
	private final int[] link = new int[CONDUCTORS.length];
	private final double[] amps = new double[CONDUCTORS.length];
	private double lineVolts;
	private boolean grown;

	public StationBusBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STATION_BUS_ENTITY, pos, state);
	}

	private StationBusBlock block() {
		return (StationBusBlock) getBlockState().getBlock();
	}

	private Voltage voltage() {
		return block().voltage();
	}

	private Direction westFace() {
		return block().facing(getBlockState()).getCounterClockWise();
	}

	private Direction eastFace() {
		return block().facing(getBlockState()).getClockWise();
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
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage() && (face == westFace() || face == eastFace());
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return hasPort(face, role, voltage) && cell.equals(block().terminalCell(worldPosition, getBlockState(), face));
	}

	@Override
	public void build(Circuit circuit) {
		double ohms = SIZE.ohmsPerMeter() * block().length();
		BlockPos w = block().terminalCell(worldPosition, getBlockState(), westFace());
		BlockPos e = block().terminalCell(worldPosition, getBlockState(), eastFace());
		for (Conductor conductor : CONDUCTORS) {
			int k = conductor.ordinal();
			west[k] = circuit.node();
			east[k] = circuit.node();
			link[k] = circuit.resistor(west[k], east[k], ohms);
			if (conductor == Conductor.G) {
				circuit.ground(west[k]);
			}
			circuit.port(w, westFace(), conductor, voltage(), west[k], dev.wildcatter.Tuning.CONTACT_OHMS / 2, false);
			circuit.port(e, eastFace(), conductor, voltage(), east[k], dev.wildcatter.Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		for (int k = 0; k < CONDUCTORS.length; k++) {
			amps[k] = Math.abs(circuit.current(link[k]));
		}
		lineVolts = circuit.across(west[Conductor.A.ordinal()], west[Conductor.B.ordinal()]);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Station bus (%s, %s): A %.0f A, B %.0f A, C %.0f A, A to B %.0f V; %.0f A allowed",
				voltage().label(), SIZE.label(), amps[Conductor.A.ordinal()], amps[Conductor.B.ordinal()], amps[Conductor.C.ordinal()], lineVolts, SIZE.amps()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
	}
}
