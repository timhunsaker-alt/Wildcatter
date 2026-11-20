package dev.wildcatter.well;

import dev.wildcatter.Tuning;

/**
 * A salt cavern's gas storage arithmetic (docs/multiblocks/oilfield-follow-ons.md, 23c). The pressure window is the real one, 0.3 to
 * 0.85 psi/ft at the casing shoe, taken as if the cavern were at a real storage depth of 1,000 m (question 1): about 68 to 193 bar. Gas in
 * place by the real gas law with z of 0.9 at 45 C, in standard m3 (15 C, 1 atm). Plain Java ({@code CavernTest}).
 */
public final class Cavern {
	private Cavern() {
	}

	public static double minBar() {
		return Tuning.CAVERN_MIN_PSI_FT * Tuning.CAVERN_SHOE_FT * Tuning.BAR_PER_PSI;
	}

	public static double maxBar() {
		return Tuning.CAVERN_MAX_PSI_FT * Tuning.CAVERN_SHOE_FT * Tuning.BAR_PER_PSI;
	}

	/** Standard m3 of gas a m3 of cavern holds at {@code bar}. */
	public static double sm3PerM3(double bar) {
		return bar / Tuning.STD_BAR * Tuning.STD_TEMP_K / Tuning.CAVERN_TEMP_K / Tuning.CAVERN_Z;
	}

	/** The pressure, bar, of {@code sm3} of gas in {@code m3} of cavern. */
	public static double bar(double sm3, double m3) {
		return m3 <= 0 ? 0 : sm3 / m3 * Tuning.STD_BAR * Tuning.CAVERN_TEMP_K / Tuning.STD_TEMP_K * Tuning.CAVERN_Z;
	}

	/** Cushion gas: what holds the minimum pressure, Sm3. */
	public static double cushion(double m3) {
		return sm3PerM3(minBar()) * m3;
	}

	/** The most the cavern holds, Sm3. */
	public static double capacity(double m3) {
		return sm3PerM3(maxBar()) * m3;
	}

	/** Working gas: what can come out above the cushion, Sm3. */
	public static double working(double sm3, double m3) {
		return Math.max(0, sm3 - cushion(m3));
	}

	/** The withdrawal rate's share of full: in proportion to the pressure above the minimum, full above half the window. */
	public static double withdrawShare(double sm3, double m3) {
		double window = maxBar() - minBar();
		return Math.max(0, Math.min(1, (bar(sm3, m3) - minBar()) / (window / 2)));
	}

	/** GJ of a standard m3 of pipeline gas (about 38 MJ, the sales line's 1,020 Btu/scf). */
	public static double gj(double sm3) {
		return sm3 * 0.038;
	}
}
