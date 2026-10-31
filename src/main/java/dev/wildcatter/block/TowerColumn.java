package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.Map;

/**
 * The true-size cooling tower cell (docs/multiblocks/cooling-tower.md): basin, fill, drift eliminators and fan deck, their
 * main cells in one column, 4 x 4 m and 6 high. The basin goes down first; a part clicked on top snaps onto the column.
 */
public final class TowerColumn {
	private TowerColumn() {
	}

	/** How far above the basin's main cell the fan deck's main cell is. */
	public static final int DECK_HEIGHT = 4;

	private static final class Holder {
		static final MultiblockLayout COLUMN = new MultiblockLayout('W',
				new String[] {"W...", "....", "....", "...."},
				new String[] {"F...", "....", "....", "...."},
				new String[] {"....", "....", "....", "...."},
				new String[] {"E...", "....", "....", "...."},
				new String[] {"M...", "....", "....", "...."});
		static final PartLayout LAYOUT = new PartLayout(WildcatterBlocks.COOLING_TOWER,
				Map.of('W', "cold-water basin", 'F', "cooling fill", 'E', "drift eliminators", 'M', "fan deck"),
				true,
				new PartLayout.Variant(COLUMN, Map.of('W', WildcatterBlocks.TOWER_BASIN, 'F', WildcatterBlocks.COOLING_FILL,
						'E', WildcatterBlocks.DRIFT_ELIMINATOR, 'M', WildcatterBlocks.COOLING_TOWER)));
	}

	public static PartLayout layout() {
		return Holder.LAYOUT;
	}
}
