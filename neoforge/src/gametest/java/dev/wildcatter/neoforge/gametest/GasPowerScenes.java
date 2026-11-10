package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.AcidSuperheaterBlockEntity;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.ConverterBlockEntity;
import dev.wildcatter.block.FireboxBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.LoadBankBlockEntity;
import dev.wildcatter.block.MeterBuildingBlockEntity;
import dev.wildcatter.block.SalesGasLine;
import dev.wildcatter.block.SteamCycleBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.SulfurMelterBlockEntity;
import dev.wildcatter.block.TurbineGeneratorBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Biogas;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SalesGas;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 14b-14d (docs/multiblocks/sales-gas.md, acid-plant-steam.md, docs/biogas.md), in three scenes as the specs list them. Parts at
 * true size at y 2, steel pipe and cable laid as a player would, gensets started by hook.
 * <ul>
 * <li>{@code sales_gas}: a receipt station on a residential service from a pole transformer takes pipeline gas from a stand-in source
 * and books it, less 2 %; a delivery station 200 blocks away feeds a 1 MW gas set that starts and carries a heater; separator gas,
 * wet gas and biogas are refused; the receipt's power cut and its battery run down: METER FAILED; the account drained: NO GAS ON THE
 * LINE and the set stops on fuel.</li>
 * <li>{@code acid_steam}: the acid plant on two diesel sets; its superheater, the 5 MW turbine, generator, condenser and feed pump; the
 * turbine synchronizes through the unit auxiliary transformer onto the plant's 4.16 kV bus with steam for 1.4-1.8 MW; the sets are
 * stopped and the plant carries itself; the converter held: LOW STEAM TEMPERATURE; the superheater broken: NO STEAM, and the acid
 * plant keeps making acid on the sets.</li>
 * <li>{@code biogas}: a stand-in digester's biogas on steel pipe to a 1 MW gas set carrying a 1 MW load bank holds 850 kW and burns
 * 407 m3/h; a firebox on the same run burns it; the steam plant's burner on biogas reads NEEDS PIPELINE GAS.</li>
 * </ul>
 */
final class GasPowerScenes {
	private GasPowerScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;
	private static final Direction WEST = Direction.WEST;

	// ---- helpers ---------------------------------------------------------------------------------------------------------------------
	private static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	private static ServerLevel level(GameTestHelper helper) {
		return helper.getLevel();
	}

	private static void place(GameTestHelper helper, Block block, int x, int y, int z, Direction facing) {
		BlockPos main = abs(helper, x, y, z);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		level(helper).setBlock(main, state, 3);
		BigParts.grow(level(helper), main, state);
	}

	private static void place(GameTestHelper helper, Block block, int x, int z, Direction facing) {
		place(helper, block, x, U, z, facing);
	}

	private static void put(GameTestHelper helper, String id, int x, int y, int z) {
		level(helper).setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3);
	}

	private static void putFacing(GameTestHelper helper, String id, int x, int y, int z, Direction facing) {
		BlockState state = BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState();
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		}
		level(helper).setBlock(abs(helper, x, y, z), state, 3);
	}

	private static void pipe(GameTestHelper helper, int x, int y, int z) {
		put(helper, "gas_line", x, y, z);
	}

	private static void air(GameTestHelper helper, int x, int y, int z) {
		level(helper).setBlock(abs(helper, x, y, z), Blocks.AIR.defaultBlockState(), 3);
	}

	private static BlockEntity at(GameTestHelper helper, int x, int y, int z) {
		return BigParts.entity(level(helper), abs(helper, x, y, z));
	}

	@SuppressWarnings("unchecked")
	private static <T> T entity(GameTestHelper helper, int x, int y, int z, Class<T> kind) {
		BlockEntity e = at(helper, x, y, z);
		check(helper, kind.isInstance(e), "expected a " + kind.getSimpleName() + " at " + x + ", " + y + ", " + z + ": " + e);
		return (T) e;
	}

	private static String read(GameTestHelper helper, int x, int y, int z) {
		return at(helper, x, y, z) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	private static String read(GameTestHelper helper, int x, int z) {
		return read(helper, x, U, z);
	}

	/** A stone floor under the scene, and its chunks forced (see AmmoniaPlantScenes.floor). */
	private static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		BlockPos a = abs(helper, x1, 0, z1);
		BlockPos b = abs(helper, x2, 0, z2);
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				level(helper).setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				level(helper).setBlock(abs(helper, x, 0, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	private static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		level(helper).setBlock(abs(helper, x, U, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) level(helper).getBlockEntity(abs(helper, x, U, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	static void genset(GameTestHelper helper, int x, int z, boolean gas) {
		GensetSkid.build(level(helper), abs(helper, x, U, z), NORTH, gas);
	}

	static GensetControlPanelBlockEntity panel(GameTestHelper helper, int x, int z) {
		return (GensetControlPanelBlockEntity) level(helper).getBlockEntity(abs(helper, x, U, z));
	}

	/** Fuels a diesel set and starts it once its panel has found its parts. */
	private static void startDiesel(GameTestHelper helper, int x, int z) {
		if (level(helper).getBlockEntity(GensetSkid.base(abs(helper, x, U, z), NORTH, false)) instanceof GensetPartBlockEntity tank) {
			tank.setFuel(2000);
		}
		panel(helper, x, z).setWantRun(true);
	}

	/**
	 * A pad-mount or unit substation in a line (as the client scenes' inlineTransformer): fed from {@code (x, z)} going {@code d}, on its
	 * main cell's terminal if {@code feedMain}; returns the x (or z) of the cell its other terminal feeds.
	 */
	static BlockPos inline(GameTestHelper helper, Block block, int x, int z, Direction d, boolean feedMain) {
		int across = block == WildcatterBlocks.UNIT_SUBSTATION ? 3 : 1;
		BlockPos from = new BlockPos(x, U, z);
		BlockPos first = from.relative(d);
		Direction facing = feedMain ? d.getCounterClockWise() : d.getClockWise();
		BlockPos main = feedMain ? first : first.relative(d, across);
		place(helper, block, main.getX(), main.getZ(), facing);
		return from.relative(d, across + 2);
	}

	private static void size(GameTestHelper helper, int x, int y, int z, ConductorSize size) {
		if (level(helper).getBlockEntity(abs(helper, x, y, z)) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
			cable.setSize(size);
		}
	}

	/** Polls {@code done} every 20 ticks for up to {@code max} ticks, then runs {@code then} (which checks and fails if it must). */
	private static void await(GameTestHelper helper, int max, BooleanSupplier done, Runnable then) {
		if (max <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(20, () -> await(helper, max - 20, done, then));
	}

	private static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}

	private static boolean near(double got, double want, double share) {
		return Math.abs(got - want) <= want * share;
	}

	// ---- sales_gas ----------------------------------------------------------------------------------------------------------------------
	/** The delivery station's offset: 200 blocks east of the receipt station. */
	private static final int FAR = 200;

	static void salesGas(GameTestHelper helper) {
		floor(helper, -22, -6, 10, 10);
		floor(helper, FAR - 2, -2, FAR + 16, 12);
		// the receipt station, facing north: building x 0-2 z 0-3, meter run x 3 z 0-7, filter separator x 4-5 z 0-1, tap x 4 z 7
		place(helper, WildcatterBlocks.METER_BUILDING, 0, 0, NORTH);
		place(helper, WildcatterBlocks.ULTRASONIC_METER_RUN, 3, 0, NORTH);
		place(helper, WildcatterBlocks.FILTER_SEPARATOR, 4, 0, NORTH);
		place(helper, WildcatterBlocks.PIPELINE_TAP, 4, 7, NORTH);
		TestGasSource.SourceEntity gas = source(helper, 4, -3, Product.PIPELINE_GAS, 1.0);   // the gas plant's 1.0 m3/s
		pipe(helper, 4, U, -2);
		pipe(helper, 4, U, -1);
		// its residential service: a diesel set, a 480 V pad-mount run backward to 12.47 kV, a 25 kVA pole unit, 120/240 V to the box
		genset(helper, -18, 0, false);                                                  // skid x -22..-18, z 0-1
		BlockPos hv = inline(helper, WildcatterBlocks.PADMOUNT_480, -18, 0, Direction.EAST, true);   // x -17..-16; 12.47 kV out at x -15
		for (int x = hv.getX(); x <= hv.getX() + 2; x++) {
			put(helper, "dist_wire_hot", x, U, 0);
		}
		int px = hv.getX() + 3;                                                         // x -12
		put(helper, "pole", px, U, -1);
		putFacing(helper, "pole_transformer", px, U, 0, WEST);
		for (int x = px + 1; x <= -1; x++) {
			put(helper, "cable_lv", x, U, 0);                                            // to the building's box, the west face of its main cell
		}
		// the delivery station, 200 blocks east: regulator skid x FAR+4..5 z 0-4 in place of the filter, its gas to a 1 MW gas set
		place(helper, WildcatterBlocks.METER_BUILDING, FAR, 0, NORTH);
		place(helper, WildcatterBlocks.ULTRASONIC_METER_RUN, FAR + 3, 0, NORTH);
		place(helper, WildcatterBlocks.REGULATOR_SKID, FAR + 4, 0, NORTH);
		place(helper, WildcatterBlocks.PIPELINE_TAP, FAR + 4, 7, NORTH);
		pipe(helper, FAR + 5, U, 5);
		pipe(helper, FAR + 5, U, 6);
		pipe(helper, FAR + 6, U, 6);
		pipe(helper, FAR + 6, U, 7);                                                    // to the engine's north side
		genset(helper, FAR + 10, 8, true);                                              // radiator x FAR+4, engine FAR+5..8, panel FAR+10, z 8-9
		put(helper, "cable_industrial", FAR + 11, U, 8);
		put(helper, "three_phase_heater", FAR + 12, U, 8);
		MeterBuildingBlockEntity receipt = entity(helper, 0, U, 0, MeterBuildingBlockEntity.class);
		MeterBuildingBlockEntity delivery = entity(helper, FAR, U, 0, MeterBuildingBlockEntity.class);
		delivery.setMode(MeterBuildingBlockEntity.Mode.DELIVERY);
		SalesGasLine line = SalesGasLine.get(level(helper));
		double startBalance = line.balance();
		double startReceived = line.receivedGj();
		double startDelivered = line.deliveredM3();
		helper.runAfterDelay(30, () -> startDiesel(helper, -18, 0));
		helper.runAfterDelay(400, () -> {
			String r = read(helper, 0, 0);
			String set = read(helper, -18, 0);
			check(helper, set.contains("RUNNING") && set.contains("breaker CLOSED"), "the receipt's diesel set should run: " + set);
			check(helper, receipt.flowing() && r.contains("RECEIPT") && !r.contains("NO POWER"), "the receipt station should take gas on its own power: " + r + " | " + read(helper, -12, 0));
			check(helper, near(receipt.flow(), 1.0, 0.25), "it should take the source's 1.0 m3/s: " + receipt.flow() + " | " + r);
			double received = line.receivedGj() - startReceived;
			double credited = line.balance() - startBalance;
			double delivered = SalesGas.debit(line.deliveredM3() - startDelivered);   // the far station's skid filled meanwhile
			check(helper, received > 0 && near(credited + delivered, received * (1 - Tuning.SALES_FUEL_RETENTION), 0.001),
					"the balance should rise by what was received less 2 %: " + credited + " (+ " + delivered + " delivered) of " + received + " GJ");
			check(helper, near(SalesGas.gj(receipt.lifetimeM3()), received, 0.01), "the run ticket should match the line: " + receipt.lifetimeM3() + " m3, " + received + " GJ");
			log("sales_gas", "receipt: " + r + " | tap " + read(helper, 4, 7) + " | filter " + read(helper, 4, 0));
			// the far gas set on the delivery station's gas
			panel(helper, FAR + 10, 8).setWantRun(true);
			helper.runAfterDelay(320, () -> {
				String d = read(helper, FAR, 0);
				String gs = read(helper, FAR + 10, 8);
				check(helper, gs.contains("RUNNING") && gs.contains("breaker CLOSED") && gs.contains("pipeline gas"), "the gas set should run on the delivery's gas: " + gs + " | " + d);
				check(helper, read(helper, FAR + 12, U, 8).contains("running"), "the heater should run on the gas set: " + read(helper, FAR + 12, U, 8));
				check(helper, d.contains("DELIVERY") && line.deliveredM3() > 0, "the delivery station should book its gas: " + d);
				log("sales_gas", "delivery: " + d + " | set " + gs);
				offSpec(helper, receipt, gas, line, delivery);
			});
		});
	}

	/** Separator gas, wet gas and biogas at the receipt: each read OFF SPEC and nothing is booked. */
	private static void offSpec(GameTestHelper helper, MeterBuildingBlockEntity receipt, TestGasSource.SourceEntity gas, SalesGasLine line, MeterBuildingBlockEntity delivery) {
		gas.product = Product.GAS;
		helper.runAfterDelay(30, () -> {
			double before = receipt.lifetimeM3();
			helper.runAfterDelay(40, () -> {
				String rich = read(helper, 0, 0);
				check(helper, rich.contains("OFF SPEC: RICH") && receipt.lifetimeM3() == before && receipt.shutIn(), "separator gas should be refused: " + rich);
				gas.product = Product.WET_GAS;
				helper.runAfterDelay(30, () -> {
					String wet = read(helper, 0, 0);
					check(helper, wet.contains("OFF SPEC: WATER"), "wet gas should be refused: " + wet);
					gas.product = Product.BIOGAS;
					helper.runAfterDelay(30, () -> {
						String co2 = read(helper, 0, 0);
						check(helper, co2.contains("OFF SPEC: CO2") && receipt.lifetimeM3() == before, "biogas should be refused: " + co2);
						log("sales_gas", "refused: " + rich + " | " + wet + " | " + co2);
						gas.product = Product.PIPELINE_GAS;
						// the receipt's power cut, and 8 game hours on its battery skipped
						panel(helper, -18, 0).setWantRun(false);
						helper.runAfterDelay(100, () -> {
							String battery = read(helper, 0, 0);
							check(helper, battery.contains("NO POWER") && receipt.flowing(), "without power it should run on its battery: " + battery);
							receipt.setBatteryForTest(0);
							helper.runAfterDelay(30, () -> {
								String failed = read(helper, 0, 0);
								check(helper, failed.contains("METER FAILED") && receipt.shutIn() && read(helper, 4, 7).contains("CLOSED"), "with the battery flat it should shut in: " + failed);
								// the account drained by hook: the far station has nothing to give
								line.drainForTest();
								helper.runAfterDelay(30, () -> {
									String none = read(helper, FAR, 0);
									check(helper, none.contains("NO GAS ON THE LINE"), "with the account empty the delivery should say so: " + none);
									await(helper, 5000, () -> !read(helper, FAR + 10, 8).contains("RUNNING"), () -> {
										String stopped = read(helper, FAR + 10, 8);
										check(helper, !stopped.contains("RUNNING") && (stopped.contains("OUT OF GAS") || stopped.contains("NO GAS")), "the gas set should stop on fuel: " + stopped);
										log("sales_gas", "ok: " + failed + " | " + none + " | " + stopped);
										helper.succeed();
									});
								});
							});
						});
					});
				});
			});
		});
	}

	// ---- acid_steam ---------------------------------------------------------------------------------------------------------------------
	private static void water(GameTestHelper helper, int x, int z) {
		if (at(helper, x, U, z) instanceof WaterUser user) {
			user.addWater(100_000);
		}
	}

	static void acidSteam(GameTestHelper helper) {
		floor(helper, -2, -12, 40, 19);
		// the acid plant, every part facing west as in the client scene acidPlant
		place(helper, WildcatterBlocks.DRYING_TOWER, 0, 0, WEST);              // x 0-2, z -2..0
		place(helper, WildcatterBlocks.MAIN_BLOWER, 3, 0, WEST);               // x 3-7
		place(helper, WildcatterBlocks.SULFUR_FURNACE, 8, 0, WEST);            // x 8-17
		place(helper, WildcatterBlocks.WASTE_HEAT_BOILER, 18, 0, WEST);        // x 18-25, z -2..0, y U..U+3
		place(helper, WildcatterBlocks.SULFUR_MELTER, 18, -3, WEST);           // x 18-25, z -6..-3
		place(helper, WildcatterBlocks.CONVERTER, 27, 0, WEST);                // x 27-31, z -4..0
		place(helper, WildcatterBlocks.INTERPASS_ABSORBER, 32, 0, WEST);       // x 32-34, z -2..0
		place(helper, WildcatterBlocks.FINAL_ABSORBER, 27, -5, WEST);          // x 27-29, z -7..-5
		place(helper, WildcatterBlocks.STOCK_TANK, 31, -6, WEST);              // x 31-34, z -9..-6
		place(helper, WildcatterBlocks.LPG_BULLET, 6, -8, WEST);               // x 6-17, z -10..-8
		for (int y = 0; y <= 3; y++) {
			pipe(helper, 26, U + y, -5);                                            // molten sulfur up out of the melter's east end
		}
		for (int x = 8; x <= 25; x++) {
			pipe(helper, x, U + 3, -5);
		}
		pipe(helper, 8, U + 3, -4);
		for (int y = 3; y >= 0; y--) {
			pipe(helper, 8, U + y, -3);                                             // down to the burner on the furnace's north side
		}
		pipe(helper, 26, U, -1);                                                    // burner gas, boiler to converter
		pipe(helper, 30, U, -6);                                                    // acid to the stock tank
		for (int x = 17; x <= 25; x++) {
			pipe(helper, x, U, -7);                                                 // start-up LPG along the melter's north side
		}
		// the steam side, facing north: superheater against the converter, the turbine east of it, its generator, breaker and condenser
		place(helper, WildcatterBlocks.ACID_SUPERHEATER, 27, 1, NORTH);         // x 27-29, z 1-3, y U..U+5
		place(helper, WildcatterBlocks.STEAM_TURBINE_5, 30, 1, NORTH);          // x 30-32, z 1-6
		place(helper, WildcatterBlocks.ST_GENERATOR, 33, 1, NORTH);             // x 33-35, z 1-4
		place(helper, WildcatterBlocks.GEN_BREAKER, 33, 5, NORTH);              // x 33-34, z 5-6
		place(helper, WildcatterBlocks.AIR_COOLED_CONDENSER, 27, 7, NORTH);     // x 27-32, z 7-12
		place(helper, WildcatterBlocks.HRSG_FEED_PUMP, 22, 1, NORTH);           // x 22-23, z 1-4, against the waste heat boiler
		place(helper, WildcatterBlocks.UNIT_AUX_TRANSFORMER, 36, 10, NORTH);    // x 35-38, z 7-10; 13.8 kV bushings over (36, 7)
		// main steam: up off the boiler's top, over, and down to the superheater's west side
		pipe(helper, 25, U + 4, 0);
		pipe(helper, 25, U + 4, 1);
		for (int y = 4; y >= 0; y--) {
			pipe(helper, 26, U + y, 1);
		}
		// 13.8 kV: the breaker's back, up and over to the UAT's bushings
		for (int y = 0; y <= 4; y++) {
			put(helper, "bus_duct_gen", 33, U + y, 7);
		}
		for (int x = 34; x <= 36; x++) {
			put(helper, "bus_duct_gen", x, U + 4, 7);
		}
		// the plant's 4.16 kV bus: the UAT's back, along z 13, up x 3 to the blower's box
		for (int z = 11; z <= 13; z++) {
			put(helper, "cable_mv", 36, U, z);
		}
		for (int x = 3; x <= 35; x++) {
			put(helper, "cable_mv", x, U, 13);
		}
		for (int z = 1; z <= 12; z++) {
			put(helper, "cable_mv", 3, U, z);
		}
		// two diesel sets on a 480 V busway, a backward unit substation onto the 4.16 kV bus
		int[] sets = {14, 22};
		for (int x : sets) {
			genset(helper, x, 16, false);                                            // x-4..x, z 16-17
			put(helper, "busway_industrial", x + 1, U, 16);
		}
		for (int x = 15; x <= 24; x++) {
			put(helper, "busway_industrial", x, U, 15);
		}
		BlockPos mv = inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 24, 15, Direction.EAST, false);   // x 25-28; 4.16 kV out at x 29
		put(helper, "cable_mv", mv.getX(), U, 15);
		put(helper, "cable_mv", mv.getX(), U, 14);
		// 480 V for the condenser fans and the feed pump: off the busway, over the 4.16 kV cable, up x 24
		int[][] lv = {{24, 0, 14}, {24, 1, 14}, {24, 1, 13}, {24, 1, 12}, {24, 0, 12}, {24, 0, 11}, {24, 0, 10}, {24, 0, 9}, {24, 0, 8}, {24, 0, 7},
				{25, 0, 7}, {26, 0, 7}, {24, 0, 6}, {24, 0, 5}, {23, 0, 5}, {22, 0, 5}, {21, 0, 5}, {21, 0, 4}, {21, 0, 3}, {21, 0, 2}, {21, 0, 1}};
		for (int[] c : lv) {
			put(helper, "cable_industrial", c[0], U + c[1], c[2]);
			size(helper, c[0], U + c[1], c[2], ConductorSize.KCMIL_500);
		}
		// fill and start
		StorageTankBlockEntity bullet = entity(helper, 6, U, -8, StorageTankBlockEntity.class);
		bullet.testFill(Product.LPG, 20_000);
		SulfurMelterBlockEntity melter = entity(helper, 18, U, -3, SulfurMelterBlockEntity.class);
		melter.slots().set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.SULFUR), 64));
		Runnable topUp = () -> {
			water(helper, 18, 0);
			water(helper, 27, -5);
		};
		topUp.run();
		helper.runAfterDelay(30, () -> {
			for (int x : sets) {
				startDiesel(helper, x, 16);
			}
		});
		SteamCycleBlockEntity st = entity(helper, 30, U, 1, SteamCycleBlockEntity.class);
		AcidSuperheaterBlockEntity sh = entity(helper, 27, U, 1, AcidSuperheaterBlockEntity.class);
		// keep the boiler and absorber in water (their pumps stand in), and roll the turbine up once it has steam
		for (int t = 100; t < 9000; t += 100) {
			helper.runAfterDelay(t, topUp);
		}
		helper.runAfterDelay(300, () -> await(helper, 2400, () -> sh.steamKgs() > 1.5 && sh.superheated(), () -> {
			String chain = " | superheater " + read(helper, 27, 1) + " | boiler " + read(helper, 18, 0) + " | furnace " + read(helper, 8, 0) + " | converter "
					+ read(helper, 27, 0) + " | melter " + read(helper, 18, -3) + " | blower " + read(helper, 3, 0) + " | sets " + read(helper, 14, 16) + " / " + read(helper, 22, 16);
			check(helper, sh.steamKgs() > 1.5 && sh.superheated(), "the superheater should pass about 2 kg/s of superheated steam:" + chain);
			st.skipRunupForTest();
			await(helper, 1600, () -> read(helper, 30, 1).contains("breaker CLOSED"), () -> {
				String turbine = read(helper, 30, 1);
				check(helper, turbine.contains("breaker CLOSED"), "the turbine should synchronize onto the plant's bus: " + turbine + " | acc " + read(helper, 27, 7)
						+ " | pump " + read(helper, 22, 1) + " | uat " + read(helper, 36, 10) + chain);
				java.util.regex.Matcher mw = java.util.regex.Pattern.compile("steam for ([0-9.]+) MW").matcher(turbine);
				check(helper, mw.find() && Double.parseDouble(mw.group(1)) >= 1.4 && Double.parseDouble(mw.group(1)) <= 1.8, "the turbine should read 1.4 to 1.8 MW: " + turbine);
				check(helper, read(helper, 18, 0).contains("condensate back"), "the feed pump should return the condensate: " + read(helper, 18, 0));
				log("acid_steam", "on line: " + turbine + " | boiler " + read(helper, 18, 0));
				// the sets stopped: the plant carries itself
				for (int x : sets) {
					panel(helper, x, 16).setWantRun(false);
				}
				helper.runAfterDelay(300, () -> {
					String alone = read(helper, 30, 1);
					String furnace = read(helper, 8, 0);
					check(helper, alone.contains("breaker CLOSED") && furnace.contains("burning") && !read(helper, 14, 16).contains("RUNNING"),
							"with the sets stopped the turbine should carry the blower: " + alone + " | " + furnace + " | blower " + read(helper, 3, 0));
					log("acid_steam", "islanded: " + alone + " | " + furnace);
					// the sets back, synchronizing onto the island
					for (int x : sets) {
						startDiesel(helper, x, 16);
					}
					helper.runAfterDelay(400, () -> lowTemperature(helper, st));
				});
			});
		}));
	}

	/** The converter held off: the superheater passes saturated steam and the turbine trips; then the superheater broken: NO STEAM. */
	private static void lowTemperature(GameTestHelper helper, SteamCycleBlockEntity st) {
		String sets = read(helper, 14, 16);
		check(helper, sets.contains("RUNNING") && sets.contains("breaker CLOSED"), "the sets should come back onto the island: " + sets);
		ConverterBlockEntity converter = entity(helper, 27, U, 0, ConverterBlockEntity.class);
		BlockPos c = abs(helper, 27, U, 0);
		converter.cycleHoa(c);                                                      // HAND to OFF
		await(helper, 400, () -> read(helper, 30, 1).contains("LOW STEAM TEMPERATURE"), () -> {
			String trip = read(helper, 30, 1);
			check(helper, trip.contains("LOW STEAM TEMPERATURE"), "with the converter stopped the turbine should trip on low steam temperature: " + trip + " | " + read(helper, 27, 1));
			converter.cycleHoa(c);                                                  // OFF to AUTO
			converter.cycleHoa(c);                                                  // AUTO to HAND
			// the superheater broken
			for (int x = 27; x <= 29; x++) {
				for (int z = 1; z <= 3; z++) {
					for (int y = 0; y <= 5; y++) {
						air(helper, x, U + y, z);
					}
				}
			}
			helper.runAfterDelay(300, () -> {
				String none = read(helper, 30, 1);
				String acid = read(helper, 27, 0);
				check(helper, none.contains("NO STEAM"), "without the superheater the turbine should read NO STEAM: " + none);
				check(helper, acid.contains("double absorption"), "the acid plant should keep making acid on the sets: " + acid + " | furnace " + read(helper, 8, 0));
				log("acid_steam", "ok: " + trip + " | " + none + " | " + acid);
				helper.succeed();
			});
		});
	}

	// ---- biogas ---------------------------------------------------------------------------------------------------------------------------
	static void biogas(GameTestHelper helper) {
		floor(helper, -2, -6, 46, 9);
		TestGasSource.SourceEntity digester = source(helper, 3, -3, Product.BIOGAS, 1.0);
		pipe(helper, 3, U, -2);
		pipe(helper, 3, U, -1);                                                     // to the engine's north side
		// a firebox under a starter boiler, on its own digester's biogas (it would take a whole small digester's gas)
		TestGasSource.SourceEntity fireGas = source(helper, 0, -4, Product.BIOGAS, 1.0);
		pipe(helper, 0, U, -3);
		level(helper).setBlock(abs(helper, 0, U, -2), WildcatterBlocks.FIREBOX.defaultBlockState(), 3);
		level(helper).setBlock(abs(helper, 0, U + 1, -2), WildcatterBlocks.BOILER.defaultBlockState(), 3);
		if (level(helper).getBlockEntity(abs(helper, 0, U + 1, -2)) instanceof BoilerBlockEntity boiler) {
			boiler.addWater(4000);
		}
		genset(helper, 8, 0, true);                                                 // radiator x 2, engine x 3-6, panel x 8, z 0-1
		BlockPos mv = inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 8, 0, Direction.EAST, false);   // x 9-12; 4.16 kV out at x 13
		for (int z = 0; z <= 5; z++) {
			put(helper, "cable_mv", mv.getX(), U, z);
		}
		put(helper, "cable_mv", 14, U, 5);
		put(helper, "cable_mv", 15, U, 5);
		put(helper, "cable_mv", 16, U, 5);                                          // the UAT's 4.16 kV side, the back of its main cell
		place(helper, WildcatterBlocks.UNIT_AUX_TRANSFORMER, 16, 4, NORTH);         // x 15-18, z 1-4; run backward up to 13.8 kV
		place(helper, WildcatterBlocks.LOAD_BANK, 20, 2, NORTH);                    // x 20-22, z 2-7
		put(helper, "bus_duct_gen", 16, U + 4, 1);
		for (int x = 17; x <= 19; x++) {
			put(helper, "bus_duct_gen", x, U + 4, 1);
		}
		for (int y = 3; y >= 0; y--) {
			put(helper, "bus_duct_gen", 19, U + y, 1);
		}
		for (int x = 20; x <= 22; x++) {
			put(helper, "bus_duct_gen", x, U, 1);                                   // along the bank's front
		}
		// the steam plant, its burner on a second digester's biogas
		BlockPos gen = abs(helper, 40, U, 0);
		dev.wildcatter.block.SteamPlantLayout.layout().build(level(helper), gen, NORTH, 0);
		source(helper, 37, -3, Product.BIOGAS, 1.0);
		pipe(helper, 38, U, -3);                                                    // to the burner at x 39
		// the bank set to 1 MW before the set starts: an unloaded 13.8 kV delta behind the transformer floats, and the set closes onto its load
		helper.runAfterDelay(20, () -> ((LoadBankBlockEntity) at(helper, 20, U, 2)).setSteps(1));
		helper.runAfterDelay(30, () -> panel(helper, 8, 0).setWantRun(true));
		boolean[] lit = new boolean[1];
		for (int t = 5; t <= 300; t += 5) {
			helper.runAfterDelay(t, () -> lit[0] |= level(helper).getBlockState(abs(helper, 0, U, -2)).getValue(FireboxBlock.LIT));
		}
		helper.runAfterDelay(60, () -> {
			TurbineGeneratorBlockEntity plant = (TurbineGeneratorBlockEntity) level(helper).getBlockEntity(gen);
			plant.setWaterForTest(Tuning.DRUM_WATER_KG * 0.8, 1000);
			plant.setWantRun(true);
		});
		helper.runAfterDelay(320, () -> {
			String set = read(helper, 8, 0);
			check(helper, set.contains("RUNNING") && set.contains("breaker CLOSED"), "the gas set should start on biogas: " + set);
			helper.runAfterDelay(200, () -> {
				GensetControlPanelBlockEntity p = panel(helper, 8, 0);
				double before = p.biogasBurned();
				helper.runAfterDelay(600, () -> {
					String held = read(helper, 8, 0);
					double m3h = (p.biogasBurned() - before) / 30.0 * 3600;
					check(helper, p.onBiogas() && held.contains("BIOGAS: 850 kW MAX"), "the set should be on biogas, derated: " + held);
					check(helper, p.watts() > 800_000 && p.watts() < 860_000, "the set should hold about 850 kW against the 1 MW bank: " + p.watts() + " | " + held + " | bank " + read(helper, 20, 2));
					check(helper, near(m3h, Biogas.fullLoadM3h(), 0.05), "it should burn about 407 m3/h: " + m3h);
					check(helper, lit[0] && fireGas.taken > 1, "the firebox should burn the biogas (until its boiler is full): " + fireGas.taken + " m3 taken, lit " + lit[0]);
					String burner = read(helper, 40, 0);
					check(helper, burner.contains("NEEDS PIPELINE GAS"), "the steam plant's burner should refuse biogas: " + burner);
					log("biogas", String.format("ok: %.0f m3/h | %s | bank %s | %s", m3h, held, read(helper, 20, 2), burner));
					helper.succeed();
				});
			});
		});
	}
}
