package dev.wildcatter.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Built From Scratch, episode 2 "First light" (WILDCATTER_ONLY=bfs02; never part of the full suite): scene {@code film} rebuilds episode
 * 1's base in a new world of the series' seed (Bfs01Films#SEED) at the same block positions, then plays the First power chapter
 * through in survival (Bfs02Episode).
 */
public class Bfs02Films extends TrailerClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("bfs02")) {
			return;
		}
		this.context = context;
		out = java.nio.file.Path.of("bfs02").toAbsolutePath();
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
			new Bfs02Episode(this).run();
		}
	}

	@Override
	void screenshot(String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1600, 900).withDestinationDir(out).disableCounterPrefix());
		log("shot " + name);
	}
}
