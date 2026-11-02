package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The fluorine cells' rectifier (docs/multiblocks/conversion.md): its rectifier transformer and 12-pulse diode bridge on 4.16 kV
 * three-phase (its box on the west face of its main cell), a variable AC load at 0.95 power factor. It drives the DC through the
 * cell house it touches by its bus bars; the DC side is worked out here, not in the solver (decided 2026-11-01).
 */
public class RectifierBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final DriveLoad load = new DriveLoad(Voltage.MEDIUM, Tuning.RECTIFIER_POWER_FACTOR, 0);
	private double wanted;   // AC watts the cell house asks for
	private double dcVolts;

	public RectifierBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RECTIFIER_ENTITY, pos, state);
	}

	/** The cell house asks for {@code dcWatts} at {@code volts}; returns the share it gets, 0 to 1. */
	public double supply(double dcWatts, double volts) {
		wanted = dcWatts / Tuning.RECTIFIER_EFFICIENCY;
		dcVolts = volts;
		load.setDemand(wanted);
		return load.undervoltage() ? 0 : load.delivered();
	}

	public double dcAmps() {
		return dcVolts > 0 ? load.watts() * Tuning.RECTIFIER_EFFICIENCY / dcVolts : 0;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private Direction boxFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == boxFace() && voltage == Voltage.MEDIUM && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		load.setDemand(wanted);
		load.build(circuit, worldPosition, List.of(boxFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Rectifier (12-pulse, %s in): DC %.0f V, %.2f kA to the cells; AC %s", Voltage.MEDIUM.label(), dcVolts, dcAmps() / 1000,
				load.describe()));
	}
}
