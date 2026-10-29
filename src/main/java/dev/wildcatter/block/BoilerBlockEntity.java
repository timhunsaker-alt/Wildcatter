package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds water and makes steam while a lit campfire is underneath. A turbine next to it takes the steam.
 */
public class BoilerBlockEntity extends BlockEntity implements Inspectable {
	private int water;
	private int steam;
	/** Steam made since the last unit of water was used up. */
	private int boiled;

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BOILER_ENTITY, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
		boolean heated = boiler.isHeated(level, pos);
		boolean working = false;
		if (heated && boiler.water > 0 && boiler.steam < Tuning.BOILER_STEAM_CAPACITY) {
			int made = Math.min(Tuning.BOILER_STEAM_PER_TICK, Tuning.BOILER_STEAM_CAPACITY - boiler.steam);
			boiler.steam += made;
			boiler.boiled += made;
			if (boiler.boiled >= Tuning.STEAM_PER_WATER) {
				boiler.boiled -= Tuning.STEAM_PER_WATER;
				boiler.water--;
			}
			working = true;
			boiler.setChanged();
		}
		if (state.getValue(BoilerBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(BoilerBlock.LIT, working), 3);
		}
	}

	private boolean isHeated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT);
	}

	/** Adds water. Returns how much fitted. */
	public int addWater(int amount) {
		int added = Math.min(amount, Tuning.BOILER_WATER_CAPACITY - water);
		if (added > 0) {
			water += added;
			setChanged();
		}
		return added;
	}

	/** Lets a turbine take steam. Returns how much it got. */
	public int takeSteam(int amount) {
		int taken = Math.min(amount, steam);
		if (taken > 0) {
			steam -= taken;
			setChanged();
		}
		return taken;
	}

	@Override
	public Component inspect() {
		String heat = level != null && isHeated(level, worldPosition) ? "heated" : "cold, needs a lit campfire underneath";
		return Component.literal("Boiler: water " + water + "/" + Tuning.BOILER_WATER_CAPACITY
				+ ", steam " + steam + "/" + Tuning.BOILER_STEAM_CAPACITY + ", " + heat);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("water", water);
		output.putInt("steam", steam);
		output.putInt("boiled", boiled);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getIntOr("water", 0);
		steam = input.getIntOr("steam", 0);
		boiled = input.getIntOr("boiled", 0);
	}
}
