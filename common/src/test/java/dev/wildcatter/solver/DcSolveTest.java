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
