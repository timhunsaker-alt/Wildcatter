package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * What a firebox gets from a unit of a piped fuel (Tim, 2026-11-10: "one real heat rate, in every world"; phase14-gas-power 20). A firebox
 * burns at one heat rate, {@link Tuning#FIREBOX_HEAT_WATTS}, its coal fire's; a fuel lasts its heating value at that rate. Plain Java, so it
 * is unit-tested without Minecraft.
 */
public final class FireboxFuel {
	private FireboxFuel() {
	}

	/** Heating value of a unit (litre or m3) of what a line carries, or 0 if a firebox does not burn it. */
	public static double joulesPerUnit(Product product) {
		if (product == null) {
			return 0;
		}
		return switch (product) {
			case WET_GAS, GAS, COMPRESSED_GAS, DRIED_GAS, PIPELINE_GAS -> Tuning.GAS_JOULES_PER_M3;
			case BIOGAS -> Tuning.BIOGAS_JOULES_PER_M3;   // any gas, by its heating value (docs/biogas.md)
			case CRUDE -> Tuning.CRUDE_JOULES_PER_L;
			case DIESEL, KEROSENE -> Tuning.LIQUID_FUEL_JOULES_PER_L;
			case GASOLINE, NATURAL_GASOLINE, CONDENSATE -> Tuning.GASOLINE_JOULES_PER_L;
			case RESIDUE -> Tuning.FUEL_OIL_JOULES_PER_L;
			default -> 0;
		};
	}

	/** Ticks of fire in this many joules, at the firebox's heat rate. */
	public static double ticks(double joules) {
		return joules / Tuning.FIREBOX_HEAT_WATTS * 20.0;
	}

	/** Ticks of fire in a unit (litre or m3) of what a line carries, or 0 if a firebox does not burn it. */
	public static double ticksPerUnit(Product product) {
		return ticks(joulesPerUnit(product));
	}
}
