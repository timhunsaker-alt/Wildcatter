package dev.wildcatter.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The joining rules and three-phase behaviour of {@link Network}, with no Minecraft. */
class NetworkTest {
	private static final int A = 0;
	private static final int B = 1;
	private static final int C = 2;
	private static final int N = 3;
	private static final int ANY_PHASE = 0b111;
	private static final int EAST = 5;
	private static final int WEST = 4;
	private static final double[] DEGREES = {0, -120, 120};

	/** A 120 V three-phase supply in the cell at the origin, with A, B, C and N on its east face. Returns the source ids. */
	private static int[] supply(Network net, int[] hot, int neutral) {
		int[] sources = new int[3];
		for (int k = 0; k < 3; k++) {
			hot[k] = net.node();
			sources[k] = net.source(hot[k], neutral, 120, DEGREES[k]);
			net.port(new Network.Cell(0, 0, 0), EAST, k, 0, hot[k], 0.001, false);
		}
		net.port(new Network.Cell(0, 0, 0), EAST, N, 0, neutral, 0.001, false);
		net.ground(neutral);
		return sources;
	}

	@Test
	void singlePhaseLoadTakesOnePhaseAndNeverBridgesTwo() {
		Network net = new Network(0.01, 0.01);
		int[] hot = new int[3];
		int neutral = net.node();
		int[] sources = supply(net, hot, neutral);
		int load = net.node();
		int loadNeutral = net.node();
		int lamp = net.resistor(load, loadNeutral, 144);
		net.portAnyOf(new Network.Cell(1, 0, 0), WEST, ANY_PHASE, 0, load, 0.001, false);
		net.port(new Network.Cell(1, 0, 0), WEST, N, 0, loadNeutral, 0.001, false);
		assertTrue(net.solve());
		assertEquals(120.0 / 144.0, net.current(lamp), 0.01);
		assertEquals(120.0 / 144.0, net.sourceCurrent(sources[0]), 0.01);
		assertEquals(0.0, net.sourceCurrent(sources[1]), 1e-3, "phase B carries nothing: the load did not bridge A and B");
		assertEquals(0.0, net.sourceCurrent(sources[2]), 1e-3);
	}

	@Test
	void balancedStarLoadHasNoNeutralCurrent() {
		Network net = new Network(0.01, 0.01);
		int[] hot = new int[3];
		int neutral = net.node();
		int[] sources = supply(net, hot, neutral);
		int star = net.node();
		for (int k = 0; k < 3; k++) {
			net.resistor(hot[k], star, 10);
		}
		int tie = net.resistor(star, neutral, 0.01);
		assertTrue(net.solve());
		assertEquals(12.0, net.sourceCurrent(sources[1]), 0.05);
		assertEquals(0.0, net.current(tie), 0.01, "a balanced load returns nothing on the neutral");
		double watts = 0;
		for (int k = 0; k < 3; k++) {
			watts += net.sourceWatts(sources[k]);
		}
		assertEquals(3 * 120 * 12.0, watts, 5.0);
	}

	@Test
	void unbalancedLoadReturnsTheDifferenceOnTheNeutral() {
		Network net = new Network(0.01, 0.01);
		int[] hot = new int[3];
		int neutral = net.node();
		supply(net, hot, neutral);
		int star = net.node();
		net.resistor(hot[0], star, 10);
		net.resistor(hot[1], star, 10);
		int tie = net.resistor(star, neutral, 0.01);
		assertTrue(net.solve());
		// two equal currents 120 degrees apart add to one of the same size
		assertEquals(12.0, net.current(tie), 0.1);
	}

	@Test
	void lineToLineIsRootThreeTimesLineToNeutral() {
		Network net = new Network(0.01, 0.01);
		int[] hot = new int[3];
		int neutral = net.node();
		supply(net, hot, neutral);
		assertTrue(net.solve());
		assertEquals(120 * Math.sqrt(3), net.across(hot[0], hot[1]), 0.1);
	}

	@Test
	void starStarTransformerKeepsEachPhase() {
		Network net = new Network(0.01, 0.01);
		int[] hot = new int[3];
		int neutral = net.node();
		supply(net, hot, neutral);
		int lowNeutral = net.node();
		net.ground(lowNeutral);
		int[] low = new int[3];
		for (int k = 0; k < 3; k++) {
			low[k] = net.node();
			net.transformer(hot[k], neutral, low[k], lowNeutral, 0.5);
			net.resistor(low[k], lowNeutral, 100);
		}
		assertTrue(net.solve());
		for (int k = 0; k < 3; k++) {
			assertEquals(240.0, net.across(low[k], lowNeutral), 0.5);
			assertEquals(DEGREES[k], net.acrossPhasor(low[k], lowNeutral).degrees(), 0.1);
		}
	}

	@Test
	void transformerImpedanceSagsUnderLoad() {
		Network net = new Network(0.01, 0.01);
		int hot = net.node();
		net.source(hot, PhasorSolver.GROUND, 7200);
		int low = net.node();
		// 2.5 % of a 25 kVA, 7.2 kV unit seen from the high side
		double x = 0.025 * 7200 * 7200 / 25000;
		net.transformer(hot, PhasorSolver.GROUND, low, PhasorSolver.GROUND, 60, new Complex(0, x));
		net.resistor(low, PhasorSolver.GROUND, 120 * 120 / 25000.0);   // full load
		assertTrue(net.solve());
		double volts = net.voltage(low);
		assertTrue(volts < 120 && volts > 119.0, "a reactive 2.5 % impedance drops a resistive full load by well under 1 %: " + volts);
	}

	/**
	 * A delta-wye bank (the generator step-up): each primary winding between two lines, each secondary from a phase to a grounded
	 * neutral. With the turns ratio V(secondary to neutral) / V(primary line to line), the secondary is the rated voltage to
	 * neutral and leads the primary by 30 degrees.
	 */
	@Test
	void deltaWyeStepsUpAndShiftsThirtyDegrees() {
		Network net = new Network(0.01, 0.01);
		int[] hot = new int[3];
		int neutral = net.node();
		supply(net, hot, neutral);
		double primaryLineToLine = 120 * Math.sqrt(3);
		double secondaryToNeutral = 7200;
		int star = net.node();
		net.ground(star);
		int[] out = new int[3];
		for (int k = 0; k < 3; k++) {
			out[k] = net.node();
			net.transformer(out[k], star, hot[k], hot[(k + 1) % 3], secondaryToNeutral / primaryLineToLine);
			net.resistor(out[k], star, 1.0e6);   // a light load so every node is tied down
		}
		assertTrue(net.solve());
		for (int k = 0; k < 3; k++) {
			assertEquals(secondaryToNeutral, net.voltagePhasor(out[k]).abs(), 1.0, "phase " + k + " to neutral");
			double shift = net.voltagePhasor(out[k]).degrees() - net.voltagePhasor(hot[k]).degrees();
			shift = ((shift + 180) % 360 + 360) % 360 - 180;
			assertEquals(30.0, shift, 0.01, "phase " + k + " leads by 30 degrees");
		}
	}
}
