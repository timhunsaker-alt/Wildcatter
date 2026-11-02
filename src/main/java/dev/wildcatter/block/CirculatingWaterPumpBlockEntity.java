package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A circulating water pump at the intake (docs/multiblocks/turbine-island.md): a vertical pump whose bottom 3 cells stand in water (a
 * lake or river, at least 12 water blocks around them), its 300 kW 4.16 kV motor on top. It gives 1,250 kg/s of circulating water on
 * the east face of its cells at height 3, ground level; its motor box is on the west face of the cell at height 3 over its main cell.
 */
public class CirculatingWaterPumpBlockEntity extends UnitMotorBlockEntity {
	private int water;
	private String unit = "starting";

	public CirculatingWaterPumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CW_PUMP_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 3 && side == Direction.EAST ? Product.COOLING_WATER : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.CW_PUMP_KGS * 2;
	}

	/** The water blocks touching its bottom 3 layers. */
	private int countWater(Level level) {
		List<BlockPos> cells = BigParts.cellsAt(level, worldPosition);
		Set<BlockPos> own = new HashSet<>(cells);
		Set<BlockPos> wet = new HashSet<>();
		for (BlockPos cell : cells) {
			if (cell.getY() - worldPosition.getY() > 2) {
				continue;
			}
			for (Direction d : Direction.values()) {
				BlockPos next = cell.relative(d);
				if (!own.contains(next) && level.getFluidState(next).is(FluidTags.WATER)) {
					wet.add(next.immutable());
				}
			}
		}
		return wet.size();
	}

	@Override
	protected void work(Level level) {
		water = countWater(level);
		if (water < Tuning.CW_INTAKE_WATER_BLOCKS) {
			unit = String.format("NO WATER: its suction must stand in a lake or river (%d of %d water blocks round its bottom 3 cells)", water,
					Tuning.CW_INTAKE_WATER_BLOCKS);
			return;
		}
		double room = room(Product.COOLING_WATER);
		if (room <= 1e-6) {
			unit = "idle: the condenser is not drawing";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Math.min(Tuning.CW_PUMP_KGS * seconds() * strength, room);
		add(Product.COOLING_WATER, kg);
		unit = String.format("pumping %,.0f kg/s", kg / seconds());
	}

	private BlockPos boxCell() {
		return worldPosition.above(3);
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(boxCell()) && face == LeasePartBlock.world(getBlockState(), Direction.WEST) && voltage == motor().voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor().build(circuit, boxCell(), List.of(LeasePartBlock.world(getBlockState(), Direction.WEST)));
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Circulating water pump (%.0f kW, %s): %s; %s", Tuning.CW_PUMP_WATTS / 1000, motor().voltage().label(), unit, describe()));
	}
}
