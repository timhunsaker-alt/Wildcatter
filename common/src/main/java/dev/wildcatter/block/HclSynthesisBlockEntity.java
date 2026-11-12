package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The HCl synthesis unit (docs/multiblocks/chlor-alkali-plant.md): a graphite burner where hydrogen burns in chlorine to HCl, absorbed
 * in water (off a pump) to 32 % hydrochloric acid, up to 100 kg of HCl an hour. Hydrogen in on the north face of its bottom cells, dry
 * chlorine on the east; the acid out of the south face, and the hydrogen it does not burn on, out of the west face of its back cell.
 * Its spec gives it no motor.
 */
public class HclSynthesisBlockEntity extends ProcessPartBlockEntity implements WaterUser {
	private static final double WATER_HOLD = 1_000.0;
	private double water;
	private double making;   // kg/h of HCl, the last interval
	private double total;

	public HclSynthesisBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HCL_SYNTHESIS_UNIT_ENTITY, pos, state);
	}

	private List<Lines.Face> hydrogenIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> chlorineIn() {
		return faces(at -> at[0] == 1 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0 || at[2] != 1) {
			return null;
		}
		return side == Direction.SOUTH ? Product.HYDROCHLORIC_ACID : side == Direction.WEST && at[0] == 0 ? Product.HYDROGEN : null;
	}

	@Override
	protected double hold(Product product) {
		return switch (product) {
			case HYDROCHLORIC_ACID -> ChlorAlkaliPlant.hclKgs() / Tuning.HCL_STRENGTH * Tuning.PLANT_HOLD_SECONDS * 8;
			case HYDROGEN -> ChlorAlkaliPlant.cl2Kgs() * ChlorAlkaliPlant.hydrogenM3PerCl2() * Tuning.PLANT_HOLD_SECONDS * 4;
			default -> 0;
		};
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

	/** kg/h of HCl over the last interval: up to 100. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double h2 = Lines.available(level, hydrogenIn(), Product.HYDROGEN, Pull.MACHINE, this);
		if (h2 < 1e-6) {
			status = "idle: no hydrogen at its north side";
			return;
		}
		double cl2 = Lines.available(level, chlorineIn(), Product.CHLORINE, Pull.MACHINE, this);
		double hcl = Math.min(ChlorAlkaliPlant.hclKgs() * seconds(), h2 / ChlorAlkaliPlant.h2M3PerHcl());
		hcl = Math.min(hcl, cl2 / ChlorAlkaliPlant.cl2PerHcl());
		hcl = Math.min(hcl, water / ChlorAlkaliPlant.waterPerHcl());
		hcl = Math.min(hcl, room(Product.HYDROCHLORIC_ACID) * Tuning.HCL_STRENGTH);
		String burner;
		if (cl2 < 1e-6) {
			burner = "NO CHLORINE: pipe dry chlorine to its east side; hydrogen passing on";
			hcl = 0;
		} else if (water < 1e-6) {
			burner = "NO WATER: pump water to it for the absorber; hydrogen passing on";
			hcl = 0;
		} else if (hcl <= 1e-7) {
			burner = "FULL: its acid has nowhere to go (pipe its south side to a tank); hydrogen passing on";
			hcl = 0;
		} else {
			burner = "";
		}
		if (hcl > 0) {
			Lines.pull(level, chlorineIn(), Product.CHLORINE, hcl * ChlorAlkaliPlant.cl2PerHcl(), Pull.MACHINE, this);
			water = Math.max(0, water - hcl * ChlorAlkaliPlant.waterPerHcl());
			add(Product.HYDROCHLORIC_ACID, hcl / Tuning.HCL_STRENGTH);
			total += hcl;
			making = hcl / seconds() * 3600;
		}
		double used = hcl * ChlorAlkaliPlant.h2M3PerHcl();
		double pass = Math.min(h2 - used, room(Product.HYDROGEN));
		Lines.pull(level, hydrogenIn(), Product.HYDROGEN, used + Math.max(0, pass), Pull.MACHINE, this);
		add(Product.HYDROGEN, Math.max(0, pass));
		status = (burner.isEmpty() ? String.format("burning %.1f Nm3/h of hydrogen in %.0f kg/h of chlorine: %.0f kg/h of HCl as %.0f kg/h of %.0f %% acid",
				used / seconds() * 3600, hcl * ChlorAlkaliPlant.cl2PerHcl() / seconds() * 3600, making, making / Tuning.HCL_STRENGTH, Tuning.HCL_STRENGTH * 100) : burner)
				+ String.format("; %.0f Nm3/h of hydrogen on out of its west side", Math.max(0, pass) / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal("HCl synthesis unit (graphite burner and absorber): " + status + String.format("; water %.0f/%.0f L; acid held %.0f kg; %.0f kg of HCl made",
				water, WATER_HOLD, held(Product.HYDROCHLORIC_ACID), total));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
		output.putDouble("total", total);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
		total = input.getDoubleOr("total", 0);
	}
}
