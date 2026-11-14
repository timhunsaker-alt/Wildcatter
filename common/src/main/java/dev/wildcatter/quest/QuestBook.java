package dev.wildcatter.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The quest chapters as read from {@code data/<namespace>/wildcatter_quests/*.json} (docs/quests.md), the same on the server and the client:
 * the server reads the files, the client gets each file's text in the sync payload and reads it with this same class. Plain data, no
 * world access, so the client can hold it.
 */
public final class QuestBook {
	/** The folder under each namespace's data. */
	public static final String FOLDER = "wildcatter_quests";

	public enum Kind {
		ITEM, ADVANCEMENT, MILESTONE
	}

	/** One task: an item id or {@code #tag} with a count, an advancement id, or a milestone key; {@code text} replaces the automatic line. */
	public record Task(Kind kind, String target, int count, String text) {
		public boolean isTag() {
			return kind == Kind.ITEM && target.startsWith("#");
		}
	}

	public record Reward(String item, int count) {
	}

	public record Quest(Identifier id, Identifier chapter, String title, String icon, String text, List<Identifier> requires, List<Task> tasks,
			List<Reward> rewards) {
		/** True if the query (lower case) is in its title or text. */
		public boolean matches(String query) {
			return title.toLowerCase(Locale.ROOT).contains(query) || text.toLowerCase(Locale.ROOT).contains(query);
		}
	}

	public record Chapter(Identifier id, String title, String icon, int order, List<Identifier> after, List<Quest> quests) {
	}

	/** A chapter file as found: its id (namespace and file name) and its text. */
	public record Source(Identifier file, String json) {
	}

	public static final QuestBook EMPTY = new QuestBook(List.of(), List.of());

	private final List<Source> sources;
	private final List<Chapter> chapters = new ArrayList<>();
	private final Map<Identifier, Quest> quests = new LinkedHashMap<>();
	private final Map<Identifier, Chapter> chapterById = new LinkedHashMap<>();
	/** What could not be read, one line each (shown in the log and by the quests_valid check). */
	private final List<String> problems = new ArrayList<>();

	private QuestBook(List<Source> sources, List<String> earlier) {
		this.sources = List.copyOf(sources);
		problems.addAll(earlier);
		for (Source s : sources) {
			try {
				Chapter c = chapter(s.file(), JsonParser.parseString(s.json()).getAsJsonObject());
				if (chapterById.containsKey(c.id())) {
					problems.add(s.file() + ": chapter id " + c.id() + " is used twice");
					continue;
				}
				chapterById.put(c.id(), c);
				chapters.add(c);
				for (Quest q : c.quests()) {
					if (quests.putIfAbsent(q.id(), q) != null) {
						problems.add(s.file() + ": quest id " + q.id() + " is used twice");
					}
				}
			} catch (RuntimeException e) {
				problems.add(s.file() + ": not read: " + e.getMessage());
			}
		}
		chapters.sort(Comparator.comparingInt(Chapter::order).thenComparing(c -> c.id().toString()));
	}

	public static QuestBook of(List<Source> sources) {
		return new QuestBook(sources, List.of());
	}

	static QuestBook of(List<Source> sources, List<String> earlier) {
		return new QuestBook(sources, earlier);
	}

	public List<Source> sources() {
		return sources;
	}

	public List<Chapter> chapters() {
		return chapters;
	}

	public Map<Identifier, Quest> quests() {
		return quests;
	}

	public Quest quest(Identifier id) {
		return quests.get(id);
	}

	public Chapter chapter(Identifier id) {
		return chapterById.get(id);
	}

	public List<String> problems() {
		return problems;
	}

	public boolean isEmpty() {
		return quests.isEmpty();
	}

	// ---- reading ----

	private static Chapter chapter(Identifier file, JsonObject o) {
		String ns = file.getNamespace();
		Identifier id = o.has("id") ? ref(ns, o.get("id").getAsString()) : file;
		List<Quest> quests = new ArrayList<>();
		for (JsonElement e : array(o, "quests")) {
			quests.add(quest(ns, id, e.getAsJsonObject()));
		}
		List<Identifier> after = new ArrayList<>();
		for (JsonElement e : array(o, "after")) {
			after.add(ref(ns, e.getAsString()));
		}
		return new Chapter(id, string(o, "title", id.toString()), string(o, "icon", "minecraft:book"), o.has("order") ? o.get("order").getAsInt() : 1000,
				List.copyOf(after), List.copyOf(quests));
	}

	private static Quest quest(String ns, Identifier chapter, JsonObject o) {
		if (!o.has("id")) {
			throw new IllegalArgumentException("a quest in " + chapter + " has no id");
		}
		Identifier id = ref(ns, o.get("id").getAsString());
		List<Identifier> requires = new ArrayList<>();
		for (JsonElement e : array(o, "requires")) {
			requires.add(ref(ns, e.getAsString()));
		}
		List<Task> tasks = new ArrayList<>();
		for (JsonElement e : array(o, "tasks")) {
			JsonObject t = e.getAsJsonObject();
			String text = string(t, "text", "");
			int count = t.has("count") ? Math.max(1, t.get("count").getAsInt()) : 1;
			if (t.has("item")) {
				tasks.add(new Task(Kind.ITEM, t.get("item").getAsString(), count, text));
			} else if (t.has("advancement")) {
				tasks.add(new Task(Kind.ADVANCEMENT, t.get("advancement").getAsString(), 1, text));
			} else if (t.has("milestone")) {
				tasks.add(new Task(Kind.MILESTONE, t.get("milestone").getAsString(), 1, text));
			} else {
				throw new IllegalArgumentException("quest " + id + " has a task that is not an item, advancement or milestone: " + t);
			}
		}
		List<Reward> rewards = new ArrayList<>();
		for (JsonElement e : array(o, "rewards")) {
			JsonObject r = e.getAsJsonObject();
			rewards.add(new Reward(r.get("item").getAsString(), r.has("count") ? Math.max(1, r.get("count").getAsInt()) : 1));
		}
		return new Quest(id, chapter, string(o, "title", id.toString()), string(o, "icon", "minecraft:book"), string(o, "text", ""),
				List.copyOf(requires), List.copyOf(tasks), List.copyOf(rewards));
	}

	/** A bare id takes the file's namespace; a full one is kept. */
	static Identifier ref(String ns, String id) {
		Identifier out = id.contains(":") ? Identifier.tryParse(id) : Identifier.tryBuild(ns, id);
		if (out == null) {
			throw new IllegalArgumentException("not an id: " + id);
		}
		return out;
	}

	private static JsonArray array(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
	}

	private static String string(JsonObject o, String key, String fallback) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback;
	}
}
