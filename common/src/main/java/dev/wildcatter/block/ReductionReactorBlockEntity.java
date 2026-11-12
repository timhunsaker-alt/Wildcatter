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
	private double aduFeed;   // of the feed, kg of U from 96 % U3O8 drums (16a)
	private double aduShare;  // of the UO2 held, the share from them
	private double hydrogenKg; // kg of H2 used, and kg of U reduced, since placed (the game test reads them)
	private double reducedU;

	public double hydrogenKg() {
		return hydrogenKg;
	}

	public double reducedU() {
		return reducedU;
	}

	public ReductionReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REDUCTION_REACTOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.YELLOWCAKE_DRUM) || stack.is(WildcatterItems.U3O8_DRUM);
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
	public double aduShare() {
		return aduShare;
	}

	/** kg of U in a 96 % U3O8 drum (16a). */
	public static double uraniumPerAduDrum() {
		return Tuning.DRUM_NET_KG * Tuning.ADU_U3O8 * dev.wildcatter.ore.Ore.U_IN_U3O8;
	}

	/** kg of H2 a kg of U for a feed with this share of U3O8 drums. */
	public static double hydrogenKgPerU(double adu) {
		return Tuning.CONV_H2_PER_U * (1 - adu) + Tuning.CONV_H2_PER_U_ADU * adu;
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
				boolean calcined = slots.get(0).is(WildcatterItems.U3O8_DRUM);
				useOne();
				double u = calcined ? uraniumPerAduDrum() : uraniumPerDrum();
				feed += u;
				aduFeed += calcined ? u : 0;
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
		if (r.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double rate = Conversion.uraniumKgs() * ConversionSize.scale(r);   // 16b2: the large size, four times
		double max = Math.min(rate * 1.2 * seconds(), rate * Tuning.PLANT_HOLD_SECONDS - r.uranium);
		if (r.feed < max && r.fed() > 0) {
			r.tip();
		}
		double waiting = Lines.available(level, r.hydrogen(), Product.HYDROGEN, Pull.MACHINE, r);
		double share = r.feed > 1e-9 ? Math.min(1, r.aduFeed / r.feed) : 0;
		double m3PerU = hydrogenKgPerU(share) / Tuning.H2_KG_PER_M3;
		double kg = Math.min(Math.min(max, r.feed), waiting / m3PerU);
		if (r.feed < 1e-6) {
			r.status = "idle: no yellowcake (put a hopper of yellowcake drums on top)";
		} else if (waiting < 1e-6) {
			r.status = "NO HYDROGEN: pipe hydrogen from a steam reformer to its east side";
		} else if (kg <= 1e-7) {
			r.status = "idle: the hydrofluorinator is full";
		} else {
			Lines.pull(level, r.hydrogen(), Product.HYDROGEN, kg * m3PerU, Pull.MACHINE, r);
			r.aduShare = UraniumStage.blend(r.uranium, r.aduShare, kg, share);
			r.feed -= kg;
			r.aduFeed = Math.max(0, r.aduFeed - kg * share);
			r.uranium += kg;
			r.hydrogenKg += kg * m3PerU * Tuning.H2_KG_PER_M3;
			r.reducedU += kg;
			r.status = String.format("reducing %.3f kg/s of U to UO2 with %.1f m3/h of hydrogen", kg / seconds(), kg * m3PerU / seconds() * 3600)
					+ (share > 0.001 ? String.format(" (%.0f %% of it from U3O8 drums)", share * 100) : "");
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
		output.putDouble("aduFeed", aduFeed);
		output.putDouble("aduShare", aduShare);
		output.putDouble("hydrogenKg", hydrogenKg);
		output.putDouble("reducedU", reducedU);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		uranium = input.getDoubleOr("uranium", 0);
		aduFeed = input.getDoubleOr("aduFeed", 0);
		aduShare = input.getDoubleOr("aduShare", 0);
		hydrogenKg = input.getDoubleOr("hydrogenKg", 0);
		reducedU = input.getDoubleOr("reducedU", 0);
	}
}
