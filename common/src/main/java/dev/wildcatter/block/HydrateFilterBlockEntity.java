package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Bayer;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The hydrate filter (docs/multiblocks/bayer-refinery.md): a horizontal pan filter, vacuum pump 90 kW at 480 V. Hydrate slurry in at
 * the north face of its front cells; spent liquor out of the east face of its bottom cells; the washed hydrate to the calciner it
 * touches (at its back end), and the share set by sneak-click to a bagging station it touches, as hydrate bags for the AlF3 plant.
 */
public class HydrateFilterBlockEntity extends UnitMotorBlockEntity implements Bagged {
	private double toCalciner;   // kg of Al(OH)3 waiting for the calciner
	private double toBags;       // kg waiting for the bagging station
	private int share;           // index into Tuning.HYDRATE_TO_BAGS
	private String unit = "starting";

	public HydrateFilterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HYDRATE_FILTER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 0 && at[0] == AluminaPartBlock.Part.HYDRATE_FILTER.w - 1 && side == Direction.EAST ? Product.SPENT_LIQUOR : null;
	}

	@Override
	protected double hold(Product product) {
		return Bayer.spentLiquor() * Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_CATCH_UP * Tuning.PLANT_HOLD_SECONDS;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private static double calcinerHold() {
		return Bayer.hydrate() * Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_CATCH_UP * Tuning.PLANT_HOLD_SECONDS;
	}

	/** Sneak-click: the next share of the hydrate sent to bags. */
	public String stepShare() {
		share = (share + 1) % Tuning.HYDRATE_TO_BAGS.length;
		setChanged();
		return String.format("Hydrate filter: %.0f %% of the hydrate to bags (a bagging station against it), the rest to the calciner", Tuning.HYDRATE_TO_BAGS[share] * 100);
	}

	public double hydrate() {
		return toCalciner;
	}

	public double takeHydrate(double kg) {
		double got = Math.min(kg, toCalciner);
		toCalciner -= got;
		setChanged();
		return got;
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.HYDRATE_BAG;
	}

	@Override
	public double bagReady() {
		return toBags;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, toBags);
		toBags -= got;
		setChanged();
		return got;
	}

	@Override
	protected void work(Level level) {
		CalcinerBlockEntity calciner = Touching.find(level, worldPosition, CalcinerBlockEntity.class, c -> true);
		BaggingStationBlockEntity bagger = Touching.find(level, worldPosition, BaggingStationBlockEntity.class, b -> true);
		if (calciner == null && bagger == null) {
			unit = "NOT BUILT: set it against the calciner (or a bagging station)";
			return;
		}
		double bags = calciner == null ? 1 : bagger == null ? 0 : Tuning.HYDRATE_TO_BAGS[share];
		double perA = Bayer.polishedLiquor();
		double waiting = Lines.available(level, inlet(), Product.HYDRATE_SLURRY, Pull.MACHINE, this) / perA;
		if (waiting < 1e-6) {
			unit = "idle: no hydrate slurry at its front (north face)";
			return;
		}
		double max = Math.min(Math.min(Tuning.BAYER_ALUMINA_KGS * Tuning.BAYER_CATCH_UP * seconds(), waiting), room(Product.SPENT_LIQUOR) / Bayer.spentLiquor());
		if (bags < 1) {
			max = Math.min(max, (calcinerHold() - toCalciner) / (Bayer.hydrate() * (1 - bags)));
		}
		if (bags > 0) {
			max = Math.min(max, (2 * Tuning.BULK_BAG_KG - toBags) / (Bayer.hydrate() * bags));
		}
		if (max <= 1e-7) {
			unit = "idle: its hydrate or spent liquor has nowhere to go";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double a = Lines.pull(level, inlet(), Product.HYDRATE_SLURRY, max * strength * perA, Pull.MACHINE, this) / perA;
		add(Product.SPENT_LIQUOR, a * Bayer.spentLiquor());
		toCalciner += a * Bayer.hydrate() * (1 - bags);
		toBags += a * Bayer.hydrate() * bags;
		unit = String.format("filtering %.2f kg/s of hydrate, %.0f %% to bags", a * Bayer.hydrate() / seconds(), bags * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Hydrate filter (pan): %s; %.0f kg for the calciner, %.0f kg for bags; ", unit, toCalciner, toBags) + describe()
				+ "; sneak and right-click sets the share to bags");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("to_calciner", toCalciner);
		output.putDouble("to_bags", toBags);
		output.putInt("share", share);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		toCalciner = input.getDoubleOr("to_calciner", 0);
		toBags = input.getDoubleOr("to_bags", 0);
		share = Math.floorMod(input.getIntOr("share", 0), Tuning.HYDRATE_TO_BAGS.length);
	}
}
