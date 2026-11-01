package dev.wildcatter.ore;

import dev.wildcatter.Wildcatter;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Puts the ores into newly generated chunks. The veins themselves (size, count, height) are data, written by
 * tools/make_assets.py into data/wildcatter/worldgen; this only says which biomes each one goes in.
 */
public final class OreGen {
	private OreGen() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Wildcatter.id("salt_dome"), SaltDomeFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Wildcatter.id("layered"), LayeredFeature.CODEC);

		for (Ore ore : Ore.values()) {
			switch (ore) {
				case SULFUR, HALITE, SYLVITE, CHROMITE, PLATINUM -> {
				}   // made by the salt domes, the evaporite beds and the Bushveld reef below
				default -> add("ore_" + ore.key, ore.where);
			}
		}
		add("salt_dome", Ore.Where.SALT_DOME);
		add("evaporite_beds", Ore.Where.EVAPORITE);
		add("bushveld_reef", Ore.Where.EVERYWHERE);
	}

	private static void add(String name, Ore.Where where) {
		ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Wildcatter.id(name));
		BiomeModifications.addFeature(biomes(where), GenerationStep.Decoration.UNDERGROUND_ORES, key);
	}

	static Predicate<BiomeSelectionContext> biomes(Ore.Where where) {
		Predicate<BiomeSelectionContext> desert = BiomeSelectors.includeByKey(Biomes.DESERT);
		Predicate<BiomeSelectionContext> badlands = BiomeSelectors.tag(BiomeTags.IS_BADLANDS);
		return switch (where) {
			case EVERYWHERE, DEEP -> BiomeSelectors.foundInOverworld();
			case DESERT_BADLANDS, EVAPORITE -> desert.or(badlands);
			case SALT_DOME -> BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
					.or(BiomeSelectors.tag(BiomeTags.IS_BEACH));
			case BEACH -> BiomeSelectors.tag(BiomeTags.IS_BEACH);
			case TROPICS -> BiomeSelectors.tag(BiomeTags.IS_JUNGLE).or(BiomeSelectors.tag(BiomeTags.IS_SAVANNA));
			case MOUNTAINS -> BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN);
			case MOUNTAINS_BADLANDS -> BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN).or(badlands);
			case PLAINS_BADLANDS -> BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS).or(badlands);
			case OCEAN_BEACH -> BiomeSelectors.tag(BiomeTags.IS_OCEAN).or(BiomeSelectors.tag(BiomeTags.IS_BEACH));
		};
	}
}
