package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
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
 * The sulfur vat (docs/multiblocks/oilfield-follow-ons.md, 23d): a 6 x 6 pour vat where molten sulfur off steel pipe (any side of its
 * bottom cells) sets solid and is broken out as sulfur, a block's worth at a time, for a hopper under it (the item the acid plant takes).
 */
public class SulfurVatBlockEntity extends ProcessPartBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double setting;   // kg of sulfur setting toward the next block
	private double poured;

	public SulfurVatBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SULFUR_VAT_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	public double poured() {
		return poured;
	}

	public int broken() {
		return slots.get(0).getCount();
	}

	private List<Lines.Face> inlet() {
		java.util.ArrayList<Lines.Face> out = new java.util.ArrayList<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			out.addAll(faces(at -> at[1] == 0, d));
		}
		return out;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	protected void work(Level level) {
		ItemStack out = slots.get(0);
		if (out.getCount() >= out.getMaxStackSize()) {
			status = "FULL: set a hopper under it";
			return;
		}
		double litres = Lines.pull(level, inlet(), Product.MOLTEN_SULFUR, 1000, Pull.MACHINE, this);
		double kg = litres * Tuning.MOLTEN_SULFUR_KG_PER_L;
		setting += kg;
		poured += kg;
		while (setting >= Ore.SULFUR.kgPerBlock()) {
			setting -= Ore.SULFUR.kgPerBlock();
			if (out.isEmpty()) {
				slots.set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.SULFUR)));
			} else {
				out.grow(1);
			}
		}
		status = kg > 0 ? String.format("pouring %.0f kg/h of molten sulfur, %.0f kg setting", kg / seconds() * 3600, setting) : "idle: no molten sulfur on its pipe";
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Sulfur vat (6 x 6 pour vat): %s; %.1f t poured, %d blocks of sulfur broken out", status, poured / 1000, broken()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("setting", setting);
		output.putDouble("poured", poured);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		setting = input.getDoubleOr("setting", 0);
		poured = input.getDoubleOr("poured", 0);
	}
}
