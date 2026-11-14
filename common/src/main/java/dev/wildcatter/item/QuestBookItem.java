package dev.wildcatter.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The quest book (docs/quests.md): use it to open the quest screen. The screen is client-only; {@link #open} is set by the client side
 * ({@code WildcatterClient.init}, which each loader's client entry point calls), so this class never touches client code.
 */
public class QuestBookItem extends Item {
	/** Opens the quest screen; null on a dedicated server. */
	public static volatile Runnable open;

	public QuestBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			Runnable o = open;
			if (o != null) {
				o.run();
			}
		}
		return InteractionResult.SUCCESS;
	}
}
