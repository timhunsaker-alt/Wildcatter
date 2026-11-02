package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.Map;

/**
 * The true-size 1 MW steam plant (docs/multiblocks/steam-plant.md), 6 x 7 m and 4 high, every part on the ground. Its origin
 * is the generator, the controller: the plant has no part to build on, so its parts do not snap. The generator checks it.
 */
public final class SteamPlantLayout {
	private SteamPlantLayout() {
	}

	private static final class Holder {
		static final MultiblockLayout PLANT = new MultiblockLayout('G',
				new String[] {"BD...S", "......", "......", ".G..T.", "......", ".P.C..", "......"});
		static final PartLayout LAYOUT = new PartLayout(WildcatterBlocks.TURBINE_GENERATOR,
				Map.of('B', "burner", 'D', "boiler", 'S', "superheater", 'T', "turbine", 'G', "generator", 'C', "condenser", 'P', "feed pump"),
				false,
				new PartLayout.Variant(PLANT, Map.of('B', WildcatterBlocks.BOILER_BURNER, 'D', WildcatterBlocks.BOILER_DRUM,
						'S', WildcatterBlocks.SUPERHEATER, 'T', WildcatterBlocks.TURBINE_CASING, 'G', WildcatterBlocks.TURBINE_GENERATOR,
						'C', WildcatterBlocks.CONDENSER, 'P', WildcatterBlocks.FEED_PUMP)));
	}

	public static PartLayout layout() {
		return Holder.LAYOUT;
	}
}
