package dev.wildcatter.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** DC on the phasor solver (docs/multiblocks/dc-bus.md): resistors and EMFs at 0 degrees, down to nano-ohm bus. */
class DcSolveTest {
	private static final double IDEAL = 1e-6;   // Network's internal impedance of a source

	@Test
	void aPotlineStringAtOneHundredEightyKiloampsSolves() {
		Network net = new Network(0.01, 0.02);
		int pots = 40;
		double backEmf = 1.65;
		double potOhms = (4.2 - backEmf) / 180_000;   // 4.2 V a pot at 180 kA
		double busOhms = 0.07e-6;                       // a metre of potline aluminium bus
		int metres = 20;                                // of bus between two pots
		int plus = net.node();
		int minus = net.node();
		double emf = 4.2 * pots + 2.0;
		int rectifier = net.source(plus, minus, emf);
		int at = plus;
		for (int p = 0; p < pots; p++) {
			for (int m = 0; m < metres; m++) {
				int next = net.node();
				net.resistor(at, next, busOhms);
				at = next;
			}
			int inside = net.node();
			net.resistor(at, inside, potOhms);
			int out = net.node();
			net.source(inside, out, backEmf);   // the pot's back-EMF: a source charged by the line current
			at = out;
		}
		net.resistor(at, minus, busOhms);
		// the ground-fault detector's balancing resistors hold the string to earth
		net.resistor(plus, PhasorSolver.GROUND, 10_000);
		net.resistor(minus, PhasorSolver.GROUND, 10_000);
		assertTrue(net.solve());
		double ohms = IDEAL + pots * (potOhms + IDEAL + metres * busOhms) + busOhms;
		double expected = (emf - pots * backEmf) / ohms;
		double amps = net.sourceCurrent(rectifier);
		assertEquals(expected, amps, expected * 1e-3, "the series current to 0.1 %");
		assertTrue(amps > 150_000 && amps < 220_000, "about 180 kA: " + amps);
		// with no fault each pole sits at half the line voltage from earth
		assertEquals(emf / 2, net.voltagePhasor(plus).re(), 0.5);
		assertEquals(-emf / 2, net.voltagePhasor(minus).re(), 0.5);
	}

	/** 40 pots of 1.65 V and 13.1 µΩ with 1.67 µΩ risers, on {@code units} rectiformers of 37 µΩ at {@code emf}; one pot's back-EMF {@code ae}. */
	private static double[] potline(int units, double emf, double ae) {
		Network net = new Network(0.01, 0.02);
		double potOhms = (4.0 - 1.65) / 180_000;
		double riser = 0.3 / 180_000;
		int plus = net.node();
		int minus = net.node();
		int[] sources = new int[units];
		for (int u = 0; u < units; u++) {
			int inside = net.node();
			sources[u] = net.source(inside, minus, emf);
			net.resistor(inside, plus, 2.2 / 60_000);
		}
		int at = plus;
		for (int p = 0; p < 40; p++) {
			int a = net.node();
			net.resistor(at, a, riser);
			int inside = net.node();
			net.resistor(a, inside, potOhms);
			int out = net.node();
			net.source(inside, out, p == 0 ? ae : 1.65);
			at = out;
		}
		net.resistor(at, minus, 30 * 0.063e-6);   // 30 m of 180 kA aluminium bus back to the yard
		net.resistor(plus, PhasorSolver.GROUND, 10_000);
		net.resistor(minus, PhasorSolver.GROUND, 10_000);
		assertTrue(net.solve());
		double[] out = new double[units + 1];
		for (int u = 0; u < units; u++) {
			out[u] = net.sourceCurrent(sources[u]);
			out[units] += out[u];
		}
		return out;
	}

	@Test
	void fourRectiformersShareTheLineEqually() {
		double emf = 40 * 4.3 + 2.0;
		double[] four = potline(4, emf, 1.65);
		assertTrue(four[4] > 160_000 && four[4] < 200_000, "about 180 kA: " + four[4]);
		for (int u = 0; u < 4; u++) {
			assertEquals(four[4] / 4, four[u], four[4] * 1e-6, "unit " + u + " carries a quarter");
		}
		// one trips: at the same EMF the other three carry a little more of a slightly lower line current, until they ramp up
		double[] three = potline(3, emf, 1.65);
		assertTrue(three[3] < four[4] && three[3] > four[4] * 0.95, "the line hardly falls: " + three[3]);
		assertEquals(three[3] / 3, three[0], three[3] * 1e-6);
	}

	@Test
	void anAnodeEffectNeedsTwentySixVoltsMore() {
		double emf = 40 * 4.3 + 2.0;
		double normal = potline(4, emf, 1.65)[4];
		double effect = potline(4, emf, 27.65)[4];
		assertTrue(effect < normal * 0.8, "at the same EMF an anode effect drops the line: " + effect);
		double held = potline(4, emf + 26.0, 27.65)[4];
		assertEquals(normal, held, normal * 0.01, "26 V more holds it");
		assertTrue(emf + 26.0 < 220, "inside the rectiformers' 220 V");
	}

	@Test
	void aGroundFaultPutsTheOtherPoleAtFullVoltage() {
		Network net = new Network(0.01, 0.02);
		int plus = net.node();
		int minus = net.node();
		net.source(plus, minus, 150);
		net.resistor(plus, PhasorSolver.GROUND, 10_000);
		net.resistor(minus, PhasorSolver.GROUND, 10_000);
		net.ground(minus);   // the minus bus touches earth
		assertTrue(net.solve());
		assertEquals(150, net.voltagePhasor(plus).re(), 0.5);
		assertEquals(0, net.voltagePhasor(minus).re(), 0.5);
	}
}
