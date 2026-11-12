package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.KrollReactorBlockEntity;
import dev.wildcatter.block.MagnesiumCellBlockEntity;
import dev.wildcatter.block.PilgerMillBlockEntity;
import dev.wildcatter.block.ZrLineBlockEntity;
import dev.wildcatter.fluid.Zirconium;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Phase 17c's server scenes (docs/multiblocks/zirconium-extensions.md). Each part true size facing north at y 2, its main cell at the
 * north-west bottom corner, its 480 V box on the west face of that cell.
 * <ul>
 * <li>{@code zirconium_batches} (17c1, the rule {@code zirconiumBatches} on): the line from the hafnium column to the anneal furnace on
 * two paralleled diesel sets down an 800 A busway (the chlorinator, magnesium cell and extrusion press stand unpowered, fed by hook).
 * A Kroll batch given by hook is reduced and goes to the vacuum distillation, which gives the 50 kg lump to its hold while the second
 * retort takes the furnace; the VAR furnace melts an ingot of 100 kg; the pilger mill's scrap drops into a hopper under it and the VAR
 * furnace takes it into its charge; a HfCl4 drum appears under the hafnium column; on a hafnium campaign the Kroll reactor takes the
 * drum, and hafnium hollows on the rodlet dies make a hafnium rodlet.</li>
 * <li>{@code pidgeon} (17c2): a lime kiln on dolomite (LPG by a stand-in source) burns dolime and fills a dolime bag at the bagging
 * station against it; a Pidgeon furnace with dolime, ferrosilicon and fluorspar by hopper and gas makes magnesium; a cycle (sped by hook)
 * drops magnesium ingots into the hopper under it, and a magnesium cell takes them by its hopper.</li>
 * <li>{@code chromium} (17c2): a chromate kiln on chromite, sulfur, caustic soda and gas roasts chromate (a sack all but made by hook) to a
 * sack of chromic oxide under it; four sacks and two aluminium ingots fire in the aluminothermic vessel to chromium metal under it; the VAR
 * furnace remelts with chromium metal and no chromite; broken while it holds chromate, the kiln leaves a poison cloud.</li>
 * <li>{@code fumed_silica} (17c2): a fumed silica burner against a chlorinator holding SiCl4 (by hook), hydrogen and oxygen from stand-in
 * sources: a sack of fumed silica under it and 32 % hydrochloric acid on its pipe into a stock tank.</li>
 * </ul>
 */
final class ZirconiumScenes {
	private ZirconiumScenes() {
	}

	/** Each scene and the world rules it runs with (a batch of its own). */
	static final Map<String, List<String>> RULES = Map.of("zirconium_batches", List.of("zirconiumBatches"));

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("zirconium_batches", ZirconiumScenes::zirconiumBatches);
		scenes.put("pidgeon", ZirconiumScenes::pidgeon);
		scenes.put("chromium", ZirconiumScenes::chromium);
		scenes.put("fumed_silica", ZirconiumScenes::fumedSilica);
		return scenes;
	}

	static final int U = 2;
	static final Direction NORTH = Direction.NORTH;

	static void place(GameTestHelper helper, net.minecraft.world.level.block.Block block, int x, int z) {
		GridSceneKit.place(helper, block, x, U, z, NORTH);
	}

	static <T> T entity(GameTestHelper helper, int x, int z, Class<T> kind) {
		return GridSceneKit.entity(helper, x, U, z, kind);
	}

	static String read(GameTestHelper helper, int x, int z) {
		return GridSceneKit.read(helper, x, U, z);
	}

	static HopperBlockEntity hopper(GameTestHelper helper, int x, int y, int z, ItemStack... stacks) {
		helper.getLevel().setBlock(abs(helper, x, y, z), Blocks.HOPPER.defaultBlockState(), 3);
		HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
		for (int i = 0; i < stacks.length; i++) {
			hopper.setItem(i, stacks[i]);
		}
		return hopper;
	}

	static int count(HopperBlockEntity hopper, Item item) {
		int n = 0;
		for (int i = 0; i < hopper.getContainerSize(); i++) {
			if (hopper.getItem(i).is(item)) {
				n += hopper.getItem(i).getCount();
			}
		}
		return n;
	}

	/**
	 * Two 500 kW sets paralleled on a busway north-west, an 800 A busway down x -2 from z -9 to {@code south}, and a 500 kcmil stub at
	 * x -1 to each box at the given z.
	 */
	static void lowVoltage(GameTestHelper helper, int south, int... boxes) {
		GridSceneKit.genset(helper, -12, U, -8);
		GridSceneKit.genset(helper, -4, U, -8);
		GridSceneKit.put(helper, "busway_industrial", -11, U, -8);
		GridSceneKit.put(helper, "busway_industrial", -3, U, -8);
		for (int x = -11; x <= -3; x++) {
			GridSceneKit.put(helper, "busway_industrial", x, U, -9);
		}
		for (int z = -9; z <= south; z++) {
			GridSceneKit.put(helper, "busway_industrial", -2, U, z);
		}
		for (int z : boxes) {
			GridSceneKit.put(helper, "cable_industrial", -1, U, z);
			if (helper.getLevel().getBlockEntity(abs(helper, -1, U, z)) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
				cable.setSize(dev.wildcatter.energy.ConductorSize.KCMIL_500);
			}
		}
	}

	static void startBoth(GameTestHelper helper, Runnable then) {
		GridSceneKit.start(helper, -12, U, -8, () -> { });
		GridSceneKit.start(helper, -4, U, -8, then);
	}

	// ---- zirconium_batches (17c1) ---------------------------------------------------------------------------------------------------
	static void zirconiumBatches(GameTestHelper helper) {
		String scene = "zirconium_batches";
		floor(helper, -18, -12, 10, 44);
		place(helper, WildcatterBlocks.CHLORINATOR, 0, 0);            // x 0-2, z 0-2 (unpowered, fed by hook)
		place(helper, WildcatterBlocks.HAFNIUM_COLUMN, 0, 3);         // z 3-5
		place(helper, WildcatterBlocks.KROLL_REACTOR, 0, 6);          // z 6-8
		place(helper, WildcatterBlocks.VACUUM_DISTILLATION, 0, 9);    // z 9-11
		place(helper, WildcatterBlocks.MAGNESIUM_CELL, 3, 6);         // x 3-5, z 6-11: against the Kroll reactor and the distillation
		place(helper, WildcatterBlocks.VAR_FURNACE, 0, 12);           // z 12-14
		place(helper, WildcatterBlocks.EXTRUSION_PRESS, 0, 15);       // z 15-22 (4.16 kV: unpowered, given hollows by hook)
		place(helper, WildcatterBlocks.PILGER_MILL, 0, 23);           // z 23-32
		place(helper, WildcatterBlocks.VACUUM_ANNEAL_FURNACE, 0, 33); // z 33-40
		lowVoltage(helper, 33, 3, 6, 9, 12, 23, 33);
		HopperBlockEntity drumsOut = hopper(helper, 1, 1, 4);          // under the hafnium column
		HopperBlockEntity scrapOut = hopper(helper, 1, 1, 27);         // under the pilger mill
		HopperBlockEntity tubesOut = hopper(helper, 1, 1, 37);         // under the anneal furnace
		HopperBlockEntity varIn = hopper(helper, 1, U + 8, 13, new ItemStack(dev.wildcatter.ore.OreBlocks.INGOTS.get("tin")), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT),
				new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.CHROMITE)));   // on top of the VAR furnace
		HopperBlockEntity krollIn = hopper(helper, 1, U + 6, 7);       // on top of the Kroll reactor

		KrollReactorBlockEntity kroll = entity(helper, 0, 6, KrollReactorBlockEntity.class);
		ZrLineBlockEntity column = entity(helper, 0, 3, ZrLineBlockEntity.class);
		ZrLineBlockEntity still = entity(helper, 0, 9, ZrLineBlockEntity.class);
		ZrLineBlockEntity var = entity(helper, 0, 12, ZrLineBlockEntity.class);
		ZrLineBlockEntity press = entity(helper, 0, 15, ZrLineBlockEntity.class);
		PilgerMillBlockEntity mill = entity(helper, 0, 23, PilgerMillBlockEntity.class);
		MagnesiumCellBlockEntity cell = entity(helper, 3, 6, MagnesiumCellBlockEntity.class);
		cell.addMg(200);

		startBoth(helper, () -> {
			// a Kroll batch, charged by hook with its reduction all but done: out to the distillation, the second retort in
			kroll.chargeBatchForTest();
			helper.runAfterDelay(120, () -> {
				String batch = " | kroll " + read(helper, 0, 6) + " | still " + read(helper, 0, 9) + " | sets " + read(helper, -12, -8) + " / " + read(helper, -4, -8);
				log(scene, "reduced" + batch);
				check(helper, kroll.retortStep(0) == KrollReactorBlockEntity.DISTILLING && kroll.furnace() == 1
						&& kroll.retortStep(1) == KrollReactorBlockEntity.CHARGING, "the reduced retort A should go to the distillation and retort B into the furnace" + batch);
				check(helper, read(helper, 0, 9).contains("distilling retort A") || read(helper, 0, 9).contains("starting"), "the vacuum distillation should take retort A" + batch);
				double mgBefore = cell.mg();
				kroll.finishDistillationForTest();
				helper.runAfterDelay(240, () -> {
					String lump = " | kroll " + read(helper, 0, 6) + " | still " + read(helper, 0, 9) + " | var " + read(helper, 0, 12);
					log(scene, "distilled" + lump);
					check(helper, kroll.retortStep(0) == KrollReactorBlockEntity.EMPTY, "retort A should come back empty" + lump);
					check(helper, still.zirconium() + var.charge() > Tuning.KROLL_BATCH_KG - 0.1, "the 50 kg lump should be in the distillation (or the VAR's charge)" + lump);
					check(helper, cell.mg() > mgBefore + 5, "the excess magnesium should go back to the cell" + lump);
					check(helper, read(helper, 0, 12).contains("charging the next ingot"), "the VAR furnace should charge its next ingot from the sponge" + lump);
					scrapAndIngot(helper, scene, var, press, mill, scrapOut, varIn, () -> hafnium(helper, scene, kroll, column, press, mill, drumsOut, krollIn, tubesOut));
				});
			});
		});
	}

	/** The pilger mill's scrap into the hopper under it, on to the VAR furnace's charge; an ingot melted twice. */
	private static void scrapAndIngot(GameTestHelper helper, String scene, ZrLineBlockEntity var, ZrLineBlockEntity press, PilgerMillBlockEntity mill,
			HopperBlockEntity scrapOut, HopperBlockEntity varIn, Runnable then) {
		press.addMetalForTest(0.02, false);
		mill.addScrapForTest(Tuning.ZR_SCRAP_KG - 0.0001);   // the rolling losses below make up the rest
		helper.runAfterDelay(100, () -> {
			String rolled = " | mill " + read(helper, 0, 23) + " | press " + read(helper, 0, 15);
			log(scene, "scrap" + rolled);
			check(helper, read(helper, 0, 23).contains("rolling a cladding tube") || press.zirconium() < 1e-6, "the pilger mill should roll the hollows" + rolled);
			check(helper, count(scrapOut, WildcatterItems.ZIRCALOY_SCRAP) >= 1, "a bundle of Zircaloy scrap should drop into the hopper under the pilger mill" + rolled);
			// the bundle to the VAR furnace's hopper
			scrapOut.clearContent();
			varIn.setItem(3, new ItemStack(WildcatterItems.ZIRCALOY_SCRAP));
			helper.runAfterDelay(60, () -> {
				String charged = " | var " + read(helper, 0, 12);
				log(scene, "scrap in the charge" + charged);
				check(helper, var.chargeScrap() >= Tuning.ZR_SCRAP_KG - 0.01, "the VAR furnace should take the scrap into its next ingot's charge" + charged);
				var.finishIngotForTest();
				helper.runAfterDelay(240, () -> {
					String ingot = " | var " + read(helper, 0, 12);
					log(scene, "ingot" + ingot);
					check(helper, var.zirconium() >= Tuning.VAR_INGOT_KG - 0.1 && !var.hafnium(), "the VAR furnace should hold a 100 kg Zircaloy-4 ingot after its second melt" + ingot);
					then.run();
				});
			});
		});
	}

	/** A HfCl4 drum under the column, into a Kroll reactor on a hafnium campaign; hafnium hollows on the rodlet dies. */
	private static void hafnium(GameTestHelper helper, String scene, KrollReactorBlockEntity kroll, ZrLineBlockEntity column, ZrLineBlockEntity press,
			PilgerMillBlockEntity mill, HopperBlockEntity drumsOut, HopperBlockEntity krollIn, HopperBlockEntity tubesOut) {
		column.addHafniumForTest(Tuning.HFCL4_DRUM_HF_KG - 0.0005);   // within the drum's weighing
		helper.runAfterDelay(60, () -> {
			String drum = " | column " + read(helper, 0, 3);
			log(scene, "drum" + drum);
			check(helper, count(drumsOut, WildcatterItems.HFCL4_DRUM) >= 1, "a HfCl4 drum should drop into the hopper under the hafnium column" + drum);
			drumsOut.clearContent();
			kroll.toggleCampaign();
			krollIn.setItem(0, new ItemStack(WildcatterItems.HFCL4_DRUM));
			helper.runAfterDelay(80, () -> {
				String campaign = " | kroll " + read(helper, 0, 6);
				log(scene, "campaign" + campaign);
				check(helper, kroll.campaign() && read(helper, 0, 6).contains("HAFNIUM CAMPAIGN") && kroll.retortKg(1) > 0,
						"on a hafnium campaign retort B should be charged from the HfCl4 drum" + campaign);
				// hafnium hollows: the mill reads HAFNIUM HOLLOWS until its dies are changed to the rodlet's
				press.takeZirconium(1000);
				press.addMetalForTest(1.0, true);
				helper.runAfterDelay(20, () -> {
					String wrong = " | mill " + read(helper, 0, 23);
					check(helper, read(helper, 0, 23).contains("HAFNIUM HOLLOWS"), "hafnium hollows on the cladding dies should read HAFNIUM HOLLOWS" + wrong);
					while (mill.die() != 3) {
						mill.changeDies();
					}
					mill.addProgressForTest(Zirconium.hafniumRodletKg() - 0.002);
					helper.runAfterDelay(200, () -> {
						String rod = " | mill " + read(helper, 0, 23) + " | anneal " + read(helper, 0, 33);
						log(scene, "rodlet" + rod);
						check(helper, count(tubesOut, WildcatterItems.HAFNIUM_RODLET) >= 1, "a hafnium rodlet should drop into the hopper under the anneal furnace" + rod);
						log(scene, "ok");
						helper.succeed();
					});
				});
			});
		});
	}

	// ---- helpers for 17c2 ----------------------------------------------------------------------------------------------------------------
	static void cableRun(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			GridSceneKit.put(helper, "cable_industrial", x, U, z1);
		}
		for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
			GridSceneKit.put(helper, "cable_industrial", x2, U, z);
		}
	}

	static void pipe(GameTestHelper helper, int x, int y, int z) {
		GridSceneKit.put(helper, "gas_line", x, U + y, z);
	}

	static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int y, int z, dev.wildcatter.fluid.Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, U + y, z), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, U + y, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	/** Waits up to {@code ticks} for {@code done}, then runs {@code then}. */
	static void await(GameTestHelper helper, int ticks, java.util.function.BooleanSupplier done, Runnable then) {
		if (ticks <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(10, () -> await(helper, ticks - 10, done, then));
	}

	// ---- pidgeon (17c2) -------------------------------------------------------------------------------------------------------------------
	static void pidgeon(GameTestHelper helper) {
		String scene = "pidgeon";
		floor(helper, -12, -10, 16, 22);
		place(helper, WildcatterBlocks.PIDGEON_FURNACE, 0, 0);       // x 0-3, z 0-9, box (-1, 0)
		place(helper, WildcatterBlocks.LIME_KILN, 0, 14);            // x 0-2, z 14-16, box (-1, 14)
		place(helper, WildcatterBlocks.BAGGING_STATION, 0, 17);      // x 0-1, z 17-18, against the kiln's south side, box (-1, 17)
		place(helper, WildcatterBlocks.MAGNESIUM_CELL, 8, 0);        // x 8-10, z 0-5 (unpowered)
		pipe(helper, 4, 0, 5);
		source(helper, 5, 0, 5, dev.wildcatter.fluid.Product.PIPELINE_GAS, 0.2);   // the furnace's gas
		pipe(helper, 3, 0, 15);
		source(helper, 4, 0, 15, dev.wildcatter.fluid.Product.PIPELINE_GAS, 0.2);  // the kiln's
		HopperBlockEntity feed = hopper(helper, 1, U + 4, 5, new ItemStack(WildcatterItems.DOLIME_BAG, 2), new ItemStack(WildcatterItems.FERROSILICON_BAG, 1),
				new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.FLUORITE), 1));
		HopperBlockEntity ingotsOut = hopper(helper, 1, 1, 5);
		hopper(helper, 1, U + 14, 15, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.DOLOMITE), 2));
		HopperBlockEntity cellIn = hopper(helper, 9, U + 3, 2);
		GridSceneKit.genset(helper, -6, U, -8);
		cableRun(helper, -5, -8, -1, 17);
		dev.wildcatter.block.PidgeonFurnaceBlockEntity furnace = GridSceneKit.entity(helper, 0, U, 0, dev.wildcatter.block.PidgeonFurnaceBlockEntity.class);
		dev.wildcatter.block.LimeKilnBlockEntity kiln = GridSceneKit.entity(helper, 0, U, 14, dev.wildcatter.block.LimeKilnBlockEntity.class);
		dev.wildcatter.block.BaggingStationBlockEntity bagger = GridSceneKit.entity(helper, 0, U, 17, dev.wildcatter.block.BaggingStationBlockEntity.class);
		bagger.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 2));
		MagnesiumCellBlockEntity cell = GridSceneKit.entity(helper, 8, U, 0, MagnesiumCellBlockEntity.class);
		GridSceneKit.start(helper, -6, U, -8, () -> await(helper, 400, () -> read(helper, 0, 0).contains("reducing dolime") && read(helper, 0, 14).contains("burning dolomite"), () -> {
			String run = " | furnace " + read(helper, 0, 0) + " | kiln " + read(helper, 0, 14) + " | set " + read(helper, -6, -8);
			log(scene, "running" + run);
			check(helper, read(helper, 0, 14).contains("burning dolomite"), "the lime kiln should burn dolomite to dolime" + run);
			check(helper, read(helper, 0, 0).contains("reducing dolime") && furnace.crowns() > 0, "the Pidgeon furnace should make magnesium" + run);
			check(helper, read(helper, 0, 0).matches(".*: (9\\.[5-9]|10\\.0) kg/h of magnesium.*"), "at about 10 kg of magnesium an hour" + run);
			kiln.addDolimeForTest(Tuning.BULK_BAG_KG);
			furnace.addCrownsForTest(2 * Tuning.MG_INGOT_KG);
			furnace.finishCycleForTest();
			await(helper, 300, () -> count(ingotsOut, WildcatterItems.MAGNESIUM_INGOT) >= 2 && bagger.slots().stream().anyMatch(st -> st.is(WildcatterItems.DOLIME_BAG)), () -> {
				String cycle = " | furnace " + read(helper, 0, 0) + " | bagger " + read(helper, 0, 17);
				log(scene, "cycle" + cycle);
				check(helper, bagger.slots().stream().anyMatch(st -> st.is(WildcatterItems.DOLIME_BAG)), "the bagging station should fill a bag of dolime" + cycle);
				check(helper, count(ingotsOut, WildcatterItems.MAGNESIUM_INGOT) >= 2, "the cycle's end should drop magnesium ingots into the hopper under it" + cycle);
				double before = cell.mg();
				ingotsOut.clearContent();
				cellIn.setItem(0, new ItemStack(WildcatterItems.MAGNESIUM_INGOT, 2));
				helper.runAfterDelay(80, () -> {
					String melted = " | cell " + read(helper, 8, 0);
					log(scene, "cell" + melted);
					check(helper, cell.mg() >= before + 2 * Tuning.MG_INGOT_KG - 0.01, "the magnesium cell should take the ingots by its hopper" + melted);
					log(scene, "ok");
					helper.succeed();
				});
			});
		}));
	}

	// ---- chromium (17c2) ------------------------------------------------------------------------------------------------------------------
	static void chromium(GameTestHelper helper) {
		String scene = "chromium";
		floor(helper, -12, -10, 22, 12);
		place(helper, WildcatterBlocks.CHROMATE_KILN, 0, 0);          // x 0-2, z 0-7, box (-1, 0)
		place(helper, WildcatterBlocks.ALUMINOTHERMIC_VESSEL, 8, 0);  // x 8-10, z 0-2, box (7, 0)
		place(helper, WildcatterBlocks.VAR_FURNACE, 14, 0);           // x 14-16, z 0-2, box (13, 0)
		place(helper, WildcatterBlocks.VACUUM_DISTILLATION, 14, 3);   // x 14-16, z 3-5 (unpowered, given sponge by hook)
		pipe(helper, 1, 0, -1);
		source(helper, 1, 0, -2, dev.wildcatter.fluid.Product.CAUSTIC_SODA, 1.0);   // 32 % caustic, the north face of its front cells
		pipe(helper, 3, 0, 4);
		source(helper, 4, 0, 4, dev.wildcatter.fluid.Product.PIPELINE_GAS, 0.2);
		hopper(helper, 1, U + 3, 3, new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.CHROMITE), 1),
				new ItemStack(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.SULFUR), 1));
		HopperBlockEntity sacksOut = hopper(helper, 1, 1, 3);
		HopperBlockEntity vesselIn = hopper(helper, 9, U + 3, 1);
		HopperBlockEntity piecesOut = hopper(helper, 9, 1, 1);
		HopperBlockEntity varIn = hopper(helper, 15, U + 8, 1, new ItemStack(dev.wildcatter.ore.OreBlocks.INGOTS.get("tin")), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
		GridSceneKit.genset(helper, -6, U, -8);
		cableRun(helper, -5, -8, -1, 0);
		cableRun(helper, -1, -8, 7, 0);
		cableRun(helper, 7, -8, 13, 0);
		dev.wildcatter.block.ChromateKilnBlockEntity kiln = GridSceneKit.entity(helper, 0, U, 0, dev.wildcatter.block.ChromateKilnBlockEntity.class);
		dev.wildcatter.block.AluminothermicVesselBlockEntity vessel = GridSceneKit.entity(helper, 8, U, 0, dev.wildcatter.block.AluminothermicVesselBlockEntity.class);
		ZrLineBlockEntity var = GridSceneKit.entity(helper, 14, U, 0, ZrLineBlockEntity.class);
		GridSceneKit.entity(helper, 14, U, 3, ZrLineBlockEntity.class).addZirconium(5.0);
		GridSceneKit.start(helper, -6, U, -8, () -> await(helper, 400, () -> read(helper, 0, 0).contains("roasting chromite"), () -> {
			String roast = " | kiln " + read(helper, 0, 0);
			log(scene, "roasting" + roast);
			check(helper, read(helper, 0, 0).contains("roasting chromite") && read(helper, 0, 0).matches(".*: 2[45]\\.[0-9] kg/h of chromic oxide.*"), "the chromate kiln should roast chromite with caustic, a sack an hour" + roast);
			check(helper, kiln.chromate() > 0 && read(helper, 0, 0).contains("HEXAVALENT"), "it should hold chromate, and say so" + roast);
			kiln.addChromateForTest(Tuning.CR2O3_SACK_KG);
			await(helper, 200, () -> count(sacksOut, WildcatterItems.CHROMIC_OXIDE) >= 1, () -> {
				String sack = " | kiln " + read(helper, 0, 0);
				log(scene, "sack" + sack);
				check(helper, count(sacksOut, WildcatterItems.CHROMIC_OXIDE) >= 1, "a sack of chromic oxide should drop into the hopper under the kiln" + sack);
				// four sacks and two aluminium ingots to the vessel; fired, sped by hook
				vesselIn.setItem(0, new ItemStack(WildcatterItems.CHROMIC_OXIDE, Tuning.VESSEL_CR_SACKS));
				vesselIn.setItem(1, new ItemStack(WildcatterItems.ALUMINIUM_INGOT, Tuning.VESSEL_CR_ALUMINIUM));
				helper.runAfterDelay(80, () -> {
					String fired = vessel.fire(helper.getLevel());
					check(helper, fired.contains("fired chromium"), "the vessel should fire a chromium batch: " + fired + " | " + read(helper, 8, 0));
					vessel.testFinishFiring();
					await(helper, 200, () -> count(piecesOut, WildcatterItems.CHROMIUM_METAL) >= 6, () -> {
						String metal = " | vessel " + read(helper, 8, 0);
						log(scene, "metal" + metal);
						check(helper, count(piecesOut, WildcatterItems.CHROMIUM_METAL) >= 6, "the batch should make 6 pieces of chromium metal (67 kg) under the vessel" + metal);
						piecesOut.clearContent();
						varIn.setItem(2, new ItemStack(WildcatterItems.CHROMIUM_METAL, 1));
						await(helper, 300, () -> read(helper, 14, 0).contains("remelting into Zircaloy-4"), () -> {
							String melt = " | var " + read(helper, 14, 0);
							log(scene, "var" + melt);
							check(helper, read(helper, 14, 0).contains("remelting into Zircaloy-4") && read(helper, 14, 0).contains("chromium 10 kg"),
									"the VAR furnace should remelt with the chromium metal, no chromite" + melt);
							// broken while it holds chromate: the spill
							kiln.addChromateForTest(5);
							kiln.spill(helper.getLevel());
							java.util.List<net.minecraft.world.entity.AreaEffectCloud> clouds = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,
									net.minecraft.world.phys.AABB.encapsulatingFullBlocks(abs(helper, -3, 0, -3), abs(helper, 6, 6, 11)));
							check(helper, !clouds.isEmpty() && kiln.chromate() == 0, "a kiln broken with chromate in it should leave a poison spill");
							clouds.forEach(net.minecraft.world.entity.Entity::discard);
							log(scene, "ok");
							helper.succeed();
						});
					});
				});
			});
		}));
	}

	// ---- fumed_silica (17c2) --------------------------------------------------------------------------------------------------------------
	static void fumedSilica(GameTestHelper helper) {
		String scene = "fumed_silica";
		floor(helper, -12, -10, 14, 14);
		place(helper, WildcatterBlocks.CHLORINATOR, 0, 0);             // x 0-2, z 0-2 (unpowered, its SiCl4 by hook)
		place(helper, WildcatterBlocks.FUMED_SILICA_BURNER, 3, 2);     // x 3-4, z 2-4: against the chlorinator's east face, z 0 left for its chlorine
		place(helper, WildcatterBlocks.STOCK_TANK, 2, 6);              // x 2-5, z 6-9: the hydrochloric acid
		pipe(helper, 3, 0, 5);                                         // the acid out of the burner's back, into the tank
		pipe(helper, 5, 0, 4);
		source(helper, 6, 0, 4, dev.wildcatter.fluid.Product.HYDROGEN, 1.0);   // hydrogen, the east face of its bottom cells (a test tank)
		pipe(helper, 5, 1, 3);
		source(helper, 6, 1, 3, dev.wildcatter.fluid.Product.OXYGEN, 1.0);     // oxygen, the east face one up
		HopperBlockEntity sacksOut = hopper(helper, 3, 1, 3);
		GridSceneKit.genset(helper, 0, U, -8);
		cableRun(helper, 1, -8, 3, 1);                                 // its box, on the north face of its main cell
		dev.wildcatter.block.ChlorinatorBlockEntity chlorinator = GridSceneKit.entity(helper, 0, U, 0, dev.wildcatter.block.ChlorinatorBlockEntity.class);
		dev.wildcatter.block.FumedSilicaBurnerBlockEntity burner = GridSceneKit.entity(helper, 3, U, 2, dev.wildcatter.block.FumedSilicaBurnerBlockEntity.class);
		chlorinator.addSiCl4ForTest(Tuning.SICL4_HOLD_KG);
		GridSceneKit.start(helper, 0, U, -8, () -> await(helper, 400, () -> read(helper, 3, 2).contains("burning"), () -> {
			String burn = " | burner " + read(helper, 3, 2);
			log(scene, "burning" + burn);
			check(helper, read(helper, 3, 2).matches(".*burning 2[12]\\.[0-9] kg/h of SiCl4.*"), "the burner should burn the chlorinator's SiCl4 at its rate (18.6 kg/h and a fifth)" + burn);
			burner.addSilicaForTest(Tuning.FUMED_SILICA_SACK_KG);
			await(helper, 400, () -> count(sacksOut, WildcatterItems.FUMED_SILICA) >= 1, () -> {
				String tank = " | burner " + read(helper, 3, 2) + " | tank " + read(helper, 2, 6);
				log(scene, "sack" + tank);
				check(helper, count(sacksOut, WildcatterItems.FUMED_SILICA) >= 1, "a sack of fumed silica should drop into the hopper under the burner" + tank);
				dev.wildcatter.block.StorageTankBlockEntity acid = GridSceneKit.entity(helper, 2, U, 6, dev.wildcatter.block.StorageTankBlockEntity.class);
				check(helper, acid.product() == dev.wildcatter.fluid.Product.HYDROCHLORIC_ACID && acid.amount() > 0,
						"32 % hydrochloric acid should reach the tank on its pipe: " + acid.product() + " " + acid.amount() + tank);
				log(scene, "ok");
				helper.succeed();
			});
		}));
	}
}
