package dev.wildcatter.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One connected group of electrical parts as a circuit, with no knowledge of Minecraft. Parts add nodes and
 * components, and place terminals (ports) on the faces of cells in a 3D grid; {@link #solve} joins the terminals of
 * neighbouring cells and solves everything with {@link PhasorSolver} (AC phasors at one frequency).
 *
 * Joining rules: two ports face each other across a cell edge and have the same voltage key. Same role key: they are
 * joined through the resistance of the wire. Different role keys: they only touch if both are bare metal, and then
 * it is a short circuit. A port can also take any one of several roles (a single-phase load takes whichever phase is
 * there): it joins the first matching port across that edge and no other, so it never bridges two phases.
 *
 * Faces are numbered 0 to 5 in the order DOWN, UP, NORTH, SOUTH, WEST, EAST, so the opposite of face {@code f} is {@code f ^ 1}.
 */
public final class Network {
	private static final int[] DX = {0, 0, 0, 0, -1, 1};
	private static final int[] DY = {-1, 1, 0, 0, 0, 0};
	private static final int[] DZ = {0, 0, -1, 1, 0, 0};

	/** A position in the grid. */
	public record Cell(int x, int y, int z) implements Comparable<Cell> {
		Cell relative(int face) {
			return new Cell(x + DX[face], y + DY[face], z + DZ[face]);
		}

		@Override
		public int compareTo(Cell o) {
			int c = Integer.compare(x, o.x);
			if (c == 0) {
				c = Integer.compare(y, o.y);
			}
			return c != 0 ? c : Integer.compare(z, o.z);
		}
	}

	/** One terminal on one face of a cell. {@code roles} is a bit mask of the role keys it takes. {@code halfOhms} is this side's share of the resistance of a joint. */
	private record Port(Cell cell, int face, int roles, int voltage, int node, double halfOhms, boolean exposed) {
		boolean anyOf() {
			return Integer.bitCount(roles) > 1;
		}
	}

	/** The internal impedance of an ideal source and the series impedance of an ideal transformer: small enough to be ideal, big enough to solve. */
	private static final double IDEAL_OHMS = 1e-6;

	private final PhasorSolver solver = new PhasorSolver();
	private final Map<Cell, List<Port>> ports = new HashMap<>();
	private final Map<Integer, List<Integer>> resistorsAt = new HashMap<>();
	private final List<Integer> groundTies = new ArrayList<>();
	private final double groundTieOhms;
	private final double shortContactOhms;

	public Network(double groundTieOhms, double shortContactOhms) {
		this.groundTieOhms = groundTieOhms;
		this.shortContactOhms = shortContactOhms;
	}

	public int node() {
		return solver.node();
	}

	public int nodes() {
		return solver.nodes();
	}

	public int resistor(int a, int b, double ohms) {
		int id = solver.resistor(a, b, ohms);
		resistorsAt.computeIfAbsent(a, k -> new ArrayList<>()).add(id);
		resistorsAt.computeIfAbsent(b, k -> new ArrayList<>()).add(id);
		return id;
	}

	/** An inductor: its reactance follows the frequency (motor windings). */
	public int inductor(int a, int b, double henries) {
		int id = solver.inductor(a, b, henries);
		resistorsAt.computeIfAbsent(a, k -> new ArrayList<>()).add(id);
		resistorsAt.computeIfAbsent(b, k -> new ArrayList<>()).add(id);
		return id;
	}

	/** A general impedance (ohms resistance and reactance at the circuit's frequency). */
	public int impedance(int a, int b, Complex z) {
		int id = solver.impedance(a, b, z);
		resistorsAt.computeIfAbsent(a, k -> new ArrayList<>()).add(id);
		resistorsAt.computeIfAbsent(b, k -> new ArrayList<>()).add(id);
		return id;
	}

	/** The frequency the circuit is solved at. */
	public void frequency(double hertz) {
		solver.frequency(hertz);
	}

	/** An ideal source of {@code volts} (RMS) at 0 degrees. */
	public int source(int plus, int minus, double volts) {
		return source(plus, minus, volts, 0);
	}

	/** An ideal source of {@code volts} (RMS) at {@code degrees}: three of them at 0, -120 and +120 make a three-phase supply. */
	public int source(int plus, int minus, double volts, double degrees) {
		return solver.source(plus, minus, volts, degrees, IDEAL_OHMS);
	}

	/** An ideal transformer: the voltage across winding A is {@code ratio} times the voltage across winding B. */
	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio) {
		return transformer(aPlus, aMinus, bPlus, bMinus, ratio, Complex.ZERO);
	}

	/** A transformer with its leakage impedance {@code z} (ohms, seen from winding A). */
	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio, Complex z) {
		return solver.transformer(aPlus, aMinus, bPlus, bMinus, new Complex(ratio, 0), z.abs() < IDEAL_OHMS ? new Complex(IDEAL_OHMS, 0) : z);
	}

	/** Bonds a node to the ground (0 V). */
	public void ground(int node) {
		groundTies.add(resistor(node, PhasorSolver.GROUND, groundTieOhms));
	}

	/** Puts a terminal on {@code face} of {@code cell}. {@code exposed} means bare metal that can short against bare metal of another role. */
	public void port(Cell cell, int face, int role, int voltage, int node, double halfOhms, boolean exposed) {
		portAnyOf(cell, face, 1 << role, voltage, node, halfOhms, exposed);
	}

	/** A terminal that takes any one of the roles in the bit mask {@code roles}: it joins the first one it finds across the edge. */
	public void portAnyOf(Cell cell, int face, int roles, int voltage, int node, double halfOhms, boolean exposed) {
		ports.computeIfAbsent(cell, k -> new ArrayList<>()).add(new Port(cell, face, roles, voltage, node, halfOhms, exposed));
	}

	/** Joins the terminals of neighbouring cells, then solves. */
	public boolean solve() {
		for (Map.Entry<Cell, List<Port>> entry : ports.entrySet()) {
			Cell cell = entry.getKey();
			for (int face = 0; face < 6; face++) {
				Cell neighbour = cell.relative(face);
				if (cell.compareTo(neighbour) >= 0) {
					continue; // each pair of faces is joined once, from the lower position
				}
				List<Port> there = ports.get(neighbour);
				if (there == null) {
					continue;
				}
				java.util.Set<Port> used = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
				for (Port p : entry.getValue()) {
					if (p.face != face) {
						continue;
					}
					for (Port q : there) {
						if (q.face != (face ^ 1) || q.voltage != p.voltage) {
							continue;
						}
						if ((q.roles & p.roles) != 0) {
							if ((p.anyOf() && used.contains(p)) || (q.anyOf() && used.contains(q))) {
								continue;
							}
							used.add(p);
							used.add(q);
							resistor(p.node, q.node, p.halfOhms + q.halfOhms);
						} else if (p.exposed && q.exposed) {
							resistor(p.node, q.node, shortContactOhms);
						}
					}
				}
			}
		}
		return solver.solve();
	}

	/** Volts at a node (the size of the phasor). */
	public double voltage(int node) {
		return solver.voltage(node).abs();
	}

	public Complex voltagePhasor(int node) {
		return solver.voltage(node);
	}

	/** Volts across two nodes (the size of the phasor difference). */
	public double across(int plus, int minus) {
		return solver.across(plus, minus).abs();
	}

	public Complex acrossPhasor(int plus, int minus) {
		return solver.across(plus, minus);
	}

	/** Amps through a branch (the size of the phasor). */
	public double current(int branchId) {
		return solver.current(branchId).abs();
	}

	public Complex currentPhasor(int branchId) {
		return solver.current(branchId);
	}

	/** Amps out of a source (the size of the phasor). */
	public double sourceCurrent(int sourceId) {
		return solver.sourceCurrent(sourceId).abs();
	}

	/** Amps into winding A of a transformer (the size of the phasor). */
	public double transformerCurrent(int transformerId) {
		return solver.transformerCurrentA(transformerId).abs();
	}

	public Complex sourceCurrentPhasor(int sourceId) {
		return solver.sourceCurrent(sourceId);
	}

	/** Real power a source delivers, in watts (negative if it is being driven). */
	public double sourceWatts(int sourceId) {
		return solver.sourcePower(sourceId).re();
	}

	/** How much current is passing through a node: half the sum of everything flowing in and out of it. */
	public double through(int node) {
		double sum = 0;
		for (int id : resistorsAt.getOrDefault(node, List.of())) {
			if (!groundTies.contains(id)) {
				sum += solver.current(id).abs();
			}
		}
		return sum / 2.0;
	}
}
