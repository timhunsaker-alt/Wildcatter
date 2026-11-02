package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SpentFuel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The spent fuel pool's controller (docs/multiblocks/fuel-handling.md): its cooling pump (75 kW, 480 V), exchanger and cleanup skid at
 * the pool's edge. The 6 x 6 m pool, 12 m deep, lies just in front of it: it digs and lines it in concrete and fills it with water on
 * its first tick. It keeps each irradiated assembly the refuelling machine sends (never an item), cools them on circulating water
 * (in on its west face, out on its east), boils off without cooling, losing its top layers, and takes make-up water off a pump.
 */
public class SpentFuelPoolBlockEntity extends UnitMotorBlockEntity implements WaterUser {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> POOLS = new HashMap<>();
	private static final double MAKEUP_HOLD = 1000.0;

	/** Forgets every pool; called when the server stops so a new world starts clean. */
	public static void clearAll() {
		POOLS.clear();
	}

	private final List<SpentFuel.Assembly> racks = new ArrayList<>();
	private boolean dug;
	private double tempC = 30;
	private double kg = Tuning.POOL_WATER_KG;
	private int layers = Tuning.POOL_DEPTH;
	private double makeup;
	private double cooling;
	private String unit = "starting";

	public SpentFuelPoolBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SPENT_FUEL_POOL_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[1] == 0 && side == Direction.EAST ? Product.COOLING_WATER : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.POOL_CW_KGS * 2;
	}

	public static Set<BlockPos> pools(Level level) {
		return POOLS.getOrDefault(level.dimension(), Set.of());
	}

	public List<SpentFuel.Assembly> racks() {
		return racks;
	}

	public boolean full() {
		return racks.size() >= Tuning.POOL_RACKS;
	}

	public double tempC() {
		return tempC;
	}

	public int layers() {
		return layers;
	}

	/** The refuelling machine's delivery. */
	public void store(Level level, SpentFuel.Assembly a) {
		racks.add(a);
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	// ---- the pool's cells --------------------------------------------------------------------------------------------------------
	/** The pool's water cell {@code across}, {@code front} (both 0-5) at {@code depth} (1 is the top layer). */
	public BlockPos cell(int across, int front, int depth) {
		Direction f = getBlockState().getValue(BigPartBlock.FACING);
		Direction right = f.getClockWise();
		return worldPosition.relative(right, across - 1).relative(f, front + 1).below(depth);
	}

	/** Where rack {@code i}'s assembly stands: the middle of its cell, 2 m over the floor. */
	public double[] rackPoint(int i) {
		BlockPos c = cell(i % Tuning.POOL_SIDE, i / Tuning.POOL_SIDE, Tuning.POOL_DEPTH);
		return new double[] {c.getX() + 0.5, c.getY() + 2.0, c.getZ() + 0.5};
	}

	private void dig(Level level) {
		BlockState liner = Blocks.CONCRETE.lightGray().defaultBlockState();
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int a = -1; a <= Tuning.POOL_SIDE; a++) {
			for (int f = -1; f <= Tuning.POOL_SIDE; f++) {
				boolean wall = a < 0 || f < 0 || a >= Tuning.POOL_SIDE || f >= Tuning.POOL_SIDE;
				for (int d = 1; d <= Tuning.POOL_DEPTH + 1; d++) {
					BlockPos p = cell(a, f, d);
					if (level.getBlockEntity(p) != null || level.getBlockState(p).getBlock() instanceof dev.wildcatter.multiblock.BigPart) {
						continue;
					}
					level.setBlock(p, wall || d == Tuning.POOL_DEPTH + 1 ? liner : water, 2);
				}
			}
		}
		dug = true;
	}

	/** Sets the pool's top water layers to match its water. */
	private void level(Level level) {
		int want = (int) Math.ceil(kg / (Tuning.POOL_SIDE * Tuning.POOL_SIDE * 1000.0) - 1e-9);
		want = Math.max(0, Math.min(Tuning.POOL_DEPTH, want));
		while (layers != want) {
			int d = layers > want ? Tuning.POOL_DEPTH - layers + 1 : Tuning.POOL_DEPTH - layers;
			BlockState to = layers > want ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
			for (int a = 0; a < Tuning.POOL_SIDE; a++) {
				for (int f = 0; f < Tuning.POOL_SIDE; f++) {
					level.setBlock(cell(a, f, d), to, 2);
				}
			}
			layers += layers > want ? -1 : 1;
		}
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

	/** For the game test: the pool holding only {@code kgWater}. */
	public void setWaterForTest(Level level, double kgWater) {
		kg = kgWater;
		level(level);
	}

	// ---- its work ----------------------------------------------------------------------------------------------------------------
	@Override
	protected void work(Level level) {
		POOLS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(worldPosition.immutable());
		if (!dug) {
			dig(level);
		}
		double dt = seconds();
		double heat = 0;
		for (SpentFuel.Assembly a : racks) {
			a.decay(dt);
			heat += a.watts();
		}
		cooling = 0;
		if (heat > 0) {
			double strength = drive(level);
			double[] mix = new double[5];
			double cw = strength > 0 ? pullMixed(level, faces(at -> at[1] == 0, Direction.WEST), Product.COOLING_WATER, Tuning.POOL_CW_KGS * dt * strength, mix) : 0;
			double inC = cw > 0 && mix[0] > 0.1 ? mix[0] : Tuning.LAKE_C;
			if (cw > 0) {
				double dT = Math.max(0, tempC - inC);
				cooling = Math.min(Tuning.POOL_UA_W_PER_C * dT, cw / dt * 4186 * dT * 0.8);
				add(Product.COOLING_WATER, Math.min(cw, room(Product.COOLING_WATER)));
			}
		}
		double net = (heat - cooling) * dt;
		if (tempC >= 100 && net > 0) {
			kg = Math.max(0, kg - net / Tuning.POOL_LATENT_J_PER_KG);
		} else {
			tempC = Math.min(100, tempC + net / dev.wildcatter.fluid.SpentFuel.poolHeat());
		}
		double fill = Math.min(makeup, Tuning.POOL_WATER_KG - kg);
		makeup -= fill;
		kg += fill;
		level(level);
		double over = layers - Tuning.POOL_FUEL_TOP_M;
		unit = String.format("%d of %d racks; decay heat %.0f kW, cooling %.0f kW; %.1f C%s; water %.1f m (%.1f m over the fuel)", racks.size(), Tuning.POOL_RACKS,
				heat / 1000, cooling / 1000, tempC, tempC >= 100 ? " BOILING: pump make-up water to it" : "", (double) layers, over);
	}

	@Override
	public Component inspect() {
		return Component.literal("Spent fuel pool (6 x 6 m, 12 m deep): " + unit + "; " + describe());
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide()) {
			POOLS.getOrDefault(level.dimension(), Set.of()).remove(worldPosition);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("dug", dug);
		output.putDouble("tempC", tempC);
		output.putDouble("kg", kg);
		output.putInt("layers", layers);
		output.putDouble("makeup", makeup);
		output.putInt("racks", racks.size());
		for (int i = 0; i < racks.size(); i++) {
			SpentFuel.Assembly a = racks.get(i);
			output.putDouble("r" + i + "b", a.burnup);
			output.putBoolean("r" + i + "x", a.damaged);
			for (int g = 0; g < a.decay.length; g++) {
				output.putDouble("r" + i + "d" + g, a.decay[g]);
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		dug = input.getBooleanOr("dug", false);
		tempC = input.getDoubleOr("tempC", 30);
		kg = input.getDoubleOr("kg", Tuning.POOL_WATER_KG);
		layers = input.getIntOr("layers", Tuning.POOL_DEPTH);
		makeup = input.getDoubleOr("makeup", 0);
		racks.clear();
		int n = input.getIntOr("racks", 0);
		for (int i = 0; i < n; i++) {
			double[] d = new double[Tuning.DECAY_LAMBDA.length];
			for (int g = 0; g < d.length; g++) {
				d[g] = input.getDoubleOr("r" + i + "d" + g, 0);
			}
			racks.add(new SpentFuel.Assembly(input.getDoubleOr("r" + i + "b", 0), d, input.getBooleanOr("r" + i + "x", false)));
		}
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
