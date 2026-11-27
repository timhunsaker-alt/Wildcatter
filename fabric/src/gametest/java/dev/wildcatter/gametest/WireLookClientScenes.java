package dev.wildcatter.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.AttachedGear;
import dev.wildcatter.block.BreakerBlock;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.WireBlockEntity;
import dev.wildcatter.energy.ConductorSize;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wires, cables and pipes that read as connected (2026-11-27), for a look (run alone with WILDCATTER_ONLY=wireLook; the shots go to
 * $WILDCATTER_SHOTS, else run/screenshots): a turbine and a lamp on single-phase cable through a breaker; wire and cable of each
 * gauge side by side, each landing on a lamp; a pipe run into a boiler and a turbine; a receptacle with its plug.
 */
final class WireLookClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int up;
	private final int z0;
	private final java.nio.file.Path out;

	WireLookClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
		this.context = context;
		this.server = server;
		this.up = up;
		this.z0 = z0;
		String dir = System.getenv("WILDCATTER_SHOTS");
		this.out = dir == null || dir.isBlank() ? null : java.nio.file.Path.of(dir);
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

	private void set(int x, int dy, int z, BlockState state) {
		on(level -> level.setBlock(at(x, dy, z), state, 3));
	}

	private BlockState block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState();
	}

	private void put(String id, int x, int dy, int z) {
		set(x, dy, z, block(id));
	}

	private void sized(String id, ConductorSize size, int x, int dy, int z) {
		on(level -> {
			level.setBlock(at(x, dy, z), block(id), 3);
			if (level.getBlockEntity(at(x, dy, z)) instanceof WireBlockEntity wire) {
				wire.setSize(size);
			} else if (level.getBlockEntity(at(x, dy, z)) instanceof MultiCableBlockEntity cable) {
				cable.setSize(size);
			}
		});
	}

	/** From (x, dy, z) (the eyes there), looking at (tx, tdy, tz); positions in the scene's cells, their corners. */
	private void shot(String name, double x, double dy, double z, double tx, double tdy, double tz) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", x, up + dy - 1.62, z0 + z, tx, up + tdy - 1.62, z0 + tz));   // facing aims from the feet: the same line, an eye height lower
		context.waitTicks(40);
		TestScreenshotOptions options = TestScreenshotOptions.of(name).withSize(1600, 900).disableCounterPrefix();
		context.takeScreenshot(out == null ? options : options.withDestinationDir(out));
	}

	void run() {
		server.runCommand("time set noon");
		server.runCommand("gamerule advance_time false");
		server.runCommand("effect give @p minecraft:night_vision 900 0 true");
		server.runCommand("tp @p 6 " + (up + 3) + " " + (z0 + 4));
		context.waitTicks(20);
		for (int z = -4; z <= 34; z += 6) {
			server.runCommand("fill -6 " + (up - 1) + " " + (z0 + z) + " 18 " + (up + 8) + " " + (z0 + z + 5) + " minecraft:air");
			server.runCommand("fill -6 " + (up - 1) + " " + (z0 + z) + " 18 " + (up - 1) + " " + (z0 + z + 5) + " minecraft:smooth_stone");
		}
		server.runCommand("gamerule send_command_feedback false");
		server.runCommand("gamemode spectator @p");   // a free camera: no hand, no hotbar, and it can stand close
		lamp();
		gauges();
		pipes();
		plug();
		server.runCommand("gamemode creative @p");
	}

	/** A campfire boiler, its turbine, single-phase cable through a breaker to a lamp. */
	private void lamp() {
		set(0, 0, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		put("boiler", 0, 1, 0);
		put("steam_turbine", 1, 1, 0);
		on(level -> ((dev.wildcatter.block.BoilerBlockEntity) level.getBlockEntity(at(0, 1, 0))).addWater(4000));
		put("single_phase_cable", 2, 1, 0);
		set(3, 1, 0, WildcatterBlocks.BREAKER.defaultBlockState().setValue(BreakerBlock.FACING, Direction.EAST));
		put("single_phase_cable", 4, 1, 0);
		put("single_phase_cable", 5, 1, 0);
		context.waitTicks(10);
		// set by commands, each piece still reaches back to the one before it (onPlace's neighbour update)
		boolean reached = server.computeOnServer(srv -> srv.overworld().getBlockState(at(4, 1, 0)).getValue(dev.wildcatter.block.WireShape.ARMS.get(Direction.EAST)));
		if (!reached) {
			throw new AssertionError("a cable set beside another should give the older one its arm toward it");
		}
		put("single_phase_cable", 5, 0, 0);
		set(6, 0, 0, WildcatterBlocks.ELECTRIC_LAMP.defaultBlockState().setValue(AttachedGear.ATTACH, Direction.DOWN));
		context.waitTicks(100);
		boolean lit = server.computeOnServer(srv -> srv.overworld().getBlockState(at(6, 0, 0)).getValue(dev.wildcatter.block.LoadBlock.LIT));
		if (!lit) {
			throw new AssertionError("the lamp should light on single-phase cable through the breaker");
		}
		System.out.println("[wire-look] lamp lit on single-phase cable");
		shot("wire-look-turbine-lamp", 3.5, 3.2, 4.5, 3.5, 1.0, 0.5);
		shot("wire-look-turbine-lamp-close", 3.3, 2.3, 1.9, 2.0, 1.45, 0.5);
	}

	/** Each gauge of wire (12, 6, 4/0 AWG, 500 kcmil), then of three-phase and single-phase cable, every run landing on a lamp. */
	private void gauges() {
		int z = 6;
		ConductorSize[] sizes = {ConductorSize.AWG_12, ConductorSize.AWG_6, ConductorSize.AWG_4_0, ConductorSize.KCMIL_500};
		for (int i = 0; i < 4; i++) {
			for (int x = 0; x < 4; x++) {
				sized("wire_hot", sizes[i], x, 0, z + 2 * i);
				sized("cable_lv", sizes[i], x + 6, 0, z + 2 * i);
				sized("single_phase_cable", sizes[i], x + 12, 0, z + 2 * i);
			}
			set(4, 0, z + 2 * i, WildcatterBlocks.ELECTRIC_LAMP.defaultBlockState().setValue(AttachedGear.ATTACH, Direction.DOWN));
			set(10, 0, z + 2 * i, WildcatterBlocks.ELECTRIC_LAMP.defaultBlockState().setValue(AttachedGear.ATTACH, Direction.DOWN));
			set(16, 0, z + 2 * i, WildcatterBlocks.ELECTRIC_LAMP.defaultBlockState().setValue(AttachedGear.ATTACH, Direction.DOWN));
		}
		context.waitTicks(20);
		shot("wire-look-gauges-wire", 2.5, 2.2, z + 9.5, 2.5, 0.3, z + 3.5);
		shot("wire-look-gauges-cable", 8.5, 2.2, z + 9.5, 8.5, 0.3, z + 3.5);
		shot("wire-look-gauges-single", 14.5, 2.2, z + 9.5, 14.5, 0.3, z + 3.5);
	}

	/** A water pipe from the boiler's side, round an elbow, into the turbine's side; and a steel gas line into a firebox. */
	private void pipes() {
		int z = 16;
		put("boiler", 0, 0, z);
		put("steam_turbine", 3, 0, z);
		put("pipe", 0, 0, z + 1);
		put("pipe", 0, 0, z + 2);
		put("pipe", 1, 0, z + 2);
		put("pipe", 2, 0, z + 2);
		put("pipe", 3, 0, z + 2);
		put("pipe", 3, 0, z + 1);
		put("firebox", 5, 0, z);
		for (int k = 1; k <= 3; k++) {
			put("gas_line", 5, 0, z + k);
		}
		put("gas_line", 6, 0, z + 3);
		put("gas_line", 7, 0, z + 3);
		context.waitTicks(20);
		shot("wire-look-pipes", 2.0, 2.0, z + 5.0, 2.0, 0.4, z + 1.5);
		shot("wire-look-gas-line", 7.0, 2.0, z + 6.0, 6.0, 0.4, z + 2.0);
	}

	/** A receptacle on a wall, fed by wire, and its plug adapter set against it. */
	private void plug() {
		int z = 26;
		for (int x = 0; x <= 4; x++) {
			for (int dy = 0; dy <= 2; dy++) {
				set(x, dy, z - 1, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		set(2, 0, z, block("receptacle_nema_5_15").setValue(AttachedGear.ATTACH, Direction.NORTH));
		put("single_phase_cable", 2, 2, z);   // single-phase cable down the wall into the box's top
		put("single_phase_cable", 2, 1, z);
		set(2, 0, z + 1, block("plug_nema_5_15").setValue(AttachedGear.ATTACH, Direction.NORTH));
		context.waitTicks(20);
		shot("wire-look-plug", 4.0, 2.0, z + 2.8, 2.5, 0.6, z + 0.2);
		shot("wire-look-plug-close", 3.4, 0.9, z + 1.5, 2.5, 0.4, z + 0.1);
	}
}
