package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.block.BaggingStationBlockEntity;
import dev.wildcatter.block.BoronLineBlockEntity;
import dev.wildcatter.block.DecantTowerBlockEntity;
import dev.wildcatter.block.ReactorVesselBlockEntity;
import dev.wildcatter.fluid.ControlRods;
import dev.wildcatter.fluid.Reactor;
import dev.wildcatter.item.RccaTags;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Phase 19d's server scene (docs/multiblocks/reactor-extensions.md items 9-11), with the rule {@code reactorRodWear} on.
 * <ul>
 * <li>{@code rods}: a cold vessel loaded by right-click with nine assemblies and five hafnium RCCAs reads each at 85 % of a B4C RCCA's
 * worth and its shutdown margin above 1,300 pcm; four worn B4C RCCAs (60 % of their B-10 burnt, carried on the item) in place of the
 * edge ones read REPLACE and SDM LOW; bank D's B4C RCCA brought to 30 % by hook reads REPLACE. A bagging station against a borax
 * dissolver fills a 1 t bag of sodium sulfate, which a hopper on a lined cell's decant tower dumps into the cell.</li>
 * </ul>
 */
final class RodScenes {
	private RodScenes() {
	}

	static final Map<String, List<String>> RULES = Map.of("rods", List.of("reactorRodWear"));

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("rods", RodScenes::rods);
		return scenes;
	}

	static final int U = 2;

	static void rods(GameTestHelper helper) {
		String scene = "rods";
		floor(helper, -4, -10, 30, 12);
		GridSceneKit.place(helper, WildcatterBlocks.REACTOR_VESSEL, 4, U, 4, Direction.NORTH);       // x 4-6, z 4-6
		GridSceneKit.place(helper, WildcatterBlocks.BORAX_DISSOLVER, 12, U, 0, Direction.NORTH);     // x 12-14, z 0-2 (unpowered)
		GridSceneKit.place(helper, WildcatterBlocks.BAGGING_STATION, 12, U, -2, Direction.NORTH);    // x 12-13, z -2..-1, against its north side
		// a lined cell, 7 x 7 and 3 high, its floor at y 1, the decant tower inside its north wall, a hopper on the tower
		for (int x = 20; x <= 26; x++) {
			for (int z = 0; z <= 6; z++) {
				GridSceneKit.put(helper, "cell_liner", x, U - 1, z);
				if (x == 20 || x == 26 || z == 0 || z == 6) {
					for (int y = U; y <= U + 2; y++) {
						GridSceneKit.put(helper, "cell_liner", x, y, z);
					}
				}
			}
		}
		GridSceneKit.put(helper, "decant_tower", 23, U, 1);
		helper.getLevel().setBlock(abs(helper, 23, U + 1, 1), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState(), 3);
		HopperBlockEntity onTower = (HopperBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 23, U + 1, 1));
		// the bagging station's 5 kW: a diesel set, cable east and south to its box west of its main cell
		GridSceneKit.genset(helper, 6, U, -8);
		for (int x = 7; x <= 11; x++) {
			GridSceneKit.put(helper, "cable_industrial", x, U, -8);
		}
		for (int z = -7; z <= -2; z++) {
			GridSceneKit.put(helper, "cable_industrial", 11, U, z);
		}
		BaggingStationBlockEntity bagger = GridSceneKit.entity(helper, 12, U, -2, BaggingStationBlockEntity.class);
		bagger.slots().set(0, new ItemStack(WildcatterItems.EMPTY_BULK_BAG, 2));
		BoronLineBlockEntity dissolver = GridSceneKit.entity(helper, 12, U, 0, BoronLineBlockEntity.class);
		ReactorVesselBlockEntity vessel = GridSceneKit.entity(helper, 4, U, 4, ReactorVesselBlockEntity.class);
		DecantTowerBlockEntity tower = (DecantTowerBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 23, U, 1));

		helper.runAfterDelay(20, () -> {
			// the rods: nine assemblies and five hafnium RCCAs, by right-click on the cold vessel
			for (int i = 0; i < Tuning.CORE_ASSEMBLIES; i++) {
				vessel.load(new ItemStack(WildcatterBlocks.FUEL_ASSEMBLY));
			}
			for (int i = 0; i < Tuning.RCCAS; i++) {
				vessel.load(new ItemStack(WildcatterItems.HAFNIUM_RCCA));
			}
			Reactor r = vessel.reactor();
			String hf = GridSceneKit.read(helper, 4, U, 4);
			log(scene, "hafnium | " + hf);
			check(helper, r.rccas() == Tuning.RCCAS && r.rccaKind(0) == ControlRods.HAFNIUM && Math.abs(r.rccaWorthShare(0) - 0.85) < 1e-9,
					"five hafnium RCCAs should load, each at 85 % of a B4C RCCA's worth: " + hf);
			check(helper, hf.contains("RCCAs Hf, Hf, Hf, Hf, Hf") && r.rodShutdownMargin() > Tuning.SHUTDOWN_MARGIN && !hf.contains("SDM LOW"),
					String.format("the hafnium set should read its margin (%.0f pcm), over 1,300: %s", r.rodShutdownMargin() * 1e5, hf));
			// four worn B4C RCCAs at the edges: REPLACE, and the margin under 1,300 pcm
			for (int i = 0; i < 4; i++) {
				r.unloadRcca();
			}
			for (int i = 0; i < 4; i++) {
				vessel.load(RccaTags.withB10(new ItemStack(WildcatterItems.RCCA), 0.60));
			}
			String worn = GridSceneKit.read(helper, 4, U, 4);
			log(scene, "worn | " + worn);
			check(helper, worn.contains("SDM LOW") && worn.contains("60 % B-10 REPLACE") && r.rodShutdownMargin() < Tuning.SHUTDOWN_MARGIN,
					"worn B4C RCCAs should read REPLACE and SDM LOW: " + worn);
			// a fresh B4C set; bank D's brought to 30 % of its B-10 by hook
			while (r.unloadRcca()) {
			}
			while (r.loadRcca()) {
			}
			r.setRccaB10ForTest(0, Tuning.B10_REPLACE);
			String bankD = GridSceneKit.read(helper, 4, U, 4);
			log(scene, "bank D | " + bankD);
			check(helper, bankD.contains("RCCAs B4C 30 % B-10 REPLACE, B4C 0 % B-10") && !bankD.contains("SDM LOW"),
					"bank D's RCCA at 30 % B-10 should read REPLACE, the rest fresh: " + bankD);
			// the sodium sulfate: a bag at the dissolver's bagging station, dumped through the tower's hopper
			dissolver.addSulfateForTest(Tuning.BULK_BAG_KG + 10);
			GridSceneKit.start(helper, 6, U, -8, () -> helper.runAfterDelay(100, () -> {
				String bagged = " | bagger " + GridSceneKit.read(helper, 12, U, -2) + " | dissolver " + GridSceneKit.read(helper, 12, U, 0);
				log(scene, "bagged" + bagged);
				ItemStack bag = ItemStack.EMPTY;
				for (ItemStack s : bagger.slots()) {
					if (s.is(WildcatterItems.SODIUM_SULFATE_BAG)) {
						bag = s.copy();
						s.setCount(0);
					}
				}
				check(helper, !bag.isEmpty(), "the bagging station should fill a bag of sodium sulfate" + bagged);
				onTower.setItem(0, bag);
				helper.runAfterDelay(120, () -> {
					String dumped = " | tower " + tower.inspect().getString();
					log(scene, "dumped" + dumped);
					check(helper, tower.sulfateKg() >= Tuning.BULK_BAG_KG - 1 && dumped.contains("1.0 t sodium sulfate"), "the bag should be dumped in the lined cell" + dumped);
					log(scene, "ok");
					helper.succeed();
				});
			}));
		});
	}
}
