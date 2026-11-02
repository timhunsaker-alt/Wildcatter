package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * Irradiated fuel (docs/multiblocks/fuel-handling.md): an assembly's record (its burnup, its decay heat groups as a fraction of its own
 * full power, and whether its cladding failed) and how its decay heat falls; the spent fuel pool's heat balance. Plain Java, so it is
 * unit-tested without Minecraft.
 */
public final class SpentFuel {
	private SpentFuel() {
	}

	/** An irradiated assembly. */
	public static final class Assembly {
		public final double burnup;
		public final double[] decay;
		public final boolean damaged;

		public Assembly(double burnup, double[] decay, boolean damaged) {
			this.burnup = burnup;
			this.decay = decay.clone();
			this.damaged = damaged;
		}

		/** W of decay heat it makes now. */
		public double watts() {
			double sum = 0;
			for (double d : decay) {
				sum += d;
			}
			return sum * Reactor.assemblyWatts();
		}

		/** Its decay heat after {@code seconds} more. */
		public void decay(double seconds) {
			for (int i = 0; i < decay.length; i++) {
				decay[i] *= Math.exp(-Tuning.DECAY_LAMBDA[i] * seconds);
			}
		}
	}

	/** J/C of the pool's water. */
	public static double poolHeat() {
		return Tuning.POOL_WATER_KG * 4186;
	}

	/** Hours a pool at 30 C holding {@code watts} with no cooling takes to reach the boil. */
	public static double hoursToBoil(double watts) {
		return 70 * poolHeat() / watts / 3600;
	}

	/** kg/s it then boils off. */
	public static double boilOff(double watts) {
		return watts / Tuning.POOL_LATENT_J_PER_KG;
	}
}
