package dev.wildcatter.multiblock;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a multiblock, as written in its spec (docs/multiblocks): layers from the bottom up, each a grid of characters
 * read with north at the top and east to the right, '.' or ' ' for "anything". The grid is drawn for a controller facing NORTH;
 * {@link #cells(int)} turns it for the other three facings. Plain Java, so it is unit-tested without Minecraft.
 */
public final class MultiblockLayout {
	/** One required cell: its offset from the controller (x east, y up, z south) and the legend character it must match. */
	public record Cell(int dx, int dy, int dz, char part) {
	}

	private final List<Cell> north = new ArrayList<>();

	/**
	 * @param layers bottom layer first; each layer's rows run north to south, each row west to east
	 * @param controller the legend character of the controller; it must appear exactly once
	 */
	public MultiblockLayout(char controller, String[]... layers) {
		int cx = -1, cy = -1, cz = -1;
		for (int y = 0; y < layers.length; y++) {
			for (int z = 0; z < layers[y].length; z++) {
				String row = layers[y][z];
				for (int x = 0; x < row.length(); x++) {
					if (row.charAt(x) == controller) {
						if (cx >= 0) {
							throw new IllegalArgumentException("the controller '" + controller + "' appears more than once");
						}
						cx = x;
						cy = y;
						cz = z;
					}
				}
			}
		}
		if (cx < 0) {
			throw new IllegalArgumentException("no controller '" + controller + "' in the layout");
		}
		for (int y = 0; y < layers.length; y++) {
			for (int z = 0; z < layers[y].length; z++) {
				String row = layers[y][z];
				for (int x = 0; x < row.length(); x++) {
					char c = row.charAt(x);
					if (c != '.' && c != ' ') {
						north.add(new Cell(x - cx, y - cy, z - cz, c));
					}
				}
			}
		}
	}

	/**
	 * The cells for a controller facing {@code quarterTurns} clockwise from north (0 north, 1 east, 2 south, 3 west),
	 * the way Minecraft's horizontal facings go round.
	 */
	public List<Cell> cells(int quarterTurns) {
		List<Cell> out = new ArrayList<>(north.size());
		for (Cell cell : north) {
			int dx = cell.dx, dz = cell.dz;
			for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
				int t = dx;
				dx = -dz;
				dz = t;
			}
			out.add(new Cell(dx, cell.dy, dz, cell.part));
		}
		return out;
	}

	public int size() {
		return north.size();
	}
}
