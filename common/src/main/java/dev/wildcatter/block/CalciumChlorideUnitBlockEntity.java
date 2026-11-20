package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The calcium chloride unit (docs/multiblocks/oilfield-follow-ons.md, 23a): a rubber-lined reactor where 32 % hydrochloric acid dissolves
 * calcite or limestone, CaCO3 + 2 HCl -> CaCl2 + H2O + CO2, and a steam evaporator and flaker taking the liquor to 77 % CaCl2 flake, 0.5
 * t/h. 15 kW at industrial LV for its agitator and pumps. Calcite or limestone by a hopper on top; the acid on the north face of its
 * front bottom cells, process steam on the east face of its bottom cells; the CO2 is vented and counted. A bagging station against it
 * fills 1 t bags of flake for oil mud's water phase.
 */
public class CalciumChlorideUnitBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private static final double MW_CACL2 = 110.98, MW_CACO3 = 100.09, MW_HCL = 36.46, MW_CO2 = 44.01, MW_H2O = 18.02;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double feed;     // kg of CaCO3 in its bin
	private double flake;    // kg of 77 % flake ready for bags
	private double made;
	private double co2;      // kg vented
	private String unit = "starting";

	public CalciumChlorideUnitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CALCIUM_CHLORIDE_UNIT_ENTITY, pos, state);
	}

	/** kg of each a kg of flake: {CaCO3, HCl (as 32 % acid), CO2, steam}. */
	public static double[] perKgFlake() {
		double mol = Tuning.CACL2_FLAKE / MW_CACL2;   // kmol of CaCl2 a kg of flake... in kg/kmol units, the ratios hold
		double caco3 = mol * MW_CACO3;
		double acid = mol * 2 * MW_HCL / Tuning.HCL_STRENGTH;
		double co2 = mol * MW_CO2;
		double water = acid * (1 - Tuning.HCL_STRENGTH) + mol * MW_H2O;   // the acid's water and the reaction's
		double evaporated = Math.max(0, water - (1 - Tuning.CACL2_FLAKE));
		return new double[] {caco3, acid, co2, evaporated * Tuning.CACL2_STEAM_PER_WATER};
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(Items.CALCITE) || stack.is(OreBlocks.MINERALS.get(Ore.LIMESTONE));
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.CACL2_BAG;
	}

	@Override
	public double bagReady() {
		return flake;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, flake);
		flake -= got;
		setChanged();
		return got;
	}

	public double made() {
		return made;
	}

	/** For the game test: flake as if made. */
	public void testFlake(double kg) {
		flake += kg;
		setChanged();
	}

	private List<Lines.Face> acidIn() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
	}

	private List<Lines.Face> steamIn() {
		return faces(at -> at[1] == 0 && at[0] == 1, Direction.EAST);
	}

	@Override
	protected void work(Level level) {
		double[] per = perKgFlake();
		double max = Tuning.CACL2_UNIT_KGS * seconds();
		if (feed < max * per[0] && fed() > 0) {
			useOne();
			feed += Ore.LIMESTONE.kgPerBlock();
		}
		if (feed < 0.01) {
			unit = "NO CALCITE: put a hopper of calcite or limestone on it";
			return;
		}
		max = Math.min(Math.min(max, feed / per[0]), 2 * Tuning.BULK_BAG_KG - flake);
		if (max <= 1e-7) {
			unit = "FULL: set a bagging station against it";
			return;
		}
		double acid = Lines.available(level, acidIn(), Product.HYDROCHLORIC_ACID, Pull.MACHINE, this);
		if (acid < 1e-3) {
			unit = "NO ACID: pipe 32 % hydrochloric acid to the north side of its front";
			return;
		}
		double steam = Lines.available(level, steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		if (steam < 1e-3) {
			unit = "NO STEAM: pipe process steam to its east side for the evaporator";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Math.min(max * strength, Math.min(acid / per[1], steam / per[3]));
		Lines.pull(level, acidIn(), Product.HYDROCHLORIC_ACID, kg * per[1], Pull.MACHINE, this);
		Lines.pull(level, steamIn(), Product.PROCESS_STEAM, kg * per[3], Pull.MACHINE, this);
		feed -= kg * per[0];
		flake += kg;
		made += kg;
		co2 += kg * per[2];
		unit = String.format("making %.2f t/h of 77 %% CaCl2 flake on %.2f t/h of acid; %.0f kg/h of CO2 vented", kg / seconds() * 3.6, kg * per[1] / seconds() * 3.6,
				kg * per[2] / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Calcium chloride unit (reactor and evaporator, %.1f t/h): %s; %,.0f kg of CaCO3 in its bin and %d blocks; %,.0f kg of flake ready for bags, %.1f t made, %.1f t of CO2 vented; ",
				Tuning.CACL2_UNIT_KGS * 3.6, unit, feed, fed(), flake, made / 1000, co2 / 1000) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("feed", feed);
		output.putDouble("flake", flake);
		output.putDouble("made", made);
		output.putDouble("co2", co2);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		flake = input.getDoubleOr("flake", 0);
		made = input.getDoubleOr("made", 0);
		co2 = input.getDoubleOr("co2", 0);
	}
}
