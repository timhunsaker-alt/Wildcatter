package dev.wildcatter.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Opens like a written book. The text rides along as the book component on the item itself (see {@link Manual}),
 * which is all the client needs to show it.
 */
public class FieldManualItem extends Item {
	public FieldManualItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server) {
			server.openItemGui(player.getItemInHand(hand), hand);
		}
		return InteractionResult.SUCCESS;
	}
}
