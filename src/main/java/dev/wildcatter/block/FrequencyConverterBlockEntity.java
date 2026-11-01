package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Enrichment;
import dev.wildcatter.fluid.Product;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The cascade's frequency converter (docs/multiblocks/enrichment.md): an MV frequency changer lineup on 12.47 kV (its box on the west
 * face of its main cell) making the machines' 1,225 Hz. It drives up to 12 centrifuges on the header at the south face of its back
 * cells, their power running along the header; a variable load at 0.95 power factor with the land rig's power management.
 */
public class FrequencyConverterBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final DriveLoad load = new DriveLoad(Voltage.DISTRIBUTION, Tuning.CONVERTER_POWER_FACTOR, Tuning.LAND_RIG_FREQUENCY_FLOOR);
	private int machines;

	public FrequencyConverterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FREQUENCY_CONVERTER_ENTITY, pos, state);
	}

	private List<Lines.Face> header() {
		return faces(at -> at[2] == 5 && at[1] == 0, Direction.SOUTH);
	}

	/** The centrifuges joined by their top cells to the runs on {@code faces}, each once. */
	static List<CentrifugeBlockEntity> centrifuges(Level level, List<Lines.Face> faces) {
		List<CentrifugeBlockEntity> out = new ArrayList<>();
		for (Lines.Line line : Lines.lines(level, faces)) {
			for (Lines.Port port : line.ports()) {
				if (port.node() instanceof CentrifugeBlockEntity c && c.header(port.cell()) && !out.contains(c)) {
					out.add(c);
				}
			}
		}
		return out;
	}

	public int machines() {
		return machines;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FrequencyConverterBlockEntity f) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		long now = level.getGameTime();
		List<CentrifugeBlockEntity> driven = new ArrayList<>();
		for (CentrifugeBlockEntity c : centrifuges(level, f.header())) {
			if (driven.size() < Tuning.CONVERTER_MACHINES && c.free(pos, now)) {
				driven.add(c);
			}
		}
		f.machines = driven.size();
		f.load.setDemand(f.machines * Enrichment.machineWatts());
		double share = f.load.undervoltage() ? 0 : f.load.delivered();
		for (CentrifugeBlockEntity c : driven) {
			c.drive(pos, share, now);
		}
		f.status = f.machines == 0 ? "NO MACHINES: set centrifuges on its header (the pipe at the south face of its back)"
				: String.format("driving %d machines at %.0f Hz (%.1f MW wanted)", f.machines, Tuning.CENTRIFUGE_HERTZ * share, f.machines * Enrichment.machineWatts() / 1e6);
		f.setChanged();
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	// ---- its 12.47 kV supply --------------------------------------------------------------------------------------------------
	private Direction boxFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == boxFace() && voltage == Voltage.DISTRIBUTION && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		load.setDemand(machines * Enrichment.machineWatts());
		load.build(circuit, worldPosition, List.of(boxFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Frequency converter (%s in, %,.0f Hz out): %s; %s", Voltage.DISTRIBUTION.label(), Tuning.CENTRIFUGE_HERTZ, status, load.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("machines", machines);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		machines = input.getIntOr("machines", 0);
	}
}
