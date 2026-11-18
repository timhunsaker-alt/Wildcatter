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
public class BoilerBlockEntity extends BlockEntity implements Inspectable, dev.wildcatter.multiblock.BigPartEntity, dev.wildcatter.control.Measurable {
	@Override
	public double measure(Quantity quantity) {
		return quantity == Quantity.LEVEL ? waterLevel() * 100 : Double.NaN;
	}

	private int water;
	/** The safety valve is lifting: the steam is at full pressure (a full steam space). Sent to the client when it changes. */
	private boolean lifting;
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
			// a firebox's fire boils as much as its heat gives at 25 % (docs/dry-type-transformer.md); a campfire 2 a tick. The fire burns
			// only while the boiler wants heat, so the fuel follows the steam drawn.
			int rate = fireboxFired(level, pos) ? Tuning.FIREBOX_STEAM_PER_TICK : Tuning.BOILER_STEAM_PER_TICK;
			int made = Math.min(rate, Tuning.BOILER_STEAM_CAPACITY - boiler.steam);
			boiler.steam += made;
			boiler.boiled += made;
			while (boiler.boiled >= Tuning.STEAM_PER_WATER && boiler.water > 0) {
				boiler.boiled -= Tuning.STEAM_PER_WATER;
				boiler.water--;
			}
			boiler.setChanged();
		}
		// the safety valve lifts at full pressure and reseats well below it, so it does not chatter as the fire cycles on and off
		// with the turbine's draw (a tick or two of draw takes the steam down a few percent)
		boolean lift = boiler.lifting ? boiler.steam >= Tuning.BOILER_STEAM_CAPACITY * 0.75 : boiler.steam >= Tuning.BOILER_STEAM_CAPACITY * 0.95;
		if (lift != boiler.lifting) {
			boiler.lifting = lift;
			boiler.setChanged();
			level.sendBlockUpdated(pos, state, state, 3);
		}
		if (state.getValue(BoilerBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(BoilerBlock.LIT, working), 3);
		}
		if (working && phase % 70 == 0) {
			level.playSound(null, pos, WildcatterSounds.BOILER_HISS, SoundSource.BLOCKS, 0.5F, 1.0F);
		}
	}

	/**
	 * Client side (docs/kinetics-and-sound.md): while the fire under it is boiling water, the flue gas rises out of the stack's top,
	 * 1.87 m over the boiler's centre (tools/models/starter_plant.py); a coal firebox's thick and dark, a campfire's thin.
	 */
	public static void clientTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
		net.minecraft.util.RandomSource random = level.getRandom();
		if (boiler.lifting) {
			// the safety valve on the crown, 1.5 m up (tools/models/starter_plant.py), blowing a hard jet of steam straight up
			double[] at = GensetPartBlockEntity.turned(0.35, 0.6, state.getValue(BoilerBlock.FACING));
			double vx = pos.getX() + at[0], vy = pos.getY() + 1.52, vz = pos.getZ() + at[1];
			for (int i = 0; i < 2; i++) {
				level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, vx, vy, vz,
						(random.nextDouble() - 0.5) * 0.04, 0.45 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.04);
			}
			if (random.nextFloat() < 0.5F) {
				level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, vx, vy + 0.6, vz, 0, 0.2, 0);
			}
			if ((level.getGameTime() + pos.hashCode()) % 30 == 0) {
				level.playLocalSound(vx, vy, vz, WildcatterSounds.BOILER_HISS, SoundSource.BLOCKS, 0.9F, 1.3F, false);
			}
		}
		if (!state.getValue(BoilerBlock.LIT)) {
			return;
		}
		boolean firebox = fireboxFired(level, pos);
		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.1, y = pos.getY() + 1.9, z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.1;
		if (random.nextFloat() < (firebox ? 0.3F : 0.1F)) {
			level.addAlwaysVisibleParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y, z, 0, 0.05, 0);
		}
		if (firebox && random.nextFloat() < 0.35F) {
			level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.07, 0);
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

	/** True if it stands on a firebox (lit or not): the turbines it feeds are rated by that fire. */
	public static boolean fireboxFired(Level level, BlockPos pos) {
		return level.getBlockState(pos.below()).getBlock() instanceof FireboxBlock;
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
		output.putBoolean("lifting", lifting);
	}

	/** True while the safety valve is lifting (on the client as well: it is in the update packet). */
	public boolean lifting() {
		return lifting;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	/** Only what the client draws: the valve. */
	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putBoolean("lifting", lifting);
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = Math.min(input.getIntOr("water", 0), Tuning.BOILER_WATER_CAPACITY);   // a former coil boiler's master keeps only one boiler's worth
		steam = Math.min(input.getIntOr("steam", 0), Tuning.BOILER_STEAM_CAPACITY);
		lifting = input.getBooleanOr("lifting", false);
		boiled = input.getIntOr("boiled", 0);
		grown = input.getBooleanOr("grown", false);
	}
}
