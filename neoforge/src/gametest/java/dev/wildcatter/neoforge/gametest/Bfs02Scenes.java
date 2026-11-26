package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.AttachedGear;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.BreakerBlockEntity;
import dev.wildcatter.block.FireboxBlockEntity;
import dev.wildcatter.block.FuseBlock;
import dev.wildcatter.block.GearMount;
import dev.wildcatter.block.LoadBlock;
import dev.wildcatter.block.LoadBlockEntity;
import dev.wildcatter.block.ReceptacleBlockEntity;
import dev.wildcatter.block.SteamTurbineBlock;
import dev.wildcatter.block.SteamTurbineBlockEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.quest.Milestones;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestTracker;
import dev.wildcatter.safety.Grounding;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

/**
 * Built From Scratch, episode 2 "First light" (the First power chapter, {@code wildcatter_quests/first_power.json}): the proof that what
 * the episode films works in normal play, on the episode's own layout (relative to the campfire at 0, 1, 0, as the film's base at
 * -567, 66, 945):
 * <ul>
 * <li>{@code bfs02_first_power}: a boiler on a lit campfire with four buckets, a steam turbine on a stone pad against it, a hot wire out
 * to a lamp. With the hot wire alone (the episode's "what goes wrong") the lamp stays dark, reads NO POWER, and neither the turbine's
 * milestone nor the lamp's is reached: Raise steam and Light a lamp stay open. The neutral laid back, the lamp lights and both quests
 * finish at once (the turbine generates only with a load). Then the real chapter's last three quests in order, each only at its own step:
 * Protect the circuit (breaker, fuse, ground rod and meter carried; the breaker set in the hot wire at the turbine, the lamp still lit
 * through it, and rods against the turbine and the lamp ground both); A firebox (the campfire swapped for a firebox of coal: the boiler
 * boils on it and the turbine is rated by it); Receptacle and plug (a fused branch over the neutral to a receptacle with a plug adapter
 * set against it: the receptacle has the low tier's volts on its line).</li>
 * </ul>
 */
final class Bfs02Scenes {
	private Bfs02Scenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("bfs02_first_power", Bfs02Scenes::firstPower);
		return out;
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String quest) {
		return QuestData.get(server).of(player.getStringUUID()).done.contains("wildcatter:" + quest);
	}

	private static boolean reached(MinecraftServer server, ServerPlayer player, Identifier key) {
		return QuestData.get(server).of(player.getStringUUID()).milestones.contains(key.toString());
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	private static void give(ServerPlayer player, String id, int n) {
		player.getInventory().add(new ItemStack(item(id), n));
	}

	static void firstPower(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		QuestBook book = QuestTracker.book(server);
		QuestBook.Chapter chapter = book.chapter(Identifier.parse("wildcatter:first_power"));
		check(helper, chapter != null, "the First power chapter should load");
		List<String> ids = book.quests().values().stream().filter(q -> q.chapter().equals(chapter.id())).map(q -> q.id().getPath()).toList();
		check(helper, ids.equals(List.of("steam", "lamp", "protection", "firebox", "plugs")), "the chapter should hold the episode's five quests: " + ids);
		GridSceneKit.floor(helper, -2, -4, 8, 4);
		for (int x = -2; x <= 8; x++) {
			for (int z = -4; z <= 4; z++) {
				helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
			}
		}
		// the episode 1 campfire, the boiler on it, four buckets
		helper.setBlock(0, 1, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		GridSceneKit.place(helper, WildcatterBlocks.BOILER, 0, 2, 0, Direction.NORTH);
		BoilerBlockEntity boiler = GridSceneKit.entity(helper, 0, 2, 0, BoilerBlockEntity.class);
		for (int i = 0; i < 4; i++) {
			check(helper, boiler.addWater(dev.wildcatter.Tuning.WATER_PER_BUCKET) == dev.wildcatter.Tuning.WATER_PER_BUCKET, "each bucket should fit");
		}
		// a stone pad for the turbine (and later the breaker and the ground rod), the turbine against the boiler, facing east
		helper.setBlock(1, 1, 0, Blocks.SMOOTH_STONE);
		helper.setBlock(2, 1, 0, Blocks.SMOOTH_STONE);
		helper.setBlock(1, 1, 1, Blocks.SMOOTH_STONE);
		helper.setBlock(1, 2, 0, WildcatterBlocks.STEAM_TURBINE.defaultBlockState().setValue(SteamTurbineBlock.FACING, Direction.EAST));
		// the hot wire out of its east face, across the pad, down and on to the lamp
		int[][] hot = {{2, 2, 0}, {3, 2, 0}, {3, 1, 0}, {4, 1, 0}};
		for (int[] c : hot) {
			put(helper, "wool_wire_hot", c[0], c[1], c[2]);
		}
		helper.setBlock(5, 1, 0, WildcatterBlocks.ELECTRIC_LAMP);
		ServerPlayer player = QuestScenes.player(helper, 6, 1, 3);
		give(player, "wildcatter:boiler", 1);
		give(player, "wildcatter:steam_turbine", 1);
		give(player, "wildcatter:wool_wire_hot", 4);
		give(player, "wildcatter:wool_wire_neutral", 4);
		give(player, "wildcatter:electric_lamp", 1);
		BlockPos lampAt = abs(helper, 5, 1, 0), turbineAt = abs(helper, 1, 2, 0);
		int[][] neutral = {{1, 2, -1}, {1, 1, -1}, {2, 1, -1}, {3, 1, -1}, {4, 1, -1}, {5, 1, -1}};
		helper.runAfterDelay(260, () -> {
			// what goes wrong: one wire is a dead end
			LoadBlockEntity lamp = helper.getBlockEntity(new BlockPos(5, 1, 0), LoadBlockEntity.class);
			String read = lamp.inspect().getString();
			check(helper, !helper.getBlockState(new BlockPos(5, 1, 0)).getValue(LoadBlock.LIT), "the lamp on the hot wire alone should stay dark");
			check(helper, read.contains("NO POWER"), "the meter should read NO POWER on a lamp with no neutral: " + read);
			QuestTracker.check(player);
			check(helper, !reached(server, player, Milestones.LAMP_LIT) && !reached(server, player, Milestones.STEAM_TURBINE_GENERATING),
					"with no loop, neither the lamp's nor the turbine's milestone should be reached: lamp " + reached(server, player, Milestones.LAMP_LIT)
							+ ", turbine " + reached(server, player, Milestones.STEAM_TURBINE_GENERATING) + "; "
							+ helper.getBlockEntity(new BlockPos(1, 2, 0), SteamTurbineBlockEntity.class).inspect().getString() + " | " + read);
			check(helper, !done(server, player, "steam") && !done(server, player, "lamp"), "Raise steam and Light a lamp should wait for the loop");
			check(helper, boiler.lifting(), "a boiler with nothing drawing its steam should reach full pressure and lift its safety valve");
			log("bfs02_first_power", "hot wire only: " + read + " | boiler " + boiler.inspect().getString() + " | "
					+ helper.getBlockEntity(new BlockPos(1, 2, 0), SteamTurbineBlockEntity.class).inspect().getString());
			for (int[] c : neutral) {
				put(helper, "wool_wire_neutral", c[0], c[1], c[2]);
			}
		});
		helper.runAfterDelay(380, () -> {
			check(helper, helper.getBlockState(new BlockPos(5, 1, 0)).getValue(LoadBlock.LIT), "with the neutral back, the lamp should light: "
					+ helper.getBlockEntity(new BlockPos(5, 1, 0), LoadBlockEntity.class).inspect().getString());
			QuestTracker.check(player);
			check(helper, done(server, player, "steam") && done(server, player, "lamp"), "Raise steam and Light a lamp should finish together");
			check(helper, !done(server, player, "protection"), "Protect the circuit should not be done yet");
			check(helper, !Grounding.isGrounded(helper.getLevel(), turbineAt) && !Grounding.isGrounded(helper.getLevel(), lampAt), "nothing is grounded before the rods");
			log("bfs02_first_power", "loop closed: " + helper.getBlockEntity(new BlockPos(5, 1, 0), LoadBlockEntity.class).inspect().getString() + " | "
					+ helper.getBlockEntity(new BlockPos(1, 2, 0), SteamTurbineBlockEntity.class).inspect().getString());
			// Protect the circuit: each item alone is not enough
			give(player, "wildcatter:breaker", 1);
			give(player, "wildcatter:fuse", 1);
			give(player, "wildcatter:ground_rod", 2);
			QuestTracker.check(player);
			check(helper, !done(server, player, "protection"), "Protect the circuit should wait for the power meter");
			player.getInventory().add(new ItemStack(WildcatterItems.POWER_METER));
			QuestTracker.check(player);
			check(helper, done(server, player, "protection"), "breaker, fuse, ground rod and meter should finish Protect the circuit");
			check(helper, !done(server, player, "firebox"), "A firebox should not be done yet");
			// the breaker in the hot wire right at the turbine: in at the back (the turbine, west), out at the front (east); on the pad, floor mount
			helper.setBlock(2, 2, 0, WildcatterBlocks.BREAKER.defaultBlockState().setValue(BreakerBlock.FACING, Direction.EAST).setValue(GearMount.MOUNT, GearMount.FLOOR));
			player.getInventory().removeItem(player.getInventory().findSlotMatchingItem(new ItemStack(item("wildcatter:breaker"))), 1);
			// a ground rod on the pad against the turbine, one in the earth beside the lamp
			put(helper, "ground_rod", 1, 2, 1);
			put(helper, "ground_rod", 5, 1, 1);
		});
		helper.runAfterDelay(500, () -> {
			check(helper, helper.getBlockState(new BlockPos(5, 1, 0)).getValue(LoadBlock.LIT), "the lamp should stay lit through the breaker");
			BreakerBlockEntity breaker = helper.getBlockEntity(new BlockPos(2, 2, 0), BreakerBlockEntity.class);
			String br = breaker.inspect().getString();
			check(helper, br.contains("ON, carrying") && breaker.amps() > 0.2, "the breaker should carry the lamp's current: " + br);
			check(helper, Grounding.isGrounded(helper.getLevel(), turbineAt) && Grounding.isGrounded(helper.getLevel(), lampAt), "the rods should ground the turbine and the lamp");
			QuestTracker.check(player);
			check(helper, done(server, player, "protection"), "Protect the circuit should stay done with the breaker set down");
			log("bfs02_first_power", "protected: " + br + " | rated " + Math.round(dev.wildcatter.Tuning.BOILER_STEAM_PER_TICK * 20 * dev.wildcatter.Tuning.JOULES_PER_STEAM) + " W on the campfire");
			// A firebox: the campfire out, a firebox of coal in its place under the boiler
			give(player, "wildcatter:firebox", 1);
			QuestTracker.check(player);
			check(helper, done(server, player, "firebox"), "carrying a firebox should finish A firebox");
			check(helper, !done(server, player, "plugs"), "Receptacle and plug should not be done yet");
			helper.setBlock(0, 1, 0, Blocks.AIR);
			check(helper, helper.getBlockState(new BlockPos(0, 2, 0)).is(WildcatterBlocks.BOILER), "the boiler should stay put when the campfire under it goes");
			helper.setBlock(0, 1, 0, WildcatterBlocks.FIREBOX);
			helper.getBlockEntity(new BlockPos(0, 1, 0), FireboxBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 16));
		});
		helper.runAfterDelay(700, () -> {
			FireboxBlockEntity firebox = helper.getBlockEntity(new BlockPos(0, 1, 0), FireboxBlockEntity.class);
			String tr = helper.getBlockEntity(new BlockPos(1, 2, 0), SteamTurbineBlockEntity.class).inspect().getString();
			check(helper, firebox.isBurning() || helper.getBlockState(new BlockPos(0, 2, 0)).getValue(dev.wildcatter.block.BoilerBlock.LIT),
					"the firebox should burn under the boiler: " + firebox.inspect().getString() + " | " + boiler.inspect().getString());
			check(helper, tr.contains("on a firebox"), "the turbine should be rated by the firebox: " + tr);
			check(helper, helper.getBlockState(new BlockPos(5, 1, 0)).getValue(LoadBlock.LIT), "the lamp should stay lit on the firebox");
			log("bfs02_first_power", "firebox: " + firebox.inspect().getString() + " | " + tr);
			// Receptacle and plug: a branch from the hot wire's high cell, through a fuse over the neutral, down to a receptacle on the
			// neutral's corner, and a plug adapter set against it
			helper.setBlock(3, 2, -1, WildcatterBlocks.FUSE.defaultBlockState().setValue(FuseBlock.FACING, Direction.NORTH).setValue(GearMount.MOUNT, GearMount.FLOOR));
			put(helper, "wool_wire_hot", 3, 2, -2);
			put(helper, "wool_wire_hot", 3, 1, -2);
			put(helper, "receptacle_nema_5_15", 4, 1, -2);
			helper.setBlock(5, 1, -2, BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("plug_nema_5_15")).defaultBlockState().setValue(AttachedGear.ATTACH, Direction.WEST));
			give(player, "wildcatter:receptacle_nema_5_15", 1);
			QuestTracker.check(player);
			check(helper, !done(server, player, "plugs"), "Receptacle and plug should wait for the plug");
			give(player, "wildcatter:plug_nema_5_15", 1);
			QuestTracker.check(player);
			check(helper, done(server, player, "plugs"), "a receptacle and a plug should finish Receptacle and plug");
		});
		helper.runAfterDelay(820, () -> {
			ReceptacleBlockEntity receptacle = helper.getBlockEntity(new BlockPos(4, 1, -2), ReceptacleBlockEntity.class);
			check(helper, receptacle.volts() > Voltage.LOW.nominal() * 0.8, "the receptacle should have the low tier's volts on its line: " + receptacle.volts());
			check(helper, helper.getBlockState(new BlockPos(5, 1, -2)).getBlock() instanceof dev.wildcatter.block.PlugAdapterBlock, "the plug adapter should stay against its receptacle");
			check(helper, helper.getBlockState(new BlockPos(5, 1, 0)).getValue(LoadBlock.LIT), "the lamp should still be lit");
			for (String q : ids) {
				check(helper, done(server, player, q), q + " should be done");
			}
			log("bfs02_first_power", String.format("ok: %s; receptacle %.0f V on its line (the low tier, %s)", ids, receptacle.volts(), Voltage.LOW.label()));
			QuestScenes.leave(helper, player);
			helper.succeed();
		});
	}
}
