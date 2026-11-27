package dev.wildcatter.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Built From Scratch, episode 3 "Strike oil" (WILDCATTER_ONLY=bfs03; never part of the full suite): scene {@code film} makes a new world of
 * the series' seed (Bfs01Films#SEED), rebuilds episode 2's base at the same block positions, then plays the step-up quest and the Oil and
 * gas chapter through in survival (Bfs03Episode). With BFS_SCOUT=1 it only scouts: the seismograph's own report, the ground's height and
 * the biome at points along the survey routes (logged, nothing filmed).
 */
public class Bfs03Films extends TrailerClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("bfs03")) {
			return;
		}
		this.context = context;
		out = java.nio.file.Path.of("bfs03").toAbsolutePath();
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
				for (String g : System.getenv("BFS_GRID").split(";")) {
					grid(g);
				}
			} else if (System.getenv("BFS_SCOUT") != null) {
				scout();
			} else {
				new Bfs03Episode(this).run();
			}
		}
	}

	/** BFS_SCOUT=x1,z1;x2,z2;...: the seismograph's report, ground and biome at each point and along the legs between them, every 8 blocks. */
	private void scout() {
		String route = System.getenv("BFS_SCOUT");
		if (route.isBlank() || route.equals("1")) {
			route = "-568,940;-580,952;-568,940;-540,945;-520,955;-500,965;-483,975;-471,978;-450,985;-420,990;-395,998;-370,1003;-358,1005";
		}
		String[] pts = route.split(";");
		server.runCommand("gamemode spectator @p");
		int[] prev = null;
		for (String p : pts) {
			String[] xz = p.split(",");
			int[] at = {Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim())};
			if (prev != null) {
				int steps = (int) Math.max(1, Math.hypot(at[0] - prev[0], at[1] - prev[1]) / 8);
				for (int i = 1; i < steps; i++) {
					point(prev[0] + (at[0] - prev[0]) * i / steps, prev[1] + (at[1] - prev[1]) * i / steps, false);
				}
			}
			point(at[0], at[1], true);
			prev = at;
		}
	}

	/** BFS_GRID=x1,z1,x2,z2: a height map (ground top, last two digits; ~ water, T a tree's log over it, # a field under the column). */
	private void grid(String box) {
		String[] v = box.split(",");
		int x1 = Integer.parseInt(v[0].trim()), z1 = Integer.parseInt(v[1].trim()), x2 = Integer.parseInt(v[2].trim()), z2 = Integer.parseInt(v[3].trim());
		server.runCommand("gamemode spectator @p");
		server.runCommand("tp @p " + (x1 + x2) / 2 + " 140 " + (z1 + z2) / 2);
		server.runCommand("forceload add " + x1 + " " + z1 + " " + x2 + " " + z2);
		context.waitTicks(200);
		log("grid " + box + " (columns x " + x1 + ".." + x2 + ")");
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
					char f = dev.wildcatter.well.ReservoirData.field(level, x, zz) != null && !dev.wildcatter.well.ReservoirData.field(level, x, zz).deep() ? '#' : '.';
					b.append(String.format(java.util.Locale.ROOT, "%02d%c%c", Math.floorMod(top, 100), c, f));
				}
				return b.toString();
			});
			log("grid " + row);
		}
		server.runCommand("forceload remove " + x1 + " " + z1 + " " + x2 + " " + z2);
	}

	private void point(int x, int z, boolean key) {
		server.runCommand("tp @p " + x + " 140 " + z);
		for (int i = 0; i < 80; i++) {
			context.waitTicks(5);
			boolean loaded = server.computeOnServer(srv -> srv.overworld().hasChunk(x >> 4, z >> 4)
					&& srv.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) > -60);
			if (loaded) {
				break;
			}
		}
		String line = server.computeOnServer(srv -> {
			var level = srv.overworld();
			int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			String ground = level.getBlockState(new net.minecraft.core.BlockPos(x, top, z)).getBlock().getName().getString();
			String biome = level.getBiome(new net.minecraft.core.BlockPos(x, top, z)).getRegisteredName();
			boolean water = !level.getFluidState(new net.minecraft.core.BlockPos(x, top, z)).isEmpty();
			return String.format(java.util.Locale.ROOT, "%d %d top %d %s%s %s | %s", x, z, top, ground, water ? " (water)" : "", biome,
					dev.wildcatter.item.SeismographItem.report(level, x + 0.5, z + 0.5));
		});
		log("scout " + (key ? "KEY " : "") + line);
	}

	@Override
	void screenshot(String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1600, 900).withDestinationDir(out).disableCounterPrefix());
		log("shot " + name);
	}
}
