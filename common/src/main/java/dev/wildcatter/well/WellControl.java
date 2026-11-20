package dev.wildcatter.well;

import dev.wildcatter.Tuning;

/**
 * The well-control arithmetic of Phase 23 (docs/multiblocks/oilfield-follow-ons.md), in oilfield units (psi, ppg, ft) as a driller reads
 * them: the casing shoe's fracture gradient, the MAASP, ECD, losses, and the kill sheet. Plain Java, so unit tests can check it
 * ({@code WellControlTest}).
 */
public final class WellControl {
	private WellControl() {
	}

	/** Hydrostatic pressure, psi, of {@code ppg} mud over {@code feet}: 0.052 x ppg x ft. */
	public static double psi(double ppg, double feet) {
		return Tuning.PSI_PER_PPG_FT * ppg * feet;
	}

	/** A pressure at a depth as an equivalent mud weight, ppg. */
	public static double emw(double psi, double feet) {
		return feet <= 0 ? 0 : psi / (Tuning.PSI_PER_PPG_FT * feet);
	}

	/**
	 * The fracture gradient of a casing shoe at height {@code shoeY}, as an equivalent mud weight: 15.4 ppg for an intermediate shoe set
	 * within 12 blocks above a deep field's top (its transition zone), else 14.0 (the open hole's under surface casing).
	 *
	 * @param deepTopY the top of the deep field under the hole, or {@code Integer.MIN_VALUE} for none
	 */
	public static double shoeFg(int shoeY, int deepTopY) {
		if (deepTopY != Integer.MIN_VALUE && shoeY > deepTopY && shoeY - deepTopY <= Tuning.DEEP_SHOE_WINDOW) {
			return Tuning.FG_DEEP_SHOE_PPG;
		}
		return Tuning.FG_OPEN_HOLE_PPG;
	}

	/** MAASP, psi: the surface pressure that would break down the shoe, (FG - MW) x 0.052 x shoe TVD. Under zero, the shoe is already over. */
	public static double maasp(double fgPpg, double mudPpg, double shoeFeet) {
		return psi(fgPpg - mudPpg, shoeFeet);
	}

	/** What a loss is: none, partial (pits falling, up to 1 m3 a minute), or total (no returns). */
	public enum Losses {
		NONE, PARTIAL, TOTAL
	}

	/** The losses for an equivalent mud weight at the shoe against its fracture gradient. */
	public static Losses losses(double shoeEmw, double fgPpg) {
		double over = shoeEmw - fgPpg;
		return over <= 1e-9 ? Losses.NONE : over > Tuning.LOSS_TOTAL_OVER_PPG ? Losses.TOTAL : Losses.PARTIAL;
	}

	/** m3/s of mud a partial loss takes: up to 1 m3 a minute, growing with the excess over the FG up to 0.5 ppg. */
	public static double partialLossRate(double shoeEmw, double fgPpg) {
		double over = Math.max(0, shoeEmw - fgPpg);
		return Tuning.LOSS_PARTIAL_M3S * Math.min(1, over / Tuning.LOSS_TOTAL_OVER_PPG);
	}

	/** The kill sheet for a shut-in kick (wait and weight; the driller's method uses its ICP for the first circulation). */
	public record KillSheet(double sidpp, double sicp, double omw, double kmwExact, double kmw, double scrPsi, double icp, double fcp,
			int strokesToBit, int strokesBitToShoe, int strokesShoeToSurface, double maasp) {
		/** The drill pipe pressure the schedule asks for after {@code strokes} (wait and weight). */
		public double schedule(double strokes) {
			if (strokes >= strokesToBit) {
				return fcp;
			}
			return icp + (fcp - icp) * Math.max(0, strokes) / Math.max(1, strokesToBit);
		}

		/** Strokes from the start to kill mud at surface: one full circulation. */
		public int strokesCirculation() {
			return strokesToBit + strokesBitToShoe + strokesShoeToSurface;
		}
	}

	/**
	 * The kill sheet: KMW = OMW + SIDPP / (0.052 x TVD), the trip margin on top; ICP = SIDPP + SCR pressure; FCP = SCR pressure x KMW / OMW;
	 * strokes from the hole's capacities at 0.1 bbl a stroke.
	 */
	public static KillSheet killSheet(double sidpp, double sicp, double omw, double tvdFeet, double shoeFeet, double fgPpg) {
		double exact = omw + sidpp / Math.max(1, Tuning.PSI_PER_PPG_FT * tvdFeet);
		double kmw = exact + Tuning.TRIP_MARGIN_PPG;
		double scr = Tuning.SCR_PSI;
		double depthM = tvdFeet / Tuning.FEET_PER_M, shoeM = shoeFeet / Tuning.FEET_PER_M;
		double stroke = Tuning.PUMP_L_PER_STROKE;
		int toBit = (int) Math.round(depthM * Tuning.STRING_L_PER_M / stroke);
		int bitToShoe = (int) Math.round((depthM - shoeM) * Tuning.ANNULUS_L_PER_M / stroke);
		int shoeUp = (int) Math.round(shoeM * Tuning.ANNULUS_L_PER_M / stroke);
		return new KillSheet(sidpp, sicp, omw, exact, kmw, scr, sidpp + scr, scr * kmw / omw, toBit, bitToShoe, shoeUp, maasp(fgPpg, omw, shoeFeet));
	}
}
