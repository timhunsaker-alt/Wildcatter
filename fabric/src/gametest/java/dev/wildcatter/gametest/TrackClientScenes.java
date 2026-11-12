package dev.wildcatter.gametest;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client screenshots for track w-17c (2026-11-12): phase 17c2's Pidgeon furnace (docs/multiblocks/zirconium-extensions.md), and the
 * scenes 16c2 and 16d2 ask for. Laid out round an origin (0, up, z0); each run alone with WILDCATTER_ONLY.
 */
final class TrackClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int up;
	private final int z0;

	TrackClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
		this.context = context;
		this.server = server;
		this.y = y;
		this.up = up;
		this.z0 = z0;
	}

	BlockPos at(int x, int dy, int z) {
		return new BlockPos(x, up + dy, z0 + z);
	}

	void on(Consumer<ServerLevel> work) {
		server.computeOnServer(srv -> {
			work.accept(srv.overworld());
			return null;
		});
	}

	<T> T get(Function<ServerLevel, T> work) {
		return server.computeOnServer(srv -> work.apply(srv.overworld()));
	}

	static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	void clear(int x1, int z1, int x2, int z2, int height) {
		for (int zz = z1; zz <= z2; zz += 8) {
			int top = Math.min(zz + 7, z2);
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y + height) + " " + (z0 + top) + " minecraft:air");
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y - 1) + " " + (z0 + top) + " minecraft:stone");
		}
	}

	void place(Block block, int x, int dy, int z, Direction facing) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			level.setBlock(at(x, dy, z), state, 3);
			BigParts.grow(level, at(x, dy, z), state);
		});
	}

	void put(String id, int x, int dy, int z) {
		on(level -> level.setBlock(at(x, dy, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3));
	}

	void hopper(int x, int dy, int z, ItemStack... stacks) {
		on(level -> {
			level.setBlock(at(x, dy, z), Blocks.HOPPER.defaultBlockState(), 3);
			HopperBlockEntity h = (HopperBlockEntity) level.getBlockEntity(at(x, dy, z));
			for (int i = 0; i < stacks.length; i++) {
				h.setItem(i, stacks[i]);
			}
		});
	}

	void source(int x, int dy, int z, Product product, double rate) {
		on(level -> {
			level.setBlock(at(x, dy, z), TestGasSource.BLOCK.defaultBlockState(), 3);
			TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) level.getBlockEntity(at(x, dy, z));
			s.product = product;
			s.rate = rate;
		});
	}

	String read(int x, int dy, int z) {
		return get(level -> BigParts.entity(level, at(x, dy, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	void genset(int x, int z) {
		on(level -> GensetSkid.build(level, at(x, 0, z), Direction.NORTH, false));
	}

	void run(int x, int z) {
		on(level -> {
			if (level.getBlockEntity(GensetSkid.base(at(x, 0, z), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			((GensetControlPanelBlockEntity) level.getBlockEntity(at(x, 0, z))).setWantRun(true);
		});
	}

	/** The readout of the part at (x, dy, z) on the player's screen, then a screenshot. */
	void show(int x, int dy, int z, String tp, String shot) {
		server.runCommand(tp);
		context.waitTicks(3);
		on(level -> {
			if (BigParts.entity(level, at(x, dy, z)) instanceof Inspectable i) {
				for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
					player.sendOverlayMessage(i.inspect());
				}
			}
		});
		context.waitTicks(10);
		context.takeScreenshot(shot);
		context.waitTicks(20);
	}

	String tp(int x, int dy, int z, int yaw, int pitch) {
		return "tp @p " + x + " " + (up + dy) + " " + (z0 + z) + " " + yaw + " " + pitch;
	}

	static void log(String text) {
		System.out.println("[wildcatter-test] " + text);
	}

	// ---- pidgeon (17c2) -------------------------------------------------------------------------------------------------------------
	/**
	 * The Pidgeon furnace on a diesel set with pipeline gas from a stand-in source, dolime, ferrosilicon and fluorspar in the hopper on
	 * top, a hopper under it for the ingots and a magnesium cell beside it: it runs, and the screenshot shows it with its readout.
	 */
	void pidgeon() {
		Direction north = Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand(tp(14, 10, 18, 140, 30));
		context.waitTicks(20);
		clear(-12, -12, 20, 22, 14);
		place(WildcatterBlocks.PIDGEON_FURNACE, 0, 0, 0, north);          // x 0-3, z 0-9, box (-1, 0)
		place(WildcatterBlocks.MAGNESIUM_CELL, 8, 0, 0, north);           // x 8-10, z 0-5
		put("gas_line", 4, 0, 5);
		source(5, 0, 5, Product.PIPELINE_GAS, 0.2);
		hopper(1, 4, 5, new ItemStack(WildcatterItems.DOLIME_BAG, 4), new ItemStack(WildcatterItems.FERROSILICON_BAG, 1),
				new ItemStack(OreBlocks.MINERALS.get(Ore.FLUORITE), 1));
		hopper(1, -1, 5);
		hopper(9, 3, 2);
		genset(-6, -8);
		for (int x = -5; x <= -1; x++) {
			put("cable_industrial", x, 0, -8);
		}
		for (int z = -7; z <= 0; z++) {
			put("cable_industrial", -1, 0, z);
		}
		context.waitTicks(40);
		run(-6, -8);
		String state = "";
		for (int i = 0; i < 30 && !state.contains("reducing dolime"); i++) {
			context.waitTicks(20);
			state = read(0, 0, 0);
		}
		log("pidgeon: " + state + " | set " + read(-6, 0, -8));
		check(state.contains("reducing dolime"), "the Pidgeon furnace should run: " + state + " | set " + read(-6, 0, -8));
		show(0, 0, 0, tp(10, 6, -8, 40, 25), "wildcatter-pidgeon");   // from the north-east: the retorts' cold ends and condensers
		log("pidgeon ok");
	}

	// ---- cascadeCrash (16c2) --------------------------------------------------------------------------------------------------------
	/** A row of centrifuges on their header, the middle one crashed (its rotor broken up in its casing): the hall after a crash. */
	void cascadeCrash() {
		Direction north = Direction.NORTH;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand(tp(4, 4, 7, 160, 15));
		context.waitTicks(20);
		clear(-6, -6, 14, 12, 10);
		for (int x = 0; x <= 8; x++) {
			place(x == 4 ? WildcatterBlocks.CRASHED_CENTRIFUGE : WildcatterBlocks.CENTRIFUGE, x, 0, 0, north);
			put("gas_line", x, 6, 0);
		}
		on(level -> {
			if (level.getBlockEntity(at(4, 0, 0)) instanceof dev.wildcatter.block.CrashedCentrifugeBlockEntity wreck) {
				wreck.setReason("held at a critical speed (45 %) for 120 s");
			}
		});
		context.waitTicks(20);
		String wreck = read(4, 0, 0);
		check(wreck.contains("CRASHED"), "the crashed machine should read CRASHED: " + wreck);
		show(4, 0, 0, tp(4, 3, 6, 180, 10), "wildcatter-cascade-crash");
		log("cascadeCrash ok: " + wreck);
	}
}
