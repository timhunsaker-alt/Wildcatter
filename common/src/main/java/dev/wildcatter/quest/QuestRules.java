package dev.wildcatter.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;

/**
 * The check every chapter must pass (docs/quests.md, "The check"): ids resolve, prerequisites exist with no cycle, and rewards are small
 * things a player can make by hand, never past a gate. The server logs what it finds when it reads the chapters; the {@code quests_valid}
 * scene fails on any of it, and an add-on's own test can call {@link #problems(MinecraftServer)}.
 */
public final class QuestRules {
	/**
	 * Never a reward, by any part of the item's path: plastic and what only plastic makes (every cable and busway), coke, platinum, and the
	 * nuclear and refined-metal products that only a plant makes.
	 */
	public static final List<String> NEVER_REWARD = List.of("plastic", "polyethylene", "cable", "busway", "coke", "platinum", "yellowcake",
			"uranium", "uf6", "enriched", "fuel_assembly", "fuel_pellet", "rcca", "zirconium", "zircaloy", "hafnium", "helium", "boron",
			"ammonia", "aluminium", "aluminum", "titanium", "lithium", "nickel", "ferro", "silicon_metal", "chromium", "spent");

	/** Recipe types a player works by hand: the crafting table, the furnaces and the stonecutter. */
	private static final Set<String> HAND_RECIPES = Set.of("minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting",
			"minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting", "minecraft:crafting_transmute");

	private QuestRules() {
	}

	/** True if the item may never be given as a reward. */
	public static boolean neverReward(Identifier item) {
		if (item == null) {
			return true;
		}
		String path = item.getPath();
		for (String word : NEVER_REWARD) {
			if (path.contains(word)) {
				return true;
			}
		}
		return false;
	}

	/** Every problem with the server's chapters; empty when every chapter is good. */
	public static List<String> problems(MinecraftServer server) {
		QuestBook book = QuestTracker.book(server);
		List<String> out = new ArrayList<>(book.problems());
		out.addAll(problems(server, book));
		return out;
	}

	public static List<String> problems(MinecraftServer server, QuestBook book) {
		List<String> out = new ArrayList<>();
		Set<Identifier> handMade = handMade(server.getResourceManager());
		for (QuestBook.Chapter c : book.chapters()) {
			item(out, c.id() + " icon", c.icon());
			for (Identifier after : c.after()) {
				if (book.chapter(after) == null) {
					out.add(c.id() + ": comes after " + after + ", which is not a chapter");
				}
			}
			if (c.quests().isEmpty()) {
				out.add(c.id() + ": has no quests");
			}
		}
		for (QuestBook.Quest q : book.quests().values()) {
			item(out, q.id() + " icon", q.icon());
			for (Identifier need : q.requires()) {
				if (book.quest(need) == null) {
					out.add(q.id() + ": needs " + need + ", which is not a quest");
				}
			}
			if (q.tasks().isEmpty() && q.requires().isEmpty()) {
				out.add(q.id() + ": has no tasks and needs nothing, so it is done at once");
			}
			for (QuestBook.Task t : q.tasks()) {
				switch (t.kind()) {
					case ITEM -> item(out, q.id() + " task", t.target());
					case ADVANCEMENT -> {
						Identifier id = Identifier.tryParse(t.target());
						if (id == null || server.getAdvancements().get(id) == null) {
							out.add(q.id() + ": advancement " + t.target() + " does not exist");
						}
					}
					case MILESTONE -> {
						if (Identifier.tryParse(t.target()) == null) {
							out.add(q.id() + ": milestone " + t.target() + " is not an id");
						}
						if (t.text().isBlank()) {
							out.add(q.id() + ": milestone " + t.target() + " has no text");
						}
					}
				}
			}
			for (QuestBook.Reward r : q.rewards()) {
				Identifier id = Identifier.tryParse(r.item());
				if (!item(out, q.id() + " reward", r.item())) {
					continue;
				}
				if (neverReward(id)) {
					out.add(q.id() + ": reward " + r.item() + " is on the never-reward list");
				} else if (!id.getNamespace().equals("minecraft") && !handMade.contains(id)) {
					out.add(q.id() + ": reward " + r.item() + " has no hand recipe (crafting table, furnace or stonecutter), so only a machine makes it");
				}
				if (r.count() > 64) {
					out.add(q.id() + ": reward " + r.item() + " x" + r.count() + " is more than a stack");
				}
			}
		}
		cycles(out, book);
		return out;
	}

	/** True if the item or tag exists; otherwise a line in {@code out}. */
	private static boolean item(List<String> out, String where, String target) {
		if (target.startsWith("#")) {
			Identifier id = Identifier.tryParse(target.substring(1));
			if (id == null || BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id)).isEmpty()) {
				out.add(where + ": item tag " + target + " does not exist");
				return false;
			}
			return true;
		}
		Identifier id = Identifier.tryParse(target);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			out.add(where + ": item " + target + " does not exist");
			return false;
		}
		return true;
	}

	private static void cycles(List<String> out, QuestBook book) {
		Map<Identifier, Integer> state = new HashMap<>();   // 1 visiting, 2 done
		for (Identifier id : book.quests().keySet()) {
			visit(out, book, id, state, new ArrayList<>());
		}
	}

	private static void visit(List<String> out, QuestBook book, Identifier id, Map<Identifier, Integer> state, List<Identifier> path) {
		Integer s = state.get(id);
		if (s != null && s == 2) {
			return;
		}
		if (s != null) {
			out.add("prerequisite cycle: " + path.subList(path.indexOf(id), path.size()) + " -> " + id);
			return;
		}
		QuestBook.Quest q = book.quest(id);
		if (q == null) {
			return;
		}
		state.put(id, 1);
		path.add(id);
		for (Identifier need : q.requires()) {
			visit(out, book, need, state, path);
		}
		path.remove(path.size() - 1);
		state.put(id, 2);
	}

	/** Every item some hand recipe makes, read from the server's recipe files. */
	private static Set<Identifier> handMade(ResourceManager manager) {
		Set<Identifier> out = new HashSet<>();
		manager.listResources("recipe", id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
			try (Reader reader = resource.openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				String type = json.has("type") ? json.get("type").getAsString() : "";
				if (!type.contains(":")) {
					type = "minecraft:" + type;
				}
				if (HAND_RECIPES.contains(type) && json.has("result")) {
					var result = json.get("result");
					String item = result.isJsonObject() ? result.getAsJsonObject().get("id").getAsString() : result.getAsString();
					Identifier id = Identifier.tryParse(item);
					if (id != null) {
						out.add(id);
					}
				}
			} catch (Exception e) {
				// not a recipe we can read: it makes nothing we count
			}
		});
		return out;
	}
}
