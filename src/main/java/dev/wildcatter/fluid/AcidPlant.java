package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The chemistry of a sulfur-burning contact acid plant (docs/multiblocks/acid-plant.md): S + O2 -> SO2, 2 SO2 + O2 -> 2 SO3,
 * SO3 + H2O -> H2SO4. Masses in kg. Plain Java, so it is unit-tested without Minecraft.
 */
public final class AcidPlant {
	private AcidPlant() {
	}

	public static final double S = 32.06, SO2 = 64.06, SO3 = 80.06, H2SO4 = 98.08, H2O = 18.02, AIR = 28.97;

	/** kg of H2SO4 a second at the plant's rating. */
	public static double ratedAcidKgs() {
		return Tuning.ACID_PLANT_TPD * 1000 / 86_400;
	}

	/** kg of sulfur a second at the plant's rating, with double absorption. */
	public static double ratedSulfurKgs() {
		return ratedAcidKgs() * S / H2SO4 / Tuning.ACID_CONVERSION_DOUBLE;
	}

	/**
	 * kg of burner gas a kg of sulfur: the SO2 at {@link Tuning#BURNER_SO2_VOLUME} in what is left of the air. Each mole of SO2 takes
	 * the place of the mole of O2 it burned, so the gas has as many moles as the air did.
	 */
	public static double gasPerSulfur() {
		double airMoles = 1 / Tuning.BURNER_SO2_VOLUME;
		return (airMoles * AIR + S) / S;
	}

	/** The share of burner gas, by weight, that is SO2. */
	public static double so2InGas() {
		return SO2 / S / gasPerSulfur();
	}

	/** Normal m3 of air a kg of sulfur. */
	public static double airM3PerSulfur() {
		return 1 / Tuning.BURNER_SO2_VOLUME * 22.414 / S;
	}

	/** kg of H2SO4 from a kg of SO2 converted and absorbed. */
	public static double acidPerSo2() {
		return H2SO4 / SO2;
	}

	/** kg of product (at {@link Tuning#ACID_STRENGTH}) from a kg of H2SO4. */
	public static double productPerAcid() {
		return 1 / Tuning.ACID_STRENGTH;
	}

	/** kg of water a kg of product takes: what the SO3 combines with, and the dilution. */
	public static double waterPerProduct() {
		return 1 - Tuning.ACID_STRENGTH * SO3 / H2SO4;
	}

	public static double conversion(boolean interpass) {
		return interpass ? Tuning.ACID_CONVERSION_DOUBLE : Tuning.ACID_CONVERSION_SINGLE;
	}
}
