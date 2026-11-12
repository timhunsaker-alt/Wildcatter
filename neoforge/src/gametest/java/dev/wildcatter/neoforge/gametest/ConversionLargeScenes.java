package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.ChemicalScenes.await;
import static dev.wildcatter.neoforge.gametest.ChemicalScenes.peaker;
import static dev.wildcatter.neoforge.gametest.ChemicalScenes.run;
import static dev.wildcatter.neoforge.gametest.ChemicalScenes.source;
import static dev.wildcatter.neoforge.gametest.ChemicalScenes.startPeaker;
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
import dev.wildcatter.block.ConversionStageBlockEntity;
import dev.wildcatter.block.CylinderFillerBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.RectifierBlockEntity;
import dev.wildcatter.block.ReductionReactorBlockEntity;
import dev.wildcatter.fluid.Conversion;
import dev.wildcatter.fluid.Product;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 16b2's scene (docs/multiblocks/conversion-extensions.md, "A full-size plant"), {@code conversion_large}: four fluorine cell houses in
 * series on DC bus bars from the 12.47 kV rectifier transformer (the chlor-alkali scene's peaker and substation), the large reduction
 * reactor on U3O8 drums and test hydrogen, the large hydrofluorinator on test HF, the large fluorinator on the four houses' fluorine, the
 * large cold traps and columns and a cylinder filler. The rectifier reads about 432 V and 6 kA; the fluorinator runs near 0.3 kg of U a
 * second; a 48Y brought near full by hook fills.
 */
final class ConversionLargeScenes {
	private ConversionLargeScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("conversion_large", ConversionLargeScenes::conversionLarge);
		return scenes;
	}

	/** A run of DC bus bar at height dy between the corners (x, z), both ends included. */
	private static void bar(GameTestHelper helper, int dy, int[]... corners) {
		for (int i = 0; i + 1 < corners.length; i++) {
			int[] a = corners[i];
			int[] b = corners[i + 1];
			int dx = Integer.signum(b[0] - a[0]);
			int dz = Integer.signum(b[1] - a[1]);
			for (int x = a[0], z = a[1];; x += dx, z += dz) {
				put(helper, "dc_bus_16k", x, U + dy, z);
				if (x == b[0] && z == b[1]) {
					break;
				}
			}
		}
	}

	/** A bus bar rising from y U to y U+2 at (x, z). */
	private static void rise(GameTestHelper helper, int x, int z) {
		for (int dy = 0; dy <= 2; dy++) {
			put(helper, "dc_bus_16k", x, U + dy, z);
		}
	}

	static void conversionLarge(GameTestHelper helper) {
		String scene = "conversion_large";
		floor(helper, -42, -46, 32, 30);
		GasTurbineBlockEntity gt = peaker(helper, -40, -30);
		run(helper, "cable_dist", new int[] {-4, -35}, new int[] {0, -35}, new int[] {0, -8}, new int[] {-7, -8}, new int[] {-7, 0});
		place(helper, WildcatterBlocks.RECTIFIER_MV, -6, U, 0, NORTH);         // x -6..-4, z 0-3: + (-3, U+2, 2), - (-3, U, 2)
		int[] houses = {0, 8, 16, 24};                                          // x 0-5, 8-13, 16-21, 24-29; z 0-11; terminals west at z 6
		for (int x : houses) {
			place(helper, WildcatterBlocks.FLUORINE_CELL_HOUSE, x, U, 0, NORTH);
			run(helper, "gas_line", new int[] {x + 2, -1});
			source(helper, x + 2, -2, Product.ANHYDROUS_HF, 1.0);                                       // HF to its front
			for (int dy = 0; dy <= 3; dy++) {
				put(helper, "gas_line", x + 2, U + dy, 12);                                             // its fluorine up to the header
			}
		}
		for (int x = -27; x <= 26; x++) {
			put(helper, "gas_line", x, U + 3, 12);                                                      // the fluorine header, y U+3
		}
		for (int dy = 0; dy <= 2; dy++) {
			put(helper, "gas_line", -27, U + dy, 12);
		}
		put(helper, "gas_line", -27, U, 11);                                                            // to the large fluorinator's east side
		// the DC: + to the first house, each - to the next +, the last - back
		bar(helper, 2, new int[] {-3, 2}, new int[] {-3, 6}, new int[] {-1, 6});
		bar(helper, 0, new int[] {-1, 6}, new int[] {-1, 13}, new int[] {7, 13});
		rise(helper, 7, 13);
		bar(helper, 2, new int[] {7, 13}, new int[] {7, 6});
		bar(helper, 0, new int[] {7, 6}, new int[] {7, -3}, new int[] {15, -3});
		rise(helper, 15, -3);
		bar(helper, 2, new int[] {15, -3}, new int[] {15, 6});
		bar(helper, 0, new int[] {15, 6}, new int[] {15, 13}, new int[] {23, 13});
		rise(helper, 23, 13);
		bar(helper, 2, new int[] {23, 13}, new int[] {23, 6});
		bar(helper, 0, new int[] {23, 6}, new int[] {23, -5}, new int[] {-3, -5}, new int[] {-3, 2});
		// the large main line, x -30..-28, reagents on its east side (x -27)
		place(helper, WildcatterBlocks.REDUCTION_REACTOR_LARGE, -30, U, 0, NORTH);        // z 0-2
		place(helper, WildcatterBlocks.HYDROFLUORINATOR_LARGE, -30, U, 3, NORTH);         // z 3-8
		place(helper, WildcatterBlocks.FLUORINATOR_LARGE, -30, U, 9, NORTH);              // z 9-11
		place(helper, WildcatterBlocks.COLD_TRAPS_LARGE, -30, U, 12, NORTH);              // z 12-17, box (-31, 12)
		place(helper, WildcatterBlocks.DISTILLATION_COLUMNS_LARGE, -30, U, 18, NORTH);    // z 18-20, box (-31, 18)
		place(helper, WildcatterBlocks.CYLINDER_FILLER, -30, U, 21, NORTH);               // z 21-26, box (-31, 21)
		run(helper, "gas_line", new int[] {-27, 1});
		source(helper, -26, 1, Product.HYDROGEN, 1.0);
		run(helper, "gas_line", new int[] {-27, 5});
		source(helper, -26, 5, Product.ANHYDROUS_HF, 1.0);
		genset(helper, -36, U, 8);
		run(helper, "cable_industrial", new int[] {-35, 8}, new int[] {-35, 21}, new int[] {-31, 21});
		run(helper, "cable_industrial", new int[] {-35, 12}, new int[] {-31, 12});
		run(helper, "cable_industrial", new int[] {-35, 18}, new int[] {-31, 18});
		ReductionReactorBlockEntity reducer = entity(helper, -30, U, 0, ReductionReactorBlockEntity.class);
		reducer.slots().set(0, new ItemStack(WildcatterItems.U3O8_DRUM, 8));
		CylinderFillerBlockEntity filler = entity(helper, -30, U, 21, CylinderFillerBlockEntity.class);
		filler.slots().set(0, new ItemStack(WildcatterItems.EMPTY_48Y_CYLINDER, 2));
		ConversionStageBlockEntity fluorinator = entity(helper, -30, U, 9, ConversionStageBlockEntity.class);
		RectifierBlockEntity rectifier = entity(helper, -6, U, 0, RectifierBlockEntity.class);
		int[][] wet = {{0, 0}, {8, 0}, {16, 0}, {24, 0}};
		start(helper, -36, U, 8, () -> { });
		startPeaker(helper, -40, -30, gt, () -> await(helper, 2400, wet, () -> rectifier.dc().amps() > 5700 && fluorinator.uraniumThrough() > 1, () -> {
			double before = fluorinator.uraniumThrough();
			helper.runAfterDelay(200, () -> {
				double rate = (fluorinator.uraniumThrough() - before) / 10.0;
				String chain = String.format(" | %.3f kg/s of U through the fluorinator", rate) + " | rectifier " + read(helper, -6, U, 0) + " | house 1 " + read(helper, 0, U, 0)
						+ " | house 4 " + read(helper, 24, U, 0) + " | reducer " + read(helper, -30, U, 0) + " | hydrofluorinator " + read(helper, -30, U, 3)
						+ " | fluorinator " + read(helper, -30, U, 9) + " | traps " + read(helper, -30, U, 12) + " | columns " + read(helper, -30, U, 18) + " | filler " + read(helper, -30, U, 21);
				log(scene, "running:" + chain);
				double volts = rectifier.dc().volts();
				double amps = rectifier.dc().amps();
				check(helper, near(volts, Tuning.F2_HOUSES_IN_SERIES * Tuning.F2_CELLS * Tuning.F2_CELL_VOLTS, 0.05), "four houses in series should read about 432 V: " + volts + chain);
				check(helper, near(amps, Tuning.F2_CELL_KA * 1000, 0.05), "at about 6 kA: " + amps + chain);
				check(helper, near(rate, Conversion.uraniumKgs() * Tuning.CONVERSION_LARGE_SCALE, 0.25), "the large fluorinator should take about 0.3 kg of U a second" + chain);
				filler.addUf6(Tuning.CYLINDER_48Y_KG - filler_uf6_margin());
				await(helper, 600, wet, () -> filler.full() >= 1, () -> {
					String filled = read(helper, -30, U, 21);
					check(helper, filler.full() >= 1, "the large line should fill the 48Y: " + filled + " | columns " + read(helper, -30, U, 18));
					log(scene, "ok: " + filled);
					helper.succeed();
				});
			});
		}));
	}

	/** kg of UF6 left for the line to put in the cylinder: a few seconds' worth at the large rate. */
	private static double filler_uf6_margin() {
		return Conversion.uraniumKgs() * Tuning.CONVERSION_LARGE_SCALE * Tuning.UF6_PER_U * 5;
	}
}
