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
 * The assembly fixture (docs/multiblocks/fuel.md): 10 kW at 480 V, against the rod welder. It builds the skeleton (25 guide thimbles,
 * 6 Zircaloy spacer grids, and 20 kg of steel for the end grids and nozzles, all by a hopper on top), pulls the 264 rods in from the
 * welder's rack, and puts the finished 17x17 assembly out to a hopper under it, or to an empty hand.
 */
public class AssemblyFixtureBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 3;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(OUT + 1, ItemStack.EMPTY);
	private boolean skeleton;
	private double steel;
	private int rods;
	private int built;
	private String unit = "starting";

	public AssemblyFixtureBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ASSEMBLY_FIXTURE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takes(0, stack) || takes(1, stack) || takes(2, stack);
	}

	private boolean takes(int slot, ItemStack stack) {
		return slot == 0 && stack.is(WildcatterItems.GUIDE_THIMBLE) || slot == 1 && stack.is(WildcatterItems.SPACER_GRID) || slot == 2 && stack.is(WildcatterItems.STEEL_INGOT);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1, 2} : side == Direction.DOWN ? new int[] {OUT} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUT && side == Direction.DOWN;
	}

	public int finished() {
		return slots.get(OUT).getCount();
	}

	public void giveAll(Player player) {
		ItemStack stack = slots.get(OUT);
		if (!stack.isEmpty()) {
			if (!player.getInventory().add(stack.copy())) {
				net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
			}
			slots.set(OUT, ItemStack.EMPTY);
			setChanged();
		}
	}

	/** For the game test: the skeleton up and {@code n} rods in it. */
	public void setRodsForTest(int n) {
		skeleton = true;
		rods = n;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		RodWelderBlockEntity welder = Touching.find(level, worldPosition, RodWelderBlockEntity.class, w -> true);
		if (welder == null) {
			unit = "NOT BUILT: set it against the ROD WELDER";
			return;
		}
		if (!slots.get(OUT).isEmpty()) {
			unit = "FULL: take the finished assembly (empty hand, or a hopper under it)";
			return;
		}
		if (!skeleton) {
			if (steel < Tuning.ASSEMBLY_STEEL_KG && !slots.get(2).isEmpty()) {
				slots.get(2).shrink(1);
				steel += Tuning.STEEL_INGOT_KG;
			}
			String missing = slots.get(0).getCount() < Tuning.ASSEMBLY_THIMBLES ? String.format("NO THIMBLES: it needs %d guide thimbles, has %d", Tuning.ASSEMBLY_THIMBLES, slots.get(0).getCount())
					: slots.get(1).getCount() < Tuning.ASSEMBLY_MID_GRIDS ? String.format("NO GRIDS: it needs %d spacer grids, has %d", Tuning.ASSEMBLY_MID_GRIDS, slots.get(1).getCount())
					: steel < Tuning.ASSEMBLY_STEEL_KG ? "NO STEEL: put steel ingots in its hopper (nozzles and end grids)" : null;
			if (missing != null) {
				unit = missing;
				return;
			}
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		if (!skeleton) {   // the thimbles through the grids, the nozzles on
			slots.get(0).shrink(Tuning.ASSEMBLY_THIMBLES);
			slots.get(1).shrink(Tuning.ASSEMBLY_MID_GRIDS);
			steel -= Tuning.ASSEMBLY_STEEL_KG;
			skeleton = true;
		}
		if (rods < Tuning.ASSEMBLY_RODS && welder.takeRod()) {
			rods++;
		}
		if (rods >= Tuning.ASSEMBLY_RODS) {
			slots.set(OUT, new ItemStack(WildcatterBlocks.FUEL_ASSEMBLY));
			skeleton = false;
			rods = 0;
			built++;
			unit = "assembly finished: 264 rods in the skeleton, nozzles on";
			return;
		}
		unit = String.format("pulling rods into the skeleton: %d of %d", rods, Tuning.ASSEMBLY_RODS);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Assembly fixture: %s; %d built; %s", unit, built, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putBoolean("skeleton", skeleton);
		output.putDouble("steel", steel);
		output.putInt("rods", rods);
		output.putInt("built", built);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		skeleton = input.getBooleanOr("skeleton", false);
		steel = input.getDoubleOr("steel", 0);
		rods = input.getIntOr("rods", 0);
		built = input.getIntOr("built", 0);
	}
}
