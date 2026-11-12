package dev.wildcatter.block;

import dev.wildcatter.TimeScale;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Enrichment;
import dev.wildcatter.item.CylinderTags;
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
 * The product sampling autoclave (docs/multiblocks/enrichment-operations.md item 5, 16c2): a steam-heated autoclave that liquefies a 30B
 * and draws a liquid sample for its assay, 3 x 3 x 5, 50 kW on the industrial supply (box west). Full 30Bs by a hopper on top come out
 * SAMPLED below after 30 minutes (through {@link TimeScale}). Set to blend (sneak-click: SAMPLE, then each product assay), with two full
 * 30Bs of assays either side of the target and an empty 30B, it blends them: one 30B at the target and one at the rest, both sampled,
 * and the empty shell that is left.
 */
public class SamplingAutoclaveBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int FULL_A = 0, FULL_B = 1, EMPTY = 2, OUT = 3, OUTS = 3;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(OUT + OUTS, ItemStack.EMPTY);
	private int mode;           // 0 SAMPLE, else 1 + the index of the product assay to blend to
	private double time;        // s into the cylinder (or the blend) in it
	private int sampled, blended;
	private String unit = "starting";
	private String last = "";   // what it did last, for the readout

	public SamplingAutoclaveBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SAMPLING_AUTOCLAVE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static boolean full(ItemStack stack) {
		return stack.is(WildcatterItems.LEU_CYLINDER_30B) && !CylinderTags.sampled(stack);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return full(stack) || stack.is(WildcatterItems.EMPTY_30B_CYLINDER);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {FULL_A, FULL_B, EMPTY} : side == Direction.DOWN ? new int[] {OUT, OUT + 1, OUT + 2} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return (slot == FULL_A || slot == FULL_B) && full(stack) && slots.get(slot).isEmpty() || slot == EMPTY && stack.is(WildcatterItems.EMPTY_30B_CYLINDER);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= OUT && side == Direction.DOWN;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/** The blend's target, or a negative number when it samples. */
	public double target() {
		return mode == 0 ? -1 : Enrichment.PRODUCT_ASSAYS[(mode - 1) % Enrichment.PRODUCT_ASSAYS.length];
	}

	/** A sneak-click: SAMPLE, then blend to each product assay. */
	public String nextMode() {
		mode = (mode + 1) % (Enrichment.PRODUCT_ASSAYS.length + 1);
		time = 0;
		setChanged();
		return mode == 0 ? "Sampling autoclave: SAMPLE each 30B" : String.format("Sampling autoclave: BLEND two 30Bs to %.2f %% (an empty 30B for the blend)", target() * 100);
	}

	/** For the game test: the cylinder (or blend) in it all but done. */
	public void finishForTest() {
		time = TimeScale.scaled(Tuning.SAMPLING_SECONDS) - 1;
		setChanged();
	}

	public int outCount() {
		int n = 0;
		for (int i = OUT; i < OUT + OUTS; i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	private boolean roomFor(int n) {
		int free = 0;
		for (int i = OUT; i < OUT + OUTS; i++) {
			free += slots.get(i).isEmpty() ? 1 : 0;
		}
		return free >= n;
	}

	private void out(ItemStack stack) {
		for (int i = OUT; i < OUT + OUTS; i++) {
			if (slots.get(i).isEmpty()) {
				slots.set(i, stack);
				return;
			}
		}
	}

	private static ItemStack thirtyB(double assay) {
		return CylinderTags.markSampled(WildcatterComponents.withAssay(new ItemStack(WildcatterItems.LEU_CYLINDER_30B), assay));
	}

	@Override
	protected void work(Level level) {
		boolean blend = mode > 0;
		ItemStack a = slots.get(FULL_A), b = slots.get(FULL_B);
		if (a.isEmpty() && b.isEmpty()) {
			unit = (blend ? "idle: two 30Bs to blend and an empty one, by a hopper on top" : "idle: put full 30Bs in a hopper on top") + (last.isEmpty() ? "" : "; last: " + last);
			time = 0;
			return;
		}
		double t = target();
		if (blend) {
			if (a.isEmpty() || b.isEmpty() || slots.get(EMPTY).isEmpty()) {
				unit = String.format("BLEND to %.2f %%: it needs two full 30Bs and an empty one", t * 100);
				return;
			}
			double lo = Math.min(WildcatterComponents.assay(a), WildcatterComponents.assay(b)), hi = Math.max(WildcatterComponents.assay(a), WildcatterComponents.assay(b));
			if (!(lo < t - 1e-9 && t < hi + 1e-9 && hi - lo > 1e-9)) {
				unit = String.format("CANNOT BLEND: %.2f %% is not between %.2f and %.2f %%", t * 100, lo * 100, hi * 100);
				return;
			}
		}
		if (!roomFor(blend ? 3 : 1)) {
			unit = "FULL: take the sampled cylinders (a hopper under it)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double hold = TimeScale.scaled(Tuning.SAMPLING_SECONDS);
		time += seconds() * strength;
		if (time < hold - 1e-6) {
			unit = String.format("%s at about 95 C: %.0f %% of its %.0f minutes%s", blend ? String.format("liquefying two 30Bs to blend to %.2f %%", t * 100)
					: "liquefying a 30B and drawing its sample", time / hold * 100, Tuning.SAMPLING_SECONDS / 60, TimeScale.note(Tuning.SAMPLING_SECONDS));
			return;
		}
		time = 0;
		if (blend) {
			// a 30B at the target from both, and the rest of both in one cylinder: the mass and the U-235 balance
			double rest = WildcatterComponents.assay(a) + WildcatterComponents.assay(b) - t;
			slots.set(FULL_A, ItemStack.EMPTY);
			slots.set(FULL_B, ItemStack.EMPTY);
			slots.get(EMPTY).shrink(1);
			out(thirtyB(t));
			out(thirtyB(rest));
			out(new ItemStack(WildcatterItems.EMPTY_30B_CYLINDER));
			blended++;
			unit = last = String.format("blended: a 30B of %.2f %% and one of %.2f %%, both SAMPLED", t * 100, rest * 100);
		} else {
			ItemStack in = a.isEmpty() ? b : a;
			double assay = WildcatterComponents.assay(in);
			ItemStack done = CylinderTags.markSampled(in.copy());
			in.shrink(1);
			out(done);
			sampled++;
			unit = last = String.format("SAMPLED, %.2f %%: the 30B is signed off for the fuel plant", assay * 100);
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		String m = mode == 0 ? "SAMPLE" : String.format("BLEND to %.2f %%", target() * 100);
		return Component.literal(String.format("Sampling autoclave (%.0f kW, %s), %s: %s; %d sampled, %d blends, %d out below; ", Tuning.SAMPLING_WATTS / 1000,
				Voltage.INDUSTRIAL.label(), m, unit, sampled, blended, outCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("mode", mode);
		output.putDouble("time", time);
		output.putInt("sampled", sampled);
		output.putInt("blended", blended);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		mode = input.getIntOr("mode", 0);
		time = input.getDoubleOr("time", 0);
		sampled = input.getIntOr("sampled", 0);
		blended = input.getIntOr("blended", 0);
	}
}
