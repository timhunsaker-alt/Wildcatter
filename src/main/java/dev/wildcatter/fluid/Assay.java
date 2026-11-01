package dev.wildcatter.fluid;

import java.util.EnumMap;
import java.util.Map;

/**
 * How a crude unit splits its feed (docs/multiblocks/crude-units.md), by volume: a light sweet, WTI-like crude, and a gas well's
 * condensate. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Assay {
	private Assay() {
	}

	private static final Map<Product, Double> CRUDE = cuts(0.02, 0.30, 0.14, 0.26, 0.28);
	private static final Map<Product, Double> CONDENSATE = cuts(0.05, 0.60, 0.15, 0.15, 0.05);

	private static Map<Product, Double> cuts(double lpg, double gasoline, double kerosene, double diesel, double residue) {
		Map<Product, Double> m = new EnumMap<>(Product.class);
		m.put(Product.LPG, lpg);
		m.put(Product.GASOLINE, gasoline);
		m.put(Product.KEROSENE, kerosene);
		m.put(Product.DIESEL, diesel);
		m.put(Product.RESIDUE, residue);
		return m;
	}

	/** The cuts of {@code feed} (crude or condensate), as shares of its volume; empty for anything else. */
	public static Map<Product, Double> of(Product feed) {
		return feed == Product.CRUDE ? CRUDE : feed == Product.CONDENSATE ? CONDENSATE : Map.of();
	}

	/** True for what a crude unit takes. */
	public static boolean feed(Product p) {
		return p == Product.CRUDE || p == Product.CONDENSATE;
	}
}
