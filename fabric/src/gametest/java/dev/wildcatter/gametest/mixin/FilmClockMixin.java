package dev.wildcatter.gametest.mixin;

import dev.wildcatter.gametest.Filming;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The film clock (film60, 2026-11-27): while a 60 fps take films, the game's frame timer counts game time in frames instead of the
 * wall clock. {@code advanceGameTime} is called once at the start of every frame the client draws (Minecraft.runTick); here it first
 * hands the frame just drawn to the recorder, then advances the game a third of a tick: the frames are drawn at partial ticks 0, 1/3
 * and 2/3, and every third frame is a game tick (which the client game test then waits on as ever). So 60 frames are drawn and filmed
 * for every 20 ticks, however long the Mac takes to draw them. Outside a take the timer is the game's own.
 */
@Mixin(DeltaTracker.Timer.class)
public abstract class FilmClockMixin implements Filming.Clock {
	@Shadow
	private float deltaTicks;
	@Shadow
	private float deltaTickResidual;
	@Shadow
	private long lastMs;
	/** Frames drawn since the last game tick (0 on the tick's own frame). */
	@Unique
	private int filmFrame;

	@Inject(method = "advanceGameTime", at = @At("HEAD"), cancellable = true)
	private void wildcatterFilmClock(long currentMs, CallbackInfoReturnable<Integer> cir) {
		int frames = Filming.frameDrawn(this);
		if (frames > 0) {
			lastMs = currentMs;   // the game's own clock picks up from now when the take ends
			filmFrame = (filmFrame + 1) % frames;
			deltaTicks = 1.0F / frames;
			deltaTickResidual = filmFrame / (float) frames;
			cir.setReturnValue(filmFrame == 0 ? 1 : 0);
		}
	}

	@Override
	public void wildcatterFilmStart() {
		filmFrame = 0;   // the frame now being drawn (a tick's, the take started in the test's tasks) is drawn at partial tick 0
		deltaTickResidual = 0.0F;
	}
}
