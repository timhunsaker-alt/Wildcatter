package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Carbon;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The ring baking furnace (docs/multiblocks/anode-plant.md): an open-top furnace of about 16 sections whose fire runs to about
 * 1,100 C; exhaust fan 75 kW at 480 V. Green anodes by a hopper on top (16 in its pits); it bakes them at up to the plant rate on
 * 2.4 MJ a kg of gas, coker gas, coke oven gas or LPG at the east face of its bottom cells. Baked anodes go out to a hopper under it,
 * or to an empty hand. The two-week firing is a steady rate here.
 */
public class BakingFurnaceBlockEntity extends FillerBlockEntity {
	private double baking;   // anodes' worth of firing done toward the next
	private int baked;
	private String unit = "starting";

	public BakingFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BAKING_FURNACE_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.GREEN_ANODE);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** For the game test: an anode almost fired. */
	public void addBaking(double anodes) {
		baking += anodes;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		ItemStack anode = new ItemStack(WildcatterItems.BAKED_ANODE);
		if (baking >= 1 && fed() > 0) {
			if (!roomOut(anode)) {
				unit = "FULL: take the anodes (empty hand, or a hopper under it)";
				return;
			}
			useOne();
			putOut(anode);
			baking -= 1;
			baked++;
		}
		if (fed() == 0) {
			unit = "NO ANODES: put a hopper of green anodes on it";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double max = Math.min(Tuning.ANODES_PER_SECOND * Tuning.CARBON_CATCH_UP * seconds(), fed() + 0.001 - baking) * strength;
		double need = max * Carbon.bakingJoules();
		double got = GasFiring.fire(level, east(), need, this);
		double done = max * Math.min(1, got / need);
		if (done <= 1e-9) {
			unit = "NO FUEL: pipe gas, coker gas, coke oven gas or LPG to its east side";
			return;
		}
		baking += done;
		unit = String.format("firing at 1,100 C: %.1f anodes a day, %.0f kW of fuel; %.0f %% toward the next", done / seconds() * 86_400, got / seconds() / 1000, baking * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Ring baking furnace: %s; %d green anodes in its pits, %d baked, %d waiting; ", unit, fed(), baked, outCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("baking", baking);
		output.putInt("baked", baked);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		baking = input.getDoubleOr("baking", 0);
		baked = input.getIntOr("baked", 0);
	}
}
