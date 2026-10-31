package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Complex;
import dev.wildcatter.solver.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The Minecraft side of a circuit: parts talk to it with block positions and directions, and it hands the work to
 * the pure {@link Network}. See there for the joining rules.
 */
public final class Circuit {
	private final Network network = new Network(Tuning.GROUND_TIE_OHMS, Tuning.SHORT_CONTACT_OHMS);
	private double hertz = dev.wildcatter.energy.ElectricalStandard.active().hertz();

	/** The frequency of the island this circuit is part of. */
	public double hertz() {
		return hertz;
	}

	public void setHertz(double hertz) {
		this.hertz = hertz;
		network.frequency(hertz);
	}

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

	/** An impedance of {@code ohms} resistance and {@code reactance} ohms at the circuit's frequency. */
	public int impedance(int a, int b, double ohms, double reactance) {
		return network.impedance(a, b, new Complex(ohms, reactance));
	}

	public int source(int plus, int minus, double volts) {
		return network.source(plus, minus, volts);
	}

	/** A source at an angle: a three-phase generator is three of these at 0, -120 and +120 degrees. */
	public int source(int plus, int minus, double volts, double degrees) {
		return network.source(plus, minus, volts, degrees);
	}

	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio) {
		return network.transformer(aPlus, aMinus, bPlus, bMinus, ratio);
	}

	/** A transformer with its leakage impedance, {@code ohms} + j {@code reactance} seen from winding A. */
	public int transformer(int aPlus, int aMinus, int bPlus, int bMinus, double ratio, double ohms, double reactance) {
		return network.transformer(aPlus, aMinus, bPlus, bMinus, ratio, new Complex(ohms, reactance));
	}

	/** Bonds a node to the ground (0 V). Neutral conductors are tied to ground where the power comes in. */
	public void ground(int node) {
		network.ground(node);
	}

	/**
	 * Puts a terminal on {@code face} of the block at {@code pos}. {@code exposed} means bare metal that can touch
	 * the bare metal of a neighbour of the other role (a short circuit).
	 */
	public void port(BlockPos pos, Direction face, Conductor role, Voltage voltage, int node, double halfOhms, boolean exposed) {
		network.port(cell(pos), face.ordinal(), role.ordinal(), voltage.ordinal(), node, halfOhms, exposed);
	}

	/** The voltage key of the hidden bus inside a motor control center: only MCC sections join on it. */
	private static final int BUS = 64;

	/** A terminal on the internal bus of a motor control center, on a face where two sections touch. Nothing else joins it. */
	public void busPort(BlockPos pos, Direction face, Conductor conductor, int node) {
		network.port(cell(pos), face.ordinal(), conductor.ordinal(), BUS, node, Tuning.CONTACT_OHMS / 2, false);
	}

	/** The phases of a hot conductor: a single-phase load or a single-pole breaker takes whichever one is on that face. */
	private static final int ANY_PHASE = (1 << Conductor.A.ordinal()) | (1 << Conductor.B.ordinal()) | (1 << Conductor.C.ordinal());

	/** A hot terminal that connects to whichever one phase (A, B or C) meets it on {@code face}, and never to two at once. */
	public void hotPort(BlockPos pos, Direction face, Voltage voltage, int node, double halfOhms, boolean exposed) {
		network.portAnyOf(cell(pos), face.ordinal(), ANY_PHASE, voltage.ordinal(), node, halfOhms, exposed);
	}

	private final java.util.Map<BlockPos, Integer> endpoints = new java.util.HashMap<>();
	private final java.util.List<Object[]> spans = new java.util.ArrayList<>();

	/** A part says which node an overhead span attaches to. */
	public void publish(BlockPos pos, int node) {
		endpoints.put(pos.immutable(), node);
	}

	/** An overhead span between two published endpoints, with the resistance of its length. */
	public void span(BlockPos a, BlockPos b, double ohms) {
		spans.add(new Object[] {a.immutable(), b.immutable(), ohms});
	}

	/** Joins the terminals of neighbouring parts and the ends of spans, then solves. */
	public boolean solve() {
		for (Object[] span : spans) {
			Integer a = endpoints.get((BlockPos) span[0]);
			Integer b = endpoints.get((BlockPos) span[1]);
			if (a != null && b != null) {
				network.resistor(a, b, (Double) span[2]);
			}
		}
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

	/** Real power a source delivers, in watts. */
	public double sourceWatts(int sourceId) {
		return network.sourceWatts(sourceId);
	}

	/** Amps into a transformer's winding A (its high side). */
	public double transformerCurrent(int transformerId) {
		return network.transformerCurrent(transformerId);
	}

	public Complex sourceCurrentPhasor(int sourceId) {
		return network.sourceCurrentPhasor(sourceId);
	}

	public Complex voltagePhasor(int node) {
		return network.voltagePhasor(node);
	}

	public Complex acrossPhasor(int plus, int minus) {
		return network.acrossPhasor(plus, minus);
	}

	public Complex currentPhasor(int branchId) {
		return network.currentPhasor(branchId);
	}

	/** How much current is passing through a node: half the sum of everything flowing in and out of it. */
	public double through(int node) {
		return network.through(node);
	}
}
