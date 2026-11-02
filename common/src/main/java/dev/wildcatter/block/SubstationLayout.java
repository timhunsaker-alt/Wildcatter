package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.Map;

/**
 * The true-size substation yard (docs/multiblocks/substation.md), 16 x 9 m and 7 high: the line runs west (138 kV) to east
 * (12.47 kV) along the yard's centre line, where every part's terminals are. Its origin is the power transformer, the
 * controller; the yard has no part to build on, so its parts do not snap.
 */
public final class SubstationLayout {
	private SubstationLayout() {
	}

	/** How far across the yard its centre line is, from the north edge. */
	public static final int CENTRE = 4;

	private static final class Holder {
		static final MultiblockLayout YARD = new MultiblockLayout('T',
				new String[] {"H.D.B...K.......", "................", "......T.........", "............bdL.", "................",
						"................", "................", "................", "................"});
		static final PartLayout LAYOUT = new PartLayout(WildcatterBlocks.POWER_TRANSFORMER,
				Map.of('H', "138 kV breaker", 'D', "138 kV disconnect", 'B', "138 kV bus", 'T', "power transformer", 'b', "12.47 kV bus",
						'd', "12.47 kV disconnect", 'L', "12.47 kV feeder breaker", 'K', "relay panel"),
				false,
				new PartLayout.Variant(YARD, Map.of('H', WildcatterBlocks.HV_BREAKER_3P, 'D', WildcatterBlocks.DISCONNECT_HV, 'B', WildcatterBlocks.BUS_HV,
						'T', WildcatterBlocks.POWER_TRANSFORMER, 'b', WildcatterBlocks.BUS_DIST, 'd', WildcatterBlocks.DISCONNECT_DIST,
						'L', WildcatterBlocks.DIST_BREAKER_3P, 'K', WildcatterBlocks.RELAY_PANEL)));
	}

	public static PartLayout layout() {
		return Holder.LAYOUT;
	}
}
