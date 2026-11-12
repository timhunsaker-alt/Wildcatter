package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * 19d: the control rods' materials (docs/multiblocks/reactor-extensions.md items 9 and 10). A B4C RCCA burns its boron-10 while it is
 * in the flux (B-10 + n -> Li-7 + He-4): at 30 % at its tip it reads REPLACE, and past 40 % its worth falls in a straight line to 70 % at
 * 60 %. A hafnium RCCA is worth 85 % of a B4C one and takes no helium. Plain Java.
 */
public final class ControlRods {
	private ControlRods() {
	}

	public static final int B4C = 0, HAFNIUM = 1;

	/** Its worth as a share of a fresh B4C RCCA's. {@code wear}: the rule reactorRodWear (without it B-10 burnup changes nothing). */
	public static double worthShare(int kind, double b10, boolean wear) {
		if (kind == HAFNIUM) {
			return Tuning.HF_RCCA_WORTH;
		}
		if (!wear || b10 <= Tuning.B10_WORTH_FALL_FROM) {
			return 1;
		}
		double f = Math.min(1, (b10 - Tuning.B10_WORTH_FALL_FROM) / (Tuning.B10_WORTH_FALL_TO - Tuning.B10_WORTH_FALL_FROM));
		return 1 - f * (1 - Tuning.B10_WORTH_FLOOR);
	}

	/** B-10 burnup a second, fully inserted at full power, at the reactor's 100x burnup. */
	public static double b10PerSecond() {
		return Tuning.B10_REPLACE / (Tuning.B10_REPLACE_EFPY * 365.25 * 86400) * Tuning.BURNUP_SPEEDUP;
	}

	/** Hours of world time fully inserted at full power until a fresh B4C RCCA reads REPLACE: about 438. */
	public static double hoursToReplace() {
		return Tuning.B10_REPLACE / b10PerSecond() / 3600;
	}

	public static boolean replace(int kind, double b10) {
		return kind == B4C && b10 >= Tuning.B10_REPLACE;
	}

	public static String name(int kind) {
		return kind == HAFNIUM ? "Hf" : "B4C";
	}
}
