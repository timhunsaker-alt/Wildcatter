package dev.wildcatter.control;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** The key to a device's HAND / OFF / AUTO selector: a click steps it (docs/multiblocks/plc.md). */
public class ControlKeyItem extends Item {
	public ControlKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ControlDevice device = ControlNet.deviceAt(context.getLevel(), context.getClickedPos());
		if (device == null) {
			return InteractionResult.PASS;
		}
		if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
			String now = device.cycleHoa(context.getClickedPos());
			context.getPlayer().sendOverlayMessage(Component.literal(now == null ? device.deviceName() + " has no HAND / OFF / AUTO selector" : device.deviceName() + ": " + now));
		}
		return InteractionResult.SUCCESS;
	}
}
