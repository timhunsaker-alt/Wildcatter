package dev.wildcatter.client;

import dev.wildcatter.kinetic.Sounding;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A machine's running loop, bound to its block entity: pitch and volume follow the machine every tick, and it fades out when the
 * machine stops, changes its sound, or is gone. One loop per machine.
 */
public class MachineSound extends AbstractTickableSoundInstance {
	private static final Map<BlockPos, MachineSound> PLAYING = new HashMap<>();
	private static final float FADE_PER_TICK = 0.05F;

	private final BlockEntity machine;
	private final SoundEvent event;
	private float fade = 0;
	private float loudness = 0;
	private float tone = 1;

	private MachineSound(BlockEntity machine, SoundEvent event) {
		super(event, SoundSource.BLOCKS, RandomSource.create());
		this.machine = machine;
		this.event = event;
		this.looping = true;
		this.delay = 0;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		BlockPos pos = machine.getBlockPos();
		this.x = pos.getX() + 0.5;
		this.y = pos.getY() + 0.5;
		this.z = pos.getZ() + 0.5;
		this.volume = 0.01F;
	}

	/** Called from a sounding machine's client tick: starts its loop if it should be playing and is not. */
	public static void start(BlockEntity machine) {
		if (!(machine instanceof Sounding sounding) || sounding.loop() == null) {
			return;
		}
		MachineSound current = PLAYING.get(machine.getBlockPos());
		if (current != null && !current.isStopped() && current.event == sounding.loop()) {
			return;
		}
		MachineSound sound = new MachineSound(machine, sounding.loop());
		PLAYING.put(machine.getBlockPos().immutable(), sound);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	@Override
	public void tick() {
		boolean on = !machine.isRemoved() && machine instanceof Sounding sounding && sounding.loop() == event && sounding.soundVolume() > 0;
		fade = on ? Math.min(1, fade + FADE_PER_TICK) : fade - FADE_PER_TICK;
		if (fade <= 0) {
			PLAYING.remove(machine.getBlockPos(), this);
			stop();
			return;
		}
		if (on) {   // while fading out, keep the last loudness and let the pitch sag as the machine runs down
			Sounding sounding = (Sounding) machine;
			loudness = sounding.soundVolume();
			tone = sounding.soundPitch();
		} else {
			tone *= 0.97F;
		}
		volume = Math.max(0.01F, loudness * fade);
		pitch = Math.max(0.5F, Math.min(2.0F, tone));
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
