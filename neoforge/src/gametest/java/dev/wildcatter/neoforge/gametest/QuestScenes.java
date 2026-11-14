package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestEvents;
import dev.wildcatter.quest.QuestRules;
import dev.wildcatter.quest.QuestTracker;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The quest book's engine (docs/quests.md), on the server, against the test chapters in this test mod's data
 * ({@code data/wildcatter_gametest/wildcatter_quests/}), which also shows a chapter coming from another namespace:
 * <ul>
 * <li>{@code quest_data_loads}: the chapters load from every namespace, sorted by order, with bare ids in the file's namespace;</li>
 * <li>{@code quest_item_task}: an item task is met only at its count, and the quest stays done when the items go;</li>
 * <li>{@code quest_milestone}: a milestone reaches players within 64 blocks and not beyond, and a quest with no tasks follows;</li>
 * <li>{@code quest_reward_claim}: rewards are given once, only for a done quest;</li>
 * <li>{@code quest_book_given_once}: the tracker gives a joining player the book, and never a second;</li>
 * <li>{@code quests_valid}: every chapter the server has passes the check, and the check catches each kind of fault.</li>
 * </ul>
 */
final class QuestScenes {
	private QuestScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("quest_data_loads", QuestScenes::dataLoads);
		out.put("quest_item_task", QuestScenes::itemTask);
		out.put("quest_milestone", QuestScenes::milestone);
		out.put("quest_reward_claim", QuestScenes::rewardClaim);
		out.put("quest_book_given_once", QuestScenes::bookGivenOnce);
		out.put("quests_valid", QuestScenes::questsValid);
		return out;
	}

	/** Phase 21.2: the milestones Wildcatter's machines fire, each kind in a scene of its own, with room round it. */
	static Map<String, Consumer<GameTestHelper>> machineScenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("quest_turbine_milestone", QuestScenes::turbineMilestone);
		out.put("quest_genset_milestone", QuestScenes::gensetMilestone);
		out.put("quest_multiblock_milestone", QuestScenes::multiblockMilestone);
		out.put("quest_reactor_milestone", QuestScenes::reactorMilestone);
		return out;
	}

	static Identifier tid(String path) {
		return Identifier.fromNamespaceAndPath("wildcatter_gametest", path);
	}

	/** A stand-in player at the test's origin, with an empty inventory. */
	static ServerPlayer player(GameTestHelper helper, int x, int y, int z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(x, y, z));
		player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.getInventory().clearContent();
		return player;
	}

	static void leave(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	static boolean done(MinecraftServer server, ServerPlayer player, String quest) {
		return QuestData.get(server).of(player.getStringUUID()).done.contains(tid(quest).toString());
	}

	static int count(ServerPlayer player, Item item) {
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() == item) {
				n += s.getCount();
			}
		}
		return n;
	}

	static void dataLoads(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		QuestBook book = QuestTracker.book(server);
		check(helper, book.chapter(tid("test_chapter")) != null && book.chapter(tid("test_chapter_zero")) != null,
				"the test mod's two chapters should load from its own namespace: " + book.chapters().stream().map(c -> c.id().toString()).toList());
		List<Identifier> order = book.chapters().stream().map(QuestBook.Chapter::id).toList();
		check(helper, order.indexOf(tid("test_chapter_zero")) < order.indexOf(tid("test_chapter")), "chapters should sort by order: " + order);
		QuestBook.Quest machine = book.quest(tid("machine"));
		check(helper, machine != null && machine.requires().equals(List.of(tid("cobble"))), "a bare id in requires should take the file's namespace: " + machine);
		check(helper, book.quest(tid("after_machine")).requires().equals(List.of(tid("machine"))), "a full id in requires should be kept");
		QuestBook.Quest logs = book.quest(tid("logs"));
		check(helper, logs.tasks().get(0).isTag() && logs.tasks().get(0).count() == 2 && logs.tasks().get(1).kind() == QuestBook.Kind.ADVANCEMENT,
				"the logs quest should have a tag task and an advancement task: " + logs.tasks());
		check(helper, book.chapter(tid("test_chapter")).after().equals(List.of(tid("test_chapter_zero"))), "the chapter's after should load");
		// a file read the way the client reads the payload: the same parser, from text
		QuestBook client = QuestBook.of(book.sources());
		check(helper, client.quests().keySet().equals(book.quests().keySet()), "the client's copy, read from the sent text, should have the same quests");
		log("quest_data_loads", "ok: " + book.chapters().size() + " chapters, " + book.quests().size() + " quests; order " + order);
		helper.succeed();
	}

	static void itemTask(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = player(helper, 1, 2, 1);
		player.getInventory().add(new ItemStack(Items.COBBLESTONE, 3));
		QuestTracker.check(player);
		check(helper, !done(server, player, "cobble"), "three cobblestone should not meet a task for four");
		player.getInventory().add(new ItemStack(Items.COBBLESTONE, 1));
		List<QuestBook.Quest> now = QuestTracker.check(player);
		check(helper, done(server, player, "cobble") && now.stream().anyMatch(q -> q.id().equals(tid("cobble"))), "four cobblestone should finish the quest: " + now);
		player.getInventory().clearContent();
		QuestTracker.check(player);
		check(helper, done(server, player, "cobble"), "the quest should stay done when the items go");
		// a tag: two logs of different kinds count together; the advancement is still missing, so not done
		player.getInventory().add(new ItemStack(Items.OAK_LOG, 1));
		player.getInventory().add(new ItemStack(Items.BIRCH_LOG, 1));
		QuestTracker.check(player);
		check(helper, QuestTracker.count(player.getInventory(), "#minecraft:logs") == 2, "two logs should count for #minecraft:logs");
		check(helper, !done(server, player, "logs"), "the logs quest also needs its advancement");
		leave(helper, player);
		log("quest_item_task", "ok");
		helper.succeed();
	}

	static void milestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer near = player(helper, 1, 2, 1);
		ServerPlayer far = player(helper, 1, 2, 1);
		BlockPos machine = helper.absolutePos(new BlockPos(2, 1, 2));
		far.teleportTo(machine.getX() + 80.5, machine.getY(), machine.getZ() + 0.5);
		Identifier key = tid("test_machine");
		QuestEvents.milestone(helper.getLevel(), machine, key);
		QuestData data = QuestData.get(server);
		check(helper, data.of(near.getStringUUID()).milestones.contains(key.toString()), "a player 2 blocks from the machine should reach its milestone");
		check(helper, !data.of(far.getStringUUID()).milestones.contains(key.toString()), "a player 80 blocks off should not");
		// the same machine again within 5 s is not counted (the far player walks up, but the call is rate-limited)
		far.teleportTo(machine.getX() + 3.5, machine.getY(), machine.getZ() + 0.5);
		QuestEvents.milestone(helper.getLevel(), machine, key);
		check(helper, !data.of(far.getStringUUID()).milestones.contains(key.toString()), "the same position and key within 5 s should not count again");
		QuestTracker.check(near);
		check(helper, done(server, near, "machine"), "the milestone should finish its quest, prerequisites or not");
		check(helper, done(server, near, "after_machine"), "a quest with no tasks should follow the quest it needs in the same check");
		leave(helper, near);
		leave(helper, far);
		log("quest_milestone", "ok");
		helper.succeed();
	}

	static void rewardClaim(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = player(helper, 1, 2, 1);
		check(helper, !QuestTracker.claim(player, tid("cobble")), "a quest not done should give nothing");
		player.getInventory().add(new ItemStack(Items.COBBLESTONE, 4));
		QuestTracker.check(player);
		player.getInventory().clearContent();
		check(helper, QuestTracker.claim(player, tid("cobble")), "a done quest's rewards should be given");
		check(helper, count(player, Items.BREAD) == 2 && count(player, WildcatterItems.QUEST_BOOK) == 1, "two bread and a book should be in the inventory");
		check(helper, !QuestTracker.claim(player, tid("cobble")), "a second claim should give nothing");
		check(helper, count(player, Items.BREAD) == 2, "still two bread");
		check(helper, QuestData.get(server).of(player.getStringUUID()).claimed.contains(tid("cobble").toString()), "the claim should be kept");
		check(helper, !QuestTracker.claim(player, tid("after_machine")), "a quest with no rewards gives nothing");
		leave(helper, player);
		log("quest_reward_claim", "ok");
		helper.succeed();
	}

	static void bookGivenOnce(GameTestHelper helper) {
		ServerPlayer player = player(helper, 1, 2, 1);
		player.removeTag(QuestTracker.GIVEN_TAG);
		helper.runAfterDelay(QuestTracker.PERIOD + 5, () -> {
			// the tracker saw the player join and gave the book (the inventory was cleared after the join, so give again if it came first)
			boolean tagged = player.entityTags().contains(QuestTracker.GIVEN_TAG);
			if (!tagged) {
				check(helper, QuestTracker.giveOnce(player), "the first give should give the book");
			}
			check(helper, player.entityTags().contains(QuestTracker.GIVEN_TAG), "the player should carry the given tag");
			int books = count(player, WildcatterItems.QUEST_BOOK);
			check(helper, !QuestTracker.giveOnce(player), "a second give should give nothing");
			check(helper, count(player, WildcatterItems.QUEST_BOOK) == books, "no second book");
			// a new player, never given one: the first give gives exactly one
			ServerPlayer fresh = player(helper, 1, 2, 1);
			fresh.removeTag(QuestTracker.GIVEN_TAG);
			check(helper, QuestTracker.giveOnce(fresh) && count(fresh, WildcatterItems.QUEST_BOOK) == 1 && !QuestTracker.giveOnce(fresh)
					&& count(fresh, WildcatterItems.QUEST_BOOK) == 1, "a new player should get exactly one book");
			leave(helper, player);
			leave(helper, fresh);
			log("quest_book_given_once", "ok: given at join " + tagged);
			helper.succeed();
		});
	}

	static boolean reached(MinecraftServer server, ServerPlayer player, Identifier key) {
		return QuestData.get(server).of(player.getStringUUID()).milestones.contains(key.toString());
	}

	static Identifier wid(String path) {
		return dev.wildcatter.Wildcatter.id(path);
	}

	/** The starter plant lights a lamp: the turbine's and the lamp's milestones, and the First power chapter's steam quest done. */
	static void turbineMilestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		for (int x = -1; x <= 6; x++) {
			for (int z = -1; z <= 2; z++) {
				helper.setBlock(x, 0, z, net.minecraft.world.level.block.Blocks.STONE);
			}
		}
		helper.setBlock(0, 1, 0, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
		helper.setBlock(0, 2, 0, dev.wildcatter.WildcatterBlocks.BOILER);
		helper.setBlock(1, 2, 0, dev.wildcatter.WildcatterBlocks.STEAM_TURBINE);
		helper.getBlockEntity(new BlockPos(0, 2, 0), dev.wildcatter.block.BoilerBlockEntity.class).addWater(4000);
		for (int x = 2; x <= 4; x++) {
			GridSceneKit.put(helper, "wool_wire_hot", x, 2, 0);
		}
		helper.setBlock(5, 2, 0, dev.wildcatter.WildcatterBlocks.ELECTRIC_LAMP);
		for (int x = 1; x <= 5; x++) {
			GridSceneKit.put(helper, "wool_wire_neutral", x, 2, 1);
		}
		ServerPlayer player = player(helper, 3, 1, 3);
		player.getInventory().add(new ItemStack(dev.wildcatter.WildcatterBlocks.BOILER));
		player.getInventory().add(new ItemStack(dev.wildcatter.WildcatterBlocks.STEAM_TURBINE));
		helper.succeedWhen(() -> {
			check(helper, reached(server, player, dev.wildcatter.quest.Milestones.STEAM_TURBINE_GENERATING), "the turbine should fire steam_turbine_generating");
			check(helper, reached(server, player, dev.wildcatter.quest.Milestones.LAMP_LIT), "the lamp should fire lamp_lit");
			QuestTracker.check(player);
			check(helper, QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:steam"), "the steam quest (boiler, turbine, generating) should be done");
			leave(helper, player);
			log("quest_turbine_milestone", "ok");
		});
	}

	/** A diesel set started: it fires genset_running, diesel_genset_running and its skid's formed milestone. */
	static void gensetMilestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		GridSceneKit.floor(helper, -8, -4, 4, 4);
		GridSceneKit.genset(helper, 0, 1, 0);
		ServerPlayer player = player(helper, 2, 1, 2);
		GridSceneKit.start(helper, 0, 1, 0, () -> helper.succeedWhen(() -> {
			check(helper, reached(server, player, dev.wildcatter.quest.Milestones.GENSET_RUNNING), "the running set should fire genset_running");
			check(helper, reached(server, player, dev.wildcatter.quest.Milestones.DIESEL_GENSET_RUNNING), "a diesel set should fire diesel_genset_running");
			check(helper, !reached(server, player, dev.wildcatter.quest.Milestones.GAS_GENSET_RUNNING), "a diesel set is not a gas set");
			check(helper, reached(server, player, wid("formed/genset_control_panel")), "its formed skid should fire formed/genset_control_panel");
			leave(helper, player);
			log("quest_genset_milestone", "ok");
		}));
	}

	/** A diesel set's skid built and not started: the multiblock listener fires formed/genset_control_panel, and nothing runs. */
	static void multiblockMilestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		GridSceneKit.floor(helper, -8, -4, 4, 4);
		GridSceneKit.genset(helper, 0, 1, 0);
		ServerPlayer player = player(helper, 2, 1, 2);
		ServerPlayer far = player(helper, 2, 1, 2);
		BlockPos panel = helper.absolutePos(new BlockPos(0, 1, 0));
		far.teleportTo(panel.getX() + 100.5, panel.getY(), panel.getZ() + 0.5);
		helper.succeedWhen(() -> {
			check(helper, GridSceneKit.gensetPanel(helper, 0, 1, 0).formed(), "the skid should form: " + GridSceneKit.read(helper, 0, 1, 0));
			check(helper, reached(server, player, wid("formed/genset_control_panel")), "the formed skid should fire formed/genset_control_panel");
			check(helper, !reached(server, far, wid("formed/genset_control_panel")), "a player 100 blocks off should not reach it");
			check(helper, !reached(server, player, dev.wildcatter.quest.Milestones.GENSET_RUNNING), "a set never started should not fire genset_running");
			leave(helper, player);
			leave(helper, far);
			log("quest_multiblock_milestone", "ok");
		});
	}

	/** The nuclear island built round a vessel at hot standby, its power held: formed/reactor_vessel and reactor_critical. */
	static void reactorMilestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		int u = 1;
		net.minecraft.core.Direction north = net.minecraft.core.Direction.NORTH;
		GridSceneKit.floor(helper, -6, -8, 6, 8);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.REACTOR_VESSEL, 0, u, 0, north);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.CRDM_HEAD, 0, u + 10, 0, north);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.STEAM_GENERATOR, 0, u, -3, north);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.REACTOR_COOLANT_PUMP, 0, u, -5, north);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.PRESSURIZER, -2, u, -3, north);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.CVCS_SKID, -3, u, 0, north);
		dev.wildcatter.block.ReactorVesselBlockEntity vessel = GridSceneKit.entity(helper, 0, u, 0, dev.wildcatter.block.ReactorVesselBlockEntity.class);
		vessel.setHotStandbyForTest(1500);
		vessel.reactor().setSetpoint(-1);   // reset, then a setpoint, as the reactor scenes do
		vessel.reactor().setSetpoint(0.10);
		vessel.reactor().pinPowerForTest(0.02);
		ServerPlayer player = player(helper, 4, 1, 4);
		helper.succeedWhen(() -> {
			check(helper, reached(server, player, wid("formed/reactor_vessel")), "the built island should fire formed/reactor_vessel: " + GridSceneKit.read(helper, 0, u, 0));
			check(helper, reached(server, player, dev.wildcatter.quest.Milestones.REACTOR_CRITICAL), "the critical reactor should fire reactor_critical: " + GridSceneKit.read(helper, 0, u, 0));
			vessel.reactor().pinPowerForTest(-1);
			leave(helper, player);
			log("quest_reactor_milestone", "ok: " + GridSceneKit.read(helper, 0, u, 0));
		});
	}

	static void questsValid(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		List<String> problems = QuestRules.problems(server);
		QuestBook book = QuestTracker.book(server);
		check(helper, problems.isEmpty(), "every chapter should pass the check: " + problems);
		check(helper, !book.isEmpty(), "there should be quests to check");
		// the check catches each kind of fault
		String bad = """
				{"title": "Bad", "icon": "minecraft:stone", "order": 1, "after": ["nowhere:chapter"], "quests": [
				  {"id": "a", "title": "A", "icon": "minecraft:no_such_item", "requires": ["b"], "tasks": [{"item": "minecraft:stone"}],
				   "rewards": [{"item": "wildcatter:plastic_sheet", "count": 4}]},
				  {"id": "b", "title": "B", "icon": "minecraft:stone", "requires": ["a", "missing"], "tasks": [{"advancement": "minecraft:no/such"},
				   {"milestone": "x:y"}, {"item": "#minecraft:no_such_tag"}]}
				]}""";
		QuestBook test = QuestBook.of(List.of(new QuestBook.Source(tid("bad"), bad)));
		List<String> found = QuestRules.problems(server, test);
		String all = String.join(" | ", found);
		for (String expect : new String[] {"not a chapter", "minecraft:no_such_item does not exist", "never-reward", "not a quest", "minecraft:no/such does not exist",
				"has no text", "#minecraft:no_such_tag does not exist", "prerequisite cycle"}) {
			check(helper, all.contains(expect), "the check should catch '" + expect + "': " + all);
		}
		check(helper, QuestRules.neverReward(Identifier.parse("wildcatter:plastic_sheet")) && QuestRules.neverReward(Identifier.parse("wildcatter:yellowcake_drum"))
				&& !QuestRules.neverReward(Identifier.parse("wildcatter:steel_ingot")), "the never-reward list");
		log("quests_valid", "ok: " + book.chapters().size() + " chapters, " + book.quests().size() + " quests pass; the bad chapter: " + found.size() + " problems");
		helper.succeed();
	}
}
