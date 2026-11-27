package dev.wildcatter.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;

/**
 * The recorder's proof film (film60, 2026-11-27; WILDCATTER_ONLY=film60, never part of the full suite): film {@code film60-pan}, a slow
 * pan made the way the episodes make their camera moves (the rotation set from the test thread each tick), past a row of wool posts on
 * a stone platform in the sky, a pig and a few arrows flying; film {@code film60-walk}, the player walking on the keys (seen from
 * behind) and then walked a step a tick by setPos while turning, as the episodes' walk and face helpers do. Checked with ffmpeg's
 * mpdecimate: in a 60 fps take every frame differs from the one before during a move, a frame each 16.7 ms.
 */
public class Film60ProofClientGameTest implements FabricClientGameTest {
	private static final int Y = 120;
	private ClientGameTestContext context;
	private TestServerContext server;

	@Override
	public void runTest(ClientGameTestContext context) {
		String only = System.getenv("WILDCATTER_ONLY");
		if (only == null || !java.util.Arrays.asList(only.split(",")).contains("film60")) {
			return;
		}
		this.context = context;
		context.getInput().resizeWindow(1440, 810);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed("film60");
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
			s.setAllowCommands(true);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.computeOnServer(srv -> {
				var rules = srv.overworld().getGameRules();
				rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, srv);
				rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, srv);
				return null;
			});
			context.runOnClient(c -> c.options.bobView().set(true));
			platform();
			pan();
			walk();
		}
	}

	private void platform() {
		server.runCommand("forceload add -40 -40 40 40");
		server.runCommand("fill -24 " + Y + " -24 24 " + Y + " 24 minecraft:stone");
		for (int x = -24; x <= 24; x += 2) {
			for (int z = -24; z <= 24; z += 4) {
				server.runCommand("setblock " + x + " " + Y + " " + z + " minecraft:polished_andesite");
			}
		}
		String[] wool = {"white", "red", "yellow", "lime", "light_blue", "magenta", "orange", "black"};
		for (int i = 0; i < 25; i++) {
			int x = -24 + i * 2;
			server.runCommand("fill " + x + " " + (Y + 1) + " 12 " + x + " " + (Y + 3 + i % 3) + " 12 minecraft:" + wool[i % wool.length] + "_wool");
		}
		server.runCommand("fill -24 " + (Y + 1) + " -24 24 " + (Y + 8) + " 11 minecraft:air");
		server.runCommand("summon minecraft:pig 4 " + (Y + 1) + " 6");
		server.runCommand("summon minecraft:pig -6 " + (Y + 1) + " 8");
		context.waitTicks(60);
	}

	private void place(double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", x, y, z, yaw, pitch));
		context.runOnClient(c -> {
			c.player.snapTo(x, y, z, yaw, pitch);
			c.player.setDeltaMovement(Vec3.ZERO);
		});
		context.waitTicks(20);
	}

	/** Six seconds turning 70 degrees, eased, the rotation set from the test thread each tick (as Bfs01Episode.face does). */
	private void pan() {
		place(0.5, Y + 1, -4.5, -35, 8);
		Filming film = context.computeOnClient(c -> Filming.start(c, "film60-pan"));
		try {
			int ticks = 120;
			for (int i = 0; i <= ticks; i++) {
				double s = (double) i / ticks;
				s = s * s * (3 - 2 * s);
				float yaw = (float) (-35 + 70 * s);
				context.runOnClient(c -> {
					c.player.setYRot(yaw);
					c.player.setXRot(8);
					c.player.setYHeadRot(yaw);
				});
				if (i % 30 == 0) {
					server.runCommand(String.format(java.util.Locale.ROOT, "summon minecraft:arrow %d %d 10 {Motion:[0.9d,0.55d,0.0d]}", -16 + i / 5, Y + 2));
				}
				context.waitTick();
			}
			context.waitTicks(10);
		} finally {
			film.stop();
		}
	}

	/** The player walks forward on the key, seen from behind (3 s), then is walked by setPos a fifth of a block a tick while turning (3 s). */
	private void walk() {
		place(-10.5, Y + 1, 2.5, -90, 12);
		Filming film = context.computeOnClient(c -> Filming.start(c, "film60-walk"));
		try {
			context.runOnClient(c -> c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.getInput().holdKey(o -> o.keyUp);
			context.waitTicks(60);
			context.getInput().releaseKey(o -> o.keyUp);
			context.waitTicks(15);
			context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
			Vec3 from = context.computeOnClient(c -> c.player.position());
			for (int i = 1; i <= 60; i++) {
				Vec3 p = from.add(0.2 * i, 0, 0);
				float yaw = (float) (-90 + 60.0 * i / 60);
				context.runOnClient(c -> {
					c.player.setPos(p.x, p.y, p.z);
					c.player.setYRot(yaw);
					c.player.setYHeadRot(yaw);
					c.player.setDeltaMovement(Vec3.ZERO);
				});
				context.waitTick();
			}
			context.waitTicks(10);
		} finally {
			film.stop();
		}
	}
}
