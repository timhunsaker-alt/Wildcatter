package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.air;
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
import static dev.wildcatter.neoforge.gametest.PlcKit.auto;
import static dev.wildcatter.neoforge.gametest.PlcKit.cabinet;
import static dev.wildcatter.neoforge.gametest.PlcKit.land;
import static dev.wildcatter.neoforge.gametest.PlcKit.line;
import static dev.wildcatter.neoforge.gametest.PlcKit.number;
import static dev.wildcatter.neoforge.gametest.PlcKit.on;
import static dev.wildcatter.neoforge.gametest.PlcKit.run;
import static dev.wildcatter.neoforge.gametest.PlcKit.supply;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.AmmoniaPlant;
import dev.wildcatter.block.AnNeutraliserBlockEntity;
import dev.wildcatter.block.NitricAcidUnitBlockEntity;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem.Type;
import dev.wildcatter.control.ValveBlockEntity;
import dev.wildcatter.fluid.Product;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * The 13d, 13e and 13g signal families that had only compiled, run by a real PLC in the world (docs/questions/plc-gaps.md):
 * <ul>
 * <li>{@code plc_units}: two process units of different kinds under one program. The nitric acid unit (a motor-driven unit) is held until
 * the PLC enables it, then makes its 27 kg/h of HNO3, its ammonia through a motor-operated valve on steel pipe driven open by
 * {@code VALVE_CTRL}'s CMD_OPEN. The AN neutraliser (a unit with a tick of its own) runs only with its ammonia valve, an air on-off valve
 * on steel pipe, proved open; with the air cut the valve springs shut, {@code VALVE_CTRL} latches FAIL_OPEN, the PLC drops the
 * neutraliser's enable and it is held; air back and RESET, and it runs again.</li>
 * <li>{@code plc_crane}: a crawler crane with a site to build, its selector in AUTO: without its PLC's RUN ENABLE it does nothing; given it,
 * it lifts, and CARRYING reads the load on its hook; the enable withdrawn mid-lift, it holds where it is with the load on the hook.</li>
 * <li>{@code plc_motor_faults}: a 480 V drive and a 37 kW pump under {@code MOTOR_CTRL}, its RUNNING proved by the drive's speed: a
 * welded contactor (a test hook) keeps it running after a stop and latches FAIL_STOP; with the drive's supply cut, a start sees the
 * contactor in and no speed, and latches FAIL_START. Each clears on RESET.</li>
 * </ul>
 */
final class PlcPlantScenes {
	private PlcPlantScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("plc_units", PlcPlantScenes::units);
		scenes.put("plc_crane", PlcPlantScenes::crane);
		scenes.put("plc_motor_faults", PlcPlantScenes::motorFaults);
		return scenes;
	}

	private static final String UNITS_PROGRAM = """
			PROGRAM MAIN
			VAR
				nRun AT %QX16.0 : BOOL; nRunning AT %IX0.0 : BOOL; nAuto AT %IX0.1 : BOOL; nRate AT %IW32 : INT;
				aRun AT %QX16.1 : BOOL; aRunning AT %IX0.2 : BOOL; aAuto AT %IX0.3 : BOOL; aRate AT %IW34 : INT;
				sol AT %QX16.2 : BOOL; sOpened AT %IX0.4 : BOOL; sClosed AT %IX0.5 : BOOL; sAuto AT %IX0.6 : BOOL;
				mOpen AT %QX16.3 : BOOL; mClose AT %QX16.4 : BOOL; mOpened AT %IX0.7 : BOOL; mClosed AT %IX1.0 : BOOL; mAuto AT %IX1.1 : BOOL;
				enN, runN, enA, reset : BOOL;
				nh3 : VALVE_CTRL;
				mov : VALVE_CTRL;
			END_VAR
			(* the nitric acid unit: its ammonia valve driven open, then the unit enabled *)
			mov(OPEN := enN, OPENED := mOpened, CLOSED := mClosed, AUTO := mAuto, RESET := reset, TRAVEL_T := T#45s);
			mOpen := mov.CMD_OPEN;
			mClose := mov.CMD_CLOSE;
			nRun := runN AND mov.STATE = 2;
			(* the neutraliser: its air valve held open, the unit enabled only while the valve is proved open *)
			nh3(OPEN := enA, OPENED := sOpened, CLOSED := sClosed, AUTO := sAuto, RESET := reset, TRAVEL_T := T#10s);
			sol := nh3.OUT;
			aRun := enA AND nh3.STATE = 2;
			END_PROGRAM""";

	// ---- plc_units --------------------------------------------------------------------------------------------------------------------
	static void units(GameTestHelper helper) {
		String scene = "plc_units";
		floor(helper, -10, -16, 30, 13);
		// the nitric acid unit, as the nitric_acid scene builds it, its ammonia through a motor-operated valve on steel pipe
		place(helper, WildcatterBlocks.NITRIC_ACID_UNIT, 0, U, 0, NORTH);          // x 0-3, z 0-3
		place(helper, WildcatterBlocks.LPG_BULLET, 5, U, -12, NORTH);              // x 5-7, z -12..-1: its ammonia
		place(helper, WildcatterBlocks.STOCK_TANK, 0, U, 6, NORTH);                // x 0-3, z 6-9: its acid
		put(helper, "gas_line", 4, U, 0);
		put(helper, "motor_operated_valve_steel", 4, U, -1);                       // between the bullet's cell (5, -1) and the unit's east side
		put(helper, "gas_line", 1, U, 4);
		put(helper, "gas_line", 1, U, 5);
		genset(helper, -3, U, -6);                                                 // skid x -8..-3, z -6..-5
		put(helper, "cable_industrial", -2, U, -6);
		line(helper, "cable_industrial", -1, U, -6, -1, U, 0);
		line(helper, "cable_industrial", 0, U, -6, 4, U, -6);                      // 480 V to the valve's actuator
		line(helper, "cable_industrial", 4, U, -5, 4, U, -2);
		// the AN neutraliser, as the an_emulsion scene builds it, its ammonia through an air on-off valve on steel pipe
		place(helper, WildcatterBlocks.AN_NEUTRALISER, 20, U, 0, NORTH);           // x 20-22, z 0-2
		place(helper, WildcatterBlocks.LPG_BULLET, 24, U, -12, NORTH);             // x 24-26, z -12..-1: its ammonia
		place(helper, WildcatterBlocks.STOCK_TANK, 24, U, 3, NORTH);               // x 24-27, z 3-6: its acid
		put(helper, "gas_line", 23, U, 0);
		put(helper, "on_off_valve_steel", 23, U, -1);
		put(helper, "gas_line", 23, U, 2);
		put(helper, "gas_line", 23, U, 3);
		// instrument air: compressor, dryer, tubing onto the valve's top; the compressor on 480 V from the set
		put(helper, "air_tubing", 23, U + 1, -1);
		put(helper, "air_dryer", 22, U + 1, -1);
		put(helper, "air_compressor", 21, U + 1, -1);
		line(helper, "cable_industrial", -1, U, -7, -1, U, -14);
		line(helper, "cable_industrial", 0, U, -14, 21, U, -14);
		line(helper, "cable_industrial", 21, U, -13, 21, U, -1);
		entity(helper, 5, U, -12, StorageTankBlockEntity.class).testFill(Product.AMMONIA, 5_000);
		entity(helper, 24, U, -12, StorageTankBlockEntity.class).testFill(Product.AMMONIA, 5_000);
		entity(helper, 24, U, 3, StorageTankBlockEntity.class).testFill(Product.NITRIC_ACID, 5_000);
		entity(helper, 0, U, 0, WaterUser.class).addWater(100_000);
		NitricAcidUnitBlockEntity unit = entity(helper, 0, U, 0, NitricAcidUnitBlockEntity.class);
		unit.slots().set(0, new ItemStack(WildcatterItems.GAUZE_PACK, 1));
		AnNeutraliserBlockEntity neutraliser = entity(helper, 20, U, 0, AnNeutraliserBlockEntity.class);
		// the PLC: supply, cabinet, and three control cables
		supply(helper, 10, U, 10);                                                 // supply at (13, U, 10)
		put(helper, "junction_box", 14, U, 10);
		put(helper, "junction_box", 14, U, 9);
		put(helper, "junction_box", 14, U + 1, 9);
		PlcCabinetBlockEntity cabinet = cabinet(helper, 15, U, 10, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AI8);
		// to the nitric acid unit's east side, ending in a box; from it, under, to the motor-operated valve
		line(helper, "control_cable", 14, U, 8, 5, U, 8);
		line(helper, "control_cable", 5, U, 7, 5, U, 3);
		put(helper, "junction_box", 4, U, 3);
		line(helper, "control_cable", 4, U - 1, 3, 4, U - 1, -1);
		// over the bench and down to the neutraliser's west side, ending in a box; from it, under, to the on-off valve
		line(helper, "control_cable", 14, U + 2, 9, 17, U + 2, 9);
		put(helper, "control_cable", 17, U + 1, 9);
		line(helper, "control_cable", 17, U, 9, 17, U, 1);
		put(helper, "control_cable", 18, U, 1);
		put(helper, "junction_box", 19, U, 1);
		line(helper, "control_cable", 19, U - 1, 1, 19, U - 1, -2);
		line(helper, "control_cable", 20, U - 1, -2, 23, U - 1, -2);
		put(helper, "control_cable", 23, U - 1, -1);
		helper.runAfterDelay(40, () -> {
			String status = read(helper, 15, U, 10);
			log(scene, "wired | " + status);
			check(helper, status.contains("4 devices reached") && !status.contains("BAD JOINT") && !status.contains("NO 24 V"), "the cabinet should reach both units and both valves: " + status);
			for (int[] d : new int[][] {{0, U, 0}, {20, U, 0}, {23, U, -1}, {4, U, -1}}) {
				auto(helper, d[0], d[1], d[2]);
			}
			land(helper, cabinet, 0, U, 0, 0, "%QX16.0");
			land(helper, cabinet, 0, U, 0, 1, "%IX0.0");
			land(helper, cabinet, 0, U, 0, 2, "%IX0.1");
			land(helper, cabinet, 0, U, 0, 3, "%IW32");
			land(helper, cabinet, 20, U, 0, 0, "%QX16.1");
			land(helper, cabinet, 20, U, 0, 1, "%IX0.2");
			land(helper, cabinet, 20, U, 0, 2, "%IX0.3");
			land(helper, cabinet, 20, U, 0, 3, "%IW34");
			land(helper, cabinet, 23, U, -1, 0, "%QX16.2");
			land(helper, cabinet, 23, U, -1, 1, "%IX0.4");
			land(helper, cabinet, 23, U, -1, 2, "%IX0.5");
			land(helper, cabinet, 23, U, -1, 3, "%IX0.6");
			land(helper, cabinet, 4, U, -1, 0, "%QX16.3");
			land(helper, cabinet, 4, U, -1, 1, "%QX16.4");
			land(helper, cabinet, 4, U, -1, 2, "%IX0.7");
			land(helper, cabinet, 4, U, -1, 3, "%IX1.0");
			land(helper, cabinet, 4, U, -1, 4, "%IX1.1");
			run(helper, cabinet, UNITS_PROGRAM);
		});
		start(helper, -3, U, -6, () -> helper.runAfterDelay(60, () -> {
			// t ~350: the set is up, the PLC has enabled nothing
			String n = read(helper, 0, U, 0);
			String a = read(helper, 20, U, 0);
			log(scene, "held | nitric " + n + " | neutraliser " + a);
			check(helper, n.contains("NO AMMONIA") && unit.making() == 0 && !on(cabinet, "MAIN.NRUNNING") && on(cabinet, "MAIN.NAUTO"),
					"with its motor-operated valve shut on the steel line the nitric acid unit should have no ammonia: " + n);
			check(helper, a.contains("HELD: its PLC is not enabling it") && neutraliser.making() == 0 && !on(cabinet, "MAIN.ARUNNING"), "and the neutraliser: " + a);
			check(helper, on(cabinet, "MAIN.MCLOSED") && on(cabinet, "MAIN.SCLOSED"), "both valves should read shut");
			cabinet.cpu().write("MAIN.ENN", true);
			cabinet.cpu().write("MAIN.ENA", true);
			helper.runAfterDelay(160, () -> {
				// the air valve's 3 s stroke is long done; the motor-operated valve is part way through its 30 s
				ValveBlockEntity mov = entity(helper, 4, U, -1, ValveBlockEntity.class);
				log(scene, "opening | mov " + read(helper, 4, U, -1) + " | on-off " + read(helper, 23, U, -1) + " | neutraliser " + read(helper, 20, U, 0));
				check(helper, on(cabinet, "MAIN.MOPEN") && mov.position() > 0.1 && mov.position() < 0.9, "VALVE_CTRL should drive the motor-operated valve open: " + read(helper, 4, U, -1));
				check(helper, on(cabinet, "MAIN.SOPENED") && number(cabinet, "MAIN.NH3.STATE") == 2, "the on-off valve should be proved open: " + read(helper, 23, U, -1));
				check(helper, near(neutraliser.making(), AmmoniaPlant.anKgh(), 0.05) && on(cabinet, "MAIN.ARUNNING"), "the neutraliser should run at about 34.6 kg/h: " + read(helper, 20, U, 0));
				check(helper, unit.making() == 0, "the nitric acid unit waits for its valve: " + read(helper, 0, U, 0));
				air(helper, 23, U + 1, -1);   // the air cut
			});
			helper.runAfterDelay(720, () -> {
				// 33 s on: the motor-operated valve is open and the nitric unit running; the air valve has failed
				log(scene, "air cut | on-off " + read(helper, 23, U, -1) + " | neutraliser " + read(helper, 20, U, 0) + " | nitric " + read(helper, 0, U, 0));
				check(helper, on(cabinet, "MAIN.NH3.FAIL_OPEN") && on(cabinet, "MAIN.NH3.FAULT") && !on(cabinet, "MAIN.SOL"), "with no air the valve should spring shut and VALVE_CTRL latch FAIL_OPEN");
				check(helper, read(helper, 20, U, 0).contains("HELD") && !on(cabinet, "MAIN.ARUN"), "the PLC should hold the neutraliser without its ammonia: " + read(helper, 20, U, 0));
				check(helper, on(cabinet, "MAIN.MOPENED") && !on(cabinet, "MAIN.MOPEN") && number(cabinet, "MAIN.MOV.STATE") == 2, "the motor-operated valve should be open, its CMD_OPEN dropped at the limit: " + read(helper, 4, U, -1));
				check(helper, read(helper, 0, U, 0).contains("HELD: its PLC is not enabling it") && unit.making() == 0 && !on(cabinet, "MAIN.NRUNNING"),
						"with its ammonia there and no enable, the nitric acid unit should be held by its PLC and say so: " + read(helper, 0, U, 0));
				cabinet.cpu().write("MAIN.RUNN", true);
				put(helper, "air_tubing", 23, U + 1, -1);   // air back
				cabinet.cpu().write("MAIN.RESET", true);
			});
			helper.runAfterDelay(760, () -> cabinet.cpu().write("MAIN.RESET", false));
			helper.runAfterDelay(960, () -> {
				log(scene, "reset | on-off " + read(helper, 23, U, -1) + " | neutraliser " + read(helper, 20, U, 0));
				check(helper, !on(cabinet, "MAIN.NH3.FAULT") && on(cabinet, "MAIN.SOPENED"), "air back and RESET, the valve should open again: " + read(helper, 23, U, -1));
				check(helper, neutraliser.making() > 0 && on(cabinet, "MAIN.ARUNNING") && number(cabinet, "MAIN.ARATE") > 28_000, "and the neutraliser run again: " + read(helper, 20, U, 0));
				check(helper, near(unit.making(), AmmoniaPlant.hno3Kgh(), 0.10) && on(cabinet, "MAIN.NRUNNING") && number(cabinet, "MAIN.NRATE") > 28_000,
						"enabled, the nitric acid unit should make about 27 kg/h of HNO3, its rate read back near 20 mA: " + unit.making() + " kg/h, raw " + number(cabinet, "MAIN.NRATE") + " | " + read(helper, 0, U, 0));
				log(scene, String.format("ok: HNO3 %.1f kg/h (raw %.0f), AN %.1f kg/h (raw %.0f)", unit.making(), number(cabinet, "MAIN.NRATE"), neutraliser.making(), number(cabinet, "MAIN.ARATE")));
				helper.succeed();
			});
		}));
	}

	// ---- plc_crane --------------------------------------------------------------------------------------------------------------------
	static void crane(GameTestHelper helper) {
		String scene = "plc_crane";
		floor(helper, -12, -24, 28, 16);
		helper.getLevel().getServer().setWeatherParameters(36_000, 0, false, false);
		net.minecraft.core.BlockPos main = abs(helper, 0, 1, 0);
		net.minecraft.world.level.block.state.BlockState state = WildcatterBlocks.CRAWLER_CRANE.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, NORTH);
		helper.getLevel().setBlock(main, state, 3);
		dev.wildcatter.multiblock.BigParts.grow(helper.getLevel(), main, state);   // x 0-8, z 0-8
		dev.wildcatter.block.CraneBlockEntity crawler = (dev.wildcatter.block.CraneBlockEntity) helper.getLevel().getBlockEntity(main);
		crawler.testFuel(500);
		auto(helper, 0, 1, 0);   // in AUTO before it first looks for work
		// its site 17 m north, surveyed from stone bricks, and its bricks in a chest east of its base (as crane_siting)
		for (int x = 3; x <= 5; x++) {
			for (int z = -14; z <= -12; z++) {
				helper.getLevel().setBlock(abs(helper, x, 1, z), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
			}
		}
		net.minecraft.core.BlockPos origin = abs(helper, 2, 1, -15);
		String data = dev.wildcatter.item.SurveyToolItem.record(helper.getLevel(), origin, abs(helper, 6, 2, -11)).compressed();
		for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(origin, abs(helper, 6, 2, -11))) {
			helper.getLevel().setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		}
		helper.getLevel().setBlock(origin, WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
		((dev.wildcatter.block.SiteMarkerBlockEntity) helper.getLevel().getBlockEntity(origin)).set(data, 0);
		helper.getLevel().setBlock(abs(helper, 9, 1, 4), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
		((net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs(helper, 9, 1, 4))).setItem(0, new ItemStack(net.minecraft.world.item.Items.STONE_BRICKS, 64));
		// the PLC east of it; a cable at ground level to the crane's east side
		supply(helper, 16, U, 8);                                                  // supply at (19, U, 8)
		put(helper, "junction_box", 20, U, 8);
		PlcCabinetBlockEntity cabinet = cabinet(helper, 21, U, 8, Type.CPU_SMALL, Type.DI16, Type.DO16);
		line(helper, "control_cable", 20, 1, 8, 20, 1, 6);
		line(helper, "control_cable", 19, 1, 6, 9, 1, 6);
		helper.runAfterDelay(20, () -> {
			land(helper, cabinet, 0, 1, 0, 0, "%QX16.0");
			land(helper, cabinet, 0, 1, 0, 1, "%IX0.0");
			land(helper, cabinet, 0, 1, 0, 2, "%IX0.1");
			run(helper, cabinet, "PROGRAM MAIN VAR enable AT %QX16.0 : BOOL; carrying AT %IX0.0 : BOOL; auto AT %IX0.1 : BOOL; go : BOOL; lifts : INT; seen : R_TRIG; END_VAR"
					+ " enable := go; seen(CLK := carrying); IF seen.Q THEN lifts := lifts + 1; END_IF; END_PROGRAM");
		});
		boolean[] carried = new boolean[1];
		helper.runAfterDelay(260, () -> {
			String held = crawler.inspect().getString();
			log(scene, "no enable | " + held + " | " + read(helper, 21, U, 8));
			check(helper, on(cabinet, "MAIN.AUTO") && !held.contains("BUILDING") && !on(cabinet, "MAIN.CARRYING"), "in AUTO with no enable the crane should do nothing: " + held);
			cabinet.cpu().write("MAIN.GO", true);
		});
		// polled from the top: callbacks scheduled inside a callback by the hundred upset the runner's map
		for (int t = 265; t <= 1465; t += 5) {
				helper.runAfterDelay(t, () -> {
					if (!carried[0] && on(cabinet, "MAIN.CARRYING")) {
						carried[0] = true;
						cabinet.cpu().write("MAIN.GO", false);   // withdrawn mid-lift
						String at = crawler.inspect().getString();
						log(scene, "carrying, enable withdrawn | " + at);
						check(helper, at.contains("BUILDING") && crawler.carrying(), "enabled, the crane should lift and CARRYING read its load: " + at);
						helper.runAfterDelay(20, () -> {
							String a = crawler.inspect().getString();
							helper.runAfterDelay(100, () -> {
								String b = crawler.inspect().getString();
								log(scene, "held | " + b);
								check(helper, a.equals(b) && crawler.carrying() && on(cabinet, "MAIN.CARRYING"), "without its enable the crane should hold, the load on its hook: " + a + " | " + b);
								cabinet.cpu().write("MAIN.GO", true);
								helper.runAfterDelay(300, () -> {
									String c = crawler.inspect().getString();
									log(scene, "ok: " + c + " | lifts seen " + number(cabinet, "MAIN.LIFTS"));
									check(helper, !c.equals(b), "enabled again it should go on: " + c);
									helper.succeed();
								});
							});
						});
					}
				});
		}
	}

	// ---- plc_motor_faults -------------------------------------------------------------------------------------------------------------
	private static final String MOTOR_PROGRAM = """
			PROGRAM MAIN
			VAR
				run AT %QX16.0 : BOOL; running AT %IX0.0 : BOOL; tripped AT %IX0.1 : BOOL; auto AT %IX0.2 : BOOL;
				speed AT %IW32 : INT; ref AT %QW48 : INT;
				go, rst, proof : BOOL;
				m : MOTOR_CTRL;
			END_VAR
			(* running is proved by the drive's speed, not only its contactor: over 5 % and not an open loop *)
			proof := running AND speed > 1600 AND speed <> 32767;
			m(START := go, RUNNING := proof, TRIPPED := tripped, AUTO := auto, RESET := rst, START_T := T#5s, STOP_T := T#5s);
			run := m.RUN;
			ref := 32000;
			END_PROGRAM""";

	static void motorFaults(GameTestHelper helper) {
		try {
			motorFaultsBody(helper);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static void motorFaultsBody(GameTestHelper helper) {
		String scene = "plc_motor_faults";
		floor(helper, 2, -4, 20, 13);
		genset(helper, 12, U, 0);                                                  // skid x 7-12, z 0-1; its 480 V at (13, U, 0)
		put(helper, "cable_industrial", 13, U, 0);
		put(helper, "cable_industrial", 14, U, 0);
		put(helper, "cable_industrial", 14, U + 1, 0);
		put(helper, "cable_industrial", 14, U + 2, 0);
		put(helper, "cable_industrial", 15, U + 2, 0);                             // onto the drive's top
		place(helper, WildcatterBlocks.DRIVE_CABINET, 15, U, 0, NORTH);
		put(helper, "cable_industrial", 15, U, 1);
		put(helper, "cable_industrial", 15, U, 2);
		for (int x = 13; x <= 17; x++) {
			for (int z = 2; z <= 4; z++) {
				helper.getLevel().setBlock(abs(helper, x, 1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
			}
		}
		helper.getLevel().setBlock(abs(helper, 15, 1, 3), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
		net.minecraft.world.level.block.state.BlockState pump = WildcatterBlocks.PROCESS_WATER_PUMP.defaultBlockState();
		if (pump.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
			pump = pump.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, NORTH);
		}
		helper.getLevel().setBlock(abs(helper, 15, U, 3), pump, 3);                // a 37 kW pump over its sump
		put(helper, "pipe", 16, U, 3);
		put(helper, "pipe", 17, U, 3);
		put(helper, "boiler", 18, U, 3);                                          // an empty boiler for the pump to fill: its load
		dev.wildcatter.block.Starter drive = entity(helper, 15, U, 0, dev.wildcatter.block.StarterCabinetBlockEntity.class).starterForTest();
		while (drive.overloadAmps() < 52) {
			drive.nextOverloadSetting();                                           // the dial for a 37 kW motor's full-load amps
		}
		supply(helper, 8, U, 10);                                                  // supply at (11, U, 10)
		put(helper, "junction_box", 12, U, 10);
		put(helper, "junction_box", 12, U, 9);
		PlcCabinetBlockEntity cabinet = cabinet(helper, 13, U, 10, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AI8, Type.AO4);
		line(helper, "control_cable", 12, U, 8, 12, U, 5);
		line(helper, "control_cable", 13, U, 5, 16, U, 5);
		line(helper, "control_cable", 16, U + 1, 5, 16, U + 1, 0);
		helper.runAfterDelay(40, () -> {
			log(scene, "wired | " + read(helper, 13, U, 10));
			auto(helper, 15, U, 0);
			land(helper, cabinet, 15, U, 0, 0, "%QX16.0");
			land(helper, cabinet, 15, U, 0, 1, "%IX0.0");
			land(helper, cabinet, 15, U, 0, 2, "%IX0.1");
			land(helper, cabinet, 15, U, 0, 3, "%IX0.2");
			land(helper, cabinet, 15, U, 0, 4, "%QW48");
			land(helper, cabinet, 15, U, 0, 5, "%IW32");
			run(helper, cabinet, MOTOR_PROGRAM);
		});
		start(helper, 12, U, 0, () -> {
			cabinet.cpu().write("MAIN.GO", true);
			helper.runAfterDelay(300, () -> {
				String up = read(helper, 15, U, 0);
				log(scene, "running | " + up + " | speed raw " + number(cabinet, "MAIN.SPEED") + " | pump " + read(helper, 15, U, 3) + " | " + helper.getLevel().getBlockState(abs(helper, 15, U, 3)));
				check(helper, drive.lineWatts() > 1_000, "the drive should be driving its pump: " + up + " | pump " + read(helper, 15, U, 3));
				check(helper, number(cabinet, "MAIN.M.STATE") == 2 && on(cabinet, "MAIN.PROOF") && !on(cabinet, "MAIN.M.FAULT") && number(cabinet, "MAIN.SPEED") > 30_000,
						"the drive should be up to speed under MOTOR_CTRL: " + up + " | state " + number(cabinet, "MAIN.M.STATE"));
				drive.setWeldedForTest(true);
				cabinet.cpu().write("MAIN.GO", false);
			});
			helper.runAfterDelay(460, () -> {
				String welded = read(helper, 15, U, 0);
				log(scene, "welded | " + welded + " | state " + number(cabinet, "MAIN.M.STATE"));
				check(helper, on(cabinet, "MAIN.M.FAIL_STOP") && on(cabinet, "MAIN.M.FAULT") && !on(cabinet, "MAIN.RUN") && on(cabinet, "MAIN.RUNNING"),
						"a stop the welded contactor ignores should latch FAIL_STOP after 5 s: " + welded);
				drive.setWeldedForTest(false);
				cabinet.cpu().write("MAIN.RST", true);
				GridSceneKit.air(helper, 14, U, 0);   // the drive's supply cut
			});
			helper.runAfterDelay(480, () -> {
				cabinet.cpu().write("MAIN.RST", false);
				check(helper, !on(cabinet, "MAIN.M.FAULT") && !on(cabinet, "MAIN.RUNNING"), "RESET should clear it, and the contactor drop: " + read(helper, 15, U, 0));
				cabinet.cpu().write("MAIN.GO", true);
			});
			helper.runAfterDelay(640, () -> {
				String dead = read(helper, 15, U, 0);
				log(scene, "no supply | " + dead + " | speed raw " + number(cabinet, "MAIN.SPEED"));
				check(helper, on(cabinet, "MAIN.M.FAIL_START") && on(cabinet, "MAIN.M.FAULT") && !on(cabinet, "MAIN.PROOF"),
						"with its supply cut, a start should see no speed and latch FAIL_START after 5 s: " + dead);
				put(helper, "cable_industrial", 14, U, 0);
				cabinet.cpu().write("MAIN.RST", true);
			});
			helper.runAfterDelay(660, () -> cabinet.cpu().write("MAIN.RST", false));
			helper.runAfterDelay(900, () -> {
				String back = read(helper, 15, U, 0);
				log(scene, "ok: " + back + " | state " + number(cabinet, "MAIN.M.STATE"));
				check(helper, number(cabinet, "MAIN.M.STATE") == 2 && !on(cabinet, "MAIN.M.FAULT"), "supply back and reset, it should run: " + back);
				helper.succeed();
			});
		});
	}
}
