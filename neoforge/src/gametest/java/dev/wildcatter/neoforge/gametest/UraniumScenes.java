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
 * <li>{@code cascade_operations} (16c1, enrichment-operations.md): eleven centrifuges driven by hook (a frequency converter needs
 * 12.47 kV and several MW); set to 3.00 / 0.25 %, ten machines are enough and the 30B the product take-off fills reads 3.00 %; with the
 * spin-up rule on and the drive gone, the machines coast and the cascade still feeds; with the cooling skid's water cut and five
 * minutes passed by hook, CASING HOT holds the feed.</li>
 * <li>{@code isr_operations} (16d1, isr-operations.md): a CO2 liquefier on a lime kiln's gas (the kiln burning by hook) fills the CO2
 * storage tank against a header house, which doses its injection at 0.5 g/L (the injector carries the full dose); with the tank empty,
 * NO CO2. The header house's bleed reaches a disposal wellhead through an injection pump. A satellite's IX columns fill a resin trailer,
 * which carries its loading as an item; a full trailer against the central columns gives eluate.</li>
 * </ul>
 */
final class UraniumScenes {
	private UraniumScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("adu_yellowcake", UraniumScenes::aduYellowcake);
		scenes.put("conversion_extensions", UraniumScenes::conversionExtensions);
		scenes.put("cascade_operations", UraniumScenes::cascadeOperations);
		scenes.put("isr_operations", UraniumScenes::isrOperations);
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
		RuleScenes.rule(helper, "fluorinatorAshBed", true);
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
						RuleScenes.rule(helper, "fluorinatorAshBed", false);
						log(scene, "ok: " + retired + " | " + spent + " | " + stopped);
						helper.succeed();
					});
				});
			});
		}));
	}

	// ---- cascade_operations (16c1) -------------------------------------------------------------------------------------------------
	/** Drives the centrifuges at (x1..x2, U, z) at full speed every second while {@code on[0]}, as a frequency converter would. */
	static void driveLoop(GameTestHelper helper, int x1, int x2, int z, boolean[] on) {
		if (!on[0]) {
			return;
		}
		long now = helper.getLevel().getGameTime();
		net.minecraft.core.BlockPos fake = abs(helper, -50, U, -50);
		for (int x = x1; x <= x2; x++) {
			entity(helper, x, z, dev.wildcatter.block.CentrifugeBlockEntity.class).drive(fake, 1.0, now);
		}
		helper.runAfterDelay(20, () -> driveLoop(helper, x1, x2, z, on));
	}

	static void cascadeOperations(GameTestHelper helper) {
		String scene = "cascade_operations";
		floor(helper, -10, -6, 28, 16);
		place(helper, WildcatterBlocks.FEED_STATION, 0, 0);            // x 0-2, z 0-4; the header at z 5
		place(helper, WildcatterBlocks.PRODUCT_TAKEOFF, 2, 6);         // x 2-4, z 6-9
		place(helper, WildcatterBlocks.TAILS_TAKEOFF, 6, 6);           // x 6-8, z 6-10
		place(helper, WildcatterBlocks.CASCADE_COOLING_SKID, 10, 8);   // x 10-12, z 8-13, its east side on the header's branch
		for (int x = 0; x <= 15; x++) {
			pipe(helper, new int[] {x, 0, 5});
		}
		for (int z = 6; z <= 10; z++) {
			pipe(helper, new int[] {13, 0, z});                                                          // the header's branch past the skid
		}
		for (int y = 1; y <= 6; y++) {
			pipe(helper, new int[] {15, y, 5});
		}
		for (int x = 16; x <= 26; x++) {
			place(helper, WildcatterBlocks.CENTRIFUGE, x, 5);
			pipe(helper, new int[] {x, 6, 5});                                                           // the header over the machines
		}
		pipe(helper, new int[] {11, 0, 7});
		helper.getLevel().setBlock(abs(helper, 11, U + 1, 7), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity cw = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, 11, U + 1, 7));
		cw.product = Product.COOLING_WATER;
		cw.rate = 300.0;
		genset(helper, -4, -4);
		cableRun(helper, -3, -4, -1, 14);                                                                // the feed station's box at (-1, 0), on to z 14
		cableRun(helper, -1, 14, 9, 14);
		for (int z = 6; z <= 13; z++) {
			cable(helper, 1, z);                                                                         // the product take-off's box
			cable(helper, 5, z);                                                                         // the tails take-off's box
		}
		for (int z = 8; z <= 13; z++) {
			cable(helper, 9, z);                                                                         // the skid's box
		}
		dev.wildcatter.block.FeedStationBlockEntity feed = entity(helper, 0, 0, dev.wildcatter.block.FeedStationBlockEntity.class);
		feed.slots().set(0, new ItemStack(WildcatterItems.UF6_CYLINDER_48Y, 1));
		feed.setAssaysForTest(0, 1);                                                                     // 3.00 / 0.25 %
		dev.wildcatter.block.TakeoffBlockEntity product = entity(helper, 2, 6, dev.wildcatter.block.TakeoffBlockEntity.class);
		product.slots().set(0, new ItemStack(WildcatterItems.EMPTY_30B_CYLINDER, 2));
		dev.wildcatter.block.TakeoffBlockEntity tails = entity(helper, 6, 6, dev.wildcatter.block.TakeoffBlockEntity.class);
		tails.slots().set(0, new ItemStack(WildcatterItems.EMPTY_48Y_CYLINDER, 2));
		boolean[] on = {true};
		check(helper, dev.wildcatter.fluid.Enrichment.stages(0.03, 0.0025) == 10, "3.00 / 0.25 % should need ten stages: " + dev.wildcatter.fluid.Enrichment.stages(0.03, 0.0025));
		start(helper, -4, -4, () -> {
			driveLoop(helper, 16, 25, 5, on);                                                            // ten machines
			helper.runAfterDelay(100, () -> {
				String running = read(helper, 0, 0);
				check(helper, running.contains("feeding") && running.contains("3.00 %"), "ten machines should run a 3.00 / 0.25 % cascade: " + running + " | skid " + read(helper, 10, 8));
				check(helper, read(helper, 10, 8).contains("cooling 10 machines"), "the skid should cool the ten machines: " + read(helper, 10, 8));
				product.addUf6(Math.max(0, product.capacity() - 0.01), 0.03);                                // a hook brings the 30B near full
				log(scene, "running: " + running);
				helper.runAfterDelay(60, () -> {
					ItemStack cylinder = product.slots().get(1);
					double assay = dev.wildcatter.WildcatterComponents.assay(cylinder);
					check(helper, cylinder.is(WildcatterItems.LEU_CYLINDER_30B) && Math.abs(assay - 0.03) < 0.0002, "the product 30B should read 3.00 %: " + cylinder + " " + assay
							+ " | " + read(helper, 2, 6));
					// the drive gone with the spin-up rule on: the machines coast and the cascade keeps feeding
					RuleScenes.rule(helper, "cascadeSpinUp", true);
					for (int x = 16; x <= 25; x++) {
						entity(helper, x, 5, dev.wildcatter.block.CentrifugeBlockEntity.class).setSpinForTest(1.0);
					}
					on[0] = false;
					helper.runAfterDelay(100, () -> {
						dev.wildcatter.block.CentrifugeBlockEntity c = entity(helper, 16, 5, dev.wildcatter.block.CentrifugeBlockEntity.class);
						double s = c.speed();
						String coast = read(helper, 16, 5);
						String still = read(helper, 0, 0);
						check(helper, s < 1 && s > 0.99 && coast.contains("COASTING"), "with no drive a machine should coast: " + s + " | " + coast);
						check(helper, still.contains("feeding"), "the coasting cascade should keep feeding: " + still);
						RuleScenes.rule(helper, "cascadeSpinUp", false);
						on[0] = true;
						driveLoop(helper, 16, 25, 5, on);
						// the cooling water cut, five minutes passed by hook
						helper.getLevel().setBlock(abs(helper, 11, U + 1, 7), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
						helper.runAfterDelay(80, () -> feed.setHotForTest(Tuning.CASING_HOT_SECONDS - 2));   // once the skid has run dry
						helper.runAfterDelay(160, () -> {
							String hot = read(helper, 0, 0);
							check(helper, hot.startsWith("Feed station: CASING HOT"), "without cooling water the cascade should read CASING HOT: " + hot + " | skid " + read(helper, 10, 8));
							on[0] = false;
							log(scene, "ok: " + coast + " | " + hot);
							helper.succeed();
						});
					});
				});
			});
		});
	}

	// ---- isr_operations (16d1) -----------------------------------------------------------------------------------------------------
	static void isrOperations(GameTestHelper helper) {
		String scene = "isr_operations";
		floor(helper, -18, -10, 32, 22);
		place(helper, WildcatterBlocks.HEADER_HOUSE, 0, 0);            // x 0-3, z 0-9
		place(helper, WildcatterBlocks.CO2_STORAGE_TANK, -3, 0);       // x -3..-1, z 0-7, against its west side
		place(helper, WildcatterBlocks.CO2_LIQUEFIER, -3, -8);         // x -3..-1, z -8..-3
		place(helper, WildcatterBlocks.LIME_KILN, 0, -8);              // x 0-2, z -8..-6, against the liquefier
		place(helper, WildcatterBlocks.INJECTION_PUMP, -6, 6);         // x -6..-5, z 6-11
		place(helper, WildcatterBlocks.IX_COLUMNS, 20, 0);             // x 20-27, z 0-3: the satellite
		place(helper, WildcatterBlocks.RESIN_TRAILER, 28, 0);          // x 28-29, z 0-5, against it
		place(helper, WildcatterBlocks.IX_COLUMNS, 20, 10);            // x 20-27, z 10-13: the central plant's
		pipe(helper, new int[] {-2, 0, -2}, new int[] {-2, 0, -1});                                       // liquid CO2 to the tank
		pipe(helper, new int[] {2, 0, 10});
		TestGasSource.SourceEntity barren = source(helper, 2, 11, Product.BARREN_LIXIVIANT, 20.0);       // barren lixiviant back from IX columns
		pipe(helper, new int[] {4, 0, 2}, new int[] {5, 0, 2});                                           // the injection header
		GridSceneKit.put(helper, "isr_wellhead", 6, U, 2);                                                // an injector on it
		pipe(helper, new int[] {0, 1, 10}, new int[] {0, 1, 11}, new int[] {0, 1, 12}, new int[] {-1, 1, 12}, new int[] {-2, 1, 12}, new int[] {-3, 1, 12},
				new int[] {-4, 1, 12}, new int[] {-5, 1, 12}, new int[] {-5, 0, 12});                    // the bleed, one up on its back, to the pump's suction
		pipe(helper, new int[] {-6, 1, 9});                                                               // the pump's discharge
		GridSceneKit.put(helper, "disposal_wellhead", -6, U + 2, 9);
		pipe(helper, new int[] {28, 0, 11});
		source(helper, 29, 11, Product.SULFURIC_ACID, 5.0);                                               // the central columns' acid
		genset(helper, -12, -4);                                                                         // skid x -17..-12, z -4..-3
		cableRun(helper, -11, -4, -4, -8);                                                               // the liquefier's box
		cableRun(helper, -11, -3, -7, 6);                                                                // the pump's box
		dev.wildcatter.block.LimeKilnBlockEntity kiln = entity(helper, 0, -8, dev.wildcatter.block.LimeKilnBlockEntity.class);
		dev.wildcatter.block.IxColumnsBlockEntity satellite = entity(helper, 20, 0, dev.wildcatter.block.IxColumnsBlockEntity.class);
		satellite.toggleSatellite();
		satellite.setResinForTest(100);
		dev.wildcatter.block.ResinTrailerBlockEntity trailer = entity(helper, 28, 0, dev.wildcatter.block.ResinTrailerBlockEntity.class);
		trailer.setU3o8(Tuning.RESIN_TRAILER_U3O8_KG - 10);                                              // a hook brings it near full
		dev.wildcatter.block.IxColumnsBlockEntity central = entity(helper, 20, 10, dev.wildcatter.block.IxColumnsBlockEntity.class);
		central.slots().set(0, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.HALITE), 4));
		start(helper, -12, -4, () -> {
			kiln.addKilnGasForTest(50);                                                                   // the kiln burning
			helper.runAfterDelay(240, () -> {
				dev.wildcatter.block.Co2LiquefierBlockEntity liquefier = entity(helper, -3, -8, dev.wildcatter.block.Co2LiquefierBlockEntity.class);
				dev.wildcatter.block.Co2StorageTankBlockEntity tank = entity(helper, -3, 0, dev.wildcatter.block.Co2StorageTankBlockEntity.class);
				dev.wildcatter.block.IsrWellheadBlockEntity injector = (dev.wildcatter.block.IsrWellheadBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 6, U, 2));
				dev.wildcatter.block.InjectionPumpBlockEntity pump = (dev.wildcatter.block.InjectionPumpBlockEntity) dev.wildcatter.multiblock.BigParts.entity(helper.getLevel(), abs(helper, -6, U, 6));
				String chain = " | liquefier " + read(helper, -3, -8) + " | tank " + read(helper, -3, 0) + " | header " + read(helper, 0, 0) + " | pump " + pump.inspect().getString()
						+ " | satellite " + read(helper, 20, 0) + " | trailer " + trailer.describe() + " | central " + read(helper, 20, 10);
				check(helper, read(helper, -3, -8).contains("liquefying") || tank.co2() > 0, "the liquefier should liquefy the kiln's CO2 into the tank" + chain);
				check(helper, read(helper, 0, 0).contains("CO2 0.50 g/L") && injector.co2() > 0.9, "the header house should dose its injection with 0.5 g/L of CO2: " + injector.co2() + chain);
				check(helper, pump.pumpingBleed() && pump.total() > 0, "the bleed should reach the disposal well through the injection pump" + chain);
				check(helper, trailer.u3o8() >= Tuning.RESIN_TRAILER_U3O8_KG - 0.5 && read(helper, 20, 0).contains("full"), "the satellite should fill its resin trailer" + chain);
				log(scene, "running:" + chain);
				// the trailer carried: its drop keeps its loading; set down against the central columns, eluted
				java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(helper.getLevel().getBlockState(abs(helper, 28, U, 0)), helper.getLevel(),
						abs(helper, 28, U, 0), trailer);
				double carried = drops.isEmpty() ? 0 : drops.get(0).getOrDefault(dev.wildcatter.WildcatterComponents.RESIN_U3O8, 0.0);
				check(helper, carried >= Tuning.RESIN_TRAILER_U3O8_KG - 0.5, "the trailer's item should carry its loading: " + drops);
				place(helper, WildcatterBlocks.RESIN_TRAILER, 18, 10);                                     // x 18-19, z 10-15, against the central columns
				entity(helper, 18, 10, dev.wildcatter.block.ResinTrailerBlockEntity.class).setU3o8(carried);
				tank.take(Tuning.CO2_TANK_KG);                                                             // the CO2 tank run dry
				helper.getLevel().setBlock(abs(helper, -2, U, -1), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				helper.runAfterDelay(60, () -> {
					String eluting = read(helper, 20, 10);
					String noCo2 = read(helper, 0, 0);
					check(helper, eluting.contains("eluting") || eluting.contains("eluate waiting") && eluting.contains("by resin trailer"), "the full trailer's resin should give eluate at the central columns: " + eluting);
					check(helper, noCo2.contains("NO CO2"), "with its CO2 tank empty the header house should read NO CO2: " + noCo2);
					log(scene, "ok: " + eluting + " | " + noCo2);
					helper.succeed();
				});
			});
		});
	}
}

