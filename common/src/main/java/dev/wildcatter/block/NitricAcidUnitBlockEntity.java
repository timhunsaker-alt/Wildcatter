package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.AmmoniaPlant;
import dev.wildcatter.fluid.Product;
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
 * The nitric acid unit (docs/multiblocks/ammonia-plant.md), the Ostwald process: its air blower, the burner with a platinum-rhodium
 * gauze at 850-900 C (4 NH3 + 5 O2 -> 4 NO + 6 H2O), a cooler and an absorption tower making 60 % acid with process water off a pump.
 * Ammonia in on the east face of its bottom cells; acid out of the south face of its back cells. Gauze packs by a hopper on top or by
 * hand; the gauze loses platinum slowly and a pack is changed when worn. The blower is its load (15 kW at the industrial voltage),
 * its terminal box on the west face of its main cell.
 */
public class NitricAcidUnitBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double gauze;      // g of Pt-Rh in the pack on the burner; 0 with no pack
	private double lost;       // g that pack has lost
	private double water;
	private double making;     // kg/h of HNO3 (as 100 %), the last interval
	private String unit = "starting";

	public NitricAcidUnitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.NITRIC_ACID_UNIT_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.GAUZE_PACK);
	}

	private List<Lines.Face> inlets() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 3 && at[1] == 0 && side == Direction.SOUTH ? Product.NITRIC_ACID : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.NITRIC_ACID ? AmmoniaPlant.acidLph() / 3600 * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.AMMONIA_PLANT_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.AMMONIA_PLANT_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg/h of HNO3 (as 100 %) over the last interval. */
	public double making() {
		return making;
	}

	/** g of platinum-rhodium in the pack on the burner. */
	public double gauze() {
		return gauze - lost;
	}

	/** Game test hook: wears the pack on the burner to {@code share} of its metal lost. */
	public void testWear(double share) {
		lost = gauze * share;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		if (gauze <= 0 || lost >= gauze * Tuning.GAUZE_WORN_SHARE) {
			gauze = 0;
			lost = 0;
			if (fed() > 0) {
				useOne();
				gauze = Tuning.GAUZE_PT_G;
			} else {
				unit = "NO GAUZE: put a platinum-rhodium gauze pack in a hopper on its top, or click one in";
				return;
			}
		}
		double ammoniaKg = Lines.available(level, inlets(), Product.AMMONIA, Pull.MACHINE, this) * Tuning.AMMONIA_KG_PER_L;
		if (ammoniaKg < 1e-9) {
			unit = "NO AMMONIA: pipe ammonia to its east side";
			return;
		}
		if (water < 1e-6) {
			unit = "NO WATER: pump water to its absorption tower";
			return;
		}
		double perNh3 = AmmoniaPlant.hno3PerAmmonia();
		double kg = Math.min(AmmoniaPlant.nitricAmmoniaKgh() / 3600 * seconds(), ammoniaKg);
		kg = Math.min(kg, room(Product.NITRIC_ACID) * Tuning.NITRIC_KG_PER_L * Tuning.NITRIC_STRENGTH / perNh3);
		kg = Math.min(kg, water / AmmoniaPlant.processWaterPerHno3() / perNh3);
		if (kg <= 1e-9) {
			unit = "idle: FULL, its acid has nowhere to go (pipe its south side to a stock tank)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "NO AIR: its air blower is not running";
			return;
		}
		kg = Lines.pull(level, inlets(), Product.AMMONIA, kg * strength / Tuning.AMMONIA_KG_PER_L, Pull.MACHINE, this) * Tuning.AMMONIA_KG_PER_L;
		double hno3 = kg * perNh3;
		water = Math.max(0, water - hno3 * AmmoniaPlant.processWaterPerHno3());
		add(Product.NITRIC_ACID, hno3 / Tuning.NITRIC_STRENGTH / Tuning.NITRIC_KG_PER_L);
		lost += hno3 / 1000 * Tuning.GAUZE_LOSS_G_PER_T;
		making = hno3 / seconds() * 3600;
		unit = String.format("burning %.2f kg/h of ammonia on the gauze at 850-900 C: %.1f kg/h of %.0f %% acid (%.1f L/h)", kg / seconds() * 3600,
				making / Tuning.NITRIC_STRENGTH, Tuning.NITRIC_STRENGTH * 100, making / Tuning.NITRIC_STRENGTH / Tuning.NITRIC_KG_PER_L);
	}

	@Override
	public Component inspect() {
		String pack = gauze > 0 ? String.format("gauze %.4f g of %.0f g (changed at %.0f g lost)", gauze(), gauze, gauze * Tuning.GAUZE_WORN_SHARE) : "no gauze";
		return Component.literal(String.format("Nitric acid unit (air blower %.0f kW, %s): ", Tuning.NITRIC_UNIT_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; %s, %d spare; water %.0f/%.0f L; acid held %.1f L; ", pack, fed(), water, Tuning.AMMONIA_PLANT_WATER_HOLD, held(Product.NITRIC_ACID))
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
		return RatePoints.ma(making, AmmoniaPlant.hno3Kgh());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("gauze", gauze);
		output.putDouble("lost", lost);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		gauze = input.getDoubleOr("gauze", 0);
		lost = input.getDoubleOr("lost", 0);
		water = input.getDoubleOr("water", 0);
	}
}
