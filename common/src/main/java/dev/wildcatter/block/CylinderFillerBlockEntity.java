package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Conversion;
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
 * The 48Y cylinder fill station (5 kW, 480 V) against the distillation columns (docs/multiblocks/conversion.md): an empty 48Y from
 * the hopper on top goes on its scale and fills with liquid UF6 to 12,501 kg; full ones go out to a hopper under it, or to an
 * empty hand.
 */
public class CylinderFillerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 2;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private double uf6;   // kg in the cylinder on the scale
	private String unit = "starting";

	public CylinderFillerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CYLINDER_FILLER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.EMPTY_48Y_CYLINDER);
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

	/** kg of UF6 in the cylinder on its scale; for the game test, a top-up. */
	public void addUf6(double kg) {
		uf6 = Math.min(Tuning.CYLINDER_48Y_KG, uf6 + kg);
		setChanged();
	}

	public int full() {
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
		if (uf6 >= Tuning.CYLINDER_48Y_KG - 1e-6) {
			int out = freeOut();
			if (out < 0) {
				unit = "FULL: take the cylinders (empty hand, or a hopper under it)";
				return;
			}
			slots.set(out, new ItemStack(WildcatterItems.UF6_CYLINDER_48Y));
			uf6 = 0;
			if (fed() > 0) {
				useOne();   // the cylinder just filled leaves the scale; the next empty in the bin goes on
			}
		}
		if (fed() == 0 && uf6 <= 0) {
			unit = "NO CYLINDER: put a hopper of empty 48Y cylinders on top";
			return;
		}
		UraniumStage columns = Touching.find(level, worldPosition, Uf6StageBlockEntity.class, s -> s.kind().equals("distillation_columns"));
		if (columns == null) {
			unit = "NOT BUILT: set it against the DISTILLATION COLUMNS";
			return;
		}
		if (columns.uranium() < 1e-6) {
			unit = String.format("waiting for UF6: %,.0f/%,.0f kg in the cylinder (%.1f %%)", uf6, Tuning.CYLINDER_48Y_KG, uf6 / Tuning.CYLINDER_48Y_KG * 100);
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double room = (Tuning.CYLINDER_48Y_KG - uf6) / Tuning.UF6_PER_U;
		double u = columns.takeUranium(Math.min(Conversion.uraniumKgs() * 1.2 * seconds(), room));
		uf6 += u * Tuning.UF6_PER_U;
		unit = String.format("filling: %,.0f/%,.0f kg of UF6 (%.1f %%)", uf6, Tuning.CYLINDER_48Y_KG, uf6 / Tuning.CYLINDER_48Y_KG * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal("48Y cylinder filler: " + unit + String.format("; %d full, %d empty waiting; ", full(), fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("uf6", uf6);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		uf6 = input.getDoubleOr("uf6", 0);
	}
}
