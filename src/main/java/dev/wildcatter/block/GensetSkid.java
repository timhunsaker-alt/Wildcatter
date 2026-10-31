package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Multiblock;
import dev.wildcatter.multiblock.MultiblockLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * The true-size genset skids (docs/multiblocks/genset.md): where each part's main cell goes, measured from the base's main
 * cell. The control panel checks a set against these layouts, and a part clicked onto a base snaps into its slot by them.
 */
public final class GensetSkid {
	private GensetSkid() {
	}

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

	private static final Map<Character, String> NAMES = Map.of('F', "sub-base fuel tank", 'B', "base frame", 'R', "radiator",
			'E', "diesel engine", 'G', "gas engine", 'A', "alternator", 'K', "control panel", 'X', "exhaust silencer");

	private static Block block(char c, boolean gas) {
		return switch (c) {
			case 'F' -> WildcatterBlocks.FUEL_TANK;
			case 'B' -> WildcatterBlocks.BASE_FRAME;
			case 'R' -> WildcatterBlocks.RADIATOR;
			case 'E' -> WildcatterBlocks.DIESEL_ENGINE_PART;
			case 'G' -> WildcatterBlocks.GAS_ENGINE_PART;
			case 'A' -> WildcatterBlocks.ALTERNATOR;
			case 'K' -> WildcatterBlocks.GENSET_CONTROL_PANEL;
			default -> gas ? WildcatterBlocks.EXHAUST_SILENCER_LARGE : WildcatterBlocks.EXHAUST_STACK;
		};
	}

	private static MultiblockLayout layout(boolean gas) {
		return gas ? GAS : DIESEL;
	}

	/** The offset of a part's main cell from the base's main cell, for a set turned {@code turns}; null if it has no slot. */
	private static int @Nullable [] slot(Block part, boolean gas, int turns) {
		for (MultiblockLayout.Cell cell : layout(gas).cells(turns)) {
			if (block(cell.part(), gas) == part) {
				return new int[] {cell.dx(), cell.dy(), cell.dz()};
			}
		}
		return null;
	}

	/** The base a part at {@code pos} would stand on: straight under it, or under the parts it would sit on. */
	private static @Nullable BlockPos baseUnder(BlockGetter level, BlockPos pos) {
		BlockPos at = pos.below();
		for (int i = 0; i < 3; i++) {
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof GensetPartBlock part && part.part().isBase()) {
				return BigParts.main(at, state);
			}
			if (!(state.getBlock() instanceof GensetPartBlock) && !(state.getBlock() instanceof GensetControlPanelBlock)) {
				return null;
			}
			at = BigParts.main(at, state).below();
		}
		return null;
	}

	/** Where a skid part clicked at {@code pos} belongs, and which way it faces: its slot on the base it is clicked onto. */
	record Slot(BlockPos main, Direction facing) {
	}

	static @Nullable Slot slotFor(BlockGetter level, BlockPos pos, Block part) {
		BlockPos base = baseUnder(level, pos);
		if (base == null) {
			return null;
		}
		BlockState baseState = level.getBlockState(base);
		boolean gas = baseState.is(WildcatterBlocks.BASE_FRAME);
		Direction facing = ((BigPart) baseState.getBlock()).facing(baseState);
		int[] at = slot(part, gas, Multiblock.turns(facing));
		return at == null ? null : new Slot(base.offset(at[0], at[1], at[2]), facing);
	}

	/** For {@link BigPart#snap}: a click on top of a base (or on a part already on it) moves to the part's slot. */
	static BlockPlaceContext snap(BlockPlaceContext context, Block part) {
		Slot slot = slotFor(context.getLevel(), context.getClickedPos(), part);
		if (slot == null || slot.main().equals(context.getClickedPos())) {
			return context;
		}
		return BlockPlaceContext.at(context, slot.main(), Direction.UP);
	}

	/** The base's main cell for a set whose control panel's main cell is at {@code panel}. */
	public static BlockPos base(BlockPos panel, Direction facing, boolean gas) {
		int[] k = slot(WildcatterBlocks.GENSET_CONTROL_PANEL, gas, Multiblock.turns(facing));
		return panel.offset(-k[0], -k[1], -k[2]);
	}

	/**
	 * Test hook: builds a whole set round a panel at {@code panel}, every part grown into its cells, as if placed by hand. Returns
	 * the cells it overwrote that were not air, so a test can tell when a set does not fit its scene.
	 */
	public static List<BlockPos> build(Level level, BlockPos panel, Direction facing, boolean gas) {
		BlockPos base = base(panel, facing, gas);
		List<BlockPos> overwrote = new ArrayList<>();
		for (MultiblockLayout.Cell cell : layout(gas).cells(Multiblock.turns(facing))) {
			Block part = block(cell.part(), gas);
			BlockPos main = base.offset(cell.dx(), cell.dy(), cell.dz());
			BlockState state = part.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			for (BlockPos at : BigParts.cells(main, state)) {
				if (!level.getBlockState(at).isAir()) {
					overwrote.add(at.immutable());
				}
			}
			level.setBlock(main, state, Block.UPDATE_ALL);
			BigParts.grow(level, main, state);
		}
		return overwrote;
	}

	/** What the panel at {@code panel}, facing {@code facing}, finds: every part's main cell, or why the set is not built. */
	record Found(@Nullable List<BlockPos> parts, boolean gas, String missing) {
	}

	static Found check(Level level, BlockPos panel, Direction facing) {
		int turns = Multiblock.turns(facing);
		String firstWhy = null;
		for (boolean gas : new boolean[] {false, true}) {
			int[] k = slot(WildcatterBlocks.GENSET_CONTROL_PANEL, gas, turns);
			BlockPos base = panel.offset(-k[0], -k[1], -k[2]);
			BlockState under = level.getBlockState(base);
			boolean thisKind = under.is(gas ? WildcatterBlocks.BASE_FRAME : WildcatterBlocks.FUEL_TANK);
			List<BlockPos> parts = new ArrayList<>();
			String why = null;
			for (MultiblockLayout.Cell cell : layout(gas).cells(turns)) {
				BlockPos at = base.offset(cell.dx(), cell.dy(), cell.dz());
				BlockState state = level.getBlockState(at);
				Block want = block(cell.part(), gas);
				String name = NAMES.get(cell.part());
				String where = " at " + at.getX() + " " + at.getY() + " " + at.getZ();
				if (!level.isLoaded(at) || !state.is(want)) {
					why = name + where;
				} else if (state.getValue(((BigPart) want).piece()) != 0 || ((BigPart) want).facing(state) != facing) {
					why = name + where + " turned to face the set's way (break it and place it again on the skid)";
				}
				if (why != null) {
					break;
				}
				parts.add(at.immutable());
			}
			if (why == null) {
				return new Found(parts, gas, "");
			}
			if (thisKind || firstWhy == null) {
				firstWhy = why;
			}
			if (thisKind) {
				break;
			}
		}
		return new Found(null, false, firstWhy);
	}
}
