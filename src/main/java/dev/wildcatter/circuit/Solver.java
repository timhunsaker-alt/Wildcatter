package dev.wildcatter.circuit;

import java.util.ArrayList;
import java.util.List;

/**
 * A small DC circuit solver (modified nodal analysis). No Minecraft in here, so it can be checked against textbook
 * circuits on its own.
 *
 * Build a circuit out of nodes, resistors, voltage sources and ideal transformers, call {@link #solve()}, then read
 * the voltage at any node and the current through any resistor or source. Node 0 is the ground (0 V).
 *
 * The numbers are "RMS volts": there is no frequency or phase, so this behaves like DC. That is the right level for
 * Ohm's law, power, series and parallel, breakers and shorts.
 */
public final class Solver {
	/** Stops floating pieces of circuit making the equations unsolvable: a tiny leak to ground at every node. */
	private static final double LEAK = 1e-12;

	private record Resistor(int a, int b, double ohms) {
	}

	private record Source(int plus, int minus, double volts) {
	}

	private record Transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio) {
	}

	private int nodeCount = 1; // node 0 is ground
	private final List<Resistor> resistors = new ArrayList<>();
	private final List<Source> sources = new ArrayList<>();
	private final List<Transformer> transformers = new ArrayList<>();

	private double[] voltage;
	private double[] sourceCurrent;
	private double[] transformerCurrent;

	public static final int GROUND = 0;

	public int node() {
		return nodeCount++;
	}

	public int nodes() {
		return nodeCount;
	}

	/** Adds a resistor and returns its id. Current is measured from {@code a} to {@code b}. */
	public int resistor(int a, int b, double ohms) {
		resistors.add(new Resistor(a, b, Math.max(ohms, 1e-6)));
		return resistors.size() - 1;
	}

	/** An ideal voltage source holding {@code plus} at {@code volts} above {@code minus}. Returns its id. */
	public int source(int plus, int minus, double volts) {
		sources.add(new Source(plus, minus, volts));
		return sources.size() - 1;
	}

	/**
	 * An ideal transformer. The voltage across winding A is {@code ratio} times the voltage across winding B, and
	 * the currents are the other way round, so no power is made or lost. Put resistors in series for real losses.
	 */
	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio) {
		transformers.add(new Transformer(aPlus, aMinus, bPlus, bMinus, ratio));
		return transformers.size() - 1;
	}

	/** Solves the circuit. Returns false if the numbers fell apart (they should not). */
	public boolean solve() {
		int unknownNodes = nodeCount - 1;                 // everything but ground
		int size = unknownNodes + sources.size() + transformers.size();
		double[][] m = new double[size][size + 1];        // the last column is the right-hand side

		for (int n = 1; n < nodeCount; n++) {
			m[n - 1][n - 1] += LEAK;
		}
		for (Resistor r : resistors) {
			double g = 1.0 / r.ohms;
			stampConductance(m, r.a, r.b, g);
		}
		for (int k = 0; k < sources.size(); k++) {
			Source s = sources.get(k);
			int col = unknownNodes + k;
			// The unknown is the current leaving the source's + terminal into the circuit.
			add(m, s.plus, col, -1);
			add(m, s.minus, col, 1);
			if (s.plus > 0) {
				m[col][s.plus - 1] += 1;
			}
			if (s.minus > 0) {
				m[col][s.minus - 1] -= 1;
			}
			m[col][size] = s.volts;
		}
		for (int k = 0; k < transformers.size(); k++) {
			Transformer t = transformers.get(k);
			int col = unknownNodes + sources.size() + k;
			// The unknown is the current entering winding A's + terminal; winding B then carries -ratio times that.
			add(m, t.aPlus, col, 1);
			add(m, t.aMinus, col, -1);
			add(m, t.bPlus, col, -t.ratio);
			add(m, t.bMinus, col, t.ratio);
			// V(a+) - V(a-) - ratio * (V(b+) - V(b-)) = 0
			if (t.aPlus > 0) {
				m[col][t.aPlus - 1] += 1;
			}
			if (t.aMinus > 0) {
				m[col][t.aMinus - 1] -= 1;
			}
			if (t.bPlus > 0) {
				m[col][t.bPlus - 1] -= t.ratio;
			}
			if (t.bMinus > 0) {
				m[col][t.bMinus - 1] += t.ratio;
			}
		}

		double[] x = gauss(m, size);
		if (x == null) {
			return false;
		}
		voltage = new double[nodeCount];
		for (int n = 1; n < nodeCount; n++) {
			voltage[n] = x[n - 1];
		}
		sourceCurrent = new double[sources.size()];
		for (int k = 0; k < sources.size(); k++) {
			sourceCurrent[k] = x[unknownNodes + k];
		}
		transformerCurrent = new double[transformers.size()];
		for (int k = 0; k < transformers.size(); k++) {
			transformerCurrent[k] = x[unknownNodes + sources.size() + k];
		}
		return true;
	}

	private static void add(double[][] m, int node, int col, double value) {
		if (node > 0) {
			m[node - 1][col] += value;
		}
	}

	private static void stampConductance(double[][] m, int a, int b, double g) {
		if (a > 0) {
			m[a - 1][a - 1] += g;
		}
		if (b > 0) {
			m[b - 1][b - 1] += g;
		}
		if (a > 0 && b > 0) {
			m[a - 1][b - 1] -= g;
			m[b - 1][a - 1] -= g;
		}
	}

	/** Gaussian elimination with partial pivoting. */
	private static double[] gauss(double[][] m, int n) {
		for (int col = 0; col < n; col++) {
			int pivot = col;
			for (int row = col + 1; row < n; row++) {
				if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
					pivot = row;
				}
			}
			if (Math.abs(m[pivot][col]) < 1e-18) {
				return null;
			}
			double[] swap = m[col];
			m[col] = m[pivot];
			m[pivot] = swap;
			for (int row = col + 1; row < n; row++) {
				double factor = m[row][col] / m[col][col];
				if (factor == 0) {
					continue;
				}
				for (int k = col; k <= n; k++) {
					m[row][k] -= factor * m[col][k];
				}
			}
		}
		double[] x = new double[n];
		for (int row = n - 1; row >= 0; row--) {
			double sum = m[row][n];
			for (int k = row + 1; k < n; k++) {
				sum -= m[row][k] * x[k];
			}
			x[row] = sum / m[row][row];
		}
		return x;
	}

	public double voltage(int node) {
		return voltage[node];
	}

	/** Current through a resistor, from its {@code a} end to its {@code b} end. */
	public double current(int resistorId) {
		Resistor r = resistors.get(resistorId);
		return (voltage[r.a] - voltage[r.b]) / r.ohms;
	}

	/** Current a source pushes out of its + terminal into the circuit. */
	public double sourceCurrent(int sourceId) {
		return sourceCurrent[sourceId];
	}

	public double transformerCurrent(int transformerId) {
		return transformerCurrent[transformerId];
	}
}
