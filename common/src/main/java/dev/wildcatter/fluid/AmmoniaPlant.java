package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The rates of the ammonia plant (docs/multiblocks/ammonia-plant.md), all sized to one steam reformer's hydrogen:
 * <ul>
 * <li>synthesis: N2 + 3 H2 -> 2 NH3;</li>
 * <li>the nitric acid unit: 4 NH3 + 5 O2 -> 4 NO + 6 H2O on the gauze, then NO to NO2 and 3 NO2 + H2O -> 2 HNO3 + NO in the
 * absorption tower, overall NH3 + 2 O2 -> HNO3 + H2O at {@link Tuning#NITRIC_CONVERSION}; what is not converted burns to N2;</li>
 * <li>the neutraliser: NH3 + HNO3 -> NH4NO3;</li>
 * <li>the blender: AN, water, diesel and emulsifier to the emulsion matrix.</li>
 * </ul>
 * Masses in kg, gases in Nm3 (0 C, 1 atm, the ideal 22.414 m3 a kmol). Plain Java, so it is unit-tested without Minecraft.
 */
public final class AmmoniaPlant {
	private AmmoniaPlant() {
	}

	public static final double NH3 = 17.031, HNO3 = 63.013, AN = 80.043, H2O = 18.015, NM3_PER_KMOL = 22.414;

	/** Nm3 of hydrogen and of nitrogen a Nm3 of ammonia. */
	public static final double H2_PER_NH3 = 1.5, N2_PER_NH3 = 0.5;

	/** kg of ammonia a Nm3 of it. */
	public static double ammoniaKgPerNm3() {
		return NH3 / NM3_PER_KMOL;
	}

	/** Nm3/h of ammonia from one reformer's hydrogen. */
	public static double ammoniaNm3h() {
		return Tuning.SMR_M3H / H2_PER_NH3;
	}

	/** kg/h of ammonia from one reformer's hydrogen: about 15.2. */
	public static double ammoniaKgh() {
		return ammoniaNm3h() * ammoniaKgPerNm3();
	}

	/** Nm3/h of nitrogen the converter takes at full rate: the generator's 10. */
	public static double nitrogenNm3h() {
		return ammoniaNm3h() * N2_PER_NH3;
	}

	/**
	 * The share of the ammonia burned to acid so that the acid made neutralises the rest: x conversion = 1 - x, about 52 %.
	 */
	public static double nitricShare() {
		return 1 / (1 + Tuning.NITRIC_CONVERSION);
	}

	/** kg/h of ammonia the nitric acid unit burns at its rating. */
	public static double nitricAmmoniaKgh() {
		return ammoniaKgh() * nitricShare();
	}

	/** kg of HNO3 (as 100 %) a kg of ammonia burned. */
	public static double hno3PerAmmonia() {
		return Tuning.NITRIC_CONVERSION * HNO3 / NH3;
	}

	/** kg/h of HNO3 (as 100 %) at the unit's rating. */
	public static double hno3Kgh() {
		return nitricAmmoniaKgh() * hno3PerAmmonia();
	}

	/** kg/h of 60 % acid, and litres. */
	public static double acidKgh() {
		return hno3Kgh() / Tuning.NITRIC_STRENGTH;
	}

	public static double acidLph() {
		return acidKgh() / Tuning.NITRIC_KG_PER_L;
	}

	/**
	 * kg of process water the absorption tower takes a kg of HNO3: the acid's water, less what the burning makes (one H2O a HNO3 net,
	 * and 1.5 a NH3 for the ammonia that burns to nitrogen).
	 */
	public static double processWaterPerHno3() {
		double inAcid = (1 - Tuning.NITRIC_STRENGTH) / Tuning.NITRIC_STRENGTH;
		double made = H2O / HNO3 + (1 - Tuning.NITRIC_CONVERSION) / Tuning.NITRIC_CONVERSION * 1.5 * H2O / HNO3;
		return inAcid - made;
	}

	/** g/h of platinum-rhodium the gauze loses at the unit's rating. */
	public static double gauzeLossGh() {
		return hno3Kgh() / 1000 * Tuning.GAUZE_LOSS_G_PER_T;
	}

	/** kg of ammonia the neutraliser takes a kg of HNO3. */
	public static double ammoniaPerHno3() {
		return NH3 / HNO3;
	}

	/** kg of AN a kg of HNO3. */
	public static double anPerHno3() {
		return AN / HNO3;
	}

	/** kg/h of ammonium nitrate (as 100 %) from the whole plant: about 34.6. */
	public static double anKgh() {
		return hno3Kgh() * anPerHno3();
	}

	/** kg/h of the 85 % solution. */
	public static double anSolutionKgh() {
		return anKgh() / Tuning.AN_STRENGTH;
	}

	/** kg of water the neutraliser boils off a kg of HNO3, by its own heat of reaction: the acid's water less the solution's. */
	public static double evaporatedPerHno3() {
		double solutionWater = anPerHno3() * (1 - Tuning.AN_STRENGTH) / Tuning.AN_STRENGTH;
		return (1 - Tuning.NITRIC_STRENGTH) / Tuning.NITRIC_STRENGTH - solutionWater;
	}

	/** kg of emulsion matrix a kg of AN (as 100 %). */
	public static double matrixPerAn() {
		return 1 / Tuning.MATRIX_AN;
	}

	/** kg/h of emulsion matrix from the whole plant: about 46. */
	public static double matrixKgh() {
		return anKgh() * matrixPerAn();
	}

	/** kg of AN solution a kg of matrix. */
	public static double solutionPerMatrix() {
		return Tuning.MATRIX_AN / Tuning.AN_STRENGTH;
	}

	/** Litres of water the blender adds a kg of matrix: the matrix's water less what the solution brings. */
	public static double blenderWaterPerMatrix() {
		return Tuning.MATRIX_WATER - solutionPerMatrix() * (1 - Tuning.AN_STRENGTH);
	}

	/** Litres of diesel a kg of matrix. */
	public static double dieselLPerMatrix() {
		return Tuning.MATRIX_DIESEL / Tuning.DIESEL_KG_PER_L;
	}

	/** Hours a pail of emulsifier lasts at the plant's rate. */
	public static double hoursPerPail() {
		return Tuning.EMULSIFIER_PAIL_KG / (matrixKgh() * Tuning.MATRIX_EMULSIFIER);
	}

	/** Tonnes of rock a day the plant's matrix blasts at the open pit's powder factor. */
	public static double rockTonnesPerDay() {
		return matrixKgh() * 24 / Tuning.BLAST_POWDER_FACTOR_KG_PER_T;
	}

	/** Hours a gauze pack lasts until it is worn. */
	public static double gauzeHours() {
		return Tuning.GAUZE_PT_G * Tuning.GAUZE_WORN_SHARE / gauzeLossGh();
	}
}
