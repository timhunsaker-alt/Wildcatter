package dev.wildcatter.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** 19d: an RCCA taken out of the vessel keeps the B-10 its B4C rodlets have burnt (docs/multiblocks/reactor-extensions.md, item 10). */
public final class RccaTags {
	private RccaTags() {
	}

	public static double b10(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
		return tag.getDoubleOr("b10", 0);
	}

	public static ItemStack withB10(ItemStack stack, double b10) {
		if (b10 > 0) {
			CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putDouble("b10", b10));
		}
		return stack;
	}
}
