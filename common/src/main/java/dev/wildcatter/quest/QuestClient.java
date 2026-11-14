package dev.wildcatter.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;

/**
 * What the client knows of the quest book: the chapters and this player's progress, as the server last sent them. Plain data with no
 * client classes, so it can live in common; the quest screen reads it.
 */
public final class QuestClient {
	public enum State {
		DONE, NEXT, LATER
	}

	private static QuestBook book = QuestBook.EMPTY;
	private static Set<String> done = Set.of();
	private static Set<String> claimed = Set.of();
	/** "quest id#task index" for each task of an open quest that is met now. */
	private static Set<String> met = Set.of();
	private static Map<String, String> advancementTitles = Map.of();
	/** Counts up each time something arrives, so an open screen knows to lay itself out again. */
	private static int version;

	private QuestClient() {
	}

	public static void fromServer(CompoundTag tag) {
		if (tag.contains("chapters")) {
			List<QuestBook.Source> sources = new ArrayList<>();
			ListTag list = tag.getListOrEmpty("chapters");
			for (int i = 0; i < list.size(); i++) {
				CompoundTag c = list.getCompoundOrEmpty(i);
				Identifier file = Identifier.tryParse(c.getStringOr("file", ""));
				if (file != null) {
					sources.add(new QuestBook.Source(file, c.getStringOr("json", "{}")));
				}
			}
			book = QuestBook.of(sources);
			Map<String, String> titles = new HashMap<>();
			CompoundTag adv = tag.getCompoundOrEmpty("advancements");
			for (String key : adv.keySet()) {
				titles.put(key, adv.getStringOr(key, key));
			}
			advancementTitles = titles;
		}
		if (tag.contains("progress")) {
			CompoundTag p = tag.getCompoundOrEmpty("progress");
			done = strings(p.getListOrEmpty("done"));
			claimed = strings(p.getListOrEmpty("claimed"));
			met = strings(p.getListOrEmpty("met"));
		}
		version++;
	}

	private static Set<String> strings(ListTag list) {
		Set<String> out = new HashSet<>();
		for (int i = 0; i < list.size(); i++) {
			list.getString(i).ifPresent(out::add);
		}
		return out;
	}

	public static QuestBook book() {
		return book;
	}

	public static int version() {
		return version;
	}

	public static boolean done(Identifier quest) {
		return done.contains(quest.toString());
	}

	public static boolean claimed(Identifier quest) {
		return claimed.contains(quest.toString());
	}

	public static boolean met(Identifier quest, int task) {
		return done(quest) || met.contains(quest + "#" + task);
	}

	public static String advancementTitle(String id) {
		return advancementTitles.getOrDefault(id, id);
	}

	/** Done; next when every quest it needs is done (or it needs none); otherwise later. */
	public static State state(QuestBook.Quest quest) {
		if (done(quest.id())) {
			return State.DONE;
		}
		for (Identifier need : quest.requires()) {
			if (!done(need)) {
				return State.LATER;
			}
		}
		return State.NEXT;
	}

	/** Forgets everything (leaving a world). */
	public static void clear() {
		book = QuestBook.EMPTY;
		done = Set.of();
		claimed = Set.of();
		met = Set.of();
		advancementTitles = Map.of();
		version++;
	}
}
