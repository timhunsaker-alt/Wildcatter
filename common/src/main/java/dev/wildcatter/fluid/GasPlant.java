package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * What a gas plant's parts make (docs/multiblocks/gas-plant.md): the dehydrator's fuel, the chiller's residue gas and NGL, the
 * stabilizer's split. Plain Java, so it is unit-tested without Minecraft.
 */
public final class GasPlant {
	private GasPlant() {
	}

	/** Dried gas out of a dehydrator for {@code m3} of compressed gas in: the reboiler burns its share. */
	public static double dried(double m3) {
		return m3 * (1 - Tuning.GLYCOL_FUEL);
	}

	/** Pipeline gas (m3) out of a chiller for {@code m3} of dried gas in. */
	public static double residue(double m3) {
		return m3 * Tuning.CHILLER_RESIDUE;
	}

	/** Y-grade (litres) out of a chiller for {@code m3} of dried gas in. */
	public static double ngl(double m3) {
		return m3 * Tuning.CHILLER_NGL_PER_M3;
	}

	/** LPG (litres) out of a stabilizer for {@code litres} of Y-grade in. */
	public static double lpg(double litres) {
		return litres * Tuning.STABILIZER_LPG;
	}

	/** Natural gasoline (litres) out of a stabilizer for {@code litres} of Y-grade in. */
	public static double gasoline(double litres) {
		return litres - lpg(litres);
	}
}
