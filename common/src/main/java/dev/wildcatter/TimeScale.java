package dev.wildcatter;

/**
 * The world's time scale (Tim, 2026-11-10; docs/time-scale.md): one of five presets, 1:1 to 50:1, that shortens the long waits and
 * nothing else. At 10:1 a wait of 10 hours in the real plant takes one hour of play: the reactor's real heat-up and cooldown
 * ({@code reactorRealHeatup}), the residence holds ({@code realResidenceTimes}), dry cask cooling, and other mods' waits that read it
 * (Tailings' paste cure). Rates, fuel, motion and everything electrical stay real at every preset.
 * <p>
 * Saved with the world rules ({@link RulesData}); an existing world loads at 1:1. Held here, static, for the running server, as
 * {@link Rules} is. Plain Java.
 */
public final class TimeScale {
	private TimeScale() {
	}

	/** The presets, and the only values the setting takes. */
	public static final int[] PRESETS = {1, 5, 10, 20, 50};

	private static volatile int scale = 1;
	private static volatile java.util.function.IntConsumer listener;

	/** The scale now: 1, 5, 10, 20 or 50. */
	public static int scale() {
		return scale;
	}

	/** A real wait of this many seconds, at the world's scale. */
	public static double scaled(double seconds) {
		return seconds / scale;
	}

	public static boolean valid(int value) {
		for (int p : PRESETS) {
			if (p == value) {
				return true;
			}
		}
		return false;
	}

	/** The preset itself, or 1 for anything else (a hand-edited save). */
	public static int preset(int value) {
		return valid(value) ? value : 1;
	}

	/** Sets the scale for the running world (and its saved copy, if one is loaded). Refuses anything but a preset. */
	public static void set(int value) {
		if (!valid(value)) {
			throw new IllegalArgumentException("time scale " + value + " is not one of 1, 5, 10, 20, 50");
		}
		scale = value;
		java.util.function.IntConsumer l = listener;
		if (l != null) {
			l.accept(value);
		}
	}

	static void listen(java.util.function.IntConsumer l) {
		listener = l;
	}

	/** Back to 1:1 with no world, as {@link Rules#clear()}. */
	static void clear() {
		listener = null;
		scale = 1;
	}

	public static String label(int value) {
		return value + ":1";
	}

	/** For a readout: ", 22 min at 50:1" after a real time, or nothing at 1:1. A % in it is safe in a format string. */
	public static String note(double realSeconds) {
		if (scale == 1) {
			return "";
		}
		double s = scaled(realSeconds);
		String time = s >= 5400 ? String.format("%.1f h", s / 3600) : s >= 120 ? Math.round(s / 60) + " min" : Math.round(s) + " s";
		return ", " + time + " at " + label(scale);
	}
}
