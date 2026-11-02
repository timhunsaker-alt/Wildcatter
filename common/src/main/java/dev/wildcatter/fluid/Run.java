package dev.wildcatter.fluid;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * What one joined run of steel pipe carries (docs/multiblocks/lease.md): the product its sources put on it. Sinks have no say. With no
 * source it carries nothing yet; with sources of two products it is BLOCKED and nothing flows.
 *
 * @param product what flows, or null if nothing does (no source, or blocked)
 * @param clash   the products that met, when blocked; empty otherwise
 */
public record Run(@Nullable Product product, Set<Product> clash) {
	public static final Run EMPTY = new Run(null, Set.of());

	/** The run for the products its sources offer (nulls are sources with nothing to offer yet, like an empty tank). */
	public static Run of(Collection<@Nullable Product> offered) {
		Set<Product> seen = new LinkedHashSet<>();
		for (Product p : offered) {
			if (p != null) {
				seen.add(p);
			}
		}
		if (seen.isEmpty()) {
			return EMPTY;
		}
		return seen.size() == 1 ? new Run(seen.iterator().next(), Set.of()) : new Run(null, java.util.Collections.unmodifiableSet(seen));
	}

	public boolean blocked() {
		return !clash.isEmpty();
	}

	/** True if {@code wanted} flows on this run. */
	public boolean carries(Product wanted) {
		return product == wanted;
	}

	/** For the Power Meter: what the run carries, or what met on it. */
	public String describe() {
		if (blocked()) {
			StringBuilder met = new StringBuilder();
			for (Product p : clash) {
				met.append(met.length() == 0 ? "" : " meets ").append(p.label());
			}
			return "BLOCKED: " + met + " here, so nothing flows. Break the joint between them";
		}
		return product == null ? "carrying nothing yet" : "carrying " + product.label();
	}
}
