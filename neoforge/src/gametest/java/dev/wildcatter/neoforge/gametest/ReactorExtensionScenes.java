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
		out.put("casks", ReactorExtensionScenes::casks);
		out.put("contamination", ReactorExtensionScenes::contamination);
		out.put("fuel_inspection", ReactorExtensionScenes::fuelInspection);
		return out;
	}

	/** The scenes that switch a rule on: each in a batch of its own. */
	static final java.util.Set<String> RULE_SCENES = java.util.Set.of("reactor_limits", "contamination", "fuel_inspection");

	/** A check that hands the rules back before it fails. */
	private static void check(GameTestHelper helper, boolean ok, String message, boolean[] rules) {
		if (!ok && rules != null) {
			dev.wildcatter.Rules.restore(rules);
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
		boolean[] rules = dev.wildcatter.Rules.snapshot();
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
		RuleScenes.rule(helper, "reactorLtop", true);
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
		dev.wildcatter.Rules.restore(rules);
		RuleScenes.rule(helper, "reactorHeatupLimit", true);
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
		dev.wildcatter.Rules.restore(rules);
		helper.runAfterDelay(30, () -> GridSceneKit.start(helper, 0, 1, 2, () -> {
			r.setHotStandbyForTest(1500);
			r.setHistoryForTest(0.08);
			r.setSetpoint(-1);
			r.setSetpoint(0.10);
			r.pinPowerForTest(0.08);
			helper.runAfterDelay(40, () -> {
				String off = read(helper, 4, 1, 4);
				check(helper, !r.tripped(), "without the rule it should run on at 8 %: " + off + " | CRDM " + read(helper, 4, 11, 4), rules);
				RuleScenes.rule(helper, "reactorDeltaTTrips", true);
				helper.runAfterDelay(20, () -> {
					String trip = read(helper, 4, 1, 4);
					r.pinPowerForTest(-1);
					dev.wildcatter.Rules.restore(rules);
					check(helper, r.tripped() && r.trip().startsWith("OT-DELTA-T"), "with the rule it should trip on OT-DELTA-T: " + trip, rules);
					log("reactor_limits", "ok: " + trip);
					helper.succeed();
				});
			});
		}));
	}

	// ---- 19b: casks -----------------------------------------------------------------------------------------------------------------
	/** An irradiated assembly's record making {@code watts} of decay heat (its long-lived group only, as old fuel's is). */
	private static dev.wildcatter.fluid.SpentFuel.Assembly old(double watts) {
		double[] d = new double[Tuning.DECAY_LAMBDA.length];
		d[d.length - 1] = watts / Reactor.assemblyWatts();
		return new dev.wildcatter.fluid.SpentFuel.Assembly(45, d, false);
	}

	/**
	 * Dry casks: the pool's controller with a station against it on 480 V and a storage module on the pad. The pool holds 12 old
	 * assemblies (about 0.9 kW each) and 2 hotter ones (5 kW); on LOAD CASK, with the moves sped up, the station loads the 12 cool ones,
	 * dries, welds and sends the canister to the module; the hot ones stay. The dose beside the module is low; it cannot be broken.
	 */
	static void casks(GameTestHelper helper) {
		floor(helper, -6, -8, 22, 20);
		place(helper, WildcatterBlocks.SPENT_FUEL_POOL, 4, 1, 4, Direction.NORTH);            // x 4-7, z 4-7; the pool north of it
		place(helper, WildcatterBlocks.CASK_LOADING_STATION, 4, 1, 8, Direction.NORTH);       // x 4-6, z 8-11, against the controller
		place(helper, WildcatterBlocks.STORAGE_MODULE, 12, 1, 10, Direction.NORTH);           // x 12-15, z 10-15, y 1-6
		GridSceneKit.genset(helper, 1, 1, 8);                                                 // its terminal at (2, 1, 8)
		put(helper, "cable_industrial", 2, 1, 8);
		put(helper, "cable_industrial", 3, 1, 8);
		dev.wildcatter.block.SpentFuelPoolBlockEntity pool = entity(helper, 4, 1, 4, dev.wildcatter.block.SpentFuelPoolBlockEntity.class);
		dev.wildcatter.block.CaskLoadingStationBlockEntity station = entity(helper, 4, 1, 8, dev.wildcatter.block.CaskLoadingStationBlockEntity.class);
		dev.wildcatter.block.StorageModuleBlockEntity module = entity(helper, 12, 1, 10, dev.wildcatter.block.StorageModuleBlockEntity.class);
		for (int i = 0; i < 12; i++) {
			pool.store(helper.getLevel(), old(850 + 10 * i));
		}
		pool.store(helper.getLevel(), old(5000));
		pool.store(helper.getLevel(), old(5200));
		station.setTimesForTest(0.4, 1, 1, 1);
		station.setItem(0, new net.minecraft.world.item.ItemStack(dev.wildcatter.WildcatterItems.DRY_STORAGE_CANISTER));
		helper.runAfterDelay(20, () -> {
			String linked = station.link(helper.getLevel());
			GridSceneKit.check(helper, linked.contains("linked"), "the station should link the module: " + linked);
			pool.toggleCaskMode();
			GridSceneKit.start(helper, 1, 1, 8, () -> await(helper, 1200, () -> !module.records().isEmpty(), () -> {
				String moved = read(helper, 12, 1, 10) + " | " + read(helper, 4, 1, 8) + " | " + read(helper, 4, 1, 4);
				GridSceneKit.check(helper, module.records().size() == 12 && pool.racks().size() == 2, "the station should load the 12 cool assemblies into the module: " + moved);
				GridSceneKit.check(helper, pool.racks().stream().allMatch(a -> a.watts() > 4000), "the hot ones should stay in the pool: " + moved);
				net.minecraft.world.phys.Vec3 beside = net.minecraft.world.phys.Vec3.atCenterOf(abs(helper, 18, 2, 12));
				double rate = dev.wildcatter.radiation.RadiationTicker.rateAt(helper.getLevel(), beside, null);
				double bare = module.records().stream().mapToDouble(a -> dev.wildcatter.fluid.Radiation.assemblyAt1m(a.watts())).sum() / module.centre().distanceToSqr(beside);
				GridSceneKit.check(helper, rate > 0 && rate < bare / 500, "beside the module the dose should be low: " + dev.wildcatter.fluid.Radiation.rateText(rate)
						+ " (bare " + dev.wildcatter.fluid.Radiation.rateText(bare) + ")");
				net.minecraft.world.entity.player.Player miner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
				BlockPos wall = abs(helper, 13, 3, 10);
				float progress = helper.getLevel().getBlockState(wall).getDestroyProgress(miner, helper.getLevel(), wall);
				GridSceneKit.check(helper, progress == 0, "a loaded module cannot be broken: " + progress);
				log("casks", "ok: " + moved + " | beside " + dev.wildcatter.fluid.Radiation.rateText(rate));
				helper.succeed();
			}));
		});
	}

	// ---- 19b: contamination -----------------------------------------------------------------------------------------------------------
	/**
	 * Contamination, with its rule switched on and handed back: a vessel full of coolant at the limit's activity; a player breaks its
	 * pressurizer and the floor round it is contaminated; a player standing there picks some up; the personnel monitor (on a 120/240 V
	 * service: a diesel set, a pad-mount run backward, a pole unit) washes them; the washer, plugged into a live receptacle on a starter
	 * plant, cleans a block by 90 %.
	 */
	static void contamination(GameTestHelper helper) {
		boolean[] rules = dev.wildcatter.Rules.snapshot();
		floor(helper, -24, -4, 18, 18);
		place(helper, WildcatterBlocks.REACTOR_VESSEL, 6, 1, 4, Direction.NORTH);             // x 6-8, z 4-6
		place(helper, WildcatterBlocks.PRESSURIZER, 6, 1, 7, Direction.NORTH);                // x 6-7, z 7-8
		place(helper, WildcatterBlocks.PERSONNEL_MONITOR, 0, 1, 0, Direction.NORTH);          // its box on the west face, at (-1, 1, 0)
		// the monitor's residential service, as the sales gas scene's meter building
		GridSceneKit.genset(helper, -18, 1, 0);                                               // its 480 V terminal at (-17, 1, 0)
		place(helper, WildcatterBlocks.PADMOUNT_480, -17, 1, 0, Direction.NORTH);             // run backward: 12.47 kV out at x -15
		BlockPos hv = new BlockPos(-15, 1, 0);
		for (int x = hv.getX(); x <= hv.getX() + 2; x++) {
			put(helper, "dist_wire_hot", x, 1, 0);
		}
		int px = hv.getX() + 3;
		put(helper, "pole", px, 1, -1);
		helper.getLevel().setBlock(abs(helper, px, 1, 0), WildcatterBlocks.POLE_TRANSFORMER.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);
		for (int x = px + 1; x <= -1; x++) {
			put(helper, "cable_lv", x, 1, 0);
		}
		// the washer's receptacle on a 120 V starter plant
		helper.setBlock(12, 1, 14, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		helper.setBlock(12, 2, 14, WildcatterBlocks.BOILER);
		helper.setBlock(13, 2, 14, WildcatterBlocks.STEAM_TURBINE);
		helper.getBlockEntity(new BlockPos(12, 2, 14), BoilerBlockEntity.class).addWater(4000);
		put(helper, "wool_wire_hot", 14, 2, 14);
		put(helper, "receptacle_nema_5_15", 15, 2, 14);
		for (int x = 13; x <= 15; x++) {
			put(helper, "wool_wire_neutral", x, 2, 15);
		}
		ReactorVesselBlockEntity vessel = entity(helper, 6, 1, 4, ReactorVesselBlockEntity.class);
		vessel.setHotStandbyForTest(1500);
		vessel.reactor().setActivityForTest(Tuning.ACTIVITY_LIMIT);
		RuleScenes.rule(helper, "reactorContamination", true);
		dev.wildcatter.radiation.ContaminationData data = dev.wildcatter.radiation.ContaminationData.get(helper.getLevel());
		GridSceneKit.start(helper, -18, 1, 0, () -> {
			net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
			player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
			// a player breaks the pressurizer with the RCS full
			player.gameMode.destroyBlock(abs(helper, 6, 1, 7));
			BlockPos floorCell = abs(helper, 9, 0, 9);
			long now = helper.getLevel().getGameTime();
			double on = data.at(floorCell, now);
			check(helper, on > 0.3, "breaking a primary part with water in it should contaminate the floor round it: " + on + " (" + data.size() + " blocks)", rules);
			double rate = dev.wildcatter.radiation.Contamination.rate(helper.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(floorCell.above(2)));
			check(helper, rate > 1e-4, "the contaminated floor should be a dose source: " + dev.wildcatter.fluid.Radiation.rateText(rate), rules);
			// standing on it the player picks some up
			player.teleportTo(floorCell.getX() + 0.5, floorCell.getY() + 1, floorCell.getZ() + 0.5);
			for (int i = 0; i < 5; i++) {
				dev.wildcatter.radiation.Contamination.pickUp(helper.getLevel(), player);
			}
			double skin = dev.wildcatter.radiation.Contamination.skinRate(helper.getLevel(), player);
			check(helper, skin > 0, "standing on it the player should carry contamination", rules);
			// the personnel monitor washes them
			dev.wildcatter.block.PersonnelMonitorBlockEntity monitor = entity(helper, 0, 1, 0, dev.wildcatter.block.PersonnelMonitorBlockEntity.class);
			String washed = monitor.wash(player);
			check(helper, washed.contains("CONTAMINATED") && dev.wildcatter.radiation.Contamination.skinRate(helper.getLevel(), player) == 0,
					"the powered monitor should read and wash the player: " + washed + " | " + read(helper, 0, 1, 0), rules);
			// the washer cleans a block by 90 %
			double before = data.at(floorCell, helper.getLevel().getGameTime());
			BlockPos near = abs(helper, 14, 1, 13);
			data.set(near, 0.8, helper.getLevel().getGameTime());
			String wash = dev.wildcatter.item.DeconWasherItem.wash(helper.getLevel(), near, player);
			double after = data.at(near, helper.getLevel().getGameTime());
			check(helper, Math.abs(after - 0.08) < 0.01, "the washer should clean the block by 90 %: " + wash + " | receptacle " + read(helper, 15, 2, 14), rules);
			dev.wildcatter.Rules.restore(rules);
			log("contamination", "ok: floor " + before + ", dose " + dev.wildcatter.fluid.Radiation.rateText(rate) + ", skin " + dev.wildcatter.fluid.Radiation.rateText(skin)
					+ " | " + washed + " | " + wash);
			helper.succeed();
		});
	}

	// ---- 19b: fresh-fuel inspection ---------------------------------------------------------------------------------------------------
	/**
	 * Fresh-fuel inspection, with the fuel handling damage rule switched on (and a bent grid made certain) and handed back: the stand on
	 * 480 V finds the bent grid; the assembly fixture straightens it; the vessel refuses an inspected bent assembly and takes an
	 * uninspected one, which leaks after a third of its cycle.
	 */
	static void fuelInspection(GameTestHelper helper) {
		boolean[] rules = dev.wildcatter.Rules.snapshot();
		floor(helper, -6, -2, 14, 18);
		GridSceneKit.genset(helper, 0, 1, 0);                                                 // its terminal at (1, 1, 0)
		for (int x = 1; x <= 3; x++) {
			put(helper, "cable_industrial", x, 1, 0);
		}
		for (int z = 1; z <= 6; z++) {
			put(helper, "cable_industrial", 3, 1, z);
		}
		place(helper, WildcatterBlocks.NEW_FUEL_INSPECTION_STAND, 4, 1, 2, Direction.NORTH);  // x 4-5, z 2-3; its box at (3, 1, 2)
		place(helper, WildcatterBlocks.ASSEMBLY_FIXTURE, 4, 1, 6, Direction.NORTH);           // x 4-6, z 6-15; its box at (3, 1, 6)
		place(helper, WildcatterBlocks.REACTOR_VESSEL, 10, 1, 2, Direction.NORTH);
		dev.wildcatter.block.InspectionStandBlockEntity stand = entity(helper, 4, 1, 2, dev.wildcatter.block.InspectionStandBlockEntity.class);
		dev.wildcatter.block.AssemblyFixtureBlockEntity fixture = entity(helper, 4, 1, 6, dev.wildcatter.block.AssemblyFixtureBlockEntity.class);
		ReactorVesselBlockEntity vessel = entity(helper, 10, 1, 2, ReactorVesselBlockEntity.class);
		RuleScenes.rule(helper, "fuelHandlingDamage", true);
		dev.wildcatter.item.FuelAssemblyTags.setBentChanceForTest(1.0);
		Runnable handBack = () -> {
			dev.wildcatter.Rules.restore(rules);
			dev.wildcatter.item.FuelAssemblyTags.setBentChanceForTest(-1);
		};
		stand.setSecondsForTest(1);
		fixture.setRepairSecondsForTest(2);
		stand.setItem(0, new net.minecraft.world.item.ItemStack(WildcatterBlocks.FUEL_ASSEMBLY));
		GridSceneKit.start(helper, 0, 1, 0, () -> await(helper, 400, () -> !stand.getItem(1).isEmpty(), () -> {
			net.minecraft.world.item.ItemStack found = stand.getItem(1).copy();
			if (!(dev.wildcatter.item.FuelAssemblyTags.inspected(found) && dev.wildcatter.item.FuelAssemblyTags.bent(found))) {
				handBack.run();
			}
			check(helper, dev.wildcatter.item.FuelAssemblyTags.inspected(found) && dev.wildcatter.item.FuelAssemblyTags.bent(found),
					"the stand should find the bent grid: " + read(helper, 4, 1, 2), rules);
			// the vessel, cold, refuses it
			String refused = vessel.load(found.copy());
			if (!refused.contains("bent grid")) {
				handBack.run();
			}
			check(helper, refused.contains("bent grid"), "the vessel should refuse an inspected bent assembly: " + refused, rules);
			// the fixture straightens it
			stand.setItem(1, net.minecraft.world.item.ItemStack.EMPTY);
			fixture.setItem(4, found);
			await(helper, 400, () -> !fixture.getItem(3).isEmpty(), () -> {
				net.minecraft.world.item.ItemStack fixed = fixture.getItem(3);
				boolean ok = !fixed.isEmpty() && !dev.wildcatter.item.FuelAssemblyTags.bent(fixed) && !dev.wildcatter.item.FuelAssemblyTags.inspected(fixed);
				if (!ok) {
					handBack.run();
				}
				check(helper, ok, "the fixture should straighten the grid: " + read(helper, 4, 1, 6), rules);
				// an uninspected bent assembly loads, and leaks after a third of its cycle
				net.minecraft.world.item.ItemStack hidden = new net.minecraft.world.item.ItemStack(WildcatterBlocks.FUEL_ASSEMBLY);
				dev.wildcatter.item.FuelAssemblyTags.markBent(hidden);
				String loaded = vessel.load(hidden);
				Reactor r = vessel.reactor();
				r.setBurnupsForTest(dev.wildcatter.fluid.Reactor.burnupPerSecond() * Reactor.cycleHours() * 3600 / 3 + 0.5);
				helper.runAfterDelay(20, () -> {
					handBack.run();
					check(helper, loaded.contains("loaded") && r.bent(0) && r.leaking(0), "the bent assembly should load and leak after a third of its cycle: " + loaded
							+ " | bent " + r.bent(0) + ", leaking " + r.leaking(0), rules);
					log("fuel_inspection", "ok: " + read(helper, 4, 1, 2) + " | " + refused + " | fixture: " + read(helper, 4, 1, 6) + " | " + loaded);
					helper.succeed();
				});
			});
		}));
	}
}
