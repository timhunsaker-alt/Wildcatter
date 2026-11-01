package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Conversion;
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
 * The fluid-bed reduction reactor at the head of the conversion line (docs/multiblocks/conversion.md): yellowcake drums in by a
 * hopper on top, emptied into it and sent out empty to a hopper under it; hydrogen on the east face of its bottom cells reduces
 * the uranium to UO2 at 575 C for the hydrofluorinator touching it.
 */
public class ReductionReactorBlockEntity extends PlantUnitBlockEntity implements HopperFed, UraniumStage {
	private static final int OUT = 4;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private double feed;      // kg of U of yellowcake tipped in, not yet reduced
	private double uranium;   // kg of U as UO2

	public ReductionReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REDUCTION_REACTOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.YELLOWCAKE_DRUM);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2, 3, 4} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot > 0 && side == Direction.DOWN;
	}

	private List<Lines.Face> hydrogen() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public double uranium() {
		return uranium;
	}

	@Override
	public double takeUranium(double kg) {
		double got = Math.min(kg, uranium);
		uranium -= got;
		setChanged();
		return got;
	}

	/** kg of U in a yellowcake drum. */
	public static double uraniumPerDrum() {
		return Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8 * dev.wildcatter.ore.Ore.U_IN_U3O8;
	}

	/** Tips the next drum in, if the hopper under it has room for the empty. */
	private void tip() {
		for (int i = 1; i <= OUT; i++) {
			ItemStack out = slots.get(i);
			if (out.isEmpty() || (out.is(WildcatterItems.EMPTY_DRUM) && out.getCount() < out.getMaxStackSize())) {
				useOne();
				feed += uraniumPerDrum();
				if (out.isEmpty()) {
					slots.set(i, new ItemStack(WildcatterItems.EMPTY_DRUM));
				} else {
					out.grow(1);
				}
				return;
			}
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ReductionReactorBlockEntity r) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double max = Math.min(Conversion.uraniumKgs() * 1.2 * seconds(), Conversion.uraniumKgs() * Tuning.PLANT_HOLD_SECONDS - r.uranium);
		if (r.feed < max && r.fed() > 0) {
			r.tip();
		}
		double waiting = Lines.available(level, r.hydrogen(), Product.HYDROGEN, Pull.MACHINE, r);
		double kg = Math.min(Math.min(max, r.feed), waiting / Conversion.hydrogenM3PerU());
		if (r.feed < 1e-6) {
			r.status = "idle: no yellowcake (put a hopper of yellowcake drums on top)";
		} else if (waiting < 1e-6) {
			r.status = "NO HYDROGEN: pipe hydrogen from a steam reformer to its east side";
		} else if (kg <= 1e-7) {
			r.status = "idle: the hydrofluorinator is full";
		} else {
			Lines.pull(level, r.hydrogen(), Product.HYDROGEN, kg * Conversion.hydrogenM3PerU(), Pull.MACHINE, r);
			r.feed -= kg;
			r.uranium += kg;
			r.status = String.format("reducing %.3f kg/s of U to UO2 with %.1f m3/h of hydrogen", kg / seconds(), kg * Conversion.hydrogenM3PerU() / seconds() * 3600);
		}
		r.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Reduction reactor: " + status + String.format("; %.0f kg of U in its feed, %d drums waiting, %.1f kg of UO2 (as U) held", feed, fed(), uranium));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("feed", feed);
		output.putDouble("uranium", uranium);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		uranium = input.getDoubleOr("uranium", 0);
	}
}
