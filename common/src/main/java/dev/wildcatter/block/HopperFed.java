package dev.wildcatter.block;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A process part fed solids by a hopper on its top (docs/multiblocks/uranium-mill.md, acid-plant.md): sulfur, ore, pyrolusite, salt,
 * magnesia, empty drums. Slot 0 holds one stack of what it takes; it uses a whole item at a time, an item being one block's worth.
 */
public interface HopperFed extends WorldlyContainer {
	NonNullList<ItemStack> slots();

	/** True for what slot 0 takes. */
	boolean takes(ItemStack stack);

	/** Takes one item out of slot 0; empty if there is none. */
	default ItemStack useOne() {
		ItemStack stack = slots().get(0);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack one = stack.split(1);
		setChanged();
		return one;
	}

	default int fed() {
		return slots().get(0).getCount();
	}

	@Override
	default int getContainerSize() {
		return slots().size();
	}

	@Override
	default boolean isEmpty() {
		return slots().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	default ItemStack getItem(int slot) {
		return slots().get(slot);
	}

	@Override
	default ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(slots(), slot, count);
		setChanged();
		return out;
	}

	@Override
	default ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(slots(), slot);
	}

	@Override
	default void setItem(int slot, ItemStack stack) {
		slots().set(slot, stack);
		setChanged();
	}

	@Override
	default boolean stillValid(Player player) {
		return false;
	}

	@Override
	default void clearContent() {
		slots().clear();
	}

	@Override
	default int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : new int[0];
	}

	@Override
	default boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && takes(stack);
	}

	@Override
	default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side == Direction.UP && takes(stack);
	}

	@Override
	default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}
}
