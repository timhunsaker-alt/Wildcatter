package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Assay;
import dev.wildcatter.fluid.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The late crude unit's atmospheric column and controller: 20,000 bbl/day (docs/multiblocks/crude-units.md). The heater, the side
 * strippers and the overhead condenser touch the column; the charge pump touches the heater and the desalter the charge pump. Crude
 * or condensate goes in at the desalter. Gasoline and LPG come off the condenser, kerosene and diesel off the strippers, residue off
 * the south face of the column's foot. Its readout names what is missing.
 */
public class CrudeColumnBlockEntity extends PlantUnitBlockEntity {
	public CrudeColumnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CRUDE_COLUMN_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 0 && side == Direction.SOUTH ? Product.RESIDUE : null;
	}

	@Override
	protected double hold(Product product) {
		return 200;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CrudeColumnBlockEntity col) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		FiredHeaterBlockEntity heater = Touching.find(level, pos, FiredHeaterBlockEntity.class, FiredHeaterBlockEntity::big);
		StripperBlockEntity strippers = Touching.find(level, pos, StripperBlockEntity.class, s -> true);
		UnitMotorBlockEntity condenser = Touching.find(level, pos, UnitMotorBlockEntity.class, m -> m.kind().equals("overhead_condenser"));
		UnitMotorBlockEntity pump = heater == null ? null : Touching.find(level, heater.getBlockPos(), UnitMotorBlockEntity.class, m -> m.kind().equals("crude_charge_pump"));
		UnitMotorBlockEntity desalter = pump == null ? null : Touching.find(level, pump.getBlockPos(), UnitMotorBlockEntity.class, m -> m.kind().equals("desalter"));
		List<String> missing = new ArrayList<>();
		if (heater == null) {
			missing.add("a CRUDE HEATER against the column");
		}
		if (strippers == null) {
			missing.add("SIDE STRIPPERS against the column");
		}
		if (condenser == null) {
			missing.add("an OVERHEAD CONDENSER against the column");
		}
		if (heater != null && pump == null) {
			missing.add("a CHARGE PUMP against the heater");
		}
		if (pump != null && desalter == null) {
			missing.add("a DESALTER against the charge pump");
		}
		if (!missing.isEmpty()) {
			if (heater != null) {
				heater.idle();
			}
			col.status = "NOT BUILT: needs " + String.join(", ", missing);
			col.setChanged();
			return;
		}
		Product feed = null;
		for (Lines.Line line : Lines.lines(level, desalter.feed())) {
			if (line.run().product() != null && Assay.feed(line.run().product())) {
				feed = line.run().product();
			}
		}
		double waiting = feed == null ? 0 : Lines.available(level, desalter.feed(), feed, Pull.MACHINE, col);
		if (waiting < 0.01) {
			heater.idle();
			col.status = "idle: no crude or condensate at the desalter (its north end)";
			col.setChanged();
			return;
		}
		// the auxiliaries first, the charge pump once they run: the big motor starts alone
		double aux = Math.min(desalter.drive(level), condenser.drive(level));
		double strength = aux > 0 ? Math.min(aux, pump.drive(level)) : 0;
		if (strength <= 0) {
			heater.idle();
			col.status = "waiting for its motors: " + pump.describe() + "; " + desalter.describe() + "; " + condenser.describe();
			col.setChanged();
			return;
		}
		Map<Product, Double> cuts = Assay.of(feed);
		double seconds = seconds();
		double amount = Math.min(waiting, Tuning.CRUDE_UNIT_LPS * seconds * strength);
		amount = Math.min(amount, col.room(Product.RESIDUE) / cuts.get(Product.RESIDUE));
		amount = Math.min(amount, strippers.roomFor(Product.KEROSENE) / cuts.get(Product.KEROSENE));
		amount = Math.min(amount, strippers.roomFor(Product.DIESEL) / cuts.get(Product.DIESEL));
		amount = Math.min(amount, condenser.roomFor(Product.GASOLINE) / cuts.get(Product.GASOLINE));
		amount = Math.min(amount, condenser.roomFor(Product.LPG) / cuts.get(Product.LPG));
		if (amount < 0.001) {
			heater.idle();
			col.status = "STOPPED: a product has nowhere to go (gasoline and LPG off the condenser, kerosene and diesel off the strippers, residue off the column's foot)";
			col.setChanged();
			return;
		}
		double need = Tuning.CRUDE_HEATER_WATTS * amount / Tuning.CRUDE_UNIT_LPS;   // joules: its duty follows the feed
		double heated = need > 0 ? Math.min(1, heater.burn(level, need, seconds) / need) : 1;
		amount *= heated;
		double got = amount > 0.001 ? Lines.pull(level, desalter.feed(), feed, amount, Pull.MACHINE, col) : 0;
		col.add(Product.RESIDUE, got * cuts.get(Product.RESIDUE));
		strippers.give(Product.KEROSENE, got * cuts.get(Product.KEROSENE));
		strippers.give(Product.DIESEL, got * cuts.get(Product.DIESEL));
		condenser.give(Product.GASOLINE, got * cuts.get(Product.GASOLINE));
		condenser.give(Product.LPG, got * cuts.get(Product.LPG));
		col.status = got > 0 ? String.format("refining %.1f L/s of %s%s", got / seconds, feed.label(), heated < 0.99 ? " (SHORT OF FUEL: pipe gas or residue to the heater's front)" : "")
				: "SHORT OF FUEL: pipe gas or residue to the heater's front";
		col.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Crude unit (20,000 bbl/day; column 3 m x 30 m): " + status + String.format("; holding residue %.0f L", held(Product.RESIDUE)));
	}
}
