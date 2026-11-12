package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Lithium;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The lithium carbonator (docs/multiblocks/lithium.md): lithium hydroxide dissolved and carbonated with CO2 to lithium carbonate, up to
 * 100 kg an hour (proposed). Lithium hydroxide bags by a hopper on top, liquid CO2 on the east face of its main cell; Li2CO3 to a bagging
 * station against it in 1 t bags. 10 kW at the world's industrial voltage, the box on the west face of its main cell.
 */
public class LithiumCarbonatorBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double lioh;      // kg of LiOH.H2O dissolved
	private double li2co3;    // kg ready for bags
	private double making;    // kg/h, the last interval
	private String unit = "starting";

	public LithiumCarbonatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LITHIUM_CARBONATOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.LIOH_BAG);
	}

	private List<Lines.Face> co2In() {
		return faces(at -> at[0] == 1 && at[2] == 0 && at[1] == 0, Direction.EAST);
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

	/** For the game test: carbonate as if made. */
	public void testAdd(double kg) {
		li2co3 += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double max = Tuning.LITHIUM_CARBONATOR_KGH / 3600 * seconds();
		if (lioh < max * Lithium.liohPerLi2co3() && fed() > 0) {
			useOne();
			lioh += Tuning.BULK_BAG_KG;
		}
		if (lioh <= 1e-6) {
			unit = "NO LITHIUM HYDROXIDE: its bags in a hopper on top";
			return;
		}
		double co2 = Lines.available(level, co2In(), Product.CO2, Pull.MACHINE, this);
		if (co2 <= 1e-6) {
			unit = "NO CO2: pipe liquid CO2 to the east face of its main cell";
			return;
		}
		double kg = Math.min(max, Math.min(lioh / Lithium.liohPerLi2co3(), co2 / Lithium.co2PerLi2co3()));
		kg = Math.min(kg, 2 * Tuning.BULK_BAG_KG - li2co3);
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
		Lines.pull(level, co2In(), Product.CO2, kg * Lithium.co2PerLi2co3(), Pull.MACHINE, this);
		lioh -= kg * Lithium.liohPerLi2co3();
		li2co3 += kg;
		making = kg / seconds() * 3600;
		unit = String.format("carbonating: %.0f kg/h of Li2CO3 on %.1f kg/h of CO2", making, kg * Lithium.co2PerLi2co3() / seconds() * 3600);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.LI2CO3_BAG;
	}

	@Override
	public double bagReady() {
		return li2co3;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, li2co3);
		li2co3 -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Lithium carbonator (%.0f kW, %s): ", Tuning.LITHIUM_CARBONATOR_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; LiOH.H2O %.0f kg and %d bags; %,.0f kg of Li2CO3 ready for bags; ", lioh, fed(), li2co3) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("lioh", lioh);
		output.putDouble("li2co3", li2co3);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		lioh = input.getDoubleOr("lioh", 0);
		li2co3 = input.getDoubleOr("li2co3", 0);
	}
}
