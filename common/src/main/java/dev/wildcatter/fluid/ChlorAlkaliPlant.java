package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The full-size chlor-alkali plant's numbers (docs/multiblocks/chlor-alkali-plant.md), from Faraday and the zirconium spec's ratios:
 * 52 membrane elements at 15 kA and 3.1 V (161 V DC, 2.4 MW), about a tonne of chlorine an hour. Plain Java, so the unit test reads it.
 */
public final class ChlorAlkaliPlant {
	private ChlorAlkaliPlant() {
	}

	/** kg of chlorine an element makes a kA·h at the plant's current efficiency: 1.27. */
	public static double cl2PerKah() {
		return Tuning.CL2_KG_PER_KAH * Tuning.CA_CURRENT_EFFICIENCY;
	}

	/** kg/s of chlorine at the rated current: about 0.275 (990 kg/h). */
	public static double cl2Kgs() {
		return Tuning.CAP_ELEMENTS * Tuning.CAP_ELEMENT_KA * cl2PerKah() / 3600;
	}

	/** The cell room's DC volts at its rating: 161. */
	public static double dcVolts() {
		return Tuning.CAP_ELEMENTS * Tuning.CAP_ELEMENT_VOLTS;
	}

	/** Its DC watts at the rating: about 2.4 MW. */
	public static double dcWatts() {
		return dcVolts() * Tuning.CAP_ELEMENT_KA * 1000;
	}

	/** kWh of DC a kg of chlorine: about 2.44. */
	public static double kwhPerCl2() {
		return dcWatts() / 1000 / (cl2Kgs() * 3600);
	}

	/** kg of salt a kg of chlorine: 1.65. */
	public static double saltPerCl2() {
		return Tuning.CA_SALT_PER_CL2;
	}

	/** kg of 32 % caustic a kg of chlorine: 3.53. */
	public static double caustic32PerCl2() {
		return Tuning.NAOH_PER_CL2 / Tuning.CAUSTIC_STRENGTH;
	}

	/** Nm3 of hydrogen a kg of chlorine: 0.316 (315 Nm3/h at the rating). */
	public static double hydrogenM3PerCl2() {
		return Zirconium.hydrogenM3PerCl2();
	}

	/** kg of brine a kg of salt in it: 3.85 at 26 %. */
	public static double brinePerSalt() {
		return 1 / Tuning.BRINE_NACL;
	}

	/** Litres of water a kg of salt carries as saturated brine: 2.85. */
	public static double waterPerSaltInBrine() {
		return brinePerSalt() - 1;
	}

	/** kg of CO2 a kg of chlorine for the brine's calcium: about 0.0053 (5.2 kg/h). */
	public static double co2PerCl2() {
		return saltPerCl2() * Tuning.BRINE_SALT_CA_CO2;
	}

	/** kg of spent 75 % acid a kg of chlorine dried, from 98 % acid: 0.0196. */
	public static double spentAcidPerCl2() {
		return Tuning.DRYING_ACID_PER_CL2 * 0.98 / 0.75;
	}

	/** kg of 50 % caustic a kg of 32 % caustic: 0.64. */
	public static double caustic50Per32() {
		return Tuning.CAUSTIC_STRENGTH / Tuning.CAUSTIC_50_STRENGTH;
	}

	/** kg of process steam a kg of 32 % caustic concentrated to 50 %: 0.224. */
	public static double steamPer32() {
		return Tuning.CAUSTIC_STRENGTH * Tuning.CAUSTIC_EVAPORATOR_STEAM;
	}

	/** kg of chlorine a kg of HCl: 70.9 / 72.9. */
	public static double cl2PerHcl() {
		return 70.906 / (2 * 36.461);
	}

	/** Nm3 of hydrogen a kg of HCl: 0.31. */
	public static double h2M3PerHcl() {
		return 2.016 / (2 * 36.461) / Tuning.H2_KG_PER_M3;
	}

	/** Litres of water absorbing a kg of HCl into 32 % acid: 2.125. */
	public static double waterPerHcl() {
		return 1 / Tuning.HCL_STRENGTH - 1;
	}

	/** kg/s of HCl at the burner's rating. */
	public static double hclKgs() {
		return Tuning.HCL_MAX_KGH / 3600;
	}

	/** The voltage an element has crept after {@code seconds} on calcium-bearing brine, capped. */
	public static double creep(double seconds) {
		return Math.min(Tuning.CAP_CREEP_MAX_VOLTS, Tuning.CAP_CREEP_VOLTS_PER_DAY * seconds / 86_400.0);
	}
}
