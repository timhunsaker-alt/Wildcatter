package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Built From Scratch, episode 1 (the "Getting started" chapter, {@code wildcatter_quests/getting_started.json}): the proof that what the
 * episode films works in normal play. The chapter has no milestones, so these prove its item and advancement tasks and the steel recipe:
 * <ul>
 * <li>{@code bfs01_getting_started}: the real chapter's seven quests, each finished by what a player carries and the vanilla advancements
 * they earn, one at a time in the episode's order; and the Field Manual's recipe (a book and a copper ingot) found by the server;</li>
 * <li>{@code bfs01_steel_blast_furnace}: an iron ingot and coal in a furnace never light it and never make steel (no furnace recipe), while the
 * same in a blast furnace makes a steel ingot every 100 ticks (the recipe asks 200; the blast furnace cooks at twice the rate) (the episode's "what goes wrong").</li>
 * </ul>
 */
final class Bfs01Scenes {
	private Bfs01Scenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs01_getting_started", Bfs01Scenes::gettingStarted);
		out.put("bfs01_steel_blast_furnace", Bfs01Scenes::steelBlastFurnace);
		return out;
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String quest) {
		return QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:" + quest);
	}

	private static void advancement(MinecraftServer server, ServerPlayer player, String id) {
		var holder = server.getAdvancements().get(Identifier.parse(id));
		for (String c : player.getAdvancements().getOrStartProgress(holder).getRemainingCriteria()) {
			player.getAdvancements().award(holder, c);
		}
	}

	static void gettingStarted(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		QuestBook book = QuestTracker.book(server);
		QuestBook.Chapter chapter = book.chapter(Identifier.parse("wildcatter:getting_started"));
		check(helper, chapter != null, "the Getting started chapter should load");
		List<String> ids = book.quests().values().stream().filter(q -> q.chapter().equals(chapter.id())).map(q -> q.id().getPath()).toList();
		check(helper, ids.size() == 7 && ids.containsAll(List.of("wood", "stone", "campfire", "iron", "copper", "steel", "field_manual")),
				"the chapter should hold the episode's seven quests: " + ids);
		ServerPlayer player = QuestScenes.player(helper, 1, 2, 1);
		// one quest at a time, as the episode films them; each must not finish before its own step
		Object[][] steps = {
				{"wood", (Runnable) () -> player.getInventory().add(new ItemStack(Items.OAK_LOG, 4))},
				{"stone", (Runnable) () -> {
					advancement(server, player, "minecraft:story/mine_stone");
					advancement(server, player, "minecraft:story/upgrade_tools");
				}},
				{"campfire", (Runnable) () -> player.getInventory().add(new ItemStack(Items.CAMPFIRE))},
				{"iron", (Runnable) () -> {
					advancement(server, player, "minecraft:story/smelt_iron");
					player.getInventory().add(new ItemStack(Items.IRON_INGOT, 8));
				}},
				{"copper", (Runnable) () -> player.getInventory().add(new ItemStack(Items.COPPER_INGOT, 16))},
				{"steel", (Runnable) () -> player.getInventory().add(new ItemStack(WildcatterItems.STEEL_INGOT, 8))},
				{"field_manual", (Runnable) () -> player.getInventory().add(new ItemStack(WildcatterItems.FIELD_MANUAL))}};
		for (int i = 0; i < steps.length; i++) {
			String quest = (String) steps[i][0];
			QuestTracker.check(player);
			check(helper, !done(server, player, quest), quest + " should not be done before its step");
			((Runnable) steps[i][1]).run();
			QuestTracker.check(player);
			check(helper, done(server, player, quest), quest + " should be done by its step");
		}
		// the Field Manual's recipe, as the crafting grid asks the server for it
		var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.COPPER_INGOT)));
		var recipe = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
		check(helper, recipe.isPresent() && recipe.get().value().assemble(input).is(WildcatterItems.FIELD_MANUAL),
				"a book and a copper ingot should craft the Field Manual: " + recipe);
		QuestScenes.leave(helper, player);
		log("bfs01_getting_started", "ok: " + ids);
		helper.succeed();
	}

	static void steelBlastFurnace(GameTestHelper helper) {
		BlockPos furnace = new BlockPos(0, 1, 0), blast = new BlockPos(2, 1, 0);
		helper.setBlock(furnace, Blocks.FURNACE);
		helper.setBlock(blast, Blocks.BLAST_FURNACE);
		for (BlockPos at : new BlockPos[] {furnace, blast}) {
			AbstractFurnaceBlockEntity f = helper.getBlockEntity(at, AbstractFurnaceBlockEntity.class);
			f.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
			f.setItem(1, new ItemStack(Items.COAL, 1));
		}
		boolean[] litSeen = {false};
		int[] tick = {0}, first = {-1};
		helper.onEachTick(() -> {
			tick[0]++;
			litSeen[0] |= helper.getBlockState(furnace).getValue(BlockStateProperties.LIT);
			if (first[0] < 0 && !helper.getBlockEntity(blast, AbstractFurnaceBlockEntity.class).getItem(2).isEmpty()) {
				first[0] = tick[0];
			}
		});
		helper.runAfterDelay(215, () -> {
			AbstractFurnaceBlockEntity f = helper.getBlockEntity(furnace, AbstractFurnaceBlockEntity.class);
			AbstractFurnaceBlockEntity b = helper.getBlockEntity(blast, AbstractFurnaceBlockEntity.class);
			check(helper, !litSeen[0] && f.getItem(2).isEmpty() && f.getItem(0).getCount() == 2 && f.getItem(1).getCount() == 1,
					"the furnace should never light for an iron ingot and make nothing: lit " + litSeen[0] + ", out " + f.getItem(2) + ", in " + f.getItem(0) + ", fuel " + f.getItem(1));
			log("bfs01_steel_blast_furnace", "first steel at tick " + first[0] + ", out " + b.getItem(2));
			check(helper, b.getItem(2).is(WildcatterItems.STEEL_INGOT) && b.getItem(2).getCount() == 2 && first[0] >= 95 && first[0] <= 105,
					"the blast furnace should make a steel ingot from an iron ingot every 100 ticks (the recipe's 200 at the blast furnace's double speed): first at "
							+ first[0] + ", out " + b.getItem(2) + ", in " + b.getItem(0));
			log("bfs01_steel_blast_furnace", "ok: furnace never lit, blast furnace " + b.getItem(2) + ", the first at tick " + first[0]);
			helper.succeed();
		});
	}
}
