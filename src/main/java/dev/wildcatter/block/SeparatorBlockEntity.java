package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Stream;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.List;
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
 * A 36 in x 10 ft horizontal three-phase production separator (docs/multiblocks/lease.md). It draws the well stream in at its front
 * end and lets it settle: gas off the top, oil over the weir, water from the bottom, each out of its own nozzle onto its own run of
 * steel pipe. It needs no power. The gas side's back-pressure valve opens to a flare only when it is three quarters full, and it
 * takes a gas well's gas only while its gas side is under half full, so a gas well is never flared. When a side has nowhere to go
 * it fills and the separator shuts in.
 */
public class SeparatorBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, FluidNode {
	private double oil;
	private double water;
	private double gas;
	/** What came in over the last interval, for the readout. */
	private Stream.Split came = Stream.Split.NONE;
	private String status = "starting";
	private boolean grown;

	public SeparatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SEPARATOR_ENTITY, pos, state);
	}

	// ---- its nozzles, in its own frame -----------------------------------------------------------------------------------------
	private List<Lines.Face> inlet() {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == 0, Direction.NORTH);
	}

	private @Nullable Product nozzle(BlockPos cell, Direction side) {
		int[] at = LeasePartBlock.local(worldPosition, getBlockState(), cell);
		if (at[2] != 3) {
			return null;
		}
		if (at[1] == 1 && side == LeasePartBlock.world(getBlockState(), Direction.UP)) {
			return Product.GAS;
		}
		if (side != LeasePartBlock.world(getBlockState(), Direction.SOUTH)) {
			return null;
		}
		return at[1] == 1 ? Product.CRUDE : Product.PRODUCED_WATER;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return nozzle(cell, side);
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		if (nozzle(cell, side) != product) {
			return 0;
		}
		return switch (product) {
			case GAS -> pull == Pull.FLARE ? Math.max(0, gas - Tuning.SEPARATOR_GAS_HOLD * Tuning.SEPARATOR_FLARE_ABOVE) : gas;
			case CRUDE -> pull == Pull.FLARE ? 0 : oil;
			case PRODUCED_WATER -> pull == Pull.FLARE ? 0 : water;
			default -> 0;
		};
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		double got = Math.min(max, available(cell, side, product, pull));
		if (got <= 0) {
			return 0;
		}
		switch (product) {
			case GAS -> gas -= got;
			case CRUDE -> oil -= got;
			default -> water -= got;
		}
		setChanged();
		return got;
	}

	/** Game test hook. */
	public double oil() {
		return oil;
	}

	// ---- separating -----------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, SeparatorBlockEntity sep) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || !(level instanceof ServerLevel)) {
			return;
		}
		double seconds = Tuning.WELL_INTERVAL / 20.0;
		double liquidLeft = Tuning.SEPARATOR_LIQUID_LPS * seconds, gasLeft = Tuning.SEPARATOR_GAS_M3S * seconds;
		Stream.Split in = Stream.Split.NONE;
		boolean wells = false;
		for (Lines.Line line : Lines.lines(level, sep.inlet())) {
			Product product = line.run().product();
			if (product != Product.WELL_FLUID && product != Product.WET_GAS) {
				continue;
			}
			for (Lines.Port port : Lines.sources(line, product, Pull.MACHINE, sep)) {
				if (!(port.node() instanceof WellSource well)) {
					continue;
				}
				wells = true;
				double oilRoom = Tuning.SEPARATOR_OIL_HOLD - sep.oil - in.oil();
				double waterRoom = Tuning.SEPARATOR_WATER_HOLD - sep.water - in.water();
				double gasRoom = Tuning.SEPARATOR_GAS_HOLD - sep.gas - in.gas();
				double amount;
				if (product == Product.WELL_FLUID) {   // the worst case of the stream must fit every side
					amount = Math.min(Math.min(liquidLeft, Math.min(oilRoom, waterRoom)), gasRoom / Tuning.GAS_OIL_RATIO);
				} else if (sep.gas + in.gas() < Tuning.SEPARATOR_GAS_HOLD * Tuning.SEPARATOR_GAS_WELLS_BELOW) {
					amount = Math.min(Math.min(gasLeft, Tuning.SEPARATOR_GAS_HOLD * Tuning.SEPARATOR_GAS_WELLS_BELOW - sep.gas - in.gas()),
							Math.min(oilRoom / Tuning.CONDENSATE_PER_M3, waterRoom / Tuning.GAS_WELL_WATER_PER_M3));
				} else {
					amount = 0;
				}
				if (amount > 0) {
					Stream.Split got = well.drain(amount);
					in = in.plus(got);
					liquidLeft -= got.liquid();
					gasLeft -= got.gas();
				}
			}
		}
		sep.oil = Math.min(Tuning.SEPARATOR_OIL_HOLD, sep.oil + in.oil());
		sep.water = Math.min(Tuning.SEPARATOR_WATER_HOLD, sep.water + in.water());
		sep.gas = Math.min(Tuning.SEPARATOR_GAS_HOLD, sep.gas + in.gas());
		sep.came = in;
		boolean flowing = in.liquid() + in.gas() > 1e-6;
		if (sep.gas >= Tuning.SEPARATOR_GAS_HOLD - 0.01) {
			sep.status = "SHUT IN: gas has nowhere to go (pipe its gas side to a burner, an engine or a flare)";
		} else if (sep.oil >= Tuning.SEPARATOR_OIL_HOLD - 1) {
			sep.status = "SHUT IN: oil has nowhere to go (pipe its oil side to a stock tank or a refinery)";
		} else if (sep.water >= Tuning.SEPARATOR_WATER_HOLD - 1) {
			sep.status = "SHUT IN: water has nowhere to go (pipe its water side to a water tank)";
		} else if (flowing) {
			sep.status = "separating";
		} else {
			sep.status = wells ? "waiting for the wells" : "idle: no well on its inlet (pipe a wellhead to its front end)";
		}
		sep.setChanged();
	}

	@Override
	public Component inspect() {
		double perSecond = 20.0 / Tuning.WELL_INTERVAL;
		String cut = came.liquid() > 1e-6 ? String.format(", water cut %.0f%%", came.water() / came.liquid() * 100) : "";
		return Component.literal(String.format("Three-phase separator (36 in x 10 ft, 125 psi): %s; in %.1f L/s oil, %.1f L/s water, %.2f m3/s gas%s; "
				+ "holding oil %.0f/%.0f L, water %.0f/%.0f L, gas %.1f/%.0f m3", status, came.oil() * perSecond, came.water() * perSecond,
				came.gas() * perSecond, cut, oil, Tuning.SEPARATOR_OIL_HOLD, water, Tuning.SEPARATOR_WATER_HOLD, gas, Tuning.SEPARATOR_GAS_HOLD));
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
		output.putDouble("oil", oil);
		output.putDouble("water", water);
		output.putDouble("gas", gas);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		oil = input.getDoubleOr("oil", 0);
		water = input.getDoubleOr("water", 0);
		gas = input.getDoubleOr("gas", 0);
		grown = input.getBooleanOr("grown", grown);
	}
}
