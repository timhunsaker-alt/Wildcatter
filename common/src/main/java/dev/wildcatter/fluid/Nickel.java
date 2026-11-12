package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The numbers of the nickel smelter and refinery (docs/multiblocks/nickel-smelter.md): the roaster's sulfur, the electric furnace's
 * energy, the furnace and Bessemer mattes, the converter's off-gas, the leach's chlorine and the tankhouse's Faraday. The nickel is
 * carried through the stages as kg of Ni; its copper and cobalt go with it in the feed's proportions. Masses in kg, power in watts.
 * Plain Java, so it is unit-tested without Minecraft.
 */
public final class Nickel {
	private Nickel() {
	}

	// molar masses, g/mol
	public static final double NI = 58.693, CU = 63.546, CO = 58.933, S = 32.06, CL2 = 70.906;

	// ---- the roaster -----------------------------------------------------------------------------------------------------------
	/** kg of sulfur the roaster sends to the acid plant as SO2, a kg of feed: 70 % of the feed's 30 %. */
	public static double roasterSulfurPerFeed() {
		return Tuning.NICKEL_FEED_S * Tuning.NI_ROASTER_S_TO_GAS;
	}

	/** kg of burner gas on the pipe a kg of feed, at a sulfur burner's strength so the converter makes the right acid from it. */
	public static double burnerGasPerFeed() {
		return roasterSulfurPerFeed() * AcidPlant.gasPerSulfur();
	}

	/** kg of calcine a kg of feed: 1 (proposed: the sulfur roasted off is made up by the oxygen taken on). */
	public static double calcinePerFeed() {
		return 1.0;
	}

	// ---- the electric furnace --------------------------------------------------------------------------------------------------
	/** Joules a kg of calcine: 500 kWh a tonne. */
	public static double furnaceJoulesPerKg() {
		return Tuning.NI_FURNACE_KWH_PER_T * 3.6e6 / 1000;
	}

	/** kg/h of calcine the furnace smelts at {@code watts}: 2 t/h at 1 MW. */
	public static double furnaceKgh(double watts) {
		return watts * 3600 / furnaceJoulesPerKg();
	}

	/** kg of furnace matte a kg of Ni in it. */
	public static double furnaceMattePerNi() {
		return 1 / Tuning.NI_FURNACE_MATTE_NI;
	}

	// ---- the converter ---------------------------------------------------------------------------------------------------------
	/** kg of Bessemer matte a kg of Ni in it. */
	public static double bessemerPerNi() {
		return 1 / Tuning.BESSEMER_MATTE_NI;
	}

	/** kg of sulfur the converter blows off a kg of Ni: what the roaster left in the calcine, less what stays in the Bessemer matte. */
	public static double converterSulfurPerNi() {
		return Tuning.NICKEL_FEED_S / Tuning.NICKEL_FEED_NI * (1 - Tuning.NI_ROASTER_S_TO_GAS) - Tuning.BESSEMER_MATTE_S / Tuning.BESSEMER_MATTE_NI;
	}

	/** kg of burner gas the converter sends to the acid plant a kg of Ni blown. */
	public static double converterGasPerNi() {
		return converterSulfurPerNi() * AcidPlant.gasPerSulfur();
	}

	// ---- the leach and the tankhouse -------------------------------------------------------------------------------------------
	/** kg of copper with a kg of Ni, as the feed carries it. */
	public static double copperPerNi() {
		return Tuning.NICKEL_FEED_CU / Tuning.NICKEL_FEED_NI;
	}

	/** kg of cobalt with a kg of Ni. */
	public static double cobaltPerNi() {
		return Tuning.NICKEL_FEED_CO / Tuning.NICKEL_FEED_NI;
	}

	/** kg of Cl2 the leach takes a kg of Ni dissolved: Ni, Cu and Co to their chlorides. */
	public static double leachChlorinePerNi() {
		return CL2 / NI + copperPerNi() * CL2 / CU + cobaltPerNi() * CL2 / CO;
	}

	/** kg of Cl2 the cells' anodes give back a kg of Ni won. */
	public static double anodeChlorinePerNi() {
		return CL2 / NI;
	}

	/** kg of Ni the tankhouse wins a second at {@code amps} through its cells in series. */
	public static double cathodeKgs(double amps) {
		return amps * Tuning.NI_CELLS * Tuning.NI_G_PER_AH / 1000 * Tuning.NI_CURRENT_EFFICIENCY / 3600;
	}

	/** The tankhouse's rated DC volts: 18 cells at 3.5 V. */
	public static double tankhouseVolts() {
		return Tuning.NI_CELLS * Tuning.NI_CELL_VOLTS;
	}

	/** The tankhouse's rated DC watts. */
	public static double tankhouseWatts() {
		return tankhouseVolts() * Tuning.NI_CELL_KA * 1000;
	}

	/** kg of Ni an hour the line makes at its rating: the roaster's feed at 12 %, 95 % of it won. */
	public static double lineNiKgh() {
		return Tuning.NI_ROASTER_FEED_KGH * Tuning.NICKEL_FEED_NI * Tuning.NI_RECOVERY;
	}
}
