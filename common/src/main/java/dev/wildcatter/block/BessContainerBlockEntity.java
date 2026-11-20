package dev.wildcatter.block;

import dev.wildcatter.Rules;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.BessCells;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A 20 ft battery container (docs/multiblocks/grid-batteries.md, 22b): 96 LFP modules in 12 racks (5.0 MWh, a 416-series string at 1,331 V
 * nominal), its BMS, liquid chiller, gas detection, roof vents, aerosol suppression and a sprinkler connection. It must touch its PCS, which
 * steps its cells. Broken, it drops itself with its state of health and cycles kept, discharged.
 * <p>
 * 22c, the world rule {@code bessThermalRunaway}: fire or lava touching it for 30 s, a burning container touching it for 2 minutes, an
 * explosion's blast reaching it (its blocks are spared: it burns out instead), or lightning striking it with no ground rod within 4 blocks,
 * sets it off: THERMAL RUNAWAY, its PCS trips, the vents lift, it burns 10 minutes (5 with sprinkler water) with toxic smoke round it, and is
 * destroyed to scrap. Never at random.
 */
public class BessContainerBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, WaterUser, BessModules {
	/** Litres its sprinkler holds from the fire-water pipe, and uses a second while it burns (proposed). */
	public static final int SPRINKLER_LITRES = 2_000;
	public static final int SPRINKLER_LPS = 10;

	private final BessCells cells = new BessCells(0, Tuning.CELL_HEAT_CAPACITY, Tuning.CHILLER_WATTS);
	private int modules;
	private boolean grown;
	private double fireSeconds;
	private double spreadSeconds;
	/** Seconds it has burned; negative: not burning. */
	private double burning = -1;
	private int water;
	private boolean sprinkled;

	public BessContainerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BESS_CONTAINER_ENTITY, pos, state);
	}

	@Override
	public BessCells cells() {
		return cells;
	}

	@Override
	public int modules() {
		return modules;
	}

	@Override
	public int maxModules() {
		return Tuning.CONTAINER_MODULES;
	}

	@Override
	public void setModules(int n) {
		modules = n;
		cells.setRated(n * Tuning.LFP_MODULE_KWH);
		setChanged();
	}

	public boolean burning() {
		return burning >= 0;
	}

	public double burnedSeconds() {
		return burning;
	}

	/** For the game test: fire held to it as if for {@code s}. */
	public void testFire(double s) {
		fireSeconds = s;
	}

	public void testBurn(double s) {
		burning = s;
	}

	public void testSpread(double s) {
		spreadSeconds = s;
	}

	@Override
	public double waterLevel() {
		return water / (double) SPRINKLER_LITRES;
	}

	@Override
	public int addWater(int litres) {
		int fits = Math.max(0, Math.min(litres, SPRINKLER_LITRES - water));
		water += fits;
		if (fits > 0) {
			setChanged();
		}
		return fits;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BessContainerBlockEntity box) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		// a bolt lives at least 3 ticks, so looking every other tick catches it
		if (level.getGameTime() % 2 == 0 && box.burning < 0 && Rules.on(Rules.Rule.BESS_THERMAL_RUNAWAY)) {
			box.lightning(server);
		}
		if (level.getGameTime() % 20 == 0) {
			box.second(server);
		}
	}

	/** An explosion's blast reaching one of its cells (its block calls this in place of being blown up), under the rule. */
	public void blasted(ServerLevel level) {
		if (burning < 0) {
			runaway(level, "an explosion's blast reaching it");
		}
	}

	/** Lightning striking it: with no ground rod within 4 blocks of it the surge goes through its racks and sets it off (proposed). */
	private void lightning(ServerLevel level) {
		List<net.minecraft.world.entity.LightningBolt> bolts = level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class,
				new AABB(worldPosition).inflate(Tuning.CONTAINER_LIGHTNING_LOOK));
		if (bolts.isEmpty()) {
			return;
		}
		List<BlockPos> mine = BigParts.cellsAt(level, worldPosition);
		for (net.minecraft.world.entity.LightningBolt bolt : bolts) {
			// the block a bolt strikes is the one just under its feet, as the game's own lightning rod reads it
			BlockPos struck = BlockPos.containing(bolt.position().subtract(0, 1.0E-6, 0));
			if (mine.contains(struck)) {
				if (!groundRodNear(level, mine)) {
					runaway(level, "lightning striking it with no ground rod within " + Tuning.CONTAINER_ROD_REACH + " blocks");
				}
				return;
			}
		}
	}

	private static boolean groundRodNear(Level level, List<BlockPos> mine) {
		int r = Tuning.CONTAINER_ROD_REACH;
		for (BlockPos cell : mine) {
			for (BlockPos p : BlockPos.betweenClosed(cell.offset(-r, -r, -r), cell.offset(r, r, r))) {
				if (level.getBlockState(p).is(dev.wildcatter.GridBlocks.GROUND_ROD)) {
					return true;
				}
			}
		}
		return false;
	}

	private void second(ServerLevel level) {
		if (burning >= 0) {
			burn(level);
			return;
		}
		if (!Rules.on(Rules.Rule.BESS_THERMAL_RUNAWAY)) {
			fireSeconds = spreadSeconds = 0;
			return;
		}
		List<BlockPos> mine = BigParts.cellsAt(level, worldPosition);
		boolean fire = false;
		boolean neighbour = false;
		for (BlockPos cell : mine) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (mine.contains(next)) {
					continue;
				}
				BlockState there = level.getBlockState(next);
				if (there.getBlock() instanceof BaseFireBlock || there.getFluidState().is(FluidTags.LAVA)) {
					fire = true;
				}
				if (BigParts.entity(level, next) instanceof BessContainerBlockEntity other && other != this && other.burning()) {
					neighbour = true;
				}
			}
		}
		fireSeconds = fire ? fireSeconds + 1 : 0;
		spreadSeconds = neighbour ? spreadSeconds + 1 : 0;
		if (fireSeconds >= Tuning.RUNAWAY_FIRE_SECONDS || spreadSeconds >= Tuning.RUNAWAY_SPREAD_SECONDS) {
			runaway(level, fire ? "fire touching it for " + (int) fireSeconds + " s" : "the container beside it burning");
		}
	}

	private void runaway(ServerLevel level, String why) {
		burning = 0;
		cells.setSoc(0);
		setChanged();
		double x = worldPosition.getX() + 1.5;
		double y = worldPosition.getY() + 3.2;
		double z = worldPosition.getZ() + 1.5;
		// off-gas lifts the roof vents: a deflagration with no block damage
		level.explode(null, x, y, z, 1.5F, Level.ExplosionInteraction.NONE);
		level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 0.6F);
		dev.wildcatter.Wildcatter.LOGGER.info("A battery container at {} went into THERMAL RUNAWAY: {}", worldPosition, why);
	}

	private void burn(ServerLevel level) {
		burning += 1;
		if (water >= SPRINKLER_LPS) {
			water -= SPRINKLER_LPS;
			sprinkled = true;
		}
		setChanged();
		double x = worldPosition.getX();
		double y = worldPosition.getY();
		double z = worldPosition.getZ();
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x + 1.5, y + 3.2, z + 3.0, 12, 1.2, 0.4, 2.5, 0.03);
		level.sendParticles(ParticleTypes.FLAME, x + 1.5, y + 2.5, z + 3.0, 10, 1.2, 0.8, 2.5, 0.01);
		if (sprinkled) {
			level.sendParticles(ParticleTypes.SPLASH, x + 1.5, y + 3.0, z + 3.0, 10, 1.2, 0.2, 2.5, 0.1);
		}
		if ((int) burning % 4 == 0) {
			level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 2.0F, 0.7F);
		}
		double r = Tuning.RUNAWAY_SMOKE_RADIUS;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(x - r, y - 2, z - r, x + 3 + r, y + 6, z + 6 + r))) {
			e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
			e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
		}
		double life = sprinkled ? Tuning.RUNAWAY_SPRINKLED_BURN_SECONDS : Tuning.RUNAWAY_BURN_SECONDS;
		if (burning >= life) {
			destroy(level);
		}
	}

	/** Burned out: the container is gone, its steel left as scrap (iron ingots), its modules lost. */
	private void destroy(ServerLevel level) {
		BlockPos at = worldPosition.immutable();
		for (BlockPos cell : BigParts.cellsAt(level, at)) {
			level.setBlock(cell, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS);
		}
		net.minecraft.world.entity.item.ItemEntity scrap = new net.minecraft.world.entity.item.ItemEntity(level, at.getX() + 1.5, at.getY() + 0.5, at.getZ() + 3,
				new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 16));
		level.addFreshEntity(scrap);
		dev.wildcatter.Wildcatter.LOGGER.info("The battery container at {} burned out", at);
	}

	/** Its sprinkler had water while it burned. */
	public boolean sprinkled() {
		return sprinkled;
	}

	@Override
	public Component inspect() {
		String fire = burning >= 0 ? String.format("THERMAL RUNAWAY: burning %.0f s%s; ", burning, sprinkled ? " (sprinkler on: neighbours kept cool)" : "")
				: fireSeconds > 0 ? String.format("FIRE touching it %.0f s; ", fireSeconds) : "";
		return Component.literal(String.format("Battery container (%d of 96 LFP modules, %,.0f kWh, %,.0f V DC string): ", modules, modules * Tuning.LFP_MODULE_KWH, Tuning.CONTAINER_DC_VOLTS)
				+ (modules < Tuning.CONTAINER_MODULES ? "load LFP modules by right-clicking them in; " : "")
				+ fire + cells.describe() + String.format("; sprinkler %d L", water)
				+ (Rules.on(Rules.Rule.BESS_THERMAL_RUNAWAY) ? "" : "; thermal runaway off (world rule bessThermalRunaway)"));
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
		output.putInt("modules", modules);
		output.putDouble("soc", cells.soc());
		output.putDouble("soh", cells.soh());
		output.putDouble("cycles", cells.cycles());
		output.putDouble("temp", cells.tempC());
		output.putDouble("burning", burning);
		output.putDouble("fireSeconds", fireSeconds);
		output.putInt("water", water);
		output.putBoolean("sprinkled", sprinkled);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
		modules = input.getIntOr("modules", 0);
		cells.setRated(modules * Tuning.LFP_MODULE_KWH);
		cells.set(input.getDoubleOr("soc", 0), input.getDoubleOr("soh", 1), input.getDoubleOr("cycles", 0), input.getDoubleOr("temp", Tuning.CELL_AMBIENT_C));
		burning = input.getDoubleOr("burning", -1);
		fireSeconds = input.getDoubleOr("fireSeconds", 0);
		water = input.getIntOr("water", 0);
		sprinkled = input.getBooleanOr("sprinkled", false);
	}
}
