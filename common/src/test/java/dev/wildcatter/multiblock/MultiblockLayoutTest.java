package dev.wildcatter.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MultiblockLayoutTest {
	// the genset spec's skid: fuel tank, engine, alternator, control panel in a row; radiator in front of the engine; exhaust over the panel
	private static final MultiblockLayout GENSET = new MultiblockLayout('K',
			new String[] {"FEAK", ".R.."},
			new String[] {"...X"});

	private static boolean has(List<MultiblockLayout.Cell> cells, int dx, int dy, int dz, char part) {
		return cells.contains(new MultiblockLayout.Cell(dx, dy, dz, part));
	}

	@Test
	void northIsTheGridAsDrawn() {
		List<MultiblockLayout.Cell> cells = GENSET.cells(0);
		assertEquals(6, cells.size());
		assertTrue(has(cells, -3, 0, 0, 'F'), "fuel tank three to the west of the panel");
		assertTrue(has(cells, -2, 0, 1, 'R'), "radiator one row south of the engine");
		assertTrue(has(cells, 0, 1, 0, 'X'), "exhaust on top of the panel");
		assertTrue(has(cells, 0, 0, 0, 'K'));
	}

	@Test
	void turnsClockwiseWithTheFacing() {
		// facing east: what was west of the panel is now north of it, and what was south is now west
		List<MultiblockLayout.Cell> east = GENSET.cells(1);
		assertTrue(has(east, 0, 0, -3, 'F'));
		assertTrue(has(east, -1, 0, -2, 'R'));
		List<MultiblockLayout.Cell> south = GENSET.cells(2);
		assertTrue(has(south, 3, 0, 0, 'F'));
		assertTrue(has(south, 2, 0, -1, 'R'));
		List<MultiblockLayout.Cell> west = GENSET.cells(3);
		assertTrue(has(west, 0, 0, 3, 'F'));
		assertTrue(has(west, 1, 0, 2, 'R'));
		assertEquals(GENSET.cells(0), GENSET.cells(4), "four quarter turns come back to north");
	}

	@Test
	void needsExactlyOneController() {
		assertThrows(IllegalArgumentException.class, () -> new MultiblockLayout('K', new String[] {"FEA"}));
		assertThrows(IllegalArgumentException.class, () -> new MultiblockLayout('K', new String[] {"KEK"}));
	}
}
