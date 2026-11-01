package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A cascade take-off (docs/multiblocks/enrichment.md): chilled desublimers that freeze the cascade's product into a 30B (the product
 * take-off, 15 kW) or its tails into a 48Y (the tails take-off, 30 kW), under vacuum, on 480 V. Empty cylinders come in by the hopper
 * on top; full ones go out to a hopper under it, or to an empty hand. The feed station hands it its UF6.
 */
public class TakeoffBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 2;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private boolean loaded;   // a cylinder is on its scale
	private double uf6;       // kg in it
	private double ready;     // how hard its refrigeration is running, 0 to 1
	private String unit = "starting";

	public TakeoffBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TAKEOFF_ENTITY, pos, state);
	}

	public boolean product() {
		return kind().equals("product_takeoff");
	}

	private double capacity() {
		return product() ? Tuning.CYLINDER_30B_KG : Tuning.CYLINDER_48Y_KG;
	}

	private Item empty() {
		return product() ? WildcatterItems.EMPTY_30B_CYLINDER : WildcatterItems.EMPTY_48Y_CYLINDER;
	}

	private Item full() {
		return product() ? WildcatterItems.LEU_CYLINDER_30B : WildcatterItems.TAILS_CYLINDER_48Y;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(empty());
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot > 0 && side == Direction.DOWN;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side == Direction.UP && takes(stack);
	}

	/** kg of UF6 it can take now: room in its cylinder while its refrigeration runs. */
	public double room() {
		return loaded && ready > 0 ? Math.max(0, capacity() - uf6) : 0;
	}

	/** The feed station hands it UF6; for the game test, a top-up. */
	public void addUf6(double kg) {
		if (!loaded && fed() > 0) {
			useOne();
			loaded = true;
		}
		uf6 = Math.min(capacity(), uf6 + kg);
		setChanged();
	}

	public double uf6() {
		return uf6;
	}

	public int fullCount() {
		int n = 0;
		for (int i = 1; i <= OUT; i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	public void giveAll(Player player) {
		for (int i = 1; i <= OUT; i++) {
			ItemStack stack = slots.get(i);
			if (!stack.isEmpty()) {
				if (!player.getInventory().add(stack.copy())) {
					net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
				}
				slots.set(i, ItemStack.EMPTY);
			}
		}
		setChanged();
	}

	private int freeOut() {
		for (int i = 1; i <= OUT; i++) {
			if (slots.get(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void work(Level level) {
		ready = 0;
		if (loaded && uf6 >= capacity() - 1e-6) {
			int out = freeOut();
			if (out < 0) {
				unit = "FULL: take the cylinders (empty hand, or a hopper under it)";
				return;
			}
			slots.set(out, new ItemStack(full()));
			loaded = false;
			uf6 = 0;
		}
		if (!loaded && fed() > 0) {
			useOne();   // the next empty in the bin goes on the scale
			loaded = true;
		}
		if (!loaded) {
			unit = "NO CYLINDER: put a hopper of empty " + (product() ? "30B" : "48Y") + " cylinders on top";
			return;
		}
		ready = drive(level);
		String what = product() ? String.format("30B of %.2f %%", Tuning.ASSAY_PRODUCT * 100) : String.format("48Y of %.2f %% tails", Tuning.ASSAY_TAILS * 100);
		unit = ready <= 0 ? "starting: " + describe()
				: String.format("%s: %,.0f/%,.0f kg of UF6 (%.1f %%)", what, uf6, capacity(), uf6 / capacity() * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal((product() ? "Product take-off: " : "Tails take-off: ") + unit + String.format("; %d full, %d empty waiting; ", fullCount(), fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putBoolean("loaded", loaded);
		output.putDouble("uf6", uf6);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		loaded = input.getBooleanOr("loaded", false);
		uf6 = input.getDoubleOr("uf6", 0);
	}
}
