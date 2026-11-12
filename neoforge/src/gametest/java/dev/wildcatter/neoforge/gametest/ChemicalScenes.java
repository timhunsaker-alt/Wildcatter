package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.air;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.genset;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.near;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.start;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BrineSaturatorBlockEntity;
import dev.wildcatter.block.BrineTreatmentBlockEntity;
import dev.wildcatter.block.BrineWellheadBlock;
import dev.wildcatter.block.BrineWellheadBlockEntity;
import dev.wildcatter.block.CausticEvaporatorBlockEntity;
import dev.wildcatter.block.ChlorineFillerBlockEntity;
import dev.wildcatter.block.ChlorineTrainBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.HclSynthesisBlockEntity;
import dev.wildcatter.block.InjectionPumpBlockEntity;
import dev.wildcatter.block.MembraneCellRoomBlockEntity;
import dev.wildcatter.block.RectifierBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Phase 20's scenes (docs/multiblocks/chlor-alkali-plant.md, "Game tests"). Every part faces north at y 2, its main cell at its
 * north-west bottom corner; test sources stand in for liquid CO2, sulfuric acid and process steam.
 * <ul>
 * <li>{@code chlor_alkali}: a 45 MW aeroderivative through its GSU and a 138/12.47 kV substation transformer to the rectifier_mv against
 * the north end of the membrane cell room; the brine treatment between the saturator (rock salt) and the room; the chlorine train with a
 * chlorine filler against it, the caustic evaporator and the HCl unit. The rectifier reads about 161 V and 2.4 MW; about a tonne of
 * chlorine an hour; caustic, hydrogen, 50 % caustic and 32 % HCl flow; a ton container (near full by hook) fills; the train broken, WET
 * CHLORINE.</li>
 * <li>{@code brine_well}: a cased hole into a placed rock salt column, a brine wellhead, an injection pump on a diesel set given fresh
 * water: brine reaches a saturator, and (the salt brought near a block's worth by hook) the rock salt at the hole's bottom becomes water.</li>
 * </ul>
 */
final class ChemicalScenes {
	private ChemicalScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("chlor_alkali", ChemicalScenes::chlorAlkali);
		scenes.put("brine_well", ChemicalScenes::brineWell);
		return scenes;
	}

	// ---- helpers --------------------------------------------------------------------------------------------------------------------
	/** Straight runs of {@code id} at y U between the corners given, each run including both ends. */
	static void run(GameTestHelper helper, String id, int[]... corners) {
		for (int i = 0; i + 1 < corners.length; i++) {
			int[] a = corners[i];
			int[] b = corners[i + 1];
			int dx = Integer.signum(b[0] - a[0]);
			int dz = Integer.signum(b[1] - a[1]);
			for (int x = a[0], z = a[1];; x += dx, z += dz) {
				put(helper, id, x, U, z);
				if (id.equals("cable_industrial") && helper.getLevel().getBlockEntity(abs(helper, x, U, z)) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
					cable.setSize(dev.wildcatter.energy.ConductorSize.KCMIL_500);
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

	static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, U, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, U, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	/** A 500 kW set with a unit substation run backward off it: 4.16 kV out at (x+5, z). */
	static void mvSet(GameTestHelper helper, int x, int z) {
		genset(helper, x, U, z);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, x + 4, U, z, Direction.SOUTH);
	}

	static void water(GameTestHelper helper, int[]... at) {
		for (int[] p : at) {
			entity(helper, p[0], U, p[1], WaterUser.class).addWater(100_000);
		}
	}

	/** Polls {@code done} every 20 ticks for up to {@code max} ticks, topping up the water users, then runs {@code then}. */
	static void await(GameTestHelper helper, int max, int[][] wet, BooleanSupplier done, Runnable then) {
		water(helper, wet);
		if (max <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(20, () -> await(helper, max - 20, wet, done, then));
	}

	/** The peaker of the ferroalloys scene, moved by (ox, oz): its 12.47 kV comes out of the substation transformer's throat at (ox+36, oz-5). */
	static GasTurbineBlockEntity peaker(GameTestHelper helper, int ox, int oz) {
		place(helper, WildcatterBlocks.AERO_TURBINE, ox + 20, U, oz, NORTH);
		place(helper, WildcatterBlocks.AERO_INLET_HOUSE, ox + 20, U, oz - 5, NORTH);
		place(helper, WildcatterBlocks.AERO_EXHAUST_STACK, ox + 20, U, oz + 14, NORTH);
		place(helper, WildcatterBlocks.AERO_GENERATOR, ox + 25, U, oz + 1, NORTH);
		place(helper, WildcatterBlocks.GEN_BREAKER_3000, ox + 26, U, oz + 8, NORTH);
		place(helper, WildcatterBlocks.GSU_60, ox + 36, U, oz + 8, NORTH);
		place(helper, WildcatterBlocks.SUBSTATION_TRANSFORMER_60, ox + 36, U, oz - 6, NORTH);
		place(helper, WildcatterBlocks.STOCK_TANK, ox + 14, U, oz + 4, NORTH);
		put(helper, "gas_line", ox + 18, U, oz + 6);
		put(helper, "gas_line", ox + 19, U, oz + 6);
		for (int x = 26; x <= 37; x++) {
			put(helper, "bus_duct_gen", ox + x, U, oz + 10);
		}
		put(helper, "bus_duct_gen", ox + 36, U, oz + 9);
		for (int z = -10; z <= 4; z++) {
			put(helper, "cable_hv", ox + 36, U + 6, oz + z);
		}
		genset(helper, ox + 10, U, oz + 2);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, ox + 14, U, oz + 2, Direction.SOUTH);
		for (int x = 15; x <= 19; x++) {
			put(helper, "cable_mv", ox + x, U, oz + 2);
		}
		entity(helper, ox + 14, U, oz + 4, StorageTankBlockEntity.class).testFill(Product.KEROSENE, 40_000);
		return entity(helper, ox + 20, U, oz, GasTurbineBlockEntity.class);
	}

	/** Starts the peaker's station set, then the turbine, its start and ramp skipped; {@code then} once it carries load. */
	static void startPeaker(GameTestHelper helper, int ox, int oz, GasTurbineBlockEntity gt, Runnable then) {
		start(helper, ox + 10, U, oz + 2, () -> {
			gt.setWantRun(true);
			helper.runAfterDelay(200, () -> {
				gt.skipStartForTest(false);
				helper.runAfterDelay(100, () -> {
					gt.skipRampForTest();
					helper.runAfterDelay(100, then);
				});
			});
		});
	}

	// ---- chlor_alkali ---------------------------------------------------------------------------------------------------------------
	static void chlorAlkali(GameTestHelper helper) {
		String scene = "chlor_alkali";
		floor(helper, -42, -36, 20, 40);
		GasTurbineBlockEntity gt = peaker(helper, -40, -20);
		// 12.47 kV from the throat (-4, -25) east, then south along x 3 to the rectifier's box
		run(helper, "cable_dist", new int[] {-4, -25}, new int[] {3, -25}, new int[] {3, -4});
		place(helper, WildcatterBlocks.BRINE_TREATMENT, 0, U, 0, NORTH);        // x 0-3, z 0-7, its box north (0, -1)
		place(helper, WildcatterBlocks.BRINE_SATURATOR, -4, U, 0, NORTH);       // x -4..-1, z 0-3, its box west (-5, 0)
		place(helper, WildcatterBlocks.MEMBRANE_CELL_ROOM, 4, U, 0, NORTH);     // x 4-9, z 0-15
		place(helper, WildcatterBlocks.RECTIFIER_MV, 4, U, -4, NORTH);          // x 4-6, z -4..-1, against the room's north end; box (3, -4)
		place(helper, WildcatterBlocks.CHLORINE_TRAIN, 0, U, 18, NORTH);        // x 0-3, z 18-27; MV box (-1, 18), LV box (0, 17)
		place(helper, WildcatterBlocks.CHLORINE_FILLER, 0, U, 28, NORTH);       // x 0-2, z 28-30, against the train's south end; box (-1, 28)
		place(helper, WildcatterBlocks.CAUSTIC_EVAPORATOR, 11, U, 18, NORTH);   // x 11-13, z 18-20
		place(helper, WildcatterBlocks.STOCK_TANK, 11, U, 23, NORTH);           // x 11-14, z 23-26: the 50 % caustic
		place(helper, WildcatterBlocks.HCL_SYNTHESIS_UNIT, 6, U, 30, NORTH);    // x 6-7, z 30-31
		// pipe
		run(helper, "gas_line", new int[] {1, 8});
		source(helper, 1, 9, Product.CO2, 0.1);                                                   // liquid CO2 to the treatment
		run(helper, "gas_line", new int[] {4, 16}, new int[] {4, 17}, new int[] {3, 17});         // wet chlorine to the train
		run(helper, "gas_line", new int[] {4, 19});
		source(helper, 4, 20, Product.SULFURIC_ACID, 0.1);                                        // drying acid
		run(helper, "gas_line", new int[] {6, 16}, new int[] {6, 29});                            // hydrogen to the HCl unit
		run(helper, "gas_line", new int[] {3, 28}, new int[] {3, 33}, new int[] {8, 33}, new int[] {8, 31});   // dry chlorine to the HCl unit
		run(helper, "gas_line", new int[] {8, 16}, new int[] {8, 17}, new int[] {11, 17});        // 32 % caustic to the evaporator
		run(helper, "gas_line", new int[] {14, 19});
		source(helper, 15, 19, Product.PROCESS_STEAM, 1.0);                                       // steam to the evaporator
		run(helper, "gas_line", new int[] {12, 21}, new int[] {12, 22});                          // 50 % to its tank
		// power: A (480 V) the saturator and the treatment; B (480 V) the liquefier; C (4.16 kV) the compressor; D (480 V) the filler
		genset(helper, -10, U, -6);
		run(helper, "cable_industrial", new int[] {-9, -6}, new int[] {-9, 0}, new int[] {-5, 0});
		run(helper, "cable_industrial", new int[] {-9, -1}, new int[] {0, -1});
		genset(helper, -12, U, 12);
		run(helper, "cable_industrial", new int[] {-11, 12}, new int[] {0, 12}, new int[] {0, 17});
		mvSet(helper, -20, 26);
		run(helper, "cable_mv", new int[] {-15, 26}, new int[] {-15, 19}, new int[] {-1, 19}, new int[] {-1, 18});
		genset(helper, -8, U, 34);
		run(helper, "cable_industrial", new int[] {-7, 34}, new int[] {-7, 28}, new int[] {-1, 28});
		// feeds
		BrineSaturatorBlockEntity saturator = entity(helper, -4, U, 0, BrineSaturatorBlockEntity.class);
		saturator.slots().set(0, new ItemStack(OreBlocks.MINERALS.get(Ore.HALITE), 16));
		ChlorineFillerBlockEntity filler = entity(helper, 0, U, 28, ChlorineFillerBlockEntity.class);
		filler.slots().set(0, new ItemStack(WildcatterItems.EMPTY_TON_CONTAINER, 4));
		int[][] wet = {{-4, 0}, {6, 30}};
		water(helper, wet);
		BrineTreatmentBlockEntity treatment = entity(helper, 0, U, 0, BrineTreatmentBlockEntity.class);
		MembraneCellRoomBlockEntity room = entity(helper, 4, U, 0, MembraneCellRoomBlockEntity.class);
		RectifierBlockEntity rectifier = entity(helper, 4, U, -4, RectifierBlockEntity.class);
		ChlorineTrainBlockEntity train = entity(helper, 0, U, 18, ChlorineTrainBlockEntity.class);
		CausticEvaporatorBlockEntity evaporator = entity(helper, 11, U, 18, CausticEvaporatorBlockEntity.class);
		HclSynthesisBlockEntity hcl = entity(helper, 6, U, 30, HclSynthesisBlockEntity.class);
		start(helper, -10, U, -6, () -> { });
		start(helper, -12, U, 12, () -> { });
		start(helper, -20, U, 26, () -> { });
		start(helper, -8, U, 34, () -> { });
		startPeaker(helper, -40, -20, gt, () -> await(helper, 1600, wet, () -> room.making() > 0.9 * ChlorAlkaliPlant.cl2Kgs() * 3600 && evaporator.making() > 0
				&& hcl.making() > 0 && train.liquefying() > 0, () -> {
			String chain = " | rectifier " + read(helper, 4, U, -4) + " | room " + read(helper, 4, U, 0) + " | treatment " + read(helper, 0, U, 0) + " | saturator "
					+ read(helper, -4, U, 0) + " | train " + read(helper, 0, U, 18) + " | evaporator " + read(helper, 11, U, 18) + " | hcl " + read(helper, 6, U, 30)
					+ " | filler " + read(helper, 0, U, 28) + " | gt " + read(helper, -20, U, -20);
			log(scene, "running:" + chain);
			double volts = rectifier.dc().volts();
			double mw = volts * rectifier.dc().amps() / 1e6;
			check(helper, near(volts, ChlorAlkaliPlant.dcVolts(), 0.05), "the rectifier should read about 161 V: " + volts + chain);
			check(helper, mw > 2.2 && mw < 2.6, "the rectifier should deliver about 2.4 MW: " + mw + chain);
			check(helper, near(room.making(), ChlorAlkaliPlant.cl2Kgs() * 3600, 0.10), "the room should make about a tonne of chlorine an hour: " + room.making() + chain);
			check(helper, treatment.making() > 0 && read(helper, 0, U, 0).contains("carbonate"), "the treatment should take the calcium out on CO2: " + chain);
			check(helper, evaporator.making() > 0, "the evaporator should make 50 % caustic: " + chain);
			check(helper, hcl.making() > 0 && read(helper, 6, U, 30).contains("32 % acid"), "the HCl unit should make 32 % acid: " + chain);
			check(helper, train.making() > 0, "the train should dry chlorine: " + chain);
			filler.addChlorine(Tuning.CL2_TON_KG - 3);
			await(helper, 400, wet, () -> filler.outCount() >= 1, () -> {
				String filled = read(helper, 0, U, 28);
				check(helper, filler.outCount() >= 1, "a ton container should fill from the liquefier: " + filled + " | train " + read(helper, 0, U, 18));
				log(scene, "filled: " + filled);
				helper.getLevel().destroyBlock(abs(helper, 0, U, 18), false);                    // the chlorine train broken
				await(helper, 200, wet, () -> read(helper, 4, U, 0).contains("WET CHLORINE"), () -> {
					String wetRead = read(helper, 4, U, 0);
					check(helper, wetRead.contains("WET CHLORINE"), "with the train gone the room should read WET CHLORINE: " + wetRead);
					log(scene, "ok: " + wetRead);
					helper.succeed();
				});
			});
		}));
	}

	// ---- brine_well -----------------------------------------------------------------------------------------------------------------
	static void brineWell(GameTestHelper helper) {
		String scene = "brine_well";
		floor(helper, -14, -10, 14, 12);
		// the salt: a column of rock salt 5 x 5, y 1-5; the hole cased through y 6-7, the wellhead at y 8 facing east
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				for (int y = 1; y <= 5; y++) {
					helper.getLevel().setBlock(abs(helper, x, y, z), OreBlocks.block(Ore.HALITE).defaultBlockState(), 3);
				}
			}
		}
		put(helper, "casing", 0, 6, 0);
		put(helper, "casing", 0, 7, 0);
		helper.getLevel().setBlock(abs(helper, 0, 8, 0), WildcatterBlocks.BRINE_WELLHEAD.defaultBlockState().setValue(BrineWellheadBlock.FACING, Direction.EAST), 3);
		// brine: east from the wellhead, down and on to the saturator's north side
		for (int x = 1; x <= 6; x++) {
			put(helper, "gas_line", x, 8, 0);
		}
		for (int y = 2; y <= 7; y++) {
			put(helper, "gas_line", 6, y, 0);
		}
		put(helper, "gas_line", 6, 2, 1);
		put(helper, "gas_line", 6, 2, 2);
		place(helper, WildcatterBlocks.BRINE_SATURATOR, 5, U, 3, NORTH);        // x 5-8, z 3-6
		// fresh water: the injection pump's discharge (on top of its z 3 cells, here world z -1) to the wellhead's west
		place(helper, WildcatterBlocks.INJECTION_PUMP, -6, U, -4, NORTH);       // x -6..-5, z -4..1
		for (int y = 3; y <= 8; y++) {
			put(helper, "gas_line", -6, y, -1);
		}
		put(helper, "gas_line", -6, 8, 0);
		for (int x = -5; x <= -1; x++) {
			put(helper, "gas_line", x, 8, 0);
		}
		genset(helper, -10, U, -7);
		run(helper, "cable_industrial", new int[] {-9, -7}, new int[] {-6, -7}, new int[] {-6, -5});
		BrineWellheadBlockEntity head = (BrineWellheadBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 0, 8, 0));
		InjectionPumpBlockEntity pump = entity(helper, -6, U, -4, InjectionPumpBlockEntity.class);
		BrineSaturatorBlockEntity saturator = entity(helper, 5, U, 3, BrineSaturatorBlockEntity.class);
		BlockPos bottom = abs(helper, 0, 5, 0);
		start(helper, -10, U, -7, () -> {
			head.testSalt(Ore.HALITE.kgPerBlock() - 20);
			feed(helper, pump, 600, () -> saturator.fromWell() > 0 && head.blocks() >= 1, () -> {
				String chain = " | head " + head.inspect().getString() + " | pump " + pump.inspect().getString() + " | saturator " + read(helper, 5, U, 3);
				log(scene, "running:" + chain);
				check(helper, saturator.fromWell() > 0, "brine should reach the saturator: " + chain);
				check(helper, helper.getLevel().getBlockState(bottom).is(Blocks.WATER), "the rock salt at the hole's bottom should be water: "
						+ helper.getLevel().getBlockState(bottom) + chain);
				log(scene, "ok");
				helper.succeed();
			});
		});
	}

	/** Gives the pump fresh water each second, as a water pump on its suction would, until {@code done}. */
	private static void feed(GameTestHelper helper, InjectionPumpBlockEntity pump, int max, BooleanSupplier done, Runnable then) {
		pump.addWater(2_000);
		if (max <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(20, () -> feed(helper, pump, max - 20, done, then));
	}
}
