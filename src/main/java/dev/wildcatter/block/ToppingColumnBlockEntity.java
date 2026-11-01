package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Assay;
import dev.wildcatter.fluid.Product;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The early crude unit's column and controller: a 1,000 bbl/day skid topping unit (docs/multiblocks/crude-units.md). Its heater and
 * its pump and air-cooler skid touch it. Crude or condensate goes in at the skid; gasoline comes off the skid's reflux drum;
 * kerosene, diesel and residue come off the column's foot (east, south and west faces). The heater burns the unit's own LPG first.
 */
public class ToppingColumnBlockEntity extends PlantUnitBlockEntity {
	private double refined;
	private @Nullable Product feeding;

	public ToppingColumnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TOPPING_COLUMN_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		return side == Direction.EAST ? Product.KEROSENE : side == Direction.SOUTH ? Product.DIESEL : side == Direction.WEST ? Product.RESIDUE : null;
	}

	@Override
	protected double hold(Product product) {
		return 50;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ToppingColumnBlockEntity col) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		col.refined = 0;
		FiredHeaterBlockEntity heater = Touching.find(level, pos, FiredHeaterBlockEntity.class, h -> !h.big());
		UnitMotorBlockEntity skid = Touching.find(level, pos, UnitMotorBlockEntity.class, m -> m.kind().equals("topping_skid"));
		if (heater == null || skid == null) {
			col.status = "NOT BUILT: set " + (heater == null ? "a TOPPING HEATER" : "") + (heater == null && skid == null ? " and " : "")
					+ (skid == null ? "a TOPPING SKID" : "") + " against the column";
			col.setChanged();
			return;
		}
		Product feed = null;
		for (Lines.Line line : Lines.lines(level, skid.feed())) {
			if (line.run().product() != null && Assay.feed(line.run().product())) {
				feed = line.run().product();
			}
		}
		col.feeding = feed;
		double waiting = feed == null ? 0 : Lines.available(level, skid.feed(), feed, Pull.MACHINE, col);
		if (waiting < 0.01) {
			heater.idle();
			col.status = "idle: no crude or condensate at the skid's suction (its north end)";
			col.setChanged();
			return;
		}
		double strength = skid.drive(level);
		if (strength <= 0) {
			heater.idle();
			col.status = "waiting for its skid: " + skid.describe();
			col.setChanged();
			return;
		}
		Map<Product, Double> cuts = Assay.of(feed);
		double seconds = seconds();
		double amount = Math.min(waiting, Tuning.TOPPING_LPS * seconds * strength);
		amount = Math.min(amount, col.room(Product.KEROSENE) / cuts.get(Product.KEROSENE));
		amount = Math.min(amount, col.room(Product.DIESEL) / cuts.get(Product.DIESEL));
		amount = Math.min(amount, col.room(Product.RESIDUE) / cuts.get(Product.RESIDUE));
		amount = Math.min(amount, skid.roomFor(Product.GASOLINE) / cuts.get(Product.GASOLINE));
		if (amount < 0.001) {
			heater.idle();
			col.status = "STOPPED: a product has nowhere to go (gasoline off the skid; kerosene east, diesel south, residue west of the column's foot)";
			col.setChanged();
			return;
		}
		// heat: the heater's duty follows the feed; its own LPG first, then fuel off its nozzle
		double need = Tuning.TOPPING_HEATER_WATTS * amount / Tuning.TOPPING_LPS;   // joules: its duty follows the feed
		double ownLpg = Math.min(need, amount * cuts.get(Product.LPG) * Tuning.LPG_JOULES_PER_L);
		double bought = heater.burn(level, need - ownLpg, seconds);
		heater.credit(ownLpg, need, seconds);
		double heated = need > 0 ? Math.min(1, (ownLpg + bought) / need) : 1;
		amount *= heated;
		double got = amount > 0.001 ? Lines.pull(level, skid.feed(), feed, amount, Pull.MACHINE, col) : 0;
		col.add(Product.KEROSENE, got * cuts.get(Product.KEROSENE));
		col.add(Product.DIESEL, got * cuts.get(Product.DIESEL));
		col.add(Product.RESIDUE, got * cuts.get(Product.RESIDUE));
		skid.give(Product.GASOLINE, got * cuts.get(Product.GASOLINE));
		col.refined = got;
		col.status = got > 0 ? String.format("refining %.2f L/s of %s%s", got / seconds, feed.label(), heated < 0.99 ? " (SHORT OF FUEL: pipe gas or residue to the heater)" : "")
				: "SHORT OF FUEL: pipe gas or residue to the heater's foot";
		col.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Topping unit (1,000 bbl/day; column 0.9 m x 12 m): " + status + String.format("; holding kerosene %.0f, diesel %.0f, residue %.0f L",
				held(Product.KEROSENE), held(Product.DIESEL), held(Product.RESIDUE)));
	}
}
