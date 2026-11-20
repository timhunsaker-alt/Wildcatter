package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The cell line, the cell plant's controller (docs/multiblocks/grid-batteries.md, 22a): electrode mixing and coating, calendering, stacking,
 * electrolyte filling, sealing, formation cycling and module assembly in a dry room. One LFP module (52 cells of 314 Ah, 52.2 kWh) every 10
 * minutes at full power (proposed): 115 kg of cathode powder, 60 kg of purified graphite, 9 kg of copper and 6 kg of aluminium foil, and its
 * LiPF6 electrolyte made in line from 5 kg of lithium carbonate and 3 kg of anhydrous HF in carbonate solvents from 6 kg of ethylene (a
 * stand-in for the solvent chain). Bags and ingots by a hopper on top; anhydrous HF to the east face of its main cell, ethylene to the east
 * face of the next cell south; modules out to a hopper under it. 300 kW at the world's industrial voltage, the box on the west face of its
 * main cell.
 */
public class CellLineBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int CATHODE = 0, GRAPHITE = 1, LI2CO3 = 2, COPPER = 3, ALUMINIUM = 4, OUT = 5;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);
	private double cathode;     // kg
	private double graphite;    // kg
	private double li2co3;      // kg
	private double copper;      // kg
	private double aluminium;   // kg
	/** Of the module in hand, 0 to 1; negative: none started. */
	private double progress = -1;
	private int made;
	private String unit = "starting";

	public CellLineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CELL_LINE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static Item itemOf(int slot) {
		return switch (slot) {
			case CATHODE -> WildcatterItems.LFP_CATHODE_BAG;
			case GRAPHITE -> WildcatterItems.PURIFIED_GRAPHITE_BAG;
			case LI2CO3 -> WildcatterItems.LI2CO3_BAG;
			case COPPER -> Items.COPPER_INGOT;
			case ALUMINIUM -> WildcatterItems.ALUMINIUM_INGOT;
			default -> Items.AIR;
		};
	}

	@Override
	public boolean takes(ItemStack stack) {
		for (int i = 0; i < OUT; i++) {
			if (stack.is(itemOf(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {CATHODE, GRAPHITE, LI2CO3, COPPER, ALUMINIUM} : side == Direction.DOWN ? new int[] {OUT} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= 0 && slot < OUT && stack.is(itemOf(slot));
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUT && side == Direction.DOWN;
	}

	private List<Lines.Face> hfIn() {
		return faces(at -> at[0] == 3 && at[1] == 0 && at[2] == 0, Direction.EAST);
	}

	private List<Lines.Face> ethyleneIn() {
		return faces(at -> at[0] == 3 && at[1] == 0 && at[2] == 1, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public int modules() {
		return slots.get(OUT).getCount();
	}

	public int made() {
		return made;
	}

	public double progress() {
		return progress;
	}

	/** For the game test: the module in hand brought to {@code value}. */
	public void testProgress(double value) {
		progress = value;
	}

	private void top(int slot, double[] store, double perItem, double want) {
		if (store[0] < want && !slots.get(slot).isEmpty()) {
			slots.get(slot).shrink(1);
			store[0] += perItem;
			setChanged();
		}
	}

	@Override
	protected void work(Level level) {
		ItemStack out = slots.get(OUT);
		if (out.getCount() >= 16) {
			unit = "FULL: set a hopper under it";
			return;
		}
		if (progress < 0) {
			double[] c = {cathode}, g = {graphite}, l = {li2co3}, cu = {copper}, al = {aluminium};
			top(CATHODE, c, Tuning.BULK_BAG_KG, Tuning.MODULE_CATHODE_KG);
			top(GRAPHITE, g, Tuning.BULK_BAG_KG, Tuning.MODULE_GRAPHITE_KG);
			top(LI2CO3, l, Tuning.BULK_BAG_KG, Tuning.MODULE_ELECTROLYTE_LI2CO3_KG);
			top(COPPER, cu, Tuning.COPPER_INGOT_KG, Tuning.MODULE_COPPER_KG);
			top(ALUMINIUM, al, Tuning.AL_INGOT_KG, Tuning.MODULE_ALUMINIUM_KG);
			cathode = c[0];
			graphite = g[0];
			li2co3 = l[0];
			copper = cu[0];
			aluminium = al[0];
			String missing = cathode < Tuning.MODULE_CATHODE_KG ? "NO CATHODE: LFP cathode bags in a hopper on top"
					: graphite < Tuning.MODULE_GRAPHITE_KG ? "NO GRAPHITE: purified graphite bags in a hopper on top"
					: li2co3 < Tuning.MODULE_ELECTROLYTE_LI2CO3_KG ? "NO LITHIUM CARBONATE (for the electrolyte): its bags in a hopper on top"
					: copper < Tuning.MODULE_COPPER_KG ? "NO COPPER: copper ingots in a hopper on top"
					: aluminium < Tuning.MODULE_ALUMINIUM_KG ? "NO ALUMINIUM: aluminium ingots in a hopper on top" : null;
			if (missing != null) {
				unit = missing;
				return;
			}
			double hfL = Tuning.MODULE_HF_KG / Tuning.AHF_KG_PER_L;
			double ethM3 = Tuning.MODULE_ETHYLENE_KG / Tuning.ETHYLENE_KG_PER_M3;
			if (Lines.available(level, hfIn(), Product.ANHYDROUS_HF, Pull.MACHINE, this) < hfL - 1e-6) {
				unit = "NO HF (for the electrolyte): pipe anhydrous HF to the east face of its main cell";
				return;
			}
			if (Lines.available(level, ethyleneIn(), Product.ETHYLENE, Pull.MACHINE, this) < ethM3 - 1e-6) {
				unit = "NO ETHYLENE (for the electrolyte's solvents): pipe it to the east face of its second cell";
				return;
			}
			Lines.pull(level, hfIn(), Product.ANHYDROUS_HF, hfL, Pull.MACHINE, this);
			Lines.pull(level, ethyleneIn(), Product.ETHYLENE, ethM3, Pull.MACHINE, this);
			cathode -= Tuning.MODULE_CATHODE_KG;
			graphite -= Tuning.MODULE_GRAPHITE_KG;
			li2co3 -= Tuning.MODULE_ELECTROLYTE_LI2CO3_KG;
			copper -= Tuning.MODULE_COPPER_KG;
			aluminium -= Tuning.MODULE_ALUMINIUM_KG;
			progress = 0;
			setChanged();
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		progress += strength * seconds() / Tuning.LFP_MODULE_SECONDS;
		if (progress >= 1) {
			progress = -1;
			made++;
			if (out.isEmpty()) {
				slots.set(OUT, new ItemStack(WildcatterItems.LFP_MODULE));
			} else {
				out.grow(1);
			}
			setChanged();
		}
		unit = String.format("making a module: %.0f %% (coating, stacking, filling, formation)", Math.max(0, progress) * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Cell line (%.0f kW, %s): ", Tuning.CELL_LINE_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; cathode %.0f kg, graphite %.0f kg, Li2CO3 %.0f kg, copper %.0f kg, aluminium %.0f kg in hand; %d modules made, %d waiting below; ",
						cathode, graphite, li2co3, copper, aluminium, made, modules()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("cathode", cathode);
		output.putDouble("graphite", graphite);
		output.putDouble("li2co3", li2co3);
		output.putDouble("copper", copper);
		output.putDouble("aluminium", aluminium);
		output.putDouble("progress", progress);
		output.putInt("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		cathode = input.getDoubleOr("cathode", 0);
		graphite = input.getDoubleOr("graphite", 0);
		li2co3 = input.getDoubleOr("li2co3", 0);
		copper = input.getDoubleOr("copper", 0);
		aluminium = input.getDoubleOr("aluminium", 0);
		progress = input.getDoubleOr("progress", -1);
		made = input.getIntOr("made", 0);
	}
}
