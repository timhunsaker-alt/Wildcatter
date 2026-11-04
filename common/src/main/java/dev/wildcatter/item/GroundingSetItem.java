package dev.wildcatter.item;

import dev.wildcatter.block.MultiCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A grounding clamp set (docs/multiblocks/gen-bus-protection.md; ASTM F855): three clamps on 2/0 copper to a cluster bar. Right-click
 * a cable, busway or bus duct to put the clamps on it, phases A, B and C to ground; right-click it again to take them off. On a dead
 * conductor the grounds keep it at earth; on a live one they are a bolted fault.
 */
public class GroundingSetItem extends Item {
	public GroundingSetItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		if (!(level.getBlockEntity(clicked) instanceof MultiCableBlockEntity cable)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		cable.setGrounded(!cable.grounded());
		message(player, cable.grounded() ? "Grounding set: grounds applied, A, B and C to ground" : "Grounding set: grounds removed");
		return InteractionResult.SUCCESS;
	}

	private static void message(Player player, String text) {
		if (player != null) {
			player.sendOverlayMessage(Component.literal(text));
		}
	}
}
