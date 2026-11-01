package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The extruder and pelletizer (250 hp, 4.16 kV) against a polyethylene reactor (docs/multiblocks/cracker.md): it melts the powder
 * and makes plastic sheets, a sheet a kilogram, into its bin of 256. An empty hand, or a hopper under its main cell, takes them.
 */
public class PelletizerBlockEntity extends UnitMotorBlockEntity implements WorldlyContainer {
	private static final int SLOTS = Tuning.PELLETIZER_BIN / 64;
	private final NonNullList<ItemStack> bin = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private double polymer;

	public PelletizerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PELLETIZER_ENTITY, pos, state);
	}

	public int sheets() {
		return bin.stream().mapToInt(ItemStack::getCount).sum();
	}

	public boolean full() {
		return sheets() >= Tuning.PELLETIZER_BIN;
	}

	/** Polyethylene from the reactor, in kg: made into sheets as it comes. */
	public void addPolymer(double kg) {
		polymer += kg;
		while (polymer >= Tuning.PE_KG_PER_SHEET && !full()) {
			polymer -= Tuning.PE_KG_PER_SHEET;
			for (int i = 0; i < SLOTS; i++) {
				ItemStack stack = bin.get(i);
				if (stack.isEmpty()) {
					bin.set(i, new ItemStack(WildcatterItems.PLASTIC_SHEET));
					break;
				}
				if (stack.getCount() < stack.getMaxStackSize()) {
					stack.grow(1);
					break;
				}
			}
		}
		setChanged();
	}

	/** An empty hand takes every sheet. */
	public void giveAll(Player player) {
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = bin.get(i);
			if (!stack.isEmpty()) {
				if (!player.getInventory().add(stack.copy())) {
					net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
				}
				bin.set(i, ItemStack.EMPTY);
			}
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Pelletizer (extruder, 250 hp): " + sheets() + "/" + Tuning.PELLETIZER_BIN + " plastic sheets in its bin; " + describe());
	}

	// ---- its bin, for a hopper under its main cell ---------------------------------------------------------------------------
	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return sheets() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		return bin.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(bin, slot, count);
		setChanged();
		return out;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(bin, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		bin.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		bin.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		int[] all = new int[SLOTS];
		for (int i = 0; i < SLOTS; i++) {
			all[i] = i;
		}
		return all;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, bin);
		output.putDouble("polymer", polymer);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		bin.clear();
		ContainerHelper.loadAllItems(input, bin);
		polymer = input.getDoubleOr("polymer", 0);
	}
}
