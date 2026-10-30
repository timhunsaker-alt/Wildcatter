package dev.wildcatter.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One connected group of electrical parts as a circuit, with no knowledge of Minecraft. Parts add nodes and
 * components, and place terminals (ports) on the faces of cells in a 3D grid; {@link #solve} joins the terminals of
 * neighbouring cells and solves everything with {@link Solver}.
 *
 * Joining rules: two ports face each other across a cell edge and have the same voltage key. Same role key: they are
 * joined through the resistance of the wire. Different role keys: they only touch if both are bare metal, and then
 * it is a short circuit.
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

	/** One terminal on one face of a cell. {@code halfOhms} is this side's share of the resistance of a joint. */
	private record Port(Cell cell, int face, int role, int voltage, int node, double halfOhms, boolean exposed) {
	}

	private final Solver solver = new Solver();
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

	public int source(int plus, int minus, double volts) {
		return solver.source(plus, minus, volts);
	}

	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio) {
		return solver.transformer(aPlus, aMinus, bPlus, bMinus, ratio);
	}

	/** Bonds a node to the ground (0 V). */
	public void ground(int node) {
		groundTies.add(solver.resistor(node, Solver.GROUND, groundTieOhms));
	}

	/** Puts a terminal on {@code face} of {@code cell}. {@code exposed} means bare metal that can short against bare metal of another role. */
	public void port(Cell cell, int face, int role, int voltage, int node, double halfOhms, boolean exposed) {
		ports.computeIfAbsent(cell, k -> new ArrayList<>()).add(new Port(cell, face, role, voltage, node, halfOhms, exposed));
	}

	/** Joins the terminals of neighbouring cells, then solves. */
	public boolean solve() {
		for (List<Port> here : ports.values()) {
			for (Port p : here) {
				Cell neighbour = p.cell.relative(p.face);
				if (p.cell.compareTo(neighbour) >= 0) {
					continue; // each pair of faces is joined once, from the lower position
				}
				List<Port> there = ports.get(neighbour);
				if (there == null) {
					continue;
				}
				for (Port q : there) {
					if (q.face != (p.face ^ 1) || q.voltage != p.voltage) {
						continue;
					}
					if (q.role == p.role) {
						resistor(p.node, q.node, p.halfOhms + q.halfOhms);
					} else if (p.exposed && q.exposed) {
						resistor(p.node, q.node, shortContactOhms);
					}
				}
			}
		}
		return solver.solve();
	}

	public double voltage(int node) {
		return solver.voltage(node);
	}

	/** Volts across two nodes. */
	public double across(int plus, int minus) {
		return solver.voltage(plus) - solver.voltage(minus);
	}

	public double current(int resistorId) {
		return solver.current(resistorId);
	}

	public double sourceCurrent(int sourceId) {
		return solver.sourceCurrent(sourceId);
	}

	/** How much current is passing through a node: half the sum of everything flowing in and out of it. */
	public double through(int node) {
		double sum = 0;
		for (int id : resistorsAt.getOrDefault(node, List.of())) {
			if (!groundTies.contains(id)) {
				sum += Math.abs(solver.current(id));
			}
		}
		return sum / 2.0;
	}
}
