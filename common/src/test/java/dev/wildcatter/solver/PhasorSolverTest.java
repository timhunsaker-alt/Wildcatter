package dev.wildcatter.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Textbook circuits, checked against hand calculation. */
class PhasorSolverTest {
	private static final double TINY = 1e-6;

	@Test
	void ohmsLawAndPower() {
		PhasorSolver s = new PhasorSolver();
		int a = s.node();
		int src = s.source(a, PhasorSolver.GROUND, 120, 0, TINY);
		int load = s.resistor(a, PhasorSolver.GROUND, 12);
		assertTrue(s.solve());
		assertEquals(10.0, s.current(load).abs(), 1e-4);
		assertEquals(1200.0, s.sourcePower(src).re(), 0.1);
		assertEquals(0.0, s.sourcePower(src).im(), 0.1);
	}

	@Test
	void seriesAndParallel() {
		PhasorSolver s = new PhasorSolver();
		int a = s.node();
		int b = s.node();
		s.source(a, PhasorSolver.GROUND, 100, 0, TINY);
		int r1 = s.resistor(a, b, 10);
		s.resistor(b, PhasorSolver.GROUND, 20);
		s.resistor(b, PhasorSolver.GROUND, 20);     // 20 || 20 = 10, so 100 V over 10 + 10 = 5 A
		assertTrue(s.solve());
		assertEquals(5.0, s.current(r1).abs(), 1e-4);
		assertEquals(50.0, s.voltage(b).abs(), 1e-3);
	}

	@Test
	void inductiveLoadLagsAndPowerFactor() {
		// 3 ohm resistor + 4 ohm reactance at 60 Hz: |Z| = 5, 120 V gives 24 A lagging 53.13 degrees, PF 0.6
		PhasorSolver s = new PhasorSolver();
		s.frequency(60);
		int a = s.node();
		int mid = s.node();
		int src = s.source(a, PhasorSolver.GROUND, 120, 0, TINY);
		s.resistor(a, mid, 3);
		s.inductor(mid, PhasorSolver.GROUND, 4.0 / (2 * Math.PI * 60));
		assertTrue(s.solve());
		Complex i = s.sourceCurrent(src);
		assertEquals(24.0, i.abs(), 1e-3);
		assertEquals(-53.13, i.degrees(), 0.01);
		Complex power = s.sourcePower(src);
		assertEquals(0.6, power.re() / power.abs(), 1e-4);
	}

	@Test
	void reactanceFollowsFrequency() {
		double c = 1e-3;
		double[] amps = new double[2];
		double[] hz = {50, 60};
		for (int k = 0; k < 2; k++) {
			PhasorSolver s = new PhasorSolver();
			s.frequency(hz[k]);
			int a = s.node();
			int src = s.source(a, PhasorSolver.GROUND, 100, 0, TINY);
			s.capacitor(a, PhasorSolver.GROUND, c);
			assertTrue(s.solve());
			amps[k] = s.sourceCurrent(src).abs();
			assertEquals(100 * 2 * Math.PI * hz[k] * c, amps[k], 1e-3 * amps[k]);
		}
		assertTrue(amps[1] > amps[0]);
	}

	/** A wye source of the given line-to-line voltage: returns node ids a, b, c and the neutral. */
	private static int[] wye(PhasorSolver s, double lineToLine, double sourceOhms) {
		double ln = lineToLine / Math.sqrt(3);
		int a = s.node();
		int b = s.node();
		int c = s.node();
		int n = s.node();
		s.source(a, n, ln, 0, sourceOhms);
		s.source(b, n, ln, -120, sourceOhms);
		s.source(c, n, ln, 120, sourceOhms);
		s.resistor(n, PhasorSolver.GROUND, 0.001);
		return new int[] {a, b, c, n};
	}

	@Test
	void balancedThreePhaseHasNoNeutralCurrent() {
		PhasorSolver s = new PhasorSolver();
		int[] w = wye(s, 480, TINY);
		int star = s.node();
		int la = s.resistor(w[0], star, 10);
		s.resistor(w[1], star, 10);
		s.resistor(w[2], star, 10);
		int neutral = s.resistor(star, w[3], 0.001);
		assertTrue(s.solve());
		// 277.1 V across 10 ohms: 27.71 A per line; P = sqrt(3) x 480 x 27.71 = 23.04 kW
		assertEquals(480 / Math.sqrt(3) / 10, s.current(la).abs(), 1e-3);
		assertEquals(0.0, s.current(neutral).abs(), 1e-3);
		assertEquals(Math.sqrt(3) * 480 * (480 / Math.sqrt(3) / 10), 3 * Math.pow(s.current(la).abs(), 2) * 10, 1.0);
		assertEquals(480.0, s.across(w[0], w[1]).abs(), 1e-3);   // line to line = sqrt(3) x line to neutral
	}

	@Test
	void unbalancedThreePhaseDrivesNeutralCurrent() {
		PhasorSolver s = new PhasorSolver();
		int[] w = wye(s, 480, TINY);
		int star = s.node();
		int la = s.resistor(w[0], star, 10);
		int lb = s.resistor(w[1], star, 20);
		s.resistor(w[2], star, 1e9);                 // phase C open
		int neutral = s.resistor(star, w[3], 1e-3);
		assertTrue(s.solve());
		double ln = 480 / Math.sqrt(3);
		Complex expected = Complex.polar(ln / 10, 0).plus(Complex.polar(ln / 20, -120));   // what the neutral must carry back
		assertEquals(expected.abs(), s.current(neutral).abs(), 0.02);
	}

	@Test
	void splitPhaseNeutralCarriesTheDifference() {
		PhasorSolver s = new PhasorSolver();
		int l1 = s.node();
		int l2 = s.node();
		int n = s.node();
		int loadNeutral = s.node();
		s.source(l1, n, 120, 0, TINY);
		s.source(l2, n, 120, 180, TINY);
		s.resistor(n, PhasorSolver.GROUND, 0.001);
		int neutral = s.resistor(loadNeutral, n, 0.001);
		int a = s.resistor(l1, loadNeutral, 6);      // 20 A
		int b = s.resistor(l2, loadNeutral, 12);     // 10 A
		s.resistor(l1, l2, 24);                       // 240 V load, 10 A, no neutral current
		assertTrue(s.solve());
		assertEquals(240.0, s.across(l1, l2).abs(), 1e-3);
		assertEquals(20.0, s.current(a).abs(), 0.01);
		assertEquals(10.0, s.current(b).abs(), 0.01);
		assertEquals(10.0, s.current(neutral).abs(), 0.01);   // the legs are 180 degrees apart, so the neutral carries 20 - 10
	}

	@Test
	void deltaWyeBankShiftsThirtyDegrees() {
		// 480 V delta primary to 208Y/120 secondary: three single-phase transformers
		PhasorSolver s = new PhasorSolver();
		int[] w = wye(s, 480, TINY);
		int a2 = s.node();
		int b2 = s.node();
		int c2 = s.node();
		int star = s.node();
		double ratio = 480.0 / (208.0 / Math.sqrt(3));
		Complex z = new Complex(0.01, 0.02);
		s.transformer(w[0], w[1], a2, star, new Complex(ratio, 0), z);   // winding AB feeds phase a
		s.transformer(w[1], w[2], b2, star, new Complex(ratio, 0), z);
		s.transformer(w[2], w[0], c2, star, new Complex(ratio, 0), z);
		s.resistor(star, PhasorSolver.GROUND, 0.001);                     // secondary neutral is grounded
		s.resistor(a2, star, 100);
		s.resistor(b2, star, 100);
		s.resistor(c2, star, 100);
		assertTrue(s.solve());
		assertEquals(120.09, s.voltage(a2).minus(s.voltage(star)).abs(), 0.3);
		assertEquals(208.0, s.across(a2, b2).abs(), 0.5);
		double shift = s.across(a2, star).degrees() - s.across(w[0], w[3]).degrees();
		assertEquals(30.0, shift, 0.5);
	}

	@Test
	void powerIsConserved() {
		PhasorSolver s = new PhasorSolver();
		int[] w = wye(s, 480, 0.05);
		int star = s.node();
		s.impedance(w[0], star, new Complex(8, 6));
		s.impedance(w[1], star, new Complex(12, 3));
		s.impedance(w[2], star, new Complex(9, -2));
		s.resistor(star, w[3], 0.5);
		assertTrue(s.solve());
		// every watt a source makes goes into resistors: compare with I^2 R over all branches
		double made = 0;
		for (int k = 0; k < 3; k++) {
			made += s.sourcePower(k).re();
		}
		double used = 0;
		for (int id = 0; id < 5; id++) {
			double r = new double[] {0.001, 8, 12, 9, 0.5}[id];
			used += Math.pow(s.current(id).abs(), 2) * r;
		}
		// sourcePower is measured at the source terminals, so the loss inside each source is not in it
		assertEquals(made, used, made * 1e-4);
	}

	/** ELECTRICAL_MODEL: a 1,000-node network must solve in under 5 ms. */
	@Test
	void thousandNodeNetworkSolvesInUnderFiveMilliseconds() {
		long best = Long.MAX_VALUE;
		for (int run = 0; run < 30; run++) {
			PhasorSolver s = new PhasorSolver();
			int[][] line = new int[250][4];
			for (int[] cell : line) {
				for (int c = 0; c < 4; c++) {
					cell[c] = s.node();
				}
			}
			for (int c = 0; c < 3; c++) {
				s.source(line[0][c], line[0][3], 277.1, new double[] {0, -120, 120}[c], 0.01);
			}
			s.resistor(line[0][3], PhasorSolver.GROUND, 0.001);
			for (int i = 1; i < line.length; i++) {
				for (int c = 0; c < 4; c++) {
					s.impedance(line[i - 1][c], line[i][c], new Complex(0.01, 0.005));
				}
				if (i % 10 == 0) {
					for (int c = 0; c < 3; c++) {
						s.resistor(line[i][c], line[i][3], 500);
					}
				}
			}
			assertEquals(1001, s.nodes());
			long t0 = System.nanoTime();
			assertTrue(s.solve());
			best = Math.min(best, System.nanoTime() - t0);
		}
		System.out.println("1,000-node three-phase solve, best of 30: " + best / 1e6 + " ms");
		assertTrue(best < 5_000_000L, "took " + best / 1e6 + " ms");
	}
}
