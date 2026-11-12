package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bagging station (docs/multiblocks/alumina-plant.md): an FIBC filling station, 5 kW at 480 V. Each time the part it touches (a
 * calciner, hydrate filter, AlF3 reactor or lime kiln) has 1,000 kg ready, it fills an empty bulk bag from the hopper on top; full
 * bags go out to a hopper under it, or to an empty hand.
 */
public class BaggingStationBlockEntity extends FillerBlockEntity {
	private int filled;
	private String unit = "starting";

	public BaggingStationBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BAGGING_STATION_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.EMPTY_BULK_BAG);
	}

	@Override
	protected void work(Level level) {
		Bagged source = Touching.find(level, worldPosition, Bagged.class, b -> true);
		if (source == null) {
			unit = "NOT BUILT: set it against a calciner, hydrate filter, AlF3 reactor, lime kiln, coke cooler, granulation plant, potash dryer, barite mill, bentonite dryer or borax dissolver";
			return;
		}
		ItemStack bag = new ItemStack(source.bagItem());
		if (source.bagReady() < Tuning.BULK_BAG_KG) {
			unit = String.format("filling %s: %.0f/%.0f kg ready", bag.getHoverName().getString(), source.bagReady(), Tuning.BULK_BAG_KG);
			return;
		}
		if (fed() == 0) {
			unit = "NO BAGS: put a hopper of empty bulk bags on top";
			return;
		}
		if (!roomOut(bag)) {
			unit = "FULL: take the bags (empty hand, or a hopper under it)";
			return;
		}
		if (drive(level) <= 0) {
			unit = "starting: " + describe();
			return;
		}
		useOne();
		source.takeForBag(Tuning.BULK_BAG_KG);
		putOut(bag);
		filled++;
		unit = "filled a bag";
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Bagging station: %s; %d bags filled, %d waiting, %d empty; ", unit, filled, outCount(), fed()) + describe());
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("filled", filled);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		filled = input.getIntOr("filled", 0);
	}
}
