package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The lithium hydroxide plant's numbers (docs/multiblocks/lithium.md): the sulfate route from spodumene, 0.5 t of LiOH.H2O an hour. Per kg
 * of Li2O recovered: 2.81 kg of LiOH.H2O, 2.68 kg of NaOH, 4.75 kg of sodium sulfate; per kg of Li2O roasted, 3.28 kg of H2SO4 and 30 %
 * excess. Plain Java, so the unit test reads it.
 */
public final class Lithium {
	private Lithium() {
	}

	public static final double LI2O = 29.88, LIOH_H2O = 41.96, NAOH = 40.00, H2SO4 = 98.08, NA2SO4 = 142.04, LI2CO3 = 73.89, CO2 = 44.01;

	/** kg of LiOH.H2O a kg of Li2O: 2.81. */
	public static double liohPerLi2o() {
		return 2 * LIOH_H2O / LI2O;
	}

	/** kg/s of LiOH.H2O at the rating. */
	public static double liohKgs() {
		return Tuning.LIOH_TPH * 1000 / 3600;
	}

	/** kg/s of Li2O recovered at the rating, and of ore through the kilns. */
	public static double li2oKgs() {
		return liohKgs() / liohPerLi2o();
	}

	public static double oreKgs() {
		return li2oKgs() / Tuning.LITHIUM_RECOVERY / Tuning.SPODUMENE_ORE_LI2O;
	}

	/** kg of H2SO4 a kg of ore roasted: 3.28 a kg of its Li2O, and 30 % excess. */
	public static double acidPerOre() {
		return Tuning.SPODUMENE_ORE_LI2O * H2SO4 / LI2O * Tuning.ROAST_ACID_EXCESS;
	}

	/** kg of NaOH a kg of Li2O recovered: 2.68. */
	public static double naohPerLi2o() {
		return 2 * NAOH / LI2O;
	}

	/** kg of sodium sulfate a kg of Li2O recovered: 4.75. */
	public static double na2so4PerLi2o() {
		return NA2SO4 / LI2O;
	}

	/** kg of leach residue a kg of ore: the ore less the Li2O taken out. */
	public static double residuePerOre() {
		return 1 - Tuning.SPODUMENE_ORE_LI2O * Tuning.LITHIUM_RECOVERY;
	}

	/** kg of LiOH.H2O and CO2 a kg of Li2CO3: 2 LiOH.H2O + CO2 -> Li2CO3 + 3 H2O. */
	public static double liohPerLi2co3() {
		return 2 * LIOH_H2O / LI2CO3;
	}

	public static double co2PerLi2co3() {
		return CO2 / LI2CO3;
	}

	/** Watts of gas the decrepitation kiln burns at the rating: about 0.9 MW. */
	public static double decrepitationWatts() {
		return oreKgs() * Tuning.DECREPITATION_J_PER_KG;
	}
}
