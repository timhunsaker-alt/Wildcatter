package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The zirconium plant's numbers (docs/multiblocks/zirconium.md). Plain Java, so it is unit-tested without Minecraft. */
public final class Zirconium {
	public static final double ZR = 91.224, SI = 28.086, C = 12.011, CL2 = 70.906, MG = 24.305, MGCL2 = 95.211, MGO = 40.304;

	private Zirconium() {
	}

	/** kg of zircon a kg of Zr. */
	public static double zirconPerZr() {
		return 1 / Tuning.ZR_IN_ZIRCON;
	}

	/** kg of carbon a kg of Zr: four C a ZrSiO4, with the excess. */
	public static double carbonPerZr() {
		return 4 * C / ZR * Tuning.ZR_CARBON_EXCESS;
	}

	/** kg of Cl2 into the chlorinator a kg of Zr: two for the ZrCl4 and two for the SiCl4. */
	public static double chlorinePerZr() {
		return 4 * CL2 / ZR;
	}

	/** kg of SiCl4 a kg of Zr. */
	public static double siCl4PerZr() {
		return (SI + 2 * CL2) / ZR;
	}

	/** kg of Mg reacted a kg of Zr. */
	public static double magnesiumPerZr() {
		return 2 * MG / ZR;
	}

	/** kg of MgCl2 a kg of Zr. */
	public static double mgCl2PerZr() {
		return 2 * MGCL2 / ZR;
	}

	/** kg of Cl2 in a kg of MgCl2. */
	public static double chlorineInMgCl2() {
		return CL2 / MGCL2;
	}

	/** kg of Mg in a kg of MgCl2. */
	public static double magnesiumInMgCl2() {
		return MG / MGCL2;
	}

	/** kg of Mg the magnesium cell makes a second at full current. */
	public static double mgCellKgs() {
		return Tuning.MG_CELL_KA * Tuning.MG_KG_PER_KAH * Tuning.MG_CURRENT_EFFICIENCY / 3600;
	}

	public static double mgCellWatts() {
		return Tuning.MG_CELL_KA * 1000 * Tuning.MG_CELL_VOLTS;
	}

	/** kg of Cl2 the chlor-alkali cell makes a second at full current. */
	public static double chlorAlkaliKgs() {
		return Tuning.CA_CELLS * Tuning.CA_CELL_KA * Tuning.CL2_KG_PER_KAH * Tuning.CA_CURRENT_EFFICIENCY / 3600;
	}

	public static double chlorAlkaliVolts() {
		return Tuning.CA_CELLS * Tuning.CA_CELL_VOLTS;
	}

	public static double chlorAlkaliWatts() {
		return chlorAlkaliVolts() * Tuning.CA_CELL_KA * 1000;
	}

	/** m3 of hydrogen a kg of Cl2: one H2 a Cl2. */
	public static double hydrogenM3PerCl2() {
		return 2.016 / CL2 / Tuning.H2_KG_PER_M3;
	}

	/** For the magnesia chlorinator, a kg of MgCl2: MgO, carbon and Cl2. */
	public static double mgoPerMgCl2() {
		return MGO / MGCL2;
	}

	public static double carbonPerMgCl2() {
		return C / MGCL2;
	}

	public static double chlorinePerMgCl2() {
		return CL2 / MGCL2;
	}

	/** kg of a Zircaloy tube: its wall's area times its length. */
	public static double tubeKg(double odMm, double wallMm, double lengthM) {
		double id = odMm - 2 * wallMm;
		double areaMm2 = Math.PI / 4 * (odMm * odMm - id * id);
		return areaMm2 * lengthM * 1000 / 1000 * Tuning.ZIRCALOY_DENSITY / 1000;
	}

	public static double claddingKg() {
		return tubeKg(Tuning.CLAD_OD_MM, Tuning.CLAD_WALL_MM, Tuning.CLAD_LENGTH_M);
	}

	public static double thimbleKg() {
		return tubeKg(Tuning.THIMBLE_OD_MM, Tuning.THIMBLE_WALL_MM, Tuning.THIMBLE_LENGTH_M);
	}

	/** Hours of the line for one assembly's 264 cladding tubes and 25 thimbles. */
	public static double hoursPerAssembly() {
		return (264 * claddingKg() + 25 * thimbleKg()) / Tuning.ZR_LINE_KGS / 3600;
	}
}
