package dev.wildcatter.crane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Wind, ground, slews and the ring crane's rails (docs/questions/plant-crane-gaps.md, 1 to 4). */
class SitingTest {
	@Test
	void theWindRisesWithTheWeatherAndWithHeight() {
		assertEquals(4.0, Siting.windAt10(0, 0), 1e-9);
		assertEquals(8.0, Siting.windAt10(1, 0), 1e-9);
		assertEquals(18.0, Siting.windAt10(1, 1), 1e-9);
		assertEquals(8.0, Siting.windAt(8.0, 5), 1e-9);                       // below 10 m it is the 10 m wind
		assertEquals(8.0 * Math.pow(6.32, 1.0 / 7), Siting.windAt(8.0, 63.2), 1e-9);
	}

	@Test
	void rainStopsTheCrawlerAndTheRingOnALongBoomButNotTheTowerCrane() {
		double rain = Siting.windAt10(1, 0), storm = Siting.windAt10(1, 1), fair = Siting.windAt10(0, 0);
		double tower = CraneKind.TOWER.headHeight(0, 20);
		double crawler = CraneKind.CRAWLER.headHeight(CraneKind.CRAWLER.firstBoom(), 10);   // 32 m boom close in
		double ring = CraneKind.RING.headHeight(0, 60);                                      // 118 m boom at 60 m
		assertTrue(Siting.windAt(rain, tower) < CraneKind.TOWER_WIND_LIMIT);
		assertTrue(Siting.windAt(storm, tower) > CraneKind.TOWER_WIND_LIMIT);
		assertTrue(Siting.windAt(rain, crawler) > CraneKind.CRAWLER_WIND_LIMIT);
		assertTrue(Siting.windAt(fair, crawler) < CraneKind.CRAWLER_WIND_LIMIT);
		assertTrue(Siting.windAt(rain, ring) < CraneKind.RING_WIND_LIMIT, "the ring crane works on in rain: " + Siting.windAt(rain, ring));
		assertTrue(Siting.windAt(storm, ring) > CraneKind.RING_WIND_LIMIT);
	}

	@Test
	void eachGroundBearsWhatItsTableSays() {
		assertTrue(Siting.Ground.GRAVEL.bears(CraneKind.CRAWLER_GROUND_T_M2));
		assertFalse(Siting.Ground.SAND.bears(CraneKind.CRAWLER_GROUND_T_M2));
		assertFalse(Siting.Ground.CLAY.bears(CraneKind.CRAWLER_GROUND_T_M2));
		assertTrue(Siting.Ground.ROCK.bears(CraneKind.RING_GROUND_T_M2));
		assertFalse(Siting.Ground.SEDIMENTARY.bears(CraneKind.RING_GROUND_T_M2));
		assertEquals(0, CraneKind.TOWER.groundTonnesPerM2(), 1e-9);
	}

	@Test
	void aSlewIsSampledAtLeastEveryMetreAtItsReach() {
		double[][] s = Siting.sweep(new LiftCycle.Hook(350, 10, 0), new LiftCycle.Hook(20, 30, 0), 75);
		assertEquals(350, s[0][0], 1e-9);
		assertEquals(380, s[s.length - 1][0], 1e-9);                           // the short way round, through north
		assertEquals(30, s[s.length - 1][1], 1e-9);
		double step = Math.toRadians(30.0 / (s.length - 1)) * 75;
		assertTrue(step <= 1.0 + 1e-9, "a sample every " + step + " m at the tip");
	}

	@Test
	void theRingCraneTravelsToBringTheSiteInAndNotForAMetre() {
		double[] c = {0, 0};
		int[] south = {0, 1};
		double aim = 40 + Siting.RING_TRAVEL_MARGIN;
		// a site 150 m south: out of its 120 m reach; the rails run 200 m south
		assertEquals(100, Siting.bestStop(c, south, 0, 200, new double[] {0, 150}, aim, 120));
		// the rails end 60 m on: it goes as far as they let it, as that is nearer its aim
		assertEquals(60, Siting.bestStop(c, south, 0, 60, new double[] {0, 150}, aim, 120));
		// a site beside the rails, 51 m off: no stop is 2 m better
		assertEquals(0, Siting.bestStop(c, south, 50, 50, new double[] {51, 0}, aim, 120));
		// a site 30 m off the rails' line: the stop where it is 50 m away, the nearer of the two
		int k = Siting.bestStop(c, south, 100, 100, new double[] {30, 10}, aim, 120);
		assertEquals(50, Math.hypot(30, 10 - k), 0.6);
		assertTrue(k < 0, "the nearer of the two stops, north: " + k);
		assertEquals(3600, Siting.travelSeconds(Siting.RING_TRAVEL_M_H), 1e-9);
	}
}
