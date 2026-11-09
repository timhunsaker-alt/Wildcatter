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
 * face of its back cells. Its steam (docs/multiblocks/acid-plant-steam.md): the melter touching it takes its share first, and the rest
 * goes out of its top face on a steam pipe to an acid superheater, or up its silencer. A feed pump touching it returns the turbine's
 * condensate, so its make-up falls to a tenth.
 */
public class WasteHeatBoilerBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private double water;
	private double steam;      // kg/s, the last interval
	private long steamed = -1000;
	private double returned;   // kg/s of condensate back from the feed pump, the last few seconds
	private long returnedAt = -1000;

	public WasteHeatBoilerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.WASTE_HEAT_BOILER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] == 3 && side == Direction.UP) {
			return Product.STEAM;   // main steam, 40 bar saturated, out of its top
		}
		return at[2] == 7 && at[1] == 0 && side == Direction.SOUTH ? Product.BURNER_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.STEAM ? dev.wildcatter.fluid.AcidSteam.boilerKgs() * Tuning.PLANT_HOLD_SECONDS
				: AcidPlant.ratedSulfurKgs() * AcidPlant.gasPerSulfur() * Tuning.PLANT_HOLD_SECONDS;
	}

	/** The feed pump returns {@code kg} of the turbine's condensate. */
	public void returnCondensate(Level level, double kg, double seconds) {
		water = Math.min(Tuning.ACID_PLANT_WATER_HOLD, water + kg);
		returned = seconds > 0 ? kg / seconds : 0;
		returnedAt = level.getGameTime();
		setChanged();
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
		double raised = kg * steamPerSulfur();
		water = Math.max(0, water - raised);
		steam = raised / seconds;
		// the melter's coils take their share first; the rest goes to the steam line (what is not taken goes up the silencer)
		boolean melter = Touching.find(level, worldPosition, SulfurMelterBlockEntity.class, m -> true) != null;
		double coils = melter ? Math.min(raised, Tuning.MELTER_STEAM_KGS * seconds) : 0;
		add(Product.STEAM, raised - coils);
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
		boolean condensate = level != null && level.getGameTime() - returnedAt < 40 && returned > 0;
		return Component.literal("Waste heat boiler: " + (on ? String.format("raising %.1f kg/s of 40 bar steam", steam) : "cold")
				+ String.format("; water %.0f/%.0f L; burner gas %.0f kg held; steam %.0f kg held", water, Tuning.ACID_PLANT_WATER_HOLD, held(Product.BURNER_GAS), held(Product.STEAM))
				+ (condensate ? String.format("; %.2f kg/s of condensate back from the feed pump", returned) : ""));
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
