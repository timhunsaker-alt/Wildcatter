package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Product;
import java.util.List;
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
 * The paste plant (docs/multiblocks/anode-plant.md): crushing and screening, the aggregate preheater and a heated mixer at about
 * 160 C, 200 kW at 4.16 kV. Calcined coke bags (slot 0) and crushed butts (slot 1) by a hopper on top, pitch at the east face of
 * its bottom cells: 65 % coke, 20 % butts and 15 % pitch, the butts made up with coke while it has none. It holds up to two anodes'
 * worth of paste for the forming press it touches.
 */
public class PastePlantBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double coke;
	private double butts;
	private double paste;
	private String unit = "starting";

	public PastePlantBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PASTE_PLANT_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.CALCINED_COKE_BAG);
	}

	private static boolean takesIn(int slot, ItemStack stack) {
		return slot == 0 ? stack.is(WildcatterItems.CALCINED_COKE_BAG) : slot == 1 && stack.is(WildcatterItems.CRUSHED_BUTTS);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takesIn(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takesIn(slot, stack);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** kg of paste ready for the press. */
	public double paste() {
		return paste;
	}

	public void takePaste(double kg) {
		paste = Math.max(0, paste - kg);
		setChanged();
	}

	/** For the game test: paste as if mixed. */
	public void addPaste(double kg) {
		paste += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		double max = Tuning.ANODES_PER_SECOND * Tuning.GREEN_ANODE_KG * Tuning.CARBON_CATCH_UP * seconds();
		if (coke < max * (Tuning.PASTE_COKE + Tuning.PASTE_BUTTS) && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			coke += Tuning.BULK_BAG_KG;
			setChanged();
		}
		if (butts < max * Tuning.PASTE_BUTTS && !slots.get(1).isEmpty()) {
			slots.get(1).shrink(1);
			butts += Tuning.BUTT_KG;
			setChanged();
		}
		max = Math.min(max, 2 * Tuning.GREEN_ANODE_KG - paste);
		if (max <= 1e-7) {
			unit = "FULL: set a forming press against it";
			return;
		}
		double pitch = Lines.available(level, east(), Product.PITCH, Pull.MACHINE, this);
		if (coke < 0.01) {
			unit = "NO COKE: put calcined coke bags in its hopper";
			return;
		}
		if (pitch < 0.01) {
			unit = "NO PITCH: pipe pitch to its east side";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double useButts = Math.min(kg * Tuning.PASTE_BUTTS, butts);
		double needCoke = kg * Tuning.PASTE_COKE + (kg * Tuning.PASTE_BUTTS - useButts);
		double scale = Math.min(1, Math.min(coke / needCoke, pitch / (kg * Tuning.PASTE_PITCH)));
		kg *= scale;
		useButts *= scale;
		needCoke *= scale;
		double gotPitch = Lines.pull(level, east(), Product.PITCH, kg * Tuning.PASTE_PITCH, Pull.MACHINE, this);
		coke = Math.max(0, coke - needCoke);
		butts = Math.max(0, butts - useButts);
		paste += needCoke + useButts + gotPitch;
		unit = String.format("mixing at 160 C: %.0f kg/h of paste%s", kg / seconds() * 3600, useButts < kg * Tuning.PASTE_BUTTS * 0.999 ? " (SHORT OF BUTTS: made up with coke)" : "");
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Paste plant: %s; coke %,.0f kg and %d bags, butts %,.0f kg and %d more; %,.0f kg of paste for the press; ", unit, coke,
				slots.get(0).getCount(), butts, slots.get(1).getCount(), paste) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("coke", coke);
		output.putDouble("butts", butts);
		output.putDouble("paste", paste);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		coke = input.getDoubleOr("coke", 0);
		butts = input.getDoubleOr("butts", 0);
		paste = input.getDoubleOr("paste", 0);
	}
}
