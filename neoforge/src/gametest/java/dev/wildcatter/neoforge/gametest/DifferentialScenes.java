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

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BusRelayPanelBlockEntity;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GeneratorPartBlockEntity;
import dev.wildcatter.block.MultiCableBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.TransformerBlockEntity;
import dev.wildcatter.circuit.ClampHeating;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.fluid.Product;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NeoForge server scenes of the protection gap wave, group B (docs/questions/protection-gaps.md, gen-bus-protection.md):
 * <ul>
 * <li>{@code unit_differentials}: a gas turbine on its bus duct, a 25 MVA GSU, a 138 kV yard breaker and a 10 MVA step-down. Grounds
 * on the 10 MVA unit's 4.16 kV leads trip its 87T, which opens the 138 kV breaker alone, and the clamps survive the five-cycle fault;
 * grounds on the GSU's 138 kV leads trip the GSU's 87T, which opens the breaker and the generator; grounds on the generator's
 * terminals trip its 87G.</li>
 * <li>{@code gsu_backfeed}: a 138 kV line (a diesel set stepped up through a 10 MVA unit and a yard breaker) behind a GSU straight off a
 * bus with a bus relay panel; grounds on the bus trip 87B, and 86B transfer-trips the line-side breaker, so the fault is cleared.</li>
 * <li>{@code clamp_burn}: grounds left on a live 480 V cable with their rating lowered for the test burn off and leave it UNGROUNDED.</li>
 * </ul>
 */
final class DifferentialScenes {
	private DifferentialScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("unit_differentials", DifferentialScenes::unitDifferentials);
		scenes.put("gsu_backfeed", DifferentialScenes::gsuBackfeed);
		scenes.put("clamp_burn", DifferentialScenes::clampBurn);
		return scenes;
	}

	private static void cables(GameTestHelper helper, String id, int[]... cells) {
		for (int[] c : cells) {
			put(helper, id, c[0], c[1], c[2]);
		}
	}

	private static void grounds(GameTestHelper helper, int x, int y, int z, boolean on) {
		entity(helper, x, y, z, dev.wildcatter.safety.Groundable.class).setGrounded(on);
	}

	/** The 480 V set at (x, U, z) facing north, and a unit substation run backward east of it: 4.16 kV out at x + 5. */
	private static void stationPower(GameTestHelper helper, int x, int z) {
		genset(helper, x, U, z);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, x + 4, U, z, Direction.SOUTH);
	}

	// ---- unit_differentials -------------------------------------------------------------------------------------------------------------
	static void unitDifferentials(GameTestHelper helper) {
		floor(helper, -2, -10, 48, 24);
		place(helper, WildcatterBlocks.GAS_TURBINE, 20, U, 0, NORTH);          // x 20-23, z 0-9
		place(helper, WildcatterBlocks.GT_INLET_HOUSE, 20, U, -4, NORTH);      // x 20-23, z -4..-1
		place(helper, WildcatterBlocks.GT_GENERATOR, 24, U, 1, NORTH);         // x 24-27, z 1-6
		place(helper, WildcatterBlocks.GEN_BREAKER, 25, U, 7, NORTH);          // x 25-26, z 7-8
		place(helper, WildcatterBlocks.HRSG, 20, U, 10, NORTH);                // its exhaust
		place(helper, WildcatterBlocks.STOCK_TANK, 14, U, 4, NORTH);           // kerosene
		put(helper, "gas_line", 18, U, 6);
		put(helper, "gas_line", 19, U, 6);
		for (int x = 25; x <= 31; x++) {
			put(helper, "bus_duct_gen", x, U, 9);                               // the generator breaker's back to the GSU's throat
		}
		put(helper, "bus_duct_gen", 31, U, 8);
		place(helper, WildcatterBlocks.GSU_25, 31, U, 7, NORTH);               // x 29-33, z 4-7, its bushings over (31, 4)
		for (int h = 0; h <= 4; h++) {
			put(helper, "cable_hv", 31, U + h, 3);                              // a riser off the bushings' front face
		}
		for (int z = -3; z <= 2; z++) {
			put(helper, "cable_hv", 31, U, z);                                  // north to the yard breaker
		}
		place(helper, WildcatterBlocks.HV_BREAKER_3P, 32, U, -7, NORTH);       // x 32-33, z -7..1, the line through z -3
		cables(helper, "cable_hv", new int[] {34, U, -3}, new int[] {35, U, -3});
		for (int h = 0; h <= 4; h++) {
			put(helper, "cable_hv", 36, U + h, -3);                             // up to the 10 MVA unit's bushings
		}
		place(helper, WildcatterBlocks.SUBSTATION_TRANSFORMER_10, 40, U, -3, Direction.WEST);   // x 37-40, z -5..-1
		for (int x = 41; x <= 44; x++) {
			put(helper, "cable_mv", x, U, -3);                                  // its 4.16 kV leads, open-ended
		}
		stationPower(helper, 10, 2);                                           // 4.16 kV to the starter at x 15-19
		for (int x = 15; x <= 19; x++) {
			put(helper, "cable_mv", x, U, 2);
		}
		entity(helper, 14, U, 4, StorageTankBlockEntity.class).testFill(Product.KEROSENE, 40_000);
		GasTurbineBlockEntity gt = entity(helper, 20, U, 0, GasTurbineBlockEntity.class);
		start(helper, 10, U, 2, () -> {
			gt.setWantRun(true);
			helper.runAfterDelay(200, () -> {
				gt.skipStartForTest();
				helper.runAfterDelay(200, () -> unitChecks(helper, gt));
			});
		});
	}

	private static void unitChecks(GameTestHelper helper, GasTurbineBlockEntity gt) {
		String scene = "unit_differentials";
		String running = read(helper, 20, U, 0);
		String unit10 = read(helper, 40, U, -3);
		String gsu = read(helper, 31, U, 7);
		log(scene, "up | " + running + " | " + unit10 + " | " + gsu + " | " + read(helper, 32, U, -7));
		check(helper, running.contains("breaker CLOSED"), "the gas turbine should be on its bus: " + running);
		check(helper, unit10.contains("87T ready") && unit10.contains("bounded by 1 breaker"), "the 10 MVA unit's 87T should find the 138 kV breaker: " + unit10);
		check(helper, gsu.contains("87T ready") && gsu.contains("bounded by 2 breakers"), "the GSU's 87T should find the generator breaker and the 138 kV breaker: " + gsu);
		check(helper, read(helper, 32, U, -7).contains("CLOSED"), "the 138 kV breaker should be closed: " + read(helper, 32, U, -7));
		// ---- 87T on the 10 MVA unit: grounds on its 4.16 kV leads
		grounds(helper, 42, U, -3, true);
		helper.runAfterDelay(20, () -> {
			String trip = read(helper, 40, U, -3);
			String hv = read(helper, 32, U, -7);
			MultiCableBlockEntity lead = entity(helper, 42, U, -3, MultiCableBlockEntity.class);
			log(scene, "87T 10 MVA | " + trip + " | " + hv + " | " + read(helper, 20, U, 0) + " | " + read(helper, 31, U, 7) + " | " + read(helper, 42, U, -3));
			check(helper, trip.contains("87T TRANSFORMER DIFFERENTIAL TRIP") && trip.contains("LOCKED OUT (86T)"), "grounds on its leads should trip the 10 MVA unit's 87T: " + trip);
			check(helper, hv.contains("TRIPPED") && hv.contains("87T"), "87T should open the 138 kV breaker: " + hv);
			check(helper, read(helper, 20, U, 0).contains("breaker CLOSED") && !read(helper, 31, U, 7).contains("DIFFERENTIAL TRIP"),
					"a fault past the 138 kV breaker is outside the GSU's zone: the generator stays on: " + read(helper, 20, U, 0));
			check(helper, lead.grounded() && !lead.burnedOff() && lead.clampHeating() < 1, "the clamps should survive a fault cleared in five cycles: " + read(helper, 42, U, -3));
			grounds(helper, 42, U, -3, false);
			entity(helper, 40, U, -3, TransformerBlockEntity.class).resetLockout();
			helper.runAfterDelay(20, () -> {
				check(helper, read(helper, 32, U, -7).contains("CLOSED"), "86T's reset should close the 138 kV breaker again: " + read(helper, 32, U, -7));
				gsuFault(helper, gt);
			});
		});
	}

	private static void gsuFault(GameTestHelper helper, GasTurbineBlockEntity gt) {
		String scene = "unit_differentials";
		// ---- 87T on the GSU: grounds on its 138 kV leads, between it and the yard breaker
		grounds(helper, 31, U, 0, true);
		helper.runAfterDelay(20, () -> {
			String trip = read(helper, 31, U, 7);
			String gen = read(helper, 24, U, 1);
			String hv = read(helper, 32, U, -7);
			log(scene, "87T GSU | " + trip + " | " + gen + " | " + hv);
			check(helper, trip.contains("87T TRANSFORMER DIFFERENTIAL TRIP"), "grounds on its 138 kV leads should trip the GSU's 87T: " + trip);
			check(helper, gen.contains("TRIPPED") && gen.contains("87T"), "the GSU's 87T should trip the generator: " + gen);
			check(helper, hv.contains("TRIPPED"), "the GSU's 87T should open the 138 kV breaker: " + hv);
			grounds(helper, 31, U, 0, false);
			entity(helper, 31, U, 7, TransformerBlockEntity.class).resetLockout();
			helper.runAfterDelay(100, () -> {
				gt.skipStartForTest();
				helper.runAfterDelay(200, () -> {
					String back = read(helper, 20, U, 0);
					log(scene, "reset | " + back + " | " + read(helper, 32, U, -7));
					check(helper, back.contains("breaker CLOSED") && read(helper, 32, U, -7).contains("CLOSED"),
							"after 86T's reset the generator should resynchronize and the 138 kV breaker close: " + back + " | " + read(helper, 32, U, -7));
					generatorFault(helper);
				});
			});
		});
	}

	private static void generatorFault(GameTestHelper helper) {
		String scene = "unit_differentials";
		// ---- 87G: grounds on the generator's terminals
		grounds(helper, 24, U, 1, true);
		helper.runAfterDelay(20, () -> {
			String gen = read(helper, 24, U, 1);
			String gsu = read(helper, 31, U, 7);
			log(scene, "87G | " + gen + " | " + gsu);
			check(helper, gen.contains("TRIPPED") && gen.contains("87G") && gen.contains("GROUNDED"), "grounds on its terminals should trip the generator's 87G: " + gen);
			check(helper, gsu.contains("87T ready"), "a generator fault is outside the GSU's 87T zone: " + gsu);
			grounds(helper, 24, U, 1, false);
			log(scene, "ok");
			helper.succeed();
		});
	}

	// ---- gsu_backfeed -------------------------------------------------------------------------------------------------------------------
	static void gsuBackfeed(GameTestHelper helper) {
		floor(helper, -2, -6, 36, 6);
		stationPower(helper, 6, 0);                                            // 4.16 kV out at x 11
		put(helper, "cable_mv", 11, U, 0);
		place(helper, WildcatterBlocks.SUBSTATION_TRANSFORMER_10, 12, U, 0, Direction.EAST);   // x 12-15, z -2..2, a step-up
		for (int h = 0; h <= 4; h++) {
			put(helper, "cable_hv", 16, U + h, 0);
		}
		cables(helper, "cable_hv", new int[] {17, U, 0}, new int[] {18, U, 0});
		place(helper, WildcatterBlocks.HV_BREAKER_3P, 19, U, -4, NORTH);       // x 19-20, z -4..4, the line through z 0
		cables(helper, "cable_hv", new int[] {21, U, 0}, new int[] {22, U, 0}, new int[] {23, U, 0});
		for (int h = 0; h <= 4; h++) {
			put(helper, "cable_hv", 24, U + h, 0);                              // to the GSU's bushings
		}
		place(helper, WildcatterBlocks.GSU_25, 28, U, 0, Direction.WEST);      // x 25-28, z -2..2, its 13.8 kV throat east
		for (int x = 29; x <= 32; x++) {
			put(helper, "bus_duct_gen", x, U, 0);
		}
		place(helper, WildcatterBlocks.BUS_RELAY_PANEL, 30, U, 1, NORTH);     // against the bus
		String scene = "gsu_backfeed";
		start(helper, 6, U, 0, () -> helper.runAfterDelay(40, () -> {
			String panel = read(helper, 30, U, 1);
			String bus = read(helper, 32, U, 0);
			log(scene, "up | " + panel + " | " + bus + " | " + read(helper, 19, U, -4));
			check(helper, panel.contains("1 line-side breaker behind a GSU"), "the panel should find the GSU's line-side breaker: " + panel);
			check(helper, bus.matches(".*A [1-9][0-9]{3,} V.*"),
					"the bus should be live from the 138 kV line: " + bus);
			grounds(helper, 32, U, 0, true);
			helper.runAfterDelay(20, () -> {
				String trip = read(helper, 30, U, 1);
				String hv = read(helper, 19, U, -4);
				MultiCableBlockEntity duct = entity(helper, 32, U, 0, MultiCableBlockEntity.class);
				log(scene, "bus fault | " + trip + " | " + hv + " | " + read(helper, 32, U, 0) + " | " + read(helper, 28, U, 0));
				check(helper, trip.contains("87B BUS DIFFERENTIAL TRIP") && trip.contains("LOCKED OUT"), "grounds on the bus should trip 87B: " + trip);
				check(helper, hv.contains("TRIPPED"), "86B should transfer-trip the GSU's line-side breaker: " + hv);
				check(helper, duct.amps(Conductor.A) < 1, "with the line-side breaker open nothing feeds the fault: " + read(helper, 32, U, 0));
				grounds(helper, 32, U, 0, false);
				entity(helper, 30, U, 1, BusRelayPanelBlockEntity.class).reset();
				TransformerBlockEntity gsu = entity(helper, 28, U, 0, TransformerBlockEntity.class);
				if (gsu.lockedOut()) {
					gsu.resetLockout();   // its own 87T saw the bus fault too (the bus is inside its zone)
				}
				helper.runAfterDelay(40, () -> {
					String back = read(helper, 19, U, -4);
					log(scene, "reset | " + back + " | " + read(helper, 32, U, 0));
					check(helper, back.contains("CLOSED"), "the panel's reset should close the line-side breaker again: " + back);
					log(scene, "ok");
					helper.succeed();
				});
			});
		}));
	}

	// ---- clamp_burn ---------------------------------------------------------------------------------------------------------------------
	static void clampBurn(GameTestHelper helper) {
		floor(helper, -2, -4, 16, 4);
		genset(helper, 6, U, 0);
		for (int x = 7; x <= 11; x++) {
			put(helper, "busway_industrial", x, U, 0);   // 800 A busway, so the conductor outlasts the clamps
		}
		String scene = "clamp_burn";
		start(helper, 6, U, 0, () -> {
			ClampHeating.testLimit = 1.0e6;   // a set rated for 1/300th of the real 35 kA for 15 cycles, so a genset's fault burns it
			grounds(helper, 10, U, 0, true);
			helper.runAfterDelay(60, () -> {
				ClampHeating.testLimit = null;
				MultiCableBlockEntity cable = entity(helper, 10, U, 0, MultiCableBlockEntity.class);
				String text = read(helper, 10, U, 0);
				log(scene, text + " | " + read(helper, 6, U, 0));
				check(helper, cable.burnedOff() && !cable.grounded() && text.contains("BURNED OFF"), "clamps past their I²t should burn off: " + text);
				log(scene, "ok");
				helper.succeed();
			});
		});
	}
}
