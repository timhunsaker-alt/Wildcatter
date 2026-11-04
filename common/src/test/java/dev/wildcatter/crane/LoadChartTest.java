package dev.wildcatter.crane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 10c2: the real charts, read at the farther radius (docs/multiblocks/tower-crane.md, crawler-crane.md). */
class LoadChartTest {
	@Test
	void theTowerCraneCarries16TonnesTo19Point6AndThreeAtItsTip() {
		LoadChart chart = LoadChart.tower();
		assertEquals(16.0, chart.capacity(2.6), 1e-9);
		assertEquals(16.0, chart.capacity(19.6), 1e-9);
		assertEquals(15.66, chart.capacity(19.7), 1e-9);   // the farther radius's figure: 20 m
		assertEquals(9.83, chart.capacity(26), 1e-9);      // read at 30 m
		assertEquals(3.00, chart.capacity(75), 1e-9);
		assertEquals(0, chart.capacity(75.5), 1e-9);
		assertEquals(0, chart.capacity(2.0), 1e-9);
	}

	@Test
	void theCrawlerLifts300TonnesOnlyOnAShortBoomCloseIn() {
		assertEquals(286.2, LoadChart.crawler(0).capacity(4.3), 1e-9);    // 20 m boom
		assertEquals(300.5, LoadChart.crawler(1).capacity(5), 1e-9);      // 23 m boom
		assertFalse(LoadChart.crawler(1).reaches(4.3));
		assertEquals(154.0, LoadChart.crawler(3).capacity(10), 1e-9);     // 32 m boom: a GSU 60 at 10 m
		assertEquals(67.8, LoadChart.crawler(14).capacity(10), 1e-9);     // 95 m boom
		assertEquals(2.6, LoadChart.crawler(12).capacity(80), 1e-9);      // 86 m boom at 80 m
	}

	@Test
	void aBoomReachesNoFartherThanFifteenDegrees() {
		LoadChart twenty = LoadChart.crawler(0);
		assertEquals(20.0, twenty.maxRadius(), 1e-9);                     // its last figure, inside 1.5 + 20 cos 15 = 20.8
		LoadChart twentyThree = LoadChart.crawler(1);
		assertEquals(1.5 + 23 * Math.cos(Math.toRadians(15)), twentyThree.maxRadius(), 1e-9);   // short of its 24 m figure
		assertTrue(twentyThree.reaches(23.5));
		assertFalse(twentyThree.reaches(24));
	}
}
