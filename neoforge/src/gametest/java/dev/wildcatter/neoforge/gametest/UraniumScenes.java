package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.near;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.ConversionStageBlockEntity;
import dev.wildcatter.block.DrumFillerBlockEntity;
import dev.wildcatter.block.PrecipitationTankBlockEntity;
import dev.wildcatter.block.ReductionReactorBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.UraniumCalcinerBlockEntity;
import dev.wildcatter.fluid.Product;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Phase 16's server scenes (docs/ideas-plan.md): uranium routes and plant operations. Each builds its parts at true size facing north
 * at y 2 (main cell at the north-west bottom corner), with steel pipe, 480 V cable and a diesel set where a part has a motor, and test
 * sources (the stand-in {@link TestGasSource}) for the streams a whole upstream plant would make.
 * <ul>
 * <li>{@code adu_yellowcake} (16a, adu-yellowcake.md): strip liquor and liquid ammonia to the precipitation tank make ADU; the uranium
 * calciner against it fires up on LPG and fills the drum filler with U3O8 at 96 %, a drum of 326 kg of U. Ammonia cut: NO AMMONIA; with
 * magnesia it goes back to the magnesia route. A reduction reactor given U3O8 drums draws 5.65 g of hydrogen a kg of U, and the
 * hydrofluorinator after it takes no HF for magnesia.</li>
 * <li>{@code conversion_extensions} (16b1, conversion-extensions.md): the fluorine cell house's cathode hydrogen (its cells at full
 * current by hook) through a cathode gas scrubber, 28.6 Nm3/h, runs the reduction reactor with no reformer; a KOH scrubber charged with
 * caustic potash scrubs the hydrofluorinator's HF, bags its CaF2 as synthetic fluorspar, and without lime reads SPENT and stops the
 * hydrofluorinator; a fluorinator with a full bed (the rule on) reads BED FULL, and retired fills five ash drums and takes fluorspar for a
 * new bed; an ash drum in a leach tank adds its uranium to the slurry.</li>
 * </ul>
 */
final class UraniumScenes {
	private UraniumScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("adu_yellowcake", UraniumScenes::aduYellowcake);
		scenes.put("conversion_extensions", UraniumScenes::conversionExtensions);
		return scenes;
	}

	static final int U = 2;
	static final Direction NORTH = Direction.NORTH;

	// ---- helpers --------------------------------------------------------------------------------------------------------------------
	static void place(GameTestHelper helper, Block block, int x, int z) {
		GridSceneKit.place(helper, block, x, U, z, NORTH);
	}

	static void pipe(GameTestHelper helper, int[]... cells) {
		for (int[] c : cells) {
			GridSceneKit.put(helper, "gas_line", c[0], U + c[1], c[2]);
		}
	}

	static void cable(GameTestHelper helper, int x, int z) {
		GridSceneKit.put(helper, "cable_industrial", x, U, z);
	}

	static void air(GameTestHelper helper, int x, int z) {
		GridSceneKit.air(helper, x, U, z);
	}

	static <T> T entity(GameTestHelper helper, int x, int z, Class<T> kind) {
		return GridSceneKit.entity(helper, x, U, z, kind);
	}

	static String read(GameTestHelper helper, int x, int z) {
		return GridSceneKit.read(helper, x, U, z);
	}

	static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, U, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, U, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	static void fill(GameTestHelper helper, int x, int z, Product product, double amount) {
		entity(helper, x, z, StorageTankBlockEntity.class).testFill(product, amount);
	}

	static void genset(GameTestHelper helper, int x, int z) {
		GridSceneKit.genset(helper, x, U, z);
	}

	static void start(GameTestHelper helper, int x, int z, Runnable then) {
		GridSceneKit.start(helper, x, U, z, then);
	}

	/** A run of cable from (x1, z1) to (x2, z2), along x then z. */
	static void cableRun(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			cable(helper, x, z1);
		}
		for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
			cable(helper, x2, z);
		}
	}

	// ---- adu_yellowcake (16a) ------------------------------------------------------------------------------------------------------
	static void aduYellowcake(GameTestHelper helper) {
		floor(helper, -10, -8, 26, 19);
		place(helper, WildcatterBlocks.PRECIPITATION_TANK, 0, 0);     // x 0-3, z 0-3
		place(helper, WildcatterBlocks.URANIUM_CALCINER, 4, 0);       // x 4-6, z 0-2, against the tank
		place(helper, WildcatterBlocks.DRUM_FILLER, 7, -1);           // x 7-8, z -1..0, against the calciner
		place(helper, WildcatterBlocks.LPG_BULLET, 3, 6);             // x 3-5, z 6-17: the ammonia
		place(helper, WildcatterBlocks.LPG_BULLET, 7, 6);             // x 7-9, z 6-17: the calciner's LPG
		pipe(helper, new int[] {1, 0, -1});                                                               // strip liquor in, the tank's front
		TestGasSource.SourceEntity strip = source(helper, 1, -2, Product.STRIP_LIQUOR, 20.0);
		strip.mix[0] = Tuning.SX_STRIP_U3O8_G_PER_L / 1000;
		pipe(helper, new int[] {4, 0, 3}, new int[] {4, 0, 4}, new int[] {4, 0, 5});                     // ammonia to the tank's east side
		pipe(helper, new int[] {7, 0, 2}, new int[] {7, 0, 3}, new int[] {7, 0, 4}, new int[] {7, 0, 5});   // LPG to the calciner's burners
		fill(helper, 3, 6, Product.AMMONIA, 5_000);
		fill(helper, 7, 6, Product.LPG, 20_000);
		genset(helper, -4, -6);                                                                          // skid x -9..-4, z -6..-5
		cableRun(helper, -3, -6, -1, 0);                                                                 // the tank's box, west of its main cell
		cableRun(helper, -1, -6, 4, -1);                                                                 // the calciner's box, north of its main cell
		cableRun(helper, 4, -6, 6, -1);                                                                  // the filler's box, west of its main cell
		DrumFillerBlockEntity filler = entity(helper, 7, -1, DrumFillerBlockEntity.class);
		filler.slots().set(0, new ItemStack(WildcatterItems.EMPTY_DRUM, 4));
		filler.addU3o8(Tuning.DRUM_NET_KG - 2);                                                          // near full: the calciner finishes the drum
		// the conversion line's head, off to the east: a reduction reactor on U3O8 drums and the hydrofluorinator against it
		place(helper, WildcatterBlocks.REDUCTION_REACTOR, 20, 0);    // x 20-22, z 0-2
		place(helper, WildcatterBlocks.HYDROFLUORINATOR, 20, 3);     // x 20-22, z 3-8
		pipe(helper, new int[] {23, 0, 1});
		source(helper, 24, 1, Product.HYDROGEN, 1.0);
		pipe(helper, new int[] {23, 0, 5});
		source(helper, 24, 5, Product.ANHYDROUS_HF, 1.0);
		ReductionReactorBlockEntity reducer = entity(helper, 20, 0, ReductionReactorBlockEntity.class);
		reducer.slots().set(0, new ItemStack(WildcatterItems.U3O8_DRUM, 2));
		check(helper, near(ReductionReactorBlockEntity.uraniumPerAduDrum(), 326, 0.005), "a 96 % drum should hold 326 kg of U: " + ReductionReactorBlockEntity.uraniumPerAduDrum());

		start(helper, -4, -6, () -> helper.runAfterDelay(100, () -> {
			PrecipitationTankBlockEntity tank = entity(helper, 0, 0, PrecipitationTankBlockEntity.class);
			UraniumCalcinerBlockEntity calciner = entity(helper, 4, 0, UraniumCalcinerBlockEntity.class);
			String chain = " | tank " + read(helper, 0, 0) + " | calciner " + read(helper, 4, 0);
			check(helper, read(helper, 0, 0).contains("precipitating ADU") && tank.adu() > 0, "strip liquor and ammonia should make ADU in the tank" + chain);
			check(helper, read(helper, 4, 0).contains("firing up") && calciner.warm() > 0, "the calciner should be firing up on LPG" + chain);
			log("adu_yellowcake", "firing up:" + chain);
			calciner.setWarmForTest();                                                                       // five minutes of firing, by hook
			helper.runAfterDelay(200, () -> {
				String run = " | calciner " + read(helper, 4, 0) + " | filler " + read(helper, 7, -1);
				check(helper, read(helper, 4, 0).contains("calcining"), "at temperature the calciner should calcine the ADU" + run);
				check(helper, filler.drums() >= 1 && filler.slots().get(1).is(WildcatterItems.U3O8_DRUM), "the filler should fill a drum of U3O8, 96 %" + run);
				// the conversion line on those drums
				ConversionStageBlockEntity hf = entity(helper, 20, 3, ConversionStageBlockEntity.class);
				double h2 = reducer.hydrogenKg() / Math.max(1e-9, reducer.reducedU());
				double hfKg = hf.reagentTaken() * Tuning.AHF_KG_PER_L / Math.max(1e-9, hf.uraniumThrough());
				String line = String.format(" | reducer %.2f g H2/kg U over %.1f kg | hydrofluorinator %.3f kg HF/kg U over %.1f kg | %s | %s", h2 * 1000, reducer.reducedU(),
						hfKg, hf.uraniumThrough(), read(helper, 20, 0), read(helper, 20, 3));
				check(helper, reducer.reducedU() > 0.3 && near(h2, 5.65e-3, 0.05), "U3O8 drums should take 5.65 g of hydrogen a kg of U" + line);
				check(helper, hf.uraniumThrough() > 0.2 && near(hfKg, 0.43, 0.02), "the hydrofluorinator should take no HF for magnesia (0.43 kg a kg of U)" + line);
				log("adu_yellowcake", "running:" + run + line);
				air(helper, 4, 4);                                                                           // the ammonia cut
				helper.runAfterDelay(40, () -> {
					String noNh3 = read(helper, 0, 0);
					check(helper, noNh3.contains("NO AMMONIA"), "without ammonia and magnesia the tank should read NO AMMONIA: " + noNh3);
					tank.slots().set(0, new ItemStack(WildcatterItems.MAGNESIA, 4));                           // magnesia in its hopper
					helper.runAfterDelay(120, () -> {
						String mg = read(helper, 0, 0);
						check(helper, mg.contains("kg/s of MgO"), "with magnesia the tank should go back to the magnesia route: " + mg);
						log("adu_yellowcake", "ok: " + noNh3 + " | " + mg);
						helper.succeed();
					});
				});
			});
		}));
	}

	// ---- conversion_extensions (16b1) ----------------------------------------------------------------------------------------------
	static void conversionExtensions(GameTestHelper helper) {
		String scene = "conversion_extensions";
		floor(helper, -12, -7, 35, 24);
		place(helper, WildcatterBlocks.FLUORINE_CELL_HOUSE, 0, 0);    // x 0-5, z 0-11
		place(helper, WildcatterBlocks.CATHODE_GAS_SCRUBBER, 6, 10);  // x 6-7, z 10-11, against its back cells
		place(helper, WildcatterBlocks.REDUCTION_REACTOR, 2, 13);     // x 2-4, z 13-15
		place(helper, WildcatterBlocks.HYDROFLUORINATOR, 2, 16);      // x 2-4, z 16-21
		place(helper, WildcatterBlocks.KOH_SCRUBBER, -1, 16);         // x -1..1, z 16-18, against the hydrofluorinator
		place(helper, WildcatterBlocks.FLUORINATOR, 14, 0);           // x 14-16, z 0-2, on its own: its bed
		place(helper, WildcatterBlocks.LEACH_TANK, 24, 0);            // x 24-31, z 0-7
		pipe(helper, new int[] {6, 0, 12}, new int[] {6, 0, 13}, new int[] {5, 0, 13});                   // cathode hydrogen to the reducer
		pipe(helper, new int[] {5, 0, 18});
		source(helper, 6, 18, Product.ANHYDROUS_HF, 1.0);
		pipe(helper, new int[] {0, 0, 15});
		source(helper, 0, 14, Product.CAUSTIC_POTASH, 100.0);                                             // the first charge
		pipe(helper, new int[] {27, 0, -1});
		TestGasSource.SourceEntity ore = source(helper, 27, -2, Product.ORE_SLURRY, 50.0);
		ore.mix[0] = 0.0005;                                                                              // kg of U3O8 a kg of slurry
		ore.mix[1] = Tuning.MILL_SLURRY_SOLIDS;
		pipe(helper, new int[] {32, 0, 3});
		source(helper, 33, 3, Product.SULFURIC_ACID, 10.0);
		genset(helper, -6, -4);                                                                          // skid x -11..-6, z -4..-3
		cableRun(helper, -5, -4, -2, 16);                                                                // the KOH scrubber's box
		cableRun(helper, -2, -5, 6, 9);                                                                  // the cathode gas scrubber's box, north
		cableRun(helper, 6, -6, 23, 0);                                                                  // the leach tank's box
		dev.wildcatter.Rules.setFluorinatorAshBed(true);
		dev.wildcatter.block.FluorineCellHouseBlockEntity cells = entity(helper, 0, 0, dev.wildcatter.block.FluorineCellHouseBlockEntity.class);
		cells.testCells(dev.wildcatter.fluid.Conversion.fluorineKgs());                                 // the cells at full current
		ReductionReactorBlockEntity reducer = entity(helper, 2, 13, ReductionReactorBlockEntity.class);
		reducer.slots().set(0, new ItemStack(WildcatterItems.U3O8_DRUM, 2));
		dev.wildcatter.block.KohScrubberBlockEntity koh = entity(helper, -1, 16, dev.wildcatter.block.KohScrubberBlockEntity.class);
		koh.slots().set(0, new ItemStack(WildcatterItems.QUICKLIME_BAG, 2));
		ConversionStageBlockEntity fluorinator = entity(helper, 14, 0, ConversionStageBlockEntity.class);
		fluorinator.addAshForTest(Tuning.ASH_BED_KG);
		dev.wildcatter.block.LeachTankBlockEntity leach = entity(helper, 24, 0, dev.wildcatter.block.LeachTankBlockEntity.class);
		leach.slots().set(0, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.PYROLUSITE), 4));
		leach.slots().set(1, new ItemStack(WildcatterItems.FLUORINATOR_ASH_DRUM, 1));
		start(helper, -6, -4, () -> helper.runAfterDelay(100, () -> {
			dev.wildcatter.block.CathodeGasScrubberBlockEntity scrubber = entity(helper, 6, 10, dev.wildcatter.block.CathodeGasScrubberBlockEntity.class);
			ConversionStageBlockEntity hf = entity(helper, 2, 16, ConversionStageBlockEntity.class);
			String chain = " | scrubber " + read(helper, 6, 10) + " | reducer " + read(helper, 2, 13) + " | hf " + read(helper, 2, 16) + " | koh " + read(helper, -1, 16)
					+ " | fluorinator " + read(helper, 14, 0) + " | leach " + read(helper, 24, 0);
			check(helper, near(scrubber.nm3h(), 28.6, 0.10), "the cathode gas scrubber should take about 28.6 Nm3/h of hydrogen: " + scrubber.nm3h() + chain);
			check(helper, read(helper, 2, 13).contains("reducing") && reducer.reducedU() > 0.1, "the reducer should run on cathode hydrogen with no reformer" + chain);
			check(helper, koh.koh() > 1000 && read(helper, 2, 16).contains("hydrofluorinating") || hf.uraniumThrough() > 0.1, "the charged KOH scrubber should let the hydrofluorinator run" + chain);
			check(helper, read(helper, 14, 0).contains("BED FULL"), "a fluorinator with a full bed should read BED FULL" + chain);
			check(helper, leach.ashLeached() > 0, "the ash drum's uranium should go into the leach slurry" + chain);
			log(scene, "running:" + chain);
			// retire the bed: ten minutes skipped by hook, empty drums and fluorspar in its hopper
			fluorinator.slots().set(0, new ItemStack(WildcatterItems.EMPTY_DRUM, 6));
			fluorinator.slots().set(1, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.FLUORITE), 1));
			check(helper, fluorinator.retireBed(), "the fluorinator should start retiring its bed");
			helper.runAfterDelay(20, () -> {
				String retiring = read(helper, 14, 0);
				check(helper, retiring.contains("RETIRING THE BED"), "the fluorinator should be stopped retiring its bed: " + retiring);
				fluorinator.skipRetireForTest();
				// the KOH scrubber's cake near a full bag: its lime regenerates 30 kg of KOH
				koh.setForTest(1_970, 30, Tuning.BULK_BAG_KG - 1, 0);
				helper.runAfterDelay(100, () -> {
					String retired = read(helper, 14, 0);
					check(helper, fluorinator.ashDrums() == 5 && !fluorinator.retiring() && fluorinator.ash() < 1, "the bed should be drummed into five ash drums and renewed: " + retired);
					check(helper, koh.slots().get(1).is(WildcatterItems.SYNTHETIC_FLUORSPAR), "the KOH scrubber should bag its CaF2 as synthetic fluorspar: " + read(helper, -1, 16));
					// the lime gone and the KOH all spent
					koh.slots().set(0, ItemStack.EMPTY);
					koh.setForTest(0, Tuning.KOH_CHARGE_KG, 0, 0);
					hf.takeUranium(1_000);
					helper.runAfterDelay(40, () -> {
						String spent = read(helper, -1, 16);
						String stopped = read(helper, 2, 16);
						check(helper, spent.contains("SPENT"), "with its KOH spent and no lime the scrubber should read SPENT: " + spent);
						check(helper, stopped.contains("KOH SCRUBBER SPENT"), "with its scrubber spent the hydrofluorinator should stop: " + stopped);
						dev.wildcatter.Rules.setFluorinatorAshBed(false);
						log(scene, "ok: " + retired + " | " + spent + " | " + stopped);
						helper.succeed();
					});
				});
			});
		}));
	}
}
