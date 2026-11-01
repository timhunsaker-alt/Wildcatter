package dev.wildcatter.ore;

/**
 * The real ores (docs/ores.md). Each is one block drawn as its mineral in its real host rock, and drops its mineral. The game
 * grade is about ten times a typical mined grade, capped at 80 % of the ore mineral; ores already richer than that keep their
 * real grade. A block is 1 m3 at its host rock's density. No Minecraft imports here: the mill (7b) and the tests use it too.
 */
public enum Ore {
	//          key            ore block name           mineral name              formula              real grade                                 game  of           t/m3  where                 tool
	URANINITE("uraninite", "Uranium Ore", "Uraninite", "UO2", "0.05-0.4 % U3O8 (roll-front sandstone, Wyoming)", 0.02, "U3O8", 2.3, Where.DESERT_BADLANDS, Tool.STONE),
	PITCHBLENDE("pitchblende", "Pitchblende", "Pitchblende", "UO2", "7-20 % U3O8 (Athabasca unconformity; McArthur River 6.9 %)", 0.60, "U3O8", 2.5, Where.DEEP, Tool.IRON),
	SULFUR("sulfur", "Native Sulfur", "Sulfur", "S", "20-40 % S (salt-dome caprock, Gulf Coast; textbook figure)", 0.80, "sulfur", 2.7, Where.SALT_DOME, Tool.STONE),
	HALITE("halite", "Rock Salt", "Halite", "NaCl", "95-99 % NaCl (Goderich, Avery Island)", 0.98, "halite", 2.16, Where.SALT_DOME, Tool.STONE),
	SYLVITE("sylvite", "Potash (Sylvinite)", "Sylvite", "KCl", "20-30 % K2O (Saskatchewan)", 0.80, "sylvite", 2.1, Where.EVAPORITE, Tool.STONE),
	FLUORITE("fluorite", "Fluorspar", "Fluorite", "CaF2", "20-60 % CaF2 (Illinois-Kentucky)", 0.80, "fluorite", 2.7, Where.EVERYWHERE, Tool.STONE),
	HEAVY_MINERAL("heavy_mineral", "Heavy Mineral Sand", "Heavy Minerals", "zircon ZrSiO4, ilmenite FeTiO3, rutile TiO2, monazite",
			"2-10 % heavy minerals (Jacinth-Ambrosia, Richards Bay)", 0.50, "heavy minerals (25 % zircon, 55 % ilmenite, 5 % rutile, 3 % monazite)", 1.8, Where.BEACH, Tool.SHOVEL),
	BAUXITE("bauxite", "Bauxite", "Bauxite", "Al(OH)3 (gibbsite)", "40-60 % Al2O3 (Weipa, Boke)", 0.80, "gibbsite", 2.1, Where.TROPICS, Tool.STONE),
	GALENA("galena", "Galena Ore", "Galena", "PbS", "about 2 % Pb (Mississippi Valley type, Viburnum Trend)", 0.23, "galena", 2.8, Where.EVERYWHERE, Tool.STONE),
	SPHALERITE("sphalerite", "Sphalerite Ore", "Sphalerite", "ZnS", "about 6 % Zn (Tri-State, Pine Point)", 0.80, "sphalerite", 2.8, Where.EVERYWHERE, Tool.STONE),
	CASSITERITE("cassiterite", "Tin Ore", "Cassiterite", "SnO2", "0.5-1.5 % Sn (Cornwall)", 0.13, "cassiterite", 2.65, Where.MOUNTAINS, Tool.STONE),
	PENTLANDITE("pentlandite", "Nickel Ore", "Pentlandite", "(Fe,Ni)9S8", "about 2 % Ni (Sudbury)", 0.60, "nickel sulfides", 2.9, Where.EVERYWHERE, Tool.IRON),
	CHROMITE("chromite", "Chromitite", "Chromite", "FeCr2O4", "40-46 % Cr2O3 (Bushveld)", 0.80, "chromite", 3.3, Where.EVERYWHERE, Tool.IRON),
	PLATINUM("platinum", "Platinum Reef", "Platinum Reef Ore", "Pt-Pd sulfides", "6.3 g/t Pt+Pd+Rh+Au (Merensky Reef)", 63e-6, "platinum metals", 3.1, Where.EVERYWHERE, Tool.IRON),
	MOLYBDENITE("molybdenite", "Molybdenum Ore", "Molybdenite", "MoS2", "0.2-0.5 % MoS2 (Climax, Henderson)", 0.03, "molybdenite", 2.65, Where.MOUNTAINS, Tool.STONE),
	MAGNESITE("magnesite", "Magnesite", "Magnesite", "MgCO3", "about 90 % MgCO3 (Haicheng)", 0.90, "magnesite", 2.9, Where.EVERYWHERE, Tool.STONE),
	DOLOMITE("dolomite", "Dolomite", "Dolomite", "CaMg(CO3)2", "about 95 % dolomite (platform carbonate rock)", 0.95, "dolomite", 2.85, Where.EVERYWHERE, Tool.STONE),
	GRAPHITE("graphite", "Graphite Schist", "Graphite", "C", "5-20 % graphitic carbon (Balama)", 0.80, "graphite", 2.7, Where.EVERYWHERE, Tool.STONE),
	BORAX("borax", "Borax Ore", "Borax", "Na2B4O7.10H2O", "about 25 % B2O3 (Boron, California; textbook figure)", 0.80, "borax", 2.1, Where.DESERT_BADLANDS, Tool.STONE),
	SPODUMENE("spodumene", "Spodumene Pegmatite", "Spodumene", "LiAlSi2O6", "1-2 % Li2O (Greenbushes, Kings Mountain)", 0.80, "spodumene", 2.7, Where.EVERYWHERE, Tool.STONE),
	BARITE("barite", "Barite Ore", "Barite", "BaSO4", "50-90 % BaSO4 (Battle Mountain)", 0.90, "barite", 2.6, Where.EVERYWHERE, Tool.STONE),
	BENTONITE("bentonite", "Bentonite", "Bentonite", "montmorillonite", "over 80 % smectite (Black Hills)", 0.85, "montmorillonite", 2.2, Where.PLAINS_BADLANDS, Tool.SHOVEL),
	ACANTHITE("acanthite", "Silver Ore", "Acanthite", "Ag2S", "100-1,000 g/t Ag (Comstock, Potosi)", 0.003, "silver", 2.65, Where.MOUNTAINS_BADLANDS, Tool.IRON),
	SCHEELITE("scheelite", "Tungsten Ore", "Scheelite", "CaWO4", "0.3-1 % WO3 (Pine Creek)", 0.065, "WO3", 2.9, Where.EVERYWHERE, Tool.STONE),
	COBALTITE("cobaltite", "Cobalt Ore", "Cobaltite", "CoAsS", "0.1-0.5 % Co (Cobalt, Ontario)", 0.09, "cobaltite", 2.7, Where.EVERYWHERE, Tool.STONE),
	PYROLUSITE("pyrolusite", "Manganese Ore", "Pyrolusite", "MnO2", "30-48 % Mn (Kalahari; textbook figure)", 0.80, "manganese oxides", 3.2, Where.EVERYWHERE, Tool.STONE),
	PHOSPHATE("phosphate", "Phosphate Rock", "Phosphate Rock", "Ca5(PO4,CO3)3F (francolite)", "18-40 % P2O5 and 70-200 ppm U (Bone Valley, Khouribga)", 0.80, "apatite", 2.6, Where.OCEAN_BEACH, Tool.STONE);

	/** The biomes an ore generates in; OreGen turns each into a biome selector. */
	public enum Where { EVERYWHERE, DEEP, DESERT_BADLANDS, EVAPORITE, SALT_DOME, BEACH, TROPICS, MOUNTAINS, MOUNTAINS_BADLANDS, PLAINS_BADLANDS, OCEAN_BEACH }

	/** The lowest tool that mines it. */
	public enum Tool { SHOVEL, STONE, IRON }

	/** Phosphate rock's uranium (ppm), ten times a typical 130 ppm: the scintillometer hears it faintly (docs/ores.md). */
	public static final double PHOSPHATE_URANIUM_PPM = 1300;
	/** U3O8 is 84.8 % uranium by mass. */
	public static final double U_IN_U3O8 = 0.848;

	public final String key, blockName, mineralName, formula, realGrade, gradeOf;
	public final double grade, density;
	public final Where where;
	public final Tool tool;

	Ore(String key, String blockName, String mineralName, String formula, String realGrade, double grade, String gradeOf, double density, Where where, Tool tool) {
		this.key = key;
		this.blockName = blockName;
		this.mineralName = mineralName;
		this.formula = formula;
		this.realGrade = realGrade;
		this.grade = grade;
		this.gradeOf = gradeOf;
		this.density = density;
		this.where = where;
		this.tool = tool;
	}

	/** The block's id: {@code <key>_ore}. Its mineral item's id is the key. */
	public String blockId() {
		return key + "_ore";
	}

	/** Mass of rock in one block (kg). */
	public double blockKg() {
		return density * 1000;
	}

	/** Mass of the graded mineral or metal in one block (kg). */
	public double kgPerBlock() {
		return blockKg() * grade;
	}

	/** The fraction of the block that is U3O8, for the scintillometer and the mill: zero for ores with no uranium. */
	public double u3o8() {
		return switch (this) {
			case URANINITE, PITCHBLENDE -> grade;
			case PHOSPHATE -> PHOSPHATE_URANIUM_PPM * 1e-6 / U_IN_U3O8;
			default -> 0;
		};
	}

	/** "80 % fluorite, 2,160 kg in a block", or g/t for the precious ores. */
	public String gradeText() {
		if (grade < 0.001) {
			return String.format("%,.0f g/t %s, %,.0f g in a block", grade * 1e6, gradeOf, kgPerBlock() * 1000);
		}
		if (grade < 0.01) {
			return String.format("%,.0f g/t %s, %,.1f kg in a block", grade * 1e6, gradeOf, kgPerBlock());
		}
		return String.format("%.0f %% %s, %,.0f kg in a block", grade * 100, gradeOf, kgPerBlock());
	}
}
