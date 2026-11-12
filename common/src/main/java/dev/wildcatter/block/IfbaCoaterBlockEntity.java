package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Zirconium;
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
 * The IFBA coater (docs/multiblocks/fuel-fabrication-extensions.md, 19c2): a sputter coater putting a thin zirconium diboride (ZrB2) layer
 * on pellets, beside the line against the rod loader; 50 kW at LV (box north). Zirconium as Zircaloy tube and boric acid bags by a hopper
 * on top: about 4 g of zirconium and 5.7 g of boric acid a coated rod (estimate). The rod loader asks it for each IFBA rod its sneak-click
 * setting calls for (0, 32, 64 or 104 an assembly).
 */
public class IfbaCoaterBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double zr;         // kg of zirconium on its targets
	private double boric;      // kg of boric acid
	private int coated;        // rods' worth, all told
	private String unit = "idle: the rod loader has called for no IFBA rods";

	public IfbaCoaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.IFBA_COATER_ENTITY, pos, state);
	}

	/** Its box on the north face of its main cell: its west side stands against the rod loader. */
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
		return stack.is(WildcatterItems.ZIRCALOY_TUBE) || stack.is(WildcatterItems.BORIC_ACID);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && stack.is(WildcatterItems.ZIRCALOY_TUBE) || slot == 1 && stack.is(WildcatterItems.BORIC_ACID);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	public int coated() {
		return coated;
	}

	/** The rod loader's call for one IFBA rod's pellets: null when coated, or why not. */
	public @Nullable String coat(Level level) {
		if (zr < Tuning.IFBA_ZR_KG_PER_ROD && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			zr += Zirconium.claddingKg();
		}
		if (boric < Tuning.IFBA_BORIC_KG_PER_ROD && !slots.get(1).isEmpty()) {
			slots.get(1).shrink(1);
			boric += Tuning.BORIC_ACID_BAG_KG;
		}
		if (zr < Tuning.IFBA_ZR_KG_PER_ROD) {
			return unit = "NO ZIRCONIUM: put Zircaloy tubes in the IFBA coater's hopper";
		}
		if (boric < Tuning.IFBA_BORIC_KG_PER_ROD) {
			return unit = "NO BORIC ACID: put boric acid bags in the IFBA coater's hopper";
		}
		if (drive(level) <= 0) {
			return unit = "IFBA coater starting: " + describe();
		}
		zr -= Tuning.IFBA_ZR_KG_PER_ROD;
		boric -= Tuning.IFBA_BORIC_KG_PER_ROD;
		coated++;
		unit = String.format("sputtering ZrB2 on a rod's pellets: %d rods coated", coated);
		setChanged();
		return null;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("IFBA coater (ZrB2 sputter, %.0f kW, %s): %s; zirconium %.2f kg, boric acid %.1f kg; ", Tuning.IFBA_COATER_WATTS / 1000,
				Voltage.INDUSTRIAL.label(), unit, zr, boric) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("zr", zr);
		output.putDouble("boric", boric);
		output.putInt("coated", coated);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		zr = input.getDoubleOr("zr", 0);
		boric = input.getDoubleOr("boric", 0);
		coated = input.getIntOr("coated", 0);
	}
}
