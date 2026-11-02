package dev.wildcatter.multiblock;

import java.util.ArrayList;
import java.util.List;

/**
 * The cells one big part fills at true scale (docs/multiblocks/README.md, "Big parts"): one item that, placed, takes up its
 * real size the way a bed takes two spaces. Cells are listed for the part facing NORTH as offsets from its main cell
 * (x east, y up, z south); the main cell is piece 0 and holds the block entity. {@link #offset} turns them for the other
 * facings, the same way round as {@link MultiblockLayout#cells(int)}. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Footprint {
	/** One cell of the part: its offset from the main cell when the part faces north. */
	public record Cell(int dx, int dy, int dz) {
	}

	private final List<Cell> cells;

	private Footprint(List<Cell> cells) {
		this.cells = List.copyOf(cells);
	}

	/**
	 * A box of cells {@code width} across (west to east), {@code height} up and {@code depth} front to back (north to south),
	 * with the main cell at ({@code mainX}, 0, {@code mainZ}) of the box's bottom layer. Piece 0 is the main cell; the rest go
	 * bottom layer first, each layer north to south, each row west to east.
	 */
	public static Footprint box(int width, int height, int depth, int mainX, int mainZ) {
		if (width < 1 || height < 1 || depth < 1 || mainX < 0 || mainX >= width || mainZ < 0 || mainZ >= depth) {
			throw new IllegalArgumentException("bad footprint " + width + "x" + height + "x" + depth + " with its main cell at " + mainX + "," + mainZ);
		}
		List<Cell> out = new ArrayList<>();
		out.add(new Cell(0, 0, 0));
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < depth; z++) {
				for (int x = 0; x < width; x++) {
					if (y != 0 || x != mainX || z != mainZ) {
						out.add(new Cell(x - mainX, y, z - mainZ));
					}
				}
			}
		}
		return new Footprint(out);
	}

	/**
	 * A part that does not fill a box (a crossarm leaves out the cell its pole stands in): the cells as offsets from the main
	 * cell, facing north, the main cell first. Each cell once.
	 */
	public static Footprint of(Cell... cells) {
		if (cells.length == 0 || !cells[0].equals(new Cell(0, 0, 0)) || java.util.Set.of(cells).size() != cells.length) {
			throw new IllegalArgumentException("a footprint lists its main cell (0, 0, 0) first and each cell once");
		}
		return new Footprint(List.of(cells));
	}

	/** How many cells (pieces) the part fills. */
	public int pieces() {
		return cells.size();
	}

	public Cell cell(int piece) {
		return cells.get(piece);
	}

	/**
	 * Piece {@code piece}'s offset from the main cell for a part turned {@code quarterTurns} clockwise from north
	 * (0 north, 1 east, 2 south, 3 west), as {x, y, z}.
	 */
	public int[] offset(int piece, int quarterTurns) {
		Cell c = cells.get(piece);
		int dx = c.dx, dz = c.dz;
		for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return new int[] {dx, c.dy, dz};
	}

	/** The piece at an offset from the main cell for a part turned {@code quarterTurns}, or -1 if the part does not fill it. */
	public int pieceAt(int dx, int dy, int dz, int quarterTurns) {
		for (int piece = 0; piece < cells.size(); piece++) {
			int[] at = offset(piece, quarterTurns);
			if (at[0] == dx && at[1] == dy && at[2] == dz) {
				return piece;
			}
		}
		return -1;
	}
}
