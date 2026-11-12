package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.genset;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.start;
import static dev.wildcatter.neoforge.gametest.NickelScenes.hopper;
import static dev.wildcatter.neoforge.gametest.NickelScenes.run;

import dev.wildcatter.Rules;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.AssemblyFixtureBlockEntity;
import dev.wildcatter.block.FuelLineBlockEntity;
import dev.wildcatter.block.FuelStage;
import dev.wildcatter.block.PelletInspectionBlockEntity;
import dev.wildcatter.block.RodInspectionBlockEntity;
import dev.wildcatter.block.RodWelderBlockEntity;
import dev.wildcatter.block.ScrapOxidizerBlockEntity;
import dev.wildcatter.fluid.Fuel;
import dev.wildcatter.item.FuelAssemblyTags;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Phase 19c's scenes (docs/multiblocks/fuel-fabrication-extensions.md, "Game tests"), each in a batch of its own with the rule it needs
 * switched on and handed back (the `fuel` client scene is left as it is: the line it films is the 7g line):
 * <ul>
 * <li>{@code fuel_quality} (19c1, the fuelLineDefects rule): the kiln, granulator (the scrap oxidizer against it), press, sintering
 * furnace, grinder, pellet inspection bench and rod loader in a line; the press without zinc stearate reads NO LUBRICANT and cracks a
 * tenth of its pellets to scrap; a scrap can by hopper, the oxidizer's U3O8 goes into the granulator; the bench rejects 1 % of the
 * pellets; a rod inspection line rejects one rod in 200; an assembly made with neither inspection part carries a 1 % leak chance.</li>
 * <li>{@code fuel_hardware} (19c2, the fuelNickelHardware rule): the IDR kiln (U in its autoclave, stand-in hydrogen) with the HF
 * recovery condenser against it fills a tank with 70 % aqueous HF, which the conversion plant's HF column distils over its HF kiln's
 * sulfuric acid; the rod loader set to 104 IFBA rods has the IFBA coater coat its pellets and takes nickel-chromium alloy for its
 * plugs; the fixture builds its skeleton of 304 and nickel alloy, and one given steel reads NO STAINLESS; an assembly of 104 IFBA rods
 * reads 3,000 pcm lower in a vessel than a plain one.</li>
 * </ul>
 */
final class FuelExtensionScenes {
	private FuelExtensionScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("fuel_quality", FuelExtensionScenes::fuelQuality);
		scenes.put("fuel_hardware", FuelExtensionScenes::fuelHardware);
		return scenes;
	}

	private static void check(GameTestHelper helper, boolean ok, String message, boolean[] rules) {
		if (!ok) {
			Rules.restore(rules);
		}
		helper.assertTrue(ok, Component.literal(message));
	}

	// ---- fuel_quality -------------------------------------------------------------------------------------------------------------
	static void fuelQuality(GameTestHelper helper) {
		String scene = "fuel_quality";
		boolean[] rules = Rules.snapshot();
		Rules.set(Rules.Rule.FUEL_LINE_DEFECTS, true);
		floor(helper, -12, -16, 26, 40);
		// line 1, down x 0: kiln, granulator (oxidizer against its east side), press, sintering furnace, grinder, pellet bench, rod loader
		place(helper, WildcatterBlocks.IDR_KILN, 0, U, -10, NORTH);            // z -10..-1
		place(helper, WildcatterBlocks.GRANULATOR, 0, U, 0, NORTH);            // z 0-3
		place(helper, WildcatterBlocks.SCRAP_OXIDIZER, 3, U, 0, NORTH);        // x 3-4, z 0-3, box (3, -1) north
		place(helper, WildcatterBlocks.PELLET_PRESS, 0, U, 4, NORTH);          // z 4-7
		place(helper, WildcatterBlocks.SINTERING_FURNACE, 0, U, 8, NORTH);     // z 8-19
		place(helper, WildcatterBlocks.PELLET_GRINDER, 0, U, 20, NORTH);       // z 20-23
		place(helper, WildcatterBlocks.PELLET_INSPECTION, 0, U, 24, NORTH);    // x 0-1, z 24-27
		place(helper, WildcatterBlocks.ROD_LOADER, 0, U, 28, NORTH);           // z 28-37
		helper.getLevel().setBlock(abs(helper, 1, U - 1, 5), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState(), 3);   // under the press
		// line 2, x 10: welder, rod inspection, fixture; line 3, x 20: welder and fixture, no inspection
		place(helper, WildcatterBlocks.ROD_WELDER, 10, U, 0, NORTH);           // z 0-5
		place(helper, WildcatterBlocks.ROD_INSPECTION, 10, U, 6, NORTH);       // z 6-15
		place(helper, WildcatterBlocks.ASSEMBLY_FIXTURE, 10, U, 16, NORTH);    // z 16-25
		place(helper, WildcatterBlocks.ROD_WELDER, 20, U, 0, NORTH);           // z 0-5
		place(helper, WildcatterBlocks.ASSEMBLY_FIXTURE, 20, U, 6, NORTH);     // z 6-15
		// 480 V: a diesel set west of the kiln, along x -1 and z -13, down each line's west side to its parts' boxes
		genset(helper, -6, U, -4);                                             // 480 V at (-5, -4)
		run(helper, "cable_industrial", new int[] {-5, -4}, new int[] {-2, -4});
		run(helper, "cable_industrial", new int[] {-1, 37}, new int[] {-1, -13}, new int[] {19, -13});
		run(helper, "cable_industrial", new int[] {3, -12}, new int[] {3, -1});
		run(helper, "cable_industrial", new int[] {9, -12}, new int[] {9, 16});
		run(helper, "cable_industrial", new int[] {19, -12}, new int[] {19, 6});
		FuelLineBlockEntity press = entity(helper, 0, U, 4, FuelLineBlockEntity.class);
		ScrapOxidizerBlockEntity oxidizer = entity(helper, 3, U, 0, ScrapOxidizerBlockEntity.class);
		PelletInspectionBlockEntity bench = entity(helper, 0, U, 24, PelletInspectionBlockEntity.class);
		RodInspectionBlockEntity rodCheck = entity(helper, 10, U, 6, RodInspectionBlockEntity.class);
		AssemblyFixtureBlockEntity bare = entity(helper, 20, U, 6, AssemblyFixtureBlockEntity.class);
		start(helper, -6, U, -4, () -> {
			// uranium in the line, as if made: powder in the kiln and the granulator, pellets in the sintering furnace
			entity(helper, 0, U, -10, FuelStage.class).addFuelU(50);
			entity(helper, 0, U, 0, FuelStage.class).addFuelU(20);
			entity(helper, 0, U, 8, FuelStage.class).addFuelU(50);
			hopper(helper, 3, U + 3, 1, WildcatterComponents.withAssay(new ItemStack(WildcatterItems.UO2_SCRAP_CAN), Tuning.ASSAY_PRODUCT));
			entity(helper, 10, U, 0, RodWelderBlockEntity.class).setRackForTest(200);
			entity(helper, 20, U, 0, RodWelderBlockEntity.class).setRackForTest(1);
			bare.setRodsForTest(Tuning.ASSEMBLY_RODS - 1);
			helper.runAfterDelay(400, () -> {
				String line = " | press " + read(helper, 0, U, 4) + " | granulator " + read(helper, 0, U, 0) + " | oxidizer " + read(helper, 3, U, 0)
						+ " | grinder " + read(helper, 0, U, 20) + " | bench " + read(helper, 0, U, 24);
				log(scene, "line:" + line);
				check(helper, read(helper, 0, U, 4).contains("NO LUBRICANT") && press.scrapTotal() > 0, "without zinc stearate the press should crack pellets to scrap: " + line, rules);
				check(helper, oxidizer.given() > 0 && read(helper, 0, U, 0).contains("recycled U3O8"), "the oxidizer's U3O8 should go into the granulator: " + line, rules);
				double share = bench.rejected() / bench.passed();
				check(helper, bench.passed() > 0.1 && Math.abs(share - Tuning.PELLET_REJECT) < 0.001, "the bench should reject 1 % of the pellets: " + share + line, rules);
				rodCheck.testInspect(200);
				String rods = read(helper, 10, U, 6);
				log(scene, "rods: " + rods);
				check(helper, rodCheck.inspected() == 200 && rodCheck.rejected() == 1 && rodCheck.rack() == 199, "the rod line should reject one rod in 200: " + rods, rules);
				ItemStack made = bare.slots().get(3);
				String fixture = read(helper, 20, U, 6);
				log(scene, "bare fixture: " + fixture + " | leak " + FuelAssemblyTags.leakChance(made));
				check(helper, made.is(WildcatterBlocks.FUEL_ASSEMBLY.asItem()) && Math.abs(FuelAssemblyTags.leakChance(made) - 0.01) < 1e-9,
						"an assembly made with no inspection should carry a 1 % leak chance: " + made + " " + FuelAssemblyTags.leakChance(made) + " | " + fixture, rules);
				// a press with zinc stearate cracks none
				press.slots().set(0, new ItemStack(WildcatterItems.ZINC_STEARATE, 1));
				double before = press.scrapTotal();
				helper.runAfterDelay(100, () -> {
					String lubed = read(helper, 0, U, 4);
					log(scene, "ok: " + lubed);
					check(helper, !lubed.contains("NO LUBRICANT") && press.scrapTotal() == before, "with zinc stearate the press should crack none: " + lubed, rules);
					Rules.restore(rules);
					helper.succeed();
				});
			});
		});
	}

	// ---- fuel_hardware ------------------------------------------------------------------------------------------------------------
	static void fuelHardware(GameTestHelper helper) {
		String scene = "fuel_hardware";
		boolean[] rules = Rules.snapshot();
		Rules.set(Rules.Rule.FUEL_NICKEL_HARDWARE, true);
		floor(helper, -12, -10, 62, 32);
		// HF: the autoclave and the IDR kiln, the condenser against the kiln, its aqueous HF to a tank, on to an HF column and its kiln
		place(helper, WildcatterBlocks.UF6_AUTOCLAVE, 0, U, 0, NORTH);         // z 0-3
		place(helper, WildcatterBlocks.IDR_KILN, 0, U, 4, NORTH);              // z 4-13, hydrogen east
		place(helper, WildcatterBlocks.HF_RECOVERY_CONDENSER, 3, U, 12, NORTH); // x 3-4, z 12-13, box (3, 11) north, aqueous HF (4, 13) south
		place(helper, WildcatterBlocks.STOCK_TANK, 4, U, 15, NORTH);           // x 4-7, z 15-18
		place(helper, WildcatterBlocks.HF_COLUMN, 8, U, 17, NORTH);            // x 8-9, z 17-18, aqueous HF on its north side
		place(helper, WildcatterBlocks.HF_KILN, 10, U, 15, NORTH);             // x 10-12, z 15-24, against the column; acid east
		place(helper, WildcatterBlocks.STOCK_TANK, 14, U, 14, NORTH);          // x 14-17, z 14-17: sulfuric acid
		put(helper, "gas_line", 3, U, 6);
		NickelScenes.source(helper, 4, 6, dev.wildcatter.fluid.Product.HYDROGEN, 1.0);
		put(helper, "gas_line", 4, U, 14);
		put(helper, "gas_line", 8, U, 16);
		put(helper, "gas_line", 13, U, 15);
		// IFBA and hardware, x 30: grinder, loader (the coater against its east side), welder, fixture; x 40: a welder and a fixture on steel
		place(helper, WildcatterBlocks.PELLET_GRINDER, 30, U, 0, NORTH);       // z 0-3
		place(helper, WildcatterBlocks.ROD_LOADER, 30, U, 4, NORTH);           // z 4-13
		place(helper, WildcatterBlocks.IFBA_COATER, 33, U, 4, NORTH);          // x 33-35, z 4-9, box (33, 3) north
		place(helper, WildcatterBlocks.ROD_WELDER, 30, U, 14, NORTH);          // z 14-19
		place(helper, WildcatterBlocks.ASSEMBLY_FIXTURE, 30, U, 20, NORTH);    // z 20-29
		place(helper, WildcatterBlocks.ROD_WELDER, 40, U, 14, NORTH);
		place(helper, WildcatterBlocks.ASSEMBLY_FIXTURE, 40, U, 20, NORTH);
		// the vessels, cold and open
		place(helper, WildcatterBlocks.REACTOR_VESSEL, 50, U, 0, NORTH);
		place(helper, WildcatterBlocks.REACTOR_VESSEL, 56, U, 0, NORTH);
		// 480 V: a diesel set, along z -6, down to each part's box
		genset(helper, -6, U, -6);                                             // 480 V at (-5, -6)
		run(helper, "cable_industrial", new int[] {-5, -6}, new int[] {39, -6});
		run(helper, "cable_industrial", new int[] {-1, -5}, new int[] {-1, 4});
		run(helper, "cable_industrial", new int[] {5, -5}, new int[] {5, 11}, new int[] {3, 11});
		run(helper, "cable_industrial", new int[] {29, -5}, new int[] {29, 20});
		run(helper, "cable_industrial", new int[] {33, -5}, new int[] {33, 3});
		run(helper, "cable_industrial", new int[] {39, -5}, new int[] {39, 20});
		entity(helper, 14, U, 14, dev.wildcatter.block.StorageTankBlockEntity.class).testFill(dev.wildcatter.fluid.Product.SULFURIC_ACID, 5_000);
		entity(helper, 0, U, 4, dev.wildcatter.block.WaterUser.class).addWater(100_000);
		hopper(helper, 31, U + 3, 5, new ItemStack(WildcatterItems.ZIRCALOY_TUBE, 16), new ItemStack(WildcatterItems.NICKEL_CHROMIUM_INGOT, 1));
		hopper(helper, 34, U + 3, 5, new ItemStack(WildcatterItems.ZIRCALOY_TUBE, 2), new ItemStack(WildcatterItems.BORIC_ACID, 1));
		hopper(helper, 31, U + 3, 15, new ItemStack(WildcatterItems.HELIUM_CYLINDER, 1));
		hopper(helper, 31, U + 6, 21, new ItemStack(WildcatterItems.GUIDE_THIMBLE, 25), new ItemStack(WildcatterItems.SPACER_GRID, 6),
				new ItemStack(WildcatterItems.STAINLESS_304_INGOT, 1), new ItemStack(WildcatterItems.NICKEL_CHROMIUM_INGOT, 1));
		hopper(helper, 41, U + 6, 21, new ItemStack(WildcatterItems.GUIDE_THIMBLE, 25), new ItemStack(WildcatterItems.SPACER_GRID, 6),
				new ItemStack(WildcatterItems.STEEL_INGOT, 1));
		FuelLineBlockEntity loader = entity(helper, 30, U, 4, FuelLineBlockEntity.class);
		loader.nextIfba();
		loader.nextIfba();
		loader.nextIfba();   // 104
		dev.wildcatter.block.HfRecoveryCondenserBlockEntity condenser = entity(helper, 3, U, 12, dev.wildcatter.block.HfRecoveryCondenserBlockEntity.class);
		dev.wildcatter.block.HfColumnBlockEntity column = entity(helper, 8, U, 17, dev.wildcatter.block.HfColumnBlockEntity.class);
		dev.wildcatter.block.IfbaCoaterBlockEntity coater = entity(helper, 33, U, 4, dev.wildcatter.block.IfbaCoaterBlockEntity.class);
		start(helper, -6, U, -6, () -> {
			entity(helper, 0, U, 0, FuelStage.class).addFuelU(50);    // UF6 vaporized, as if from a 30B
			entity(helper, 30, U, 0, FuelStage.class).addFuelU(20);   // ground pellets
			helper.runAfterDelay(500, () -> {
				String rods = " | loader " + read(helper, 30, U, 4) + " | coater " + read(helper, 33, U, 4);
				log(scene, "ifba:" + rods);
				check(helper, coater.coated() >= 1 && read(helper, 30, U, 4).contains("104 IFBA"), "the coater should coat the loader's IFBA rods: " + rods, rules);
				check(helper, !read(helper, 30, U, 4).contains("NO NICKEL ALLOY") && read(helper, 30, U, 4).contains("nickel alloy"), "the loader should take nickel alloy for its plugs: " + rods, rules);
				String fixtures = " | 304 fixture " + read(helper, 30, U, 20) + " | steel fixture " + read(helper, 40, U, 20);
				log(scene, "fixtures:" + fixtures);
				check(helper, read(helper, 30, U, 20).contains("pulling rods") && read(helper, 30, U, 20).contains("304 84 kg"),
						"the fixture should build its skeleton of 304 and nickel alloy: " + fixtures, rules);
				check(helper, read(helper, 40, U, 20).contains("NO STAINLESS"), "with the rule a fixture given steel should read NO STAINLESS: " + fixtures, rules);
				// an IFBA assembly and a plain one, each loaded into a vessel
				ItemStack ifba = new ItemStack(WildcatterBlocks.FUEL_ASSEMBLY);
				FuelAssemblyTags.setIfbaRods(ifba, 104);
				dev.wildcatter.block.ReactorVesselBlockEntity v1 = entity(helper, 50, U, 0, dev.wildcatter.block.ReactorVesselBlockEntity.class);
				dev.wildcatter.block.ReactorVesselBlockEntity v2 = entity(helper, 56, U, 0, dev.wildcatter.block.ReactorVesselBlockEntity.class);
				String a = v1.load(ifba);
				String b = v2.load(new ItemStack(WildcatterBlocks.FUEL_ASSEMBLY));
				double pcm = (v2.reactor().assemblyRho(0) - v1.reactor().assemblyRho(0)) * 1e5;
				log(scene, "ifba in a vessel: " + Math.round(pcm) + " pcm lower | " + a + " | " + b);
				check(helper, a.contains("loaded") && b.contains("loaded") && Math.abs(pcm - 3000) < 1, "104 IFBA rods should read 3,000 pcm lower in a vessel: " + pcm + " | " + a, rules);
				helper.runAfterDelay(1600, () -> {   // over a minute of the kiln's HF: the tank offers it from half a kg
					String hf = " | kiln " + read(helper, 0, U, 4) + " | condenser " + read(helper, 3, U, 12) + " | tank " + read(helper, 4, U, 15) + " | column "
							+ read(helper, 8, U, 17);
					log(scene, "hf:" + hf);
					check(helper, condenser.made() > 0, "the condenser should make aqueous HF from the kiln's: " + hf, rules);
					check(helper, column.recovered() > 0, "the HF column should distil the aqueous HF over the kiln's acid: " + hf, rules);
					log(scene, "ok");
					Rules.restore(rules);
					helper.succeed();
				});
			});
		});
	}
}
