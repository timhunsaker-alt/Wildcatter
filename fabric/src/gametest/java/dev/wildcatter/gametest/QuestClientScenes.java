package dev.wildcatter.gametest;

import dev.wildcatter.WildcatterItems;
import dev.wildcatter.client.quest.QuestScreen;
import dev.wildcatter.quest.QuestClient;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestNet;
import dev.wildcatter.quest.QuestTracker;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * Client screenshots for Phase 21 (docs/quests.md): the quest book given on first join, the quest screen with done, next and later
 * quests, and a reward claimed through the payload.
 */
final class QuestClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;

	QuestClientScenes(ClientGameTestContext context, TestServerContext server) {
		this.context = context;
		this.server = server;
	}

	static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private ServerPlayer player(net.minecraft.server.MinecraftServer srv) {
		return srv.getPlayerList().getPlayers().get(0);
	}

	private int count(net.minecraft.world.item.Item item) {
		return server.computeOnServer(srv -> QuestTracker.count(player(srv).getInventory(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()));
	}

	void questBook() {
		// the book, given on first join
		boolean tagged = server.computeOnServer(srv -> player(srv).entityTags().contains(QuestTracker.GIVEN_TAG));
		check(tagged && count(WildcatterItems.QUEST_BOOK) == 1, "the player should have been given one quest book on joining: tag " + tagged + ", books " + count(WildcatterItems.QUEST_BOOK));
		check(!QuestClient.book().isEmpty(), "the client should have the chapters: " + QuestClient.book().chapters().size());
		context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-quest-book-given");
		context.setScreen(() -> null);

		// some quests done: logs, a campfire and copper (Getting started), nothing of iron yet
		server.runCommand("give @p minecraft:oak_log 4");
		server.runCommand("give @p minecraft:campfire 1");
		server.runCommand("give @p minecraft:copper_ingot 16");
		server.runCommand("advancement grant @p only minecraft:story/mine_stone");
		server.runCommand("advancement grant @p only minecraft:story/upgrade_tools");
		context.waitTicks(60);
		String done = server.computeOnServer(srv -> String.join(", ", QuestData.get(srv).of(player(srv).getStringUUID()).done));
		System.out.println("[wildcatter-test] questBook: done " + done);
		check(done.contains("wildcatter:wood") && done.contains("wildcatter:campfire") && done.contains("wildcatter:copper") && done.contains("wildcatter:stone")
				&& !done.contains("wildcatter:iron"), "wood, campfire, copper and stone should be done, iron not: " + done);
		check(QuestClient.done(Identifier.parse("wildcatter:wood")), "the client should have the progress");
		QuestScreen[] screen = new QuestScreen[1];
		context.runOnClient(client -> {
			client.gui.toastManager().clear();   // the advancements' toasts would cover the right page
			screen[0] = new QuestScreen();
			client.setScreenAndShow(screen[0]);
		});
		context.waitTicks(5);
		context.runOnClient(client -> screen[0].open(Identifier.parse("wildcatter:iron"), false));
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-quest-screen-next");
		context.runOnClient(client -> screen[0].open(Identifier.parse("wildcatter:steam"), true));
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-quest-screen-later");

		// the wood quest's reward claimed through the payload, as the Claim button sends it
		context.runOnClient(client -> screen[0].open(Identifier.parse("wildcatter:wood"), true));
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-quest-screen-claim");
		int apples = count(Items.APPLE);
		context.runOnClient(client -> QuestNet.claim(Identifier.parse("wildcatter:wood")));
		context.waitTicks(30);
		check(count(Items.APPLE) == apples + 3, "claiming should give three apples: " + apples + " -> " + count(Items.APPLE));
		check(QuestClient.claimed(Identifier.parse("wildcatter:wood")), "the client should see the reward claimed");
		context.waitTicks(5);
		context.takeScreenshot("wildcatter-quest-screen-claimed");
		context.runOnClient(client -> screen[0].search("steel"));
		context.waitTicks(5);
		context.takeScreenshot("wildcatter-quest-screen-search");
		context.setScreen(() -> null);
		System.out.println("[wildcatter-test] questBook ok: done " + done);
	}
}
