package dev.wildcatter.item;

import dev.wildcatter.WildcatterComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** 16c1: a 30B of LEU whose tooltip reads its assay (docs/multiblocks/enrichment-operations.md); unstamped, the standard 4.95 %. */
public class AssayedItem extends net.minecraft.world.item.Item {
	private final String first;

	public AssayedItem(Properties properties, String first) {
		super(properties);
		this.first = first;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.literal(first).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal(String.format("%.2f %% U-235: low-enriched, for fuel", WildcatterComponents.assay(stack) * 100)).withStyle(ChatFormatting.GRAY));
	}
}
