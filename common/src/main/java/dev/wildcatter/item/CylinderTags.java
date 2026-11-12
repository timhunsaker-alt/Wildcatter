package dev.wildcatter.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 16c2 (docs/multiblocks/enrichment-operations.md items 5 and 6), kept on a UF6 cylinder: whether a 30B has been sampled in the sampling
 * autoclave, and the heel of UF6 an emptied cylinder still holds (kg), counted when it is filled again.
 */
public final class CylinderTags {
	private CylinderTags() {
	}

	private static CompoundTag tag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	public static boolean sampled(ItemStack stack) {
		return tag(stack).getBooleanOr("sampled", false);
	}

	public static ItemStack markSampled(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("sampled", true));
		return stack;
	}

	public static double heel(ItemStack stack) {
		return tag(stack).getDoubleOr("heel", 0);
	}

	public static ItemStack withHeel(ItemStack stack, double kg) {
		if (kg > 0) {
			CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putDouble("heel", kg));
		}
		return stack;
	}
}
