package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Holds water and makes steam while it is heated. A turbine next to it takes the steam.
 *
 * Alone it sits on a lit campfire. Build a 3x3x3 cube of boilers over a 3x3 floor of furnaces and the cube becomes
 * one coil boiler: the lowest corner boiler is its master and holds everything, the other 26 hand their tanks to it.
 * Each lit furnace does a ninth of the work, so the whole thing makes 27 times the steam of one campfire boiler.
 */
public class BoilerBlockEntity extends BlockEntity implements Inspectable {
	private int water;
	private int steam;
	/** Steam made since the last unit of water was used up. */
	private int boiled;
	/** The master of the coil boiler this belongs to, or null if it stands alone. */
	private @Nullable BlockPos master;
	/** True on the one boiler (lowest corner) that runs a whole coil boiler. */
	private boolean isMaster;
	private int litFurnaces;

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BOILER_ENTITY, pos, state);
	}

	private boolean isSlave() {
		return master != null && !isMaster;
	}

	/** The boiler whose tanks this one uses (itself unless it is a slave of a coil boiler). */
	private @Nullable BoilerBlockEntity root() {
		if (!isSlave()) {
			return this;
		}
		return level != null && level.getBlockEntity(master) instanceof BoilerBlockEntity m && m.isMaster ? m : null;
	}

	public boolean isFormed() {
		return master != null;
	}

	private int size() {
		return isMaster ? Tuning.BIG_SIZE * Tuning.BIG_SIZE * Tuning.BIG_SIZE : 1;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
		long phase = level.getGameTime() + pos.hashCode();
		if (boiler.isSlave()) {
			boiler.tickSlave(level, pos, state);
			return;
		}
		if (phase % Tuning.STRUCTURE_CHECK_TICKS == 0) {
			if (boiler.isMaster) {
				if (!boiler.stillValid(level)) {
					boiler.dissolve(level);
				}
			} else {
				boiler.tryForm(level);
			}
			state = boiler.getBlockState();
		}

		boolean heated;
		int power;   // steam made per tick at full water
		if (boiler.isMaster) {
			boiler.litFurnaces = boiler.countLitFurnaces(level);
			heated = boiler.litFurnaces > 0;
			power = Tuning.BOILER_STEAM_PER_TICK * Tuning.BIG_SIZE * boiler.litFurnaces;   // 27 boilers' worth over 9 furnaces
		} else {
			heated = boiler.isHeated(level, pos);
			power = Tuning.BOILER_STEAM_PER_TICK;
		}
		// Lit means "heated and has water to boil". It stays lit while the steam tank is full, or it would flicker
		// on and off every tick as the turbine draws the tank down and it refills.
		boolean working = heated && boiler.water > 0;
		int steamCap = Tuning.BOILER_STEAM_CAPACITY * boiler.size();
		if (working && boiler.steam < steamCap) {
			int made = Math.min(power, steamCap - boiler.steam);
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
			level.playSound(null, pos, WildcatterSounds.BOILER_HISS, SoundSource.BLOCKS, boiler.isMaster ? 1.2F : 0.5F, 1.0F);
		}
	}

	/** A slave of a coil boiler copies its master's lit state, and lets go if the master is gone. */
	private void tickSlave(Level level, BlockPos pos, BlockState state) {
		BoilerBlockEntity m = root();
		if (m == null || !m.contains(pos)) {
			release(level);
			return;
		}
		boolean lit = m.getBlockState().getValue(BoilerBlock.LIT);
		if (state.getValue(BoilerBlock.LIT) != lit) {
			level.setBlock(pos, state.setValue(BoilerBlock.LIT, lit), 3);
		}
	}

	private boolean contains(BlockPos pos) {
		return isMaster && pos.getX() >= worldPosition.getX() && pos.getX() < worldPosition.getX() + Tuning.BIG_SIZE
				&& pos.getY() >= worldPosition.getY() && pos.getY() < worldPosition.getY() + Tuning.BIG_SIZE
				&& pos.getZ() >= worldPosition.getZ() && pos.getZ() < worldPosition.getZ() + Tuning.BIG_SIZE;
	}

	private static boolean isFurnace(BlockState state) {
		return state.getBlock() instanceof AbstractFurnaceBlock;
	}

	/** Looks for a 3x3x3 cube of free boilers with this one as its lowest corner, over a 3x3 floor of furnaces. */
	private void tryForm(Level level) {
		int n = Tuning.BIG_SIZE;
		BlockPos o = worldPosition;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (!isFurnace(level.getBlockState(o.offset(x, -1, z)))) {
					return;
				}
				for (int y = 0; y < n; y++) {
					if (!(level.getBlockEntity(o.offset(x, y, z)) instanceof BoilerBlockEntity b) || b.master != null) {
						return;
					}
				}
			}
		}
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					BlockPos at = o.offset(x, y, z);
					BoilerBlockEntity b = (BoilerBlockEntity) level.getBlockEntity(at);
					if (b != this) {
						water += b.water;
						steam += b.steam;
						b.water = 0;
						b.steam = 0;
						b.boiled = 0;
						b.master = o;
						b.setChanged();
					}
					level.setBlock(at, level.getBlockState(at).setValue(BoilerBlock.FORMED, true), 3);
				}
			}
		}
		master = o;
		isMaster = true;
		water = Math.min(water, Tuning.BOILER_WATER_CAPACITY * size());
		steam = Math.min(steam, Tuning.BOILER_STEAM_CAPACITY * size());
		setChanged();
		level.playSound(null, o.offset(1, 1, 1), WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.5F, 1.0F);
	}

	private boolean stillValid(Level level) {
		int n = Tuning.BIG_SIZE;
		BlockPos o = worldPosition;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (!isFurnace(level.getBlockState(o.offset(x, -1, z)))) {
					return false;
				}
				for (int y = 0; y < n; y++) {
					if (!(level.getBlockEntity(o.offset(x, y, z)) instanceof BoilerBlockEntity b) || !o.equals(b.master)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private int countLitFurnaces(Level level) {
		int lit = 0;
		for (int x = 0; x < Tuning.BIG_SIZE; x++) {
			for (int z = 0; z < Tuning.BIG_SIZE; z++) {
				BlockState furnace = level.getBlockState(worldPosition.offset(x, -1, z));
				if (isFurnace(furnace) && furnace.getValue(AbstractFurnaceBlock.LIT)) {
					lit++;
				}
			}
		}
		return lit;
	}

	/** The structure was broken: every boiler stands alone again. What the master held is kept in the master. */
	private void dissolve(Level level) {
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					BlockPos at = worldPosition.offset(x, y, z);
					if (level.getBlockEntity(at) instanceof BoilerBlockEntity b && worldPosition.equals(b.master)) {
						b.release(level);
					}
				}
			}
		}
	}

	private void release(Level level) {
		master = null;
		isMaster = false;
		water = Math.min(water, Tuning.BOILER_WATER_CAPACITY);
		steam = Math.min(steam, Tuning.BOILER_STEAM_CAPACITY);
		setChanged();
		BlockState state = getBlockState();
		if (state.is(WildcatterBlocks.BOILER)) {
			level.setBlock(worldPosition, state.setValue(BoilerBlock.FORMED, false), 3);
		}
	}

	private boolean isHeated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT);
	}

	/** Adds water. Returns how much fitted. */
	public int addWater(int amount) {
		BoilerBlockEntity r = root();
		if (r == null) {
			return 0;
		}
		int added = Math.min(amount, Tuning.BOILER_WATER_CAPACITY * r.size() - r.water);
		if (added > 0) {
			r.water += added;
			r.setChanged();
		}
		return added;
	}

	/** Lets a turbine take steam. Returns how much it got. */
	public int takeSteam(int amount) {
		BoilerBlockEntity r = root();
		if (r == null) {
			return 0;
		}
		int taken = Math.min(amount, r.steam);
		if (taken > 0) {
			r.steam -= taken;
			r.setChanged();
		}
		return taken;
	}

	@Override
	public Component inspect() {
		BoilerBlockEntity r = root();
		if (r == null) {
			return Component.literal("Boiler: its coil boiler has lost its master");
		}
		if (r.isMaster) {
			int size = r.size();
			return Component.literal("Coil boiler (27 boilers): water " + r.water + "/" + Tuning.BOILER_WATER_CAPACITY * size
					+ ", steam " + r.steam + "/" + Tuning.BOILER_STEAM_CAPACITY * size + ", " + r.litFurnaces + " of 9 furnaces lit"
					+ (r.litFurnaces == 0 ? " (light the furnaces: they need something to smelt)" : ""));
		}
		String heat = level != null && isHeated(level, worldPosition) ? "heated"
				: "cold, needs a lit campfire underneath (or build 27 over 9 furnaces)";
		return Component.literal("Boiler: water " + r.water + "/" + Tuning.BOILER_WATER_CAPACITY
				+ ", steam " + r.steam + "/" + Tuning.BOILER_STEAM_CAPACITY + ", " + heat);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("water", water);
		output.putInt("steam", steam);
		output.putInt("boiled", boiled);
		output.putBoolean("is_master", isMaster);
		if (master != null) {
			output.putInt("master_x", master.getX());
			output.putInt("master_y", master.getY());
			output.putInt("master_z", master.getZ());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getIntOr("water", 0);
		steam = input.getIntOr("steam", 0);
		boiled = input.getIntOr("boiled", 0);
		isMaster = input.getBooleanOr("is_master", false);
		master = input.getInt("master_x").isPresent()
				? new BlockPos(input.getIntOr("master_x", 0), input.getIntOr("master_y", 0), input.getIntOr("master_z", 0)) : null;
	}
}
