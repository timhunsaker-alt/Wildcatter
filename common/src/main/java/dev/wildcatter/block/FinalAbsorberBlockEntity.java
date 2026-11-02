package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.AcidPlant;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The final absorption tower and its stack (docs/multiblocks/acid-plant.md): the converter touching it hands it its SO3, which the
 * 98.5 % acid takes up with water off a pump. The plant's acid leaves the south face of its bottom cells.
 */
public class FinalAbsorberBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private double water;

	public FinalAbsorberBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FINAL_ABSORBER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 2 && at[1] == 0 && side == Direction.SOUTH ? Product.SULFURIC_ACID : null;
	}

	@Override
	protected double hold(Product product) {
		return AcidPlant.ratedAcidKgs() * AcidPlant.productPerAcid() / Tuning.ACID_KG_PER_L * Tuning.PLANT_HOLD_SECONDS * 4;
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

	/** The most acid (kg of product) it can make now: room at its nozzle and water. */
	public double canMake() {
		return Math.min(room(Product.SULFURIC_ACID) * Tuning.ACID_KG_PER_L, water / AcidPlant.waterPerProduct());
	}

	public boolean dry() {
		return water < 1;
	}

	/** Makes {@code kg} of product acid from the SO3 the converter gave it. */
	public void absorb(double kg) {
		water = Math.max(0, water - kg * AcidPlant.waterPerProduct());
		add(Product.SULFURIC_ACID, kg / Tuning.ACID_KG_PER_L);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Final absorber: %.0f %% acid; %.0f L of acid held; water %.0f/%.0f L", Tuning.ACID_STRENGTH * 100,
				held(Product.SULFURIC_ACID), water, Tuning.ACID_PLANT_WATER_HOLD));
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
