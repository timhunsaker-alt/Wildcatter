package dev.wildcatter.block;

import dev.wildcatter.well.CavernData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/** A sonar survey under way at a brine or storage wellhead (23c): pings for 30 s, then the outline and the cavern's record. */
final class Sonar {
	private Sonar() {
	}

	/** Each second of a survey; returns the ticks left. */
	static int tick(ServerLevel level, BlockPos head, CavernData.Hole hole, int left, @Nullable UUID surveyor, String name) {
		if (left <= 0) {
			return 0;
		}
		left -= 20;
		level.playSound(null, head, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 1.8F);
		if (left <= 0) {
			Caverns.outline(level, hole);
			ServerPlayer player = surveyor == null ? null : level.getServer().getPlayerList().getPlayer(surveyor);
			if (player != null) {
				player.sendSystemMessage(Component.literal("SONAR SURVEY (" + name + "): " + Caverns.recordText(level, hole)));
			}
		}
		return Math.max(0, left);
	}
}
