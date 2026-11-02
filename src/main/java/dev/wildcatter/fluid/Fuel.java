package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The fuel plant's numbers (docs/multiblocks/fuel.md). Plain Java, so it is unit-tested without Minecraft. */
public final class Fuel {
	private Fuel() {
	}

	/** g of UO2 in a sintered pellet: the cylinder at 95 % density, less its dish and chamfers. */
	public static double pelletGrams() {
		double r = Tuning.PELLET_DIAMETER_MM / 20;   // cm
		double cm3 = Math.PI * r * r * Tuning.PELLET_LENGTH_MM / 10;
		return cm3 * Tuning.UO2_THEORETICAL_DENSITY * Tuning.PELLET_DENSITY_FRACTION * (1 - Tuning.PELLET_DISH_CHAMFER);
	}

	public static int pelletsPerRod() {
		return (int) Math.round(Tuning.ROD_STACK_M * 1000 / Tuning.PELLET_LENGTH_MM);
	}

	/** kg of U in a rod. */
	public static double rodU() {
		return pelletsPerRod() * pelletGrams() / 1000 * Tuning.U_IN_UO2;
	}

	/** kg of U in an assembly. */
	public static double assemblyU() {
		return Tuning.ASSEMBLY_RODS * rodU();
	}

	/** kg of the whole assembly: UO2, Zircaloy and steel. */
	public static double assemblyKg() {
		return assemblyU() / Tuning.U_IN_UO2 + Tuning.ASSEMBLY_RODS * Zirconium.claddingKg() + Tuning.ASSEMBLY_THIMBLES * Zirconium.thimbleKg()
				+ Tuning.ASSEMBLY_MID_GRIDS * Tuning.SPACER_GRID_KG + Tuning.ASSEMBLY_STEEL_KG;
	}

	/** m3 of hydrogen a kg of U in the kiln: one H2 a U, with the excess. */
	public static double hydrogenPerU() {
		return 2.016 / 238.03 / Tuning.H2_KG_PER_M3 * Tuning.IDR_H2_EXCESS;
	}

	/** L of water a kg of U, as steam: two H2O a U, with the excess. */
	public static double waterPerU() {
		return 2 * 18.015 / 238.03 * Tuning.IDR_WATER_EXCESS;
	}

	/** kg of HF a kg of U: all six fluorines. */
	public static double hfPerU() {
		return 6 * 20.006 / 238.03;
	}

	/** Pellets a second at the line rate. */
	public static double pelletsPerSecond() {
		return Tuning.FUEL_LINE_U_KGS / Tuning.U_IN_UO2 / (pelletGrams() / 1000);
	}

	/** Hours of the line for a 30B, and for an assembly. */
	public static double hoursPer30b() {
		return Tuning.CYLINDER_30B_KG / Tuning.UF6_PER_U / Tuning.FUEL_LINE_U_KGS / 3600;
	}

	public static double hoursPerAssembly() {
		return assemblyU() / Tuning.FUEL_LINE_U_KGS / 3600;
	}

	/** Rods a helium cylinder fills. */
	public static double rodsPerCylinder() {
		return Tuning.HELIUM_CYLINDER_M3 * 1000 / Tuning.ROD_HELIUM_L;
	}
}
