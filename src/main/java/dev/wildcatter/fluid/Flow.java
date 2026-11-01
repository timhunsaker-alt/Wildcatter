package dev.wildcatter.fluid;

/**
 * How a pull is shared out over a run's sources (docs/multiblocks/lease.md): in order, each giving what it can until the want is met.
 * The caller lists process outlets before tanks. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Flow {
	private Flow() {
	}

	/** What each source gives towards {@code want}, given what each has to give, in the order listed. */
	public static double[] share(double want, double[] available) {
		double[] out = new double[available.length];
		double left = Math.max(0, want);
		for (int i = 0; i < available.length && left > 0; i++) {
			out[i] = Math.min(left, Math.max(0, available[i]));
			left -= out[i];
		}
		return out;
	}
}
