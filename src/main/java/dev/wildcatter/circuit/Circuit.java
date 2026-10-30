package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The Minecraft side of a circuit: parts talk to it with block positions and directions, and it hands the work to
 * the pure {@link Network}. See there for the joining rules.
 */
public final class Circuit {
	private final Network network = new Network(Tuning.GROUND_TIE_OHMS, Tuning.SHORT_CONTACT_OHMS);

	private static Network.Cell cell(BlockPos pos) {
		return new Network.Cell(pos.getX(), pos.getY(), pos.getZ());
	}

	public int node() {
		return network.node();
	}

	public int nodes() {
		return network.nodes();
	}

	public int resistor(int a, int b, double ohms) {
		return network.resistor(a, b, ohms);
	}

	public int source(int plus, int minus, double volts) {
		return network.source(plus, minus, volts);
	}

	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio) {
		return network.transformer(aPlus, aMinus, bPlus, bMinus, ratio);
	}

	/** Bonds a node to the ground (0 V). Neutral conductors are tied to ground where the power comes in. */
	public void ground(int node) {
		network.ground(node);
	}

	/**
	 * Puts a terminal on {@code face} of the block at {@code pos}. {@code exposed} means bare metal that can touch
	 * the bare metal of a neighbour of the other role (a short circuit).
	 */
	public void port(BlockPos pos, Direction face, Role role, Voltage voltage, int node, double halfOhms, boolean exposed) {
		network.port(cell(pos), face.ordinal(), role.ordinal(), voltage.ordinal(), node, halfOhms, exposed);
	}

	/** Joins the terminals of neighbouring parts, then solves. */
	public boolean solve() {
		return network.solve();
	}

	public double voltage(int node) {
		return network.voltage(node);
	}

	/** Volts across two nodes. */
	public double across(int plus, int minus) {
		return network.across(plus, minus);
	}

	public double current(int resistorId) {
		return network.current(resistorId);
	}

	public double sourceCurrent(int sourceId) {
		return network.sourceCurrent(sourceId);
	}

	/** How much current is passing through a node: half the sum of everything flowing in and out of it. */
	public double through(int node) {
		return network.through(node);
	}
}
