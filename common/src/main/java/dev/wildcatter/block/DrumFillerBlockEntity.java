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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The drum filling station (5 kW, 480 V) (docs/multiblocks/uranium-mill.md): the yellowcake dryer touching it fills it; each time it
 * holds 400 kg it fills an empty drum from the hopper on top. Full drums come out to a hopper under it, or to an empty hand.
 * 16a: a uranium calciner touching it fills it with calcined U3O8 (96 %), held apart and drummed as its own grade.
 */
public class DrumFillerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 4;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private double yellowcake;
	private double u3o8;   // kg of calcined U3O8 concentrate from a calciner (16a)
	private String unit = "starting";

	public DrumFillerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DRUM_FILLER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.EMPTY_DRUM);
	}

	public double room() {
		return Math.max(0, Tuning.DRUM_NET_KG * 2 - yellowcake);
	}

	/** kg of concentrate from the dryer (or the game test). */
	public void addYellowcake(double kg) {
		yellowcake += kg;
		setChanged();
	}

	public double yellowcake() {
		return yellowcake;
	}

	/** Room for calcined concentrate, held apart from the magnesia route's. */
	public double roomU3o8() {
		return Math.max(0, Tuning.DRUM_NET_KG * 2 - u3o8);
	}

	/** kg of 96 % U3O8 concentrate from the calciner (or the game test). */
	public void addU3o8(double kg) {
		u3o8 += kg;
		setChanged();
	}

	public double u3o8() {
		return u3o8;
	}

	public int drums() {
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
		boolean calcined = u3o8 >= Tuning.DRUM_NET_KG && yellowcake < Tuning.DRUM_NET_KG;
		if (yellowcake < Tuning.DRUM_NET_KG && !calcined) {
			unit = u3o8 > 0.5 ? String.format("filling: %.0f/%.0f kg of U3O8 (96 %%) in its hopper", u3o8, Tuning.DRUM_NET_KG)
					: String.format("filling: %.0f/%.0f kg in its hopper", yellowcake, Tuning.DRUM_NET_KG);
			return;
		}
		if (fed() == 0) {
			unit = "NO DRUMS: put a hopper of empty drums on top";
			return;
		}
		int out = freeOut();
		if (out < 0) {
			unit = "FULL: take the drums (empty hand, or a hopper under it)";
			return;
		}
		if (drive(level) <= 0) {
			unit = "starting: " + describe();
			return;
		}
		useOne();
		if (calcined) {
			u3o8 -= Tuning.DRUM_NET_KG;
			slots.set(out, new ItemStack(WildcatterItems.U3O8_DRUM));
			unit = "filled a drum of U3O8, 96 %";
			return;
		}
		yellowcake -= Tuning.DRUM_NET_KG;
		slots.set(out, new ItemStack(WildcatterItems.YELLOWCAKE_DRUM));
		unit = "filled a drum";
	}

	@Override
	public Component inspect() {
		String calcined = u3o8 > 0.5 ? String.format(", %.0f kg of U3O8 (96 %%)", u3o8) : "";
		return Component.literal(String.format("Drum filler: %s; %d full drums, %d empty; %.0f kg of yellowcake held%s; ", unit, drums(), fed(), yellowcake, calcined) + describe());
	}

	// ---- empty drums in from the top, full drums out of the bottom -------------------------------------------------------
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
		output.putDouble("yellowcake", yellowcake);
		output.putDouble("u3o8", u3o8);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		yellowcake = input.getDoubleOr("yellowcake", 0);
		u3o8 = input.getDoubleOr("u3o8", 0);
	}
}
