package dev.wildcatter.gametest;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.FluidBedRoasterBlockEntity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigParts;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 17b's client scene (run alone with WILDCATTER_ONLY=nickelSmelter): the NeoForge scenes' smelter (the furnace transformer, the
 * six-in-line electric furnace, the fluid-bed roaster against its south end, the Peirce-Smith converter against its east side) and
 * refinery (the chlorine leach reactor, the purification circuit, the tankhouse and its rectifier) laid out round an origin (0, up, z0).
 * Screenshots of the smelter, the refinery and their readouts.
 */
final class NickelClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int up;
	private final int z0;

	NickelClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
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

	private void clear(int x1, int z1, int x2, int z2, int height) {
		for (int zz = z1; zz <= z2; zz += 8) {
			int top = Math.min(zz + 7, z2);
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y + height) + " " + (z0 + top) + " minecraft:air");
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y - 1) + " " + (z0 + top) + " minecraft:stone");
		}
	}

	private void place(Block block, int x, int z, Direction facing) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			level.setBlock(at(x, 0, z), state, 3);
			BigParts.grow(level, at(x, 0, z), state);
		});
	}

	private String read(int x, int dy, int z) {
		return get(level -> BigParts.entity(level, at(x, dy, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	private void show(int x, int dy, int z, String tp, String shot) {
		server.runCommand(tp);
		context.waitTicks(3);
		on(level -> {
			BlockEntity e = BigParts.entity(level, at(x, dy, z));
			if (e instanceof Inspectable i) {
				for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
					player.sendOverlayMessage(i.inspect());
				}
			}
		});
		context.waitTicks(10);
		context.takeScreenshot(shot);
		context.waitTicks(20);
	}

	private String tp(int x, int dy, int z, int yaw, int pitch) {
		return "tp @p " + x + " " + (up + dy) + " " + (z0 + z) + " " + yaw + " " + pitch;
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	// ---- the smelter and the refinery ----------------------------------------------------------------------------------------------
	void smelter() {
		Direction north = Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand(tp(30, 14, 10, 135, 30));
		context.waitTicks(20);
		clear(-6, -26, 46, 26, 14);
		place(WildcatterBlocks.FURNACE_TRANSFORMER, 0, -20, north);         // x 0-3, z -20..-16
		place(WildcatterBlocks.NICKEL_ELECTRIC_FURNACE, 4, -20, north);     // x 4-9, z -20..-9
		place(WildcatterBlocks.FLUID_BED_ROASTER, 4, -8, north);            // x 4-7, z -8..-5
		place(WildcatterBlocks.PEIRCE_SMITH_CONVERTER, 10, -22, north);     // x 10-14, z -22..-13
		on(level -> ((FluidBedRoasterBlockEntity) BigParts.entity(level, at(4, 0, -8))).testAddCalcine(2_000));
		// the refinery, south of it
		place(WildcatterBlocks.CHLORINE_LEACH_REACTOR, 23, 4, north);       // x 23-25, z 4-6
		place(WildcatterBlocks.NICKEL_TANKHOUSE, 20, 7, north);             // x 20-25, z 7-20
		place(WildcatterBlocks.NI_PURIFICATION, 26, 5, north);              // x 26-29, z 5-14
		place(WildcatterBlocks.RECTIFIER, 20, 21, north);                   // x 20-22, z 21-24
		// the alloy melt shop (17b2), east of the refinery
		place(WildcatterBlocks.INDUCTION_FURNACE, 34, 4, north);           // x 34-36, z 4-6
		place(WildcatterBlocks.AOD_CONVERTER, 34, 7, north);               // x 34-36, z 7-9
		place(WildcatterBlocks.INGOT_CASTER, 34, 10, north);               // x 34-35, z 10-15
		place(WildcatterBlocks.OXYGEN_GENERATOR, 31, 8, north);            // x 31-33, z 8-13
		context.waitTicks(60);
		String furnace = read(4, 0, -20);
		check(!furnace.contains("NOT BUILT"), "the furnace should find its transformer and roaster: " + furnace);
		check(read(23, 0, 4).contains("leach"), "the leach reactor should read: " + read(23, 0, 4));
		show(4, 0, -20, tp(22, 12, 2, 140, 28), "wildcatter-nickel-smelter");
		show(10, 0, -22, tp(20, 6, -26, 50, 20), "wildcatter-nickel-converter");
		show(4, 0, -8, tp(-4, 8, 0, -135, 25), "wildcatter-nickel-roaster");
		show(23, 0, 4, tp(36, 10, 26, 145, 30), "wildcatter-nickel-refinery");
		show(34, 0, 7, tp(42, 7, 14, 135, 30), "wildcatter-nickel-melt-shop");
		System.out.println("[wildcatter-test] nickelSmelter ok: " + furnace);
	}

	// ---- 19c: the fuel line's inspection parts (run alone with WILDCATTER_ONLY=fuelInspection) ---------------------------------------
	void fuelInspection() {
		Direction north = Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 600 0 true");
		server.runCommand(tp(12, 8, 30, 135, 30));
		context.waitTicks(20);
		clear(-6, -4, 20, 44, 10);
		place(WildcatterBlocks.GRANULATOR, 0, 0, north);                    // z 0-3
		place(WildcatterBlocks.SCRAP_OXIDIZER, 3, 0, north);                // x 3-4, z 0-3
		place(WildcatterBlocks.PELLET_GRINDER, 0, 6, north);                // z 6-9
		place(WildcatterBlocks.PELLET_INSPECTION, 0, 10, north);            // x 0-1, z 10-13
		place(WildcatterBlocks.ROD_LOADER, 0, 14, north);                   // z 14-23
		place(WildcatterBlocks.ROD_WELDER, 0, 24, north);                   // z 24-29
		place(WildcatterBlocks.ROD_INSPECTION, 0, 30, north);               // z 30-39
		context.waitTicks(40);
		String bench = read(0, 0, 10);
		check(bench.contains("Pellet inspection"), "the bench should read: " + bench);
		show(0, 0, 10, tp(6, 4, 15, 125, 25), "wildcatter-fuel-pellet-inspection");
		show(0, 0, 30, tp(7, 5, 42, 140, 30), "wildcatter-fuel-rod-inspection");
		show(3, 0, 0, tp(8, 4, -3, 45, 30), "wildcatter-fuel-scrap-oxidizer");
		System.out.println("[wildcatter-test] fuelInspection ok: " + bench);
	}
}
