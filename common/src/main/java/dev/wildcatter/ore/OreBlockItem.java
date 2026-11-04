package dev.wildcatter.ore;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** An ore block in the hand (Silk Touch or creative). Its tooltip names its mineral and says where it is found and what uses it. */
public class OreBlockItem extends BlockItem {
	private final Ore ore;

	public OreBlockItem(Block block, Properties properties, Ore ore) {
		super(block, properties);
		this.ore = ore;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.literal(ore.mineralName + ", " + ore.formula).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal(ore.gradeText()).withStyle(ChatFormatting.GRAY));
		MineralItem.usage(ore, tooltip);
	}
}
