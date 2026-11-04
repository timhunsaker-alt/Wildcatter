package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The vacuum tapping crucible (docs/multiblocks/potline.md), standing in for the crucible and its transport vehicle: right-click a pot
 * to siphon its metal, up to 8 t, and the casting machine to pour it. The kg it holds is on the stack.
 */
public class TappingCrucibleItem extends Item {
	public TappingCrucibleItem(Properties properties) {
		super(properties);
	}

	public static double kg(ItemStack stack) {
		Double kg = stack.get(WildcatterComponents.METAL_KG);
		return kg == null ? 0 : kg;
	}

	public static void setKg(ItemStack stack, double kg) {
		if (kg <= 1e-6) {
			stack.remove(WildcatterComponents.METAL_KG);
		} else {
			stack.set(WildcatterComponents.METAL_KG, Math.min(Tuning.CRUCIBLE_KG, kg));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.literal(String.format("%,.0f kg of aluminium of %,.0f kg", kg(stack), Tuning.CRUCIBLE_KG)).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Right-click a pot to tap it, the casting machine to pour").withStyle(ChatFormatting.GRAY));
	}
}
