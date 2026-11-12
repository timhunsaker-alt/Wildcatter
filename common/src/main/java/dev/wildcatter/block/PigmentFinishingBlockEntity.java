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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The pigment finishing (docs/multiblocks/titanium-pigment.md): wet milling, surface treatment with alumina (the Bayer refinery's hydrate,
 * bags by a hopper on top, 30 kg a tonne), filtration, drying and a steam micronizer on process steam (2 t a tonne, on the east face of its
 * front cells). 75 kW at the world's industrial voltage, its box on the west face of its main cell. TiO2 pigment, rutile grade, to a
 * bagging station against it in 1 t bags. The chlorinator runs it.
 */
public class PigmentFinishingBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	double hydrate;   // kg of hydrate opened
	double raw;       // kg of raw TiO2 from the oxidiser
	double pigment;   // kg ready for bags
	String unit = "idle: no TiO2 from the oxidiser";

	public PigmentFinishingBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PIGMENT_FINISHING_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.HYDRATE_BAG);
	}

	List<Lines.Face> steamIn() {
		return faces(at -> at[0] == 3 && at[2] <= 1 && at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** kg of pigment it can still take: two bags. */
	double room() {
		return Math.max(0, 2 * Tuning.BULK_BAG_KG - pigment - raw);
	}

	/** For the game test: pigment as if finished. */
	public void testAddPigment(double kg) {
		pigment += kg;
		setChanged();
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.TIO2_PIGMENT_BAG;
	}

	@Override
	public double bagReady() {
		return pigment;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, pigment);
		pigment -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Pigment finishing (%.0f kW, %s): ", Tuning.PIGMENT_FINISHING_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; %,.0f kg of pigment ready for bags; hydrate %.0f kg and %d bags; ", pigment, hydrate, fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("hydrate", hydrate);
		output.putDouble("pigment", pigment);
		output.putDouble("raw", raw);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		hydrate = input.getDoubleOr("hydrate", 0);
		pigment = input.getDoubleOr("pigment", 0);
		raw = input.getDoubleOr("raw", 0);
	}
}
