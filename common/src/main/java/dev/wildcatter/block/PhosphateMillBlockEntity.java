package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
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
 * The phosphate mill (docs/multiblocks/fertilizer.md): a wet ball mill grinding phosphate rock to about 150 um in water off a pump.
 * Rock by a hopper on its top; rock slurry (62 % solids) out of the south face of its back cells, for the phosphoric reactor and the
 * granulation plant. Its motor is 200 kW on the world's medium voltage, the box on the west face of its main cell. It grinds what is
 * drawn: with nowhere for its slurry to go it idles.
 */
public class PhosphateMillBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double rock;     // kg of rock in the mill
	private double water;
	private double making;   // kg/s of rock ground, the last interval
	private String unit = "starting";

	public PhosphateMillBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PHOSPHATE_MILL_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.PHOSPHATE));
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.ROCK_SLURRY : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.ROCK_SLURRY ? Phosphate.millRockKgs() * Phosphate.slurryPerRock() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.PHOSPHATE_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.PHOSPHATE_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg/s of rock ground over the last interval. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double max = Phosphate.millRockKgs() * seconds();
		if (rock < max && fed() > 0) {
			useOne();
			rock += Ore.PHOSPHATE.blockKg();
		}
		if (rock < 1e-6) {
			unit = "NO ROCK: put a hopper of phosphate rock on its top";
			return;
		}
		if (water < 1e-6) {
			unit = "NO WATER: pump water to it";
			return;
		}
		double kg = Math.min(Math.min(max, rock), water / Phosphate.millWaterPerRock());
		kg = Math.min(kg, room(Product.ROCK_SLURRY) / Phosphate.slurryPerRock());
		if (kg <= 1e-7) {
			unit = "idle: FULL, nothing is drawing its slurry (pipe its south side to the reactor or the granulator)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		kg *= strength;
		rock -= kg;
		water = Math.max(0, water - kg * Phosphate.millWaterPerRock());
		add(Product.ROCK_SLURRY, kg * Phosphate.slurryPerRock());
		making = kg / seconds();
		unit = String.format("grinding %.2f kg/s of rock (%.0f t a day) to rock slurry at %.0f %% solids", making, making * 86.4, Tuning.ROCK_SLURRY_SOLIDS * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Phosphate mill (wet ball mill %.0f kW, %s): ", Tuning.PHOSPHATE_MILL_WATTS / 1000, Voltage.MEDIUM.label()) + unit
				+ String.format("; rock %,.0f kg and %d blocks; water %.0f/%.0f L; slurry held %.0f kg; ", rock, fed(), water, Tuning.PHOSPHATE_WATER_HOLD, held(Product.ROCK_SLURRY))
				+ describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, Phosphate.millRockKgs());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("rock", rock);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		rock = input.getDoubleOr("rock", 0);
		water = input.getDoubleOr("water", 0);
	}
}
