package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
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
 * The pellet inspection bench (docs/multiblocks/fuel-fabrication-extensions.md, 19c1): an automated laser-gauge and camera bench between
 * the pellet grinder and the rod loader, touching both; 10 kW at LV (box west). It passes the grinder's pellets on to the loader and
 * rejects 1 % (size and chips) to scrap cans for a hopper under it.
 */
public class PelletInspectionBlockEntity extends UnitMotorBlockEntity implements FuelStage, HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private final ScrapCans scrap = new ScrapCans();
	private double u;
	private double assay = Tuning.ASSAY_PRODUCT;
	private double passed;      // kg of U inspected, all told
	private String unit = "starting";

	public PelletInspectionBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PELLET_INSPECTION_ENTITY, pos, state);
	}

	@Override
	public double fuelU() {
		return u;
	}

	@Override
	public double fuelAssay() {
		return assay;
	}

	@Override
	public double takeFuelU(double kg) {
		double got = Math.max(0, Math.min(kg, u));
		u -= got;
		setChanged();
		return got;
	}

	@Override
	public void addFuelU(double kg) {
		u = Math.min(Tuning.FUEL_STAGE_HOLD_KG, u + kg);
		setChanged();
	}

	public double passed() {
		return passed;
	}

	public double rejected() {
		return scrap.total();
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

	@Override
	protected void work(Level level) {
		FuelLineBlockEntity grinder = Touching.find(level, worldPosition, FuelLineBlockEntity.class, f -> f.kind().equals("pellet_grinder"));
		scrap.emit(slots, 0);
		if (grinder == null) {
			unit = "NOT BUILT: set it between the PELLET GRINDER and the rod loader, touching both";
			return;
		}
		if (grinder.fuelU() < 1e-9) {
			unit = "waiting for pellets from the pellet grinder";
			return;
		}
		double room = Tuning.FUEL_STAGE_HOLD_KG - u;
		if (room <= 1e-9) {
			unit = "FULL: the rod loader is not taking its pellets";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double a = grinder.fuelAssay();
		double kg = grinder.takeFuelU(Math.min(Tuning.FUEL_LINE_U_KGS * 1.2 * seconds() * strength, room));
		double reject = kg * Tuning.PELLET_REJECT;
		scrap.add(reject, a);
		assay = FuelStage.blend(u, assay, kg - reject, a);
		u += kg - reject;
		passed += kg;
		scrap.emit(slots, 0);
		unit = String.format("gauging and imaging pellets: %.1f kg of U an hour, %.0f %% rejected to scrap", kg / seconds() * 3600, Tuning.PELLET_REJECT * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Pellet inspection (laser gauge and cameras, %.0f kW, %s): %s; %.2f kg of U held; %.1f kg inspected, %.2f kg rejected; %d scrap cans; ",
				Tuning.PELLET_INSPECTION_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, u, passed, scrap.total(), slots.get(0).getCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		scrap.save(output, "scrap");
		output.putDouble("u", u);
		output.putDouble("assay", assay);
		output.putDouble("passed", passed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		scrap.load(input, "scrap");
		u = input.getDoubleOr("u", 0);
		assay = input.getDoubleOr("assay", Tuning.ASSAY_PRODUCT);
		passed = input.getDoubleOr("passed", 0);
	}
}
