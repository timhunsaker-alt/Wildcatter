package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The mass balance of the anode carbon (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md): the coker's yields, the
 * calciner, the coke ovens and tar still, and the anode recipe. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Carbon {
	private Carbon() {
	}

	// ---- the coker, a kg of residue ------------------------------------------------------------------------------------------
	/** Litres of coker gas oil (as diesel) a kg of residue. */
	public static double gasOilLitres() {
		return Tuning.COKER_GAS_OIL_YIELD / Tuning.COKER_GAS_OIL_KG_PER_L;
	}

	/** Litres of coker naphtha (as gasoline) a kg of residue. */
	public static double naphthaLitres() {
		return Tuning.COKER_NAPHTHA_YIELD / Tuning.GASOLINE_KG_PER_L;
	}

	/** m3 of coker gas a kg of residue. */
	public static double cokerGas() {
		return Tuning.COKER_GAS_YIELD / Tuning.COKER_GAS_KG_PER_M3;
	}

	/** kg of calcined coke a day at the coker's rate. */
	public static double calcinedCokePerDay() {
		return Tuning.COKER_RESIDUE_KGS * 86_400 * Tuning.COKER_COKE_YIELD * Tuning.COKE_CALCINE_YIELD;
	}

	// ---- the coke ovens, a kg of coal ----------------------------------------------------------------------------------------
	/** m3 of coke oven gas a kg of coal, after the underfiring is taken back. */
	public static double netCokeOvenGas() {
		return Tuning.COKE_OVEN_GAS_PER_KG * (1 - Tuning.COKE_OVEN_UNDERFIRE);
	}

	/** kg of coal tar a m3 of raw gas: the gas carries it to the by-product plant. */
	public static double tarPerRawGas() {
		return Tuning.COKE_OVEN_TAR_PER_KG / Tuning.COKE_OVEN_GAS_PER_KG;
	}

	/** m3 of clean gas out a m3 of raw gas in. */
	public static double cleanPerRawGas() {
		return 1 - Tuning.COKE_OVEN_UNDERFIRE;
	}

	/** Litres of tar oils (as residue) a kg of tar. */
	public static double tarOilLitres() {
		return (1 - Tuning.PITCH_PER_TAR) / Tuning.TAR_OIL_KG_PER_L;
	}

	/** kg of pitch a day at the coke ovens' rate. */
	public static double pitchPerDay() {
		return Tuning.COKE_OVEN_COAL_KGS * 86_400 * Tuning.COKE_OVEN_TAR_PER_KG * Tuning.PITCH_PER_TAR;
	}

	// ---- the anodes ----------------------------------------------------------------------------------------------------------
	public static double cokePerAnode() {
		return Tuning.GREEN_ANODE_KG * Tuning.PASTE_COKE;
	}

	public static double buttsPerAnode() {
		return Tuning.GREEN_ANODE_KG * Tuning.PASTE_BUTTS;
	}

	public static double pitchPerAnode() {
		return Tuning.GREEN_ANODE_KG * Tuning.PASTE_PITCH;
	}

	/** Joules of fuel to bake one anode. */
	public static double bakingJoules() {
		return Tuning.BAKED_ANODE_KG * Tuning.BAKING_J_PER_KG;
	}

	/** kg of calcined coke a day the anode plant takes at its rate. */
	public static double anodeCokePerDay() {
		return cokePerAnode() * Tuning.ANODES_PER_SECOND * 86_400;
	}

	/** kg of pitch a day the anode plant takes at its rate. */
	public static double anodePitchPerDay() {
		return pitchPerAnode() * Tuning.ANODES_PER_SECOND * 86_400;
	}
}
