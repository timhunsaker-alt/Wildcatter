package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Dry UO2 scrap a fuel line part makes (docs/multiblocks/fuel-fabrication-extensions.md, 19c1): the press's cracked pellets, the grinder's
 * swarf, the inspection parts' rejects. It gathers as kg of U at its assay and goes out in 5 kg cans to a slot a hopper under the part
 * empties. It never holds the line up: with the slot full the scrap waits in the part, counted.
 */
final class ScrapCans {
	private double u;
	private double assay = Tuning.ASSAY_PRODUCT;
	private double total;

	void add(double kg, double a) {
		if (kg <= 0) {
			return;
		}
		assay = FuelStage.blend(u, assay, kg, a);
		u += kg;
		total += kg;
	}

	double held() {
		return u;
	}

	double total() {
		return total;
	}

	/** Fills cans into {@code slot} while there is a can's worth and room for it. */
	void emit(NonNullList<ItemStack> slots, int slot) {
		while (u >= Tuning.SCRAP_CAN_KG_U) {
			ItemStack can = WildcatterComponents.withAssay(new ItemStack(WildcatterItems.UO2_SCRAP_CAN), assay);
			ItemStack out = slots.get(slot);
			if (out.isEmpty()) {
				slots.set(slot, can);
			} else if (ItemStack.isSameItemSameComponents(out, can) && out.getCount() < out.getMaxStackSize()) {
				out.grow(1);
			} else {
				return;
			}
			u -= Tuning.SCRAP_CAN_KG_U;
		}
	}

	void save(ValueOutput output, String key) {
		output.putDouble(key + "U", u);
		output.putDouble(key + "Assay", assay);
		output.putDouble(key + "Total", total);
	}

	void load(ValueInput input, String key) {
		u = input.getDoubleOr(key + "U", 0);
		assay = input.getDoubleOr(key + "Assay", Tuning.ASSAY_PRODUCT);
		total = input.getDoubleOr(key + "Total", 0);
	}
}
