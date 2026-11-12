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
}
