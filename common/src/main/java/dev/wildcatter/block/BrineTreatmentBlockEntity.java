package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
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
 * The brine treatment (docs/multiblocks/chlor-alkali-plant.md), the chlor-alkali plant's controller: primary treatment (caustic for the
 * magnesium, carbonate made in line from caustic and CO2 for the calcium, a clarifier and polishing filters) and a pair of chelating
 * ion-exchange columns. It draws saturated brine from the saturator against it and sends it, treated, to the membrane cell room against
 * it. Liquid CO2 on the south face of its back cells; without it the brine keeps its calcium and the cell room's voltage creeps up.
 * 45 kW on the world's industrial voltage, its box on the north face of its main cell (the saturator covers its west side).
 */
public class BrineTreatmentBlockEntity extends UnitMotorBlockEntity {
	private double making;   // kg/h of salt treated, the last interval
	private double co2Total;
	private String unit = "starting";

	public BrineTreatmentBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BRINE_TREATMENT_ENTITY, pos, state);
	}

	private List<Lines.Face> co2In() {
		return faces(at -> at[2] == 7 && at[1] == 0, Direction.SOUTH);
	}

	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** kg/h of salt treated over the last interval: about 1,630 at the rating. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		BrineSaturatorBlockEntity saturator = Touching.find(level, worldPosition, BrineSaturatorBlockEntity.class, s -> true);
		MembraneCellRoomBlockEntity room = Touching.find(level, worldPosition, MembraneCellRoomBlockEntity.class, r -> true);
		if (saturator == null || room == null) {
			unit = "NOT BUILT: set " + (saturator == null ? "a brine saturator" : "the membrane cell room") + " against it";
			return;
		}
		double wantCl2 = room.brineWanted();
		if (wantCl2 <= 1e-7) {
			unit = "idle: the cell room's brine is full";
			saturator.unit = "idle: the treatment is full";
			return;
		}
		double cl2 = Math.min(wantCl2, Math.min(saturator.salt() / ChlorAlkaliPlant.saltPerCl2(), saturator.water() / Tuning.CA_WATER_PER_CL2));
		if (saturator.salt() <= 1e-6) {
			unit = "NO SALT: rock salt in the saturator's hopper, or brine from a brine well to its north side";
			saturator.unit = "NO SALT";
			return;
		}
		if (cl2 <= 1e-7) {
			unit = "NO WATER: pump make-up water to the saturator";
			saturator.unit = "NO WATER: pump make-up water to it";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = Math.min(drive(level), saturator.drive(level));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; saturator " + saturator.motor().describe();
			return;
		}
		cl2 *= strength;
		double co2Need = cl2 * ChlorAlkaliPlant.co2PerCl2();
		double co2 = Lines.pull(level, co2In(), Product.CO2, co2Need, Pull.MACHINE, this);
		co2Total += co2;
		boolean calcium = co2 < co2Need * 0.99;
		double salt = cl2 * ChlorAlkaliPlant.saltPerCl2();
		double water = cl2 * Tuning.CA_WATER_PER_CL2;
		saturator.draw(salt, water);
		room.giveBrine(salt, water, calcium);
		making = salt / seconds() * 3600;
		unit = String.format("treating %,.0f kg/h of salt as %,.0f kg/h of brine: magnesium out with caustic, calcium %s, polished and through the chelating columns",
				making, making * ChlorAlkaliPlant.brinePerSalt(), calcium ? "LEFT IN (NO CO2: pipe liquid CO2 to its south side)" : String.format("out as carbonate on %.1f kg/h of CO2", co2 / seconds() * 3600));
		saturator.unit = String.format("dissolving %,.0f kg/h of salt to %.0f %% brine", making, Tuning.BRINE_NACL * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Brine treatment (%.0f kW, %s): ", Tuning.BRINE_TREATMENT_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; %.0f kg of CO2 used; ", co2Total) + describe());
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
		return RatePoints.ma(making, ChlorAlkaliPlant.cl2Kgs() * ChlorAlkaliPlant.saltPerCl2() * 3600);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("co2Total", co2Total);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		co2Total = input.getDoubleOr("co2Total", 0);
	}
}
