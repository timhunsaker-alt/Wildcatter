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
		out.put("dry_type_runs_rig", QuestScenes::dryTypeRunsRig);
		out.put("quest_pumpjack_milestone", QuestScenes::pumpjackMilestone);
		out.put("quest_well_struck_milestone", QuestScenes::wellStruckMilestone);
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

	// ---- Tim's answers of 2026-11-14: the first 480 V from a dry-type step-up, and the oilfield milestones ----------------------------

	/** The height of these scenes' floor in the world: high enough that a rig has rock to drill and stops at a field, not the dry-hole floor. */
	private static final int OIL_FLOOR_Y = 40;

	/** The relative y of the floor's top layer + 1: where the machines stand. */
	private static int oilY(GameTestHelper helper) {
		return OIL_FLOOR_Y - helper.absolutePos(BlockPos.ZERO).getY() + 1;
	}

	/** A stone floor at relative height y over (x1, z1)..(x2, z2), its chunks force-loaded. */
	private static void stone(GameTestHelper helper, int x1, int z1, int x2, int z2, int y) {
		BlockPos a = helper.absolutePos(new BlockPos(x1, y, z1));
		BlockPos b = helper.absolutePos(new BlockPos(x2, y, z2));
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				helper.getLevel().setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	/** Everything between the corners back to air, so no rig is left drilling into the next batch. */
	private static void clearBox(GameTestHelper helper, int x1, int z1, int x2, int z2, int y1, int y2) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				for (int y = y1; y <= y2; y++) {
					at.set(helper.absolutePos(new BlockPos(x, y, z)));
					if (!helper.getLevel().getBlockState(at).isAir()) {
						helper.getLevel().removeBlockEntity(at);
						helper.getLevel().setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
					}
				}
			}
		}
	}

	/** A seed whose fields put nothing under any of these absolute columns (no dome grid: the scenes' world has its rules off). */
	private static long noFieldSeed(java.util.List<BlockPos> holes) {
		for (long s = 1; s < 100_000; s++) {
			final long seed = s;
			if (holes.stream().allMatch(h -> dev.wildcatter.well.Reservoir.at(seed, h.getX(), h.getZ(), d -> false) == null)) {
				return seed;
			}
		}
		return -1;
	}

	/** A seed with a field the small rig can reach under this absolute column (oil only if {@code oil}), or -1. */
	private static long fieldSeed(BlockPos hole, boolean oil) {
		for (long s = 1; s < 3_000_000; s++) {
			dev.wildcatter.well.Reservoir f = dev.wildcatter.well.Reservoir.at(s, hole.getX(), hole.getZ(), d -> false);
			if (f != null && f.units() > 0 && (!oil || f.kind() == dev.wildcatter.well.Reservoir.Kind.OIL) && f.topY() < hole.getY() - 4
					&& hole.getY() - f.topY() < dev.wildcatter.Tuning.SMALL_RIG_MAX_DEPTH - 2) {
				return s;
			}
		}
		return -1;
	}

	/**
	 * One steam plant feeding a dry-type into a rig, along x from {@code x0} at row z: {@code n} steam turbines in a row, touching, each with
	 * a boiler north of it on a firebox (coal in) or, for {@code campfire}, on a lit campfire; water in. Then the dry-type with its LOW side
	 * against the last turbine and its 480 V front facing east against the rig's terminal box, and the rig facing east. Returns the rig's
	 * main cell (relative). The dry-type goes in only after {@code energiseAt} ticks, when the boilers have built their steam: the switch
	 * closing.
	 */
	private static BlockPos dryTypePlant(GameTestHelper helper, int x0, int z, int y, int n, boolean campfire, int energiseAt) {
		int xt = x0 + n;            // the dry-type
		int hx = xt + 6;            // the rig's main cell: its terminal box 5 west of it, at xt + 1
		stone(helper, x0 - 1, z - 2, hx + 2, z + 2, y - 1);
		for (int i = 0; i < n; i++) {
			int x = x0 + i;
			helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
			if (campfire) {
				helper.setBlock(new BlockPos(x, y, z - 1), net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
			} else {
				helper.setBlock(new BlockPos(x, y, z - 1), dev.wildcatter.WildcatterBlocks.FIREBOX);
				GridSceneKit.entity(helper, x, y, z - 1, dev.wildcatter.block.FireboxBlockEntity.class).setItem(0, new ItemStack(Items.COAL_BLOCK, 64));
			}
			helper.setBlock(new BlockPos(x, y + 1, z - 1), dev.wildcatter.WildcatterBlocks.BOILER);
			GridSceneKit.entity(helper, x, y + 1, z - 1, dev.wildcatter.block.BoilerBlockEntity.class).addWater(4000);
			helper.setBlock(new BlockPos(x, y + 1, z), dev.wildcatter.WildcatterBlocks.STEAM_TURBINE);
		}
		helper.setBlock(new BlockPos(xt, y, z), net.minecraft.world.level.block.Blocks.STONE);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.DRILLING_RIG, hx, y + 1, z, net.minecraft.core.Direction.EAST);
		helper.runAfterDelay(energiseAt, () -> helper.setBlock(new BlockPos(xt, y + 1, z), dev.wildcatter.WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState()
				.setValue(dev.wildcatter.block.TransformerBlock.FACING, net.minecraft.core.Direction.EAST)));
		return new BlockPos(hx, y + 1, z);
	}

	/** How many of the plant's turbines have tripped on frequency. */
	private static int trippedTurbines(GameTestHelper helper, int x0, int z, int y, int n) {
		int tripped = 0;
		for (int i = 0; i < n; i++) {
			if (GridSceneKit.entity(helper, x0 + i, y + 1, z, dev.wildcatter.block.SteamTurbineBlockEntity.class).isTripped()) {
				tripped++;
			}
		}
		return tripped;
	}

	/** One plant of the scene: its turbines, and whether their boilers stand on campfires rather than fireboxes. */
	record DryTypePlant(int turbines, boolean campfire) {
		String name() {
			return turbines + (campfire ? " campfire" : " firebox") + " set" + (turbines == 1 ? "" : "s");
		}
	}

	/** The plants side by side; DRY_TYPE_SETS=1f,2c,... overrides them while the counts are being found. */
	static final java.util.List<DryTypePlant> DRY_TYPE_PLANTS = plants();

	private static java.util.List<DryTypePlant> plants() {
		String env = System.getenv("DRY_TYPE_SETS");
		if (env != null && !env.isBlank()) {
			return java.util.Arrays.stream(env.split(",")).map(t -> new DryTypePlant(Integer.parseInt(t.substring(0, t.length() - 1)), t.endsWith("c"))).toList();
		}
		return java.util.List.of(new DryTypePlant(DRY_TYPE_FIRED, false), new DryTypePlant(DRY_TYPE_FIRED, true));
	}

	/** The fewest firebox sets that start the rig through the dry-type (docs/dry-type-transformer.md); the same number on campfires cannot. */
	static final int DRY_TYPE_FIRED = 1;

	/**
	 * Small steam sets feed the dry-type on its LOW side; its 480 V side feeds the rig. A turbine on a firebox boiler is rated by the fire
	 * (Tim, 2026-11-14): one firebox set starts the rig despite its 6 x inrush and keeps it drilling, the dry-type's high side live at the
	 * industrial tier and transformer_energised fired. The same set on a campfire boiler (2 kW) trips on frequency during the start.
	 */
	static void dryTypeRunsRig(GameTestHelper helper) {
		String scene = "dry_type_runs_rig";
		MinecraftServer server = helper.getLevel().getServer();
		int y = oilY(helper);
		java.util.List<DryTypePlant> plants = DRY_TYPE_PLANTS;
		int count = plants.size();
		int[] rows = new int[count];
		BlockPos[] rigs = new BlockPos[count];
		java.util.List<BlockPos> holes = new java.util.ArrayList<>();
		for (int k = 0; k < count; k++) {
			rows[k] = k * 8;
			holes.add(helper.absolutePos(new BlockPos(plants.get(k).turbines() + 6, y + 1, rows[k])));
		}
		long seed = noFieldSeed(holes);
		check(helper, seed >= 0, "no seed leaves the rigs' columns without a field");
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		int maxX = plants.stream().mapToInt(DryTypePlant::turbines).max().orElse(0) + 10;
		dev.wildcatter.well.ReservoirData.TestRegion region = new dev.wildcatter.well.ReservoirData.TestRegion(o.getX() - 4, o.getZ() - 4, o.getX() + maxX, o.getZ() + rows[count - 1] + 4, seed);
		dev.wildcatter.well.ReservoirData.TEST_REGIONS.add(region);
		for (int k = 0; k < count; k++) {
			rigs[k] = dryTypePlant(helper, 0, rows[k], y, plants.get(k).turbines(), plants.get(k).campfire(), 220);
		}
		ServerPlayer player = player(helper, 4, y + 1, rows[count - 1] + 3);
		int[] firstDrilling = new int[count];
		java.util.Arrays.fill(firstDrilling, -1);
		int[] firstTrip = new int[count];
		java.util.Arrays.fill(firstTrip, -1);
		for (int t = 240; t <= 1000; t += 20) {
			final int at = t;
			helper.runAfterDelay(t, () -> {
				for (int k = 0; k < count; k++) {
					int n = plants.get(k).turbines();
					String rig = GridSceneKit.read(helper, rigs[k].getX(), rigs[k].getY(), rigs[k].getZ());
					int tripped = trippedTurbines(helper, 0, rows[k], y, n);
					if (firstDrilling[k] < 0 && rig.contains("drilling,")) {
						firstDrilling[k] = at;
					}
					if (firstTrip[k] < 0 && tripped > 0) {
						firstTrip[k] = at;
					}
					if (at % 100 == 0 || at == 240) {
						log(scene, plants.get(k).name() + " at " + at + " | tripped " + tripped + " | rig " + rig + " | dry-type " + GridSceneKit.read(helper, n, y + 1, rows[k])
								+ " | turbine " + GridSceneKit.read(helper, n - 1, y + 1, rows[k]) + " | boiler " + GridSceneKit.read(helper, n - 1, y + 1, rows[k] - 1));
					}
				}
				if (at == 1000) {
					StringBuilder sum = new StringBuilder();
					for (int k = 0; k < count; k++) {
						sum.append(plants.get(k).name()).append(": drilling from ").append(firstDrilling[k]).append(", tripped from ").append(firstTrip[k]).append("; ");
					}
					log(scene, "summary | " + sum);
					if (System.getenv("DRY_TYPE_SETS") == null || System.getenv("DRY_TYPE_SETS").isBlank()) {
						int f = 0, c = 1;
						String rig = GridSceneKit.read(helper, rigs[f].getX(), rigs[f].getY(), rigs[f].getZ());
						String xfmr = GridSceneKit.read(helper, DRY_TYPE_FIRED, y + 1, rows[f]);
						check(helper, firstTrip[f] < 0 && rig.contains("drilling,") && !rig.contains(" 0 blocks down"), plants.get(f).name() + " should start the rig and keep it drilling: " + rig);
						java.util.regex.Matcher hv = java.util.regex.Pattern.compile("([\\d.]+) V [\\d.]+ A on the " + java.util.regex.Pattern.quote(dev.wildcatter.energy.Voltage.INDUSTRIAL.label())).matcher(xfmr);
						check(helper, hv.find() && Double.parseDouble(hv.group(1)) > dev.wildcatter.energy.Voltage.INDUSTRIAL.nominal() * 0.5,
								"the dry-type's high side should be live at the industrial tier, fed from its low side: " + xfmr);
						String camp = GridSceneKit.read(helper, rigs[c].getX(), rigs[c].getY(), rigs[c].getZ());
						check(helper, firstTrip[c] > 0 && firstDrilling[c] < 0, plants.get(c).name() + " (2 kW each) should trip on frequency during the rig's start, never drilling: " + camp);
						check(helper, reached(server, player, dev.wildcatter.quest.Milestones.TRANSFORMER_ENERGISED), "the dry-type should fire transformer_energised");
						check(helper, reached(server, player, dev.wildcatter.quest.Milestones.MOTOR_RUNNING), "the rig's motor should fire motor_running");
						log(scene, "ok: " + sum + "| rig " + rig + " | dry-type " + xfmr);
					}
					dev.wildcatter.well.ReservoirData.TEST_REGIONS.remove(region);
					leave(helper, player);
					clearBox(helper, -2, -3, maxX + 2, rows[count - 1] + 3, y - 1, y + 12);
					helper.succeed();
				}
			});
		}
	}

	/** A diesel set at x0 feeds 480 V cable along row z to {@code boxX - 1}: the terminal box of a rig or pumpjack facing east sits at boxX. */
	private static void setAndCable(GameTestHelper helper, int x0, int y, int z, int boxX) {
		GridSceneKit.genset(helper, x0, y, z);   // its terminal at (x0 + 1, y, z)
		for (int x = x0 + 1; x < boxX; x++) {
			GridSceneKit.put(helper, "cable_industrial", x, y, z);
		}
	}

	/**
	 * How far to move a scene along one axis so that the absolute coordinate {@code at} lands in the middle third of a 64-block field
	 * cell: a shallow field never reaches a cell's edge, so a hole there has no seed with a field under it.
	 */
	private static int aim(int at) {
		int cell = dev.wildcatter.Tuning.FIELD_CELL;
		return Math.floorMod(-at, cell) - cell / 2;   // at + this is half a cell past a cell's edge
	}

	/** A pumpjack over an oil wellhead on a field, on a diesel set: its motor runs, it lifts, and it fires pumpjack_running. */
	static void pumpjackMilestone(GameTestHelper helper) {
		String scene = "quest_pumpjack_milestone";
		MinecraftServer server = helper.getLevel().getServer();
		int y = oilY(helper);
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		int dx = aim(o.getX() + 14), z = aim(o.getZ());
		int wx = dx + 14;                                  // the wellhead; the pumpjack's main cell west of it, its box 5 further west
		BlockPos well = helper.absolutePos(new BlockPos(wx, y, z));
		long seed = fieldSeed(well, true);
		check(helper, seed >= 0, "no seed puts an oil field under the well");
		dev.wildcatter.well.ReservoirData.TestRegion region = new dev.wildcatter.well.ReservoirData.TestRegion(o.getX() + dx - 12, o.getZ() + z - 8, o.getX() + dx + 24, o.getZ() + z + 8, seed);
		dev.wildcatter.well.ReservoirData.TEST_REGIONS.add(region);
		stone(helper, dx - 10, z - 4, dx + 18, z + 4, y - 1);
		GridSceneKit.put(helper, "oil_wellhead", wx, y, z);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.PUMPJACK, wx - 1, y, z, net.minecraft.core.Direction.EAST);
		setAndCable(helper, dx, y, z, wx - 6);
		ServerPlayer player = player(helper, dx + 4, y, z + 3);
		GridSceneKit.start(helper, dx, y, z, () -> helper.runAfterDelay(200, () -> {
			String jack = GridSceneKit.read(helper, wx - 1, y, z);
			log(scene, "pumpjack | " + jack);
			check(helper, jack.contains("pumping at"), "the pumpjack should run and lift: " + jack);
			check(helper, reached(server, player, dev.wildcatter.quest.Milestones.PUMPJACK_RUNNING), "the running pumpjack should fire pumpjack_running: " + jack);
			QuestTracker.check(player);
			check(helper, QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:oil_well"), "the oil well quest (pumpjack_running) should be done");
			dev.wildcatter.well.ReservoirData.TEST_REGIONS.remove(region);
			leave(helper, player);
			log(scene, "ok: " + jack);
			clearBox(helper, dx - 10, z - 4, dx + 18, z + 4, y - 1, y + 8);
			helper.succeed();
		}));
	}

	/** A small rig two blocks above a field, on a diesel set: it strikes, rigs down, and fires well_struck; the gas well quest's strike task follows. */
	static void wellStruckMilestone(GameTestHelper helper) {
		String scene = "quest_well_struck_milestone";
		MinecraftServer server = helper.getLevel().getServer();
		int y = oilY(helper);
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		int dx = aim(o.getX() + 12), z = aim(o.getZ());
		int hx = dx + 12;                                  // the rig's main cell; its box at hx - 5
		BlockPos hole = helper.absolutePos(new BlockPos(hx, y, z));
		long seed = fieldSeed(hole, false);
		check(helper, seed >= 0, "no seed puts a field under the rig");
		dev.wildcatter.well.ReservoirData.TestRegion region = new dev.wildcatter.well.ReservoirData.TestRegion(o.getX() + dx - 12, o.getZ() + z - 8, o.getX() + dx + 24, o.getZ() + z + 8, seed);
		dev.wildcatter.well.ReservoirData.TEST_REGIONS.add(region);
		dev.wildcatter.well.Reservoir field = dev.wildcatter.well.ReservoirData.field(helper.getLevel(), hole.getX(), hole.getZ());
		check(helper, field != null, "the region's seed should put the field under the hole");
		stone(helper, dx - 10, z - 4, dx + 18, z + 4, y - 1);
		GridSceneKit.place(helper, dev.wildcatter.WildcatterBlocks.DRILLING_RIG, hx, y, z, net.minecraft.core.Direction.EAST);
		GridSceneKit.entity(helper, hx, y, z, dev.wildcatter.block.DrillingRigBlockEntity.class).testDepth(hole.getY() - field.topY() - 2);
		setAndCable(helper, dx, y, z, hx - 5);
		ServerPlayer player = player(helper, dx + 4, y, z + 3);
		check(helper, !reached(server, player, dev.wildcatter.quest.Milestones.WELL_STRUCK), "no strike yet");
		GridSceneKit.start(helper, dx, y, z, () -> {});
		boolean[] done = {false};
		for (int t = 320; t <= 1100; t += 20) {
			helper.runAfterDelay(t, () -> {
				if (done[0]) {
					return;
				}
				boolean oil = helper.getLevel().getBlockState(hole).is(dev.wildcatter.WildcatterBlocks.OIL_WELLHEAD);
				boolean gas = helper.getLevel().getBlockState(hole).is(dev.wildcatter.WildcatterBlocks.GAS_WELLHEAD);
				if (!oil && !gas) {
					if (helper.getTick() % 200 == 0) {
						log(scene, "waiting at " + helper.getTick() + " | " + GridSceneKit.read(helper, hx, y, z) + " | field " + field + " | set " + GridSceneKit.read(helper, dx, y, z));
					}
					return;
				}
				done[0] = true;
				check(helper, reached(server, player, dev.wildcatter.quest.Milestones.WELL_STRUCK), "the strike should fire well_struck");
				player.getInventory().add(new ItemStack(dev.wildcatter.WildcatterBlocks.GAS_LINE, 16));
				QuestTracker.check(player);
				check(helper, QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:gas_well"), "the strike and 16 steel pipe should finish the gas well quest");
				dev.wildcatter.well.ReservoirData.TEST_REGIONS.remove(region);
				leave(helper, player);
				log(scene, "ok: struck " + (oil ? "oil" : "gas") + " " + (hole.getY() - field.topY()) + " blocks down");
				clearBox(helper, dx - 10, z - 4, dx + 18, z + 4, y - 1, y + 12);
				helper.succeed();
			});
		}
	}
}
