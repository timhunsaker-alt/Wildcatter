package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
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
import org.jspecify.annotations.Nullable;

/**
 * The LFP kiln (docs/multiblocks/grid-batteries.md, 22a): iron phosphate precipitated from iron and phosphoric acid, mixed with lithium
 * carbonate and a carbon source and calcined at 700 C under nitrogen in an electric roller-hearth kiln: LiFePO4/C cathode powder, 700 kg/h
 * (proposed), to a bagging station against it. Iron ingots and lithium carbonate bags by a hopper on top; merchant phosphoric acid (54 %
 * P2O5) to the east face of its main cell and nitrogen to the east face of its back cell. 400 kW at the world's industrial voltage, the box on
 * the west face of its main cell.
 */
public class LfpKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private static final int IRON = 0, LI2CO3 = 1;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double iron;      // kg
	private double li2co3;    // kg
	private double cathode;   // kg ready for bags
	private double making;
	private String unit = "starting";

	public LfpKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LFP_KILN_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static Item itemOf(int slot) {
		return slot == IRON ? Items.IRON_INGOT : WildcatterItems.LI2CO3_BAG;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(Items.IRON_INGOT) || stack.is(WildcatterItems.LI2CO3_BAG);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {IRON, LI2CO3} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return (slot == IRON || slot == LI2CO3) && stack.is(itemOf(slot));
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	private List<Lines.Face> acidIn() {
		return faces(at -> at[0] == 2 && at[1] == 0 && at[2] == 0, Direction.EAST);
	}

	private List<Lines.Face> nitrogenIn() {
		return faces(at -> at[0] == 2 && at[1] == 0 && at[2] == 11, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double making() {
		return making;
	}

	public void testAdd(double kg) {
		cathode += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double max = Tuning.LFP_KILN_KGH * dev.wildcatter.TimeScale.production() / 3600 * seconds();
		if (iron < max * Tuning.LFP_IRON_PER_KG && !slots.get(IRON).isEmpty()) {
			slots.get(IRON).shrink(1);
			iron += Tuning.IRON_INGOT_KG;
			setChanged();
		}
		if (li2co3 < max * Tuning.LFP_LI2CO3_PER_KG && !slots.get(LI2CO3).isEmpty()) {
			slots.get(LI2CO3).shrink(1);
			li2co3 += Tuning.BULK_BAG_KG;
			setChanged();
		}
		if (iron <= 1e-6) {
			unit = "NO IRON: iron ingots in a hopper on top";
			return;
		}
		if (li2co3 <= 1e-6) {
			unit = "NO LITHIUM CARBONATE: its bags in a hopper on top";
			return;
		}
		double acidL = Lines.available(level, acidIn(), Product.MERCHANT_ACID, Pull.MACHINE, this);
		double acidKg = acidL * Tuning.MERCHANT_ACID_KG_PER_L * Tuning.MERCHANT_ACID_H3PO4;
		if (acidKg <= 1e-6) {
			unit = "NO PHOSPHORIC ACID: pipe merchant acid to the east face of its main cell";
			return;
		}
		double n2 = Lines.available(level, nitrogenIn(), Product.NITROGEN, Pull.MACHINE, this);
		if (n2 <= 1e-9) {
			unit = "NO NITROGEN: pipe nitrogen to the east face of its back cell";
			return;
		}
		double kg = Math.min(max, Math.min(iron / Tuning.LFP_IRON_PER_KG, li2co3 / Tuning.LFP_LI2CO3_PER_KG));
		kg = Math.min(kg, Math.min(acidKg / Tuning.LFP_ACID_PER_KG, n2 / Tuning.LFP_KILN_N2_PER_KG));
		kg = Math.min(kg, 2 * Tuning.BULK_BAG_KG - cathode);
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
		Lines.pull(level, acidIn(), Product.MERCHANT_ACID, kg * Tuning.LFP_ACID_PER_KG / (Tuning.MERCHANT_ACID_KG_PER_L * Tuning.MERCHANT_ACID_H3PO4), Pull.MACHINE, this);
		Lines.pull(level, nitrogenIn(), Product.NITROGEN, kg * Tuning.LFP_KILN_N2_PER_KG, Pull.MACHINE, this);
		iron -= kg * Tuning.LFP_IRON_PER_KG;
		li2co3 -= kg * Tuning.LFP_LI2CO3_PER_KG;
		cathode += kg;
		making = kg / seconds() * 3600;
		unit = String.format("calcining at 700 C: %.0f kg/h of LiFePO4/C", making);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.LFP_CATHODE_BAG;
	}

	@Override
	public double bagReady() {
		return cathode;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, cathode);
		cathode -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("LFP kiln (%.0f kW, %s): ", Tuning.LFP_KILN_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; iron %.0f kg (%d ingots), Li2CO3 %.0f kg (%d bags); %,.0f kg of cathode ready for bags; ", iron, slots.get(IRON).getCount(), li2co3,
						slots.get(LI2CO3).getCount(), cathode) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("iron", iron);
		output.putDouble("li2co3", li2co3);
		output.putDouble("cathode", cathode);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		iron = input.getDoubleOr("iron", 0);
		li2co3 = input.getDoubleOr("li2co3", 0);
		cathode = input.getDoubleOr("cathode", 0);
	}
}
