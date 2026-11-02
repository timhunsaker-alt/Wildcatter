package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * Point reactor kinetics for the core (docs/multiblocks/reactor.md): six delayed-neutron groups (Keepin, U-235) with the prompt-jump
 * approximation, stepped implicitly so a game interval is stable below prompt critical. Power is a fraction of full power; the
 * precursors are kept multiplied by the generation time, so in equilibrium each is its group's fraction of beta times the power over
 * its decay constant. A small source keeps the shut-down core readable. Plain Java, so it is unit-tested without Minecraft.
 */
public final class PointKinetics {
	static final double[] FRACTION = {0.033, 0.219, 0.196, 0.395, 0.115, 0.042};
	static final double[] LAMBDA = {0.0124, 0.0305, 0.111, 0.301, 1.14, 3.01};
	private static final double SUBSTEP = 0.01;

	private final double[] precursors = new double[6];
	private double power;
	private double rate;   // decades a minute over the last step

	public PointKinetics() {
		hold(Tuning.NEUTRON_SOURCE / 0.05);
	}

	/** Sets the core steady at {@code power}, its precursors in equilibrium. */
	public void hold(double power) {
		this.power = power;
		for (int i = 0; i < 6; i++) {
			precursors[i] = FRACTION[i] * Tuning.BETA * power / LAMBDA[i];
		}
		rate = 0;
	}

	public double power() {
		return power;
	}

	/** The startup rate, decades a minute. */
	public double startupRate() {
		return rate;
	}

	/** Steps {@code seconds} at reactivity {@code rho} (dk/k). Above 99 % of beta it is held there: the high-flux trip comes first. */
	public void step(double rho, double seconds) {
		double beta = Tuning.BETA;
		double r = Math.min(rho, 0.99 * beta);
		double before = Math.max(power, 1e-30);
		int n = Math.max(1, (int) Math.ceil(seconds / SUBSTEP));
		double h = seconds / n;
		for (int s = 0; s < n; s++) {
			double lhs = beta - r;
			double rhs = Tuning.NEUTRON_SOURCE;
			for (int i = 0; i < 6; i++) {
				lhs -= LAMBDA[i] * h * FRACTION[i] * beta / (1 + h * LAMBDA[i]);
				rhs += LAMBDA[i] * precursors[i] / (1 + h * LAMBDA[i]);
			}
			power = Math.max(0, rhs / lhs);
			for (int i = 0; i < 6; i++) {
				precursors[i] = (precursors[i] + h * FRACTION[i] * beta * power) / (1 + h * LAMBDA[i]);
			}
		}
		rate = 60 * Math.log10(Math.max(power, 1e-30) / before) / seconds;
	}

	public double[] precursors() {
		return precursors;
	}

	public void load(double power, double[] precursors) {
		this.power = power;
		System.arraycopy(precursors, 0, this.precursors, 0, 6);
	}
}
