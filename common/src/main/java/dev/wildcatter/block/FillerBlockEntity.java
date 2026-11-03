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
	private final int ins;
	private final NonNullList<ItemStack> slots;

	protected FillerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		this(type, pos, state, 1);
	}

	/** With {@code ins} input slots from the hopper on top (the rodding shop's four), then its outlet slots. */
	protected FillerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int ins) {
		super(type, pos, state);
		this.ins = ins;
		this.slots = NonNullList.withSize(ins + OUT, ItemStack.EMPTY);
	}

	/** True for what input slot {@code slot} takes; with one input, what {@link #takes} takes. */
	protected boolean takesIn(int slot, ItemStack stack) {
		return slot == 0 && takes(stack);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	public int outCount() {
		int n = 0;
		for (int i = ins; i < ins + OUT; i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	public void giveAll(Player player) {
		for (int i = ins; i < ins + OUT; i++) {
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
		for (int i = ins; i < ins + OUT; i++) {
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
		for (int i = ins; i < ins + OUT; i++) {
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
		int from = side == Direction.UP ? 0 : side == Direction.DOWN ? ins : 0;
		int n = side == Direction.UP ? ins : side == Direction.DOWN ? OUT : 0;
		int[] out = new int[n];
		for (int i = 0; i < n; i++) {
			out[i] = from + i;
		}
		return out;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= ins && side == Direction.DOWN;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < ins && takesIn(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot < ins && side == Direction.UP && takesIn(slot, stack);
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
