package dev.wildcatter.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Built From Scratch, episode 5 "Medium voltage" (WILDCATTER_ONLY=bfs05; never part of the full suite): scene {@code film} makes a new
 * world of the series' seed (Bfs01Films#SEED) and plays episodes 3 and 4 through off camera, exactly as their films play them (Bfs03Episode
 * and Bfs04Episode with no shot wanted), so the world is the one episode 4 left. Then it plays the second half of Distribution in survival
 * (Bfs05Episode). BFS_QUICK4 (working runs only) sets the gas well by command instead.
 */
public class Bfs05Films extends TrailerClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("bfs05")) {
			return;
		}
		this.context = context;
		out = java.nio.file.Path.of("bfs05").toAbsolutePath();
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
			if (System.getenv("BFS_QUICK4") == null) {
				// episodes 3 and 4, played through off camera in this world (their film code, nothing filmed)
				Bfs03Films three = new Bfs03Films();
				three.context = context;
				three.server = server;
				three.out = out;
				long t0 = System.nanoTime();
				new Bfs03Episode(three, java.util.Set.of("none")).run();
				Bfs05Episode.log(String.format(java.util.Locale.ROOT, "episode 3 played off camera in %.0f s", (System.nanoTime() - t0) / 1e9));
				Bfs04Films four = new Bfs04Films();
				four.context = context;
				four.server = server;
				four.out = out;
				t0 = System.nanoTime();
				new Bfs04Episode(four, java.util.Set.of("none")).run();
				Bfs05Episode.log(String.format(java.util.Locale.ROOT, "episode 4 played off camera in %.0f s", (System.nanoTime() - t0) / 1e9));
			}
			new Bfs05Episode(this).run();
		}
	}

	@Override
	void screenshot(String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1600, 900).withDestinationDir(out).disableCounterPrefix());
		Bfs05Episode.log("shot " + name);
	}
}
