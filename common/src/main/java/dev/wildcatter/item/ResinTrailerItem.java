package dev.wildcatter.item;

import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.block.ResinTrailerBlockEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A resin trailer, carried (16d1, docs/multiblocks/isr-operations.md): set down, it stands 2 x 3 x 6 with its loading. */
public class ResinTrailerItem extends BigPartItem {
	public ResinTrailerItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.literal(ResinTrailerBlockEntity.describe(stack.getOrDefault(WildcatterComponents.RESIN_U3O8, 0.0))).withStyle(ChatFormatting.GRAY));
	}
}
