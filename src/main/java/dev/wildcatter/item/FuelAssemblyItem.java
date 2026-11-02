package dev.wildcatter.item;

import dev.wildcatter.block.FuelAssemblyBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A finished 17x17 fuel assembly, carried (docs/multiblocks/fuel.md): it stands up as its block, 4.06 m tall. */
public class FuelAssemblyItem extends BigPartItem {
	public FuelAssemblyItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.literal(FuelAssemblyBlock.describe()).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.literal("214 mm square, 4.06 m tall: 24 guide thimbles, 1 instrument tube, 8 grids").withStyle(ChatFormatting.GRAY));
	}
}
