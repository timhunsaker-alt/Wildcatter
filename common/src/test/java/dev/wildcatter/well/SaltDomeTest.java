package dev.wildcatter.well;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 15a: salt domes on their seed grid, their flank fields and caprock pools (docs/salt-dome-oil.md). */
class SaltDomeTest {
	private static SaltDome someDome(long seed) {
		for (int cx = 0; cx < 40; cx++) {
			SaltDome dome = SaltDome.inCell(seed, cx, 3);
			if (dome != null) {
				return dome;
			}
		}
		return null;
	}

	@Test
	void aDomeDependsOnlyOnTheSeed() {
		assertEquals(someDome(42), someDome(42));
	}

	@Test
	void aboutSixtyPercentOfCellsHoldOne() {
		int n = 0;
		for (int i = 0; i < 2000; i++) {
			n += SaltDome.inCell(7, i, -i) != null ? 1 : 0;
		}
		assertEquals(Tuning.DOME_CHANCE, n / 2000.0, 0.04);
	}

	@Test
	void flankFieldsAreArcsAgainstTheSaltThatDoNotOverlap() {
		for (long seed = 1; seed < 200; seed++) {
			SaltDome dome = someDome(seed);
			assertNotNull(dome);
			assertTrue(dome.flanks().size() >= Tuning.FLANK_MIN_FIELDS && dome.flanks().size() <= Tuning.FLANK_MAX_FIELDS);
			for (Reservoir f : dome.flanks()) {
				assertEquals(Reservoir.Source.FLANK, f.source());
				assertTrue(f.topY() >= Tuning.FLANK_MIN_Y && f.topY() <= Tuning.FLANK_MAX_Y);
				assertTrue(f.radius() - f.inner() >= Tuning.FLANK_MIN_WIDTH - 1e-9 && f.radius() - f.inner() <= Tuning.FLANK_MAX_WIDTH + 1e-9);
				int[] m = f.middle();
				assertTrue(f.covers(m[0], m[1]), "a flank field covers its own middle");
				assertTrue(f.edgeDistance(m[0] + 0.5, m[1] + 0.5) <= 0);
				assertFalse(f.covers(dome.centerX(), dome.centerZ()), "the dome's axis is salt, not field");
				// the field reaches in under the salt's edge, so its inner ring goes through rock salt
				assertTrue(f.inner() < dome.saltRadius(f.topY()));
				for (Reservoir g : dome.flanks()) {
					if (g != f) {
						assertFalse(g.covers(m[0], m[1]), "two fields of one dome do not overlap");
					}
				}
				// it stays inside its cell
				assertTrue(Math.floorDiv(m[0], Tuning.DOME_CELL) == dome.cellX() && Math.floorDiv(m[1], Tuning.DOME_CELL) == dome.cellZ());
			}
		}
	}

	@Test
	void reservoirAtFindsTheFlankFirstAndNothingUnderTheMiddleButTheCaprock() {
		for (long seed = 1; seed < 400; seed++) {
			SaltDome dome = someDome(seed);
			Reservoir flank = dome.flanks().get(0);
			int[] m = flank.middle();
			assertEquals(flank, Reservoir.at(seed, m[0], m[1]));
			assertEquals(dome.caprockPool(), Reservoir.at(seed, dome.centerX(), dome.centerZ()));
			// with the dome not there (another biome), the flank is just the ground's ordinary field or none
			Reservoir off = Reservoir.at(seed, m[0], m[1], d -> false);
			assertTrue(off == null || off.source() == Reservoir.Source.FIELD || off.source() == Reservoir.Source.DEEP);
		}
	}

	@Test
	void theSaltAndCapMatchTheShape() {
		SaltDome dome = someDome(11);
		assertTrue(dome.inSalt(dome.centerX(), -40, dome.centerZ()));
		assertTrue(dome.inSalt(dome.centerX(), dome.crestY(), dome.centerZ()));
		assertFalse(dome.inSalt(dome.centerX(), dome.crestY() + 1, dome.centerZ()));
		assertTrue(dome.inCap(dome.centerX(), dome.crestY() + 1, dome.centerZ()));
		assertFalse(dome.inSalt(dome.centerX() + Tuning.DOME_MAX_RADIUS + 4, -40, dome.centerZ()));
		assertTrue(dome.saltRadius(-55) > dome.saltRadius(-10), "a little wider low down");
		assertEquals(Tuning.MOUND_MAX_HEIGHT, dome.moundHeight(dome.centerX(), dome.centerZ()));
		assertEquals(0, dome.moundHeight(dome.centerX() + 20, dome.centerZ()));
	}

	@Test
	void aCaprockPoolFlowsFullThenFallsFast() {
		Reservoir pool = null;
		for (long seed = 1; pool == null; seed++) {
			SaltDome dome = someDome(seed);
			pool = dome.caprockPool();
		}
		assertTrue(pool.units() >= Tuning.CAPROCK_OIL_MIN_UNITS && pool.units() <= Tuning.CAPROCK_OIL_MAX_UNITS);
		assertNull(pool.arc() > 0 ? pool : null);
	}
}
