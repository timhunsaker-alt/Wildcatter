package dev.wildcatter.quest;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The quest book's one public hook (docs/quests.md, "Milestones"): a machine that is working says so, and every player near it reaches
 * the milestone. Wildcatter's machines call it; add-ons (Tailings, Farmings, any mod) call the same method with their own keys.
 */
public final class QuestEvents {
	/** Players within this many blocks of the machine reach the milestone. */
	public static final double RADIUS = 64;
	/** The same position and key is counted at most once in this many ticks, so a machine may call every tick. */
	public static final long REPEAT_TICKS = 100;

	private static final Map<String, Long> LAST = new HashMap<>();

	private QuestEvents() {
	}

	/** Awards milestone {@code key} to every player within {@link #RADIUS} blocks of {@code at}. */
	public static void milestone(ServerLevel level, BlockPos at, Identifier key) {
		if (level == null || at == null || key == null) {
			return;
		}
		long now = level.getGameTime();
		String where = level.dimension().identifier() + "|" + at.asLong() + "|" + key;
		Long last = LAST.get(where);
		if (last != null && now - last < REPEAT_TICKS && now >= last) {
			return;
		}
		LAST.put(where, now);
		if (LAST.size() > 4096) {
			LAST.entrySet().removeIf(e -> now - e.getValue() >= REPEAT_TICKS);
		}
		double r2 = RADIUS * RADIUS;
		QuestData data = null;
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) > r2) {
				continue;
			}
			if (data == null) {
				data = QuestData.get(level.getServer());
			}
			if (data.of(player.getStringUUID()).milestones.add(key.toString())) {
				data.changed();
				QuestTracker.poke(player);
			}
		}
	}

	/** As {@link #milestone(ServerLevel, BlockPos, Identifier)}; does nothing on a client level. */
	public static void milestone(Level level, BlockPos at, Identifier key) {
		if (level instanceof ServerLevel server) {
			milestone(server, at, key);
		}
	}

	/** Forgets the rate limits (a new world). */
	public static void clearAll() {
		LAST.clear();
	}
}
