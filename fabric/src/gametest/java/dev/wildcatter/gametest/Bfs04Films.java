package dev.wildcatter.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Built From Scratch, episode 4 "Gas, motors and poles" (WILDCATTER_ONLY=bfs04; never part of the full suite): scene {@code film} makes a
 * new world of the series' seed (Bfs01Films#SEED) and plays episode 3 through off camera, exactly as its film plays it (Bfs03Episode with
 * no shot wanted: episode 1's base and episode 2's plant rebuilt, then the step-up and the Oil and gas chapter in survival), so the world
 * is the one episode 3 left. Then it plays the rest of Three-phase power and the first half of Distribution in survival (Bfs04Episode).
 * With BFS_GRID=x1,z1,x2,z2 it only logs a height map of the pole line's ground.
 */
public class Bfs04Films extends TrailerClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("bfs04")) {
			return;
		}
		this.context = context;
		out = java.nio.file.Path.of("bfs04").toAbsolutePath();
		try {
			java.nio.file.Files.createDirectories(out);
		} catch (java.io.IOException ex) {
			throw new RuntimeException(ex);
		}
		context.getInput().resizeWindow(1440, 810);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(Bfs01Films.SEED);
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.SURVIVAL);
			s.setAllowCommands(true);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule send_command_feedback false");
			if (System.getenv("BFS_GRID") != null) {
				grid(System.getenv("BFS_GRID"));
				return;
			}
			// episode 3, played through off camera in this world (its film code, nothing filmed)
			if (System.getenv("BFS_QUICK3") != null) {
				new Bfs04Episode(this).run();   // a working run: episode 3's world set by command (Bfs04Episode.quickThree)
				return;
			}
			Bfs03Films three = new Bfs03Films();
			three.context = context;
			three.server = server;
			three.out = out;
			long t0 = System.nanoTime();
			new Bfs03Episode(three, java.util.Set.of("none")).run();
			Bfs04Episode.log(String.format(java.util.Locale.ROOT, "episode 3 played off camera in %.0f s", (System.nanoTime() - t0) / 1e9));
			new Bfs04Episode(this).run();
		}
	}

	/** BFS_GRID=x1,z1,x2,z2: a height map (ground top, last two digits; ~ water, T a tree's log over it). */
	private void grid(String box) {
		String[] v = box.split(",");
		int x1 = Integer.parseInt(v[0].trim()), z1 = Integer.parseInt(v[1].trim()), x2 = Integer.parseInt(v[2].trim()), z2 = Integer.parseInt(v[3].trim());
		server.runCommand("gamemode spectator @p");
		server.runCommand("tp @p " + (x1 + x2) / 2 + " 140 " + (z1 + z2) / 2);
		server.runCommand("forceload add " + x1 + " " + z1 + " " + x2 + " " + z2);
		context.waitTicks(300);
		Bfs04Episode.log("grid " + box + " (columns x " + x1 + ".." + x2 + ")");
		for (int z = z1; z <= z2; z++) {
			final int zz = z;
			String row = server.computeOnServer(srv -> {
				var level = srv.overworld();
				StringBuilder b = new StringBuilder(String.format(java.util.Locale.ROOT, "%5d ", zz));
				for (int x = x1; x <= x2; x++) {
					int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz) - 1;
					var p = new net.minecraft.core.BlockPos(x, top, zz);
					var st = level.getBlockState(p);
					char c = !level.getFluidState(p).isEmpty() ? '~' : st.is(net.minecraft.tags.BlockTags.LOGS) ? 'T' : ' ';
					b.append(String.format(java.util.Locale.ROOT, "%02d%c", Math.floorMod(top, 100), c));
				}
				return b.toString();
			});
			Bfs04Episode.log("grid " + row);
		}
	}

	@Override
	void screenshot(String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1600, 900).withDestinationDir(out).disableCounterPrefix());
		Bfs04Episode.log("shot " + name);
	}
}
