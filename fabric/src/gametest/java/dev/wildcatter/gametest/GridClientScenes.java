package dev.wildcatter.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BigPartBlockEntity;
import dev.wildcatter.block.InsulatorBlock;
import dev.wildcatter.block.PoleLoad;
import dev.wildcatter.block.WoodPoleBlock;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.GuyStrandItem;
import dev.wildcatter.item.SpoolItem;
import dev.wildcatter.multiblock.BigParts;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tim's answers of 2026-11-10 (branch w-grid), for filming (run alone with WILDCATTER_ONLY=guyWire): a dead-end 40 ft pole on a span of
 * Linnet reads UNGUYED and leans its three steps; a guy to an anchor holds the pull and stands it plumb again (answer 16). Beside it, the
 * 600 mm slurry pipe and air header (answer 12). The NeoForge scene {@code guy_wire} checks the same numbers.
 */
final class GridClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int up;
	private final int z0;

	GridClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
		this.context = context;
		this.server = server;
		this.y = y;
		this.up = up;
		this.z0 = z0;
	}

	private BlockPos at(int x, int dy, int z) {
		return new BlockPos(x, up + dy, z0 + z);
	}

	private void on(Consumer<ServerLevel> work) {
		server.computeOnServer(srv -> {
			work.accept(srv.overworld());
			return null;
		});
	}

	private <T> T get(Function<ServerLevel, T> work) {
		return server.computeOnServer(srv -> work.apply(srv.overworld()));
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private void clear(int x1, int z1, int x2, int z2, int height) {
		for (int zz = z1; zz <= z2; zz += 8) {
			int top = Math.min(zz + 7, z2);
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y + height) + " " + (z0 + top) + " minecraft:air");
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y - 1) + " " + (z0 + top) + " minecraft:grass_block");
		}
	}

	private void put(String id, int x, int dy, int z) {
		on(level -> level.setBlock(at(x, dy, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3));
	}

	private void pole(int x, int z) {
		on(level -> {
			BlockState state = WildcatterBlocks.WOOD_POLE_40.defaultBlockState();
			level.setBlock(at(x, 0, z), state, 3);
			BigParts.grow(level, at(x, 0, z), state);
			InsulatorBlock insulator = (InsulatorBlock) WildcatterBlocks.INSULATOR_DIST;
			level.setBlock(at(x, 10, z), insulator.defaultBlockState().setValue(InsulatorBlock.MOUNT, insulator.mountAt(level, at(x, 10, z)))
					.setValue(dev.wildcatter.block.CableBlock.CONDUCTOR, Conductor.A), 3);
		});
	}

	private String reading(int x, int z) {
		return get(level -> {
			BlockPos foot = at(x, 0, z);
			BlockState state = level.getBlockState(foot);
			return PoleLoad.describe(level, foot, state, (WoodPoleBlock) state.getBlock(), (BigPartBlockEntity) level.getBlockEntity(foot));
		});
	}

	private int lean(int x, int z) {
		return get(level -> level.getBlockState(at(x, 0, z)).getValue(WoodPoleBlock.LEAN));
	}

	/** Says the reading on screen, as the Power Meter does, and takes a screenshot. */
	private void show(String text, String shot) {
		on(level -> {
			for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
				player.sendOverlayMessage(Component.literal(text));
			}
		});
		context.waitTicks(10);
		context.takeScreenshot(shot);
		context.waitTicks(50);
	}

	private String tp(int x, int dy, int z, int yaw, int pitch) {
		return "tp @p " + x + " " + (up + dy) + " " + (z0 + z) + " " + yaw + " " + pitch;
	}

	private static void log(String text) {
		System.out.println("[wildcatter-test] " + text);
	}

	void guyWire() {
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand(tp(14, 8, 22, 180, 10));
		context.waitTicks(20);
		clear(-16, -6, 40, 10, 24);
		pole(0, 0);
		pole(26, 0);
		on(level -> SpoolItem.connect(level, at(0, 10, 0), at(26, 10, 0), Voltage.DISTRIBUTION, false, 1024, ConductorSize.ACSR_LINNET));
		// beside the line: a 600 mm slurry pipe and a flotation air header, each a short run between two posts of liner
		for (int x = 4; x <= 22; x++) {
			put("slurry_pipe", x, 0, 6);
			put("air_header", x, 2, 6);
		}
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "guyWire"));
			server.runCommand(tp(6, 6, 30, 180, -4));
			context.waitTicks(40);
			String unguyed = reading(0, 0);
			show(unguyed, "wildcatter-guy-unguyed");
			// overloaded and unguyed, it leans a step every 4 s here (10 minutes in play)
			PoleLoad.testLeanSeconds = 4.0;
			server.runCommand(tp(2, 6, 24, 185, -6));
			for (int i = 0; i < 16 && lean(0, 0) < WoodPoleBlock.MAX_LEAN; i++) {
				context.waitTicks(20);
			}
			context.waitTicks(30);
			String leaning = reading(0, 0);
			show(leaning, "wildcatter-guy-leaning");
			check(leaning.contains("UNGUYED") && lean(0, 0) == WoodPoleBlock.MAX_LEAN, "the dead-end pole should read UNGUYED and lean: " + leaning);
			// the anchor and the guy, on the far side from the line
			put("guy_anchor", -9, 0, 0);
			GuyStrandItem.Result guy = get(level -> GuyStrandItem.connect(level, at(0, 9, 0), at(-9, 0, 0), 60));
			check(guy.ok(), "the guy should string: " + guy);
			for (int i = 0; i < 6 && lean(0, 0) > 0; i++) {
				context.waitTicks(20);
			}
			context.waitTicks(20);
			String guyed = reading(0, 0);
			show(guyed + " | " + guy.message(), "wildcatter-guy-guyed");
			check(guyed.contains("holding") && lean(0, 0) == 0, "the guy should stand the pole plumb: " + guyed);
			server.runCommand(tp(13, 4, 12, 180, 20));
			context.waitTicks(20);
			show("Slurry pipe (600 mm, rated 1 m3/s) below; air header (600 mm) above", "wildcatter-slurry-pipe-air-header");
			log("guyWire ok | " + unguyed + " | " + leaning + " | " + guyed);
		} finally {
			PoleLoad.testLeanSeconds = null;
			if (film != null) {
				film.stop();
			}
		}
	}
}
