package dev.wildcatter.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Checks a {@link MultiblockLayout} against the world for a controller at a position and facing. Every multiblock's controller
 * calls this every {@code STRUCTURE_CHECK_TICKS} ticks (docs/multiblocks/README.md); the four hand-written cube checks it replaces
 * are gone with the old cubes.
 */
public final class Multiblock {
	private Multiblock() {
	}

	/** Quarter turns clockwise from north, as {@link MultiblockLayout#cells(int)} takes them. */
	public static int turns(Direction facing) {
		return switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	/**
	 * The world position of every part if the structure is complete, or null if any cell is wrong. The controller's own cell is
	 * included. {@code legend} says what each layout character accepts.
	 */
	public static List<BlockPos> match(Level level, BlockPos controller, Direction facing, MultiblockLayout layout, Map<Character, Predicate<BlockState>> legend) {
		List<BlockPos> parts = new ArrayList<>(layout.size());
		for (MultiblockLayout.Cell cell : layout.cells(turns(facing))) {
			BlockPos at = controller.offset(cell.dx(), cell.dy(), cell.dz());
			Predicate<BlockState> accepts = legend.get(cell.part());
			if (accepts == null || !level.isLoaded(at) || !accepts.test(level.getBlockState(at))) {
				return null;
			}
			parts.add(at);
		}
		return parts;
	}

	/** The first missing or wrong part, for the readout: its legend character and where it should be, or null if complete. */
	public static String missing(Level level, BlockPos controller, Direction facing, MultiblockLayout layout, Map<Character, Predicate<BlockState>> legend, Map<Character, String> names) {
		for (MultiblockLayout.Cell cell : layout.cells(turns(facing))) {
			BlockPos at = controller.offset(cell.dx(), cell.dy(), cell.dz());
			Predicate<BlockState> accepts = legend.get(cell.part());
			if (accepts == null || !accepts.test(level.getBlockState(at))) {
				return names.getOrDefault(cell.part(), String.valueOf(cell.part())) + " at " + at.getX() + " " + at.getY() + " " + at.getZ();
			}
		}
		return null;
	}
}
