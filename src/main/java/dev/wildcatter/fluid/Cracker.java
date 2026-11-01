package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * What a steam cracker makes (docs/multiblocks/cracker.md), by weight of its feed: LPG or gasoline. Cracked gas is counted in kg;
 * ethylene and fuel gas in m3; LPG, gasoline and residue in litres. Plain Java, so it is unit-tested without Minecraft.
 *
 * @param ethylene the share that is ethylene
 * @param fuelGas methane and hydrogen
 * @param lpg propylene and the C4s, returned as LPG
 * @param pyGas pyrolysis gasoline
 * @param residue fuel oil
 */
public record Cracker(double ethylene, double fuelGas, double lpg, double pyGas, double residue) {
	public static final Cracker LPG = new Cracker(0.42, 0.28, 0.22, 0.08, 0.0);
	public static final Cracker GASOLINE = new Cracker(0.30, 0.17, 0.25, 0.25, 0.03);

	/** The yields for a feed, or null if it does not crack. */
	public static Cracker of(Product feed) {
		return feed == Product.LPG ? LPG : feed == Product.GASOLINE ? GASOLINE : null;
	}

	/** Kilograms in a litre of a cracker feed. */
	public static double kgPerLitre(Product feed) {
		return feed == Product.LPG ? Tuning.LPG_KG_PER_L : Tuning.GASOLINE_KG_PER_L;
	}

	/** The cracked gas (kg) that goes on to the quench: everything but the pyrolysis gasoline and residue the quench takes out. */
	public double total() {
		return ethylene + fuelGas + lpg + pyGas + residue;
	}
}
