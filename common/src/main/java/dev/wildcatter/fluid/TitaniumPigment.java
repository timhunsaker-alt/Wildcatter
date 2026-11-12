package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The titanium pigment plant's numbers (docs/multiblocks/titanium-pigment.md): the chloride process at 0.25 t of TiO2 an hour. Per kg of
 * TiO2 made: 2.37 kg of TiCl4 through the chlorinator, 1.78 kg of chlorine back from the oxidiser and 0.4 kg of make-up, 0.37 kg of coke,
 * 0.35 Nm3 of oxygen. Plain Java, so the unit test reads it.
 */
public final class TitaniumPigment {
	private TitaniumPigment() {
	}

	public static final double TIO2 = 79.87, TICL4 = 189.68, CL2 = 70.906;

	/** kg/s of TiO2 at the plant's rating. */
	public static double tio2Kgs() {
		return Tuning.TIO2_TPH * 1000 / 3600;
	}

	/** kg of TiCl4 a kg of TiO2: 2.37. */
	public static double ticl4PerTio2() {
		return TICL4 / TIO2;
	}

	/** kg of chlorine a kg of TiO2 the oxidiser gives back (TiCl4 + O2 -> TiO2 + 2 Cl2): 1.78. */
	public static double cl2BackPerTio2() {
		return 2 * CL2 / TIO2;
	}

	/** kg of make-up chlorine a kg of TiO2: 0.4, lost as the feed's iron, calcium and magnesium chlorides. */
	public static double cl2MakeupPerTio2() {
		return Tuning.TIO2_CL2_MAKEUP_PER_T / 1000;
	}

	/** kg of chlorine the chlorinator takes a kg of TiO2: what comes back and the make-up. */
	public static double cl2InPerTio2() {
		return cl2BackPerTio2() + cl2MakeupPerTio2();
	}

	/** kg of waste metal chlorides a kg of TiO2, counted: the make-up's chlorine as FeCl2 (56 % Cl, proposed). */
	public static double wastePerTio2() {
		return cl2MakeupPerTio2() / (CL2 / (55.845 + CL2));
	}

	/** kg of coke a kg of TiO2: 0.37. */
	public static double cokePerTio2() {
		return Tuning.TIO2_COKE_PER_T / 1000;
	}

	/** Nm3 of oxygen a kg of TiO2: 0.35 (the stoichiometric 0.28 and an excess). */
	public static double o2M3PerTio2() {
		return Tuning.TIO2_O2_NM3_PER_T / 1000;
	}

	/** The stoichiometric oxygen, for the test: 22.414 L a mole. */
	public static double o2M3PerTio2Ideal() {
		return 22.414 / TIO2;
	}

	/** Nm3/s the VPSA unit makes at its rating. */
	public static double vpsaM3s() {
		return Tuning.VPSA_O2_NM3H / 3600;
	}

	/** kg of slag (85 % TiO2) or rutile (95 %) a kg of TiO2. */
	public static double feedPerTio2(double tio2Share) {
		return 1 / tio2Share;
	}

	/** kg of alumina hydrate and kg of process steam a kg of pigment. */
	public static double hydratePerTio2() {
		return Tuning.PIGMENT_HYDRATE_PER_T / 1000;
	}

	public static double steamPerTio2() {
		return Tuning.PIGMENT_STEAM_PER_T;
	}

	/** Joules of preheater gas a kg of TiO2 at the rating. */
	public static double preheatJPerTio2() {
		return Tuning.TICL4_OXIDISER_GAS_WATTS / tio2Kgs();
	}
}
