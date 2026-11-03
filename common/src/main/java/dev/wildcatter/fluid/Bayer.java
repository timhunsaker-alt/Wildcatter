package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;
import dev.wildcatter.ore.Ore;

/**
 * The mass balance of the Bayer refinery, the lime kiln and the AlF3 reactor (docs/multiblocks/bayer-refinery.md,
 * alumina-plant.md). Everything is in kg per kg of alumina unless named otherwise. Each stream carries its alumina in a fixed share,
 * so a part's output is its input times a ratio. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Bayer {
	private Bayer() {
	}

	/** kg of alumina from one bauxite block: its gibbsite, as Al2O3, times the extraction. */
	public static double aluminaPerBauxiteBlock() {
		return Ore.BAUXITE.kgPerBlock() * Tuning.AL2O3_IN_GIBBSITE * Tuning.BAYER_EXTRACTION;
	}

	public static double bauxite() {
		return Ore.BAUXITE.blockKg() / aluminaPerBauxiteBlock();
	}

	/** Al(OH)3 dissolved, then precipitated: 1 / 0.654. */
	public static double hydrate() {
		return 1 / Tuning.AL2O3_IN_GIBBSITE;
	}

	/** Strong liquor to the mill: 1,000 / productivity m3 a t, at its density. */
	public static double strongLiquor() {
		return Tuning.BAYER_LIQUOR_KG_PER_L / Tuning.BAYER_PRODUCTIVITY * 1000;
	}

	public static double bauxiteSlurry() {
		return bauxite() + strongLiquor();
	}

	/** Dry red mud: the bauxite less the gibbsite that dissolved. */
	public static double mud() {
		return bauxite() - hydrate();
	}

	public static double mudSlurry() {
		return mud() / Tuning.MUD_UNDERFLOW_SOLIDS;
	}

	/** The settler's overflow. */
	public static double greenLiquor() {
		return bauxiteSlurry() - mudSlurry();
	}

	/** The washer's water, which leaves as its overflow: the mud carries as much liquid out as it brought in. */
	public static double washWater() {
		return Tuning.BAYER_WASH_PER_ALUMINA;
	}

	/** Polished liquor, and the hydrate slurry it becomes: both green liquors. */
	public static double polishedLiquor() {
		return greenLiquor() + washWater();
	}

	public static double spentLiquor() {
		return polishedLiquor() - hydrate();
	}

	/** kg of 32 % caustic soda made up. */
	public static double caustic() {
		return Tuning.BAYER_NAOH_PER_ALUMINA / Tuning.CAUSTIC_STRENGTH;
	}

	public static double lime() {
		return Tuning.BAYER_LIME_PER_ALUMINA;
	}

	/** Water the evaporator boils off so the loop comes back to its strong liquor. */
	public static double evaporated() {
		return spentLiquor() + caustic() - strongLiquor();
	}

	public static double digesterSteam() {
		return Tuning.DIGEST_STEAM_PER_ALUMINA;
	}

	public static double evaporatorSteam() {
		return evaporated() * Tuning.EVAPORATOR_STEAM_PER_WATER;
	}

	public static double steam() {
		return digesterSteam() + evaporatorSteam();
	}

	public static double cake() {
		return mud() / Tuning.RED_MUD_CAKE_SOLIDS;
	}

	public static double aluminaPerRedMudBlock() {
		return Tuning.RED_MUD_BLOCK_KG / cake();
	}

	/** Joules of fuel a kg of process steam takes at the package boiler. */
	public static double boilerJoulesPerSteam() {
		return Tuning.PROCESS_STEAM_J_PER_KG / Tuning.PROCESS_BOILER_EFFICIENCY;
	}

	// ---- the lime kiln and the AlF3 reactor -----------------------------------------------------------------------------------
	/** kg of quicklime from one limestone block. */
	public static double limePerLimestoneBlock() {
		return Ore.LIMESTONE.kgPerBlock() * Tuning.CAO_PER_CACO3;
	}

	/** kg of CO2 a kg of quicklime. */
	public static double co2PerLime() {
		return 1 / Tuning.CAO_PER_CACO3 - 1;
	}

	/** kg of water vapour a kg of AlF3: three moles a mole. */
	public static double waterPerAlf3() {
		return Tuning.HYDRATE_PER_ALF3 + Tuning.HF_PER_ALF3 - 1;
	}

	/** kg of NaOH, and of 32 % caustic, a kg of chlorine from the chlor-alkali cell. */
	public static double causticPerCl2() {
		return Tuning.NAOH_PER_CL2 / Tuning.CAUSTIC_STRENGTH;
	}
}
