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

	/** 20c: the bag for the station at {@code station}, for a part with two products, each to its own station; by default the one bag. */
	default Item bagItem(net.minecraft.core.BlockPos station) {
		return bagItem();
	}

	default double bagReady(net.minecraft.core.BlockPos station) {
		return bagReady();
	}

	default double takeForBag(net.minecraft.core.BlockPos station, double kg) {
		return takeForBag(kg);
	}
}
