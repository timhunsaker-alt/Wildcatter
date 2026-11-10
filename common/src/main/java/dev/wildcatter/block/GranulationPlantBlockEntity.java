package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The granulation plant (docs/multiblocks/fertilizer.md): a pre-neutraliser, a rotating granulation drum, a gas-fired dryer, screens
 * and a cooler. Sneak-click to switch it between TSP (0-46-0, from rock slurry and merchant acid) and DAP (18-46-0, from merchant acid
 * and ammonia). Everything comes in on the east face of its bottom cells: rock slurry, merchant acid, ammonia, and gas or LPG for the
 * dryer (0.8 MW at the TSP rating). The granules go to a bagging station against it, in 1 t bags. 150 kW on the world's industrial
 * voltage, the box on the west face of its main cell.
 */
public class GranulationPlantBlockEntity extends UnitMotorBlockEntity implements Bagged {
	private boolean dap;
	private double tsp;      // kg of TSP granules ready for bags
	private double dapKg;    // kg of DAP granules ready for bags
	private double making;   // kg/h of granules, the last interval
	private String unit = "starting";

	public GranulationPlantBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GRANULATION_PLANT_ENTITY, pos, state);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[0] == 3 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** True on DAP. */
	public boolean onDap() {
		return dap;
	}

	/** Sneak-click: TSP to DAP and back. What it has made stays for the bags. */
	public String switchProduct() {
		dap = !dap;
		setChanged();
		return "Granulation plant now on " + (dap ? "DAP (18-46-0): merchant acid and ammonia" : "TSP (0-46-0): rock slurry and merchant acid");
	}

	/** kg/h of granules over the last interval. */
	public double making() {
		return making;
	}

	/** For the game test: granules as if made, on what it is making now. */
	public void addGranules(double kg) {
		if (dap) {
			dapKg += kg;
		} else {
			tsp += kg;
		}
		setChanged();
	}

	// ---- the bags: what it is on, else what it still holds of the other -------------------------------------------------------
	private boolean bagDap() {
		return dap ? dapKg >= Tuning.BULK_BAG_KG || tsp < Tuning.BULK_BAG_KG : tsp < Tuning.BULK_BAG_KG && dapKg >= Tuning.BULK_BAG_KG;
	}

	@Override
	public Item bagItem() {
		return bagDap() ? WildcatterItems.DAP_BAG : WildcatterItems.TSP_BAG;
	}

	@Override
	public double bagReady() {
		return bagDap() ? dapKg : tsp;
	}

	@Override
	public double takeForBag(double kg) {
		double got;
		if (bagDap()) {
			got = Math.min(kg, dapKg);
			dapKg -= got;
		} else {
			got = Math.min(kg, tsp);
			tsp -= got;
		}
		setChanged();
		return got;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double acid = Lines.available(level, east(), Product.MERCHANT_ACID, Pull.MACHINE, this) * Tuning.MERCHANT_ACID_KG_PER_L;
		if (acid < 1e-6) {
			unit = "NO ACID: pipe merchant acid to its east side";
			return;
		}
		double kg;
		double perAcid = dap ? Phosphate.merchantPerDap() : Phosphate.merchantPerTsp();
		if (dap) {
			double nh3 = Lines.available(level, east(), Product.AMMONIA, Pull.MACHINE, this) * Tuning.AMMONIA_KG_PER_L;
			if (nh3 < 1e-9) {
				unit = "NO AMMONIA: pipe ammonia to its east side, or sneak-click it to TSP";
				return;
			}
			kg = Math.min(Phosphate.dapKgs() * seconds(), nh3 / Phosphate.ammoniaPerDap());
		} else {
			double slurry = Lines.available(level, east(), Product.ROCK_SLURRY, Pull.MACHINE, this);
			if (slurry < 1e-6) {
				unit = "NO ROCK SLURRY: pipe the mill's rock slurry to its east side, or sneak-click it to DAP";
				return;
			}
			kg = Math.min(Phosphate.tspKgs() * seconds(), slurry / Phosphate.slurryPerRock() / Phosphate.rockPerTsp());
		}
		kg = Math.min(kg, acid / perAcid);
		kg = Math.min(kg, 2 * Tuning.BULK_BAG_KG - (dap ? dapKg : tsp));
		if (kg <= 1e-7) {
			unit = "FULL: set a bagging station against it";
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
		double need = kg * Phosphate.granulatorJPerKg();
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side for the dryer";
			return;
		}
		Lines.pull(level, east(), Product.MERCHANT_ACID, kg * perAcid / Tuning.MERCHANT_ACID_KG_PER_L, Pull.MACHINE, this);
		if (dap) {
			Lines.pull(level, east(), Product.AMMONIA, kg * Phosphate.ammoniaPerDap() / Tuning.AMMONIA_KG_PER_L, Pull.MACHINE, this);
			dapKg += kg;
		} else {
			Lines.pull(level, east(), Product.ROCK_SLURRY, kg * Phosphate.rockPerTsp() * Phosphate.slurryPerRock(), Pull.MACHINE, this);
			tsp += kg;
		}
		making = kg / seconds() * 3600;
		unit = String.format("granulating %s: %,.0f kg/h, the dryer on %.0f kW of gas", dap ? "DAP (18-46-0)" : "TSP (0-46-0)", making, got / seconds() / 1000);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Granulation plant (%.0f kW, %s; on %s): ", Tuning.GRANULATION_WATTS / 1000, Voltage.INDUSTRIAL.label(), dap ? "DAP" : "TSP") + unit
				+ String.format("; TSP %,.0f kg and DAP %,.0f kg ready for bags; ", tsp, dapKg) + describe());
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
		return RatePoints.ma(making, (dap ? Phosphate.dapKgs() : Phosphate.tspKgs()) * 3600);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("dap", dap);
		output.putDouble("tsp", tsp);
		output.putDouble("dapKg", dapKg);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		dap = input.getBooleanOr("dap", false);
		tsp = input.getDoubleOr("tsp", 0);
		dapKg = input.getDoubleOr("dapKg", 0);
	}
}
