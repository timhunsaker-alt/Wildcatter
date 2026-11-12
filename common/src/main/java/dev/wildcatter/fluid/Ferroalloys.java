package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;
import dev.wildcatter.ore.Ore;

/**
 * The numbers of the ferroalloy plant (docs/multiblocks/ferroalloys.md): the submerged arc furnace's three recipes, its tap changer, the
 * capacitor bank's reactance, the moly roaster's chemistry and the aluminothermic vessel's batches. Masses in kg, power in watts. Plain
 * Java, so it is unit-tested without Minecraft.
 */
public final class Ferroalloys {
	private Ferroalloys() {
	}

	// molar masses, g/mol
	public static final double MO = 95.95, W = 183.84, S = 32.06, O = 16.00, AL = 26.98, CA = 40.08;
	public static final double MOS2 = MO + 2 * S, MOO3 = MO + 3 * O, WO3 = W + 3 * O, CAWO4 = CA + W + 4 * O, AL2O3 = 2 * AL + 3 * O, SO2 = S + 2 * O;

	/** What goes into the furnace through the hopper on its top, one bin each. */
	public enum Charge {
		CHROMITE("chromitite"), PYROLUSITE("manganese ore"), QUARTZ("quartz"), LIMESTONE("limestone"), COKE("coke"), IRON("iron");

		public final String label;

		Charge(String label) {
			this.label = label;
		}

		/** kg one item of it counts for: an ore's mineral item is one block of rock. */
		public double itemKg() {
			return switch (this) {
				case CHROMITE -> Ore.CHROMITE.blockKg();
				case PYROLUSITE -> Ore.PYROLUSITE.blockKg();
				case QUARTZ -> Tuning.QUARTZ_KG;
				case LIMESTONE -> Ore.LIMESTONE.blockKg();
				case COKE -> Tuning.MET_COKE_KG;
				case IRON -> Tuning.IRON_INGOT_KG;
			};
		}
	}

	/**
	 * The furnace's campaigns, each a tonne of alloy (the spec's Products table): its charge by weight, its slag and the MWh it takes.
	 * {@code ore} is the bin whose lack reads NO ORE, {@code flux} the one that reads NO FLUX.
	 */
	public enum Recipe {
		FECR("HC ferrochrome", "FeCr", "65 % Cr, 7 % C", 3.8, 1.2, Charge.CHROMITE, Charge.QUARTZ,
				new double[] {0.62 * Ore.CHROMITE.blockKg(), 0, 200, 0, 550, 0}),
		FEMN("HC ferromanganese", "FeMn", "76 % Mn, 7 % C", 2.5, 0.6, Charge.PYROLUSITE, Charge.LIMESTONE,
				new double[] {0, 1600, 0, 150, 400, 0}),
		FESI("ferrosilicon 75", "FeSi", "75 % Si", 8.5, 0.03, Charge.QUARTZ, null,
				new double[] {0, 0, 1800, 0, 900, 250});

		public final String label, shortName, grade;
		/** MWh a tonne of alloy. */
		public final double mwh;
		/** t of slag a tonne of alloy (proposed). */
		public final double slag;
		public final Charge ore;
		public final Charge flux;
		private final double[] kgPerTonne;

		Recipe(String label, String shortName, String grade, double mwh, double slag, Charge ore, Charge flux, double[] kgPerTonne) {
			this.label = label;
			this.shortName = shortName;
			this.grade = grade;
			this.mwh = mwh;
			this.slag = slag;
			this.ore = ore;
			this.flux = flux;
			this.kgPerTonne = kgPerTonne;
		}

		/** kg of {@code charge} a tonne of alloy. */
		public double kgPerTonne(Charge charge) {
			return kgPerTonne[charge.ordinal()];
		}

		/** Joules a kg of alloy. */
		public double joulesPerKg() {
			return mwh * 3.6e9 / 1000;
		}

		/** t/h at {@code watts}. */
		public double tonnesPerHour(double watts) {
			return watts / 1e6 / mwh;
		}

		/** What its lack of {@code charge} reads. */
		public String lack(Charge charge) {
			if (charge == ore) {
				return "NO ORE: put " + charge.label + " in a hopper on its top";
			}
			if (charge == flux) {
				return "NO FLUX: put " + charge.label + " in a hopper on its top";
			}
			return charge == Charge.COKE ? "NO COKE: put metallurgical coke in a hopper on its top" : "NO IRON: put iron ingots in a hopper on its top";
		}

		public Recipe next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	// ---- the furnace transformer ------------------------------------------------------------------------------------------------------
	/** The secondary volts (line to line) on tap {@code tap}, 1 to {@link Tuning#FURNACE_TAPS}. */
	public static double secondaryVolts(int tap) {
		int t = Math.max(1, Math.min(Tuning.FURNACE_TAPS, tap));
		return Tuning.FURNACE_SECONDARY_MIN_VOLTS + (Tuning.FURNACE_SECONDARY_VOLTS - Tuning.FURNACE_SECONDARY_MIN_VOLTS) * (t - 1) / (Tuning.FURNACE_TAPS - 1);
	}

	/** The furnace's power on tap {@code tap}: it goes as the square of the secondary volts, 8 MW at full tap. */
	public static double tapWatts(int tap) {
		double v = secondaryVolts(tap) / Tuning.FURNACE_SECONDARY_VOLTS;
		return Tuning.SAF_WATTS * v * v;
	}

	/** The tap whose power is nearest {@code watts}. */
	public static int tapFor(double watts) {
		int best = 1;
		for (int t = 1; t <= Tuning.FURNACE_TAPS; t++) {
			if (Math.abs(tapWatts(t) - watts) < Math.abs(tapWatts(best) - watts)) {
				best = t;
			}
		}
		return best;
	}

	/** Amps in each electrode for {@code watts} at the furnace's power factor on tap {@code tap}. */
	public static double electrodeAmps(double watts, int tap) {
		return watts / Tuning.SAF_POWER_FACTOR / (Math.sqrt(3) * secondaryVolts(tap));
	}

	/** Vars a load of {@code watts} draws at {@code powerFactor} (lagging). */
	public static double vars(double watts, double powerFactor) {
		return watts * Math.tan(Math.acos(powerFactor));
	}

	/** The power factor of {@code watts} with {@code vars} (either sign). */
	public static double powerFactor(double watts, double vars) {
		double va = Math.hypot(watts, vars);
		return va < 1e-9 ? 1.0 : Math.abs(watts) / va;
	}

	// ---- the capacitor bank ---------------------------------------------------------------------------------------------------------
	/** Ohms of each leg of a wye bank of {@code var} at {@code lineToLine} volts and the standard's frequency (the leg's -X). */
	public static double capacitorOhms(double lineToLine, double var) {
		return lineToLine * lineToLine / var;
	}

	// ---- the moly roaster -------------------------------------------------------------------------------------------------------------
	/** kg of MoS2 a kg of technical moly oxide: 2 MoS2 + 7 O2 -> 2 MoO3 + 4 SO2. */
	public static double mos2PerOxide() {
		return Tuning.MOLY_OXIDE_MO * MOS2 / MO / Tuning.ROASTER_RECOVERY;
	}

	/** kg of SO2 a kg of oxide: all the sulfur goes up as SO2. */
	public static double so2PerOxide() {
		return mos2PerOxide() * 2 * SO2 / MOS2;
	}

	/**
	 * kg of burner gas on the pipe a kg of oxide. The roaster's off-gas is leaner (about 6 % SO2) than a sulfur burner's, and the acid
	 * plant's converter counts burner gas at the sulfur burner's strength; the roaster puts on the pipe the burner gas that carries its
	 * SO2, so the converter makes the right acid from it.
	 */
	public static double burnerGasPerOxide() {
		return so2PerOxide() / AcidPlant.so2InGas();
	}

	/** kg of scheelite (CaWO4) in a bag of dried scheelite concentrate. */
	public static double scheelitePerBag() {
		return Tuning.BULK_BAG_KG * Tuning.SCHEELITE_BAG_WO3 * CAWO4 / WO3;
	}

	// ---- the aluminothermic vessel ----------------------------------------------------------------------------------------------------
	/** A batch: one bag of oxide or concentrate, iron and aluminium ingots; kg of alloy and slag. */
	public enum Batch {
		FERROMOLY("ferromolybdenum", Tuning.VESSEL_FEMO_IRON, Tuning.VESSEL_FEMO_ALUMINIUM),
		FERROTUNGSTEN("ferrotungsten", Tuning.VESSEL_FEW_IRON, Tuning.VESSEL_FEW_ALUMINIUM),
		/** 17c2 (zirconium-extensions.md item 5): 4 sacks of chromic oxide and 2 aluminium ingots, no iron: chromium metal. */
		CHROMIUM("chromium", 0, Tuning.VESSEL_CR_ALUMINIUM);

		public final String label;
		public final int iron;
		public final int aluminium;

		Batch(String label, int iron, int aluminium) {
			this.label = label;
			this.iron = iron;
			this.aluminium = aluminium;
		}

		/** kg of molybdenum or tungsten into the alloy. */
		public double metalKg() {
			return switch (this) {
				case FERROMOLY -> Tuning.BULK_BAG_KG * Tuning.MOLY_OXIDE_MO * Tuning.VESSEL_RECOVERY;
				case FERROTUNGSTEN -> Tuning.BULK_BAG_KG * Tuning.SCHEELITE_BAG_WO3 * W / WO3 * Tuning.VESSEL_RECOVERY;
				case CHROMIUM -> Zirconium.chromiumPerBatch();
			};
		}

		public double alloyKg() {
			return this == CHROMIUM ? metalKg() : metalKg() / (this == FERROMOLY ? Tuning.FERROMOLY_MO : Tuning.FERROTUNGSTEN_W);
		}

		/** kg of aluminium the oxide's reduction takes: MoO3 + 2 Al -> Mo + Al2O3; WO3 + 2 Al -> W + Al2O3. */
		public double aluminiumNeededKg() {
			return this == CHROMIUM ? Zirconium.aluminiumPerChromiumBatch() : metalKg() / (this == FERROMOLY ? MO : W) * 2 * AL;
		}

		public double aluminiumKg() {
			return aluminium * Tuning.AL_INGOT_KG;
		}

		/** The slag block: the alumina the aluminium burns to, and what the bag carried that is not metal oxide (proposed). */
		public double slagKg() {
			if (this == CHROMIUM) {
				return Zirconium.aluminiumPerChromiumBatch() * AL2O3 / (2 * AL);
			}
			double oxide = this == FERROMOLY ? Tuning.BULK_BAG_KG * Tuning.MOLY_OXIDE_MO * MOO3 / MO : Tuning.BULK_BAG_KG * Tuning.SCHEELITE_BAG_WO3;
			return aluminiumKg() * AL2O3 / (2 * AL) + (Tuning.BULK_BAG_KG - oxide);
		}
	}
}
