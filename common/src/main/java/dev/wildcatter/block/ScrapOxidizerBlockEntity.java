package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.WildcatterItems;
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

/**
 * The scrap oxidizer (docs/multiblocks/fuel-fabrication-extensions.md, 19c1): a tray furnace oxidising dry UO2 scrap in air at 400-450 C to
 * U3O8; 30 kW at LV, its box on the north face of its main cell (its west side stands against the granulator). Scrap cans by a hopper on top, one assay at a
 * time (a can of another assay waits until the trays are empty); about 5 kg of U an hour. The granulator it touches takes the U3O8 into
 * its powder, at most 10 % of its feed: it is the line's pore former.
 */
public class ScrapOxidizerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double scrapU;      // kg of U of scrap on the trays
	private double oxideU;      // kg of U as U3O8, for the granulator
	private double assay = Tuning.ASSAY_PRODUCT;
	private double given;       // kg of U given to the granulator, all told
	private String unit = "starting";

	public ScrapOxidizerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SCRAP_OXIDIZER_ENTITY, pos, state);
	}

	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.UO2_SCRAP_CAN);
	}

	public double oxideU() {
		return oxideU;
	}

	public double given() {
		return given;
	}

	public double assay() {
		return assay;
	}

	/** The granulator takes up to {@code kg} of U as U3O8; returns what it gave. */
	public double takeOxide(double kg) {
		double got = Math.max(0, Math.min(kg, oxideU));
		oxideU -= got;
		given += got;
		setChanged();
		return got;
	}

	@Override
	protected void work(Level level) {
		ItemStack can = slots.get(0);
		if (!can.isEmpty() && scrapU < Tuning.SCRAP_CAN_KG_U) {
			double a = WildcatterComponents.assay(can);
			if (scrapU + oxideU <= 1e-9 || Math.abs(a - assay) < 1e-6) {   // one assay at a time: never mixed
				assay = a;
				can.shrink(1);
				scrapU += Tuning.SCRAP_CAN_KG_U;
			}
		}
		if (scrapU <= 1e-9) {
			unit = !can.isEmpty() ? String.format("waiting: its trays hold %.2f %% scrap; the next can is another assay", assay * 100)
					: "NO SCRAP: put scrap cans in a hopper on its top";
			return;
		}
		if (oxideU >= 2 * Tuning.SCRAP_CAN_KG_U) {
			unit = "FULL: the granulator is not taking its U3O8";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = Math.min(scrapU, Tuning.SCRAP_OXIDIZER_U_KGH / 3600 * seconds() * strength);
		scrapU -= kg;
		oxideU += kg;
		unit = String.format("oxidising scrap to U3O8 at 400-450 C: %.1f kg of U an hour (%.2f %% U-235)", kg / seconds() * 3600, assay * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Scrap oxidizer (%.0f kW, %s): %s; scrap %.2f kg of U, U3O8 %.2f kg of U; %.2f kg given to the granulator; %d cans; ",
				Tuning.SCRAP_OXIDIZER_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, scrapU, oxideU, given, slots.get(0).getCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("scrapU", scrapU);
		output.putDouble("oxideU", oxideU);
		output.putDouble("assay", assay);
		output.putDouble("given", given);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		scrapU = input.getDoubleOr("scrapU", 0);
		oxideU = input.getDoubleOr("oxideU", 0);
		assay = input.getDoubleOr("assay", Tuning.ASSAY_PRODUCT);
		given = input.getDoubleOr("given", 0);
	}
}
