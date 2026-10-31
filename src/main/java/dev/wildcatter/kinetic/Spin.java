package dev.wildcatter.kinetic;

/**
 * A shaft's speed and, on the client, how far it has turned. The server sets the real rpm; the client turns the drawn parts at
 * rpm / {@link #DISPLAY_DIVISOR} (1,800 rpm shows as 1.5 turns a second) so they read as turning rather than strobing.
 */
public final class Spin {
	/** Shown at real rpm / 20 (docs/kinetics-and-sound.md): a display choice, not physics. */
	public static final float DISPLAY_DIVISOR = 20.0F;
	/** The server re-sends the speed when it has moved this share from what the client last heard. */
	public static final float SYNC_CHANGE = 0.02F;

	private float rpm;
	private float sentRpm = Float.NaN;
	private float angle;
	private long lastNanos = -1;
	/** A part that swings to a position (a disconnect blade) rather than turning: the angle it heads for, or NaN. */
	private float target = Float.NaN;
	/** How fast a swinging part moves, degrees a second. */
	private static final float SWING_DEGREES_PER_SECOND = 90.0F;

	public float rpm() {
		return rpm;
	}

	public void setRpm(float value) {
		rpm = value;
	}

	/** Server side: true when the client should hear the new speed (it has moved enough, or stopped or started). */
	public boolean needsSync() {
		if (Float.isNaN(sentRpm) || (sentRpm == 0) != (rpm == 0)) {
			return true;
		}
		return Math.abs(rpm - sentRpm) > Math.abs(sentRpm) * SYNC_CHANGE;
	}

	public void markSynced() {
		sentRpm = rpm;
	}

	public float target() {
		return target;
	}

	/** A swinging part: head for this angle (degrees). */
	public void setTarget(float degrees) {
		target = degrees;
	}

	/** Client side: the shaft angle in degrees now, advanced by the time since the last frame. */
	public float angle(long nowNanos) {
		if (lastNanos >= 0) {
			float seconds = Math.min(0.25F, (nowNanos - lastNanos) / 1e9F);
			if (!Float.isNaN(target)) {
				float step = SWING_DEGREES_PER_SECOND * seconds;
				angle = Math.abs(target - angle) <= step ? target : angle + Math.signum(target - angle) * step;
			} else {
				angle = (angle + rpm / DISPLAY_DIVISOR * 6.0F * seconds) % 360.0F;   // rpm / 60 turns a second, 360 degrees a turn
			}
		} else if (!Float.isNaN(target)) {
			angle = target;
		}
		lastNanos = nowNanos;
		return angle;
	}
}
