package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The acid plant's steam in the 5 MW turbine (docs/multiblocks/acid-plant-steam.md). The turbine makes 0.87 MW a kg/s at its design
 * flow; below it, its throttle loses a share that grows as the flow falls, a tenth at a third of design. Plain Java, so it is
 * unit-tested without Minecraft.
 */
public final class AcidSteam {
	private AcidSteam() {
	}

	/** kg of 40 bar steam the waste heat boiler raises a second at the plant's rating: 1.2 t a tonne of acid. */
	public static double boilerKgs() {
		return AcidPlant.ratedAcidKgs() * Tuning.ACID_STEAM_PER_ACID;
	}

	/** What is left for the turbine once the melter's coils have had their share. */
	public static double turbineKgs() {
		return boilerKgs() - Tuning.MELTER_STEAM_KGS;
	}

	/** Watts a kg/s at the design point. */
	public static double designWattsPerKgs() {
		return Tuning.ST_DESIGN_WATTS / Tuning.ST_DESIGN_STEAM_KGS;
	}

	/** The share lost to throttling at {@code kgs}: none at design flow, more as it falls. */
	public static double throttleLoss(double kgs) {
		double share = Math.max(0, Math.min(1, kgs / Tuning.ST_DESIGN_STEAM_KGS));
		return Tuning.ST_THROTTLE_LOSS * (1 - share);
	}

	/** Watts the turbine can make from {@code kgs} of superheated steam, up to its rating. */
	public static double turbineWatts(double kgs) {
		if (kgs <= 0) {
			return 0;
		}
		return Math.min(Tuning.ST_WATTS, kgs * designWattsPerKgs() * (1 - throttleLoss(kgs)));
	}

	/** Watts the superheater takes from the converter's gas to bring {@code kgs} from saturation to 400 C. */
	public static double superheatWatts(double kgs) {
		return kgs * Tuning.SUPERHEAT_J_PER_KG;
	}

	/** kg/s of make-up water the boiler needs while the turbine returns its condensate: what the melter and the losses take. */
	public static double makeUpKgs(double boilerKgs, double toTurbine) {
		return boilerKgs - toTurbine * Tuning.ACID_CONDENSATE_RETURN;
	}
}
