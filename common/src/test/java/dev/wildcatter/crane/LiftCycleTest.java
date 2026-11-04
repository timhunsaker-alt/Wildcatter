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
	void theRingCraneReevesInSixesAndSlewsSixDegreesAMinute() {
		assertEquals(6, CraneKind.ringPartsOfLine(288.2));                // a reactor vessel and the hook block: 2,827 kN, under 6 x 536
		assertEquals(12, CraneKind.ringPartsOfLine(400));                 // 3,924 kN: 8 lines, rounded up to 12
		assertEquals(60, CraneKind.ringPartsOfLine(3200));
		assertEquals(20.0, CraneKind.RING.hoistMetresPerMinute(288.2), 1e-9);   // 120 m/min on 6 parts
		assertEquals(2.0, CraneKind.RING.hoistMetresPerMinute(3200), 1e-9);     // on 60 parts, under its 3.72 MW
		assertEquals(30 * 60, CraneKind.RING.slewSeconds(180), 1e-9);            // a half turn in 30 min
		assertEquals(104.6, CraneKind.RING.hookBlockTonnes(), 1e-9);
		// the 118 m boom at 40 m stands at about 80 degrees, its hook about 104 m up
		assertEquals(Math.toDegrees(Math.acos(22.0 / 118)), CraneKind.RING.boomAngle(0, 40), 1e-9);
		double h = CraneKind.RING.hookHeight(0, 40);
		assertTrue(h > 100 && h < 106, "hook height " + h);
		assertEquals(975 / 3600.0, CraneKind.RING.dieselPerSecond(3_720_000), 1e-12);
	}

	@Test
	void aTandemLiftMovesAtTheSlowerCranesTimes() {
		LiftCycle.Hook at = new LiftCycle.Hook(0, 6, 2);
		LiftCycle.Hook pickA = new LiftCycle.Hook(0, 6, 1), setA = new LiftCycle.Hook(30, 16, 1);
		LiftCycle.Hook pickB = new LiftCycle.Hook(200, 18, 1), setB = new LiftCycle.Hook(170, 15, 1);
		double share = LiftCycle.tandemShare(CraneKind.CRAWLER, 91.8);
		assertEquals(45.9, share, 1e-9);
		List<LiftCycle.Step> a = LiftCycle.plan(CraneKind.CRAWLER, 3, at, pickA, setA, 12, share);
		List<LiftCycle.Step> b = LiftCycle.plan(CraneKind.CRAWLER, 3, new LiftCycle.Hook(200, 6, 2), pickB, setB, 12, share);
		LiftCycle.Tandem t = LiftCycle.together(a, b);
		for (int i = 0; i < a.size(); i++) {
			assertEquals(Math.max(a.get(i).seconds(), b.get(i).seconds()), t.lead().get(i).seconds(), 1e-9);
			assertEquals(t.lead().get(i).seconds(), t.follower().get(i).seconds(), 1e-9);
			assertEquals(b.get(i).to(), t.follower().get(i).to());       // each keeps its own hook path
		}
		assertEquals(LiftCycle.seconds(t.lead()), LiftCycle.seconds(t.follower()), 1e-9);
		assertEquals(0.5 * 91.8 + 104.6, LiftCycle.tandemShare(CraneKind.RING, 91.8), 1e-9);
	}

	@Test
	void theCrawlerBurnsMoreUnderLoad() {
		assertEquals(102 / 3600.0 * 0.15, CraneKind.litresPerSecond(0), 1e-12);
		assertEquals(102 / 3600.0, CraneKind.litresPerSecond(390_000), 1e-12);
	}
}
