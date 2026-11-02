package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Fuel;
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
 * The rod welder (docs/multiblocks/fuel.md): a pressurized weld chamber (30 kW, 480 V) against the rod loader. It fills each loaded
 * rod with helium at 2 MPa (0.4 L at STP) and TIG-welds its top plug and fill hole, then racks it, up to 264, for the assembly
 * fixture it touches. Helium cylinders come in by a hopper on top; the empties go out to a hopper under it.
 */
public class RodWelderBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
	private double helium;     // m3 left in the cylinder on the manifold
	private boolean cylinder;
	private double progress;   // of the rod in the chamber
	private int rack;
	private int welded;
	private String unit = "starting";

	public RodWelderBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ROD_WELDER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.HELIUM_CYLINDER);
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

	public int rack() {
		return rack;
	}

	public boolean takeRod() {
		if (rack <= 0) {
			return false;
		}
		rack--;
		setChanged();
		return true;
	}

	@Override
	protected void work(Level level) {
		FuelLineBlockEntity loader = Touching.find(level, worldPosition, FuelLineBlockEntity.class, f -> f.kind().equals("rod_loader"));
		if (loader == null) {
			unit = "NOT BUILT: set it against the ROD LOADER";
			return;
		}
		double perRod = Tuning.ROD_HELIUM_L / 1000;
		if (cylinder && helium < perRod) {
			int out = slots.get(1).isEmpty() ? 1 : slots.get(2).isEmpty() ? 2 : -1;
			if (out >= 0) {
				slots.set(out, new ItemStack(WildcatterItems.EMPTY_HELIUM_CYLINDER));
				cylinder = false;
			}
		}
		if (!cylinder && fed() > 0) {
			useOne();
			cylinder = true;
			helium = Tuning.HELIUM_CYLINDER_M3;
		}
		if (!cylinder) {
			unit = "NO HELIUM: put a hopper of helium cylinders on top";
			return;
		}
		if (rack >= Tuning.ASSEMBLY_RODS) {
			unit = "FULL: the rack holds 264 rods; set the ASSEMBLY FIXTURE against it";
			return;
		}
		if (progress <= 1e-9 && loader.rods() <= 0) {
			unit = "waiting for loaded rods from the rod loader";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		if (progress <= 1e-9) {
			loader.takeRod();
			progress = 1e-6;
		}
		progress += Tuning.FUEL_LINE_U_KGS * 1.2 * seconds() * strength / Fuel.rodU();
		if (progress >= 1) {
			progress = 0;
			helium -= perRod;
			rack++;
			welded++;
		}
		unit = String.format("filling with helium at 2 MPa and welding: rod %.0f %% done; %d in the rack; helium %.2f m3 left", Math.min(progress, 1) * 100, rack, helium);
	}

	/** For the game test: the rack filled to {@code n} rods. */
	public void setRackForTest(int n) {
		rack = n;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Rod welder: %s; %d rods welded; %s", unit, welded, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("helium", helium);
		output.putBoolean("cylinder", cylinder);
		output.putDouble("progress", progress);
		output.putInt("rack", rack);
		output.putInt("welded", welded);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		helium = input.getDoubleOr("helium", 0);
		cylinder = input.getBooleanOr("cylinder", false);
		progress = input.getDoubleOr("progress", 0);
		rack = input.getIntOr("rack", 0);
		welded = input.getIntOr("welded", 0);
	}
}
