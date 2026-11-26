package dev.wildcatter.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Built From Scratch, episode 1 (WILDCATTER_ONLY=bfs01; never part of the full suite). BFS_SCENE picks the work:
 * {@code scout} looks for the series' base site in a fresh world of seed BFS_SEED (default {@link #SEED}): flat ground, trees,
 * water and stone near, from the world's own terrain and biome noise, then visits the best few and takes a still of each from the air.
 */
public class Bfs01Films extends TrailerClientGameTest {
	/** The series' world (Built From Scratch): an ordinary new world of this seed. */
	static final String SEED = "Homestead";

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("bfs01")) {
			return;
		}
		this.context = context;
		String scene = System.getenv().getOrDefault("BFS_SCENE", "film");
		String seed = System.getenv().getOrDefault("BFS_SEED", SEED);
		out = java.nio.file.Path.of("bfs01").toAbsolutePath();
		try {
			java.nio.file.Files.createDirectories(out);
		} catch (java.io.IOException ex) {
			throw new RuntimeException(ex);
		}
		context.getInput().resizeWindow(1440, 810);
		for (String one : seed.split(";")) {
			world(scene, one);
		}
	}

	private void world(String scene, String seed) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(seed);
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.SURVIVAL);
			s.setAllowCommands(true);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule send_command_feedback false");
			if (scene.equals("scout")) {
				scout(seed);
			} else if (scene.equals("survey")) {
				survey();
			} else if (scene.equals("film")) {
				new Bfs01Episode(this).run();
			}
		}
	}

	// ---- scout ---------------------------------------------------------------------------------------------------------------------

	private static String biome(ServerLevel level, int x, int z) {
		Holder<Biome> b = level.getUncachedNoiseBiome(x >> 2, 64 >> 2, z >> 2);
		return b.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
	}

	private static boolean treeBiome(String b) {
		return b.contains("forest") || b.contains("taiga") || b.equals("meadow") || b.equals("dark_forest");
	}

	private void scout(String seed) {
		List<String> lines = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var gen = level.getChunkSource().getGenerator();
			var rs = level.getChunkSource().randomState();
			java.util.function.IntBinaryOperator h = (px, pz) -> gen.getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
			BlockPos spawn = level.getRespawnData().pos();
			List<double[]> found = new ArrayList<>();
			for (int cx = spawn.getX() - 960; cx <= spawn.getX() + 960; cx += 32) {
				for (int cz = spawn.getZ() - 960; cz <= spawn.getZ() + 960; cz += 32) {
					String b = biome(level, cx, cz);
					if (!(b.equals("plains") || b.equals("sunflower_plains") || b.equals("meadow") || b.equals("forest") || b.equals("flower_forest")
							|| b.equals("birch_forest") || b.equals("savanna"))) {
						continue;
					}
					int lo = 999, hi = -999;
					for (int dx = -24; dx <= 24; dx += 4) {
						for (int dz = -24; dz <= 24; dz += 4) {
							int v = h.applyAsInt(cx + dx, cz + dz);
							lo = Math.min(lo, v);
							hi = Math.max(hi, v);
						}
					}
					if (lo < 65 || hi - lo > 4) {
						continue;
					}
					int c = h.applyAsInt(cx, cz);
					// trees, water and a hill: the nearest of each on rings out to 128 blocks
					double tree = 999, water = 999, hill = 999;
					for (int r = 16; r <= 128; r += 8) {
						for (int a = 0; a < 32; a++) {
							double ang = a * Math.PI / 16;
							int px = cx + (int) Math.round(r * Math.cos(ang)), pz = cz + (int) Math.round(r * Math.sin(ang));
							if (tree > r && (treeBiome(biome(level, px, pz)) || (r <= 40 && treeBiome(b)))) {
								tree = r;
							}
							int v = h.applyAsInt(px, pz);
							String pb = water > r || hill > r ? biome(level, px, pz) : "";
							if (water > r && (v < 62 || pb.equals("river"))) {
								water = r;
							}
							if (hill > r && v >= c + 12) {
								hill = r;
							}
						}
					}
					if (tree > 80 || water > 110 || hill > 128) {
						continue;
					}
					double score = (hi - lo) * 6 + Math.abs(water - 48) * 0.4 + tree * 0.5 + Math.abs(hill - 64) * 0.4
							+ Math.hypot(cx - spawn.getX(), cz - spawn.getZ()) * 0.02 + (b.contains("plains") ? 0 : 10);
					found.add(new double[] {score, cx, cz, c, hi - lo, tree, water, hill, b.hashCode()});
				}
			}
			found.sort((p, q) -> Double.compare(p[0], q[0]));
			List<String> out = new ArrayList<>();
			out.add("spawn " + spawn.toShortString() + ", " + found.size() + " candidates");
			List<double[]> picked = new ArrayList<>();
			for (double[] f : found) {
				boolean far = true;
				for (double[] p : picked) {
					far &= Math.hypot(p[1] - f[1], p[2] - f[2]) > 160;
				}
				if (!far) {
					continue;
				}
				picked.add(f);
				out.add(String.format(Locale.ROOT, "%d %d %d score %.1f range %d tree %.0f water %.0f hill %.0f biome %s", (int) f[1], (int) f[3], (int) f[2], f[0], (int) f[4], f[5],
						f[6], f[7], biome(level, (int) f[1], (int) f[2])));
				if (picked.size() == 8) {
					break;
				}
			}
			return out;
		});
		for (String l : lines) {
			log("scout " + seed + ": " + l);
		}
		server.runCommand("gamemode spectator @p");
		server.runCommand("time set 6000");
		server.runCommand("gamerule advance_time false");
		context.runOnClient(client -> {
			client.options.renderDistance().set(12);
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		for (int i = 1; i < lines.size() && i <= 3; i++) {
			String[] p = lines.get(i).split(" ");
			int x = Integer.parseInt(p[0]), y = Integer.parseInt(p[1]), z = Integer.parseInt(p[2]);
			server.runCommand("tp @p " + x + " " + (y + 70) + " " + (z + 70) + " 180 45");
			context.waitTicks(260);
			screenshot("scout_" + Math.abs(seed.hashCode() % 1000) + "_" + i + "_a");
			server.runCommand("tp @p " + (x - 60) + " " + (y + 30) + " " + (z - 60) + " -45 20");
			context.waitTicks(200);
			screenshot("scout_" + Math.abs(seed.hashCode() % 1000) + "_" + i + "_b");
			int[] ores = server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				int[] n = new int[4];
				for (BlockPos q : BlockPos.betweenClosed(x - 64, y - 24, z - 64, x + 64, y + 30, z + 64)) {
					var st = level.getBlockState(q);
					int k = st.is(net.minecraft.world.level.block.Blocks.COAL_ORE) ? 0 : st.is(net.minecraft.world.level.block.Blocks.IRON_ORE) ? 1
							: st.is(net.minecraft.world.level.block.Blocks.COPPER_ORE) ? 2 : -1;
					if (k < 0) {
						continue;
					}
					n[k]++;
					for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
						if (level.getBlockState(q.relative(d)).isAir()) {
							n[3]++;
							break;
						}
					}
				}
				return n;
			});
			log(String.format(Locale.ROOT, "scout %s: site %d at %d %d %d: coal %d iron %d copper %d, %d of them open to air", seed, i, x, y, z, ores[0], ores[1], ores[2], ores[3]));
		}
	}

	// ---- survey: a map of the chosen site -------------------------------------------------------------------------------------------

	/** The base site (seed "Homestead"): plains beside a forest, a river to the east, a stony gully to the south. */
	static final int SX = -576, SZ = 944;

	private void survey() {
		server.runCommand("gamemode spectator @p");
		server.runCommand("time set 6000");
		server.runCommand("gamerule advance_time false");
		server.runCommand("tp @p " + SX + " 120 " + SZ + " 0 90");
		context.waitTicks(300);
		List<String> map = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			List<String> rows = new ArrayList<>();
			List<String> ores = new ArrayList<>();
			int r = 56;
			for (int z = SZ - r; z <= SZ + r; z++) {
				StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "%5d ", z));
				for (int x = SX - r; x <= SX + r; x++) {
					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
					var st = level.getBlockState(new BlockPos(x, y, z));
					char c;
					if (st.is(net.minecraft.tags.BlockTags.LOGS)) {
						c = 'T';
					} else if (!st.getFluidState().isEmpty()) {
						c = st.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? 'L' : '~';
					} else if (st.is(net.minecraft.world.level.block.Blocks.STONE) || st.is(net.minecraft.world.level.block.Blocks.ANDESITE)
							|| st.is(net.minecraft.world.level.block.Blocks.GRANITE) || st.is(net.minecraft.world.level.block.Blocks.DIORITE)) {
						c = 'S';
					} else if (st.is(net.minecraft.world.level.block.Blocks.SAND) || st.is(net.minecraft.world.level.block.Blocks.GRAVEL)) {
						c = ',';
					} else if (st.is(net.minecraft.world.level.block.Blocks.SUGAR_CANE)) {
						c = 'c';
					} else {
						int d = y - 66;
						c = d < -9 ? '-' : d > 9 ? '+' : (char) ('0' + Math.max(0, Math.min(9, d)));
						if (d < 0) {
							c = (char) ('a' + Math.min(8, -d - 1));
						}
					}
					if (x == SX && z == SZ) {
						c = '@';
					}
					row.append(c);
				}
				rows.add(row.toString());
			}
			for (BlockPos q : BlockPos.betweenClosed(SX - 64, 30, SZ - 64, SX + 64, 110, SZ + 64)) {
				var st = level.getBlockState(q);
				String k = st.is(net.minecraft.world.level.block.Blocks.COAL_ORE) ? "coal" : st.is(net.minecraft.world.level.block.Blocks.IRON_ORE) ? "iron"
						: st.is(net.minecraft.world.level.block.Blocks.COPPER_ORE) ? "copper" : null;
				if (k == null) {
					continue;
				}
				boolean open = false;
				for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
					BlockPos n = q.relative(d);
					open |= level.getBlockState(n).isAir() && level.canSeeSky(n);
				}
				if (open) {
					ores.add(k + " " + q.toShortString());
				}
			}
			rows.add("open-sky ores: " + ores.size());
			rows.addAll(ores);
			return rows;
		});
		for (String l : map) {
			log("survey " + l);
		}
		int[][] views = {{0, 40, 50, 180, 35}, {50, 30, 0, 90, 30}, {0, 30, -50, 0, 30}, {-50, 30, 0, -90, 30}};
		for (int i = 0; i < views.length; i++) {
			int[] v = views[i];
			server.runCommand("tp @p " + (SX + v[0]) + " " + (66 + v[1]) + " " + (SZ + v[2]) + " " + v[3] + " " + v[4]);
			context.waitTicks(200);
			screenshot("survey_" + i);
		}
	}

	@Override
	void screenshot(String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1600, 900).withDestinationDir(out).disableCounterPrefix());
		log("shot " + name);
	}
}
