package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.CraneBlockEntity;
import dev.wildcatter.block.CraneRailBlock;
import dev.wildcatter.block.SiteMarkerBlockEntity;
import dev.wildcatter.item.SurveyToolItem;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The cranes' siting (docs/questions/plant-crane-gaps.md, 1 to 4), on the server.
 * <ul>
 * <li>{@code crane_siting}: a crawler crane set on dirt reads SOFT GROUND; on stone it looks for a site. In an 18 m/s gale it reads WIND.
 * With a stone pillar in its slew from its chest to a site it reads BLOCKED; with the pillar gone it starts the lift.</li>
 * <li>{@code ring_travel}: a ring crane on crane rail, its site 150 m down the rails, out of its 120 m reach, travels along them until
 * the site is 50 m out, and stops there.</li>
 * </ul>
 */
final class CraneScenes {
	private CraneScenes() {
	}

	private static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	private static void set(GameTestHelper helper, int x, int y, int z, BlockState state) {
		helper.getLevel().setBlock(abs(helper, x, y, z), state, 3);
	}

	/** A floor of {@code block} at y 0 over the box, its chunks force-loaded (as AmmoniaPlantScenes.floor), and fair weather. */
	private static void floor(GameTestHelper helper, Block block, int x1, int z1, int x2, int z2) {
		ServerLevel level = helper.getLevel();
		BlockPos a = abs(helper, x1, 0, z1), b = abs(helper, x2, 0, z2);
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				set(helper, x, 0, z, block.defaultBlockState());
				for (int y = 1; y <= 3; y++) {
					set(helper, x, y, z, Blocks.AIR.defaultBlockState());
				}
			}
		}
		level.getServer().setWeatherParameters(36_000, 0, false, false);
	}

	private static CraneBlockEntity place(GameTestHelper helper, Block block, int x, int y, int z) {
		BlockPos main = abs(helper, x, y, z);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		helper.getLevel().setBlock(main, state, 3);
		BigParts.grow(helper.getLevel(), main, state);
		return (CraneBlockEntity) helper.getLevel().getBlockEntity(main);
	}

	/** Surveys the stone bricks in the box from {@code origin} (its corner left empty), clears them and sets a marker there. */
	private static SiteMarkerBlockEntity site(GameTestHelper helper, BlockPos origin, BlockPos far) {
		ServerLevel level = helper.getLevel();
		String data = SurveyToolItem.record(level, origin, far).compressed();
		for (BlockPos p : BlockPos.betweenClosed(origin, far)) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
		level.setBlock(origin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
		SiteMarkerBlockEntity marker = (SiteMarkerBlockEntity) level.getBlockEntity(origin);
		marker.set(data, 0);
		return marker;
	}

	private static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}

	// ---- crane_siting ---------------------------------------------------------------------------------------------------------------
	static void craneSiting(GameTestHelper helper) {
		floor(helper, Blocks.DIRT, -12, -24, 24, 16);
		CraneBlockEntity crawler = place(helper, WildcatterBlocks.CRAWLER_CRANE, 0, 1, 0);   // x 0-8, z 0-8, its centre 4.5, 4.5
		crawler.testFuel(500);
		helper.runAfterDelay(30, () -> {
			String soft = crawler.inspect().getString();
			log("crane_siting", "on dirt | " + soft);
			check(helper, soft.contains("SOFT GROUND") && soft.contains("81 of 81") && soft.contains("dirt or clay"), "a crawler on dirt should read SOFT GROUND: " + soft);
			for (int x = 0; x <= 8; x++) {
				for (int z = 0; z <= 8; z++) {
					set(helper, x, 0, z, Blocks.STONE.defaultBlockState());
				}
			}
			helper.runAfterDelay(30, () -> {
				String firm = crawler.inspect().getString();
				log("crane_siting", "on stone | " + firm);
				check(helper, firm.contains("NO SITE"), "on stone it should look for a site: " + firm);
				crawler.testWind(18);
				helper.runAfterDelay(30, () -> {
					String gale = crawler.inspect().getString();
					log("crane_siting", "gale | " + gale);
					check(helper, gale.contains("WIND") && gale.contains("over its 9 m/s limit"), "in an 18 m/s gale it should read WIND: " + gale);
					crawler.testWind(-1);
					blockedSlew(helper, crawler);
				});
			});
		});
	}

	/** Its site 17 m north, its chest east of its base, and a pillar north-east in the boom's slew. */
	private static void blockedSlew(GameTestHelper helper, CraneBlockEntity crawler) {
		ServerLevel level = helper.getLevel();
		for (int x = 3; x <= 5; x++) {
			for (int z = -14; z <= -12; z++) {
				set(helper, x, 1, z, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		SiteMarkerBlockEntity marker = site(helper, abs(helper, 2, 1, -15), abs(helper, 6, 2, -11));
		set(helper, 9, 1, 4, Blocks.CHEST.defaultBlockState());
		((Container) level.getBlockEntity(abs(helper, 9, 1, 4))).setItem(0, new ItemStack(Items.STONE_BRICKS, 64));
		for (int y = 1; y <= 45; y++) {
			set(helper, 10, y, -1, Blocks.OBSIDIAN.defaultBlockState());   // 7.8 m out at about 45 degrees
		}
		helper.runAfterDelay(40, () -> {
			String blocked = crawler.inspect().getString();
			log("crane_siting", "pillar | " + blocked);
			check(helper, blocked.contains("BLOCKED") && blocked.contains("obsidian"), "the pillar should block its boom's slew: " + blocked + " | site " + marker.inspect().getString());
			for (int y = 1; y <= 45; y++) {
				set(helper, 10, y, -1, Blocks.AIR.defaultBlockState());
			}
			helper.runAfterDelay(140, () -> {
				String working = crawler.inspect().getString();
				log("crane_siting", "clear | " + working);
				check(helper, working.contains("BUILDING"), "with the pillar gone it should start the lift: " + working);
				log("crane_siting", "ok");
				helper.succeed();
			});
		});
	}

	// ---- ring_travel --------------------------------------------------------------------------------------------------------------
	static void ringTravel(GameTestHelper helper) {
		floor(helper, Blocks.STONE, -26, -2, 26, 200);
		BlockState rail = WildcatterBlocks.CRANE_RAIL.defaultBlockState().setValue(CraneRailBlock.AXIS, Direction.Axis.Z);
		for (int z = 20; z <= 190; z++) {
			set(helper, 0, 0, z, rail);   // under its slewing centre (0, 24) and on south
		}
		CraneBlockEntity ring = place(helper, WildcatterBlocks.RING_CRANE, 0, 1, 0);   // x -24..24, z 0-48, its centre 0.5, 24.5
		ring.testFuel(6_000);
		ring.testSpeedup(7_200);   // 20 m/s: a metre a tick
		for (int x = 11; x <= 12; x++) {
			set(helper, x, 1, 175, Blocks.STONE_BRICKS.defaultBlockState());
		}
		site(helper, abs(helper, 10, 1, 174), abs(helper, 13, 2, 176));   // 150 m south of its centre, 10 m east of the rails
		BlockPos start = abs(helper, 0, 1, 0);
		BlockPos stop = abs(helper, 0, 1, 101);   // where the marker is 50 m out: 49 m along, 10 m across
		helper.runAfterDelay(400, () -> {
			ServerLevel level = helper.getLevel();
			for (int z = 0; z <= 150; z++) {
				if (level.getBlockEntity(abs(helper, 0, 1, z)) instanceof CraneBlockEntity c) {
					log("ring_travel", "the crane at z " + z + " | " + c.inspect().getString());
				}
			}
			String there = level.getBlockEntity(stop) instanceof CraneBlockEntity c ? c.inspect().getString() : "(no crane at its stop)";
			log("ring_travel", "at its stop | " + there + " | start " + level.getBlockState(start).getBlock());
			check(helper, level.getBlockEntity(stop) instanceof CraneBlockEntity c && !c.travelling(), "the ring crane should have travelled 101 m and stopped: " + there);
			check(helper, level.getBlockState(start).isAir(), "its old main cell should be clear");
			check(helper, !there.contains("NO SITE") && !there.contains("TRAVEL"), "at its stop the site should be in reach: " + there);
			check(helper, there.contains("diesel"), "it burns diesel as it travels: " + there);
			log("ring_travel", "ok");
			helper.succeed();
		});
	}
}
