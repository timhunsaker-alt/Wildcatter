package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.entity;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BoilerBlockEntity;
import dev.wildcatter.block.ContainmentDoorBlockEntity;
import dev.wildcatter.block.ContainmentMonitorBlockEntity;
import dev.wildcatter.block.ReactorConsoleBlockEntity;
import dev.wildcatter.block.ReactorVesselBlockEntity;
import dev.wildcatter.block.ReliefTankBlockEntity;
import dev.wildcatter.fluid.Reactor;
import dev.wildcatter.fluid.ReactorRules;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

/**
 * Phase 19a's scenes (docs/multiblocks/reactor-extensions.md). {@code containment}: a concrete building round a reactor vessel with its
 * equipment hatch, personnel airlock, a recombiner, a powered igniter and the monitor reads CLOSED and its volume, OPEN with the hatch
 * open; the core damaged by hook makes hydrogen that the igniter burns with no blast; the igniter's power cut and the recombiner taken
 * out, the lower compartment reaches 4 % and blasts. {@code reactor_limits}: each rule switched on in turn and handed back: LTOP, the
 * P-T curve and the PORVs into the relief tank until its disc bursts; a heat-up over 55 C/h; and an OT-DELTA-T trip at 8 % on natural
 * circulation. Each rule scene runs in its own batch, so no other scene sees a rule on.
 */
final class ReactorExtensionScenes {
	private ReactorExtensionScenes() {
	}

	static java.util.Map<String, Consumer<GameTestHelper>> scenes() {
		java.util.Map<String, Consumer<GameTestHelper>> out = new java.util.LinkedHashMap<>();
		out.put("containment", ReactorExtensionScenes::containment);
		out.put("reactor_limits", ReactorExtensionScenes::reactorLimits);
		return out;
	}

	/** The scenes that switch a rule on: each in a batch of its own. */
	static final java.util.Set<String> RULE_SCENES = java.util.Set.of("reactor_limits");

	/** A check that hands the rules back before it fails. */
	private static void check(GameTestHelper helper, boolean ok, String message, boolean[] rules) {
		if (!ok && rules != null) {
			ReactorRules.restore(rules);
		}
		helper.assertTrue(ok, Component.literal(message));
	}

	/** Waits up to {@code ticks} for {@code done}, then runs {@code then}. */
	private static void await(GameTestHelper helper, int ticks, Supplier<Boolean> done, Runnable then) {
		if (ticks <= 0 || done.get()) {
			then.run();
			return;
		}
		helper.runAfterDelay(10, () -> await(helper, ticks - 10, done, then));
	}

	// ---- containment ------------------------------------------------------------------------------------------------------------------
	/** The building: floor, walls and roof of containment concrete over x 0-18, z 0-18, y 1-14, leaving the hatch's and airlock's holes. */
	private static void building(GameTestHelper helper) {
		for (int x = 0; x <= 18; x++) {
			for (int z = 0; z <= 18; z++) {
				for (int y = 1; y <= 14; y++) {
					boolean shell = x == 0 || x == 18 || z == 0 || z == 18 || y == 1 || y == 14;
					boolean hatch = z == 18 && x >= 6 && x <= 9 && y >= 2 && y <= 5;
					boolean airlock = z == 0 && x >= 12 && x <= 13 && y >= 2 && y <= 4;
					if (shell && !hatch && !airlock) {
						put(helper, "containment_concrete", x, y, z);
					}
				}
			}
		}
	}

	static void containment(GameTestHelper helper) {
		floor(helper, -3, -3, 22, 22);
		building(helper);
		place(helper, WildcatterBlocks.REACTOR_VESSEL, 4, 2, 4, Direction.NORTH);          // x 4-6, z 4-6, y 2-11
		place(helper, WildcatterBlocks.EQUIPMENT_HATCH, 6, 2, 18, Direction.NORTH);        // x 6-9, y 2-5, in the south wall
		place(helper, WildcatterBlocks.PERSONNEL_AIRLOCK, 12, 2, 0, Direction.NORTH);      // x 12-13, y 2-4, z 0-2, through the north wall
		place(helper, WildcatterBlocks.HYDROGEN_RECOMBINER, 10, 2, 4, Direction.NORTH);
		// the igniter's power: a 120 V starter plant inside, its hot wire to the igniter and the neutral back
		helper.setBlock(8, 2, 14, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		helper.setBlock(8, 3, 14, WildcatterBlocks.BOILER);
		helper.setBlock(9, 3, 14, WildcatterBlocks.STEAM_TURBINE);
		helper.getBlockEntity(new BlockPos(8, 3, 14), BoilerBlockEntity.class).addWater(4000);
		put(helper, "wool_wire_hot", 10, 3, 14);
		put(helper, "wool_wire_hot", 11, 3, 14);
		helper.setBlock(12, 3, 14, WildcatterBlocks.HYDROGEN_IGNITER);
		for (int x = 9; x <= 12; x++) {
			put(helper, "wool_wire_neutral", x, 3, 15);
		}
		place(helper, WildcatterBlocks.CONTAINMENT_MONITOR, 15, 2, 15, Direction.NORTH);   // reads the air from (15, 2, 14)
		ReactorVesselBlockEntity vessel = entity(helper, 4, 2, 4, ReactorVesselBlockEntity.class);
		ContainmentMonitorBlockEntity monitor = entity(helper, 15, 2, 15, ContainmentMonitorBlockEntity.class);
		ContainmentDoorBlockEntity hatch = entity(helper, 6, 2, 18, ContainmentDoorBlockEntity.class);
		helper.runAfterDelay(200, () -> {
			String closed = monitor.refill(helper.getLevel());
			check(helper, monitor.closed() && monitor.volume() > 2500 && monitor.volume() < 3500, "the building should read CLOSED, about 3,000 m3: " + closed, null);
			check(helper, closed.contains("1 reactor vessels inside") && closed.contains("1 recombiners") && closed.contains("(1 powered)"),
					"it should find the vessel, the recombiner and the powered igniter: " + closed + " | igniter " + read(helper, 12, 3, 14), null);
			hatch.toggle();
			String open = monitor.refill(helper.getLevel());
			check(helper, !monitor.closed() && open.contains("OPEN"), "with the hatch open it should read OPEN and where: " + open, null);
			hatch.toggle();
			monitor.refill(helper.getLevel());
			check(helper, monitor.closed(), "shut again it should read CLOSED: " + read(helper, 15, 2, 15), null);
			log("containment", "closed: " + closed + " | open: " + open);
			// loss of cooling by hook: a hot core with a long run behind it, drained to a quarter, the fuel at 1,150 C
			Reactor r = vessel.reactor();
			vessel.setHotStandbyForTest(1500);
			r.setHistoryForTest(1.0);
			r.setSetpoint(-1);
			r.setInventoryForTest(0.25 * Tuning.RCS_WATER_KG);
			r.setFuelForTest(1150);
			await(helper, 1600, () -> monitor.burns() > 0 || monitor.blasted(), () -> {
				String burned = read(helper, 15, 2, 15);
				check(helper, r.coreDamaged() && r.h2() > 4, "the core should be damaged and make hydrogen: " + read(helper, 4, 2, 4), null);
				check(helper, monitor.burns() > 0 && !monitor.blasted() && !r.exploded(), "the powered igniter should burn the hydrogen with no blast: " + burned, null);
				check(helper, monitor.domeH2() < Tuning.H2_FLAMMABLE, "the dome should stay under 4 %: " + burned, null);
				log("containment", "burned: " + burned + " | vessel H2 " + r.h2());
				// the igniter's power cut and the recombiner taken out: the lower compartment reaches 4 % and blasts
				helper.setBlock(10, 3, 14, Blocks.AIR);
				helper.setBlock(10, 2, 4, Blocks.AIR);
				helper.setBlock(10, 3, 4, Blocks.AIR);
				helper.runAfterDelay(30, () -> {
					monitor.refill(helper.getLevel());
					check(helper, monitor.closed(), "the building should still be closed: " + read(helper, 15, 2, 15), null);
					monitor.addHydrogenForTest(Tuning.H2_KG_PER_M3 * Tuning.CONTAINMENT_LOWER_M3 * 0.05);
					await(helper, 200, monitor::blasted, () -> {
						String blast = read(helper, 15, 2, 15);
						check(helper, monitor.blasted() && r.exploded() && read(helper, 4, 2, 4).contains("HYDROGEN EXPLOSION"),
								"unpowered, with no recombiner, the lower compartment should blast: " + blast + " | " + read(helper, 4, 2, 4), null);
						check(helper, helper.getLevel().getBlockState(abs(helper, 0, 8, 8)).is(WildcatterBlocks.CONTAINMENT_CONCRETE), "the containment's concrete should stand", null);
						log("containment", "ok: blast " + blast);
						helper.succeed();
					});
				});
			});
		});
	}

	// ---- reactor_limits -----------------------------------------------------------------------------------------------------------------
	static void reactorLimits(GameTestHelper helper) {
		boolean[] rules = ReactorRules.snapshot();
		floor(helper, -6, -2, 14, 16);
		place(helper, WildcatterBlocks.REACTOR_VESSEL, 4, 1, 4, Direction.NORTH);            // x 4-6, z 4-6, y 1-10
		place(helper, WildcatterBlocks.CRDM_HEAD, 4, 11, 4, Direction.NORTH);
		place(helper, WildcatterBlocks.PRESSURIZER, 4, 1, 7, Direction.NORTH);               // x 4-5, z 7-8
		place(helper, WildcatterBlocks.PRESSURIZER_RELIEF_TANK, 6, 1, 7, Direction.NORTH);   // x 6-7, z 7-12, against the pressurizer
		place(helper, WildcatterBlocks.REACTOR_CONSOLE, 7, 1, 4, Direction.NORTH);           // x 7-9, against the vessel's east face
		// the CRDM head's 480 V: a diesel set west, its terminal up a riser to the head's box (the west face of its main cell)
		GridSceneKit.genset(helper, 0, 1, 2);
		put(helper, "cable_industrial", 1, 1, 2);
		put(helper, "cable_industrial", 2, 1, 2);
		put(helper, "cable_industrial", 3, 1, 2);
		for (int y = 1; y <= 11; y++) {
			put(helper, "cable_industrial", 3, y, 3);
		}
		put(helper, "cable_industrial", 3, 11, 4);
		ReactorVesselBlockEntity vessel = entity(helper, 4, 1, 4, ReactorVesselBlockEntity.class);
		ReliefTankBlockEntity tank = entity(helper, 6, 1, 7, ReliefTankBlockEntity.class);
		ReactorConsoleBlockEntity console = entity(helper, 7, 1, 4, ReactorConsoleBlockEntity.class);
		Reactor r = vessel.reactor();
		vessel.setHotStandbyForTest(1500);
		r.setColdForTest();
		// LTOP: the cold RCS at 3.0 MPa, not armed
		ReactorRules.ltop = true;
		r.setPressureForTest(3.0);
		helper.runAfterDelay(20, () -> {
			String cold = read(helper, 4, 1, 4);
			check(helper, cold.contains("LTOP NOT ARMED"), "cold and not armed it should alarm LTOP NOT ARMED: " + cold, rules);
			r.setPressureForTest(3.6);
			helper.runAfterDelay(10, () -> {
				String over = read(helper, 4, 1, 4);
				check(helper, over.contains("P-T LIMIT") && r.ptViolations() >= 1, "at 3.6 MPa cold it should alarm P-T LIMIT: " + over, rules);
				String armed = console.toggleLtop(helper.getLevel());
				check(helper, armed.contains("LTOP ARMED"), "the console should arm LTOP: " + armed, rules);
				r.setPressureForTest(3.6);
				helper.runAfterDelay(10, () -> {
					String held = read(helper, 4, 1, 4) + " | " + read(helper, 6, 1, 7);
					check(helper, Math.abs(r.pressure() - Tuning.LTOP_MPA) < 0.05 && tank.waterC() > 30.5, "armed, the PORVs should hold 3.2 MPa into the relief tank, warming it: " + held, rules);
					log("reactor_limits", "LTOP: " + held);
					// the tank near its disc's pressure; a few more lifts burst it
					tank.setWaterForTest(163);
					lifts(helper, r, tank, 12, () -> {
						String burst = read(helper, 6, 1, 7);
						check(helper, tank.burst() && burst.contains("BURST"), "the relief tank should burst its disc at 0.7 MPa: " + burst, rules);
						log("reactor_limits", "relief tank: " + burst);
						heatup(helper, r, rules);
					});
				});
			});
		});
	}

	/** Lifts the PORVs (the pressurizer set to 3.6 MPa with LTOP armed) every second until the tank's disc bursts. */
	private static void lifts(GameTestHelper helper, Reactor r, ReliefTankBlockEntity tank, int left, Runnable then) {
		if (tank.burst() || left <= 0) {
			then.run();
			return;
		}
		r.setPressureForTest(3.6);
		helper.runAfterDelay(20, () -> lifts(helper, r, tank, left - 1, then));
	}

	/** The heat-up limit: 6 % decay heat into the RCS at 150 C, kept up by hook, is about 110 C/h in plant time. */
	private static void heatup(GameTestHelper helper, Reactor r, boolean[] rules) {
		ReactorRules.restore(rules);
		ReactorRules.heatupLimit = true;
		r.setTemperatureForTest(150);
		r.setPressureForTest(7.0);
		decay(helper, r, 4, () -> {
			String hot = read(helper, 4, 1, 4);
			check(helper, r.heatupAlarm() && hot.contains("HEAT-UP RATE") && r.fatigue() >= 1, "a heat-up over 55 C/h should alarm HEAT-UP RATE: " + hot, rules);
			log("reactor_limits", "heat-up: " + hot);
			otDeltaT(helper, r, rules);
		});
	}

	private static void decay(GameTestHelper helper, Reactor r, int left, Runnable then) {
		if (left <= 0) {
			then.run();
			return;
		}
		r.setHistoryForTest(1.0);
		helper.runAfterDelay(200, () -> decay(helper, r, left - 1, then));
	}

	/**
	 * OT-DELTA-T: at hot standby with its rod drives powered, the power pinned at 8 % by hook on natural circulation (no coolant pumps),
	 * the core's delta-T is far over the setpoint. Without the rule it runs on; with it, it trips naming OT-DELTA-T.
	 */
	private static void otDeltaT(GameTestHelper helper, Reactor r, boolean[] rules) {
		ReactorRules.restore(rules);
		helper.runAfterDelay(30, () -> GridSceneKit.start(helper, 0, 1, 2, () -> {
			r.setHotStandbyForTest(1500);
			r.setHistoryForTest(0.08);
			r.setSetpoint(-1);
			r.setSetpoint(0.10);
			r.pinPowerForTest(0.08);
			helper.runAfterDelay(40, () -> {
				String off = read(helper, 4, 1, 4);
				check(helper, !r.tripped(), "without the rule it should run on at 8 %: " + off + " | CRDM " + read(helper, 4, 11, 4), rules);
				ReactorRules.deltaTTrips = true;
				helper.runAfterDelay(20, () -> {
					String trip = read(helper, 4, 1, 4);
					r.pinPowerForTest(-1);
					ReactorRules.restore(rules);
					check(helper, r.tripped() && r.trip().startsWith("OT-DELTA-T"), "with the rule it should trip on OT-DELTA-T: " + trip, rules);
					log("reactor_limits", "ok: " + trip);
					helper.succeed();
				});
			});
		}));
	}
}
