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
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
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
		// Tim's answer of 2026-11-14: the first 480 V from a dry-type step-up, the quest the drilling rig now needs
		context.runOnClient(client -> screen[0].open(Identifier.parse("wildcatter:step_up"), true));
		context.waitTicks(10);
		context.takeScreenshot("wildcatter-quest-screen-step-up");

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

	/**
	 * The first 480 V (docs/dry-type-transformer.md): one steam turbine beside a boiler on a firebox, rated by its fire (90 kW), feeds a
	 * dry-type transformer on its LOW side; its 480 V front against the drilling rig's terminal box. Built on a platform high over the flat
	 * world, so the rig has something to drill.
	 */
	void dryType() {
		int n = 1, x0 = 12, z0 = 24;
		int base = server.computeOnServer(srv -> srv.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 10, 10)) + 40;
		int xt = x0 + n, hx = xt + 6;
		// stand on the ground near the plant while it warms up and starts (a player left in the air falls and dies, and then nothing ticks)
		int ground = base - 40 + 1;
		server.runCommand("tp @p " + (x0 + n / 2 + 4) + " " + ground + " " + (z0 + 6) + " 90 -20");
		context.waitTicks(60);
		server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerLevel level = srv.overworld();
			net.minecraft.world.level.block.state.BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
			for (int x = x0 - 1; x <= hx + 2; x++) {
				for (int z = z0 - 2; z <= z0 + 2; z++) {
					level.setBlock(new BlockPos(x, base, z), stone, 3);
				}
			}
			for (int x = xt; x <= hx + 1; x++) {
				for (int z = z0 - 1; z <= z0 + 1; z++) {
					level.setBlock(new BlockPos(x, base + 1, z), stone, 3);
				}
			}
			for (int i = 0; i < n; i++) {
				level.setBlock(new BlockPos(x0 + i, base + 1, z0), stone, 3);
			}
			net.minecraft.world.level.block.state.BlockState rig = WildcatterBlocks.DRILLING_RIG.defaultBlockState()
					.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST);
			level.setBlock(new BlockPos(hx, base + 2, z0), rig, 3);
			dev.wildcatter.multiblock.BigParts.grow(level, new BlockPos(hx, base + 2, z0), rig);
			return null;
		});
		// the starter plant's blocks by command, as a player's placing does
		for (int i = 0; i < n; i++) {
			int x = x0 + i;
			for (int z : new int[] {z0 - 1}) {
				server.runCommand("setblock " + x + " " + (base + 1) + " " + z + " wildcatter:firebox");
				server.runCommand("item replace block " + x + " " + (base + 1) + " " + z + " container.0 with minecraft:coal_block 64");
				server.runCommand("setblock " + x + " " + (base + 2) + " " + z + " wildcatter:boiler");
			}
			server.runCommand("setblock " + x + " " + (base + 2) + " " + z0 + " wildcatter:steam_turbine");
		}
		server.computeOnServer(srv -> {
			for (int i = 0; i < n; i++) {
				for (int z : new int[] {z0 - 1}) {
					((dev.wildcatter.block.BoilerBlockEntity) dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), new BlockPos(x0 + i, base + 2, z))).addWater(4000);
				}
			}
			return null;
		});
		context.waitTicks(220);   // the boilers build their steam, then the dry-type goes in: the switch closing
		server.computeOnServer(srv -> srv.overworld().setBlock(new BlockPos(xt, base + 2, z0), WildcatterBlocks.DRY_TYPE_TRANSFORMER.defaultBlockState()
				.setValue(dev.wildcatter.block.TransformerBlock.FACING, net.minecraft.core.Direction.EAST), 3));
		context.waitTicks(160);
		String rig = read(hx, base + 2, z0);
		String xfmr = read(xt, base + 2, z0);
		System.out.println("[wildcatter-test] dryType: rig " + rig + " | dry-type " + xfmr + " | turbine " + read(xt - 1, base + 2, z0));
		check(rig.contains("drilling,"), "one firebox set through the dry-type should start the rig and keep it drilling: " + rig + " | " + xfmr);
		// stand on the platform's edge by the rig, then on a ledge above the plant (a fall from the platform would kill the player)
		server.runCommand("fill " + (xt - 3) + " " + base + " " + (z0 + 3) + " " + (xt + 6) + " " + base + " " + (z0 + 10) + " minecraft:stone");
		server.runCommand("tp @p " + (xt + 1.5) + " " + (base + 1) + " " + (z0 + 4.5) + " 160 22");
		context.waitTicks(40);
		context.takeScreenshot("wildcatter-dry-type-rig");
		server.runCommand("fill " + (xt - 1) + " " + (base + 4) + " " + (z0 + 7) + " " + (xt + 1) + " " + (base + 4) + " " + (z0 + 9) + " minecraft:stone");
		server.runCommand("tp @p " + (xt + 0.5) + " " + (base + 5) + " " + (z0 + 8.5) + " -155 25");
		context.waitTicks(40);
		context.takeScreenshot("wildcatter-dry-type-plant");
		// take it down, so nothing runs on through the later scenes
		server.runCommand("tp @p " + (x0 + n / 2) + " " + ground + " " + (z0 + 6));
		context.waitTicks(5);
		server.runCommand("fill " + (x0 - 1) + " " + base + " " + (z0 - 2) + " " + (hx + 2) + " " + (base + 12) + " " + (z0 + 17) + " minecraft:air");
		server.runCommand("fill " + hx + " " + (ground + 1) + " " + z0 + " " + hx + " " + base + " " + z0 + " minecraft:air");   // the casing down the hole
		server.runCommand("kill @e[type=item]");
		System.out.println("[wildcatter-test] dryType ok: " + rig);
	}

	private String read(int x, int y, int z) {
		return server.computeOnServer(srv -> dev.wildcatter.multiblock.BigParts.entity(srv.overworld(), new BlockPos(x, y, z)) instanceof dev.wildcatter.energy.Inspectable i
				? i.inspect().getString() : "(no readout)");
	}
}
