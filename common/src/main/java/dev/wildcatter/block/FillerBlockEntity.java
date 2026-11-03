package dev.wildcatter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A powered part that puts what it makes out as items (docs/multiblocks/alumina-plant.md, bayer-refinery.md): the bagging station,
 * the chlorine filler and the residue filter press. Slot 0 takes its empties from a hopper on top; the made items go out to a hopper
 * under it, or to an empty hand, as the mill's drum filler does.
 */
public abstract class FillerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	protected static final int OUT = 4;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);

	protected FillerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	public int outCount() {
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

	/** Puts one made item out; false if every outlet slot is full. */
	protected boolean putOut(ItemStack made) {
		for (int i = 1; i <= OUT; i++) {
			ItemStack in = slots.get(i);
			if (in.isEmpty()) {
				slots.set(i, made);
				setChanged();
				return true;
			}
			if (ItemStack.isSameItemSameComponents(in, made) && in.getCount() + made.getCount() <= in.getMaxStackSize()) {
				in.grow(made.getCount());
				setChanged();
				return true;
			}
		}
		return false;
	}

	/** True if one more of {@code made} would fit. */
	protected boolean roomOut(ItemStack made) {
		for (int i = 1; i <= OUT; i++) {
			ItemStack in = slots.get(i);
			if (in.isEmpty() || ItemStack.isSameItemSameComponents(in, made) && in.getCount() < in.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	// ---- empties in from the top, made items out of the bottom ---------------------------------------------------------------
	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2, 3, 4} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot > 0 && side == Direction.DOWN;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side == Direction.UP && takes(stack);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
	}
}
