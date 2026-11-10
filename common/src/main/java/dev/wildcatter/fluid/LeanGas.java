package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The carbon plants' gases in engines and turbines (docs/questions/misc-gaps.md, 5): a gas engine on coke oven gas is derated, and
 * the aeroderivative refuses a gas whose Wobbe index is far from the pipeline gas its fuel system is tuned to. Plain Java, so it is
 * unit-tested without Minecraft.
 */
public final class LeanGas {
	private LeanGas() {
	}

	/** Heating value in J/m3 of the gases an engine or turbine may burn, or 0. */
	public static double joulesPerM3(Product p) {
		if (p == null) {
			return 0;
		}
		return switch (p) {
			case GAS, PIPELINE_GAS -> Tuning.GAS_JOULES_PER_M3;
			case COKE_OVEN_GAS -> Tuning.COKE_OVEN_GAS_JOULES_PER_M3;
			case COKER_GAS -> Tuning.COKER_GAS_JOULES_PER_M3;
			case BIOGAS -> Tuning.BIOGAS_JOULES_PER_M3;
			default -> 0;
		};
	}

	/** Specific gravity against air, or 0 where the mod does not give one. */
	public static double specificGravity(Product p) {
		if (p == null) {
			return 0;
		}
		return switch (p) {
			case GAS, PIPELINE_GAS -> Tuning.PIPELINE_GAS_SG;
			case COKE_OVEN_GAS -> Tuning.COKE_OVEN_GAS_SG;
			case COKER_GAS -> Tuning.COKER_GAS_SG;
			default -> 0;
		};
	}

	/** The Wobbe index, J/m3: heating value over the square root of the specific gravity (0 when either is unknown). */
	public static double wobbe(Product p) {
		double sg = specificGravity(p);
		return sg > 0 ? joulesPerM3(p) / Math.sqrt(sg) : 0;
	}

	/** The gas's Wobbe index against pipeline gas's, as a fraction (-0.40 is 40 % below). */
	public static double wobbeShift(Product p) {
		return wobbe(p) / wobbe(Product.PIPELINE_GAS) - 1;
	}

	/** True if the aeroderivative's fuel system takes this gas: within its Wobbe band of pipeline gas. */
	public static boolean aeroTakes(Product p) {
		return wobbe(p) > 0 && Math.abs(wobbeShift(p)) <= Tuning.AERO_WOBBE_TOLERANCE;
	}

	/** A gas set's rating on this gas as a share of its natural gas rating (biogas has its own rating, docs/biogas.md). */
	public static double engineDerate(Product p) {
		return p == Product.COKE_OVEN_GAS ? Tuning.GENSET_COKE_OVEN_GAS_DERATE : 1.0;
	}
}
