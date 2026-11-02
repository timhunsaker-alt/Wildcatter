package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Mill;
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
 * The SAG mill (522 kW, 4.16 kV) (docs/multiblocks/uranium-mill.md): grinds the jaw crusher's ore to -0.5 mm in water off a pump, at
 * 55 % solids. Ore slurry out of the south face of its back cells, carrying its U3O8. It feeds no faster than SX can take the
 * uranium: rich pitchblende goes through slower.
 */
public class SagMillBlockEntity extends UnitMotorBlockEntity implements WaterUser {
	private double water;
	private String unit = "starting";

	public SagMillBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SAG_MILL_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 4 && at[1] == 0 && side == Direction.SOUTH ? Product.ORE_SLURRY : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.MILL_ORE_KGS / Tuning.MILL_SLURRY_SOLIDS * Tuning.PLANT_HOLD_SECONDS;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.MILL_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.MILL_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	@Override
	protected void work(Level level) {
		JawCrusherBlockEntity crusher = Touching.find(level, worldPosition, JawCrusherBlockEntity.class, c -> true);
		if (crusher == null) {
			unit = "NOT BUILT: set a JAW CRUSHER against the mill";
			return;
		}
		if (!crusher.hasOre()) {
			unit = "idle: no ore in the crusher (put a hopper of uranium ore or pitchblende on it)";
			return;
		}
		if (water < 1) {
			unit = "NO WATER: pump water to the mill";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double grade = crusher.grade();
		double rate = Mill.oreKgs(grade);
		double max = Math.min(Math.min(rate * seconds() * strength, room(Product.ORE_SLURRY) * Tuning.MILL_SLURRY_SOLIDS), water / Mill.grindWaterPerOre());
		double[] u = new double[1];
		double ore = max > 0.001 ? crusher.crush(level, max, u) : 0;
		if (ore <= 0) {
			unit = max <= 0.001 ? "idle: its slurry has nowhere to go (pipe its back end to a leach tank)" : "starting: " + crusher.describe();
			return;
		}
		double slurry = ore / Tuning.MILL_SLURRY_SOLIDS;
		water = Math.max(0, water - ore * Mill.grindWaterPerOre());
		addMixed(Product.ORE_SLURRY, slurry, new double[] {u[0] / slurry, Tuning.MILL_SLURRY_SOLIDS, 0, 0, 0});
		unit = String.format("grinding %.1f kg/s of ore at %.1f %% U3O8: %.3f kg/s of U3O8%s", ore / seconds(), grade * 100, u[0] / seconds(),
				rate < Tuning.MILL_ORE_KGS - 0.01 ? String.format(" (feed held to SX's %.2f kg/s of U3O8)", Tuning.MILL_U3O8_KGS) : "");
	}

	@Override
	public Component inspect() {
		return Component.literal("SAG mill (522 kW): " + unit + String.format("; water %.0f/%.0f L; ", water, Tuning.MILL_WATER_HOLD) + describe());
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
