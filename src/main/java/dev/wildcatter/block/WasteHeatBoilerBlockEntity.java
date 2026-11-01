package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.AcidPlant;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The acid plant's waste heat boiler (docs/multiblocks/acid-plant.md): the sulfur furnace touching it hands it the hot burner gas,
 * which it cools to about 420 C for the converter's first bed, raising 40 bar steam from water off a pump. Burner gas out of the south
 * face of its back cells.
 */
public class WasteHeatBoilerBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private double water;
	private double steam;      // kg/s, the last interval
	private long steamed = -1000;

	public WasteHeatBoilerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.WASTE_HEAT_BOILER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 7 && at[1] == 0 && side == Direction.SOUTH ? Product.BURNER_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return AcidPlant.ratedSulfurKgs() * AcidPlant.gasPerSulfur() * Tuning.PLANT_HOLD_SECONDS;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.ACID_PLANT_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.ACID_PLANT_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg of steam a kg of sulfur burned raises. */
	private static double steamPerSulfur() {
		return Tuning.ACID_STEAM_PER_ACID * AcidPlant.H2SO4 / AcidPlant.S;
	}

	/** The most sulfur (kg) whose gas it can take now: room at its outlet and water for the steam. */
	public double canTake() {
		return Math.min(room(Product.BURNER_GAS) / AcidPlant.gasPerSulfur(), water / steamPerSulfur());
	}

	public boolean dry() {
		return water < 1;
	}

	/** The furnace burned {@code kg} of sulfur into it over {@code seconds}. */
	public void burned(Level level, double kg, double seconds) {
		add(Product.BURNER_GAS, kg * AcidPlant.gasPerSulfur());
		water = Math.max(0, water - kg * steamPerSulfur());
		steam = kg * steamPerSulfur() / seconds;
		if (kg > 0) {
			steamed = level.getGameTime();
		}
		setChanged();
	}

	/** True while it is raising steam: what the melter's coils need. */
	public boolean steaming(Level level) {
		return level.getGameTime() - steamed < 40;
	}

	@Override
	public Component inspect() {
		boolean on = level != null && steaming(level);
		return Component.literal("Waste heat boiler: " + (on ? String.format("raising %.1f kg/s of 40 bar steam", steam) : "cold")
				+ String.format("; water %.0f/%.0f L; burner gas %.0f kg held", water, Tuning.ACID_PLANT_WATER_HOLD, held(Product.BURNER_GAS)));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
	}
}
