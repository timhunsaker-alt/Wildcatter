package dev.wildcatter.block;

/** What racks welded rods for the assembly fixture: the rod welder, or the rod inspection line between them (19c1). */
public interface RodSource {
	int rack();

	boolean takeRod();

	/** 16c1: the U-235 assay of the rods in its rack. */
	double rodAssay();

	/** 19c2: whether the rod the last {@link #takeRod} gave carries the IFBA coating (they go first). */
	default boolean lastTakenIfba() {
		return false;
	}
}
