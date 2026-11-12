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
	// ---- 17c1: batches, scrap and hafnium (docs/multiblocks/zirconium-extensions.md) --------------------------------------------

	/** Of the metal into the extrusion press, the share that leaves it as hollows (the rest is crop ends, scrap). */
	public static double extrusionYield() {
		return 1 - Tuning.ZR_LOSS_EXTRUSION;
	}

	/** Of the hollows into the pilger mill, the share that becomes tube: the mill's and inspection's losses are shares of the metal. */
	public static double pilgerYield() {
		return (1 - Tuning.ZR_LOSS_EXTRUSION - Tuning.ZR_LOSS_PILGER - Tuning.ZR_LOSS_INSPECTION) / extrusionYield();
	}

	/** Of the metal into the extrusion press, the share that reaches a finished tube: 84 %. */
	public static double lineYield() {
		return extrusionYield() * pilgerYield();
	}

	/** Tube a line of this many kg an hour makes, with its scrap recycled into the VAR or not. */
	public static double tubeKgh(double lineKgh, boolean recycled) {
		// recycled, the scrap goes back as part of the next ingots' charge: what comes out as tube is what comes in as sponge
		return recycled ? lineKgh : lineKgh * lineYield();
	}

	/** Mg reacted a kg of hafnium: HfCl4 + 2Mg -> Hf + 2MgCl2. */
	public static double magnesiumPerHf() {
		return 2 * MG / Tuning.HF;
	}

	public static double mgCl2PerHf() {
		return 2 * MGCL2 / Tuning.HF;
	}

	/** kg of a solid hafnium rodlet, 9.7 mm across and 3.66 m long, at 13.31 g/cm3: about 3.6 kg. */
	public static double hafniumRodletKg() {
		double areaMm2 = Math.PI / 4 * Tuning.HF_RODLET_OD_MM * Tuning.HF_RODLET_OD_MM;
		return areaMm2 * Tuning.HF_RODLET_LENGTH_M * Tuning.HF_DENSITY / 1000;
	}

	/** Hafnium the line makes an hour, kg: 2 % of the metal through the column. */
	public static double hafniumKgh() {
		return Tuning.ZR_LINE_KGS * 3600 * Tuning.ZR_HF_FRACTION / (1 - Tuning.ZR_HF_FRACTION);
	}

	/** Hours of the line's hafnium for a set of RCCAs of hafnium rodlets: about 2,100 for 5. */
	public static double hoursForHafniumRccas(int rccas) {
		return rccas * Tuning.HF_RODLETS_PER_RCCA * hafniumRodletKg() / hafniumKgh();
	}

	// ---- 17c2: fumed silica, Pidgeon magnesium, chromium (docs/multiblocks/zirconium-extensions.md) ------------------------------
	public static final double SICL4 = SI + 2 * CL2, SIO2 = SI + 32.00, HCL = 36.461, CR = 51.996, CR2O3 = 2 * CR + 48.00, FECR2O4 = 55.845 + 2 * CR + 64.00,
			NAOH = 39.997, S = 32.06, NA2SO4 = 142.04, AL = 26.98;

	/** SiCl4 + 2 H2 + O2 -> SiO2 + 4 HCl: kg of fumed silica a kg of SiCl4. */
	public static double silicaPerSiCl4() {
		return SIO2 / SICL4;
	}

	public static double hclPerSiCl4() {
		return 4 * HCL / SICL4;
	}

	/** Nm3 of hydrogen a kg of SiCl4. */
	public static double hydrogenPerSiCl4() {
		return 2 * Tuning.NM3_PER_KMOL / SICL4;
	}

	public static double oxygenPerSiCl4() {
		return Tuning.NM3_PER_KMOL / SICL4;
	}

	/** The line's SiCl4, kg an hour: 18.6. */
	public static double siCl4Kgh() {
		return Tuning.ZR_LINE_KGS * 3600 * siCl4PerZr();
	}

	/** kg of dolime a kg of Pidgeon magnesium: 5.75. */
	public static double dolimePerMg() {
		return Tuning.PIDGEON_DOLOMITE_PER_MG * Tuning.DOLIME_PER_DOLOMITE;
	}

	/** kg of fluorspar a kg of magnesium: 3 % of the briquette. */
	public static double fluorsparPerMg() {
		return (dolimePerMg() + Tuning.PIDGEON_FESI_PER_MG) * Tuning.PIDGEON_FLUORSPAR_SHARE / (1 - Tuning.PIDGEON_FLUORSPAR_SHARE);
	}

	/** Watts of gas the Pidgeon furnace burns at its rate: 0.5 MW. */
	public static double pidgeonGasWatts() {
		return Tuning.PIDGEON_MG_KGS * Tuning.PIDGEON_GAS_J_PER_KG;
	}

	/** Chromic oxide's chromium share: 0.684. */
	public static double crInCr2o3() {
		return 2 * CR / CR2O3;
	}

	/** kg of chromite (FeCr2O4) a kg of Cr2O3, at the roast's recovery. */
	public static double chromitePerCr2o3() {
		return FECR2O4 / CR2O3 / Tuning.CHROMITE_RECOVERY;
	}

	/** kg of NaOH a kg of Cr2O3: two a Cr (4 FeCr2O4 + 16 NaOH + 7 O2 -> 8 Na2CrO4 + 2 Fe2O3 + 8 H2O). */
	public static double naohPerCr2o3() {
		return 4 * NAOH / CR2O3;
	}

	/** kg of sulfur a kg of Cr2O3, and of sodium sulfate made (Na2Cr2O7 + S -> Cr2O3 + Na2SO4, simplified). */
	public static double sulfurPerCr2o3() {
		return S / CR2O3;
	}

	public static double sulfatePerCr2o3() {
		return NA2SO4 / CR2O3;
	}

	/** kg of chromium a vessel batch makes: 4 sacks of Cr2O3 reduced by aluminium, at the vessel's recovery. */
	public static double chromiumPerBatch() {
		return Tuning.VESSEL_CR_SACKS * Tuning.CR2O3_SACK_KG * crInCr2o3() * Tuning.VESSEL_RECOVERY;
	}

	/** kg of aluminium a batch needs: Cr2O3 + 2 Al -> 2 Cr + Al2O3. */
	public static double aluminiumPerChromiumBatch() {
		return Tuning.VESSEL_CR_SACKS * Tuning.CR2O3_SACK_KG / CR2O3 * 2 * AL;
	}
}
