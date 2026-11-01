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
	ETHYLENE("ethylene", true);

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
		return this == HOT_CRACKED_GAS || this == CRACKED_GAS || this == COMPRESSED_CRACKED_GAS ? "kg" : gas ? "m3" : "L";
	}
}
