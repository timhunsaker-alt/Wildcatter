package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The boron plant's numbers (docs/multiblocks/boron.md). Plain Java, so it is unit-tested without Minecraft. */
public final class Boron {
	private Boron() {
	}

	private static final double BORAX = 381.37;      // Na2B4O7.10H2O
	private static final double BORIC_ACID = 61.83;  // H3BO3
	private static final double H2SO4 = 98.08;
	private static final double NA2SO4 = 142.04;
	private static final double B2O3 = 69.62;
	private static final double B4C = 55.25;
	private static final double CO = 28.01;

	/** kg of borax a kg of boric acid: one borax makes four. */
	public static double boraxPerAcid() {
		return BORAX / (4 * BORIC_ACID);
	}

	/** kg of H2SO4 a kg of boric acid. */
	public static double sulfuricPerAcid() {
		return H2SO4 / (4 * BORIC_ACID);
	}

	/** Litres of 98 % acid (1.84 kg/L) a kg of boric acid. */
	public static double sulfuricLitresPerAcid() {
		return sulfuricPerAcid() / 0.98 / 1.84;
	}

	public static double sulfatePerAcid() {
		return NA2SO4 / (4 * BORIC_ACID);
	}

	/** kg of boric acid a kg of B4C: 2 B2O3 a B4C, 2 H3BO3 a B2O3. */
	public static double acidPerB4c() {
		return 4 * BORIC_ACID / B4C;
	}

	public static double b2o3PerB4c() {
		return 2 * B2O3 / B4C;
	}

	/** kg of carbon a kg of B4C: 7 C, one into the carbide and six to CO. */
	public static double carbonPerB4c() {
		return 7 * 12.011 / B4C;
	}

	public static double coPerB4c() {
		return 6 * CO / B4C;
	}

	/** kg of B4C in a rodlet: the pellet stack at 70 % density. */
	public static double rodletKg() {
		double r = 0.866 / 2;
		return Math.PI * r * r * 360 * 2.52 * 0.70 / 1000;
	}

	/** kg of boric acid that brings {@code waterKg} of coolant to {@code ppm} of boron. */
	public static double acidForPpm(double ppm, double waterKg) {
		return ppm * 1e-6 * waterKg / Tuning.B_IN_BORIC_ACID;
	}
}
