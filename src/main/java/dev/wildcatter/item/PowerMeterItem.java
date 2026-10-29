package dev.wildcatter.item;

import dev.wildcatter.energy.Inspectable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Right-click any machine or cable to read out what it holds and how fast it is passing power on. */
public class PowerMeterItem extends Item {
	public PowerMeterItem(Properties properties) {
		super(properties);
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
