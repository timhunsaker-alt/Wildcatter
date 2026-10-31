package dev.wildcatter.item;

import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The item of a wire, cable or busway block. Each conductor size is its own stack: the size rides on the stack as a component,
 * goes into the placed block, and comes back out when it is broken. The name shows the size; the tooltip its ampacity.
 */
public class ConductorItem extends BlockItem {
	private final Voltage tier;
	private final ConductorSize.Kind kind;

	public ConductorItem(Block block, Properties properties, Voltage tier, ConductorSize.Kind kind) {
		super(block, properties);
		this.tier = tier;
		this.kind = kind;
	}

	public Voltage tier() {
		return tier;
	}

	public ConductorSize.Kind kind() {
		return kind;
	}

	/** The size a stack carries, or the tier's standard size for a stack without one. */
	public static ConductorSize size(ItemStack stack, Voltage tier, ConductorSize.Kind kind) {
		return ConductorSize.orStandard(stack.get(WildcatterComponents.CONDUCTOR_SIZE), tier, kind);
	}

	/** A stack of this item in one size. */
	public ItemStack stack(ConductorSize size) {
		ItemStack stack = new ItemStack(this);
		stack.set(WildcatterComponents.CONDUCTOR_SIZE, size);
		return stack;
	}

	@Override
	public Component getName(ItemStack stack) {
		return named(super.getName(stack), size(stack, tier, kind));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(rating(size(stack, tier, kind)));
	}

	static Component named(Component base, ConductorSize size) {
		return base.copy().append(", " + size.label());
	}

	static Component rating(ConductorSize size) {
		return Component.literal(String.format("Rated %.0f A, %.3f mOhm per metre", size.amps(), size.ohmsPerMeter() * 1000)).withStyle(ChatFormatting.GRAY);
	}
}
