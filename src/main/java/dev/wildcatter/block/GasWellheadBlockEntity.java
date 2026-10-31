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
public class GasWellheadBlockEntity extends BlockEntity implements Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	private double owed;
	private boolean grown;
	/** Gas this well can still flow before its next interval. */
	private double allowance;
	private String status = "starting";

	public GasWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GAS_WELLHEAD_ENTITY, pos, state);
	}

	private Reservoir field(ServerLevel level) {
		return Reservoir.at(ReservoirData.seed(level), worldPosition.getX(), worldPosition.getZ());
	}

	/**
	 * Gas for a generator on the gas lines: gives up to {@code units}, within what the well can flow this interval
	 * (fireboxes are served first, so this is what they left).
	 */
	public double offer(double units) {
		if (!(level instanceof ServerLevel server)) {
			return 0;
		}
		Reservoir field = field(server);
		if (field == null) {
			return 0;
		}
		double give = Math.min(Math.min(units, allowance), ReservoirData.get(server).remaining(field) - owed);
		if (give <= 0) {
			return 0;
		}
		allowance -= give;
		owed += give;
		settle(server, field);
		return give;
	}

	private void settle(ServerLevel server, Reservoir field) {
		int whole = (int) owed;
		if (whole > 0) {
			ReservoirData.get(server).draw(field, whole);
			owed -= whole;
			setChanged();
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, GasWellheadBlockEntity head) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || !(level instanceof ServerLevel server)) {
			return;
		}
		Reservoir field = head.field(server);
		ReservoirData data = ReservoirData.get(server);
		if (field == null || data.remaining(field) <= 0) {
			head.status = "DEPLETED: the well has run dry";
			head.allowance = 0;
			return;
		}
		head.allowance = Tuning.GAS_UNITS_PER_TICK * Tuning.WELL_INTERVAL * Math.max(0.1, Math.min(1.0, data.pressure(field) * 1.25));
		List<BlockPos> cell = List.of(pos);
		Set<BlockPos> lines = Plumbing.network(level, cell, PipeBlock.Kind.GAS);
		List<FireboxBlockEntity> fireboxes = Plumbing.fireboxes(level, cell, lines, true);
		double used = Plumbing.feedFireboxes(fireboxes, head.allowance * Tuning.GAS_FUEL_TICKS_PER_UNIT) / Tuning.GAS_FUEL_TICKS_PER_UNIT;
		head.allowance -= used;
		head.owed += used;
		head.settle(server, field);
		head.status = used > 0 ? "feeding " + fireboxes.size() + " firebox(es)"
				: fireboxes.isEmpty() ? "valve shut: no firebox on its gas lines (a gas turbine can still draw from it)" : "valve shut: the fireboxes are full or idle";
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
		return Component.literal("Gas wellhead (Christmas tree): " + pressure + ", " + status);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("owed", owed);
		output.putBoolean("grown", grown);
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
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owed = input.getDoubleOr("owed", 0);
		grown = input.getBooleanOr("grown", grown);
	}
}
