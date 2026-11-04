package dev.wildcatter.ore;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The mineral a block of ore gives. Its tooltip says what it is, how rich this ore is, and how rich real ore is. */
public class MineralItem extends Item {
	private final Ore ore;

	public MineralItem(Properties properties, Ore ore) {
		super(properties);
		this.ore = ore;
	}

	public Ore ore() {
		return ore;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.literal(ore.formula).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("This ore: " + ore.gradeText()).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("Real ore: " + ore.realGrade).withStyle(ChatFormatting.DARK_GRAY));
		usage(ore, tooltip);
	}

	/** Where it is found, what mines it and what uses it (OreUses), shared with the ore block's tooltip. */
	static void usage(Ore ore, Consumer<Component> tooltip) {
		for (String line : OreUses.lines(ore)) {
			tooltip.accept(Component.literal(line).withStyle(line.startsWith("Used") ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
		}
	}
}
