package dev.wildcatter.crane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 10c2: real speeds and what a lift takes (docs/multiblocks/tower-crane.md, crawler-crane.md). */
class LiftCycleTest {
	@Test
	void theTowerCranePicksTheFastestGearThatHoldsTheLoad() {
		assertEquals(36, CraneKind.TOWER.hoistMetresPerMinute(16), 1e-9);
		assertEquals(61 + 62 * 1.3 / 4.8, CraneKind.TOWER.hoistMetresPerMinute(7.5), 1e-9);   // gear 2 (77.8) beats gear 1's 71
		assertEquals(205, CraneKind.TOWER.hoistMetresPerMinute(0.5), 1e-9);
		assertEquals(0, CraneKind.TOWER.hoistMetresPerMinute(17), 1e-9);
	}

	@Test
	void theCrawlerReevesForItsLoadAndIsHeldToItsEngine() {
		assertEquals(1, CraneKind.partsOfLine(5));
		assertEquals(7, CraneKind.partsOfLine(91.8));   // 900 kN at 150 kN a part
		assertEquals(20, CraneKind.partsOfLine(300));
		assertEquals(138.0, CraneKind.CRAWLER.hoistMetresPerMinute(5), 1e-9);           // one part, the full rope speed
		assertEquals(0.75 * 390_000 / (91_800 * 9.81) * 60, CraneKind.CRAWLER.hoistMetresPerMinute(91.8), 1e-9);   // 19.5, just under 138 / 7
		double atRated = CraneKind.CRAWLER.hoistMetresPerMinute(300);
		assertEquals(0.75 * 390_000 / (300_000 * 9.81) * 60, atRated, 1e-9);   // the engine limits it below 138 / 20
		assertTrue(atRated > 5.9 && atRated < 6.0);
	}

	@Test
	void aLiftTakesTheSumOfItsStepsAtRealSpeeds() {
		LiftCycle.Hook at = new LiftCycle.Hook(0, 2.6, 0);
		LiftCycle.Hook pick = new LiftCycle.Hook(0, 2.6, 0);
		LiftCycle.Hook set = new LiftCycle.Hook(90, 30, 5);
		List<LiftCycle.Step> steps = LiftCycle.plan(CraneKind.TOWER, 0, at, pick, set, 20, 8.0);
		assertEquals(6, steps.size());
		double v = CraneKind.TOWER.hoistMetresPerMinute(8.0);
		assertEquals(20 / v * 60, steps.get(2).seconds(), 1e-9);                    // hoist 20 m
		assertEquals(Math.max(90 / 4.8, 27.4 / 95 * 60), steps.get(3).seconds(), 1e-9);   // slew a quarter at 0.8 rpm, trolley 27.4 m
		assertEquals(15 / v * 60, steps.get(4).seconds(), 1e-9);                    // lower 15 m
		assertEquals(30, steps.get(5).seconds(), 1e-9);
		assertEquals(Math.min(110_000, 8000 * 9.81 * v / 60 / 0.85), steps.get(2).watts(), 1e-6);
		assertEquals(0, steps.get(4).watts(), 1e-9);                                // lowering regenerates
	}

	@Test
	void theShorterWayRound() {
		assertEquals(-90, LiftCycle.turn(0, 270), 1e-9);
		assertEquals(20, LiftCycle.turn(350, 10), 1e-9);
	}

	@Test
	void theCrawlerBurnsMoreUnderLoad() {
		assertEquals(102 / 3600.0 * 0.15, CraneKind.litresPerSecond(0), 1e-12);
		assertEquals(102 / 3600.0, CraneKind.litresPerSecond(390_000), 1e-12);
	}
}
