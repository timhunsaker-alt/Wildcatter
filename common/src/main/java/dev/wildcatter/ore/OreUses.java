package dev.wildcatter.ore;

import java.util.ArrayList;
import java.util.List;

/**
 * Where each ore is found, what mines it and what uses its mineral, for the tooltips and the JEI pages (docs/ores.md). The
 * switches are exhaustive, so a new ore does not compile until it is described here. A wave that gives an ore a new user
 * updates {@link #uses} in the same change. No Minecraft imports: OreUsesTest reads it.
 */
public final class OreUses {
	private OreUses() {
	}

	/** Where it generates, from docs/ores.md. */
	public static String found(Ore ore) {
		return switch (ore) {
			case URANINITE -> "deserts and badlands, y 0 to 64";
			case PITCHBLENDE -> "deep everywhere, y -64 to -40";
			case SULFUR -> "salt dome caprock, y 0 to 24 (domes trap oil on their flanks)";
			case HALITE -> "salt domes (which trap oil on their flanks); evaporite beds in deserts, y -32 to 0";
			case SYLVITE -> "evaporite beds under deserts and badlands, y -32 to 0";
			case FLUORITE -> "everywhere, y -16 to 48";
			case HEAVY_MINERAL -> "beaches, y 55 to 70";
			case BAUXITE -> "jungles and savannas, y 55 to 80";
			case GALENA, SPHALERITE -> "everywhere, y -16 to 64";
			case CASSITERITE, MOLYBDENITE -> "mountains, y 0 to 80";
			case PENTLANDITE -> "deepslate everywhere, y -64 to -16";
			case CHROMITE, PLATINUM -> "the Bushveld reef, deep everywhere, y -64 to -38";
			case MAGNESITE, SPODUMENE -> "everywhere, y 0 to 64";
			case DOLOMITE -> "everywhere, y 0 to 100";
			case GRAPHITE, COBALTITE -> "everywhere, y -32 to 32";
			case BORAX -> "deserts and badlands, y 50 to 80";
			case BARITE, PYROLUSITE -> "everywhere, y 0 to 48";
			case BENTONITE -> "plains and badlands, y 40 to 70";
			case ACANTHITE -> "mountains and badlands, y -16 to 48";
			case SCHEELITE -> "everywhere, y -16 to 48";
			case PHOSPHATE -> "oceans and beaches, y 30 to 60";
			case LIMESTONE -> "plains, forests and rivers, y 40 to 90";
		};
	}

	/** The lowest tool that mines it. */
	public static String tool(Ore ore) {
		return switch (ore.tool) {
			case SHOVEL -> "a shovel";
			case STONE -> "a stone pickaxe or better";
			case IRON -> "an iron pickaxe or better";
		};
	}

	/** Each plant that takes its mineral and what that plant makes; empty when nothing uses it yet. */
	public static List<String> uses(Ore ore) {
		return switch (ore) {
			case URANINITE, PITCHBLENDE -> List.of("the uranium mill's jaw crusher, which feeds the mill (yellowcake)");
			case SULFUR -> List.of("the acid plant's sulfur melter (sulfuric acid)", "the chromate kiln (it reduces the chromate)");
			case HALITE -> List.of("the uranium mill's SX circuit and IX columns (strip salt)", "chlor-alkali cells (chlorine, caustic soda)", "the chlor-alkali plant's brine saturator, or a brine well in a salt dome (chlorine, caustic soda, hydrochloric acid)",
					"a land rig's mud hopper (salt-saturated mud for drilling through salt)");
			case BARITE -> List.of("the barite mill (drilling-grade barite, which weights a land rig's mud)");
			case BENTONITE -> List.of("the bentonite dryer (drilling bentonite, which gels a land rig's mud)");
			case FLUORITE -> List.of("the conversion plant's HF kiln (hydrogen fluoride)", "the Pidgeon furnace (3 % of its briquettes)");
			case HEAVY_MINERAL -> List.of("the zirconium plant's mineral separator (zircon; with a hopper under it, ilmenite and rutile for the titanium pigment plant)");
			case CASSITERITE -> List.of("a furnace (tin ingot, which the zirconium plant's VAR furnace alloys into Zircaloy)");
			case CHROMITE -> List.of("the zirconium plant's VAR furnace, alloyed into Zircaloy", "the submerged arc furnace (HC ferrochrome)", "the chromate kiln (chromic oxide)");
			case MAGNESITE -> List.of("a furnace (magnesia), for the uranium mill's precipitation and the magnesia chlorinator (magnesium)");
			case BORAX -> List.of("the boron plant (boric acid, boron carbide)");
			case BAUXITE -> List.of("the Bayer refinery's ball mill (alumina)");
			case PYROLUSITE -> List.of("the uranium mill's leach tanks, as the oxidant", "the submerged arc furnace (HC ferromanganese)");
			case LIMESTONE -> List.of("the lime kiln (quicklime for the Bayer refinery)", "the submerged arc furnace, as ferromanganese's flux");
			case MOLYBDENITE -> List.of("the moly roaster, as flotation concentrate from Tailings' concentrator (moly oxide, then ferromolybdenum)");
			case SCHEELITE -> List.of("the aluminothermic vessel, as flotation concentrate from Tailings' concentrator dried in the moly roaster (ferrotungsten)");
			case GALENA -> List.of("a furnace (lead ingot)");
			case SPHALERITE -> List.of("a furnace (zinc ingot, which with an animal fat makes zinc stearate for the fuel pellet press)");
			case ACANTHITE -> List.of("a furnace (silver ingot)");
			case PHOSPHATE -> List.of("the phosphate mill (phosphoric acid, TSP and DAP fertilizer; uranium from the acid)");
			case SYLVITE -> List.of("the potash dissolver (muriate of potash fertilizer)");
			case PENTLANDITE -> List.of("the nickel smelter's fluid-bed roaster, as ore or as flotation concentrate from Tailings' concentrator (Bessemer matte, then cathode nickel at the refinery)");
			case SPODUMENE -> List.of("the lithium plant's decrepitation kiln (lithium hydroxide and carbonate)");
			case DOLOMITE -> List.of("the lime kiln, burned to dolime for the Pidgeon furnace (magnesium)");
			case PLATINUM, GRAPHITE, COBALTITE -> List.of();
		};
	}

	/** The tooltip lines after the mineral's own: found, mined with, used in (or "No use yet"). */
	public static List<String> lines(Ore ore) {
		List<String> uses = uses(ore);
		ArrayList<String> out = new ArrayList<>();
		out.add("Found: " + found(ore));
		out.add("Mined with " + tool(ore));
		if (uses.isEmpty()) {
			out.add("No use yet");
		} else {
			for (String use : uses) {
				out.add("Used in " + use);
			}
		}
		return out;
	}
}
