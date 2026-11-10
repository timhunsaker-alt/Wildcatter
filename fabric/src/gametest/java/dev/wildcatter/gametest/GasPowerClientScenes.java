package dev.wildcatter.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.AcidSuperheaterBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.MeterBuildingBlockEntity;
import dev.wildcatter.block.SalesGasLine;
import dev.wildcatter.block.SteamCycleBlockEntity;
import dev.wildcatter.energy.ConductorSize;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 14b-14d's client scenes, for filming (run one at a time with WILDCATTER_ONLY=salesGas, acidSteam or biogas). The same builds as
 * the NeoForge scenes in GasPowerScenes, laid out round an origin (0, up, z0), with screenshots of each readout.
 */
final class GasPowerClientScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private final int up;
	private final int z0;

	GasPowerClientScenes(ClientGameTestContext context, TestServerContext server, int y, int up, int z0) {
		this.context = context;
		this.server = server;
		this.y = y;
		this.up = up;
		this.z0 = z0;
	}

	// ---- helpers (x, z relative to the origin; dy above up) ---------------------------------------------------------------------------
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
		for (int zz = z1; zz <= z2; zz += 16) {   // /fill takes at most 32,768 blocks
			int top = Math.min(zz + 15, z2);
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y + height) + " " + (z0 + top) + " minecraft:air");
			server.runCommand("fill " + x1 + " " + (y - 1) + " " + (z0 + zz) + " " + x2 + " " + (y - 1) + " " + (z0 + top) + " minecraft:stone");
		}
	}

	private void place(Block block, int x, int dy, int z, Direction facing) {
		on(level -> {
			BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			level.setBlock(at(x, dy, z), state, 3);
			BigParts.grow(level, at(x, dy, z), state);
		});
	}

	private void place(Block block, int x, int z, Direction facing) {
		place(block, x, 0, z, facing);
	}

	private void put(String id, int x, int dy, int z) {
		on(level -> level.setBlock(at(x, dy, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3));
	}

	private void putFacing(String id, int x, int dy, int z, Direction facing) {
		on(level -> {
			BlockState state = BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState();
			if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
				state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
			}
			level.setBlock(at(x, dy, z), state, 3);
		});
	}

	private void pipe(int x, int dy, int z) {
		put("gas_line", x, dy, z);
	}

	private BlockEntity entity(ServerLevel level, int x, int dy, int z) {
		return BigParts.entity(level, at(x, dy, z));
	}

	private String read(int x, int dy, int z) {
		return get(level -> entity(level, x, dy, z) instanceof Inspectable i ? i.inspect().getString() : "(no readout)");
	}

	private String read(int x, int z) {
		return read(x, 0, z);
	}

	private void source(int x, int z, Product product, double rate) {
		on(level -> {
			level.setBlock(at(x, 0, z), TestGasSource.BLOCK.defaultBlockState(), 3);
			TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) level.getBlockEntity(at(x, 0, z));
			s.product = product;
			s.rate = rate;
		});
	}

	private void setProduct(int x, int z, Product product) {
		on(level -> ((TestGasSource.SourceEntity) level.getBlockEntity(at(x, 0, z))).product = product);
	}

	private void genset(int x, int z, boolean gas) {
		on(level -> GensetSkid.build(level, at(x, 0, z), Direction.NORTH, gas));
	}

	private GensetControlPanelBlockEntity panel(ServerLevel level, int x, int z) {
		return (GensetControlPanelBlockEntity) level.getBlockEntity(at(x, 0, z));
	}

	private void run(int x, int z, boolean run) {
		on(level -> {
			if (run && level.getBlockEntity(GensetSkid.base(at(x, 0, z), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			panel(level, x, z).setWantRun(run);
		});
	}

	private BlockPos inline(Block block, int x, int z, Direction d, boolean feedMain) {
		int across = block == WildcatterBlocks.UNIT_SUBSTATION ? 3 : 1;
		BlockPos from = new BlockPos(x, 0, z);
		BlockPos first = from.relative(d);
		Direction facing = feedMain ? d.getCounterClockWise() : d.getClockWise();
		BlockPos main = feedMain ? first : first.relative(d, across);
		place(block, main.getX(), main.getZ(), facing);
		return from.relative(d, across + 2);
	}

	private void show(int x, int dy, int z, String tp, String shot) {
		server.runCommand(tp);
		context.waitTicks(3);
		on(level -> {
			if (entity(level, x, dy, z) instanceof Inspectable i) {
				for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
					player.sendOverlayMessage(i.inspect());
				}
			}
		});
		context.waitTicks(10);
		context.takeScreenshot(shot);
		context.waitTicks(30);   // a moment to read it on film
	}

	private String tp(int x, int dy, int z, int yaw, int pitch) {
		return "tp @p " + x + " " + (up + dy) + " " + (z0 + z) + " " + yaw + " " + pitch;
	}

	private void log(String text) {
		System.out.println("[wildcatter-test] " + text);
	}

	// ---- salesGas -------------------------------------------------------------------------------------------------------------------------
	/**
	 * The sales line (14b): a receipt station on a residential service from a pole transformer takes pipeline gas and books it less 2 %;
	 * a delivery station 40 blocks east feeds a 1 MW gas set that starts on its gas; separator gas is refused (OFF SPEC: RICH); the
	 * receipt's power cut and 8 hours skipped: METER FAILED; the account drained: NO GAS ON THE LINE.
	 */
	void salesGas() {
		int far = 40;
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand(tp(14, 14, 22, 160, 35));
		context.waitTicks(20);
		clear(-24, -8, far + 18, 14, 10);
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "salesGas"));
			place(WildcatterBlocks.METER_BUILDING, 0, 0, Direction.NORTH);
			place(WildcatterBlocks.ULTRASONIC_METER_RUN, 3, 0, Direction.NORTH);
			place(WildcatterBlocks.FILTER_SEPARATOR, 4, 0, Direction.NORTH);
			place(WildcatterBlocks.PIPELINE_TAP, 4, 7, Direction.NORTH);
			source(4, -3, Product.PIPELINE_GAS, 1.0);
			pipe(4, 0, -2);
			pipe(4, 0, -1);
			genset(-18, 0, false);
			BlockPos hv = inline(WildcatterBlocks.PADMOUNT_480, -18, 0, Direction.EAST, true);
			for (int x = hv.getX(); x <= hv.getX() + 2; x++) {
				put("dist_wire_hot", x, 0, 0);
			}
			int px = hv.getX() + 3;
			put("pole", px, 0, -1);
			putFacing("pole_transformer", px, 0, 0, Direction.WEST);
			for (int x = px + 1; x <= -1; x++) {
				put("cable_lv", x, 0, 0);
			}
			place(WildcatterBlocks.METER_BUILDING, far, 0, Direction.NORTH);
			place(WildcatterBlocks.ULTRASONIC_METER_RUN, far + 3, 0, Direction.NORTH);
			place(WildcatterBlocks.REGULATOR_SKID, far + 4, 0, Direction.NORTH);
			place(WildcatterBlocks.PIPELINE_TAP, far + 4, 7, Direction.NORTH);
			pipe(far + 5, 0, 5);
			pipe(far + 5, 0, 6);
			pipe(far + 6, 0, 6);
			pipe(far + 6, 0, 7);
			genset(far + 10, 8, true);
			put("cable_industrial", far + 11, 0, 8);
			put("three_phase_heater", far + 12, 0, 8);
			on(level -> ((MeterBuildingBlockEntity) entity(level, far, 0, 0)).setMode(MeterBuildingBlockEntity.Mode.DELIVERY));
			context.waitTicks(30);
			show(0, 0, 0, tp(-4, 3, -4, -45, 20), "wildcatter-sales-receipt-unpowered");
			run(-18, 0, true);
			context.waitTicks(300);
			String receipt = read(0, 0);
			check(receipt.contains("RECEIPT: 1.") && !receipt.contains("NO POWER"), "the receipt station should take the source's gas on its own power: " + receipt);
			server.runCommand(tp(10, 10, 18, 155, 30));
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-sales-receipt");
			show(0, 0, 0, tp(-4, 3, -4, -45, 20), "wildcatter-sales-run-ticket");
			show(4, 0, 7, tp(7, 2, 10, 135, 20), "wildcatter-sales-tap");
			// the far gas set on the delivery's gas
			on(level -> panel(level, far + 10, 8).setWantRun(true));
			context.waitTicks(300);
			String set = read(far + 10, 8);
			check(set.contains("RUNNING") && set.contains("pipeline gas"), "the gas set should run on the delivery station's gas: " + set + " | " + read(far, 0));
			server.runCommand(tp(far + 16, 8, 16, 145, 25));
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-sales-delivery");
			show(far, 0, 0, tp(far - 4, 3, -4, -45, 20), "wildcatter-sales-delivery-ticket");
			// separator gas refused
			setProduct(4, -3, Product.GAS);
			context.waitTicks(40);
			String rich = read(0, 0);
			check(rich.contains("OFF SPEC: RICH"), "separator gas should be refused: " + rich);
			show(0, 0, 0, tp(-4, 3, -4, -45, 20), "wildcatter-sales-off-spec");
			setProduct(4, -3, Product.PIPELINE_GAS);
			// power cut, 8 hours on the battery skipped
			run(-18, 0, false);
			context.waitTicks(100);
			on(level -> ((MeterBuildingBlockEntity) entity(level, 0, 0, 0)).setBatteryForTest(0));
			context.waitTicks(30);
			String failed = read(0, 0);
			check(failed.contains("METER FAILED"), "with no power and a flat battery the station should shut in: " + failed);
			show(0, 0, 0, tp(-4, 3, -4, -45, 20), "wildcatter-sales-meter-failed");
			// the account drained
			on(level -> SalesGasLine.get(level).drainForTest());
			context.waitTicks(30);
			String none = read(far, 0);
			check(none.contains("NO GAS ON THE LINE"), "with the account empty the delivery station should say so: " + none);
			show(far, 0, 0, tp(far - 4, 3, -4, -45, 20), "wildcatter-sales-no-gas");
			log("salesGas ok: " + receipt + " | " + set + " | " + rich + " | " + failed + " | " + none);
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	// ---- acidSteam ------------------------------------------------------------------------------------------------------------------------
	/**
	 * The acid plant's steam (14c): the acid plant on two diesel sets, its superheater against the converter, the 5 MW turbine on its
	 * steam synchronizing through the unit auxiliary transformer onto the plant's 4.16 kV bus; the sets stopped and the plant carries
	 * itself; the superheater broken: NO STEAM.
	 */
	void acidSteam() {
		server.runCommand("effect give @p minecraft:slow_falling 1200 0 true");
		server.runCommand(tp(20, 24, 34, 180, 35));
		context.waitTicks(20);
		clear(-2, -12, 40, 19, 24);
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "acidSteam"));
			Direction west = Direction.WEST, north = Direction.NORTH;
			place(WildcatterBlocks.DRYING_TOWER, 0, 0, west);
			place(WildcatterBlocks.MAIN_BLOWER, 3, 0, west);
			place(WildcatterBlocks.SULFUR_FURNACE, 8, 0, west);
			place(WildcatterBlocks.WASTE_HEAT_BOILER, 18, 0, west);
			place(WildcatterBlocks.SULFUR_MELTER, 18, -3, west);
			place(WildcatterBlocks.CONVERTER, 27, 0, west);
			place(WildcatterBlocks.INTERPASS_ABSORBER, 32, 0, west);
			place(WildcatterBlocks.FINAL_ABSORBER, 27, -5, west);
			place(WildcatterBlocks.STOCK_TANK, 31, -6, west);
			place(WildcatterBlocks.LPG_BULLET, 6, -8, west);
			for (int dy = 0; dy <= 3; dy++) {
				pipe(26, dy, -5);
			}
			for (int x = 8; x <= 25; x++) {
				pipe(x, 3, -5);
			}
			pipe(8, 3, -4);
			for (int dy = 3; dy >= 0; dy--) {
				pipe(8, dy, -3);
			}
			pipe(26, 0, -1);
			pipe(30, 0, -6);
			for (int x = 17; x <= 25; x++) {
				pipe(x, 0, -7);
			}
			place(WildcatterBlocks.ACID_SUPERHEATER, 27, 1, north);
			place(WildcatterBlocks.STEAM_TURBINE_5, 30, 1, north);
			place(WildcatterBlocks.ST_GENERATOR, 33, 1, north);
			place(WildcatterBlocks.GEN_BREAKER, 33, 5, north);
			place(WildcatterBlocks.AIR_COOLED_CONDENSER, 27, 7, north);
			place(WildcatterBlocks.HRSG_FEED_PUMP, 22, 1, north);
			place(WildcatterBlocks.UNIT_AUX_TRANSFORMER, 36, 10, north);
			pipe(25, 4, 0);
			pipe(25, 4, 1);
			for (int dy = 4; dy >= 0; dy--) {
				pipe(26, dy, 1);
			}
			for (int dy = 0; dy <= 4; dy++) {
				put("bus_duct_gen", 33, dy, 7);
			}
			for (int x = 34; x <= 36; x++) {
				put("bus_duct_gen", x, 4, 7);
			}
			for (int z = 11; z <= 13; z++) {
				put("cable_mv", 36, 0, z);
			}
			for (int x = 3; x <= 35; x++) {
				put("cable_mv", x, 0, 13);
			}
			for (int z = 1; z <= 12; z++) {
				put("cable_mv", 3, 0, z);
			}
			int[] sets = {14, 22};
			for (int x : sets) {
				genset(x, 16, false);
				put("busway_industrial", x + 1, 0, 16);
			}
			for (int x = 15; x <= 24; x++) {
				put("busway_industrial", x, 0, 15);
			}
			BlockPos mv = inline(WildcatterBlocks.UNIT_SUBSTATION, 24, 15, Direction.EAST, false);
			put("cable_mv", mv.getX(), 0, 15);
			put("cable_mv", mv.getX(), 0, 14);
			int[][] lv = {{24, 0, 14}, {24, 1, 14}, {24, 1, 13}, {24, 1, 12}, {24, 0, 12}, {24, 0, 11}, {24, 0, 10}, {24, 0, 9}, {24, 0, 8}, {24, 0, 7},
					{25, 0, 7}, {26, 0, 7}, {24, 0, 6}, {24, 0, 5}, {23, 0, 5}, {22, 0, 5}, {21, 0, 5}, {21, 0, 4}, {21, 0, 3}, {21, 0, 2}, {21, 0, 1}};
			for (int[] c : lv) {
				put("cable_industrial", c[0], c[1], c[2]);
			}
			on(level -> {
				for (int[] c : lv) {
					if (level.getBlockEntity(at(c[0], c[1], c[2])) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
						cable.setSize(ConductorSize.KCMIL_500);
					}
				}
				((dev.wildcatter.block.StorageTankBlockEntity) entity(level, 6, 0, -8)).testFill(Product.LPG, 20_000);
				((dev.wildcatter.block.SulfurMelterBlockEntity) entity(level, 18, 0, -3)).slots().set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.SULFUR), 64));
			});
			Runnable water = () -> on(level -> {
				for (int[] w : new int[][] {{18, 0}, {27, -5}}) {
					if (entity(level, w[0], 0, w[1]) instanceof dev.wildcatter.block.WaterUser user) {
						user.addWater(100_000);
					}
				}
			});
			water.run();
			context.waitTicks(30);
			for (int x : sets) {
				run(x, 16, true);
			}
			boolean steam = false;
			for (int i = 0; i < 40 && !steam; i++) {
				context.waitTicks(50);
				water.run();
				steam = get(level -> entity(level, 27, 0, 1) instanceof AcidSuperheaterBlockEntity sh && sh.steamKgs() > 1.5 && sh.superheated());
			}
			check(steam, "the superheater should pass about 2 kg/s of superheated steam: " + read(27, 1) + " | boiler " + read(18, 0) + " | furnace " + read(8, 0));
			show(27, 0, 1, tp(24, 3, 5, -135, 20), "wildcatter-acid-superheater");
			on(level -> ((SteamCycleBlockEntity) entity(level, 30, 0, 1)).skipRunupForTest());
			String turbine = "";
			for (int i = 0; i < 40 && !turbine.contains("breaker CLOSED"); i++) {
				context.waitTicks(40);
				water.run();
				turbine = read(30, 1);
			}
			check(turbine.contains("breaker CLOSED"), "the turbine should synchronize onto the plant's bus: " + turbine);
			server.runCommand(tp(44, 16, 26, 135, 30));
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-acid-steam");
			show(30, 0, 1, tp(31, 4, 10, 180, 30), "wildcatter-acid-steam-turbine");
			show(18, 0, 0, tp(22, 6, 6, 160, 35), "wildcatter-acid-steam-boiler");
			for (int x : sets) {
				run(x, 16, false);
			}
			for (int i = 0; i < 6; i++) {
				context.waitTicks(50);
				water.run();
			}
			String alone = read(30, 1);
			check(alone.contains("breaker CLOSED") && read(8, 0).contains("burning"), "with the sets stopped the turbine should carry the plant: " + alone + " | " + read(8, 0));
			show(30, 0, 1, tp(31, 4, 10, 180, 30), "wildcatter-acid-steam-islanded");
			show(3, 0, 0, tp(2, 4, 6, 200, 25), "wildcatter-acid-steam-blower");
			for (int x : sets) {
				run(x, 16, true);
			}
			for (int i = 0; i < 8; i++) {
				context.waitTicks(50);
				water.run();
			}
			on(level -> {
				for (int x = 27; x <= 29; x++) {
					for (int z = 1; z <= 3; z++) {
						for (int dy = 0; dy <= 5; dy++) {
							level.setBlock(at(x, dy, z), Blocks.AIR.defaultBlockState(), 3);
						}
					}
				}
			});
			for (int i = 0; i < 6; i++) {
				context.waitTicks(50);
				water.run();
			}
			String none = read(30, 1);
			check(none.contains("NO STEAM") && read(27, 0).contains("double absorption"), "without the superheater: NO STEAM, and acid on the sets: " + none + " | " + read(27, 0));
			show(30, 0, 1, tp(31, 4, 10, 180, 30), "wildcatter-acid-steam-no-steam");
			log("acidSteam ok: " + turbine + " | " + alone + " | " + none);
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}

	// ---- biogas ---------------------------------------------------------------------------------------------------------------------------
	/**
	 * Biogas (14d): a stand-in digester's biogas on steel pipe to a 1 MW gas set carrying a 1 MW load bank: it holds 850 kW and reads
	 * BIOGAS: 850 kW MAX; a firebox under a boiler burns it; the steam plant's burner on biogas reads NEEDS PIPELINE GAS.
	 */
	void biogas() {
		server.runCommand("effect give @p minecraft:slow_falling 900 0 true");
		server.runCommand(tp(20, 14, 22, 170, 35));
		context.waitTicks(20);
		clear(-4, -8, 48, 12, 12);
		Filming film = null;
		try {
			film = context.computeOnClient(client -> Filming.start(client, "biogas"));
			source(3, -3, Product.BIOGAS, 1.0);
			pipe(3, 0, -2);
			pipe(3, 0, -1);
			source(0, -4, Product.BIOGAS, 1.0);
			pipe(0, 0, -3);
			on(level -> {
				level.setBlock(at(0, 0, -2), WildcatterBlocks.FIREBOX.defaultBlockState(), 3);
				level.setBlock(at(0, 1, -2), WildcatterBlocks.BOILER.defaultBlockState(), 3);
				if (level.getBlockEntity(at(0, 1, -2)) instanceof dev.wildcatter.block.BoilerBlockEntity boiler) {
					boiler.addWater(4000);
				}
			});
			genset(8, 0, true);
			BlockPos mv = inline(WildcatterBlocks.UNIT_SUBSTATION, 8, 0, Direction.EAST, false);
			for (int z = 0; z <= 5; z++) {
				put("cable_mv", mv.getX(), 0, z);
			}
			for (int x = 14; x <= 16; x++) {
				put("cable_mv", x, 0, 5);
			}
			place(WildcatterBlocks.UNIT_AUX_TRANSFORMER, 16, 4, Direction.NORTH);
			place(WildcatterBlocks.LOAD_BANK, 20, 2, Direction.NORTH);
			for (int x = 16; x <= 19; x++) {
				put("bus_duct_gen", x, 4, 1);
			}
			for (int dy = 3; dy >= 0; dy--) {
				put("bus_duct_gen", 19, dy, 1);
			}
			for (int x = 20; x <= 22; x++) {
				put("bus_duct_gen", x, 0, 1);
			}
			on(level -> dev.wildcatter.block.SteamPlantLayout.layout().build(level, at(40, 0, 0), Direction.NORTH, 0));
			source(37, -3, Product.BIOGAS, 1.0);
			pipe(38, 0, -3);
			context.waitTicks(30);
			boolean lit = false;
			on(level -> ((dev.wildcatter.block.LoadBankBlockEntity) entity(level, 20, 0, 2)).setSteps(1));   // the set closes onto its load
			on(level -> panel(level, 8, 0).setWantRun(true));
			on(level -> {
				dev.wildcatter.block.TurbineGeneratorBlockEntity plant = (dev.wildcatter.block.TurbineGeneratorBlockEntity) level.getBlockEntity(at(40, 0, 0));
				plant.setWaterForTest(Tuning.DRUM_WATER_KG * 0.8, 1000);
				plant.setWantRun(true);
			});
			// the firebox burns the biogas at its one heat rate, 0.36 MW (Tim, 2026-11-10; phase14-gas-power 20): 1/64 m3 a second, so it
			// stays lit on the digester's 1 m3/s
			for (int i = 0; i < 300; i++) {
				context.waitTick();
				lit |= get(level -> level.getBlockState(at(0, 0, -2)).getValue(dev.wildcatter.block.FireboxBlock.LIT));
			}
			double fireGas = get(level -> ((TestGasSource.SourceEntity) level.getBlockEntity(at(0, 0, -4))).taken);
			log("biogas firebox: lit " + lit + ", " + fireGas + " m3 taken");
			check(lit && fireGas > 0.05 && fireGas < 2, "the firebox should stay lit on the biogas at 0.36 MW: lit " + lit + ", " + fireGas + " m3 taken");
			show(0, 0, -2, tp(-3, 2, -5, -45, 20), "wildcatter-biogas-firebox");
			String set = read(8, 0);
			check(set.contains("RUNNING") && set.contains("BIOGAS: 850 kW MAX"), "the gas set should run on biogas: " + set);
			context.waitTicks(400);
			String held = read(8, 0);
			double watts = get(level -> panel(level, 8, 0).watts());
			check(watts > 800_000 && watts < 860_000 && held.contains("BIOGAS: 850 kW MAX"), "the set should hold about 850 kW against the 1 MW bank: " + held + " | " + read(20, 2));
			server.runCommand(tp(12, 10, 16, 160, 30));
			context.waitTicks(5);
			context.takeScreenshot("wildcatter-biogas");
			show(8, 0, 0, tp(5, 3, 4, -150, 15), "wildcatter-biogas-set");
			show(20, 0, 2, tp(24, 3, 10, 150, 20), "wildcatter-biogas-load-bank");
			show(38, 0, -3, tp(35, 3, -6, -45, 20), "wildcatter-biogas-pipe");
			String burner = read(40, 0);
			check(burner.contains("NEEDS PIPELINE GAS"), "the steam plant's burner should refuse biogas: " + burner);
			show(40, 0, 0, tp(36, 3, 5, -135, 20), "wildcatter-biogas-burner-refuses");
			log("biogas ok: " + held + " | " + burner);
		} finally {
			if (film != null) {
				film.stop();
			}
		}
	}
}
