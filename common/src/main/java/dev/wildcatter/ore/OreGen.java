package dev.wildcatter.ore;

import dev.wildcatter.Wildcatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Puts the ores into newly generated chunks. The veins themselves (size, count, height) are data, written by
 * tools/make_assets.py into data/wildcatter/worldgen; this only says which biomes each one goes in. Each loader adds the placed
 * features to those biomes: Fabric by its biome API, NeoForge by the biome modifiers make_assets.py writes from the same table.
 */
public final class OreGen {
	private OreGen() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Wildcatter.id("salt_dome"), SaltDomeFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Wildcatter.id("layered"), LayeredFeature.CODEC);
		Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Wildcatter.id("salt_dome_placement"), SaltDomePlacement.CODEC);
	}

	/** Each placed feature (by its path in data/wildcatter/worldgen/placed_feature) and where it goes. */
	public static Map<String, Ore.Where> placements() {
		Map<String, Ore.Where> out = new LinkedHashMap<>();
		for (Ore ore : Ore.values()) {
			switch (ore) {
				case SULFUR, HALITE, SYLVITE, CHROMITE, PLATINUM -> {
				}   // made by the salt domes, the evaporite beds and the Bushveld reef below
				default -> out.put("ore_" + ore.key, ore.where);
			}
		}
		out.put("salt_dome", Ore.Where.SALT_DOME);   // as before Phase 15, so its place among the features is unchanged; 15a's grid dome checks its own centre's biome
		out.put("evaporite_beds", Ore.Where.EVAPORITE);
		out.put("bushveld_reef", Ore.Where.EVERYWHERE);
		return out;
	}

	/** The biomes a placement goes in: "#namespace:tag" for a biome tag, else a biome's id. Any one of them matches. */
	public static List<String> biomes(Ore.Where where) {
		return switch (where) {
			case EVERYWHERE, DEEP -> List.of("#minecraft:is_overworld");
			case DESERT_BADLANDS, EVAPORITE -> List.of("minecraft:desert", "#minecraft:is_badlands");
			case SALT_DOME -> List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_beach");
			case BEACH -> List.of("#minecraft:is_beach");
			case TROPICS -> List.of("#minecraft:is_jungle", "#minecraft:is_savanna");
			case MOUNTAINS -> List.of("#minecraft:is_mountain");
			case MOUNTAINS_BADLANDS -> List.of("#minecraft:is_mountain", "#minecraft:is_badlands");
			case PLAINS_BADLANDS -> List.of("minecraft:plains", "minecraft:sunflower_plains", "#minecraft:is_badlands");
			case OCEAN_BEACH -> List.of("#minecraft:is_ocean", "#minecraft:is_beach");
			case PLAINS_FOREST -> List.of("minecraft:plains", "minecraft:sunflower_plains", "#minecraft:is_forest", "#minecraft:is_river");
		};
	}
}
