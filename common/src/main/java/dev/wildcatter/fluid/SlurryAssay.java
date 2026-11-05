package dev.wildcatter.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * What a kg of a concentrator slurry carries: the kg of each mineral, keyed by the mineral's item id ({@code wildcatter:galena},
 * {@code minecraft:iron_ingot}, {@code tailings:zircon}). The rest is water and gangue. Plain Java, so it is unit-tested without
 * Minecraft.
 */
public final class SlurryAssay {
	public static final SlurryAssay EMPTY = new SlurryAssay(Map.of());

	private final Map<String, Double> shares;

	private SlurryAssay(Map<String, Double> shares) {
		this.shares = shares;
	}

	/** An assay of these shares (kg per kg). Each is clamped to 0..1, and all are scaled down together if they add up past 1. */
	public static SlurryAssay of(Map<String, Double> shares) {
		Map<String, Double> out = new TreeMap<>();
		double sum = 0;
		for (Map.Entry<String, Double> e : shares.entrySet()) {
			double v = e.getValue() == null || Double.isNaN(e.getValue()) ? 0 : Math.clamp(e.getValue(), 0, 1);
			if (v > 0 && e.getKey() != null && !e.getKey().isBlank()) {
				out.put(e.getKey(), v);
				sum += v;
			}
		}
		if (sum > 1) {
			for (Map.Entry<String, Double> e : out.entrySet()) {
				e.setValue(e.getValue() / sum);
			}
		}
		return out.isEmpty() ? EMPTY : new SlurryAssay(Collections.unmodifiableMap(out));
	}

	/** The kg of {@code mineral} in each kg of slurry. */
	public double get(String mineral) {
		return shares.getOrDefault(mineral, 0.0);
	}

	/** The kg of {@code mineral} in {@code slurryKg} of slurry. */
	public double kg(String mineral, double slurryKg) {
		return get(mineral) * slurryKg;
	}

	public Set<String> minerals() {
		return shares.keySet();
	}

	public Map<String, Double> shares() {
		return shares;
	}

	public boolean isEmpty() {
		return shares.isEmpty();
	}

	/** {@code kgA} of {@code a} mixed with {@code kgB} of {@code b}: each mineral averaged by mass. */
	public static SlurryAssay blend(SlurryAssay a, double kgA, SlurryAssay b, double kgB) {
		kgA = Math.max(0, kgA);
		kgB = Math.max(0, kgB);
		if (kgA + kgB <= 0) {
			return kgB > 0 ? b : a;
		}
		Map<String, Double> out = new TreeMap<>();
		for (String m : a.minerals()) {
			out.merge(m, a.get(m) * kgA, Double::sum);
		}
		for (String m : b.minerals()) {
			out.merge(m, b.get(m) * kgB, Double::sum);
		}
		double total = kgA + kgB;
		out.replaceAll((m, kg) -> kg / total);
		return of(out);
	}

	/** Saved as {@code id=share;id=share}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, Double> e : shares.entrySet()) {
			out.append(out.length() == 0 ? "" : ";").append(e.getKey()).append('=').append(e.getValue());
		}
		return out.toString();
	}

	/** The assay {@link #save} wrote; whatever does not read is left out. */
	public static SlurryAssay load(String saved) {
		if (saved == null || saved.isBlank()) {
			return EMPTY;
		}
		Map<String, Double> out = new LinkedHashMap<>();
		for (String part : saved.split(";")) {
			int eq = part.lastIndexOf('=');
			if (eq <= 0) {
				continue;
			}
			try {
				out.put(part.substring(0, eq), Double.parseDouble(part.substring(eq + 1)));
			} catch (NumberFormatException ex) {
				// left out
			}
		}
		return of(out);
	}

	/** For the Power Meter: the three richest minerals, in %. */
	public String describe() {
		if (shares.isEmpty()) {
			return "no minerals";
		}
		List<Map.Entry<String, Double>> rich = new ArrayList<>(shares.entrySet());
		rich.sort(Map.Entry.<String, Double>comparingByValue().reversed());
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < Math.min(3, rich.size()); i++) {
			String id = rich.get(i).getKey();
			out.append(i == 0 ? "" : ", ").append(id.substring(id.indexOf(':') + 1).replace('_', ' '))
					.append(String.format(" %.2f %%", rich.get(i).getValue() * 100));
		}
		return out.toString();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof SlurryAssay other && shares.equals(other.shares);
	}

	@Override
	public int hashCode() {
		return shares.hashCode();
	}

	@Override
	public String toString() {
		return "SlurryAssay[" + save() + "]";
	}
}
