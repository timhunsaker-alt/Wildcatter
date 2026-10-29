package dev.wildcatter.item;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Right-click any machine or cable to read out what it holds and how fast it is passing power on. */
public class PowerMeterItem extends Item {
	public PowerMeterItem(Properties properties) {
		super(properties);
	}

	/**
	 * Called by machines first when someone right-clicks them: if it is the meter, read the machine out (and never
	 * shock the person or flip the switch). Returns null if the item is not the meter.
	 */
	public static InteractionResult readIfMeter(ItemStack stack, Level level, BlockPos pos, Player player) {
		if (!stack.is(WildcatterItems.POWER_METER)) {
			return null;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Inspectable machine) {
			if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
				context.getPlayer().sendOverlayMessage(machine.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
