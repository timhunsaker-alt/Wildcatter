package dev.wildcatter.block;

import net.minecraft.world.item.Item;

/**
 * A part a bagging station set against it fills 1 t bulk bags from (docs/multiblocks/alumina-plant.md): a calciner's alumina, a
 * hydrate filter's hydrate, an AlF3 reactor's AlF3, a lime kiln's quicklime.
 */
public interface Bagged {
	/** The full bag it makes. */
	Item bagItem();

	/** kg it has ready for bags. */
	double bagReady();

	/** Takes up to {@code kg} for a bag; returns what it gave. */
	double takeForBag(double kg);
}
