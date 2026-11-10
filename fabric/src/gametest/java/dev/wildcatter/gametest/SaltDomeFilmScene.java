package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.well.SaltDome;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;

/**
 * {@code saltDomeFilm} (filmed): a salt dome field as a new world has it (the world rule {@code saltDomeGrid192} on). A seed forced over
 * the scene puts a grid dome with a caprock pool and flank fields near; the dome's feature is placed chunk by chunk, so the salt column,
 * its calcite and sulfur caprock and the low mound over it stand where the grid says; the south half is cut away to show it. The
 * seismograph's reading over the crest goes to chat, then the camera walks round the cutaway to the flank fields' side.
 */
final class SaltDomeFilmScene {
	private SaltDomeFilmScene() {
	}

	static void run(ClientGameTestContext context, TestServerContext server, int flat) {
		int x0 = 40, z0 = -9600;
		int y = 30;   // the test world is flat: the scene builds its own ground, stone to y 27 under dirt and grass at 30
		server.runCommand("wildcatter rule saltDomeGrid192 true");
		check(server.computeOnServer(srv -> dev.wildcatter.Rules.saltDomeGrid()), "the command should turn saltDomeGrid192 on");
		long seed = -1;
		SaltDome dome = null;
		for (long s = 1; s < 5_000_000 && seed < 0; s++) {
			SaltDome d = SaltDome.inCell(s, Math.floorDiv(x0, Tuning.DOME_CELL), Math.floorDiv(z0, Tuning.DOME_CELL));
			if (d != null && d.caprockPool() != null && d.flanks().size() >= 2 && Math.abs(d.centerX() - x0) < 60 && Math.abs(d.centerZ() - z0) < 60) {
				seed = s;
				dome = d;
			}
		}
		check(dome != null, "no seed puts a dome with a caprock pool and two flank fields near the film");
		ReservoirData.testSeed = seed;
		ReservoirData.testDomeBiome = true;
		int cx = dome.centerX(), cz = dome.centerZ();
		int reach = dome.radius() + 12;
		server.runCommand("effect give @p minecraft:night_vision 1200 0 true");
		server.runCommand("gamemode spectator @p");
		server.runCommand("tp @p " + cx + " " + (y + 30) + " " + cz + " 180 60");   // over the dome, so its chunks are loaded
		context.waitTicks(80);
		for (int bottom = -63; bottom <= 27; bottom += 16) {
			server.runCommand("fill " + (cx - reach) + " " + bottom + " " + (cz - reach) + " " + (cx + reach) + " " + Math.min(27, bottom + 15) + " " + (cz + reach) + " minecraft:stone");
		}
		server.runCommand("fill " + (cx - reach) + " 28 " + (cz - reach) + " " + (cx + reach) + " 29 " + (cz + reach) + " minecraft:dirt");
		server.runCommand("fill " + (cx - reach) + " 30 " + (cz - reach) + " " + (cx + reach) + " 30 " + (cz + reach) + " minecraft:grass_block");
		context.waitTicks(20);
		// the grid dome, drawn chunk by chunk as worldgen does (each placement draws the part of its cell's dome in its own chunk)
		for (int dx = -reach; dx <= reach; dx += 8) {
			for (int dz = -reach; dz <= reach; dz += 8) {
				server.runCommand("place feature wildcatter:salt_dome " + (cx + dx) + " 0 " + (cz + dz));
			}
		}
		final int fcx = cx, fcz = cz;
		int salt = server.computeOnServer(srv -> {
			int n = 0;
			for (int x = fcx - 10; x <= fcx + 10; x++) {
				for (int z = fcz - 10; z <= fcz + 10; z++) {
					for (int yy = -50; yy <= -30; yy++) {
						if (srv.overworld().getBlockState(new net.minecraft.core.BlockPos(x, yy, z)).is(dev.wildcatter.ore.OreBlocks.block(dev.wildcatter.ore.Ore.HALITE))) {
							n++;
						}
					}
				}
			}
			return n;
		});
		check(salt > 300, "the grid dome should stand in the ground: " + salt + " rock salt");
		String report = server.computeOnServer(srv -> dev.wildcatter.item.SeismographItem.report(srv.overworld(), fcx + 0.5, fcz + 0.5));
		check(report.contains("SALT DOME"), "the seismograph over the crest should read SALT DOME: " + report);
		Reservoir flank = dome.flanks().getFirst();
		// cut the south half away, slab by slab (a fill takes at most 32,768 blocks)
		for (int top = y + 8; top > -60; top -= 8) {
			server.runCommand("fill " + (cx - reach) + " " + Math.max(-60, top - 7) + " " + (cz + 1) + " " + (cx + reach) + " " + top + " " + (cz + reach) + " minecraft:air");
		}
		Filming film = context.computeOnClient(client -> Filming.start(client, "saltDomeFilm"));
		try {
			server.runCommand("tellraw @a {\"text\":\"" + report.replace("\"", "'") + "\"}");
			server.runCommand("tp @p " + cx + " " + (y + 24) + " " + (cz + reach + 22) + " 180 35");
			context.waitTicks(200);
			for (int step = 0; step <= 8; step++) {   // down the cutaway, from the mound to the salt's foot
				int cy = y + 10 - step * 9;
				server.runCommand("tp @p " + (cx + 3) + " " + cy + " " + (cz + reach + 6) + " 170 15");
				context.waitTicks(60);
			}
			int[] mid = flank.middle();
			server.runCommand("tellraw @a {\"text\":\"Flank field " + flank.key() + ": its top at y " + flank.topY() + ", " + String.format("%,d", flank.units()) + " units of "
					+ flank.kind().name().toLowerCase() + ", against the salt\"}");
			server.runCommand("tp @p " + mid[0] + " " + (flank.topY() + 6) + " " + (cz + reach + 4) + " 180 20");
			context.waitTicks(200);
			server.runCommand("tp @p " + cx + " " + (y + 40) + " " + (cz + 30) + " 180 50");
			context.waitTicks(160);
			context.takeScreenshot("wildcatter-salt-dome-field");
		} finally {
			if (film != null) {
				film.stop();
			}
			ReservoirData.testSeed = null;
			ReservoirData.testDomeBiome = null;
			server.runCommand("wildcatter rule saltDomeGrid192 false");
			server.runCommand("gamemode creative @p");
		}
		System.out.println("[wildcatter-test] saltDomeFilm ok: " + salt + " rock salt; " + report);
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
