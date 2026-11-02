package dev.wildcatter.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** NeoForge's biome modifiers (written by tools/make_assets.py) put each ore in the same biomes as Fabric's code does. */
class OreGenTest {
	private static final Path MODIFIERS = Path.of("../neoforge/src/main/resources/data/wildcatter/neoforge/biome_modifier");

	private static void collect(JsonElement set, Set<String> out) {
		if (set.isJsonPrimitive()) {
			out.add(set.getAsString());
		} else if (set.isJsonArray()) {
			set.getAsJsonArray().forEach(e -> collect(e, out));
		} else {
			JsonObject or = set.getAsJsonObject();
			assertEquals("neoforge:or", or.get("type").getAsString());
			or.getAsJsonArray("values").forEach(e -> collect(e, out));
		}
	}

	@Test
	void neoForgeModifiersMatchTheTable() throws IOException {
		Map<String, Ore.Where> placements = OreGen.placements();
		try (var files = Files.list(MODIFIERS)) {
			assertEquals(placements.size(), files.count(), "one biome modifier per placed feature");
		}
		for (Map.Entry<String, Ore.Where> p : placements.entrySet()) {
			Path file = MODIFIERS.resolve(p.getKey() + ".json");
			assertTrue(Files.exists(file), "missing " + file);
			JsonObject modifier = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			assertEquals("wildcatter:" + p.getKey(), modifier.get("features").getAsString());
			assertEquals("underground_ores", modifier.get("step").getAsString());
			Set<String> biomes = new HashSet<>();
			collect(modifier.get("biomes"), biomes);
			assertEquals(new HashSet<>(OreGen.biomes(p.getValue())), biomes, p.getKey());
		}
	}
}
