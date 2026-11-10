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
import dev.wildcatter.block.AluminothermicVesselBlockEntity;
import dev.wildcatter.block.BaggingStationBlockEntity;
import dev.wildcatter.block.CapacitorBankBlockEntity;
import dev.wildcatter.block.FurnaceTransformerBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.MolyRoasterBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.SubmergedArcFurnaceBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.fluid.Ferroalloys;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Phase 17a's scenes (docs/multiblocks/ferroalloys.md, "Game tests"):
 * <ul>
 * <li>{@code ferroalloys}: a 45 MW aeroderivative on 13.8 kV, its 60 MVA GSU to 138 kV and a 60 MVA substation transformer to 12.47 kV;
 * the furnace transformer and the submerged arc furnace on that, its charge (chromitite, coke, quartz, iron) and green anodes by a
 * hopper on top, a capacitor bank against the transformer on its bus, a bagging station at the taphole on its own diesel set. The
 * furnace draws 7.5-8.5 MW at PF 0.75-0.85; the bank closed lifts the feeder above 0.93. A tap (the hearth brought near full by hook)
 * fills bags of HC ferrochrome; set to ferrosilicon (its hour skipped by hook), bags of ferrosilicon; the paste burned away by hook,
 * ELECTRODES SHORT and the MW fall.</li>
 * <li>{@code alumino}: on a diesel set, an aluminothermic vessel with moly oxide, iron and aluminium by hopper fires twice and fills a bag
 * of ferromolybdenum; a moly roaster on a stand-in concentrate source (a molybdenite assay) roasts 0.25 t/h of oxide and its burner gas
 * goes to an acid plant's converter, which makes acid; the gas pipe cut, NO ACID PLANT.</li>
 * </ul>
 */
final class FerroalloyScenes {
	private FerroalloyScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("ferroalloys", FerroalloyScenes::ferroalloys);
		scenes.put("alumino", FerroalloyScenes::alumino);
		return scenes;
	}

	private static void hopper(GameTestHelper helper, int x, int y, int z, ItemStack... stacks) {
		helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.HOPPER.defaultBlockState(), 3);
		HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
		for (int i = 0; i < stacks.length; i++) {
			hopper.setItem(i, stacks[i]);
		}
	}

	private static int count(BaggingStationBlockEntity station, Item item) {
		int n = 0;
		for (ItemStack s : station.slots()) {
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		return n;
	}

	// ---- ferroalloys ------------------------------------------------------------------------------------------------------------------
	static void ferroalloys(GameTestHelper helper) {
		floor(helper, -2, -24, 68, 22);
		// the peaker (as the client scene peaker): the package, its generator, breaker, GSU and the substation transformer
		place(helper, WildcatterBlocks.AERO_TURBINE, 20, U, 0, NORTH);               // x 20-24, z 0..13
		place(helper, WildcatterBlocks.AERO_INLET_HOUSE, 20, U, -5, NORTH);
		place(helper, WildcatterBlocks.AERO_EXHAUST_STACK, 20, U, 14, NORTH);
		place(helper, WildcatterBlocks.AERO_GENERATOR, 25, U, 1, NORTH);             // x 25-28, z 1..7
		place(helper, WildcatterBlocks.GEN_BREAKER_3000, 26, U, 8, NORTH);           // x 26-27, z 8..9
		place(helper, WildcatterBlocks.GSU_60, 36, U, 8, NORTH);                     // x 33-38, z 4..8
		place(helper, WildcatterBlocks.SUBSTATION_TRANSFORMER_60, 36, U, -6, NORTH); // x 33-38, z -10..-6
		place(helper, WildcatterBlocks.STOCK_TANK, 14, U, 4, NORTH);                 // kerosene
		put(helper, "gas_line", 18, U, 6);
		put(helper, "gas_line", 19, U, 6);
		for (int x = 26; x <= 37; x++) {
			put(helper, "bus_duct_gen", x, U, 10);
		}
		put(helper, "bus_duct_gen", 36, U, 9);
		for (int z = -10; z <= 4; z++) {
			put(helper, "cable_hv", 36, U + 6, z);                                   // bushing top to bushing top
		}
		// station power: a diesel set, 4.16 kV through a backward unit substation to the starter
		genset(helper, 10, U, 2);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 14, U, 2, Direction.SOUTH);
		for (int x = 15; x <= 19; x++) {
			put(helper, "cable_mv", x, U, 2);
		}
		// 12.47 kV: the substation transformer's throat, east, and north along x 43 past the furnace transformer's and the bank's boxes
		for (int x = 36; x <= 43; x++) {
			put(helper, "cable_dist", x, U, -5);
		}
		for (int z = -20; z <= -6; z++) {
			put(helper, "cable_dist", 43, U, z);
		}
		// the furnace, its transformer against its west face and the bank against the transformer (the spec's layout)
		place(helper, WildcatterBlocks.FURNACE_TRANSFORMER, 44, U, -20, NORTH);      // x 44-47, z -20..-16, its box (44, -20) west
		place(helper, WildcatterBlocks.SUBMERGED_ARC_FURNACE, 48, U, -20, NORTH);    // x 48-54, z -20..-14
		place(helper, WildcatterBlocks.CAPACITOR_BANK, 44, U, -15, NORTH);           // x 44-45, z -15..-12, its box (44, -15) west
		place(helper, WildcatterBlocks.BAGGING_STATION, 56, U, -16, Direction.SOUTH); // x 55-56, z -17..-16, at the taphole; box east
		genset(helper, 64, U, -12);                                                 // skid x 59-64, z -12..-11; 480 V at (65, -12)
		for (int z = -16; z <= -12; z++) {
			put(helper, "cable_industrial", 65, U, z);
		}
		for (int x = 57; x <= 64; x++) {
			put(helper, "cable_industrial", x, U, -16);
		}
		hopper(helper, 51, U + 8, -17, new ItemStack(OreBlocks.MINERALS.get(Ore.CHROMITE), 8), new ItemStack(WildcatterItems.MET_COKE, 64),
				new ItemStack(Items.QUARTZ, 64), new ItemStack(Items.IRON_INGOT, 32), new ItemStack(WildcatterItems.GREEN_ANODE, 4));
		entity(helper, 14, U, 4, StorageTankBlockEntity.class).testFill(Product.KEROSENE, 40_000);
		BaggingStationBlockEntity station = entity(helper, 56, U, -16, BaggingStationBlockEntity.class);
		station.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 16));
		CapacitorBankBlockEntity bank = entity(helper, 44, U, -15, CapacitorBankBlockEntity.class);
		bank.setClosed(false);
		GasTurbineBlockEntity gt = entity(helper, 20, U, 0, GasTurbineBlockEntity.class);
		start(helper, 64, U, -12, () -> { });
		start(helper, 10, U, 2, () -> {
			gt.setWantRun(true);
			helper.runAfterDelay(200, () -> {
				gt.skipStartForTest(false);
				helper.runAfterDelay(100, () -> {
					gt.skipRampForTest();
					helper.runAfterDelay(400, () -> furnaceChecks(helper, bank, station));
				});
			});
		});
	}

	private static void furnaceChecks(GameTestHelper helper, CapacitorBankBlockEntity bank, BaggingStationBlockEntity station) {
		String scene = "ferroalloys";
		FurnaceTransformerBlockEntity tx = entity(helper, 44, U, -20, FurnaceTransformerBlockEntity.class);
		SubmergedArcFurnaceBlockEntity saf = entity(helper, 48, U, -20, SubmergedArcFurnaceBlockEntity.class);
		String chain = " | furnace " + read(helper, 48, U, -20) + " | transformer " + read(helper, 44, U, -20) + " | gt " + read(helper, 20, U, 0)
				+ " | gen " + read(helper, 25, U, 1) + " | sub " + read(helper, 36, U, -6);
		double mw = tx.load().watts() / 1e6;
		double pf = Ferroalloys.powerFactor(tx.load().watts(), tx.load().vars());
		log(scene, "open bank:" + chain);
		check(helper, mw > 7.5 && mw < 8.5, "the furnace should draw 7.5-8.5 MW: " + mw + chain);
		check(helper, pf > 0.75 && pf < 0.85, "the furnace should run at PF 0.75-0.85: " + pf + chain);
		check(helper, tx.feederPowerFactor() < 0.85, "with the bank open the feeder carries the furnace's PF: " + tx.feederPowerFactor() + chain);
		check(helper, saf.making() > 1500, "the furnace should smelt about 2.1 t/h of ferrochrome: " + saf.making() + chain);
		bank.setClosed(true);
		helper.runAfterDelay(40, () -> {
			String withBank = " | transformer " + read(helper, 44, U, -20) + " | bank " + read(helper, 44, U, -15) + " | gen " + read(helper, 25, U, 1);
			log(scene, "closed bank:" + withBank);
			check(helper, bank.capacitor().vars() > 2.7e6 && bank.capacitor().vars() < 3.3e6, "the bank should give about 3 Mvar: " + bank.capacitor().vars() + withBank);
			check(helper, tx.feederPowerFactor() > 0.93, "with the bank closed the feeder's PF should be above 0.93: " + tx.feederPowerFactor() + withBank);
			// a tap: the hearth brought near full
			saf.testFillHearth(Tuning.SAF_HEARTH_KG - 0.2);
			helper.runAfterDelay(200, () -> {   // the tap, and the station's filler started
				int fecr = count(station, WildcatterItems.FERROCHROME_BAG);
				String tapped = " | furnace " + read(helper, 48, U, -20) + " | station " + read(helper, 56, U, -16);
				log(scene, "tap:" + tapped);
				check(helper, fecr >= 1, "a tap should fill bags of HC ferrochrome: " + fecr + tapped);
				// ferrosilicon: two steps of the recipe, the hour skipped
				saf.nextRecipe();
				saf.nextRecipe();
				saf.testSkipChangeover();
				helper.runAfterDelay(40, () -> {
					check(helper, saf.recipe() == Ferroalloys.Recipe.FESI, "the furnace should be on ferrosilicon: " + read(helper, 48, U, -20));
					saf.testFillHearth(Tuning.SAF_HEARTH_KG - 0.2);
					helper.runAfterDelay(200, () -> {
						int fesi = count(station, WildcatterItems.FERROSILICON_BAG);
						String si = " | furnace " + read(helper, 48, U, -20) + " | station " + read(helper, 56, U, -16);
						log(scene, "ferrosilicon:" + si);
						check(helper, fesi >= 1, "a tap on ferrosilicon should fill bags of ferrosilicon: " + fesi + si);
						double before = tx.load().watts();
						// the paste bin emptied and the electrodes burned short
						saf.slots().set(saf.slots().size() - 1, ItemStack.EMPTY);
						saf.testBurnElectrodes();
						helper.runAfterDelay(80, () -> {
							String shortRead = read(helper, 48, U, -20);
							double after = tx.load().watts();
							log(scene, "short: " + before / 1e6 + " MW to " + after / 1e6 + " MW | " + shortRead);
							check(helper, shortRead.contains("ELECTRODES SHORT"), "with no paste the furnace should read ELECTRODES SHORT: " + shortRead);
							check(helper, after < before * 0.6, "burnt short, its MW should fall: " + before + " to " + after);
							log(scene, "ok");
							helper.succeed();
						});
					});
				});
			});
		});
	}

	// ---- alumino --------------------------------------------------------------------------------------------------------------------
	private static void water(GameTestHelper helper, int x, int z) {
		entity(helper, x, U, z, WaterUser.class).addWater(100_000);
	}

	static void alumino(GameTestHelper helper) {
		floor(helper, -10, -12, 36, 4);
		// the vessel, a bagging station against its north face, a hopper of its batch on top
		place(helper, WildcatterBlocks.ALUMINOTHERMIC_VESSEL, 0, U, 0, NORTH);      // x 0-2, z 0-2, box (0, 0) west
		place(helper, WildcatterBlocks.BAGGING_STATION, 0, U, -2, NORTH);           // x 0-1, z -2..-1, box (0, -2) west
		hopper(helper, 1, U + 3, 1, new ItemStack(WildcatterItems.MOLY_OXIDE_BAG, 2), new ItemStack(Items.IRON_INGOT, 8),
				new ItemStack(WildcatterItems.ALUMINIUM_INGOT, 40));
		// the roaster, its concentrate from a stand-in source east of it, its burner gas to an acid plant's converter
		place(helper, WildcatterBlocks.MOLY_ROASTER, 20, U, -4, NORTH);             // x 20-22, z -4..-2, box (20, -4) west
		place(helper, WildcatterBlocks.CONVERTER, 27, U, 0, Direction.WEST);        // x 27-31, z -4..0
		place(helper, WildcatterBlocks.FINAL_ABSORBER, 27, U, -5, Direction.WEST);  // x 27-29, z -7..-5
		place(helper, WildcatterBlocks.STOCK_TANK, 31, U, -6, Direction.WEST);      // x 31-34, z -9..-6
		put(helper, "gas_line", 30, U, -6);                                         // acid to the stock tank
		for (int x = 21; x <= 26; x++) {
			put(helper, "gas_line", x, U, -1);                                      // burner gas: the roaster's south side to the converter
		}
		put(helper, "gas_line", 23, U, -3);
		helper.getLevel().setBlock(abs(helper, 24, U, -3), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity source = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, 24, U, -3));
		source.product = Product.THICKENED_CONCENTRATE;
		source.rate = 5.0;   // kg a second
		source.assay = SlurryAssay.of(Map.of(MolyRoasterBlockEntity.MOLYBDENITE, 0.45));   // 90 % concentrate at 50 % solids
		// 480 V: a diesel set, up x -1 past the bagging station's and the vessel's boxes, and east along z -6 to the roaster's
		genset(helper, -3, U, -6);                                                   // skid x -8..-3, z -6..-5
		put(helper, "cable_industrial", -2, U, -6);
		for (int z = -6; z <= 0; z++) {
			put(helper, "cable_industrial", -1, U, z);
		}
		for (int x = 0; x <= 19; x++) {
			put(helper, "cable_industrial", x, U, -6);
		}
		put(helper, "cable_industrial", 19, U, -5);
		put(helper, "cable_industrial", 19, U, -4);
		water(helper, 27, -5);
		BaggingStationBlockEntity station = entity(helper, 0, U, -2, BaggingStationBlockEntity.class);
		station.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 4));
		AluminothermicVesselBlockEntity vessel = entity(helper, 0, U, 0, AluminothermicVesselBlockEntity.class);
		MolyRoasterBlockEntity roaster = entity(helper, 20, U, -4, MolyRoasterBlockEntity.class);
		start(helper, -3, U, -6, () -> helper.runAfterDelay(400, () -> {   // the hopper has loaded both batches by now
			String scene = "alumino";
			String loaded = read(helper, 0, U, 0);
			check(helper, loaded.contains("READY"), "the hopper should load a full batch: " + loaded);
			String fired = vessel.fire(helper.getLevel());
			check(helper, vessel.firing(), "a sneak-click should fire it: " + fired + " | " + loaded);
			helper.runAfterDelay(100, () -> {   // the fan up to speed
				String firing = read(helper, 0, U, 0);
				check(helper, firing.contains("FIRING") && !firing.contains("NO FAN"), "it should fire with its fan running: " + firing);
				vessel.testFinishFiring();
				helper.runAfterDelay(20, () -> {
					vessel.fire(helper.getLevel());   // the second batch
					vessel.testFinishFiring();
					helper.runAfterDelay(200, () -> {   // the station's filler started
						int bags = count(station, WildcatterItems.FERROMOLYBDENUM_BAG);
						String chain = " | vessel " + read(helper, 0, U, 0) + " | station " + read(helper, 0, U, -2);
						log(scene, "vessel:" + chain);
						check(helper, bags == 1, "two batches should fill a bag of ferromolybdenum: " + bags + chain);
						String roasting = " | roaster " + read(helper, 20, U, -4) + " | converter " + read(helper, 27, U, 0) + " | absorber " + read(helper, 27, U, -5);
						log(scene, "roaster:" + roasting);
						check(helper, Math.abs(roaster.making() - Tuning.ROASTER_OXIDE_KGH) < Tuning.ROASTER_OXIDE_KGH * 0.05, "the roaster should make 0.25 t/h of oxide: " + roaster.making() + roasting);
						check(helper, roaster.oxide() > 1, "moly oxide should be waiting for bags: " + roasting);
						check(helper, read(helper, 27, U, 0).contains("converted"), "the converter should take the roaster's burner gas: " + roasting);
						helper.getLevel().setBlock(abs(helper, 24, U, -1), Blocks.AIR.defaultBlockState(), 3);   // the gas pipe cut
						helper.runAfterDelay(200, () -> {   // its 5 s of gas fills, then it stops
							String stopped = read(helper, 20, U, -4);
							check(helper, stopped.contains("NO ACID PLANT") && roaster.making() == 0, "with nowhere for its gas the roaster should stop: " + stopped);
							log(scene, "ok: " + stopped);
							helper.succeed();
						});
					});
				});
			});
		}));
	}
}
