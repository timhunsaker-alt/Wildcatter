package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * A fresh assembly's handling record (docs/multiblocks/reactor-extensions.md, 19b), kept on the item: whether it has been handled (its
 * one roll for a bent grid, with the fuel handling damage rule), whether a grid is bent, and whether it has been inspected.
 */
public final class FuelAssemblyTags {
	private FuelAssemblyTags() {
	}

	private static double chance = Tuning.BENT_GRID_CHANCE;

	/** For the game test: the chance a move bends a grid (negative for the real 1 %). */
	public static void setBentChanceForTest(double c) {
		chance = c < 0 ? Tuning.BENT_GRID_CHANCE : c;
	}

	private static CompoundTag tag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	private static void put(ItemStack stack, String key, boolean value) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(key, value));
	}

	public static boolean handled(ItemStack stack) {
		return tag(stack).getBooleanOr("handled", false);
	}

	public static boolean bent(ItemStack stack) {
		return tag(stack).getBooleanOr("bentGrid", false);
	}

	public static boolean inspected(ItemStack stack) {
		return tag(stack).getBooleanOr("inspected", false);
	}

	/** A move by hopper or crane: with the rule on, its one chance of a bent grid. */
	public static void handle(ItemStack stack, RandomSource random) {
		if (!dev.wildcatter.Rules.fuelHandlingDamage() || handled(stack)) {
			return;
		}
		put(stack, "handled", true);
		if (random.nextDouble() < chance) {
			put(stack, "bentGrid", true);
		}
	}

	public static void markInspected(ItemStack stack) {
		put(stack, "inspected", true);
	}

	/** A bent grid set by hand (the vessel giving back a fresh assembly it held). */
	public static void markBent(ItemStack stack) {
		put(stack, "handled", true);
		put(stack, "bentGrid", true);
	}

	/** The assembly fixture has straightened it: the grid good, to be inspected again. */
	public static void repaired(ItemStack stack) {
		put(stack, "bentGrid", false);
		put(stack, "inspected", false);
	}
}
