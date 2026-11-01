package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The centrifuge cascade's numbers (docs/multiblocks/enrichment.md). Plain Java, so it is unit-tested without Minecraft. */
public final class Enrichment {
	private Enrichment() {
	}

	/** The value function of separative work. */
	public static double value(double x) {
		return (2 * x - 1) * Math.log(x / (1 - x));
	}

	/** kg of feed U a kg of product U. */
	public static double feedPerProduct() {
		return (Tuning.ASSAY_PRODUCT - Tuning.ASSAY_TAILS) / (Tuning.ASSAY_FEED - Tuning.ASSAY_TAILS);
	}

	/** kg of tails U a kg of product U. */
	public static double tailsPerProduct() {
		return feedPerProduct() - 1;
	}

	/** SWU a kg of product U. */
	public static double swuPerProduct() {
		return value(Tuning.ASSAY_PRODUCT) + tailsPerProduct() * value(Tuning.ASSAY_TAILS) - feedPerProduct() * value(Tuning.ASSAY_FEED);
	}

	private static double ratio(double x) {
		return x / (1 - x);
	}

	/** The stages of a symmetric cascade, enriching and stripping, each raising the abundance ratio by the root of the separation factor. */
	public static int stages() {
		double beta = Math.log(Math.sqrt(Tuning.CENTRIFUGE_SEPARATION_FACTOR));
		double enriching = Math.log(ratio(Tuning.ASSAY_PRODUCT) / ratio(Tuning.ASSAY_FEED)) / beta;
		double stripping = Math.log(ratio(Tuning.ASSAY_FEED) / ratio(Tuning.ASSAY_TAILS)) / beta;
		return (int) Math.ceil(enriching + stripping);
	}

	/** SWU a second one machine at full speed does. */
	public static double machineSwuPerSecond() {
		return Tuning.CENTRIFUGE_REAL_SWU_YEAR * Tuning.CENTRIFUGE_SWU_SCALE / (365.0 * 24 * 3600);
	}

	/** What one machine draws at full speed, at the plant's kWh a SWU. */
	public static double machineWatts() {
		return machineSwuPerSecond() * 3600 * Tuning.KWH_PER_SWU * 1000;
	}

	/** kg of U in a full 30B. */
	public static double uPer30b() {
		return Tuning.CYLINDER_30B_KG / Tuning.UF6_PER_U;
	}

	/** Hours a cascade of {@code machines} at full speed takes to fill a 30B. */
	public static double hoursPer30b(int machines) {
		return uPer30b() / (machines * machineSwuPerSecond() / swuPerProduct()) / 3600;
	}
}
