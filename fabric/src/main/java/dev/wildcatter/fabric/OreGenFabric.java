package dev.wildcatter.fabric;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.ore.OreGen;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;

/** Adds each ore's placed feature to its biomes ({@link OreGen#biomes}) through Fabric's biome API. */
final class OreGenFabric {
	private OreGenFabric() {
	}

	static void register() {
		OreGen.placements().forEach((name, where) -> {
			Predicate<BiomeSelectionContext> biomes = context -> false;
			for (String entry : OreGen.biomes(where)) {
				biomes = biomes.or(entry.startsWith("#")
						? BiomeSelectors.tag(TagKey.create(Registries.BIOME, Identifier.parse(entry.substring(1))))
						: BiomeSelectors.includeByKey(ResourceKey.create(Registries.BIOME, Identifier.parse(entry))));
			}
			BiomeModifications.addFeature(biomes, GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, Wildcatter.id(name)));
		});
	}
}
