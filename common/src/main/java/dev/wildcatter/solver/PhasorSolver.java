package dev.wildcatter.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AC steady-state circuit solver: modified nodal analysis with complex numbers (phasors, RMS values) at one frequency.
 * No Minecraft in here, so it is checked against textbook circuits on its own.
 *
 * Nothing in it is specific to three-phase: every conductor (A, B, C, N, G) is just a node, so balanced and unbalanced
 * three-phase, split-phase and single-phase circuits are all the same thing. A three-phase source is three sources
 * at 0, -120 and +120 degrees. Node 0 is the ground.
 *
 * Every source and transformer has a real impedance (as the real equipment does), so the equations are a plain
 * nodal admittance matrix. It is solved by banded LU after a reverse Cuthill-McKee reordering, which keeps wire networks
 * (long, thin, tree-like) close to linear in size. A 1,000-node network solves in a few milliseconds.
 */
public final class PhasorSolver {
	public static final int GROUND = 0;
	/** Stops floating pieces of circuit making the equations unsolvable: a tiny leak to ground at every node. */
	private static final double LEAK = 1e-12;
	private static final double MIN_OHMS = 1e-6;

	private enum Kind { IMPEDANCE, INDUCTOR, CAPACITOR }

	private record Branch(Kind kind, int a, int b, double value, Complex z) {
	}

	private record Source(int plus, int minus, Complex emf, Complex z) {
	}

	/** An ideal transformer of ratio {@code tap} (V_A = tap x V_B at no load) with its series impedance on the A side. */
	private record Transformer(int aPlus, int aMinus, int bPlus, int bMinus, Complex tap, Complex z) {
	}

	private double frequency = 60.0;
	private int nodeCount = 1;
	private final List<Branch> branches = new ArrayList<>();
	private final List<Source> sources = new ArrayList<>();
	private final List<Transformer> transformers = new ArrayList<>();

	private Complex[] voltage;
	private Complex[] sourceCurrent;

	public void frequency(double hertz) {
		this.frequency = hertz;
	}

	public int node() {
		return nodeCount++;
	}

	public int nodes() {
		return nodeCount;
	}

	/** A general impedance between two nodes. Current is measured from {@code a} to {@code b}. Returns its id. */
	public int impedance(int a, int b, Complex z) {
		branches.add(new Branch(Kind.IMPEDANCE, a, b, 0, guard(z)));
		return branches.size() - 1;
	}

	public int resistor(int a, int b, double ohms) {
		return impedance(a, b, new Complex(Math.max(ohms, MIN_OHMS), 0));
	}

	/** An inductor of {@code henries}: its reactance follows the frequency. */
	public int inductor(int a, int b, double henries) {
		branches.add(new Branch(Kind.INDUCTOR, a, b, henries, Complex.ZERO));
		return branches.size() - 1;
	}

	/** A capacitor of {@code farads}: its reactance follows the frequency. */
	public int capacitor(int a, int b, double farads) {
		branches.add(new Branch(Kind.CAPACITOR, a, b, farads, Complex.ZERO));
		return branches.size() - 1;
	}

	/** A voltage source (a generator or a supply): {@code emf} behind its own internal impedance {@code z}. */
	public int source(int plus, int minus, Complex emf, Complex z) {
		sources.add(new Source(plus, minus, emf, guard(z)));
		return sources.size() - 1;
	}

	/** A source of {@code volts} (RMS) at {@code degrees}, with a purely resistive internal impedance. */
	public int source(int plus, int minus, double volts, double degrees, double internalOhms) {
		return source(plus, minus, Complex.polar(volts, degrees), new Complex(Math.max(internalOhms, MIN_OHMS), 0));
	}

	/**
	 * A transformer: V_A = {@code tap} x V_B with no load, losing {@code z} in series on the A side. A complex tap is a phase
	 * shift. Delta-Y banks are built from three of these wired in a delta and a star.
	 */
	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, Complex tap, Complex z) {
		transformers.add(new Transformer(aPlus, aMinus, bPlus, bMinus, tap, guard(z)));
		return transformers.size() - 1;
	}

	private static Complex guard(Complex z) {
		return z.abs() < MIN_OHMS ? new Complex(MIN_OHMS, 0) : z;
	}

	private Complex impedanceOf(Branch br) {
		double w = 2 * Math.PI * frequency;
		return switch (br.kind) {
			case IMPEDANCE -> br.z;
			case INDUCTOR -> new Complex(MIN_OHMS, w * br.value);
			case CAPACITOR -> new Complex(MIN_OHMS, -1.0 / (w * br.value));
		};
	}

	/** Solves the circuit. Returns false if the numbers fell apart (they should not). */
	public boolean solve() {
		int n = nodeCount - 1;
		if (n == 0) {
			voltage = new Complex[] {Complex.ZERO};
			sourceCurrent = new Complex[sources.size()];
			java.util.Arrays.fill(sourceCurrent, Complex.ZERO);
			return true;
		}

		// the sparsity pattern, for the ordering
		List<Set<Integer>> adjacent = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			adjacent.add(new HashSet<>());
		}
		for (Branch br : branches) {
			link(adjacent, br.a, br.b);
		}
		for (Source s : sources) {
			link(adjacent, s.plus, s.minus);
		}
		for (Transformer t : transformers) {
			int[] ts = {t.aPlus, t.aMinus, t.bPlus, t.bMinus};
			for (int i = 0; i < 4; i++) {
				for (int j = i + 1; j < 4; j++) {
					link(adjacent, ts[i], ts[j]);
				}
			}
		}
		int[] order = reverseCuthillMcKee(adjacent, n);   // order[k] = the node (minus one) placed at row k
		int[] place = new int[n];
		for (int k = 0; k < n; k++) {
			place[order[k]] = k;
		}
		int band = 0;
		for (int i = 0; i < n; i++) {
			for (int j : adjacent.get(i)) {
				band = Math.max(band, Math.abs(place[i] - place[j]));
			}
		}

		Banded m = new Banded(n, band);
		for (int i = 0; i < n; i++) {
			m.add(i, i, LEAK, 0);
		}
		for (Branch br : branches) {
			Complex y = Complex.ONE.dividedBy(impedanceOf(br));
			stampAdmittance(m, place, br.a, br.b, y);
		}
		for (Source s : sources) {
			Complex y = Complex.ONE.dividedBy(s.z);
			stampAdmittance(m, place, s.plus, s.minus, y);
			Complex inject = s.emf.times(y);               // Norton equivalent: the source current with its terminals shorted
			if (s.plus > 0) {
				m.addRhs(place[s.plus - 1], inject.re(), inject.im());
			}
			if (s.minus > 0) {
				m.addRhs(place[s.minus - 1], -inject.re(), -inject.im());
			}
		}
		for (Transformer t : transformers) {
			Complex y = Complex.ONE.dividedBy(t.z);
			Complex ia = y;                                 // I_A = y V_A - y t V_B
			Complex ib = t.tap.conjugate().times(y).negate();   // I_B = -conj(t) y V_A + |t|^2 y V_B
			Complex ab = y.times(t.tap).negate();
			Complex bb = y.times(t.tap.re() * t.tap.re() + t.tap.im() * t.tap.im());
			stampPorts(m, place, t, ia, ab, ib, bb);
		}

		if (!m.factorAndSolve()) {
			return false;
		}
		voltage = new Complex[nodeCount];
		voltage[0] = Complex.ZERO;
		for (int i = 0; i < n; i++) {
			voltage[i + 1] = new Complex(m.xr[place[i]], m.xi[place[i]]);
		}
		sourceCurrent = new Complex[sources.size()];
		for (int k = 0; k < sources.size(); k++) {
			Source s = sources.get(k);
			sourceCurrent[k] = s.emf.minus(voltage[s.plus].minus(voltage[s.minus])).dividedBy(s.z);
		}
		return true;
	}

	private static void link(List<Set<Integer>> adjacent, int a, int b) {
		if (a > 0 && b > 0 && a != b) {
			adjacent.get(a - 1).add(b - 1);
			adjacent.get(b - 1).add(a - 1);
		}
	}

	private static void stampAdmittance(Banded m, int[] place, int a, int b, Complex y) {
		if (a > 0) {
			m.add(place[a - 1], place[a - 1], y.re(), y.im());
		}
		if (b > 0) {
			m.add(place[b - 1], place[b - 1], y.re(), y.im());
		}
		if (a > 0 && b > 0) {
			m.add(place[a - 1], place[b - 1], -y.re(), -y.im());
			m.add(place[b - 1], place[a - 1], -y.re(), -y.im());
		}
	}

	/** Current into a winding's + terminal (and out of its - terminal) = ca x V_A + cb x V_B. */
	private static void stampPorts(Banded m, int[] place, Transformer t, Complex aa, Complex ab, Complex ba, Complex bb) {
		int[] rows = {t.aPlus, t.aMinus, t.bPlus, t.bMinus};
		double[] sign = {1, -1, 1, -1};
		for (int r = 0; r < 4; r++) {
			if (rows[r] == 0) {
				continue;
			}
			Complex ca = r < 2 ? aa : ba;
			Complex cb = r < 2 ? ab : bb;
			int row = place[rows[r] - 1];
			if (t.aPlus > 0) {
				m.add(row, place[t.aPlus - 1], sign[r] * ca.re(), sign[r] * ca.im());
			}
			if (t.aMinus > 0) {
				m.add(row, place[t.aMinus - 1], -sign[r] * ca.re(), -sign[r] * ca.im());
			}
			if (t.bPlus > 0) {
				m.add(row, place[t.bPlus - 1], sign[r] * cb.re(), sign[r] * cb.im());
			}
			if (t.bMinus > 0) {
				m.add(row, place[t.bMinus - 1], -sign[r] * cb.re(), -sign[r] * cb.im());
			}
		}
	}

	private static int[] reverseCuthillMcKee(List<Set<Integer>> adjacent, int n) {
		int[] degree = new int[n];
		for (int i = 0; i < n; i++) {
			degree[i] = adjacent.get(i).size();
		}
		boolean[] seen = new boolean[n];
		int[] order = new int[n];
		int count = 0;
		while (count < n) {
			int start = -1;
			for (int i = 0; i < n; i++) {
				if (!seen[i] && (start < 0 || degree[i] < degree[start])) {
					start = i;
				}
			}
			int head = count;
			order[count++] = start;
			seen[start] = true;
			while (head < count) {
				int here = order[head++];
				List<Integer> next = new ArrayList<>();
				for (int j : adjacent.get(here)) {
					if (!seen[j]) {
						seen[j] = true;
						next.add(j);
					}
				}
				next.sort((p, q) -> Integer.compare(degree[p], degree[q]));
				for (int j : next) {
					order[count++] = j;
				}
			}
		}
		for (int i = 0, j = n - 1; i < j; i++, j--) {
			int swap = order[i];
			order[i] = order[j];
			order[j] = swap;
		}
		return order;
	}

	/** A complex banded matrix with its right-hand side, solved in place by LU without pivoting (the matrix is a diagonally strong admittance matrix). */
	private static final class Banded {
		private final int n;
		private final int band;
		private final int width;
		private final double[] re;
		private final double[] im;
		private final double[] rr;
		private final double[] ri;
		double[] xr;
		double[] xi;

		Banded(int n, int band) {
			this.n = n;
			this.band = band;
			this.width = 2 * band + 1;
			this.re = new double[n * width];
			this.im = new double[n * width];
			this.rr = new double[n];
			this.ri = new double[n];
		}

		void add(int row, int col, double r, double i) {
			int at = row * width + (col - row + band);
			re[at] += r;
			im[at] += i;
		}

		void addRhs(int row, double r, double i) {
			rr[row] += r;
			ri[row] += i;
		}

		boolean factorAndSolve() {
			for (int k = 0; k < n; k++) {
				int kk = k * width + band;
				double pr = re[kk];
				double pi = im[kk];
				double pm = pr * pr + pi * pi;
				if (pm < 1e-60) {
					return false;
				}
				int last = Math.min(n - 1, k + band);
				for (int i = k + 1; i <= last; i++) {
					int ik = i * width + (k - i + band);
					double ar = re[ik];
					double ai = im[ik];
					if (ar == 0 && ai == 0) {
						continue;
					}
					double fr = (ar * pr + ai * pi) / pm;
					double fi = (ai * pr - ar * pi) / pm;
					for (int j = k; j <= last; j++) {
						int ij = i * width + (j - i + band);
						int kj = k * width + (j - k + band);
						re[ij] -= fr * re[kj] - fi * im[kj];
						im[ij] -= fr * im[kj] + fi * re[kj];
					}
					rr[i] -= fr * rr[k] - fi * ri[k];
					ri[i] -= fr * ri[k] + fi * rr[k];
				}
			}
			xr = new double[n];
			xi = new double[n];
			for (int row = n - 1; row >= 0; row--) {
				double sr = rr[row];
				double si = ri[row];
				int last = Math.min(n - 1, row + band);
				for (int j = row + 1; j <= last; j++) {
					int rj = row * width + (j - row + band);
					sr -= re[rj] * xr[j] - im[rj] * xi[j];
					si -= re[rj] * xi[j] + im[rj] * xr[j];
				}
				int d = row * width + band;
				double dm = re[d] * re[d] + im[d] * im[d];
				xr[row] = (sr * re[d] + si * im[d]) / dm;
				xi[row] = (si * re[d] - sr * im[d]) / dm;
			}
			return true;
		}
	}

	/** Phasor volts at a node. */
	public Complex voltage(int node) {
		return voltage[node];
	}

	/** Phasor volts across two nodes. */
	public Complex across(int plus, int minus) {
		return voltage[plus].minus(voltage[minus]);
	}

	/** Phasor current through a branch from its {@code a} end to its {@code b} end. */
	public Complex current(int branchId) {
		Branch br = branches.get(branchId);
		return voltage[br.a].minus(voltage[br.b]).dividedBy(impedanceOf(br));
	}

	/** Phasor current a source pushes out of its + terminal into the circuit. */
	public Complex sourceCurrent(int sourceId) {
		return sourceCurrent[sourceId];
	}

	/** Phasor current into winding A's + terminal of a transformer, and out of its - terminal. */
	public Complex transformerCurrentA(int transformerId) {
		Transformer t = transformers.get(transformerId);
		Complex va = voltage[t.aPlus].minus(voltage[t.aMinus]);
		Complex vb = voltage[t.bPlus].minus(voltage[t.bMinus]);
		return va.minus(t.tap.times(vb)).dividedBy(t.z);
	}

	/** Phasor current into winding B's + terminal of a transformer, and out of its - terminal. */
	public Complex transformerCurrentB(int transformerId) {
		return transformerCurrentA(transformerId).times(transformers.get(transformerId).tap.conjugate()).negate();
	}

	/** Complex power a source delivers, in VA: S = V I*. Real part is watts, imaginary part is vars. */
	public Complex sourcePower(int sourceId) {
		Source s = sources.get(sourceId);
		return voltage[s.plus].minus(voltage[s.minus]).times(sourceCurrent[sourceId].conjugate());
	}
}
