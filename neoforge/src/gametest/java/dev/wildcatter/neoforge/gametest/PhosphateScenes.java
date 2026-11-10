package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.AcidEvaporatorBlockEntity;
import dev.wildcatter.block.BaggingStationBlockEntity;
import dev.wildcatter.block.DecantTowerBlockEntity;
import dev.wildcatter.block.DepaTopoCircuitBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.GranulationPlantBlockEntity;
import dev.wildcatter.block.PhosphateMillBlockEntity;
import dev.wildcatter.block.PhosphoricReactorBlockEntity;
import dev.wildcatter.block.PotashDissolverBlockEntity;
import dev.wildcatter.block.PotashDryerBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Phase 18 (docs/multiblocks/fertilizer.md, uranium-from-phosphate.md), the specs' three scenes. Every part faces north at y 2 (main
 * cell at the north-west bottom corner); test sources stand in for process steam, gas, rock slurry and filter acid where the spec's
 * own source is another plant; diesel sets (through unit substations for the 4.16 kV mill and filter) on cable to each box.
 * <ul>
 * <li>{@code phosphoric}: rock by hopper to the mill, its slurry to the reactor with sulfuric acid from a test-filled stock tank; the pan
 * filter's acid through the evaporator to merchant acid, its gypsum to a lined cell whose readout names it; the granulator on TSP
 * fills a bag (brought near full by hook), then on DAP with ammonia from a test-filled bullet a DAP bag; the ammonia cut, NO AMMONIA;
 * the cell's floor broken, NO GYPSUM STACK.</li>
 * <li>{@code potash}: potash by hopper and process steam; a MOP bag under the bagging station; salt tails to a lined cell.</li>
 * <li>{@code phosphate_uranium}: filter acid through the pretreatment and the DEPA-TOPO circuit, FILLING until its drums come, then
 * about 2.8 kg/h of uranium into the strip and the treated acid on to an evaporator; without iron NO IRON and the acid round it; with
 * the strip full, FULL.</li>
 * </ul>
 */
final class PhosphateScenes {
	private PhosphateScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	// ---- helpers --------------------------------------------------------------------------------------------------------------------
	private static void check(GameTestHelper helper, boolean ok, String message) {
		helper.assertTrue(ok, Component.literal(message));
	}

	private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	private static void place(GameTestHelper helper, Block block, int x, int z, Direction facing) {
		BlockPos main = abs(helper, x, U, z);
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		helper.getLevel().setBlock(main, state, 3);
		BigParts.grow(helper.getLevel(), main, state);
	}

	private static void place(GameTestHelper helper, Block block, int x, int z) {
		place(helper, block, x, z, NORTH);
	}

	private static void put(GameTestHelper helper, String id, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), BuiltInRegistries.BLOCK.getValue(Wildcatter.id(id)).defaultBlockState(), 3);
	}

	/** Straight runs of {@code id} at y U between the corners given, each run including both ends. */
	private static void run(GameTestHelper helper, String id, int[]... corners) {
		for (int i = 0; i + 1 < corners.length; i++) {
			int[] a = corners[i];
			int[] b = corners[i + 1];
			int dx = Integer.signum(b[0] - a[0]);
			int dz = Integer.signum(b[1] - a[1]);
			for (int x = a[0], z = a[1];; x += dx, z += dz) {
				put(helper, id, x, U, z);
				if (id.equals("cable_industrial") && helper.getLevel().getBlockEntity(abs(helper, x, U, z)) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
					cable.setSize(dev.wildcatter.energy.ConductorSize.KCMIL_500);   // 500 kcmil, 380 A: the default 2 AWG would burn on these motors
				}
				if (x == b[0] && z == b[1]) {
					break;
				}
			}
		}
		if (corners.length == 1) {
			put(helper, id, corners[0][0], U, corners[0][1]);
		}
	}

	private static void air(GameTestHelper helper, int x, int y, int z) {
		helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.AIR.defaultBlockState(), 3);
	}

	@SuppressWarnings("unchecked")
	private static <T> T entity(GameTestHelper helper, int x, int z, Class<T> kind) {
		BlockEntity e = BigParts.entity(helper.getLevel(), abs(helper, x, U, z));
		check(helper, kind.isInstance(e), "expected a " + kind.getSimpleName() + " at " + x + ", " + z + ": " + e);
		return (T) e;
	}

	private static String read(GameTestHelper helper, int x, int z) {
		return BigParts.entity(helper.getLevel(), abs(helper, x, U, z)) instanceof Inspectable i ? i.inspect().getString() : "(no readout)";
	}

	/** A stone floor at y 0, the chunks forced (see AmmoniaPlantScenes.floor). */
	private static void floor(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		BlockPos a = abs(helper, x1, 0, z1);
		BlockPos b = abs(helper, x2, 0, z2);
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
				helper.getLevel().setChunkForced(cx, cz, true);
			}
		}
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				helper.getLevel().setBlock(abs(helper, x, 0, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	private static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, U, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, U, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	/**
	 * A lined tailings cell, 7 x 7 and 3 high, its floor at y U-1 from (x0, z0); the decant tower just inside its north wall. Its feed
	 * comes over the wall from (x0+3, z0-1) at y U, which the caller pipes to.
	 */
	private static BlockPos cell(GameTestHelper helper, int x0, int z0) {
		for (int x = x0; x <= x0 + 6; x++) {
			for (int z = z0; z <= z0 + 6; z++) {
				put(helper, "cell_liner", x, U - 1, z);
				if (x == x0 || x == x0 + 6 || z == z0 || z == z0 + 6) {
					for (int y = U; y <= U + 2; y++) {
						put(helper, "cell_liner", x, y, z);
					}
				}
			}
		}
		int tx = x0 + 3;
		put(helper, "decant_tower", tx, U, z0 + 1);
		for (int y = U; y <= U + 3; y++) {
			put(helper, "gas_line", tx, y, z0 - 1);
		}
		put(helper, "gas_line", tx, U + 3, z0);
		for (int y = U + 1; y <= U + 3; y++) {
			put(helper, "gas_line", tx, y, z0 + 1);
		}
		return abs(helper, tx, U, z0 + 1);
	}

	/** A 500 kW diesel set, its panel at (x, z) facing north: the skid west of it, its 480 V terminal at (x+1, z). */
	private static void genset(GameTestHelper helper, int x, int z) {
		GensetSkid.build(helper.getLevel(), abs(helper, x, U, z), NORTH, false);
	}

	/** A unit substation run backward off the 480 V terminal of the set whose panel is at (x, z), going east: its 4.16 kV out at (x+5, z). */
	private static void stepUp(GameTestHelper helper, int x, int z) {
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, x + 4, z, Direction.SOUTH);
	}

	/** Puts a unit's selector to OFF, so it does not start with its set. */
	private static void hold(GameTestHelper helper, int[]... parts) {
		for (int[] p : parts) {
			entity(helper, p[0], p[1], dev.wildcatter.block.PlantUnitBlockEntity.class).cycleHoa(null);
		}
	}

	/** Puts held units back to HAND one at a time, 60 ticks apart, as an operator starts big motors on a small set; then {@code then}. */
	private static void release(GameTestHelper helper, int[][] parts, int i, Runnable then) {
		if (i >= parts.length) {
			then.run();
			return;
		}
		dev.wildcatter.block.PlantUnitBlockEntity unit = entity(helper, parts[i][0], parts[i][1], dev.wildcatter.block.PlantUnitBlockEntity.class);
		unit.cycleHoa(null);   // OFF to AUTO
		unit.cycleHoa(null);   // AUTO to HAND
		helper.runAfterDelay(60, () -> release(helper, parts, i + 1, then));
	}

	private static GensetControlPanelBlockEntity panel(GameTestHelper helper, int x, int z) {
		return (GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, U, z));
	}

	private static void fuel(GameTestHelper helper, int x, int z) {
		if (helper.getLevel().getBlockEntity(GensetSkid.base(abs(helper, x, U, z), NORTH, false)) instanceof GensetPartBlockEntity tank) {
			tank.setFuel(2000);
		}
	}

	/** Fuels and starts the sets at the panels given, once they have found their parts; {@code then} once all are up. */
	private static void start(GameTestHelper helper, int[][] panels, Runnable then) {
		helper.runAfterDelay(30, () -> {
			for (int[] p : panels) {
				fuel(helper, p[0], p[1]);
				panel(helper, p[0], p[1]).setWantRun(true);
			}
			helper.runAfterDelay(300, () -> {
				for (int[] p : panels) {
					String state = read(helper, p[0], p[1]);
					check(helper, state.contains("RUNNING") && state.contains("breaker CLOSED"), "the set at " + p[0] + ", " + p[1] + " should be running with its breaker closed: " + state);
				}
				then.run();
			});
		});
	}

	private static void water(GameTestHelper helper, int[]... at) {
		for (int[] p : at) {
			entity(helper, p[0], p[1], WaterUser.class).addWater(100_000);
		}
	}

	/** Polls {@code done} every 20 ticks for up to {@code max} ticks, topping up the water users, then runs {@code then}. */
	private static void await(GameTestHelper helper, int max, int[][] wet, BooleanSupplier done, Runnable then) {
		water(helper, wet);
		if (max <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(20, () -> await(helper, max - 20, wet, done, then));
	}

	private static boolean near(double got, double want, double share) {
		return Math.abs(got - want) <= want * share;
	}

	private static void log(String scene, String text) {
		System.out.println("[wildcatter-neoforge-test] " + scene + " " + text);
	}

	// ---- phosphoric ---------------------------------------------------------------------------------------------------------------------
	static void phosphoric(GameTestHelper helper) {
		floor(helper, -26, -16, 37, 36);
		place(helper, WildcatterBlocks.PHOSPHATE_MILL, -6, -8);          // x -6..-4, z -8..-3
		place(helper, WildcatterBlocks.PHOSPHORIC_REACTOR, 0, 0);        // x 0-7, z 0-7
		place(helper, WildcatterBlocks.TILTING_PAN_FILTER, 8, 0);        // x 8-13, z 0-9, against the reactor's east side
		place(helper, WildcatterBlocks.STOCK_TANK, 2, 9);                // x 2-5, z 9-12: the sulfuric acid
		place(helper, WildcatterBlocks.ACID_EVAPORATOR, 9, 11);          // x 9-11, z 11-13
		place(helper, WildcatterBlocks.GRANULATION_PLANT, 9, 16);        // x 9-12, z 16-27
		place(helper, WildcatterBlocks.BAGGING_STATION, 10, 28);         // x 10-11, z 28-29, against its south end
		place(helper, WildcatterBlocks.LPG_BULLET, 15, 18);              // x 15-17, z 18-29: the ammonia
		BlockPos tower = cell(helper, 0, 16);                            // x 0-6, z 16-22
		// steel pipe
		run(helper, "gas_line", new int[] {-5, -2}, new int[] {2, -2}, new int[] {2, -1});                  // rock slurry, mill to reactor
		run(helper, "gas_line", new int[] {4, 8});                                                          // sulfuric acid, tank to reactor
		run(helper, "gas_line", new int[] {7, 9}, new int[] {7, 15}, new int[] {3, 15});                     // gypsum to the cell
		run(helper, "gas_line", new int[] {10, 10});                                                        // filter acid to the evaporator
		run(helper, "gas_line", new int[] {12, 12});
		source(helper, 13, 12, Product.PROCESS_STEAM, 2.0);                                                 // steam to the evaporator
		run(helper, "gas_line", new int[] {10, 14}, new int[] {13, 14}, new int[] {13, 16});                 // merchant acid to the granulator
		run(helper, "gas_line", new int[] {13, 18}, new int[] {14, 18});                                     // ammonia
		run(helper, "gas_line", new int[] {13, 20});
		source(helper, 14, 20, Product.ROCK_SLURRY, 2.0);                                                   // the granulator's rock slurry
		run(helper, "gas_line", new int[] {13, 24});
		source(helper, 14, 24, Product.PIPELINE_GAS, 0.1);                                                  // gas for its dryer
		// power: A (480 V) the reactor; B (480 V) the evaporator, granulator and bagging station; C and D (4.16 kV) the mill and filter
		genset(helper, -20, -4);
		run(helper, "cable_industrial", new int[] {-19, -4}, new int[] {-19, 0}, new int[] {-1, 0});
		genset(helper, 5, 34);
		run(helper, "cable_industrial", new int[] {6, 34}, new int[] {6, 30}, new int[] {9, 30}, new int[] {9, 28});
		run(helper, "cable_industrial", new int[] {8, 30}, new int[] {8, 11});
		genset(helper, -20, -14);
		stepUp(helper, -20, -14);
		run(helper, "cable_mv", new int[] {-15, -14}, new int[] {-7, -14}, new int[] {-7, -8});
		genset(helper, 30, -14);
		stepUp(helper, 30, -14);
		run(helper, "cable_mv", new int[] {35, -14}, new int[] {35, -1}, new int[] {8, -1});
		// feeds
		entity(helper, 2, 9, StorageTankBlockEntity.class).testFill(Product.SULFURIC_ACID, 30_000);
		entity(helper, 15, 18, StorageTankBlockEntity.class).testFill(Product.AMMONIA, 2_000);
		PhosphateMillBlockEntity mill = entity(helper, -6, -8, PhosphateMillBlockEntity.class);
		mill.slots().set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.PHOSPHATE), 16));
		BaggingStationBlockEntity bagger = entity(helper, 10, 28, BaggingStationBlockEntity.class);
		bagger.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 8));
		int[][] wet = {{-6, -8}, {8, 0}};
		water(helper, wet);
		PhosphoricReactorBlockEntity reactor = entity(helper, 0, 0, PhosphoricReactorBlockEntity.class);
		AcidEvaporatorBlockEntity evaporator = entity(helper, 9, 11, AcidEvaporatorBlockEntity.class);
		GranulationPlantBlockEntity granulator = entity(helper, 9, 16, GranulationPlantBlockEntity.class);
		int[][] staged = {{9, 11}, {9, 16}, {10, 28}};
		hold(helper, staged);
		start(helper, new int[][] {{-20, -4}, {5, 34}, {-20, -14}, {30, -14}}, () -> release(helper, staged, 0, () -> await(helper, 900, wet,
				() -> reactor.making() > 0 && evaporator.making() > 0 && granulator.making() > 0 && towerGypsum(helper, tower) > 0, () -> {
			String chain = " | mill " + read(helper, -6, -8) + " | reactor " + read(helper, 0, 0) + " | filter " + read(helper, 8, 0) + " | evaporator " + read(helper, 9, 11)
					+ " | granulator " + read(helper, 9, 16) + " | tower " + towerRead(helper, tower) + " | set A " + read(helper, -20, -4) + " | set B " + read(helper, 5, 34);
			check(helper, mill.making() > 0, "the mill should grind rock: " + chain);
			check(helper, near(reactor.making(), Phosphate.p2o5Kgs() * 3600, 0.10), "the reactor should make about 833 kg/h of P2O5: " + reactor.making() + chain);
			check(helper, near(evaporator.making(), Phosphate.merchantLps(), 0.15), "the evaporator should make about 0.26 L/s of merchant acid: " + evaporator.making() + chain);
			check(helper, towerGypsum(helper, tower) > 0 && towerRead(helper, tower).contains("phosphogypsum"), "the cell should name its phosphogypsum: " + chain);
			check(helper, granulator.making() > 0 && read(helper, 9, 16).contains("TSP"), "the granulator should make TSP: " + chain);
			log("phosphoric", "running:" + chain);
			granulator.addGranules(Tuning.BULK_BAG_KG - 5);
			await(helper, 300, wet, () -> bagger.outCount() >= 1, () -> {
				check(helper, bagger.outCount() >= 1 && bagger.slots().stream().anyMatch(s -> s.is(WildcatterItems.TSP_BAG)), "a TSP bag should be filled: " + read(helper, 10, 28)
						+ " | " + read(helper, 9, 16));
				granulator.switchProduct();
				granulator.addGranules(Tuning.BULK_BAG_KG - 5);
				await(helper, 300, wet, () -> bagger.slots().stream().anyMatch(s -> s.is(WildcatterItems.DAP_BAG)), () -> {
					String dap = read(helper, 9, 16);
					check(helper, bagger.slots().stream().anyMatch(s -> s.is(WildcatterItems.DAP_BAG)), "a DAP bag should be filled: " + read(helper, 10, 28) + " | " + dap);
					check(helper, dap.contains("DAP"), "the granulator should be on DAP: " + dap);
					air(helper, 13, U, 18);                                                                   // the ammonia cut
					air(helper, 14, U, 18);
					await(helper, 100, wet, () -> read(helper, 9, 16).contains("NO AMMONIA"), () -> {
						String noNh3 = read(helper, 9, 16);
						check(helper, noNh3.contains("NO AMMONIA"), "without ammonia the granulator should stop on DAP: " + noNh3);
						air(helper, 2, U - 1, 19);                                                            // a hole in the cell's floor
						await(helper, 900, wet, () -> read(helper, 8, 0).contains("NO GYPSUM STACK"), () -> {
							String noStack = read(helper, 8, 0);
							check(helper, noStack.contains("NO GYPSUM STACK"), "with the cell breached the filter should stop: " + noStack + " | " + towerRead(helper, tower));
							log("phosphoric", "ok: " + noNh3 + " | " + noStack);
							helper.succeed();
						});
					});
				});
			});
		})));
	}

	private static double towerGypsum(GameTestHelper helper, BlockPos tower) {
		return helper.getLevel().getBlockEntity(tower) instanceof DecantTowerBlockEntity t ? t.gypsumKg() : 0;
	}

	private static String towerRead(GameTestHelper helper, BlockPos tower) {
		return helper.getLevel().getBlockEntity(tower) instanceof DecantTowerBlockEntity t ? t.inspect().getString() : "(no tower)";
	}

	// ---- potash -------------------------------------------------------------------------------------------------------------------------
	static void potash(GameTestHelper helper) {
		floor(helper, -22, -10, 18, 8);
		place(helper, WildcatterBlocks.POTASH_DISSOLVER, 0, 0);          // x 0-3, z 0-3
		place(helper, WildcatterBlocks.VACUUM_CRYSTALLIZER, 4, 0);       // x 4-6, z 0-2, against it
		place(helper, WildcatterBlocks.POTASH_DRYER, 7, 0);              // x 7-14, z 0-2, against that
		place(helper, WildcatterBlocks.BAGGING_STATION, 10, -2);         // x 10-11, z -2..-1, against the dryer's north side
		BlockPos tower = cell(helper, -10, -2);                          // x -10..-4, z -2..4
		run(helper, "gas_line", new int[] {2, 4});
		source(helper, 2, 5, Product.PROCESS_STEAM, 1.0);                                                   // the dissolver's steam
		run(helper, "gas_line", new int[] {5, 3});
		source(helper, 5, 4, Product.PROCESS_STEAM, 1.0);                                                   // the crystallizer's ejectors
		run(helper, "gas_line", new int[] {10, 3});
		source(helper, 10, 4, Product.PIPELINE_GAS, 0.1);                                                   // the dryer's gas
		run(helper, "gas_line", new int[] {-1, 2}, new int[] {-2, 2}, new int[] {-2, -3}, new int[] {-7, -3});   // salt tails to the cell
		genset(helper, -15, -8);
		run(helper, "cable_industrial", new int[] {-14, -8}, new int[] {9, -8});
		run(helper, "cable_industrial", new int[] {-1, -7}, new int[] {-1, 0});
		run(helper, "cable_industrial", new int[] {4, -7}, new int[] {4, -1});
		run(helper, "cable_industrial", new int[] {7, -7}, new int[] {7, -1});
		run(helper, "cable_industrial", new int[] {9, -7}, new int[] {9, -2});
		PotashDissolverBlockEntity dissolver = entity(helper, 0, 0, PotashDissolverBlockEntity.class);
		dissolver.slots().set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.SYLVITE), 16));
		BaggingStationBlockEntity bagger = entity(helper, 10, -2, BaggingStationBlockEntity.class);
		bagger.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 4));
		PotashDryerBlockEntity dryer = entity(helper, 7, 0, PotashDryerBlockEntity.class);
		int[][] wet = {{0, 0}};
		water(helper, wet);
		int[][] staged = {{0, 0}, {7, 0}, {4, 0}, {10, -2}};   // the dissolver, then the dryer, the crystallizer and the bagging station
		hold(helper, staged);
		start(helper, new int[][] {{-15, -8}}, () -> release(helper, staged, 0, () -> await(helper, 600, wet, () -> dissolver.making() > 0 && towerSalt(helper, tower), () -> {
			String chain = " | dissolver " + read(helper, 0, 0) + " | crystallizer " + read(helper, 4, 0) + " | dryer " + read(helper, 7, 0) + " | tower " + towerRead(helper, tower) + " | set " + read(helper, -15, -8);
			check(helper, near(dissolver.making(), Phosphate.mopKgs() * 3600, 0.10), "the plant should make about 5 t/h of MOP: " + dissolver.making() + chain);
			check(helper, towerSalt(helper, tower), "salt tails should reach the cell: " + chain);
			log("potash", "running:" + chain);
			dryer.testAddMop(Tuning.BULK_BAG_KG - 20);
			await(helper, 300, wet, () -> bagger.slots().stream().anyMatch(s -> s.is(WildcatterItems.MOP_BAG)), () -> {
				check(helper, bagger.slots().stream().anyMatch(s -> s.is(WildcatterItems.MOP_BAG)), "a MOP bag should be filled: " + read(helper, 10, -2) + chain);
				log("potash", "ok: " + read(helper, 10, -2));
				helper.succeed();
			});
		})));
	}

	private static boolean towerSalt(GameTestHelper helper, BlockPos tower) {
		return towerRead(helper, tower).contains("t salt");
	}

	// ---- phosphate_uranium --------------------------------------------------------------------------------------------------------------
	static void phosphateUranium(GameTestHelper helper) {
		floor(helper, -14, -10, 20, 18);
		place(helper, WildcatterBlocks.ACID_PRETREATMENT, 0, 0);         // x 0-2, z 0-5
		place(helper, WildcatterBlocks.DEPA_TOPO_CIRCUIT, 3, 0);         // x 3-18, z 0-5, against its east side
		place(helper, WildcatterBlocks.STOCK_TANK, 4, -5);               // x 4-7, z -5..-2: the kerosene
		place(helper, WildcatterBlocks.ACID_EVAPORATOR, 3, 9);           // x 3-5, z 9-11
		place(helper, WildcatterBlocks.STOCK_TANK, 3, 13);               // x 3-6, z 13-16: the merchant acid
		run(helper, "gas_line", new int[] {1, -1});
		source(helper, 1, -2, Product.FILTER_ACID, 1.0);                                                    // the pan filter's acid
		run(helper, "gas_line", new int[] {5, -1});                                                         // kerosene
		run(helper, "gas_line", new int[] {4, 6}, new int[] {4, 8});                                         // treated acid to the evaporator
		run(helper, "gas_line", new int[] {6, 10});
		source(helper, 7, 10, Product.PROCESS_STEAM, 1.0);
		run(helper, "gas_line", new int[] {4, 12});                                                         // merchant acid to its tank
		genset(helper, -6, -8);
		run(helper, "cable_industrial", new int[] {-5, -8}, new int[] {3, -8}, new int[] {3, -1});
		run(helper, "cable_industrial", new int[] {-1, -7}, new int[] {-1, 9}, new int[] {2, 9});
		entity(helper, 4, -5, StorageTankBlockEntity.class).testFill(Product.KEROSENE, 10_000);
		DepaTopoCircuitBlockEntity circuit = entity(helper, 3, 0, DepaTopoCircuitBlockEntity.class);
		circuit.slots().set(1, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2));
		AcidEvaporatorBlockEntity evaporator = entity(helper, 3, 9, AcidEvaporatorBlockEntity.class);
		int[][] wet = {};
		int[][] staged = {{3, 9}};
		hold(helper, staged);
		start(helper, new int[][] {{-6, -8}}, () -> await(helper, 200, wet, () -> read(helper, 0, 0).contains("FILLING"), () -> {
			String filling = read(helper, 0, 0);
			check(helper, filling.contains("FILLING") && filling.contains("passing"), "without its drums the circuit should read FILLING and the acid go round it: " + filling);
			circuit.slots().set(0, new ItemStack(WildcatterItems.EXTRACTANT_DRUM, Phosphate.chargeDrums() + 1));
			helper.runAfterDelay(200, () -> release(helper, staged, 0, () -> {}));   // the evaporator once the circuit's motors are up
			await(helper, 600, wet, () -> circuit.making() > 0 && evaporator.making() > 0, () -> {
				String chain = " | pretreatment " + read(helper, 0, 0) + " | circuit " + read(helper, 3, 0) + " | evaporator " + read(helper, 3, 9) + " | set " + read(helper, -6, -8);
				check(helper, circuit.charged(), "the circuit should hold its first charge: " + chain);
				check(helper, near(circuit.making(), Phosphate.uraniumKgh(), 0.10), "the circuit should take about 2.8 kg/h of uranium: " + circuit.making() + chain);
				check(helper, circuit.uranium() > 0, "uranium should be in the strip: " + chain);
				check(helper, evaporator.making() > 0, "the treated acid should reach the evaporator: " + chain);
				log("phosphate_uranium", "running:" + chain);
				circuit.slots().set(1, ItemStack.EMPTY);
				circuit.testDrainIron();
				await(helper, 100, wet, () -> read(helper, 0, 0).contains("NO IRON"), () -> {
					String noIron = read(helper, 0, 0);
					check(helper, noIron.contains("NO IRON") && (noIron.contains("passing") || noIron.contains("bypass")), "without iron the circuit should stop and the acid go round it: " + noIron);
					circuit.slots().set(1, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 1));
					circuit.testAddUranium(Tuning.DEPA_TOPO_U_HOLD_KG);
					await(helper, 100, wet, () -> read(helper, 0, 0).contains("FULL"), () -> {
						String full = read(helper, 0, 0);
						check(helper, full.contains("FULL"), "with its strip full the circuit should read FULL: " + full);
						log("phosphate_uranium", "ok: " + filling + " | " + noIron + " | " + full);
						helper.succeed();
					});
				});
			});
		}));
	}
}
