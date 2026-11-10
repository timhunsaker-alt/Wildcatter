package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;
import dev.wildcatter.ore.Ore;

/**
 * The rates of Phase 18 (docs/multiblocks/fertilizer.md), all sized from {@link Tuning#P2O5_TPD} and
 * {@link Tuning#MOP_TPH}:
 * <ul>
 * <li>the attack: Ca5(PO4)3F + 5 H2SO4 + 10 H2O -> 3 H3PO4 + 5 CaSO4.2H2O + HF, at the spec's 2.7 t of acid and 4.5 t of gypsum a
 * tonne of P2O5;</li>
 * <li>the evaporator: filter acid at 28 % P2O5 to merchant acid at 54 %;</li>
 * <li>TSP from merchant acid and rock (Ca(H2PO4)2.H2O, 0-46-0); DAP from merchant acid and ammonia ((NH4)2HPO4, 18-46-0);</li>
 * <li>potash: KCl dissolved hot off the halite and crystallized under vacuum, 95 % KCl;</li>
 * </ul>
 * Masses in kg, rates a second unless named. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Phosphate {
	private Phosphate() {
	}

	public static final double NH3 = 17.031, N = 14.007;

	// ---- phosphoric acid ----------------------------------------------------------------------------------------------------------
	/** kg/s of P2O5 into the filter acid at the rating: about 0.23. */
	public static double p2o5Kgs() {
		return Tuning.P2O5_TPD * 1000 / 86400;
	}

	/** kg of P2O5 a kg of rock: 80 % francolite at 37 %, about 0.30. */
	public static double rockP2o5() {
		return Ore.PHOSPHATE.grade * Tuning.FRANCOLITE_P2O5;
	}

	/** kg/s of rock the reactor attacks at its rating: about 0.82 (70 t a day). */
	public static double reactorRockKgs() {
		return p2o5Kgs() / (rockP2o5() * Tuning.PHOSPHORIC_RECOVERY);
	}

	/** kg of H2SO4 a kg of rock. */
	public static double acidPerRock() {
		return Tuning.H2SO4_PER_P2O5 * rockP2o5() * Tuning.PHOSPHORIC_RECOVERY;
	}

	/** L/s of 98.5 % sulfuric acid at the rating: about 0.34 (54 t a day). */
	public static double sulfuricLps() {
		return reactorRockKgs() * acidPerRock() / Tuning.ACID_KG_PER_L;
	}

	/** kg of dry phosphogypsum a kg of rock. */
	public static double gypsumPerRock() {
		return Tuning.GYPSUM_PER_P2O5 * rockP2o5() * Tuning.PHOSPHORIC_RECOVERY;
	}

	/** kg of P2O5 lost to the gypsum a kg of rock. */
	public static double p2o5LostPerRock() {
		return rockP2o5() * (1 - Tuning.PHOSPHORIC_RECOVERY);
	}

	/** kg of filter acid (28 % P2O5) a kg of rock. */
	public static double filterAcidPerRock() {
		return rockP2o5() * Tuning.PHOSPHORIC_RECOVERY / Tuning.FILTER_ACID_P2O5;
	}

	/** L/s of filter acid at the rating: about 0.64. */
	public static double filterAcidLps() {
		return reactorRockKgs() * filterAcidPerRock() / Tuning.FILTER_ACID_KG_PER_L;
	}

	/** kg of tails slurry the filter sends to the cell a kg of dry gypsum, at the cell's solids. */
	public static double tailsPerGypsum() {
		return 1 / Tuning.CELL_SLURRY_SOLIDS;
	}

	/** kg of water the filter repulps a kg of gypsum with. */
	public static double filterWaterPerGypsum() {
		return tailsPerGypsum() - 1;
	}

	/** kg of rock slurry a kg of rock, and so of water a kg of rock in the mill. */
	public static double slurryPerRock() {
		return 1 / Tuning.ROCK_SLURRY_SOLIDS;
	}

	public static double millWaterPerRock() {
		return slurryPerRock() - 1;
	}

	// ---- the evaporator -----------------------------------------------------------------------------------------------------------
	/** kg of merchant acid (54 % P2O5) a kg of filter acid. */
	public static double merchantPerFilterAcid() {
		return Tuning.FILTER_ACID_P2O5 / Tuning.MERCHANT_ACID_P2O5;
	}

	/** L/s of merchant acid at the rating: about 0.26. */
	public static double merchantLps() {
		return p2o5Kgs() / Tuning.MERCHANT_ACID_P2O5 / Tuning.MERCHANT_ACID_KG_PER_L;
	}

	/** kg of process steam a kg of filter acid. */
	public static double steamPerFilterAcid() {
		return Tuning.EVAPORATOR_STEAM_PER_P2O5 * Tuning.FILTER_ACID_P2O5;
	}

	/** kg of fluorine off the evaporator a kg of filter acid. */
	public static double fluorinePerFilterAcid() {
		return Ore.PHOSPHATE.grade * Tuning.ROCK_FLUORINE * Tuning.EVAPORATOR_F_SHARE / filterAcidPerRock();
	}

	// ---- granules -----------------------------------------------------------------------------------------------------------------
	/** kg/s of TSP from all the merchant acid: about 0.72 (2.6 t/h). */
	public static double tspKgs() {
		return p2o5Kgs() / Tuning.TSP_ACID_P2O5 / Tuning.TSP_P2O5;
	}

	/** kg of merchant acid a kg of TSP. */
	public static double merchantPerTsp() {
		return Tuning.TSP_P2O5 * Tuning.TSP_ACID_P2O5 / Tuning.MERCHANT_ACID_P2O5;
	}

	/** kg of rock (as rock, in its slurry) a kg of TSP. */
	public static double rockPerTsp() {
		return Tuning.TSP_P2O5 * (1 - Tuning.TSP_ACID_P2O5) / rockP2o5();
	}

	/** kg/s of rock the mill grinds at its rating: the reactor's and the granulator's on TSP, about 1.16 (100 t a day). */
	public static double millRockKgs() {
		return reactorRockKgs() + tspKgs() * rockPerTsp();
	}

	/** kg of merchant acid a kg of DAP. */
	public static double merchantPerDap() {
		return Tuning.DAP_P2O5 / Tuning.MERCHANT_ACID_P2O5;
	}

	/** kg of ammonia a kg of DAP: about 0.22. */
	public static double ammoniaPerDap() {
		return Tuning.DAP_N * NH3 / N;
	}

	/** kg/s of DAP from all the merchant acid, given the ammonia: about 0.50. */
	public static double dapKgs() {
		return p2o5Kgs() / Tuning.DAP_P2O5;
	}

	/** kg/h of DAP the ammonia plant's whole output makes: about 69. */
	public static double dapFromAmmoniaKgh() {
		return AmmoniaPlant.ammoniaKgh() / ammoniaPerDap();
	}

	/** J of gas the granulator's dryer burns a kg of granules. */
	public static double granulatorJPerKg() {
		return Tuning.GRANULATION_DRYER_WATTS / tspKgs();
	}

	// ---- potash -------------------------------------------------------------------------------------------------------------------
	/** kg/s of MOP at the rating. */
	public static double mopKgs() {
		return Tuning.MOP_TPH * 1000 / 3600;
	}

	/** kg of potash ore (sylvinite) a kg of MOP: about 1.25. */
	public static double orePerMop() {
		return Tuning.MOP_KCL / (Tuning.POTASH_RECOVERY * Ore.SYLVITE.grade);
	}

	/** kg of dry salt tails a kg of MOP: the ore less the product. */
	public static double saltPerMop() {
		return orePerMop() - 1;
	}

	/** kg of make-up water a kg of MOP: what the salt tails carry away at the cell's solids. */
	public static double potashWaterPerMop() {
		return saltPerMop() * (1 / Tuning.CELL_SLURRY_SOLIDS - 1);
	}

	/** J of gas the dryer burns a kg of MOP. */
	public static double dryerJPerKg() {
		return Tuning.POTASH_DRYER_GAS_WATTS / mopKgs();
	}

}
