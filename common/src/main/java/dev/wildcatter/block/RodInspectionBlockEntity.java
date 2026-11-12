package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
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
 * The rod inspection line (docs/multiblocks/fuel-fabrication-extensions.md, 19c1): weld X-ray, a vacuum chamber with a helium
 * mass-spectrometer leak detector and a passive gamma scanner, between the rod welder and the assembly fixture, touching both; 25 kW at LV
 * (box west). It takes the welder's rods, rejects 0.5 % (one rod in two hundred), cut up with their pellets to scrap cans for a hopper
 * under it, and racks the rest for the fixture.
 */
public class RodInspectionBlockEntity extends UnitMotorBlockEntity implements RodSource, HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private final ScrapCans scrap = new ScrapCans();
	private int rack;
	private double assay = Tuning.ASSAY_PRODUCT;
	private double progress;    // of the rod in the chamber
	private double rejectDue;   // rods' worth of rejects owed
	private int inspected;
	private int rejected;
	private int ifbaRack;      // 19c2
	private boolean lastIfba;
	private String unit = "starting";

	public RodInspectionBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ROD_INSPECTION_ENTITY, pos, state);
	}

	@Override
	public int rack() {
		return rack;
	}

	@Override
	public boolean takeRod() {
		if (rack <= 0) {
			return false;
		}
		lastIfba = ifbaRack > 0;
		if (lastIfba) {
			ifbaRack--;
		}
		rack--;
		setChanged();
		return true;
	}

	@Override
	public double rodAssay() {
		return assay;
	}

	@Override
	public boolean lastTakenIfba() {
		return lastIfba;
	}

	public int inspected() {
		return inspected;
	}

	public int rejected() {
		return rejected;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	/** One rod through the X-ray, the leak test and the scanner: racked, or cut up to scrap. */
	private void inspectOne(RodWelderBlockEntity welder) {
		double a = welder.rodAssay();
		welder.takeRod();
		boolean coated = welder.lastTakenIfba();
		inspected++;
		rejectDue += Tuning.ROD_REJECT;
		if (rejectDue >= 1) {
			rejectDue -= 1;
			rejected++;
			scrap.add(Fuel.rodU(), a);
		} else {
			assay = FuelStage.blend(rack, assay, 1, a);
			rack++;
			if (coated) {
				ifbaRack++;
			}
		}
	}

	/** For the game test: {@code n} of the welder's rods inspected at once. */
	public void testInspect(int n) {
		if (level == null) {
			return;
		}
		RodWelderBlockEntity welder = Touching.find(level, worldPosition, RodWelderBlockEntity.class, w -> true);
		for (int i = 0; i < n && welder != null && welder.rack() > 0; i++) {
			inspectOne(welder);
		}
		scrap.emit(slots, 0);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		scrap.emit(slots, 0);
		RodWelderBlockEntity welder = Touching.find(level, worldPosition, RodWelderBlockEntity.class, w -> true);
		if (welder == null) {
			unit = "NOT BUILT: set it between the ROD WELDER and the assembly fixture, touching both";
			return;
		}
		if (rack >= Tuning.ASSEMBLY_RODS) {
			unit = "FULL: its rack holds 264 rods; set the ASSEMBLY FIXTURE against it";
			return;
		}
		if (progress <= 1e-9 && welder.rack() <= 0) {
			unit = "waiting for welded rods from the rod welder";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		progress += Tuning.FUEL_LINE_U_KGS * 1.2 * seconds() * strength / Fuel.rodU();
		if (progress >= 1 && welder.rack() > 0) {
			progress = 0;
			inspectOne(welder);
		}
		unit = String.format("X-ray, helium leak test and gamma scan: rod %.0f %% done; %d in the rack", Math.min(progress, 1) * 100, rack);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Rod inspection (X-ray, helium leak test, gamma scan; %.0f kW, %s): %s; %d inspected, %d rejected to scrap; %d scrap cans; ",
				Tuning.ROD_INSPECTION_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, inspected, rejected, slots.get(0).getCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		scrap.save(output, "scrap");
		output.putInt("rack", rack);
		output.putDouble("assay", assay);
		output.putDouble("progress", progress);
		output.putDouble("rejectDue", rejectDue);
		output.putInt("inspected", inspected);
		output.putInt("rejected", rejected);
		output.putInt("ifbaRack", ifbaRack);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		scrap.load(input, "scrap");
		rack = input.getIntOr("rack", 0);
		assay = input.getDoubleOr("assay", Tuning.ASSAY_PRODUCT);
		progress = input.getDoubleOr("progress", 0);
		rejectDue = input.getDoubleOr("rejectDue", 0);
		inspected = input.getIntOr("inspected", 0);
		rejected = input.getIntOr("rejected", 0);
		ifbaRack = input.getIntOr("ifbaRack", 0);
	}
}
