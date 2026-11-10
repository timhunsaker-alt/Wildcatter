package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Ferroalloys;
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
 * The moly roaster (docs/multiblocks/ferroalloys.md): a multiple-hearth roaster at 600-650 C with its own drum filter. Thickened
 * flotation concentrate (Tailings' concentrator slurry, its molybdenite in its assay) on the east face of its bottom cells; its rabble
 * drive and fans 22 kW at LV (box west). 2 MoS2 + 7 O2 -> 2 MoO3 + 4 SO2: 0.25 t/h of technical moly oxide (57 % Mo) to the bagging
 * station against it, and its off-gas on the pipe out of its south side as burner gas for an acid plant's converter. Real roasters must not
 * vent: with nowhere for the gas to go it reads NO ACID PLANT and stops. A concentrate carrying scheelite instead is only filtered and
 * dried, into bags of scheelite concentrate for the aluminothermic vessel.
 */
public class MolyRoasterBlockEntity extends UnitMotorBlockEntity implements Bagged {
	public static final String MOLYBDENITE = "wildcatter:molybdenite";
	public static final String SCHEELITE = "wildcatter:scheelite";

	private double mos2;        // kg in the filter cake
	private double scheelite;   // kg in the filter cake
	private double oxide;       // kg of moly oxide waiting for bags
	private double dried;       // kg of scheelite concentrate waiting for bags
	private double making;      // kg/h of oxide (or concentrate), the last interval
	private double madeOxide;   // t
	private String unit = "starting";

	public MolyRoasterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MOLY_ROASTER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 1 && at[1] == 0 && at[2] == 2 && side == Direction.SOUTH ? Product.BURNER_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.BURNER_GAS ? Tuning.ROASTER_OXIDE_KGH / 3600 * Ferroalloys.burnerGasPerOxide() * Tuning.PLANT_HOLD_SECONDS : 0;
	}

	public double making() {
		return making;
	}

	/** kg of oxide waiting for bags. */
	public double oxide() {
		return oxide;
	}

	@Override
	public Item bagItem() {
		return dried > oxide ? WildcatterItems.SCHEELITE_CONCENTRATE_BAG : WildcatterItems.MOLY_OXIDE_BAG;
	}

	@Override
	public double bagReady() {
		return Math.max(oxide, dried);
	}

	@Override
	public double takeForBag(double kg) {
		double got;
		if (dried > oxide) {
			got = Math.min(kg, dried);
			dried -= got;
		} else {
			got = Math.min(kg, oxide);
			oxide -= got;
		}
		setChanged();
		return got;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double dt = seconds();
		// the drum filter: slurry in while its cake is low
		if (mos2 + scheelite < Tuning.ROASTER_SLURRY_HOLD_KG * 0.05) {
			Slurry.Taken got = Slurry.take(level, inlet(), Product.THICKENED_CONCENTRATE, 50, this);
			mos2 += got.assay().kg(MOLYBDENITE, got.kg());
			scheelite += got.assay().kg(SCHEELITE, got.kg());
		}
		double oxideKg = Tuning.ROASTER_OXIDE_KGH / 3600 * dt;
		boolean roast = mos2 >= oxideKg * Ferroalloys.mos2PerOxide();
		if (!roast && scheelite <= 1e-6) {
			unit = mos2 > 0 ? "filtering: waiting for more concentrate" : "NO CONCENTRATE: pipe thickened concentrate carrying molybdenite or scheelite to its east side";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		if ((roast ? oxide : dried) >= 2 * Tuning.BULK_BAG_KG) {
			unit = "FULL: set a bagging station against it";
			return;
		}
		if (roast) {
			double gas = oxideKg * Ferroalloys.burnerGasPerOxide();
			if (room(Product.BURNER_GAS) < gas) {
				unit = "NO ACID PLANT: pipe its burner gas (south side) to an acid plant's converter; a roaster must not vent";
				return;
			}
			double strength = drive(level);
			if (strength <= 0) {
				unit = "starting: " + describe();
				return;
			}
			oxideKg *= strength;
			mos2 = Math.max(0, mos2 - oxideKg * Ferroalloys.mos2PerOxide());
			add(Product.BURNER_GAS, oxideKg * Ferroalloys.burnerGasPerOxide());
			oxide += oxideKg;
			madeOxide += oxideKg / 1000;
			making = oxideKg / dt * 3600;
			unit = String.format("roasting at 600-650 C: %.0f kg/h of moly oxide, %.0f kg/h of SO2 to the acid plant", making,
					making * Ferroalloys.so2PerOxide());
		} else {
			double strength = drive(level);
			if (strength <= 0) {
				unit = "starting: " + describe();
				return;
			}
			double kg = Math.min(scheelite, Tuning.ROASTER_OXIDE_KGH / 3600 * dt * strength * Ferroalloys.scheelitePerBag() / Tuning.BULK_BAG_KG);
			scheelite -= kg;
			dried += kg * Tuning.BULK_BAG_KG / Ferroalloys.scheelitePerBag();
			making = kg * Tuning.BULK_BAG_KG / Ferroalloys.scheelitePerBag() / dt * 3600;
			unit = String.format("drying scheelite concentrate: %.0f kg/h (%.0f %% WO3), no roasting", making, Tuning.SCHEELITE_BAG_WO3 * 100);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Moly roaster (multiple hearth, %.0f kW, %s): ", Tuning.ROASTER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; filter cake %.0f kg of MoS2, %.0f kg of scheelite; %.0f kg of oxide and %.0f kg of scheelite concentrate for bags; %.1f t of oxide made; ",
						mos2, scheelite, oxide, dried, madeOxide)
				+ describe());
	}

	// ---- PLC points: RUN, RUNNING, a 4-20 mA rate ---------------------------------------------------------------------------------
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
		return RatePoints.ma(making, Tuning.ROASTER_OXIDE_KGH);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("mos2", mos2);
		output.putDouble("scheelite", scheelite);
		output.putDouble("oxide", oxide);
		output.putDouble("dried", dried);
		output.putDouble("madeOxide", madeOxide);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		mos2 = input.getDoubleOr("mos2", 0);
		scheelite = input.getDoubleOr("scheelite", 0);
		oxide = input.getDoubleOr("oxide", 0);
		dried = input.getDoubleOr("dried", 0);
		madeOxide = input.getDoubleOr("madeOxide", 0);
	}
}
