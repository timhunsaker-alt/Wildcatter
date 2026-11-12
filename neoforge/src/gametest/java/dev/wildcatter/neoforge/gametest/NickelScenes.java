package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.genset;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.start;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BaggingStationBlockEntity;
import dev.wildcatter.block.ChlorineLeachReactorBlockEntity;
import dev.wildcatter.block.FluidBedRoasterBlockEntity;
import dev.wildcatter.block.FurnaceTransformerBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.NickelFurnaceBlockEntity;
import dev.wildcatter.block.NickelTankhouseBlockEntity;
import dev.wildcatter.block.PeirceSmithConverterBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.fluid.Nickel;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Phase 17b1's scenes (docs/multiblocks/nickel-smelter.md, "Game tests"):
 * <ul>
 * <li>{@code nickel_smelter}: the 17a scene's peaker and substation give 12.47 kV to a furnace transformer; the nickel electric furnace
 * against it, the fluid-bed roaster (ore by hopper) and the Peirce-Smith converter against the furnace, each blower on its own diesel set
 * stepped up to 4.16 kV; their off-gas on one pipe to an acid plant's converter. The roaster roasts 2 t/h and its gas makes acid; the
 * furnace draws about 1 MW and makes matte; the converter blows Bessemer matte; the acid plant's converter broken, NO ACID PLANT.</li>
 * <li>{@code nickel_refinery}: the leach reactor (Bessemer matte bags by hopper, chlorine from a stand-in supply), the purification
 * circuit (kerosene from a stand-in supply) and the tankhouse, a rectifier at its south end on a 1 MW gas set stepped up to 4.16 kV: the
 * tankhouse holds 12.3 kA at about 63 V and a bagging station fills a bag of cathode nickel a hook has brought near full; the rectifier
 * broken, NO DC, and the leach reads NO CHLORINE.</li>
 * <li>{@code melt_shop} (17b2): the induction furnace on two 1 MW gas sets in parallel stepped up to 4.16 kV, a 304 heat's FeCr, nickel,
 * FeMn and iron by hopper; the AOD converter with a PSA oxygen generator against it and nitrogen from a stand-in supply; the ingot caster
 * against the AOD: twenty 304 ingots in the hopper under it.</li>
 * </ul>
 */
final class NickelScenes {
	private NickelScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("nickel_smelter", NickelScenes::smelter);
		scenes.put("nickel_refinery", NickelScenes::refinery);
		scenes.put("melt_shop", NickelScenes::meltShop);
		return scenes;
	}

	static void hopper(GameTestHelper helper, int x, int y, int z, ItemStack... stacks) {
		helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.HOPPER.defaultBlockState(), 3);
		HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
		for (int i = 0; i < stacks.length; i++) {
			hopper.setItem(i, stacks[i]);
		}
	}

	static int count(BaggingStationBlockEntity station, Item item) {
		int n = 0;
		for (ItemStack s : station.slots()) {
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		return n;
	}

	/** Lays {@code id} along the straight legs between the points (x, z) at y U. */
	static void run(GameTestHelper helper, String id, int[]... points) {
		for (int i = 0; i + 1 < points.length; i++) {
			int[] a = points[i], b = points[i + 1];
			int dx = Integer.signum(b[0] - a[0]), dz = Integer.signum(b[1] - a[1]);
			for (int x = a[0], z = a[1]; ; x += dx, z += dz) {
				put(helper, id, x, U, z);
				if (x == b[0] && z == b[1]) {
					break;
				}
			}
		}
	}

	/** A 500 kW diesel set (panel at x, z), a unit substation run backward off its 480 V: 4.16 kV out at (x+5, z). */
	static void mvSet(GameTestHelper helper, int x, int z) {
		genset(helper, x, U, z);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, x + 4, U, z, Direction.SOUTH);
	}

	/** A 1 MW gas set with its panel at (x, z) facing north, fed by a stand-in pipeline gas supply north of its engine; its panel. */
	static BlockPos gasSet(GameTestHelper helper, int x, int z) {
		BlockPos panelAt = abs(helper, x, U, z);
		dev.wildcatter.block.GensetSkid.build(helper.getLevel(), panelAt, NORTH, true);
		BlockPos engine = null;
		for (BlockPos cell : BlockPos.betweenClosed(panelAt.offset(-10, 0, -3), panelAt.offset(2, 0, 4))) {
			if (BigParts.entity(helper.getLevel(), cell) instanceof dev.wildcatter.block.GensetPartBlockEntity part
					&& part.part() == dev.wildcatter.block.GensetPartBlock.Part.GAS_ENGINE && (engine == null || cell.getZ() < engine.getZ())) {
				engine = cell.immutable();
			}
		}
		check(helper, engine != null, "the gas set should have a gas engine");
		helper.getLevel().setBlock(engine.north(), BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("gas_line")).defaultBlockState(), 3);
		BlockPos src = engine.north(2);
		helper.getLevel().setBlock(src, BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity gas = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(src);
		gas.product = Product.PIPELINE_GAS;
		gas.rate = 1.0;
		return panelAt;
	}

	static void startGasSet(GameTestHelper helper, BlockPos panelAt) {
		helper.runAfterDelay(30, () -> ((dev.wildcatter.block.GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(panelAt)).setWantRun(true));
	}

	static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, U, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, U, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	// ---- nickel_smelter -------------------------------------------------------------------------------------------------------------
	static void smelter(GameTestHelper helper) {
		String scene = "nickel_smelter";
		floor(helper, -2, -26, 74, 22);
		// the peaker and its substation, as the ferroalloys scene: 12.47 kV north along x 43 to the furnace transformer's box
		place(helper, WildcatterBlocks.AERO_TURBINE, 20, U, 0, NORTH);
		place(helper, WildcatterBlocks.AERO_INLET_HOUSE, 20, U, -5, NORTH);
		place(helper, WildcatterBlocks.AERO_EXHAUST_STACK, 20, U, 14, NORTH);
		place(helper, WildcatterBlocks.AERO_GENERATOR, 25, U, 1, NORTH);
		place(helper, WildcatterBlocks.GEN_BREAKER_3000, 26, U, 8, NORTH);
		place(helper, WildcatterBlocks.GSU_60, 36, U, 8, NORTH);
		place(helper, WildcatterBlocks.SUBSTATION_TRANSFORMER_60, 36, U, -6, NORTH);
		place(helper, WildcatterBlocks.STOCK_TANK, 14, U, 4, NORTH);                 // kerosene for the turbine
		put(helper, "gas_line", 18, U, 6);
		put(helper, "gas_line", 19, U, 6);
		for (int x = 26; x <= 37; x++) {
			put(helper, "bus_duct_gen", x, U, 10);
		}
		put(helper, "bus_duct_gen", 36, U, 9);
		for (int z = -10; z <= 4; z++) {
			put(helper, "cable_hv", 36, U + 6, z);
		}
		genset(helper, 10, U, 2);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 14, U, 2, Direction.SOUTH);
		for (int x = 15; x <= 19; x++) {
			put(helper, "cable_mv", x, U, 2);
		}
		for (int x = 36; x <= 43; x++) {
			put(helper, "cable_dist", x, U, -5);
		}
		for (int z = -20; z <= -6; z++) {
			put(helper, "cable_dist", 43, U, z);
		}
		// the smelter: the transformer west of the furnace, the roaster against its south end, the converter against its east side
		place(helper, WildcatterBlocks.FURNACE_TRANSFORMER, 44, U, -20, NORTH);        // x 44-47, z -20..-16, box (44, -20) west
		place(helper, WildcatterBlocks.NICKEL_ELECTRIC_FURNACE, 48, U, -20, NORTH);    // x 48-53, z -20..-9
		place(helper, WildcatterBlocks.FLUID_BED_ROASTER, 48, U, -8, NORTH);           // x 48-51, z -8..-5, box (48, -8) west, gas out (49, -5) south
		place(helper, WildcatterBlocks.PEIRCE_SMITH_CONVERTER, 54, U, -22, NORTH);     // x 54-58, z -22..-13, box (54, -22) west, gas out (56, -13) south
		// the acid plant: its converter, final absorber and a stock tank (as the alumino scene's), the off-gas pipe to its front
		place(helper, WildcatterBlocks.CONVERTER, 62, U, 0, Direction.WEST);           // x 62-66, z -4..0
		place(helper, WildcatterBlocks.FINAL_ABSORBER, 62, U, -5, Direction.WEST);     // x 62-64, z -7..-5
		place(helper, WildcatterBlocks.STOCK_TANK, 66, U, -6, Direction.WEST);         // x 66-69, z -9..-6
		put(helper, "gas_line", 65, U, -6);
		run(helper, "gas_line", new int[] {49, -4}, new int[] {56, -4});
		run(helper, "gas_line", new int[] {56, -12}, new int[] {56, -1}, new int[] {61, -1});
		entity(helper, 62, U, -5, WaterUser.class).addWater(100_000);
		// MV for the blowers: set A (roaster) south of the plant, round to the roaster's box along z 14 and x 47; set B (converter) round x 72 and z -23
		mvSet(helper, 52, 16);                                                         // skid x 47-52, z 16-17; 4.16 kV out at (57, 16)
		run(helper, "cable_mv", new int[] {57, 16}, new int[] {57, 14}, new int[] {47, 14}, new int[] {47, -8});
		BlockPos setB = gasSet(helper, 66, 16);                                        // a 1 MW gas set: the blower's 250 kW start
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 70, U, 16, Direction.SOUTH);   // 4.16 kV out at (71, 16)
		run(helper, "cable_mv", new int[] {71, 16}, new int[] {72, 16}, new int[] {72, -23}, new int[] {53, -23}, new int[] {53, -22});
		hopper(helper, 49, U + 10, -7, new ItemStack(OreBlocks.MINERALS.get(Ore.PENTLANDITE), 4));
		hopper(helper, 50, U + 6, -15, new ItemStack(WildcatterItems.GREEN_ANODE, 2));
		entity(helper, 14, U, 4, StorageTankBlockEntity.class).testFill(Product.KEROSENE, 40_000);
		FluidBedRoasterBlockEntity roaster = entity(helper, 48, U, -8, FluidBedRoasterBlockEntity.class);
		NickelFurnaceBlockEntity furnace = entity(helper, 48, U, -20, NickelFurnaceBlockEntity.class);
		PeirceSmithConverterBlockEntity converter = entity(helper, 54, U, -22, PeirceSmithConverterBlockEntity.class);
		FurnaceTransformerBlockEntity tx = entity(helper, 44, U, -20, FurnaceTransformerBlockEntity.class);
		GasTurbineBlockEntity gt = entity(helper, 20, U, 0, GasTurbineBlockEntity.class);
		roaster.testAddCalcine(3_000);   // an hour and a half of calcine already made, so the furnace starts with the peaker
		start(helper, 52, U, 16, () -> { });
		startGasSet(helper, setB);
		start(helper, 10, U, 2, () -> {
			gt.setWantRun(true);
			helper.runAfterDelay(200, () -> {
				gt.skipStartForTest(false);
				helper.runAfterDelay(100, () -> {
					gt.skipRampForTest();
					helper.runAfterDelay(400, () -> {
						String chain = " | roaster " + read(helper, 48, U, -8) + " | furnace " + read(helper, 48, U, -20) + " | transformer " + read(helper, 44, U, -20)
								+ " | converter " + read(helper, 54, U, -22) + " | acid " + read(helper, 62, U, 0);
						log(scene, "running:" + chain);
						check(helper, Math.abs(roaster.making() - Tuning.NI_ROASTER_FEED_KGH) < Tuning.NI_ROASTER_FEED_KGH * 0.05, "the roaster should roast 2 t/h: " + roaster.making() + chain);
						check(helper, read(helper, 62, U, 0).contains("converted"), "the acid plant's converter should take the burner gas: " + chain);
						double mw = tx.load().watts() / 1e6;
						check(helper, mw > 0.9 && mw < 1.1, "the furnace should draw about 1 MW: " + mw + chain);
						check(helper, furnace.making() > 1800 && furnace.matte() > 0, "the furnace should smelt about 2 t/h of calcine into matte: " + furnace.making() + chain);
						// two hours of the furnace's matte, as if made: the converter charges and blows
						furnace.testAddMatteNi(1_000);
						helper.runAfterDelay(200, () -> {
							String blowing = " | converter " + read(helper, 54, U, -22) + " | acid " + read(helper, 62, U, 0) + " | set B " + read(helper, 66, U, 16)
									+ " | sub B " + read(helper, 70, U, 16) + " | cable " + read(helper, 53, U, -22);
							log(scene, "blowing:" + blowing);
							check(helper, converter.blowing() && converter.gasRate() > 0, "the converter should blow, its off-gas to the acid plant: " + blowing);
							converter.testFinishBlow();
							helper.runAfterDelay(40, () -> {
								String blown = read(helper, 54, U, -22);
								log(scene, "blown: " + blown);
								check(helper, converter.bessemer() > 1_000, "the blow should leave Bessemer matte: " + blown);
								// the acid plant's converter broken: the roaster's gas has nowhere to go
								helper.getLevel().destroyBlock(abs(helper, 62, U, 0), false);
								helper.runAfterDelay(200, () -> {
									String stopped = read(helper, 48, U, -8);
									log(scene, "ok: " + stopped);
									check(helper, BigParts.entity(helper.getLevel(), abs(helper, 62, U, 0)) == null, "the acid plant's converter should be gone");
									check(helper, stopped.contains("NO ACID PLANT") && roaster.making() == 0, "with nowhere for its gas the roaster should stop: " + stopped);
									helper.succeed();
								});
							});
						});
					});
				});
			});
		});
	}

	// ---- nickel_refinery ----------------------------------------------------------------------------------------------------------
	static void refinery(GameTestHelper helper) {
		String scene = "nickel_refinery";
		floor(helper, -30, -8, 12, 24);
		place(helper, WildcatterBlocks.CHLORINE_LEACH_REACTOR, 0, U, 0, NORTH);       // x 0-2, z 0-2, box (0, 0) west, chlorine on its north
		place(helper, WildcatterBlocks.NICKEL_TANKHOUSE, -3, U, 3, NORTH);            // x -3..2, z 3-16, terminals (-2, 16) south
		place(helper, WildcatterBlocks.NI_PURIFICATION, 3, U, 1, NORTH);              // x 3-6, z 1-10, box (3, 1) north, kerosene east
		place(helper, WildcatterBlocks.RECTIFIER, -3, U, 17, NORTH);                  // x -3..-1, z 17-20, box (-3, 17) west
		place(helper, WildcatterBlocks.BAGGING_STATION, 4, U, 13, Direction.SOUTH);   // x 3-4, z 12-13, against the tankhouse's east side; box (5, 13) east
		// chlorine and kerosene from stand-in supplies
		put(helper, "gas_line", 1, U, -1);
		source(helper, 1, -2, Product.CHLORINE, 1.0);
		put(helper, "gas_line", 7, U, 2);
		source(helper, 7, 3, Product.KEROSENE, 200.0);
		hopper(helper, 1, U + 8, 1, new ItemStack(WildcatterItems.BESSEMER_MATTE_BAG, 4));
		// LV: a diesel set north, along z -4 to the leach's box and the purification's, down x 8 to the bagging station's
		genset(helper, -6, U, -4);                                                    // skid x -11..-6, z -4..-3; 480 V at (-5, -4)
		run(helper, "cable_industrial", new int[] {-5, -4}, new int[] {8, -4}, new int[] {8, 13}, new int[] {5, 13});
		run(helper, "cable_industrial", new int[] {-1, -3}, new int[] {-1, 0});
		run(helper, "cable_industrial", new int[] {3, -3}, new int[] {3, 0});
		// MV: a 1 MW gas set stepped up to 4.16 kV, to the rectifier's box
		BlockPos panelAt = gasSet(helper, -14, 17);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, -10, U, 17, Direction.SOUTH);  // 4.16 kV out at (-9, 17)
		run(helper, "cable_mv", new int[] {-9, 17}, new int[] {-4, 17});
		BaggingStationBlockEntity station = entity(helper, 4, U, 13, BaggingStationBlockEntity.class);
		station.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 4));
		ChlorineLeachReactorBlockEntity leach = entity(helper, 0, U, 0, ChlorineLeachReactorBlockEntity.class);
		NickelTankhouseBlockEntity tankhouse = entity(helper, -3, U, 3, NickelTankhouseBlockEntity.class);
		start(helper, -6, U, -4, () -> { });
		startGasSet(helper, panelAt);
		helper.runAfterDelay(30, () -> {
			helper.runAfterDelay(700, () -> {
				String chain = " | leach " + read(helper, 0, U, 0) + " | purification " + read(helper, 3, U, 1) + " | tankhouse " + read(helper, -3, U, 3)
						+ " | rectifier " + read(helper, -3, U, 17) + " | gas set " + read(helper, -14, U, 17);
				log(scene, "leaching:" + chain);
				check(helper, leach.making() > 0, "the leach reactor should leach Bessemer matte with chlorine: " + chain);
				check(helper, !read(helper, 3, U, 1).contains("NO KEROSENE"), "the purification should have its kerosene charge: " + chain);
				tankhouse.testAddLiquor(200);   // an hour of pure liquor, as if purified, so the cells run at their full current
				helper.runAfterDelay(400, () -> {
					String dc = " | tankhouse " + read(helper, -3, U, 3) + " | rectifier " + read(helper, -3, U, 17) + " | gas set " + read(helper, -14, U, 17);
					log(scene, "dc:" + dc);
					check(helper, Math.abs(tankhouse.amps() - Tuning.NI_CELL_KA * 1000) < 300, "the rectifier should hold 12.3 kA through the cells: " + tankhouse.amps() + dc);
					check(helper, Math.abs(tankhouse.volts() - Nickel.tankhouseVolts()) < 4, "the tankhouse should read about 63 V: " + tankhouse.volts() + dc);
					check(helper, tankhouse.making() > 200, "it should win about 230 kg/h of nickel: " + tankhouse.making() + dc);
					tankhouse.testFillCathode(Tuning.BULK_BAG_KG - 0.2);
					helper.runAfterDelay(200, () -> {
						int bags = count(station, WildcatterItems.NICKEL_CATHODE_BAG);
						String bagged = " | station " + read(helper, 4, U, 13) + " | tankhouse " + read(helper, -3, U, 3);
						log(scene, "bagged:" + bagged);
						check(helper, bags >= 1, "the bagging station should fill a bag of cathode nickel: " + bags + bagged);
						helper.getLevel().destroyBlock(abs(helper, -3, U, 17), false);   // the rectifier broken: no DC
						helper.runAfterDelay(200, () -> {
							String noDc = " | tankhouse " + read(helper, -3, U, 3) + " | leach " + read(helper, 0, U, 0);
							log(scene, "ok:" + noDc);
							check(helper, read(helper, -3, U, 3).contains("NO DC"), "without its rectifier the tankhouse should read NO DC: " + noDc);
							check(helper, read(helper, 0, U, 0).contains("NO CHLORINE"), "its chlorine not coming back, the leach should read NO CHLORINE: " + noDc);
							helper.succeed();
						});
					});
				});
			});
		});
	}

	// ---- melt_shop ----------------------------------------------------------------------------------------------------------------
	static void meltShop(GameTestHelper helper) {
		String scene = "melt_shop";
		floor(helper, -14, -20, 26, 14);
		place(helper, WildcatterBlocks.INDUCTION_FURNACE, 0, U, 0, NORTH);           // x 0-2, z 0-2, box (0, 0) west, MV
		place(helper, WildcatterBlocks.AOD_CONVERTER, 0, U, 3, NORTH);               // x 0-2, z 3-5, box (0, 3) west, nitrogen east
		place(helper, WildcatterBlocks.INGOT_CASTER, 0, U, 6, NORTH);                // x 0-1, z 6-11
		place(helper, WildcatterBlocks.OXYGEN_GENERATOR, -3, U, 4, NORTH);           // x -3..-1, z 4-9, against the AOD's west side; box (-4, 4)
		helper.getLevel().setBlock(abs(helper, 0, U - 1, 7), Blocks.HOPPER.defaultBlockState(), 3);   // under the caster
		put(helper, "gas_line", 3, U, 4);
		source(helper, 4, 4, Product.NITROGEN, 1.0);
		// LV: a diesel set for the AOD and the oxygen generator
		genset(helper, -6, U, -4);                                                   // 480 V at (-5, -4)
		run(helper, "cable_industrial", new int[] {-5, -4}, new int[] {-5, 3}, new int[] {-1, 3});
		put(helper, "cable_industrial", -4, U, 4);
		// MV: two 1 MW gas sets in parallel, each stepped up to 4.16 kV, along x 20 and z -1 to the furnace's box
		BlockPos setA = gasSet(helper, 14, -8);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 18, U, -8, Direction.SOUTH);  // 4.16 kV out at (19, -8)
		BlockPos setB = gasSet(helper, 14, -16);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 18, U, -16, Direction.SOUTH); // 4.16 kV out at (19, -16)
		run(helper, "cable_mv", new int[] {19, -16}, new int[] {20, -16}, new int[] {20, -1}, new int[] {-1, -1}, new int[] {-1, 0});
		put(helper, "cable_mv", 19, U, -8);
		dev.wildcatter.block.InductionFurnaceBlockEntity furnace = entity(helper, 0, U, 0, dev.wildcatter.block.InductionFurnaceBlockEntity.class);
		dev.wildcatter.block.AodConverterBlockEntity aod = entity(helper, 0, U, 3, dev.wildcatter.block.AodConverterBlockEntity.class);
		dev.wildcatter.block.IngotCasterBlockEntity caster = entity(helper, 0, U, 6, dev.wildcatter.block.IngotCasterBlockEntity.class);
		start(helper, -6, U, -4, () -> { });
		startGasSet(helper, setA);
		helper.runAfterDelay(300, () -> startGasSet(helper, setB));
		// the charge once both sets are on the bus: the furnace's 1.5 MW is more than one set carries
		helper.runAfterDelay(700, () -> hopper(helper, 1, U + 3, 1, new ItemStack(WildcatterItems.FERROCHROME_BAG, 1), new ItemStack(WildcatterItems.NICKEL_CATHODE_BAG, 1),
				new ItemStack(WildcatterItems.FERROMANGANESE_BAG, 1), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 13)));
		helper.runAfterDelay(1100, () -> {
			String chain = " | furnace " + read(helper, 0, U, 0) + " | set A " + read(helper, 14, U, -8) + " | set B " + read(helper, 14, U, -16);
			log(scene, "melting:" + chain);
			check(helper, furnace.melting(), "the hopper's 304 charge should be melting: " + chain);
			double mw = furnace.load().watts() / 1e6;
			check(helper, mw > 1.4 && mw < 1.6, "the induction furnace should draw about 1.5 MW: " + mw + chain);
			furnace.testFinishMelt();
			helper.runAfterDelay(300, () -> {
				String blowing = " | aod " + read(helper, 0, U, 3) + " | oxygen " + read(helper, -3, U, 4) + " | furnace " + read(helper, 0, U, 0);
				log(scene, "blowing:" + blowing);
				check(helper, aod.o2Rate() > 40 && aod.n2Rate() > 10, "the AOD should blow oxygen and nitrogen: " + blowing);
				check(helper, aod.carbonShare() > 0.0008, "the heat should still carry its ferrochrome's carbon: " + blowing);
				aod.testNearlyDone();
				helper.runAfterDelay(100, () -> {
					String pouring = " | aod " + read(helper, 0, U, 3) + " | caster " + read(helper, 0, U, 6);
					log(scene, "pouring:" + pouring);
					check(helper, caster.pouring(), "the caster should take the finished heat: " + pouring);
					caster.testFinishPour();
					helper.runAfterDelay(240, () -> {   // the hopper under it takes an ingot every 8 ticks
						HopperBlockEntity under = (HopperBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 0, U - 1, 7));
						int n = 0;
						for (int i = 0; i < under.getContainerSize(); i++) {
							if (under.getItem(i).is(WildcatterItems.STAINLESS_304_INGOT)) {
								n += under.getItem(i).getCount();
							}
						}
						String cast = " | caster " + read(helper, 0, U, 6);
						log(scene, "ok: " + n + " ingots" + cast);
						check(helper, n == 20, "twenty 304 ingots should be in the hopper under the caster: " + n + cast);
						helper.succeed();
					});
				});
			});
		});
	}
}
