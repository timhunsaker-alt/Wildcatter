package dev.wildcatter.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigParts;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 20's client screenshots (run alone with WILDCATTER_ONLY=chlorAlkali, ...): each plant laid out as its NeoForge scene lays it
 * out, round an origin (0, up, z0), and photographed. The working plants are the NeoForge scenes' to check; these show the parts.
 */
final class ChemicalClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int up;
	private final int z0;

	ChemicalClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
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

	private void clear(int x1, int z1, int x2, int z2, int height) {
		for (int zz = z1; zz <= z2; zz += 8) {
			int top = Math.min(zz + 7, z2);
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y + height) + " " + (z0 + top) + " minecraft:air");
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y - 1) + " " + (z0 + top) + " minecraft:stone");
		}
	}

	private void place(Block block, int x, int z) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
			level.setBlock(at(x, 0, z), state, 3);
			BigParts.grow(level, at(x, 0, z), state);
		});
	}

	private void put(String id, int x, int dy, int z) {
		on(level -> level.setBlock(at(x, dy, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3));
	}

	/** A straight run of {@code id} at the ground between the corners, both ends included. */
	private void run(String id, int[]... corners) {
		for (int i = 0; i + 1 < corners.length; i++) {
			int[] a = corners[i];
			int[] b = corners[i + 1];
			int dx = Integer.signum(b[0] - a[0]);
			int dz = Integer.signum(b[1] - a[1]);
			for (int x = a[0], z = a[1];; x += dx, z += dz) {
				put(id, x, 0, z);
				if (x == b[0] && z == b[1]) {
					break;
				}
			}
		}
		if (corners.length == 1) {
			put(id, corners[0][0], 0, corners[0][1]);
		}
	}

	private void shot(int x, int dy, int z, int yaw, int pitch, String name) {
		server.runCommand("tp @p " + x + " " + (up + dy) + " " + (z0 + z) + " " + yaw + " " + pitch);
		context.waitTicks(40);
		context.takeScreenshot(name);
		context.waitTicks(10);
	}

	// ---- chlorAlkali: the chlor-alkali plant, as the NeoForge scene chlor_alkali lays it out -------------------------------------------
	void chlorAlkali() {
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 24 " + (up + 20) + " " + (z0 - 10) + " 130 35");
		context.waitTicks(20);
		clear(-12, -10, 24, 38, 14);
		place(WildcatterBlocks.BRINE_TREATMENT, 0, 0);
		place(WildcatterBlocks.BRINE_SATURATOR, -4, 0);
		place(WildcatterBlocks.MEMBRANE_CELL_ROOM, 4, 0);
		place(WildcatterBlocks.RECTIFIER_MV, 4, -4);
		place(WildcatterBlocks.CHLORINE_TRAIN, 0, 18);
		place(WildcatterBlocks.CHLORINE_FILLER, 0, 28);
		place(WildcatterBlocks.CAUSTIC_EVAPORATOR, 11, 18);
		place(WildcatterBlocks.STOCK_TANK, 11, 23);
		place(WildcatterBlocks.HCL_SYNTHESIS_UNIT, 6, 30);
		run("gas_line", new int[] {1, 8});
		run("gas_line", new int[] {4, 16}, new int[] {4, 17}, new int[] {3, 17});
		run("gas_line", new int[] {4, 19});
		run("gas_line", new int[] {6, 16}, new int[] {6, 29});
		run("gas_line", new int[] {3, 28}, new int[] {3, 33}, new int[] {8, 33}, new int[] {8, 31});
		run("gas_line", new int[] {8, 16}, new int[] {8, 17}, new int[] {11, 17});
		run("gas_line", new int[] {12, 21}, new int[] {12, 22});
		run("cable_dist", new int[] {3, -10}, new int[] {3, -4});
		context.waitTicks(40);
		shot(22, 16, -10, 40, 30, "wildcatter-chlor-alkali-plant");
		shot(17, 5, 7, 90, 15, "wildcatter-chlor-alkali-cell-room");
		shot(-9, 6, 23, -90, 20, "wildcatter-chlor-alkali-train");
		System.out.println("[wildcatter-test] chlorAlkali ok");
	}

	// ---- tiPigment: the titanium pigment plant, as the NeoForge scene ti_pigment lays it out ----------------------------------------
	void tiPigment() {
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 22 " + (up + 18) + " " + (z0 - 12) + " 40 30");
		context.waitTicks(20);
		clear(-6, -12, 20, 26, 16);
		place(WildcatterBlocks.TI_CHLORINATOR, 0, 0);
		place(WildcatterBlocks.TICL4_PURIFICATION, 4, 0);
		place(WildcatterBlocks.TICL4_OXIDISER, 7, 0);
		place(WildcatterBlocks.PIGMENT_FINISHING, 7, 10);
		place(WildcatterBlocks.BAGGING_STATION, 8, 20);
		place(WildcatterBlocks.VPSA_OXYGEN, 7, -7);
		run("gas_line", new int[] {8, -1});
		run("gas_line", new int[] {10, 1});
		run("gas_line", new int[] {6, 9}, new int[] {6, 4}, new int[] {2, 4});
		run("gas_line", new int[] {11, 10});
		context.waitTicks(40);
		shot(20, 14, -10, 40, 30, "wildcatter-ti-pigment-plant");
		shot(-5, 6, 8, -130, 15, "wildcatter-ti-pigment-chlorinator");
		System.out.println("[wildcatter-test] tiPigment ok");
	}

	// ---- lithium: the lithium hydroxide plant, as the NeoForge scene lithium lays it out ----------------------------------------------
	void lithium() {
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand("tp @p 16 " + (up + 14) + " " + (z0 - 10) + " 40 30");
		context.waitTicks(20);
		clear(-10, -8, 18, 20, 12);
		place(WildcatterBlocks.LITHIUM_LEACH, 0, 0);
		place(WildcatterBlocks.ACID_ROAST_KILN, -3, 0);
		place(WildcatterBlocks.DECREPITATION_KILN, -6, 0);
		place(WildcatterBlocks.LIOH_CRYSTALLIZER, 4, 0);
		on(level -> {
			for (int[] st : new int[][] {{6, -1}, {8, 1}}) {
				BlockState state = WildcatterBlocks.BAGGING_STATION.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
				level.setBlock(at(st[0], 0, st[1]), state, 3);
				BigParts.grow(level, at(st[0], 0, st[1]), state);
			}
		});
		place(WildcatterBlocks.LITHIUM_CARBONATOR, 12, 6);
		run("gas_line", new int[] {7, 3});
		run("gas_line", new int[] {5, 4});
		run("gas_line", new int[] {3, 6});
		run("gas_line", new int[] {0, 7});
		run("gas_line", new int[] {-3, 9}, new int[] {-3, 8}, new int[] {-2, 8});
		context.waitTicks(40);
		shot(14, 12, -8, 40, 30, "wildcatter-lithium-plant");
		shot(-12, 5, 14, -140, 20, "wildcatter-lithium-kilns");
		System.out.println("[wildcatter-test] lithium ok");
	}
}
