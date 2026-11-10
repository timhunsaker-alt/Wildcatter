package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BayerStageBlockEntity;
import dev.wildcatter.block.CoalTowerBlockEntity;
import dev.wildcatter.block.CokeOvenBatteryBlockEntity;
import dev.wildcatter.block.CokeQuenchTowerBlockEntity;
import dev.wildcatter.fluid.Product;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Residence times as real holds (plant-crane-gaps #11, Tim's answer 15), with the world rule {@code realResidenceTimes} on for the
 * batch ({@link RuleScenes.RulesOn}):
 * <ul>
 * <li>{@code residence_holds}: a coke oven battery against its coal tower, pusher and quench tower; a Bayer digester against its flash
 * tanks, on bauxite slurry and steam; a precipitator on polished liquor; all on one diesel set. Each takes its feed at the rate but
 * sends nothing on until its residence time has passed: coal coking in the ovens and no coke pushed, slurry in the digesters and none in
 * the flash tanks, liquor in the train and no hydrate out. Aged by hook (18 h, 1 h, 36 h), each sends its first product on.</li>
 * <li>{@code time_scale_holds}: the same plant in a world at 50:1 (docs/time-scale.md): aged a fiftieth of each hold (and a bucket),
 * each sends its first product on; at 1:1 that age would leave every hold full.</li>
 * </ul>
 */
final class ResidenceScenes {
	private ResidenceScenes() {
	}

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
		out.put("residence_holds", h -> residenceHolds(h, "residence_holds"));
		out.put("time_scale_holds", h -> residenceHolds(h, "time_scale_holds"));
		return out;
	}

	private static void cable(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				put(helper, "cable_industrial", x, 1, z);
				if (helper.getLevel().getBlockEntity(abs(helper, x, 1, z)) instanceof dev.wildcatter.block.MultiCableBlockEntity c) {
					c.setSize(dev.wildcatter.energy.ConductorSize.KCMIL_500);
				}
			}
		}
	}

	private static void busway(GameTestHelper helper, int x1, int z1, int x2, int z2) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				put(helper, "busway_industrial", x, 1, z);
			}
		}
	}

	private static TestGasSource.SourceEntity source(GameTestHelper helper, int x, int z, Product product, double rate) {
		helper.getLevel().setBlock(abs(helper, x, 1, z), BuiltInRegistries.BLOCK.getValue(WildcatterNeoForgeGameTests.id("gas_source")).defaultBlockState(), 3);
		TestGasSource.SourceEntity s = (TestGasSource.SourceEntity) helper.getLevel().getBlockEntity(abs(helper, x, 1, z));
		s.product = product;
		s.rate = rate;
		return s;
	}

	private static void await(GameTestHelper helper, int ticks, BooleanSupplier done, Runnable then) {
		if (ticks <= 0 || done.getAsBoolean()) {
			then.run();
			return;
		}
		helper.runAfterDelay(10, () -> await(helper, ticks - 10, done, then));
	}

	/** Every part's selector to OFF, so they do not all start at once on one set; then each back to HAND, 60 ticks apart. */
	private static void release(GameTestHelper helper, int[][] parts, int i, Runnable then) {
		if (i >= parts.length) {
			then.run();
			return;
		}
		dev.wildcatter.block.PlantUnitBlockEntity unit = entity(helper, parts[i][0], 1, parts[i][1], dev.wildcatter.block.PlantUnitBlockEntity.class);
		unit.cycleHoa(null);   // OFF to AUTO
		unit.cycleHoa(null);   // AUTO to HAND
		helper.runAfterDelay(60, () -> release(helper, parts, i + 1, then));
	}

	static void residenceHolds(GameTestHelper helper, String scene) {
		int scale = dev.wildcatter.TimeScale.scale();
		check(helper, scale == (scene.equals("time_scale_holds") ? 50 : 1), scene + " should run at " + (scene.equals("time_scale_holds") ? 50 : 1) + ":1, not " + scale);
		floor(helper, -16, -8, 36, 40);
		// the Bayer side
		place(helper, WildcatterBlocks.DIGESTER, 0, 1, 0, Direction.NORTH);         // x 0-4, z 0-4
		place(helper, WildcatterBlocks.FLASH_TANKS, 0, 1, 5, Direction.NORTH);      // x 0-2, z 5-10
		place(helper, WildcatterBlocks.PRECIPITATOR, 24, 1, 0, Direction.NORTH);    // x 24-35, z 0-11
		put(helper, "gas_line", 1, 1, -1);
		source(helper, 1, -2, Product.BAUXITE_SLURRY, 100);
		put(helper, "gas_line", 5, 1, 1);
		source(helper, 6, 1, Product.PROCESS_STEAM, 20);
		put(helper, "gas_line", 25, 1, -1);
		source(helper, 25, -2, Product.POLISHED_LIQUOR, 100);
		// the coke side
		place(helper, WildcatterBlocks.COKE_OVEN_BATTERY, 0, 1, 20, Direction.NORTH);    // x 0-9, z 20-35
		place(helper, WildcatterBlocks.COAL_TOWER, 3, 1, 16, Direction.NORTH);           // x 3-6, z 16-19
		place(helper, WildcatterBlocks.PUSHER_MACHINE, -4, 1, 21, Direction.NORTH);      // x -4..-1, z 21-36
		place(helper, WildcatterBlocks.COKE_QUENCH_TOWER, 14, 1, 29, Direction.SOUTH);   // x 10-14, z 25-29; its box east
		CoalTowerBlockEntity tower = entity(helper, 3, 1, 16, CoalTowerBlockEntity.class);
		tower.slots().set(0, new ItemStack(Items.COAL_BLOCK, 64));
		CokeQuenchTowerBlockEntity quench = entity(helper, 14, 1, 29, CokeQuenchTowerBlockEntity.class);
		quench.addWater((int) Tuning.CARBON_WATER_HOLD);
		// one diesel set's 480 V: a busway along z -6 and down x -9, a cable to each box
		GridSceneKit.genset(helper, -10, 1, -6);                                          // its terminal at (-9, 1, -6)
		busway(helper, -9, -6, 23, -6);
		busway(helper, -9, -5, -9, 21);
		cable(helper, -1, -5, -1, 0);      // the digester's box, west of its main cell
		cable(helper, -2, 0, -2, 5);       // the flash tanks' box at (-1, 5), off the digester's run
		cable(helper, -1, 5, -1, 5);
		cable(helper, 23, -5, 23, 0);      // the precipitator's
		cable(helper, -8, 16, 2, 16);      // the coal tower's
		cable(helper, -8, 21, -5, 21);     // the pusher's
		cable(helper, -8, 20, -1, 20);     // the battery's
		cable(helper, -8, 38, 15, 38);     // the quench tower's, round the south
		cable(helper, -9, 22, -9, 38);
		cable(helper, 15, 30, 15, 37);
		put(helper, "cable_industrial", 15, 1, 29);
		CokeOvenBatteryBlockEntity battery = entity(helper, 0, 1, 20, CokeOvenBatteryBlockEntity.class);
		BayerStageBlockEntity digester = entity(helper, 0, 1, 0, BayerStageBlockEntity.class);
		BayerStageBlockEntity precipitator = entity(helper, 24, 1, 0, BayerStageBlockEntity.class);
		check(helper, dev.wildcatter.Rules.realResidenceTimes(), "the batch should have realResidenceTimes on");
		int[][] parts = {{0, 5}, {0, 0}, {24, 0}, {3, 16}, {-4, 21}, {14, 29}, {0, 20}};
		for (int[] p : parts) {
			entity(helper, p[0], 1, p[1], dev.wildcatter.block.PlantUnitBlockEntity.class).cycleHoa(null);   // HAND to OFF
		}
		GridSceneKit.start(helper, -10, 1, -6, () -> release(helper, parts, 0, () -> await(helper, 1200, () -> battery.inOvens() > 1 && digester.inProcess() > 1 && precipitator.inProcess() > 1, () -> {
			String in = " | battery " + read(helper, 0, 1, 20) + " | digester " + read(helper, 0, 1, 0) + " | flash " + read(helper, 0, 1, 5) + " | precipitator " + read(helper, 24, 1, 0);
			check(helper, battery.inOvens() > 1 && digester.inProcess() > 1 && precipitator.inProcess() > 1, "each part should take its feed into its hold" + in);
			check(helper, battery.pushedFromHold() == 0, "no coke should be pushed before its 18 h" + in);
			check(helper, read(helper, 0, 1, 5).contains("idle"), "no slurry should reach the flash tanks before its hour" + in);
			check(helper, precipitator.holding(Product.HYDRATE_SLURRY) < 1e-6, "no hydrate should leave before its 36 h" + in);
			log(scene, "holding:" + in);
			battery.ageForTest(Tuning.COKING_HOLD_S / scale + 1800);              // and a bucket: each hold is counted in its buckets' widths
			digester.ageForTest(Tuning.DIGESTION_HOLD_S / scale + 60);
			precipitator.ageForTest(Tuning.PRECIPITATION_HOLD_S / scale + 1800);
			await(helper, 400, () -> read(helper, 0, 1, 5).contains("flashing") && precipitator.holding(Product.HYDRATE_SLURRY) > 1 && battery.pushedFromHold() > 0, () -> {
				String out = " | battery " + read(helper, 0, 1, 20) + " | flash " + read(helper, 0, 1, 5) + " | precipitator " + read(helper, 24, 1, 0) + " | quench " + read(helper, 14, 1, 29);
				check(helper, battery.pushedFromHold() > 0, "after 18 h the ovens should push their coke" + out);
				check(helper, read(helper, 0, 1, 5).contains("flashing"), "after an hour the digested slurry should reach the flash tanks" + out);
				check(helper, precipitator.holding(Product.HYDRATE_SLURRY) > 1, "after 36 h hydrate slurry should leave the precipitator" + out);
				log(scene, "ok at " + scale + ":1: " + String.format("%.1f kg of coke pushed", battery.pushedFromHold()) + out);
				helper.succeed();
			});
		})));
	}
}
