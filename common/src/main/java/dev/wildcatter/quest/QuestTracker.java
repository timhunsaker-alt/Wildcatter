package dev.wildcatter.quest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.platform.Platform;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Finds quests done (docs/quests.md, "Detection"), on the server: every 20 ticks it reads each player's inventory, advancements and
 * milestones against their open quests; it notices a player joining (the book, given once, and the chapters sent) and the data packs
 * reloading (the chapters read again and sent to everyone). Needs no loader event beyond the server tick.
 */
public final class QuestTracker {
	public static final int PERIOD = 20;
	/** Kept on the player once the book is given, so a second join, death or relog gives nothing. */
	public static final String GIVEN_TAG = "wildcatter.quest_book_given";

	private static ResourceManager readFrom;
	private static QuestBook book = QuestBook.EMPTY;
	private static final Set<UUID> ONLINE = new HashSet<>();
	private static final Map<UUID, String> LAST_SENT = new HashMap<>();
	/** Players to check on the next tick instead of waiting for the period (a milestone, a claim). */
	private static final Set<UUID> POKED = new HashSet<>();

	private QuestTracker() {
	}

	/** The chapters the server has, read again whenever the data packs reload. */
	public static synchronized QuestBook book(MinecraftServer server) {
		ResourceManager manager = server.getResourceManager();
		if (manager != null && manager != readFrom) {
			readFrom = manager;
			book = load(manager);
			for (String problem : book.problems()) {
				Wildcatter.LOGGER.warn("Quest book: {}", problem);
			}
			List<String> bad = QuestRules.problems(server, book);
			bad.forEach(p -> Wildcatter.LOGGER.warn("Quest book: {}", p));
			Wildcatter.LOGGER.info("Quest book: {} chapters, {} quests", book.chapters().size(), book.quests().size());
			LAST_SENT.clear();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (ONLINE.contains(player.getUUID())) {
					sendChapters(player, server);
				}
			}
		}
		return book;
	}

	static QuestBook load(ResourceManager manager) {
		List<QuestBook.Source> sources = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		manager.listResources(QuestBook.FOLDER, id -> id.getPath().endsWith(".json")).entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> {
					Identifier file = entry.getKey();
					String path = file.getPath().substring(QuestBook.FOLDER.length() + 1, file.getPath().length() - ".json".length());
					try (Reader reader = entry.getValue().openAsReader()) {
						StringBuilder text = new StringBuilder();
						char[] buf = new char[4096];
						for (int n; (n = reader.read(buf)) > 0; ) {
							text.append(buf, 0, n);
						}
						sources.add(new QuestBook.Source(Identifier.fromNamespaceAndPath(file.getNamespace(), path), text.toString()));
					} catch (Exception e) {
						problems.add(file + ": not read: " + e);
					}
				});
		return QuestBook.of(sources, problems);
	}

	public static void tick(MinecraftServer server) {
		QuestBook b = book(server);
		boolean period = server.getTickCount() % PERIOD == 0;
		if (!period && POKED.isEmpty()) {
			return;
		}
		Set<UUID> now = new HashSet<>();
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			UUID id = player.getUUID();
			now.add(id);
			if (!ONLINE.contains(id)) {
				joined(player, server);
			}
			if (period || POKED.contains(id)) {
				check(player, b);
			}
		}
		POKED.clear();
		ONLINE.retainAll(now);
		ONLINE.addAll(now);
		LAST_SENT.keySet().retainAll(now);
	}

	/** A player is here for the first time this session: the book (once ever), the chapters, then their progress. */
	static void joined(ServerPlayer player, MinecraftServer server) {
		ONLINE.add(player.getUUID());
		giveOnce(player);
		sendChapters(player, server);
		LAST_SENT.remove(player.getUUID());
	}

	/** Gives the quest book the first time a player joins; true if it was given now. Dropped at their feet if the inventory is full. */
	public static boolean giveOnce(ServerPlayer player) {
		if (player.entityTags().contains(GIVEN_TAG)) {
			return false;
		}
		player.addTag(GIVEN_TAG);
		ItemStack stack = new ItemStack(WildcatterItems.QUEST_BOOK);
		if (!player.getInventory().add(stack)) {
			player.spawnAtLocation(player.level(), stack);
		}
		return true;
	}

	/** Asks for the player to be checked on the next tick. */
	static void poke(ServerPlayer player) {
		POKED.add(player.getUUID());
	}

	/** Checks one player's open quests now, marks those done, and sends their progress if it changed. Returns the quests done now. */
	public static List<QuestBook.Quest> check(ServerPlayer player) {
		return check(player, book(player.level().getServer()));
	}

	private static List<QuestBook.Quest> check(ServerPlayer player, QuestBook b) {
		MinecraftServer server = player.level().getServer();
		QuestData data = QuestData.get(server);
		QuestData.Progress p = data.of(player.getStringUUID());
		List<QuestBook.Quest> finished = new ArrayList<>();
		List<String> met = new ArrayList<>();
		boolean changed = false;
		boolean again = true;
		while (again) {   // a quest with no tasks follows the ones it needs in the same pass
			again = false;
			met.clear();
			for (QuestBook.Quest q : b.quests().values()) {
				if (p.done.contains(q.id().toString())) {
					continue;
				}
				boolean all = true;
				for (int i = 0; i < q.tasks().size(); i++) {
					QuestBook.Task task = q.tasks().get(i);
					String key = q.id() + "#" + i;
					boolean kept = task.kind() == QuestBook.Kind.ITEM && p.items.contains(key);
					if (kept || met(player, p, task)) {
						met.add(key);
						if (task.kind() == QuestBook.Kind.ITEM && !kept) {
							p.items.add(key);   // an item task met once stays met: craft the machine, set it down, run it
							changed = true;
						}
					} else {
						all = false;
					}
				}
				if (q.tasks().isEmpty()) {
					all = q.requires().stream().allMatch(r -> p.done.contains(r.toString()));
				}
				if (all) {
					p.done.add(q.id().toString());
					String prefix = q.id() + "#";
					p.items.removeIf(k -> k.startsWith(prefix));   // a done quest's tasks are no longer needed
					finished.add(q);
					again = true;
				}
			}
		}
		if (changed || !finished.isEmpty()) {
			data.changed();
		}
		if (!finished.isEmpty()) {
			if (finished.size() <= 3) {
				for (QuestBook.Quest q : finished) {
					player.sendSystemMessage(Component.translatable("quest.wildcatter.done", q.title()).withStyle(ChatFormatting.GOLD));
				}
			} else {
				player.sendSystemMessage(Component.translatable("quest.wildcatter.done_many", finished.size()).withStyle(ChatFormatting.GOLD));
			}
		}
		sendProgress(player, p, met);
		return finished;
	}

	/**
	 * True if the task is met for this player now: an item carried at its count, an advancement had, a milestone reached. {@link #check}
	 * also counts an item task met once before as met (kept in {@link QuestData.Progress#items}).
	 */
	public static boolean met(ServerPlayer player, QuestData.Progress p, QuestBook.Task task) {
		return switch (task.kind()) {
			case ITEM -> count(player.getInventory(), task.target()) >= task.count();
			case ADVANCEMENT -> {
				Identifier id = Identifier.tryParse(task.target());
				AdvancementHolder holder = id == null ? null : player.level().getServer().getAdvancements().get(id);
				yield holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
			}
			case MILESTONE -> p.milestones.contains(task.target());
		};
	}

	/** How many of an item or {@code #tag} the inventory holds, components ignored. */
	public static int count(Inventory inventory, String target) {
		TagKey<Item> tag = null;
		Item item = null;
		if (target.startsWith("#")) {
			Identifier id = Identifier.tryParse(target.substring(1));
			if (id == null) {
				return 0;
			}
			tag = TagKey.create(Registries.ITEM, id);
		} else {
			Identifier id = Identifier.tryParse(target);
			item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
			if (item == Items.AIR) {
				return 0;
			}
		}
		int n = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && (tag != null ? stack.is(tag) : stack.getItem() == item)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	/** Claims a done quest's rewards: true if they were given now. A full inventory drops them at the player's feet. */
	public static boolean claim(ServerPlayer player, Identifier quest) {
		MinecraftServer server = player.level().getServer();
		QuestBook.Quest q = book(server).quest(quest);
		QuestData data = QuestData.get(server);
		QuestData.Progress p = data.of(player.getStringUUID());
		if (q == null || q.rewards().isEmpty() || !p.done.contains(quest.toString()) || p.claimed.contains(quest.toString())) {
			return false;
		}
		p.claimed.add(quest.toString());
		data.changed();
		for (QuestBook.Reward r : q.rewards()) {
			Identifier id = Identifier.tryParse(r.item());
			Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
			if (item == Items.AIR || QuestRules.neverReward(id)) {
				continue;
			}
			ItemStack stack = new ItemStack(item, r.count());
			if (!player.getInventory().add(stack) && !stack.isEmpty()) {
				player.spawnAtLocation(player.level(), stack);
			}
		}
		poke(player);
		return true;
	}

	private static void sendChapters(ServerPlayer player, MinecraftServer server) {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		CompoundTag titles = new CompoundTag();
		for (QuestBook.Source s : book.sources()) {
			CompoundTag c = new CompoundTag();
			c.putString("file", s.file().toString());
			c.putString("json", s.json());
			list.add(c);
		}
		for (QuestBook.Quest q : book.quests().values()) {
			for (QuestBook.Task t : q.tasks()) {
				if (t.kind() == QuestBook.Kind.ADVANCEMENT) {
					Identifier id = Identifier.tryParse(t.target());
					AdvancementHolder holder = id == null ? null : server.getAdvancements().get(id);
					if (holder != null) {
						holder.value().display().ifPresent(d -> titles.putString(t.target(), d.title().getString()));
					}
				}
			}
		}
		tag.put("chapters", list);
		tag.put("advancements", titles);
		send(player, tag);
	}

	private static void sendProgress(ServerPlayer player, QuestData.Progress p, List<String> met) {
		CompoundTag progress = new CompoundTag();
		progress.put("done", strings(p.done));
		progress.put("claimed", strings(p.claimed));
		progress.put("met", strings(met));
		String key = progress.toString();
		if (key.equals(LAST_SENT.get(player.getUUID()))) {
			return;
		}
		LAST_SENT.put(player.getUUID(), key);
		CompoundTag tag = new CompoundTag();
		tag.put("progress", progress);
		send(player, tag);
	}

	private static ListTag strings(Iterable<String> values) {
		ListTag list = new ListTag();
		for (String s : values) {
			list.add(StringTag.valueOf(s));
		}
		return list;
	}

	private static void send(ServerPlayer player, CompoundTag tag) {
		try {
			Platform.INSTANCE.sendToPlayer(player, new QuestNet.Payload(tag));
		} catch (RuntimeException e) {
			// a game test's stand-in player has no real connection
			Wildcatter.LOGGER.debug("Quest book: not sent to {}: {}", player.getPlainTextName(), e.toString());
		}
	}

	/** Static state must not survive into the next world opened in the same session. */
	public static void clearAll() {
		readFrom = null;
		book = QuestBook.EMPTY;
		ONLINE.clear();
		LAST_SENT.clear();
		POKED.clear();
		QuestEvents.clearAll();
	}
}
