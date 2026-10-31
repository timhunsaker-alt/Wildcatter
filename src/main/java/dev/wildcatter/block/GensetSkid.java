package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The true-size genset skids (docs/multiblocks/genset.md): where each part's main cell goes, measured from the base's main
 * cell. Variant 0 is the diesel set, variant 1 the gas set. The control panel checks a set against these layouts, and a part
 * clicked onto a base snaps into its slot by them.
 */
public final class GensetSkid {
	private GensetSkid() {
	}

	private static final class Holder {
		/** Diesel, 5 x 2 x 4: the sub-base tank, then radiator, engine, alternator and panel, the silencer over the engine. */
		static final MultiblockLayout DIESEL = new MultiblockLayout('F',
				new String[] {"F....", "....."},
				new String[] {"RE.AK", "....."},
				new String[] {".....", "....."},
				new String[] {".....", ".X..."});
		/** Gas, 7 x 2 x 4: the base frame, then radiator, the V20, alternator and panel, the large silencer over the engine. */
		static final MultiblockLayout GAS = new MultiblockLayout('B',
				new String[] {"B......", "......."},
				new String[] {"RG...AK", "......."},
				new String[] {".......", "......."},
				new String[] {".......", "..X...."});
		static final PartLayout LAYOUT = new PartLayout(WildcatterBlocks.GENSET_CONTROL_PANEL,
				Map.of('F', "sub-base fuel tank", 'B', "base frame", 'R', "radiator", 'E', "diesel engine", 'G', "gas engine",
						'A', "alternator", 'K', "control panel", 'X', "exhaust silencer"),
				true,
				new PartLayout.Variant(DIESEL, Map.of('F', WildcatterBlocks.FUEL_TANK, 'R', WildcatterBlocks.RADIATOR, 'E', WildcatterBlocks.DIESEL_ENGINE_PART,
						'A', WildcatterBlocks.ALTERNATOR, 'K', WildcatterBlocks.GENSET_CONTROL_PANEL, 'X', WildcatterBlocks.EXHAUST_STACK)),
				new PartLayout.Variant(GAS, Map.of('B', WildcatterBlocks.BASE_FRAME, 'R', WildcatterBlocks.RADIATOR, 'G', WildcatterBlocks.GAS_ENGINE_PART,
						'A', WildcatterBlocks.ALTERNATOR, 'K', WildcatterBlocks.GENSET_CONTROL_PANEL, 'X', WildcatterBlocks.EXHAUST_SILENCER_LARGE)));
	}

	public static PartLayout layout() {
		return Holder.LAYOUT;
	}

	/** The base's main cell for a set whose control panel's main cell is at {@code panel}. */
	public static BlockPos base(BlockPos panel, Direction facing, boolean gas) {
		return layout().origin(panel, facing, gas ? 1 : 0);
	}

	/** Test hook: builds a whole set round a panel (see {@link PartLayout#build}). */
	public static List<BlockPos> build(Level level, BlockPos panel, Direction facing, boolean gas) {
		return layout().build(level, panel, facing, gas ? 1 : 0);
	}
}
