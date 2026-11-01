package dev.wildcatter.fluid;

/**
 * What a run of steel pipe can carry (docs/multiblocks/lease.md): each joined run carries one of these. Liquids are counted in
 * litres (a bucket is 1,000), gases in standard cubic metres. Plain Java, so it is unit-tested without Minecraft.
 */
public enum Product {
	/** An oil well's stream: oil, produced water and gas together, counted as litres of liquid. */
	WELL_FLUID("well fluid", false),
	/** A gas well's stream: gas with its condensate and a little water, counted as m3 of gas. */
	WET_GAS("wet gas", true),
	CRUDE("crude", false),
	/** Separator gas: the liquids taken out, as gas engines need. Big burners need it processed into pipeline gas. */
	GAS("gas", true),
	PRODUCED_WATER("produced water", false),
	DIESEL("diesel", false),
	/** Separator gas after a compressor: at 1,000 psi, still wet (docs/multiblocks/gas-plant.md). */
	COMPRESSED_GAS("compressed gas", true),
	/** Compressed gas a glycol dehydrator has dried. */
	DRIED_GAS("dried gas", true),
	/** A gas plant's residue gas: dry, its heavier parts taken out, what big burners and turbines need. */
	PIPELINE_GAS("pipeline gas", true),
	/** Raw natural gas liquids out of a chiller, before the stabilizer splits them. */
	Y_GRADE("raw NGL (Y-grade)", false),
	/** Propane and butane. */
	LPG("LPG", false),
	/** The C5+ out of the bottom of a stabilizer. */
	NATURAL_GASOLINE("natural gasoline", false),
	/** A gas well's liquid out of a separator: light, mostly gasoline range (docs/multiblocks/crude-units.md). */
	CONDENSATE("condensate", false),
	/** Naphtha off a crude unit's overhead, and a cracker's pyrolysis gasoline. */
	GASOLINE("gasoline", false),
	KEROSENE("kerosene", false),
	/** Atmospheric residue: burned as fuel oil. */
	RESIDUE("residue (fuel oil)", false),
	/** Out of a cracking furnace, before the quench (docs/multiblocks/cracker.md). Counted in kg. */
	HOT_CRACKED_GAS("hot cracked gas", true),
	/** Cracked gas after the quench. Counted in kg. */
	CRACKED_GAS("cracked gas", true),
	/** Cracked gas after its compressor. Counted in kg. */
	COMPRESSED_CRACKED_GAS("compressed cracked gas", true),
	ETHYLENE("ethylene", true),
	/** Sulfur melted in an acid plant's melter (docs/multiblocks/acid-plant.md). */
	MOLTEN_SULFUR("molten sulfur", false),
	/** A sulfur furnace's gas, about 11 % SO2, after its waste heat boiler. Counted in kg. */
	BURNER_GAS("burner gas", true),
	/** 98.5 % sulfuric acid. */
	SULFURIC_ACID("sulfuric acid", false),
	/** Ground ore in water out of a SAG mill (docs/multiblocks/uranium-mill.md). Counted in kg; it carries its uranium. */
	ORE_SLURRY("ore slurry", false),
	/** Ore slurry after the leach: the uranium dissolved in its liquor. Counted in kg. */
	LEACH_SLURRY("leach slurry", false),
	/** A thickener's overflow: the leach liquor with its uranium, the solids washed out. */
	PREGNANT_LIQUOR("pregnant liquor", false),
	/** Salt water that has stripped the uranium out of the solvent: about 25 g/L of U3O8. */
	STRIP_LIQUOR("strip liquor", false),
	/** A producer's water, oxygenated lixiviant that has swept the ore (docs/multiblocks/isr-wellfield.md). It carries its uranium. */
	PREGNANT_LIXIVIANT("pregnant lixiviant", false),
	/** Lixiviant the IX columns have stripped, back to the injectors; it carries its oxygen. */
	BARREN_LIXIVIANT("barren lixiviant", false),
	/** IX eluate, acidified: salt brine with about 15 g/L of U3O8, for the precipitation tank. */
	ELUATE("eluate", false),
	/** From a steam reformer (docs/multiblocks/conversion.md), for the reduction reactor. */
	HYDROGEN("hydrogen", true),
	/** Anhydrous hydrogen fluoride, from fluorspar: for the hydrofluorinator and the fluorine cells. */
	ANHYDROUS_HF("anhydrous HF", false),
	/** From the fluorine cells, for the fluorinator. Counted in kg. */
	FLUORINE("fluorine", true),
	/** UF6 gas on a cascade header (docs/multiblocks/enrichment.md), from a feed station. Counted in kg; real UF6 is a solid in its cylinders. */
	UF6("UF6", true),
	/** From a chlor-alkali or magnesium cell (docs/multiblocks/zirconium.md), for the chlorinators. Counted in kg. */
	CHLORINE("chlorine", true);

	private final String label;
	private final boolean gas;

	Product(String label, boolean gas) {
		this.label = label;
		this.gas = gas;
	}

	public String label() {
		return label;
	}

	/** True for gases: counted in m3, and no tank holds them. */
	public boolean gas() {
		return gas;
	}

	public String unit() {
		return switch (this) {
			case HOT_CRACKED_GAS, CRACKED_GAS, COMPRESSED_CRACKED_GAS, BURNER_GAS, ORE_SLURRY, LEACH_SLURRY, FLUORINE, UF6, CHLORINE -> "kg";
			default -> gas ? "m3" : "L";
		};
	}

	/** False for liquids no stock tank holds: molten sulfur freezes, and the mill's slurries and liquors carry their uranium. */
	public boolean tanked() {
		return switch (this) {
			case MOLTEN_SULFUR, ORE_SLURRY, LEACH_SLURRY, PREGNANT_LIQUOR, STRIP_LIQUOR, PREGNANT_LIXIVIANT, BARREN_LIXIVIANT, ELUATE, ANHYDROUS_HF -> false;
			default -> !gas;
		};
	}
}
