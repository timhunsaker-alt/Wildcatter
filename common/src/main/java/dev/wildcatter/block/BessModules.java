package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.BessCells;

/**
 * A battery container or cabinet, loaded with LFP modules by hand (22b): 96 make a container (5.0 MWh), 5 a cabinet (261 kWh). Modules
 * cannot go through a crafting grid in those numbers, so the part is crafted empty and filled where it stands.
 */
public interface BessModules {
	BessCells cells();

	int modules();

	int maxModules();

	void setModules(int n);

	/** Loads up to {@code n} new modules; returns how many it took. */
	default int load(int n) {
		int take = Math.max(0, Math.min(n, maxModules() - modules()));
		if (take > 0) {
			cells().addModules(take * Tuning.LFP_MODULE_KWH);
			setModules(modules() + take);
		}
		return take;
	}
}
