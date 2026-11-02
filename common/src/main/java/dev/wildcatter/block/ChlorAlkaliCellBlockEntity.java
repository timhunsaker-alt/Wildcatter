package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Zirconium;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The chlor-alkali cell (docs/multiblocks/zirconium.md): a small bipolar membrane electrolyser, 6 cells at 2.8 kA and 3.1 V from the
 * rectifier it touches, 96 % current efficiency. Brine from rock salt (a hopper on top) and pumped water. Chlorine out of the south
 * face of its back cells, hydrogen out of the north face of its front cells; the caustic soda is counted.
 */
public class ChlorAlkaliCellBlockEntity extends PlantUnitBlockEntity implements HopperFed, WaterUser {
	private static final double WATER_HOLD = 400.0;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double salt, water, naoh;
	private double share;

	public ChlorAlkaliCellBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHLOR_ALKALI_CELL_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		return at[2] == 5 && side == Direction.SOUTH ? Product.CHLORINE : at[2] == 0 && side == Direction.NORTH ? Product.HYDROGEN : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.CHLORINE ? 20 : 10;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.HALITE));
	}

	@Override
	public double waterLevel() {
		return water / WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ChlorAlkaliCellBlockEntity c) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		if (c.salt <= 1e-6 && !c.useOne().isEmpty()) {
			c.salt += Ore.HALITE.kgPerBlock();
		}
		RectifierBlockEntity rectifier = Touching.find(level, pos, RectifierBlockEntity.class, r -> true);
		boolean work = rectifier != null && c.salt > 1e-6 && c.water > 0.01 && c.room(Product.CHLORINE) > 1e-6;
		c.share = rectifier == null ? 0 : rectifier.supply(work ? Zirconium.chlorAlkaliWatts() : 0, Zirconium.chlorAlkaliVolts());
		if (rectifier == null) {
			c.status = "NO DC: set a RECTIFIER against it";
		} else if (c.salt <= 1e-6) {
			c.status = "NO SALT: put rock salt in its hopper";
		} else if (c.water <= 0.01) {
			c.status = "NO WATER: pump water to it";
		} else if (!work) {
			c.status = "idle: its chlorine has nowhere to go (pipe its back end to a chlorinator)";
		} else {
			double cl2 = Math.min(Math.min(Zirconium.chlorAlkaliKgs() * seconds() * c.share, c.room(Product.CHLORINE)),
					Math.min(c.salt / Tuning.CA_SALT_PER_CL2, c.water / Tuning.CA_WATER_PER_CL2));
			c.salt -= cl2 * Tuning.CA_SALT_PER_CL2;
			c.water = Math.max(0, c.water - cl2 * Tuning.CA_WATER_PER_CL2);
			c.add(Product.CHLORINE, cl2);
			c.add(Product.HYDROGEN, cl2 * Zirconium.hydrogenM3PerCl2());   // what has nowhere to go is vented
			c.naoh += cl2 * Tuning.NAOH_PER_CL2;
			c.status = cl2 > 1e-9 ? String.format("%d cells at %.1f V and %.2f kA: %.1f kg/h of chlorine, %.1f m3/h of hydrogen", Tuning.CA_CELLS, Tuning.CA_CELL_VOLTS,
					Tuning.CA_CELL_KA * c.share, cl2 / seconds() * 3600, cl2 * Zirconium.hydrogenM3PerCl2() / seconds() * 3600) : "starting: waiting for DC from the rectifier";
		}
		c.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Chlor-alkali cell (membrane): %s; salt %,.0f kg, water %.0f/%.0f L; caustic soda %.0f kg made", status, salt, water,
				WATER_HOLD, naoh));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("salt", salt);
		output.putDouble("water", water);
		output.putDouble("naoh", naoh);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		salt = input.getDoubleOr("salt", 0);
		water = input.getDoubleOr("water", 0);
		naoh = input.getDoubleOr("naoh", 0);
	}
}
