package dev.wildcatter.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HostEnergyTest {
	@Test
	void anIntervalIsHandedOverEvenly() {
		long[] ticks = new long[4];
		for (int t = 0; t < 4; t++) {
			ticks[t] = HostEnergy.perTick(1000, 4, t);
		}
		assertEquals(250, ticks[0]);
		assertEquals(250, ticks[3]);
	}

	@Test
	void theRemainderIsNotLost() {
		long sum = 0;
		for (int t = 0; t < 4; t++) {
			long share = HostEnergy.perTick(1003, 4, t);
			assertEquals(t < 3 ? 251 : 250, share);
			sum += share;
		}
		assertEquals(1003, sum);
	}
}
