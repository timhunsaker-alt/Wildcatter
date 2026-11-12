package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The alloy melt shop's numbers (docs/multiblocks/nickel-smelter.md, 17b2): each grade's charge a tonne, the induction furnace's energy,
 * the carbon the AOD takes out and the oxygen and nitrogen it blows. Plain Java, so it is unit-tested without Minecraft.
 */
public final class MeltShop {
	private MeltShop() {
	}

	/** What the induction furnace's hopper takes, one bin each. */
	public enum Charge {
		FERROCHROME("HC ferrochrome"), NICKEL("cathode nickel"), FERROMANGANESE("HC ferromanganese"), FERROMOLYBDENUM("ferromolybdenum"), IRON("iron");

		public final String label;

		Charge(String label) {
			this.label = label;
		}

		/** kg one item of it counts for: a 1 t bulk bag, or a 100 kg iron ingot. */
		public double itemKg() {
			return this == IRON ? Tuning.IRON_INGOT_KG : Tuning.BULK_BAG_KG;
		}

		/** The share of carbon it brings: the HC ferroalloys' 7 %. */
		public double carbon() {
			return this == FERROCHROME || this == FERROMANGANESE ? Tuning.FERROALLOY_CARBON : 0;
		}
	}

	/** The grades, each its charge for a tonne by weight (kg of FeCr, Ni, FeMn, FeMo, Fe). */
	public enum Grade {
		S304("304 stainless", "18 % Cr, 8 % Ni, 1.5 % Mn", new double[] {280, 80, 20, 0, 620}),
		S316("316 stainless", "17 % Cr, 12 % Ni, 2.6 % Mo", new double[] {260, 120, 20, 40, 560}),
		NICR("nickel-chromium alloy (Alloy 600 type)", "72 % Ni, 15.5 % Cr, 9 % Fe", new double[] {240, 720, 0, 0, 40});

		public final String label, chemistry;
		private final double[] kgPerTonne;

		Grade(String label, String chemistry, double[] kgPerTonne) {
			this.label = label;
			this.chemistry = chemistry;
			this.kgPerTonne = kgPerTonne;
		}

		public double kgPerTonne(Charge c) {
			return kgPerTonne[c.ordinal()];
		}

		/** kg of {@code c} a heat of the furnace's 2 t. */
		public double heatKg(Charge c) {
			return kgPerTonne(c) * Tuning.INDUCTION_HEAT_KG / 1000;
		}

		/** kg of carbon a heat brings in its ferroalloys. */
		public double heatCarbon() {
			double kg = 0;
			for (Charge c : Charge.values()) {
				kg += heatKg(c) * c.carbon();
			}
			return kg;
		}

		public Grade next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** Joules to melt a heat. */
	public static double heatJoules() {
		return Tuning.INDUCTION_HEAT_KG / 1000 * Tuning.INDUCTION_KWH_PER_T * 3.6e6;
	}

	/** Tonnes an hour the induction furnace melts at its rating: 2.5. */
	public static double meltTph() {
		return Tuning.INDUCTION_WATTS / 1e6 / (Tuning.INDUCTION_KWH_PER_T / 1000);
	}

	/** Nm3 of oxygen that burns a kg of carbon to CO (half a mole of O2 a mole of C), before its efficiency. */
	public static double o2PerCarbon() {
		return 22.414 / 2 / 12.011;
	}

	/** Nm3 of oxygen the AOD blows to take a heat of {@code grade} down to its final carbon. */
	public static double heatO2(Grade grade) {
		double out = grade.heatCarbon() - Tuning.INDUCTION_HEAT_KG * Tuning.AOD_FINAL_CARBON;
		return Math.max(0, out) * o2PerCarbon() / Tuning.AOD_O2_EFFICIENCY;
	}

	/** kg of carbon a Nm3 of blown oxygen takes out. */
	public static double carbonPerO2() {
		return Tuning.AOD_O2_EFFICIENCY / o2PerCarbon();
	}

	/** Ingots a heat casts. */
	public static int ingotsPerHeat() {
		return (int) Math.round(Tuning.INDUCTION_HEAT_KG / Tuning.ALLOY_INGOT_KG);
	}
}
