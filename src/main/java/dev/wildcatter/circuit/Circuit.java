package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * One connected group of electrical parts, as a circuit. Parts add their own nodes and components, and say where
 * their terminals are ({@link #port}); then {@link #solve} joins the terminals of neighbouring parts and solves
 * everything with {@link Solver}.
 *
 * Joining rules: two terminals face each other across a block edge and have the same voltage. Same role (hot to
 * hot): they are connected through the resistance of the wire. Different roles (hot to neutral): they only touch
 * if both are bare metal, and then it is a short circuit.
 */
public final class Circuit {
	/** One terminal on one face of a block. {@code halfOhms} is this side's share of the resistance of a joint. */
	private record Port(BlockPos pos, Direction face, Role role, Voltage voltage, int node, double halfOhms, boolean exposed) {
	}

	private final Solver solver = new Solver();
	private final Map<BlockPos, List<Port>> ports = new HashMap<>();
	private final Map<Integer, List<Integer>> resistorsAt = new HashMap<>();
	private final List<Integer> groundTies = new ArrayList<>();
	private boolean solved;

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

	/** Bonds a node to the ground (0 V). Neutral conductors are tied to ground where the power comes in. */
	public void ground(int node) {
		groundTies.add(solver.resistor(node, Solver.GROUND, Tuning.GROUND_TIE_OHMS));
	}

	/**
	 * Puts a terminal on {@code face} of the block at {@code pos}. {@code exposed} means bare metal that can touch
	 * the bare metal of a neighbour of the other role (a short circuit).
	 */
	public void port(BlockPos pos, Direction face, Role role, Voltage voltage, int node, double halfOhms, boolean exposed) {
		ports.computeIfAbsent(pos, k -> new ArrayList<>()).add(new Port(pos, face, role, voltage, node, halfOhms, exposed));
	}

	/** Joins the terminals of neighbouring parts, then solves. */
	public boolean solve() {
		for (List<Port> here : ports.values()) {
			for (Port p : here) {
				BlockPos neighbour = p.pos.relative(p.face);
				if (p.pos.asLong() >= neighbour.asLong()) {
					continue; // each pair of faces is joined once, from the lower position
				}
				List<Port> there = ports.get(neighbour);
				if (there == null) {
					continue;
				}
				for (Port q : there) {
					if (q.face != p.face.getOpposite() || q.voltage != p.voltage) {
						continue;
					}
					if (q.role == p.role) {
						resistor(p.node, q.node, p.halfOhms + q.halfOhms);
					} else if (p.exposed && q.exposed) {
						resistor(p.node, q.node, Tuning.SHORT_CONTACT_OHMS);
					}
				}
			}
		}
		solved = solver.solve();
		return solved;
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
