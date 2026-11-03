package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Solid carbon by the item (docs/multiblocks/coke-ovens.md): coal and charcoal as before, and metallurgical coke from the coke ovens,
 * which works wherever coal does: a chlorinator's or the B4C furnace's carbon, the steam plant's burner.
 */
public final class SolidCarbon {
	private SolidCarbon() {
	}

	/** True for coal, charcoal and metallurgical coke. */
	public static boolean is(ItemStack stack) {
		return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(WildcatterItems.MET_COKE);
	}

	/** kg of carbon in one of the item. */
	public static double carbonKg(ItemStack stack) {
		return stack.is(WildcatterItems.MET_COKE) ? Tuning.MET_COKE_KG * Tuning.MET_COKE_CARBON : is(stack) ? Tuning.COAL_KG * Tuning.COAL_CARBON : 0;
	}
}
