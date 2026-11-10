package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.FuelAssemblyTags;
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
 * The new-fuel inspection stand (docs/multiblocks/reactor-extensions.md, 19b): lights and a camera on a 6 m stand, 5 kW at 480 V,
 * beside the refuelling machine's hopper. An assembly in by a hopper on top is looked over (grids and nozzles) and goes out INSPECTED to
 * a hopper under it; one with a bent grid is marked so, for the assembly fixture to straighten.
 */
public class InspectionStandBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 1;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double progress;
	private double seconds = Tuning.INSPECTION_SECONDS;
	private int inspected;
	private int bent;
	private String unit = "idle";

	public InspectionStandBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.INSPECTION_STAND_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterBlocks.FUEL_ASSEMBLY.asItem()) && !FuelAssemblyTags.inspected(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {OUT} : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side == Direction.UP && takes(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUT && side == Direction.DOWN;
	}

	/** For the game test: an inspection in {@code s} seconds. */
	public void setSecondsForTest(double s) {
		seconds = s;
	}

	@Override
	protected void work(Level level) {
		if (slots.get(0).isEmpty()) {
			unit = "idle: put new assemblies in by a hopper on top";
			return;
		}
		if (!slots.get(OUT).isEmpty()) {
			unit = "FULL: take the inspected assembly out (a hopper under it)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		progress += seconds() * strength;
		unit = String.format("inspecting an assembly: grids, nozzles and rods, %.0f %%", progress / seconds * 100);
		if (progress >= seconds) {
			progress = 0;
			ItemStack one = slots.get(0).split(1);
			FuelAssemblyTags.handle(one, level.getRandom());   // its move here by hopper
			FuelAssemblyTags.markInspected(one);
			inspected++;
			if (FuelAssemblyTags.bent(one)) {
				bent++;
			}
			unit = FuelAssemblyTags.bent(one) ? "INSPECTED: BENT GRID found; straighten it at the assembly fixture" : "INSPECTED: grids and nozzles good";
			slots.set(OUT, one);
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("New-fuel inspection stand: %s; %d inspected, %d with a bent grid; %s", unit, inspected, bent, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("progress", progress);
		output.putInt("inspected", inspected);
		output.putInt("bent", bent);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		progress = input.getDoubleOr("progress", 0);
		inspected = input.getIntOr("inspected", 0);
		bent = input.getIntOr("bent", 0);
	}
}
