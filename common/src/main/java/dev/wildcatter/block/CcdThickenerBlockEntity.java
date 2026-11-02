package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Mill;
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
 * The high-rate thickener standing for the counter-current decantation train (7.5 kW rake drive, 480 V)
 * (docs/multiblocks/uranium-mill.md): leach slurry in on the north face of its front cells, washed with 2.5 kg of water a kg of
 * solids off a pump. The solids settle to the tailings at 50 % solids; the overflow, the pregnant liquor with the uranium, leaves the
 * south face of its back cells.
 */
public class CcdThickenerBlockEntity extends UnitMotorBlockEntity implements WaterUser {
	private double water;
	private double tailings;   // tonnes of solids sent to the tailings
	private String unit = "starting";

	public CcdThickenerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CCD_THICKENER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 11 && at[1] == 0 && side == Direction.SOUTH ? Product.PREGNANT_LIQUOR : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.MILL_ORE_KGS / Tuning.MILL_SLURRY_SOLIDS * Mill.liquorPerSlurry(Tuning.MILL_SLURRY_SOLIDS) * 1.1 * Tuning.PLANT_HOLD_SECONDS;
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
		double waiting = Lines.available(level, inlet(), Product.LEACH_SLURRY, Pull.MACHINE, this);
		if (waiting < 0.001) {
			unit = "idle: no leach slurry at its front (north face)";
			return;
		}
		if (water < 1) {
			unit = "NO WASH WATER: pump water to the thickener";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double solidsShare = Tuning.MILL_SLURRY_SOLIDS;   // close enough to bound the pull; the true share comes with the slurry
		double max = Math.min(Tuning.MILL_ORE_KGS / solidsShare * 1.1 * seconds() * strength, waiting);
		max = Math.min(Math.min(max, water / (solidsShare * Mill.washPerSolids())), room(Product.PREGNANT_LIQUOR) / Mill.liquorPerSlurry(solidsShare));
		double[] in = new double[5];
		double got = max > 0.001 ? pullMixed(level, inlet(), Product.LEACH_SLURRY, max, in) : 0;
		if (got <= 0) {
			unit = "idle: its pregnant liquor has nowhere to go (pipe its back end to the SX circuit)";
			return;
		}
		double solids = got * in[1];
		water = Math.max(0, water - solids * Mill.washPerSolids());
		double liquor = got * Mill.liquorPerSlurry(in[1]);
		double u = got * in[0];
		addMixed(Product.PREGNANT_LIQUOR, liquor, new double[] {liquor > 0 ? u / liquor : 0, 0, 0, 0, 0});
		tailings += solids / 1000;
		unit = String.format("washing %.1f kg/s of solids with %.0f L/s of water: %.0f L/s of pregnant liquor at %.1f g/L U3O8", solids / seconds(),
				solids * Mill.washPerSolids() / seconds(), liquor / seconds(), liquor > 0 ? u / liquor * 1000 : 0);
	}

	@Override
	public Component inspect() {
		return Component.literal("CCD thickener: " + unit + String.format("; %,.1f t of solids to the tailings; water %.0f/%.0f L; ", tailings, water,
				Tuning.MILL_WATER_HOLD) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
		output.putDouble("tailings", tailings);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
		tailings = input.getDoubleOr("tailings", 0);
	}
}
