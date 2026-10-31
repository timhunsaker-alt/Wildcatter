package dev.wildcatter.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Re-checks what the model tool (tools/models/) wrote, so a hand edit or a tool bug cannot ship a model Minecraft rejects:
 * every box within -16..32, every face's texture defined and on disk, and every true-size part's shape file covering each
 * of its pieces.
 */
class ModelLintTest {
	private static final Path ASSETS = Path.of("src/main/resources/assets/wildcatter");

	private static JsonObject read(Path file) throws IOException {
		return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
	}

	@Test
	void everyBoxFitsAndEveryFaceHasATexture() throws IOException {
		List<String> problems = new ArrayList<>();
		try (Stream<Path> files = Files.walk(ASSETS.resolve("models/block"))) {
			for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
				JsonObject model = read(file);
				if (!model.has("elements")) {
					continue;
				}
				JsonObject textures = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
				for (Map.Entry<String, JsonElement> t : textures.entrySet()) {
					String path = t.getValue().getAsString();
					if (path.startsWith("wildcatter:") && !Files.exists(ASSETS.resolve("textures/" + path.substring("wildcatter:".length()) + ".png"))) {
						problems.add(file + ": texture " + path + " is not on disk");
					}
				}
				for (JsonElement e : model.getAsJsonArray("elements")) {
					JsonObject element = e.getAsJsonObject();
					for (String end : new String[] {"from", "to"}) {
						for (JsonElement v : element.getAsJsonArray(end)) {
							double d = v.getAsDouble();
							if (d < -16 || d > 32) {
								problems.add(file + ": a box's " + end + " is at " + d + ", outside -16..32");
							}
						}
					}
					for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
						String ref = face.getValue().getAsJsonObject().get("texture").getAsString();
						if (ref.startsWith("#") && !textures.has(ref.substring(1)) && !model.has("parent")) {
							problems.add(file + ": face " + face.getKey() + " uses " + ref + ", which is not defined");
						}
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void everyTrueSizePartHasAShapeForEachPiece() throws IOException {
		Path shapes = ASSETS.resolve("shapes");
		if (!Files.isDirectory(shapes)) {
			return;
		}
		try (Stream<Path> files = Files.list(shapes)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
				String name = file.getFileName().toString().replace(".json", "");
				JsonArray pieces = read(file).getAsJsonArray("pieces");
				JsonObject variants = read(ASSETS.resolve("blockstates/" + name + ".json")).getAsJsonObject("variants");
				long northPieces = variants.keySet().stream().filter(k -> k.contains("facing=north")).count();
				assertEquals(northPieces, pieces.size(), name + ": one shape list per piece");
				for (JsonElement piece : pieces) {
					for (JsonElement box : piece.getAsJsonArray()) {
						for (JsonElement v : box.getAsJsonArray()) {
							assertTrue(v.getAsDouble() >= 0 && v.getAsDouble() <= 16, name + ": collision boxes stay inside their cell");
						}
					}
				}
			}
		}
	}
}
