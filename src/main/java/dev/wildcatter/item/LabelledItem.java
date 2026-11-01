package dev.wildcatter.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A plain item whose tooltip says what it is and how much is in it: magnesia, drums. */
public class LabelledItem extends Item {
	private final String[] lines;

	public LabelledItem(Properties properties, String... lines) {
		super(properties);
		this.lines = lines;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		for (String line : lines) {
			tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
		}
	}
}
