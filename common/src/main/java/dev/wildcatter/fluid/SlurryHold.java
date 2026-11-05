package dev.wildcatter.fluid;

import java.util.EnumMap;
import java.util.Map;

/**
 * The slurry a source node holds, ready to give to the pipe: kg and assay for each product. A node that puts a concentrator slurry
 * on a pipe keeps one, answers {@code FluidNode} and {@code Assayed} from it, and saves it with {@link #save}. Plain Java.
 */
public final class SlurryHold {
	private final Map<Product, Double> kg = new EnumMap<>(Product.class);
	private final Map<Product, SlurryAssay> assay = new EnumMap<>(Product.class);

	public double held(Product product) {
		return kg.getOrDefault(product, 0.0);
	}

	public SlurryAssay assayOf(Product product) {
		return assay.getOrDefault(product, SlurryAssay.EMPTY);
	}

	/** Adds {@code amount} kg of {@code product} made up as {@code in}, blending it by mass with what is held. */
	public void offer(Product product, double amount, SlurryAssay in) {
		if (amount <= 0) {
			return;
		}
		double before = held(product);
		assay.put(product, SlurryAssay.blend(assayOf(product), before, in, amount));
		kg.put(product, before + amount);
	}

	/** Gives up to {@code max} kg of {@code product}; returns how much. What stays keeps its assay. */
	public double give(Product product, double max) {
		double got = Math.clamp(max, 0, held(product));
		double left = held(product) - got;
		if (left <= 1e-9) {
			kg.remove(product);
			assay.remove(product);
		} else {
			kg.put(product, left);
		}
		return got;
	}

	/** Saved as one line per product: {@code PRODUCT|kg|assay}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<Product, Double> e : kg.entrySet()) {
			out.append(out.length() == 0 ? "" : "\n").append(e.getKey().name()).append('|').append(e.getValue()).append('|')
					.append(assayOf(e.getKey()).save());
		}
		return out.toString();
	}

	/** Replaces what is held with what {@link #save} wrote; lines that do not read are left out. */
	public void load(String saved) {
		kg.clear();
		assay.clear();
		if (saved == null || saved.isBlank()) {
			return;
		}
		for (String line : saved.split("\n")) {
			String[] part = line.split("\\|", 3);
			if (part.length < 2) {
				continue;
			}
			try {
				Product product = Product.valueOf(part[0]);
				double amount = Double.parseDouble(part[1]);
				if (amount > 0) {
					kg.put(product, amount);
					assay.put(product, SlurryAssay.load(part.length > 2 ? part[2] : ""));
				}
			} catch (IllegalArgumentException ex) {
				// left out
			}
		}
	}
}
