package dev.wildcatter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcatter.Rules.Rule;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * The world's {@link Rules}, saved with the overworld (as its electrical standard is). The first time a world loads with this version
 * it has no rules file, and the overworld's level data says whether the world is new: a world being created is not yet
 * {@linkplain ServerLevelData#isInitialized() initialized} (vanilla sets that once it has placed the spawn, before the first save), and
 * every world saved before is. A new world gets each rule's new-world default; an existing one gets every rule off. A rule that is not in
 * the file (added by a later version) is off: the world existed before it. The verdict is kept in the file as {@code origin}.
 */
public final class RulesData extends SavedData {
	public static final Codec<RulesData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("rules").forGetter(d -> d.values),
			Codec.STRING.optionalFieldOf("origin", "existing").forGetter(d -> d.origin)).apply(i, RulesData::new));
	public static final SavedDataType<RulesData> TYPE = new SavedDataType<>(Wildcatter.id("rules"), RulesData::fresh, CODEC, DataFixTypes.LEVEL);

	/** Game test harnesses set this so the worlds they make start as an existing world would (every rule off): their scenes were written
	 * for that and switch on the rules they test. The world's {@code origin} is still found and saved as in play. */
	public static volatile boolean testWorldsAsExisting;

	private final Map<String, Boolean> values;
	private final String origin;

	private RulesData(Map<String, Boolean> values, String origin) {
		this.values = new LinkedHashMap<>(values);
		this.origin = origin;
	}

	/** Only reached through {@link #load}, which decides new or existing first; read from disk the codec is used instead. */
	private static RulesData fresh() {
		return new RulesData(Map.of(), "existing");
	}

	public String origin() {
		return origin;
	}

	/** The rule as saved with the world (off if the file has no such key). */
	public boolean saved(Rule rule) {
		return values.getOrDefault(rule.key, false);
	}

	/** Loads (or creates) the world's rules into {@link Rules}. Call as the overworld loads, before any of its chunks generate. */
	public static void load(ServerLevel overworld) {
		Rules.clear();
		RulesData data = overworld.getDataStorage().get(TYPE);
		if (data == null) {
			boolean newWorld = !(overworld.getLevelData() instanceof ServerLevelData level) || !level.isInitialized();
			Map<String, Boolean> start = new LinkedHashMap<>();
			for (Rule r : Rule.values()) {
				start.put(r.key, !testWorldsAsExisting && Rules.defaultFor(r, newWorld));
			}
			data = new RulesData(start, newWorld ? "new" : "existing");
			overworld.getDataStorage().set(TYPE, data);
			data.setDirty();
			Wildcatter.LOGGER.info("World rules: no rules saved yet; the overworld {} initialized, so this is {} world{}", newWorld ? "is not yet" : "is",
					newWorld ? "a NEW" : "an EXISTING", testWorldsAsExisting ? " (game test: every rule starts off)" : "");
		}
		for (Rule r : Rule.values()) {
			Rules.set(r, data.values.getOrDefault(r.key, false));
		}
		RulesData kept = data;
		Rules.listen((rule, on) -> {
			kept.values.put(rule.key, on);
			kept.setDirty();
		});
		Wildcatter.LOGGER.info("World rules ({} world): on: {}", data.origin, Rules.describe());
	}

	/** The rules of the world now loaded, for the command and the tests. */
	public static RulesData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}
}
