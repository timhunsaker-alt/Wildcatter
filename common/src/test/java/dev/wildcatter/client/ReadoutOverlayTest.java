package dev.wildcatter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The long readout's hold (ReadoutOverlay): counted in game time, 50 ms a tick, so a slow or paused game holds it as long in play. */
class ReadoutOverlayTest {
	@Test
	void gameTimeInMilliseconds() {
		assertEquals(0, ReadoutOverlay.gameMs(0, 0.0F));
		assertEquals(1000, ReadoutOverlay.gameMs(20, 0.0F));
		assertEquals(1017, ReadoutOverlay.gameMs(20, 1.0F / 3));
		assertEquals(1033, ReadoutOverlay.gameMs(20, 2.0F / 3));
	}

	@Test
	void holdGrowsWithLengthUpToTwelveSeconds() {
		assertEquals(2000, ReadoutOverlay.holdMs(0));
		assertEquals(10000, ReadoutOverlay.holdMs(200));
		assertEquals(12000, ReadoutOverlay.holdMs(1000));
	}
}
