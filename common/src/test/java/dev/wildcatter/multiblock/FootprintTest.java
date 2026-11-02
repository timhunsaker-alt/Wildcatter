package dev.wildcatter.multiblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FootprintTest {
	// the GSU: three cells across, three up, two deep (high side in front), main cell at the back of the bottom middle
	private static final Footprint GSU = Footprint.box(3, 3, 2, 1, 1);

	@Test
	void fillsTheWholeBoxOnceWithTheMainCellFirst() {
		assertEquals(18, GSU.pieces());
		assertEquals(new Footprint.Cell(0, 0, 0), GSU.cell(0));
		Set<Footprint.Cell> seen = new HashSet<>();
		for (int piece = 0; piece < GSU.pieces(); piece++) {
			Footprint.Cell c = GSU.cell(piece);
			assertEquals(true, seen.add(c), "each cell once");
			assertEquals(true, c.dx() >= -1 && c.dx() <= 1 && c.dy() >= 0 && c.dy() <= 2 && c.dz() >= -1 && c.dz() <= 0);
		}
	}

	@Test
	void turnsClockwiseLikeTheLayouts() {
		int front = GSU.pieceAt(0, 0, -1, 0);   // the cell in front of the main cell, facing north
		assertArrayEquals(new int[] {0, 0, -1}, GSU.offset(front, 0));
		assertArrayEquals(new int[] {1, 0, 0}, GSU.offset(front, 1), "facing east, the front is east");
		assertArrayEquals(new int[] {0, 0, 1}, GSU.offset(front, 2));
		assertArrayEquals(new int[] {-1, 0, 0}, GSU.offset(front, 3));
		assertArrayEquals(GSU.offset(front, 0), GSU.offset(front, 4));
	}

	@Test
	void pieceAndOffsetRoundTripInEveryFacing() {
		for (int turns = 0; turns < 4; turns++) {
			for (int piece = 0; piece < GSU.pieces(); piece++) {
				int[] at = GSU.offset(piece, turns);
				assertEquals(piece, GSU.pieceAt(at[0], at[1], at[2], turns));
			}
		}
		assertEquals(-1, GSU.pieceAt(0, 0, 1, 0), "behind the main cell is outside the part");
	}

	@Test
	void refusesAMainCellOutsideTheBox() {
		assertThrows(IllegalArgumentException.class, () -> Footprint.box(3, 1, 1, 3, 0));
		assertThrows(IllegalArgumentException.class, () -> Footprint.box(0, 1, 1, 0, 0));
	}

	@Test
	void aFootprintWithAGapLeavesItOut() {
		// a crossarm: its main cell west of the pole, its other cell east of it, the pole's cell left out
		Footprint arm = Footprint.of(new Footprint.Cell(0, 0, 0), new Footprint.Cell(2, 0, 0));
		assertEquals(2, arm.pieces());
		assertEquals(-1, arm.pieceAt(1, 0, 0, 0), "the pole's cell is not the arm's");
		assertArrayEquals(new int[] {0, 0, 2}, arm.offset(1, 1), "facing east, the arm runs north to south");
		assertThrows(IllegalArgumentException.class, () -> Footprint.of(new Footprint.Cell(1, 0, 0)));
		assertThrows(IllegalArgumentException.class, () -> Footprint.of(new Footprint.Cell(0, 0, 0), new Footprint.Cell(0, 0, 0)));
	}
}
