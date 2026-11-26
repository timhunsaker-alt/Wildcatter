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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The graphite furnace (docs/multiblocks/grid-batteries.md, 22a): an electric thermal purification furnace at about 2,800 C, spherical
 * graphite bags by a hopper on top to 99.95 % C, 360 kg/h (5 % driven off), under a nitrogen purge off a pipe to the east face of its main
 * cell. 1.4 MW through its furnace transformer on the world's distribution voltage, the box on the west face of its main cell. Purified graphite to a bagging station
 * against it.
 */
public class GraphiteFurnaceBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double feed;       // kg of spherical graphite in the furnace
	private double purified;   // kg ready for bags
	private double making;
	private String unit = "starting";

	public GraphiteFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GRAPHITE_FURNACE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.SPHERICAL_GRAPHITE_BAG);
	}

	private List<Lines.Face> nitrogenIn() {
		return faces(at -> at[0] == 3 && at[1] == 0 && at[2] == 0, Direction.EAST);
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
		purified += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double max = Tuning.GRAPHITE_FURNACE_KGH * dev.wildcatter.TimeScale.production() / 3600 * seconds();
		if (feed < max / (1 - Tuning.GRAPHITE_PURIFY_LOSS) && fed() > 0) {
			useOne();
			feed += Tuning.BULK_BAG_KG;
		}
		if (feed <= 1e-6) {
			unit = "NO GRAPHITE: spherical graphite bags in a hopper on top";
			return;
		}
		double n2 = Lines.available(level, nitrogenIn(), Product.NITROGEN, Pull.MACHINE, this);
		if (n2 <= 1e-9) {
			unit = "NO NITROGEN: pipe nitrogen to the east face of its main cell";
			return;
		}
		double kg = Math.min(max, Math.min(feed * (1 - Tuning.GRAPHITE_PURIFY_LOSS), n2 / Tuning.GRAPHITE_FURNACE_N2_PER_KG));
		kg = Math.min(kg, 2 * Tuning.BULK_BAG_KG - purified);
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
		Lines.pull(level, nitrogenIn(), Product.NITROGEN, kg * Tuning.GRAPHITE_FURNACE_N2_PER_KG, Pull.MACHINE, this);
		feed -= kg / (1 - Tuning.GRAPHITE_PURIFY_LOSS);
		purified += kg;
		making = kg / seconds() * 3600;
		unit = String.format("purifying at 2,800 C: %.0f kg/h of 99.95 %% C graphite", making);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.PURIFIED_GRAPHITE_BAG;
	}

	@Override
	public double bagReady() {
		return purified;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, purified);
		purified -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Graphite furnace (%.1f MW, %s): ", Tuning.GRAPHITE_FURNACE_WATTS / 1e6, Voltage.DISTRIBUTION.label()) + unit
				+ String.format("; %.0f kg in the furnace and %d bags; %,.0f kg ready for bags; ", feed, fed(), purified) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("feed", feed);
		output.putDouble("purified", purified);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		purified = input.getDoubleOr("purified", 0);
	}
}
