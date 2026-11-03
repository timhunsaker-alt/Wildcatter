package dev.wildcatter.circuit;

/**
 * An electrolysis cell, or a string of them in series, as a DC load (docs/multiblocks/dc-bus.md): a back-EMF (the decomposition
 * voltage) behind a resistance that gives the rated volts at the rated current. The current through it is what makes product
 * (Faraday). An idle cell opens its circuit and draws nothing.
 */
public final class DcCell {
	private final int cells;
	private final double backEmf;
	private final double ohms;
	private final double ratedAmps;
	private int branch = -1;
	private double amps;
	private double volts;

	/** {@code cells} in series, each {@code backEmf} volts and {@code ratedVolts} at {@code ratedAmps}. */
	public DcCell(int cells, double backEmf, double ratedVolts, double ratedAmps) {
		this.cells = cells;
		this.backEmf = backEmf;
		this.ohms = cells * (ratedVolts - backEmf) / ratedAmps;
		this.ratedAmps = ratedAmps;
	}

	public double ratedAmps() {
		return ratedAmps;
	}

	/** Adds the cells between {@code plus} and {@code minus}; while {@code drawing} is false they are open. */
	public void build(Circuit circuit, int plus, int minus, boolean drawing) {
		branch = -1;
		if (drawing) {
			int inside = circuit.node();
			branch = circuit.resistor(plus, inside, ohms);
			circuit.source(inside, minus, cells * backEmf);   // charged by the line current, as a cell is
		}
	}

	public void apply(Circuit circuit, int plus, int minus) {
		amps = branch < 0 ? 0 : Math.max(0, circuit.dcAmps(branch));
		volts = circuit.dcVolts(plus) - circuit.dcVolts(minus);
	}

	/** Amps through the cells (never backward: a cell cannot drive the line). */
	public double amps() {
		return amps;
	}

	public double volts() {
		return volts;
	}

	/** The current against the rating: what the cells make goes with it. */
	public double share() {
		return amps / ratedAmps;
	}
}
