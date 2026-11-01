package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import net.minecraft.core.BlockPos;
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
 * The mill's jaw crusher (75 kW, 480 V) (docs/multiblocks/uranium-mill.md): uranium ore or pitchblende in by a hopper on top, crushed
 * for the SAG mill touching it, which drives it. It keeps the ore it has broken out of a block, and that ore's U3O8.
 */
public class JawCrusherBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double ore;
	private double u3o8;

	public JawCrusherBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.JAW_CRUSHER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static @Nullable Ore ore(ItemStack stack) {
		for (Ore o : new Ore[] {Ore.URANINITE, Ore.PITCHBLENDE}) {
			if (stack.is(OreBlocks.MINERALS.get(o))) {
				return o;
			}
		}
		return null;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return ore(stack) != null;
	}

	/** Breaks the next block out of its bin if it has run low. */
	private void refill(double want) {
		while (ore < want && fed() > 0) {
			Ore o = ore(useOne());
			if (o != null) {
				ore += o.blockKg();
				u3o8 += o.blockKg() * o.u3o8();
			}
		}
	}

	public boolean hasOre() {
		refill(0.001);
		return ore > 0.001;
	}

	/** The U3O8 grade of the ore it is crushing now. */
	public double grade() {
		return ore > 0 ? u3o8 / ore : 0;
	}

	/** Crushes up to {@code kg}, at its motor's output; returns the kg crushed and fills {@code u3o8Out[0]} with their U3O8. */
	public double crush(Level level, double kg, double[] u3o8Out) {
		double strength = drive(level);
		refill(kg);
		double got = Math.min(kg * strength, ore);
		double u = ore > 0 ? u3o8 * got / ore : 0;
		ore -= got;
		u3o8 -= u;
		u3o8Out[0] = u;
		setChanged();
		return got;
	}

	/** For the game test: the rest of the block it is crushing goes, so the next from its bin follows. */
	public void testClear() {
		ore = 0;
		u3o8 = 0;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Jaw crusher (75 kW): %,.0f kg of ore broken out at %.1f %% U3O8, %d blocks in its bin; %s", ore, grade() * 100, fed(), describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("ore", ore);
		output.putDouble("u3o8", u3o8);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		ore = input.getDoubleOr("ore", 0);
		u3o8 = input.getDoubleOr("u3o8", 0);
	}
}
