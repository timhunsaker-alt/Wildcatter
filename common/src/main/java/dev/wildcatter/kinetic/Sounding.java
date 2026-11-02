package dev.wildcatter.kinetic;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A machine with a running sound (docs/kinetics-and-sound.md): a loop whose pitch follows its speed and whose volume follows its
 * load. The client starts the loop when the machine runs and lets it fade when it stops; the block entity only says what it sounds like.
 */
public interface Sounding {
	/** The loop, or null when the machine is silent. */
	SoundEvent loop();

	/** 1.0 at rated speed. */
	float soundPitch();

	/** 0 to 1. */
	float soundVolume();

	/** Set by the client: starts a machine's loop if it is not already playing. Does nothing on a dedicated server. */
	final class Hook {
		public static Consumer<BlockEntity> start = be -> {
		};

		private Hook() {
		}
	}
}
