package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.Map;

/**
 * The 4.16 kV drive lineup (docs/multiblocks/vfd.md): the input transformer section, the power cell section and the output and
 * control section side by side, west to east facing north, all with their doors to the front. Its origin is the transformer
 * section; the control section checks it. The sections go down where they are clicked: there is nothing to build them on.
 */
public final class DriveLineup {
	private DriveLineup() {
	}

	private static final class Holder {
		static final MultiblockLayout LINEUP = new MultiblockLayout('T', new String[] {"T.P.C", "....."});
		static final PartLayout LAYOUT = new PartLayout(WildcatterBlocks.DRIVE_MV_CONTROL,
				Map.of('T', "input transformer section", 'P', "power cell section", 'C', "output and control section"),
				false,
				new PartLayout.Variant(LINEUP, Map.of('T', WildcatterBlocks.DRIVE_MV_TRANSFORMER, 'P', WildcatterBlocks.DRIVE_MV_CELLS,
						'C', WildcatterBlocks.DRIVE_MV_CONTROL)));
	}

	public static PartLayout layout() {
		return Holder.LAYOUT;
	}
}
