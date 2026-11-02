package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Stream;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Gas needs no power: the pressure in the rock pushes it up. The choke only opens as far as what draws on its steel pipe takes (a
 * firebox short of fuel, a separator with room), so gas is only taken from the field as it is used. It comes up wet, with its
 * condensate and a little water (docs/multiblocks/lease.md). As the field empties the pressure falls and the flow with it.
 */
public class GasWellheadBlockEntity extends BlockEntity implements Inspectable, dev.wildcatter.multiblock.BigPartEntity, WellSource {
	private double owed;
	private boolean grown;
	/** Gas this well can still flow before its next interval. */
	private double allowance;
	/** Gas that has flowed since the last interval, for the readout. */
	private double flowed;
	private String status = "starting";

	public GasWellheadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GAS_WELLHEAD_ENTITY, pos, state);
	}

	private Reservoir field(ServerLevel level) {
		return Reservoir.at(ReservoirData.seed(level), worldPosition.getX(), worldPosition.getZ());
	}

	/** Gas for whatever draws on its line: gives up to {@code units} m3, within what the well can flow this interval. */
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
		flowed += give;
		settle(server, field);
		return give;
	}

	// ---- its wet gas on the steel pipe from the tree's bottom cell ------------------------------------------------------------
	@Override
	public Stream.Split drain(double m3) {
		return Stream.gasWell(offer(m3));
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return cell.equals(worldPosition) ? Product.WET_GAS : null;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		if (product != Product.WET_GAS || pull == Pull.FLARE || !(level instanceof ServerLevel server)) {
			return 0;   // a well's gas is never flared: the tree is shut in instead
		}
		Reservoir field = field(server);
		return field == null ? 0 : Math.max(0, Math.min(allowance, ReservoirData.get(server).remaining(field) - owed));
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		return available(cell, side, product, pull) > 0 ? offer(max) : 0;
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
		head.status = head.flowed > 0 ? String.format("flowing %.2f m3/s of wet gas", head.flowed * 20.0 / Tuning.WELL_INTERVAL)
				: "choke shut: nothing on its steel pipe is taking gas (a firebox or a separator)";
		head.flowed = 0;
		head.allowance = Tuning.GAS_UNITS_PER_TICK * Tuning.WELL_INTERVAL * Math.max(0.1, Math.min(1.0, data.pressure(field) * 1.25));
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
