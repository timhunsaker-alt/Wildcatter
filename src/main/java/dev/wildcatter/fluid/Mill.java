package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The mass balance of the acid-leach uranium mill (docs/multiblocks/uranium-mill.md). Masses in kg, liquors in litres (taken as
 * 1 kg/L). Plain Java, so it is unit-tested without Minecraft.
 */
public final class Mill {
	private Mill() {
	}

	/** kg of ore a second the SAG mill grinds, for ore of this U3O8 grade: its rating, or less so the feed never passes SX's limit. */
	public static double oreKgs(double grade) {
		return grade <= 0 ? Tuning.MILL_ORE_KGS : Math.min(Tuning.MILL_ORE_KGS, Tuning.MILL_U3O8_KGS / grade);
	}

	/** Litres of water a kg of ore takes to make slurry at {@link Tuning#MILL_SLURRY_SOLIDS}. */
	public static double grindWaterPerOre() {
		return (1 - Tuning.MILL_SLURRY_SOLIDS) / Tuning.MILL_SLURRY_SOLIDS;
	}

	/** kg of 98.5 % acid a kg of ore. */
	public static double acidPerOre() {
		return Tuning.LEACH_ACID_KG_PER_T / 1000 / Tuning.ACID_STRENGTH;
	}

	public static double mno2PerOre() {
		return Tuning.LEACH_MNO2_KG_PER_T / 1000;
	}

	/** Litres of wash water a kg of solids. */
	public static double washPerSolids() {
		return Tuning.CCD_WASH_RATIO;
	}

	/** Litres of liquor that leave with a kg of solids in the thickener underflow. */
	public static double underflowLiquorPerSolids() {
		return (1 - Tuning.CCD_UNDERFLOW_SOLIDS) / Tuning.CCD_UNDERFLOW_SOLIDS;
	}

	/** Litres of pregnant liquor from a kg of leach slurry that is {@code solids} solids by weight. */
	public static double liquorPerSlurry(double solids) {
		return Math.max(0, 1 - solids + solids * (washPerSolids() - underflowLiquorPerSolids()));
	}

	/** Litres of strip liquor for a kg of U3O8. */
	public static double stripPerU3o8() {
		return 1000 / Tuning.SX_STRIP_U3O8_G_PER_L;
	}

	/** kg of rock salt made up a kg of U3O8 stripped. */
	public static double saltPerU3o8() {
		return stripPerU3o8() * Tuning.SX_NACL_G_PER_L / 1000 * Tuning.SX_NACL_MAKEUP;
	}

	/** kg of yellowcake (concentrate at {@link Tuning#YELLOWCAKE_U3O8}) from a kg of U3O8. */
	public static double yellowcakePerU3o8() {
		return 1 / Tuning.YELLOWCAKE_U3O8;
	}

	/** kg of U3O8 in a full drum. */
	public static double u3o8PerDrum() {
		return Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8;
	}
}
