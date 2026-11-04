package dev.wildcatter.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 10c1: blueprints (docs/blueprints.md): the format, turning, the lift list and its pallets. */
class BlueprintTest {
	/** A 10 x 4 x 6 plant: a 3 x 2 x 2 part on the floor, a 2 x 1 x 2 part resting on it, a stone floor of 100 blocks under the rest. */
	private static Blueprint sample() {
		List<Blueprint.Block> blocks = new ArrayList<>();
		for (int x = 0; x < 10; x++) {
			for (int z = 0; z < 6; z++) {
				if (x >= 3 || z >= 2) {
					blocks.add(new Blueprint.Block(x, 0, z, 0));   // stone floor round the first part
				}
			}
		}
		blocks.add(new Blueprint.Block(9, 1, 5, 1));               // a cable on the floor
		List<Blueprint.Part> parts = List.of(
				new Blueprint.Part("lower", 0, 0, 0, 0, 0, 0, 0, 2, 1, 1),
				new Blueprint.Part("upper", 1, 1, 2, 0, 0, 2, 0, 1, 2, 1));
		return new Blueprint("sample", 10, 4, 6, List.of("minecraft:stone", "wildcatter:cable_mv"), blocks, parts, Map.of("9,1,5", "MV_500"));
	}

	@Test
	void itSurvivesJsonAndItsCompressedForm() {
		Blueprint b = sample();
		Blueprint back = Blueprint.decompress(b.compressed());
		assertEquals(b.toJson(), back.toJson());
		assertEquals(55, back.blocks().size());
		assertEquals("MV_500", back.size(9, 1, 5));
		assertEquals("upper", back.parts().get(1).id());
	}

	@Test
	void turningMovesEverythingRound() {
		Blueprint b = sample();
		Blueprint east = b.turned(1);
		assertEquals(6, east.sizeX());
		assertEquals(10, east.sizeZ());
		// the north-west corner block (3, 0, 0) of the floor goes to the north-east side: (6 - 1 - 0, 0, 3)
		assertTrue(east.blocks().stream().anyMatch(k -> k.x() == 5 && k.z() == 3 && k.y() == 0));
		Blueprint.Part lower = east.parts().get(0);
		assertEquals(1, lower.turns());
		assertEquals(5, lower.x());
		assertEquals(0, lower.z());
		assertEquals(4, lower.x0());
		assertEquals(5, lower.x1());
		assertEquals(0, lower.z0());
		assertEquals(2, lower.z1());
		assertEquals("MV_500", east.size(0, 1, 9));
		// four quarter turns come back to the start
		assertEquals(b.toJson(), b.turned(4).toJson());
		assertEquals(b.turned(1).turned(1).toJson(), b.turned(2).toJson());
	}

	@Test
	void theLiftsGoBottomUpPartsFirst() {
		Blueprint b = sample();
		List<Blueprint.Lift> lifts = b.lifts(new double[] {2.4, 0.1}, new double[] {30, 5}, 16);
		assertTrue(lifts.get(0).isPart(), "the floor layer's part goes first");
		assertEquals(0, lifts.get(0).part());
		int upper = -1;
		int lastFloor = -1;
		int total = 0;
		for (int i = 0; i < lifts.size(); i++) {
			Blueprint.Lift l = lifts.get(i);
			if (l.isPart() && l.part() == 1) {
				upper = i;
			}
			if (!l.isPart()) {
				total += l.blocks().size();
				assertTrue(l.blocks().size() <= Blueprint.PALLET_BLOCKS);
				assertTrue(l.tonnes() <= 16 + 1e-9, "a pallet stays within its cap: " + l.tonnes());
				if (l.y() == 0) {
					lastFloor = i;
				}
			}
		}
		assertEquals(55, total, "every block goes up once");
		assertTrue(upper > lastFloor, "the part resting on the first comes after the floor layer");
		// stone at 2.4 t a block: a 16 t pallet holds 6 blocks
		assertEquals(6, lifts.get(1).blocks().size());
	}

	@Test
	void aPalletKeepsToOneSquare() {
		Blueprint b = sample();
		for (Blueprint.Lift l : b.lifts(new double[] {0.1, 0.1}, new double[] {30, 5}, 100)) {
			if (!l.isPart()) {
				int sx = b.blocks().get(l.blocks().get(0)).x() / Blueprint.PALLET_AREA;
				int sz = b.blocks().get(l.blocks().get(0)).z() / Blueprint.PALLET_AREA;
				for (int i : l.blocks()) {
					assertEquals(sx, b.blocks().get(i).x() / Blueprint.PALLET_AREA);
					assertEquals(sz, b.blocks().get(i).z() / Blueprint.PALLET_AREA);
				}
			}
		}
	}
}
