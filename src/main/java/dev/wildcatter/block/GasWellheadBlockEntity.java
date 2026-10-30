package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gas needs no power: the pressure in the rock pushes it up. The valve only opens when a firebox is short of fuel, so
 * gas is only taken from the field as it is burned. As the field empties the pressure falls and the flow with it.
 */
public class GasWellheadBlockEntity extends BlockEntity implements Inspectable {
	private double owed;
	private String status = "starting";

	public GasWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GAS_WELLHEAD_ENTITY, pos, state);
	}

	private Reservoir field(ServerLevel level) {
		return Reservoir.at(ReservoirData.seed(level), worldPosition.getX(), worldPosition.getZ());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, GasWellheadBlockEntity head) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || !(level instanceof ServerLevel server)) {
			return;
		}
		Reservoir field = head.field(server);
		ReservoirData data = ReservoirData.get(server);
		if (field == null || data.remaining(field) <= 0) {
			head.status = "DEPLETED: the well has run dry";
			return;
		}
		List<BlockPos> cell = List.of(pos);
		Set<BlockPos> lines = Plumbing.network(level, cell, PipeBlock.Kind.GAS);
		List<FireboxBlockEntity> fireboxes = Plumbing.fireboxes(level, cell, lines, true);
		if (fireboxes.isEmpty()) {
			head.status = "valve shut: no firebox on its gas lines";
			return;
		}
		double flow = Tuning.GAS_UNITS_PER_TICK * Tuning.WELL_INTERVAL * Math.max(0.1, Math.min(1.0, data.pressure(field) * 1.25));
		double used = Plumbing.feedFireboxes(fireboxes, flow * Tuning.GAS_FUEL_TICKS_PER_UNIT) / Tuning.GAS_FUEL_TICKS_PER_UNIT;
		head.owed += used;
		int whole = (int) head.owed;
		if (whole > 0) {
			data.draw(field, whole);
			head.owed -= whole;
			head.setChanged();
		}
		head.status = used > 0 ? "feeding " + fireboxes.size() + " firebox(es)" : "valve shut: the fireboxes are full or idle";
	}

	@Override
	public Component inspect() {
		String pressure = "unknown pressure";
		if (level instanceof ServerLevel server) {
			Reservoir field = field(server);
			if (field != null) {
				ReservoirData data = ReservoirData.get(server);
				pressure = Math.round(data.pressure(field) * 100) + "% pressure, about " + data.remaining(field) + " units of gas left";
			}
		}
		return Component.literal("Gas wellhead: " + pressure + ", " + status);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("owed", owed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owed = input.getDoubleOr("owed", 0);
	}
}
