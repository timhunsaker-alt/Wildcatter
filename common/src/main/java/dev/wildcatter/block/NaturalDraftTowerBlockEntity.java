package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.CoolingTower;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The hyperbolic natural-draft cooling tower's controller (docs/multiblocks/natural-draft-tower.md). It builds the tower from the
 * concrete, steel and plastic in its hopper: the basin (then digs and fills the pond), the raker columns, the shell lift by lift, and
 * the fill. It places the invisible shell blocks as each part is done; its renderer draws the tower. Built, it takes the condenser's
 * warm water off the pipe on its top, cools it toward the biome's wet bulb, loses evaporation and blowdown, and takes make-up water.
 */
public class NaturalDraftTowerBlockEntity extends PlantUnitBlockEntity implements HopperFed, WaterUser {
	public static final int BASIN = 0, COLUMNS = 1, SHELL = 2, FILL = 3, DONE = 4;
	private static final Map<ResourceKey<Level>, Set<BlockPos>> TOWERS = new HashMap<>();
	private static final double MAKEUP_HOLD = 1000.0;

	/** Forgets every tower; called when the server stops so a new world starts clean. */
	public static void clearAll() {
		TOWERS.clear();
	}

	private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
	private int stage;
	private int lifts;
	private double progress;
	private double concrete, steelKg, plastic;
	private double cold = Double.NaN;
	private double heat;          // W, followed with the tower's lag
	private double basinKg;
	private double makeup;
	private double evaporated;
	private double wetBulb = Double.NaN;
	private boolean plume;
	private String status = "starting";

	public NaturalDraftTowerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.NATURAL_DRAFT_TOWER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public int stage() {
		return stage;
	}

	public int lifts() {
		return lifts;
	}

	public boolean plume() {
		return plume;
	}

	// ---- geometry ----------------------------------------------------------------------------------------------------------------
	/** The tower's centre: the middle of the controller's footprint and 34 m on in front of it. */
	public double[] centre() {
		Direction front = getBlockState().getValue(BigPartBlock.FACING);
		double x = 0, z = 0;
		int n = 0;
		for (BlockPos c : NaturalDraftTowerBlock.FOOTPRINT.pieces() > 0 ? cells() : List.of(worldPosition)) {
			if (c.getY() == worldPosition.getY()) {
				x += c.getX() + 0.5;
				z += c.getZ() + 0.5;
				n++;
			}
		}
		return new double[] {x / n + front.getStepX() * Tuning.TOWER_CENTRE, z / n + front.getStepZ() * Tuning.TOWER_CENTRE};
	}

	private List<BlockPos> cells() {
		java.util.ArrayList<BlockPos> out = new java.util.ArrayList<>();
		int turns = BigParts.turns(getBlockState());
		for (int i = 0; i < NaturalDraftTowerBlock.FOOTPRINT.pieces(); i++) {
			int[] o = NaturalDraftTowerBlock.FOOTPRINT.offset(i, turns);
			out.add(worldPosition.offset(o[0], o[1], o[2]));
		}
		return out;
	}

	private static double dist(double x, double z, double[] c) {
		return Math.hypot(x - c[0], z - c[1]);
	}

	/** The cells of one ring of radius {@code r} (one block thick) at world layer {@code y}. */
	private static void ring(Set<BlockPos> into, double[] c, double r, int y) {
		int span = (int) Math.ceil(r) + 2;
		for (int x = (int) Math.floor(c[0]) - span; x <= (int) Math.floor(c[0]) + span; x++) {
			for (int z = (int) Math.floor(c[1]) - span; z <= (int) Math.floor(c[1]) + span; z++) {
				if (Math.abs(dist(x + 0.5, z + 0.5, c) - r) <= 0.5) {
					into.add(new BlockPos(x, y, z));
				}
			}
		}
	}

	/** The shell blocks of a finished stage (or of lift {@code lift} in the shell stage). */
	private Set<BlockPos> blocksOf(int of, int lift) {
		Set<BlockPos> out = new HashSet<>();
		double[] c = centre();
		int y0 = worldPosition.getY();
		switch (of) {
			case BASIN -> ring(out, c, Tuning.TOWER_BASIN_R + 0.5, y0);
			case COLUMNS -> {
				for (int k = 0; k < 40; k++) {
					for (double off : new double[] {-0.04, 0.04}) {
						double a = 2 * Math.PI * k / 40 + off;
						for (int dy = 1; dy < (int) Tuning.TOWER_SHELL_BASE_Y; dy++) {
							double r = Tuning.TOWER_BASIN_R - (Tuning.TOWER_BASIN_R - CoolingTower.radius(Tuning.TOWER_SHELL_BASE_Y)) * dy / Tuning.TOWER_SHELL_BASE_Y;
							out.add(BlockPos.containing(c[0] + r * Math.cos(a), y0 + dy, c[1] + r * Math.sin(a)));
						}
					}
				}
			}
			case SHELL -> {
				for (int y = y0 + (int) Tuning.TOWER_SHELL_BASE_Y; y < y0 + Tuning.TOWER_TOP_Y; y++) {
					double rel = y - y0 + 0.5;
					int i = Math.min(CoolingTower.lifts() - 1, (int) Math.floor((rel - Tuning.TOWER_SHELL_BASE_Y) / Tuning.TOWER_LIFT_M));
					if (i == lift) {
						ring(out, c, CoolingTower.radius(rel), y);
					}
				}
			}
			default -> {
			}
		}
		return out;
	}

	private static boolean placeable(Level level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		return !(s.getBlock() instanceof BigPart) && level.getBlockEntity(p) == null && !s.is(Blocks.BEDROCK);
	}

	private void place(Level level, Set<BlockPos> cells) {
		BlockState shell = WildcatterBlocks.TOWER_SHELL.defaultBlockState();
		for (BlockPos p : cells) {
			if (placeable(level, p) && (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced())) {
				level.setBlock(p, shell, 2);
			}
		}
	}

	/** Digs the pond inside the rim and fills it with water. */
	private void digPond(Level level) {
		double[] c = centre();
		int y0 = worldPosition.getY();
		int span = (int) Tuning.TOWER_BASIN_R + 1;
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int x = (int) Math.floor(c[0]) - span; x <= (int) Math.floor(c[0]) + span; x++) {
			for (int z = (int) Math.floor(c[1]) - span; z <= (int) Math.floor(c[1]) + span; z++) {
				if (dist(x + 0.5, z + 0.5, c) < Tuning.TOWER_BASIN_R) {
					for (int y = y0 - (int) Tuning.TOWER_BASIN_DEPTH; y < y0; y++) {
						BlockPos p = new BlockPos(x, y, z);
						if (placeable(level, p)) {
							level.setBlock(p, water, 2);
						}
					}
				}
			}
		}
		basinKg = CoolingTower.basinKg();
	}

	/** Breaking the controller: the shell blocks go and the pond is drained. */
	public void demolish(Level level) {
		Set<BlockPos> all = new HashSet<>();
		if (stage > BASIN) {
			all.addAll(blocksOf(BASIN, 0));
		}
		if (stage > COLUMNS) {
			all.addAll(blocksOf(COLUMNS, 0));
		}
		for (int i = 0; i < lifts; i++) {
			all.addAll(blocksOf(SHELL, i));
		}
		for (BlockPos p : all) {
			if (level.getBlockState(p).is(WildcatterBlocks.TOWER_SHELL)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
			}
		}
		if (stage > BASIN) {
			double[] c = centre();
			int y0 = worldPosition.getY();
			int span = (int) Tuning.TOWER_BASIN_R + 1;
			for (int x = (int) Math.floor(c[0]) - span; x <= (int) Math.floor(c[0]) + span; x++) {
				for (int z = (int) Math.floor(c[1]) - span; z <= (int) Math.floor(c[1]) + span; z++) {
					for (int y = y0 - (int) Tuning.TOWER_BASIN_DEPTH; y < y0; y++) {
						BlockPos p = new BlockPos(x, y, z);
						if (level.getBlockState(p).is(Blocks.WATER)) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}
		}
		java.util.Optional.ofNullable(TOWERS.get(level.dimension())).ifPresent(set -> set.remove(worldPosition));
	}

	/** The tower whose basin holds {@code pos}, if any. */
	public static @Nullable NaturalDraftTowerBlockEntity basinAt(Level level, BlockPos pos) {
		for (BlockPos at : TOWERS.getOrDefault(level.dimension(), Set.of())) {
			if (level.getBlockEntity(at) instanceof NaturalDraftTowerBlockEntity t && t.stage > BASIN) {
				double[] c = t.centre();
				if (dist(pos.getX() + 0.5, pos.getZ() + 0.5, c) < Tuning.TOWER_BASIN_R + 2) {
					return t;
				}
			}
		}
		return null;
	}

	// ---- the climate -------------------------------------------------------------------------------------------------------------
	private double wetBulb(Level level) {
		if (Double.isNaN(wetBulb) || level.getGameTime() % 1200 == 0) {
			double[] c = centre();
			Holder<Biome> biome = level.getBiome(BlockPos.containing(c[0], worldPosition.getY(), c[1]));
			double rh = !biome.value().hasPrecipitation() ? 20 : biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP) ? 85 : 60;
			wetBulb = CoolingTower.wetBulb(CoolingTower.dryBulb(biome.value().getBaseTemperature()), rh);
		}
		return wetBulb;
	}

	/** The wet bulb over the basin, C (NaN until it has worked it out). */
	public double wetBulbC() {
		return wetBulb;
	}

	/** The cold water's temperature, C. */
	public double coldC() {
		return Double.isNaN(cold) ? (Double.isNaN(wetBulb) ? Tuning.LAKE_C : wetBulb) : cold;
	}

	/** The basin is too low for the circulating water pumps. */
	public boolean dry() {
		return basinKg < CoolingTower.basinKg() * 0.1;
	}

	// ---- hoppers and water -------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private boolean takes(int slot, ItemStack stack) {
		return slot == 0 && stack.is(Items.CONCRETE.gray()) || slot == 1 && stack.is(WildcatterItems.STEEL_INGOT) || slot == 2 && stack.is(WildcatterItems.PLASTIC_SHEET);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takes(0, stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1, 2} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public double waterLevel() {
		return makeup / MAKEUP_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, MAKEUP_HOLD - makeup));
		makeup += fits;
		setChanged();
		return fits;
	}

	// ---- the work ----------------------------------------------------------------------------------------------------------------
	private double needConcrete() {
		return switch (stage) {
			case BASIN -> Tuning.TOWER_BASIN_CONCRETE;
			case COLUMNS -> Tuning.TOWER_COLUMNS_CONCRETE;
			case SHELL -> CoolingTower.liftConcrete(lifts);
			default -> 0;
		};
	}

	private double needSteelKg() {
		return switch (stage) {
			case BASIN -> Tuning.TOWER_BASIN_STEEL * Tuning.STEEL_INGOT_KG;
			case COLUMNS -> Tuning.TOWER_COLUMNS_STEEL * Tuning.STEEL_INGOT_KG;
			case SHELL -> CoolingTower.liftSteelKg(lifts);
			case FILL -> Tuning.TOWER_FILL_STEEL * Tuning.STEEL_INGOT_KG;
			default -> 0;
		};
	}

	private double needPlastic() {
		return stage == FILL ? Tuning.TOWER_FILL_PLASTIC : 0;
	}

	private double seconds(int of) {
		return switch (of) {
			case BASIN -> Tuning.TOWER_BASIN_SECONDS;
			case COLUMNS -> Tuning.TOWER_COLUMNS_SECONDS;
			case SHELL -> Tuning.TOWER_LIFT_SECONDS;
			default -> Tuning.TOWER_FILL_SECONDS;
		};
	}

	private String stageName() {
		return switch (stage) {
			case BASIN -> "BASIN";
			case COLUMNS -> "RAKER COLUMNS";
			case SHELL -> String.format("SHELL, lift %d of %d (%.1f m)", lifts + 1, CoolingTower.lifts(), CoolingTower.liftY(lifts));
			case FILL -> "FILL AND DISTRIBUTION";
			default -> "BUILT";
		};
	}

	public static void tick(Level level, BlockPos pos, BlockState state, NaturalDraftTowerBlockEntity t) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0) {
			TOWERS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
			if (t.stage < DONE) {
				t.build(level);
			} else {
				t.cool(level);
			}
			t.setChanged();
		}
	}

	private void build(Level level) {
		double dt = Tuning.WELL_INTERVAL / 20.0;
		while (concrete < needConcrete() && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			concrete += 1;
		}
		while (steelKg < needSteelKg() && !slots.get(1).isEmpty()) {
			slots.get(1).shrink(1);
			steelKg += Tuning.STEEL_INGOT_KG;
		}
		while (plastic < needPlastic() && !slots.get(2).isEmpty()) {
			slots.get(2).shrink(1);
			plastic += 1;
		}
		if (concrete < needConcrete() - 1e-9 || steelKg < needSteelKg() - 1e-9 || plastic < needPlastic() - 1e-9) {
			status = String.format("%s: NEEDS %.1f gray concrete, %.1f steel ingots%s by the hopper on top (has %.1f, %.1f%s)", stageName(), needConcrete(),
					needSteelKg() / Tuning.STEEL_INGOT_KG, needPlastic() > 0 ? String.format(", %.0f plastic sheets", needPlastic()) : "", concrete,
					steelKg / Tuning.STEEL_INGOT_KG, needPlastic() > 0 ? String.format(", %.0f", plastic) : "");
			return;
		}
		progress += dt;
		status = String.format("%s: building, %.0f %%", stageName(), progress / seconds(stage) * 100);
		if (progress >= seconds(stage) - 1e-9) {
			finishUnit(level);
		}
	}

	private void finishUnit(Level level) {
		concrete -= needConcrete();
		steelKg -= needSteelKg();
		plastic -= needPlastic();
		progress = 0;
		switch (stage) {
			case BASIN -> {
				place(level, blocksOf(BASIN, 0));
				digPond(level);
				stage = COLUMNS;
			}
			case COLUMNS -> {
				place(level, blocksOf(COLUMNS, 0));
				stage = SHELL;
			}
			case SHELL -> {
				place(level, blocksOf(SHELL, lifts));
				lifts++;
				if (lifts >= CoolingTower.lifts()) {
					stage = FILL;
				}
			}
			case FILL -> stage = DONE;
			default -> {
			}
		}
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		setChanged();
	}

	/** For the game test: builds through to {@code lifts} lifts (all of them finishes it, the fill too). */
	public void buildForTest(Level level, int toLifts) {
		while (stage < DONE && !(stage == SHELL && lifts >= toLifts) && !(stage == FILL && toLifts < CoolingTower.lifts())) {
			concrete = needConcrete();
			steelKg = needSteelKg();
			plastic = needPlastic();
			finishUnit(level);
		}
	}

	private void cool(Level level) {
		double dt = Tuning.WELL_INTERVAL / 20.0;
		double wb = wetBulb(level);
		if (Double.isNaN(cold)) {
			cold = Math.max(Tuning.TOWER_MIN_COLD_C, wb);
		}
		double[] mix = new double[5];
		double kg = pullMixed(level, faces(at -> at[1] == 2, Direction.UP), Product.COOLING_WATER, 2.5 * Tuning.CW_PUMP_KGS * dt, mix);
		double watts = kg > 0 ? kg * 4186 * Math.max(0, mix[0] - cold) / dt : 0;
		heat += (watts - heat) * Math.min(1, dt / 20.0);
		cold += (Math.max(Tuning.TOWER_MIN_COLD_C, wb + CoolingTower.approach(heat)) - cold) * Math.min(1, dt / Tuning.TOWER_LAG_S);
		double use = CoolingTower.makeUp(heat) * dt;
		evaporated += CoolingTower.evaporation(heat) * dt;
		basinKg = Math.max(0, basinKg - use);
		double fill = Math.min(Math.min(makeup, CoolingTower.basinKg() - basinKg), CoolingTower.makeUp(Tuning.TOWER_DESIGN_W) * 3 * dt);
		makeup -= fill;
		basinKg += fill;
		boolean was = plume;
		plume = heat > 5e6;
		if (plume != was) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		status = String.format("BUILT: %.1f MW of heat, wet bulb %.1f C, cold water %.1f C (approach %.1f), evaporation %.1f kg/s, make-up %.1f kg/s; basin %.0f %%%s",
				heat / 1e6, wb, cold, cold - wb, CoolingTower.evaporation(heat), CoolingTower.makeUp(heat), basinKg / CoolingTower.basinKg() * 100,
				dry() ? " (LOW: pump make-up water to the tower)" : "");
	}

	/** For the game test: the tower as if rejecting {@code watts}, settled. */
	public void setHeatForTest(Level level, double watts) {
		heat = watts;
		cold = Math.max(Tuning.TOWER_MIN_COLD_C, wetBulb(level) + CoolingTower.approach(watts));
	}

	/** The client: the plume over the top while it rejects heat. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, NaturalDraftTowerBlockEntity t) {
		if (!t.plume || level.getGameTime() % 2 != 0) {
			return;
		}
		double[] c = t.centre();
		double top = pos.getY() + Tuning.TOWER_TOP_Y;
		for (int i = 0; i < 6; i++) {
			double a = level.getRandom().nextDouble() * 2 * Math.PI;
			double r = Math.sqrt(level.getRandom().nextDouble()) * CoolingTower.radius(Tuning.TOWER_TOP_Y) * 0.9;
			level.addAlwaysVisibleParticle(ParticleTypes.CLOUD, true, c[0] + r * Math.cos(a), top, c[1] + r * Math.sin(a), 0, 0.25 + level.getRandom().nextDouble() * 0.2, 0);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Natural-draft cooling tower (88 m, hyperbolic, no fans): " + status + String.format("; make-up water %.0f L", makeup));
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide()) {
			java.util.Optional.ofNullable(TOWERS.get(level.dimension())).ifPresent(set -> set.remove(worldPosition));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("stage", stage);
		output.putInt("lifts", lifts);
		output.putDouble("progress", progress);
		output.putDouble("concrete", concrete);
		output.putDouble("steelKg", steelKg);
		output.putDouble("plastic", plastic);
		output.putDouble("cold", Double.isNaN(cold) ? -999 : cold);
		output.putDouble("heat", heat);
		output.putDouble("basinKg", basinKg);
		output.putDouble("makeup", makeup);
		output.putDouble("evaporated", evaporated);
		output.putBoolean("plume", plume);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		stage = input.getIntOr("stage", 0);
		lifts = input.getIntOr("lifts", 0);
		progress = input.getDoubleOr("progress", 0);
		concrete = input.getDoubleOr("concrete", 0);
		steelKg = input.getDoubleOr("steelKg", 0);
		plastic = input.getDoubleOr("plastic", 0);
		double c = input.getDoubleOr("cold", -999);
		cold = c < -900 ? Double.NaN : c;
		heat = input.getDoubleOr("heat", 0);
		basinKg = input.getDoubleOr("basinKg", 0);
		makeup = input.getDoubleOr("makeup", 0);
		evaporated = input.getDoubleOr("evaporated", 0);
		plume = input.getBooleanOr("plume", false);
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
