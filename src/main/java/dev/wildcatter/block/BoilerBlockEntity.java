package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The starter plant's boiler: it holds water and makes steam while a lit campfire or a burning firebox is under it. A turbine
 * next to it takes the steam. Big steam comes from the 1 MW steam plant's drum (docs/multiblocks/steam-plant.md).
 */
public class BoilerBlockEntity extends BlockEntity implements Inspectable, dev.wildcatter.multiblock.BigPartEntity {
	private int water;
	private boolean grown;
	private int steam;
	/** Steam made since the last unit of water was used up. */
	private int boiled;

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BOILER_ENTITY, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
		long phase = level.getGameTime() + pos.hashCode();
		// Lit means "heated and has water to boil". It stays lit while the steam tank is full, or it would flicker
		// on and off every tick as the turbine draws the tank down and it refills.
		boolean working = boiler.isHeated(level, pos) && boiler.water > 0;
		if (working && boiler.steam < Tuning.BOILER_STEAM_CAPACITY) {
			int made = Math.min(Tuning.BOILER_STEAM_PER_TICK, Tuning.BOILER_STEAM_CAPACITY - boiler.steam);
			boiler.steam += made;
			boiler.boiled += made;
			while (boiler.boiled >= Tuning.STEAM_PER_WATER && boiler.water > 0) {
				boiler.boiled -= Tuning.STEAM_PER_WATER;
				boiler.water--;
			}
			boiler.setChanged();
		}
		if (state.getValue(BoilerBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(BoilerBlock.LIT, working), 3);
		}
		if (working && phase % 70 == 0) {
			level.playSound(null, pos, WildcatterSounds.BOILER_HISS, SoundSource.BLOCKS, 0.5F, 1.0F);
		}
	}

	/** Where this boiler's tanks are kept: its own position (pipes use it to count each boiler once). */
	public BlockPos rootPos() {
		return worldPosition;
	}

	/** True if there is water to boil and room for more steam: the fire under it should burn. */
	public boolean wantsHeat() {
		return water > 0 && steam < Tuning.BOILER_STEAM_CAPACITY;
	}

	private boolean isHeated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		if (below.getBlock() instanceof FireboxBlock) {
			return level.getBlockEntity(pos.below()) instanceof FireboxBlockEntity firebox && firebox.isBurning();
		}
		return below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT);
	}

	/** How full of water the boiler is, 0 to 1. */
	public double waterLevel() {
		return water / (double) Tuning.BOILER_WATER_CAPACITY;
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
		String heat = level != null && isHeated(level, worldPosition) ? "heated" : "cold, needs a lit campfire or a burning firebox underneath";
		return Component.literal("Boiler: water " + water + "/" + Tuning.BOILER_WATER_CAPACITY + ", steam " + steam + "/" + Tuning.BOILER_STEAM_CAPACITY + ", " + heat);
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
		output.putBoolean("grown", grown);
		output.putInt("water", water);
		output.putInt("steam", steam);
		output.putInt("boiled", boiled);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = Math.min(input.getIntOr("water", 0), Tuning.BOILER_WATER_CAPACITY);   // a former coil boiler's master keeps only one boiler's worth
		steam = Math.min(input.getIntOr("steam", 0), Tuning.BOILER_STEAM_CAPACITY);
		boiled = input.getIntOr("boiled", 0);
		grown = input.getBooleanOr("grown", false);
	}
}
