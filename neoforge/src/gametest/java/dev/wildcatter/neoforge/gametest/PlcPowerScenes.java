package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.place;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.PlcKit.auto;
import static dev.wildcatter.neoforge.gametest.PlcKit.cabinet;
import static dev.wildcatter.neoforge.gametest.PlcKit.cable;
import static dev.wildcatter.neoforge.gametest.PlcKit.land;
import static dev.wildcatter.neoforge.gametest.PlcKit.number;
import static dev.wildcatter.neoforge.gametest.PlcKit.on;
import static dev.wildcatter.neoforge.gametest.PlcKit.run;
import static dev.wildcatter.neoforge.gametest.PlcKit.supply;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.LoadBankBlockEntity;
import dev.wildcatter.block.RectifierBlockEntity;
import dev.wildcatter.block.SwitchgearSectionBlockEntity;
import dev.wildcatter.block.TransferSwitchBlockEntity;
import dev.wildcatter.block.TurbineGeneratorBlockEntity;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The power equipment's PLC signals, run in the world (13f's compiled-only families, docs/questions/plc-gaps.md). Each scene is a real PLC
 * (supply, cabinet, cable, landings, a program) on the real machine:
 * <ul>
 * <li>{@code plc_switchgear}: a 4.16 kV feeder on REMOTE: the PLC opens it on its trip coil, closes it, reads its amps, racks it out to TEST
 * and back, and reads 51 as the element that tripped it; its close coil cannot close over the trip.</li>
 * <li>{@code plc_ats}: the transfer switch's TEST starts the standby set; INHIBIT TRANSFER holds the load on normal with emergency OK; released,
 * it transfers; TEST off, it goes back.</li>
 * <li>{@code plc_gas_turbine}: RUN starts a gas turbine's sequence, RUNNING reads it up, RUN off stops it.</li>
 * <li>{@code plc_loads}: a lamp switched by the PLC (a heater is the same relay-fed load); a 13.8 kV load bank stepped by a 4-20 mA signal and read back in watts.</li>
 * <li>{@code plc_steam}: a firebox's firing rate by time share, BURNING read back; the steam plant's turbine generator run and stopped by RUN.</li>
 * <li>{@code plc_rectifier}: a rectifier on a magnesium cell, run and given its current setpoint by 4-20 mA, its amps and volts read back.</li>
 * </ul>
 */
final class PlcPowerScenes {
	private PlcPowerScenes() {
	}

	private static final int U = 2;
	private static final Direction NORTH = Direction.NORTH;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("plc_switchgear", PlcPowerScenes::switchgear);
		scenes.put("plc_ats", PlcPowerScenes::ats);
		scenes.put("plc_gas_turbine", PlcPowerScenes::gasTurbine);
		scenes.put("plc_loads", PlcPowerScenes::loads);
		scenes.put("plc_steam", PlcPowerScenes::steam);
		scenes.put("plc_rectifier", PlcPowerScenes::rectifier);
		return scenes;
	}

	/** Polls {@code done} every 10 ticks up to {@code max}, then runs {@code then}, which checks. */
	static void await(GameTestHelper helper, int max, java.util.function.BooleanSupplier done, Runnable then) {
		await(helper, max, 0, done, then);
	}

	private static void await(GameTestHelper helper, int max, int waited, java.util.function.BooleanSupplier done, Runnable then) {
		if (done.getAsBoolean() || waited >= max) {
			then.run();
			return;
		}
		helper.runAfterDelay(10, () -> await(helper, max, waited + 10, done, then));
	}

	// ---- plc_switchgear -----------------------------------------------------------------------------------------------------------
	static void switchgear(GameTestHelper helper) {
		String scene = "plc_switchgear";
		RelayScenes.lineup(helper);
		RelayScenes.closeAll(helper, 600);
		supply(helper, 14, U, -7);
		put(helper, "junction_box", 18, U, -7);
		PlcCabinetBlockEntity plc = cabinet(helper, 19, U, -7, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AI8);
		cable(helper, 19, U, -7, 23, U, -3);
		SwitchgearSectionBlockEntity feeder = RelayScenes.section(helper, 23);
		GridSceneKit.start(helper, 10, U, 0, () -> helper.runAfterDelay(60, () -> {
			auto(helper, 23, U, -3);
			String[] in = {"closed", "tripped", "remote", "", "", "connected", "racking", "t50", "t51", "t87", "tbf"};
			int[] inPoint = {2, 3, 4, -1, -1, 8, 9, 10, 11, 12, 13};
			StringBuilder vars = new StringBuilder("PROGRAM MAIN\nVAR\n");
			for (int i = 0; i < in.length; i++) {
				if (inPoint[i] >= 0) {
					String address = "%IX" + i / 8 + "." + i % 8;
					land(helper, plc, 23, U, -3, inPoint[i], address);
					vars.append(in[i]).append(" AT ").append(address).append(" : BOOL;\n");
				}
			}
			String[] out = {"closeCoil", "tripCoil", "rackIn", "rackOut"};
			int[] outPoint = {0, 1, 6, 7};
			for (int i = 0; i < out.length; i++) {
				land(helper, plc, 23, U, -3, outPoint[i], "%QX16." + i);
				vars.append(out[i]).append(" AT %QX16.").append(i).append(" : BOOL;\n");
			}
			land(helper, plc, 23, U, -3, 5, "%IW32");
			vars.append("amps AT %IW32 : WORD;\nwantClose, wantTrip, wantIn, wantOut : BOOL;\nEND_VAR\n");
			run(helper, plc, vars + "closeCoil := wantClose; tripCoil := wantTrip; rackIn := wantIn; rackOut := wantOut;\nEND_PROGRAM");
		}));
		// GridSceneKit.start checks the set at 290 ticks; the PLC runs from 350
		helper.runAfterDelay(380, () -> {
			log(scene, "remote | " + read(helper, 23, U, -3) + " | " + read(helper, 19, U, -7));
			check(helper, on(plc, "MAIN.CLOSED") && on(plc, "MAIN.REMOTE") && on(plc, "MAIN.CONNECTED"), "the PLC should read the feeder closed, on REMOTE, connected: " + read(helper, 19, U, -7));
			check(helper, number(plc, "MAIN.AMPS") > 50 && number(plc, "MAIN.AMPS") < 32000, "and read its load's amps on its 4-20 mA: " + plc.cpu().read("MAIN.AMPS") + " | " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTTRIP", true);
		});
		helper.runAfterDelay(400, () -> {
			check(helper, !feeder.closed() && !on(plc, "MAIN.CLOSED") && !on(plc, "MAIN.TRIPPED"), "the trip coil should open it, not as a relay trip: " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTTRIP", false);
			plc.cpu().write("MAIN.WANTOUT", true);
		});
		helper.runAfterDelay(420, () -> {
			check(helper, on(plc, "MAIN.RACKING") && !on(plc, "MAIN.CONNECTED"), "RACK OUT should start the racking motor: " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTOUT", false);
		});
		helper.runAfterDelay(730, () -> {
			check(helper, feeder.racked() == SwitchgearSectionBlockEntity.Rack.TEST && !on(plc, "MAIN.RACKING"), "after 15 s it should stand in TEST: " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTIN", true);
		});
		helper.runAfterDelay(1050, () -> {
			plc.cpu().write("MAIN.WANTIN", false);
			check(helper, on(plc, "MAIN.CONNECTED"), "RACK IN should bring it back to CONNECTED: " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTCLOSE", true);
		});
		helper.runAfterDelay(1070, () -> {
			check(helper, on(plc, "MAIN.CLOSED"), "the close coil should close it: " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTCLOSE", false);
			feeder.setPickup(5);   // under its load: 51 trips it
		});
		helper.runAfterDelay(1500, () -> {
			log(scene, "51 | " + read(helper, 23, U, -3));
			check(helper, on(plc, "MAIN.TRIPPED") && on(plc, "MAIN.T51") && !on(plc, "MAIN.T50") && !on(plc, "MAIN.T87") && !on(plc, "MAIN.TBF"), "the PLC should read a trip by 51 and only 51: " + read(helper, 23, U, -3));
			plc.cpu().write("MAIN.WANTCLOSE", true);
		});
		helper.runAfterDelay(1540, () -> {
			check(helper, !feeder.closed() && on(plc, "MAIN.TRIPPED"), "a close coil cannot close over a relay trip: " + read(helper, 23, U, -3));
			log(scene, "ok");
			helper.succeed();
		});
	}

	// ---- plc_ats ------------------------------------------------------------------------------------------------------------------
	static void ats(GameTestHelper helper) {
		String scene = "plc_ats";
		floor(helper, -2, -6, 34, 22);
		int zn = 0;
		int ze = 8;
		GridSceneKit.genset(helper, 10, U, zn);
		GridSceneKit.genset(helper, 10, U, ze);
		for (int x = 11; x <= 13; x++) {
			put(helper, "cable_industrial", x, U, zn);
		}
		for (int y = U + 1; y <= U + 3; y++) {
			put(helper, "cable_industrial", 13, y, zn);
		}
		for (int z = zn + 1; z <= ze + 1; z++) {
			put(helper, "cable_industrial", 13, U + 3, z);
		}
		put(helper, "cable_industrial", 12, U + 3, ze + 1);
		put(helper, "cable_industrial", 11, U, ze);
		put(helper, "cable_industrial", 11, U + 1, ze);
		put(helper, "cable_industrial", 11, U + 2, ze);
		put(helper, "cable_industrial", 11, U + 2, ze + 1);
		put(helper, "cable_industrial", 12, U, ze + 1);
		put(helper, "cable_industrial", 13, U, ze + 1);
		put(helper, "three_phase_heater", 14, U, ze + 1);
		put(helper, "three_phase_heater", 13, U, ze + 2);
		if (helper.getLevel().getBlockEntity(GensetSkid.base(abs(helper, 10, U, ze), NORTH, false)) instanceof GensetPartBlockEntity tank) {
			tank.setFuel(1000);
		}
		supply(helper, 18, U, 14);
		put(helper, "junction_box", 22, U, 14);
		PlcCabinetBlockEntity plc = cabinet(helper, 23, U, 14, Type.CPU_SMALL, Type.DI16, Type.DO16);
		GridSceneKit.start(helper, 10, U, zn, () -> {
			place(helper, WildcatterBlocks.ATS_480, 12, U + 1, ze + 1, Direction.EAST);
			helper.getLevel().setBlock(abs(helper, 11, U, ze + 1), Blocks.STONE.defaultBlockState(), 3);
			helper.getLevel().setBlock(abs(helper, 11, U + 1, ze + 1), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
			helper.runAfterDelay(5, () -> {
				((TransferSwitchBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 12, U + 1, ze + 1))).setTimers(TransferSwitchBlockEntity.Timers.SHORT);
				cable(helper, 23, U, 14, 12, U + 1, ze + 1);
			});
			helper.runAfterDelay(30, () -> {
				auto(helper, 12, U + 1, ze + 1);
				String[] in = {"onNormal", "onEmergency", "normalOk", "emergencyOk", "engineStart", "isAuto"};
				StringBuilder vars = new StringBuilder("PROGRAM MAIN\nVAR\n");
				for (int i = 0; i < in.length; i++) {
					land(helper, plc, 12, U + 1, ze + 1, i + 2, "%IX0." + i);
					vars.append(in[i]).append(" AT %IX0.").append(i).append(" : BOOL;\n");
				}
				land(helper, plc, 12, U + 1, ze + 1, 0, "%QX16.0");
				land(helper, plc, 12, U + 1, ze + 1, 1, "%QX16.1");
				vars.append("test AT %QX16.0 : BOOL;\ninhibit AT %QX16.1 : BOOL;\nwantTest, wantInhibit : BOOL;\nEND_VAR\n");
				run(helper, plc, vars + "test := wantTest; inhibit := wantInhibit;\nEND_PROGRAM");
			});
			helper.runAfterDelay(70, () -> {
				log(scene, "normal | " + read(helper, 12, U + 1, ze + 1) + " | " + read(helper, 23, U, 14));
				check(helper, on(plc, "MAIN.ONNORMAL") && on(plc, "MAIN.NORMALOK") && on(plc, "MAIN.ISAUTO") && !on(plc, "MAIN.ENGINESTART"), "the PLC should read the switch on normal: " + read(helper, 12, U + 1, ze + 1));
				plc.cpu().write("MAIN.WANTINHIBIT", true);
				plc.cpu().write("MAIN.WANTTEST", true);
			});
			await(helper, 70 + 1600, () -> on(plc, "MAIN.EMERGENCYOK"), () -> helper.runAfterDelay(200, () -> {
				log(scene, "test, inhibited | " + read(helper, 12, U + 1, ze + 1) + " | standby " + read(helper, 10, U, ze));
				check(helper, on(plc, "MAIN.ENGINESTART") && on(plc, "MAIN.EMERGENCYOK"), "TEST should start the standby set: " + read(helper, 12, U + 1, ze + 1));
				check(helper, on(plc, "MAIN.ONNORMAL") && !on(plc, "MAIN.ONEMERGENCY"), "with INHIBIT TRANSFER the load should stay on normal: " + read(helper, 12, U + 1, ze + 1));
				plc.cpu().write("MAIN.WANTINHIBIT", false);
				await(helper, 1200, () -> on(plc, "MAIN.ONEMERGENCY"), () -> {
					log(scene, "transferred | " + read(helper, 12, U + 1, ze + 1));
					check(helper, on(plc, "MAIN.ONEMERGENCY"), "released, it should transfer to emergency: " + read(helper, 12, U + 1, ze + 1));
					plc.cpu().write("MAIN.WANTTEST", false);
					await(helper, 2400, () -> on(plc, "MAIN.ONNORMAL"), () -> {
						log(scene, "back | " + read(helper, 12, U + 1, ze + 1));
						check(helper, on(plc, "MAIN.ONNORMAL"), "TEST off, it should go back to normal: " + read(helper, 12, U + 1, ze + 1));
						log(scene, "ok");
						helper.succeed();
					});
				});
			}));
		});
	}

	// ---- plc_gas_turbine ----------------------------------------------------------------------------------------------------------
	static void gasTurbine(GameTestHelper helper) {
		String scene = "plc_gas_turbine";
		floor(helper, -2, -10, 40, 24);
		place(helper, WildcatterBlocks.GAS_TURBINE, 20, U, 0, NORTH);          // x 20-23, z 0-9
		place(helper, WildcatterBlocks.GT_INLET_HOUSE, 20, U, -4, NORTH);
		place(helper, WildcatterBlocks.GT_GENERATOR, 24, U, 1, NORTH);
		place(helper, WildcatterBlocks.GEN_BREAKER, 25, U, 7, NORTH);
		place(helper, WildcatterBlocks.HRSG, 20, U, 10, NORTH);
		place(helper, WildcatterBlocks.STOCK_TANK, 14, U, 4, NORTH);           // kerosene
		put(helper, "gas_line", 18, U, 6);
		put(helper, "gas_line", 19, U, 6);
		GridSceneKit.genset(helper, 10, U, 2);
		place(helper, WildcatterBlocks.UNIT_SUBSTATION, 14, U, 2, Direction.SOUTH);   // 4.16 kV for its starter
		for (int x = 15; x <= 19; x++) {
			put(helper, "cable_mv", x, U, 2);
		}
		GridSceneKit.entity(helper, 14, U, 4, dev.wildcatter.block.StorageTankBlockEntity.class).testFill(dev.wildcatter.fluid.Product.KEROSENE, 40_000);
		supply(helper, 28, U, -8);
		put(helper, "junction_box", 32, U, -8);
		PlcCabinetBlockEntity plc = cabinet(helper, 33, U, -8, Type.CPU_SMALL, Type.DI16, Type.DO16);
		GasTurbineBlockEntity gt = GridSceneKit.entity(helper, 20, U, 0, GasTurbineBlockEntity.class);
		GridSceneKit.start(helper, 10, U, 2, () -> {
			cable(helper, 33, U, -8, 23, U, 0);
			auto(helper, 20, U, 0);
			land(helper, plc, 20, U, 0, 0, "%QX16.0");
			land(helper, plc, 20, U, 0, 1, "%IX0.0");
			land(helper, plc, 20, U, 0, 2, "%IX0.1");
			land(helper, plc, 20, U, 0, 3, "%IX0.2");
			run(helper, plc, "PROGRAM MAIN VAR runCmd AT %QX16.0 : BOOL; running AT %IX0.0 : BOOL; tripped AT %IX0.1 : BOOL; isAuto AT %IX0.2 : BOOL; want : BOOL; END_VAR runCmd := want; END_PROGRAM");
			helper.runAfterDelay(30, () -> {
				check(helper, on(plc, "MAIN.ISAUTO") && !on(plc, "MAIN.RUNNING"), "the PLC should read it stopped and in AUTO: " + read(helper, 20, U, 0));
				plc.cpu().write("MAIN.WANT", true);
			});
			helper.runAfterDelay(200, () -> {
				String starting = read(helper, 20, U, 0);
				log(scene, "RUN | " + starting);
				check(helper, gt.wantRun(), "RUN should start its sequence: " + starting);
				gt.skipStartForTest();   // the purge and run-up take minutes
			});
			helper.runAfterDelay(260, () -> {
				log(scene, "up | " + read(helper, 20, U, 0));
				check(helper, on(plc, "MAIN.RUNNING") && !on(plc, "MAIN.TRIPPED"), "the PLC should read it running: " + read(helper, 20, U, 0));
				plc.cpu().write("MAIN.WANT", false);
			});
			await(helper, 260 + 1600, () -> !on(plc, "MAIN.RUNNING"), () -> {
				log(scene, "RUN off | " + read(helper, 20, U, 0));
				check(helper, !gt.wantRun() && !on(plc, "MAIN.RUNNING"), "RUN off should stop it: " + read(helper, 20, U, 0));
				log(scene, "ok");
				helper.succeed();
			});
		});
	}

	// ---- plc_loads ----------------------------------------------------------------------------------------------------------------
	static void loads(GameTestHelper helper) {
		String scene = "plc_loads";
		floor(helper, -8, -4, 26, 9);
		GasPowerScenes.genset(helper, 8, 0, true);                                 // a 1 MW gas set, panel at x 8, z 0-1; 480 V at x 9
		BlockPos mv = GasPowerScenes.inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 8, 0, Direction.EAST, false);   // x 9-12; 4.16 kV at x 13
		for (int z = 0; z <= 5; z++) {
			put(helper, "cable_mv", mv.getX(), U, z);
		}
		put(helper, "cable_mv", 14, U, 5);
		put(helper, "cable_mv", 15, U, 5);
		put(helper, "cable_mv", 16, U, 5);
		place(helper, WildcatterBlocks.UNIT_AUX_TRANSFORMER, 16, U, 4, NORTH);      // run backward up to 13.8 kV
		place(helper, WildcatterBlocks.LOAD_BANK, 20, U, 2, NORTH);                 // x 20-22, z 2-7
		put(helper, "bus_duct_gen", 16, U + 4, 1);
		for (int x = 17; x <= 19; x++) {
			put(helper, "bus_duct_gen", x, U + 4, 1);
		}
		for (int y = 3; y >= 0; y--) {
			put(helper, "bus_duct_gen", 19, U + y, 1);
		}
		for (int x = 20; x <= 22; x++) {
			put(helper, "bus_duct_gen", x, U, 1);
		}
		// the PLC's own 120 V plant also lights a lamp: the lamp test's layout, west
		supply(helper, -6, U, 6);
		put(helper, "junction_box", -2, U, 6);
		PlcCabinetBlockEntity plc = cabinet(helper, -1, U, 6, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AI8, Type.AO4);
		put(helper, "plastic_wire_hot", -4, U, 5);
		put(helper, "plastic_wire_hot", -3, U, 5);
		put(helper, "electric_lamp", -2, U, 5);
		put(helper, "plastic_wire_neutral", -2, U, 4);
		put(helper, "plastic_wire_neutral", -3, U, 4);
		put(helper, "plastic_wire_neutral", -4, U, 4);
		put(helper, "plastic_wire_neutral", -5, U, 4);
		put(helper, "plastic_wire_neutral", -5, U, 5);
		helper.runAfterDelay(20, () -> {
			GasPowerScenes.panel(helper, 8, 0).setGasForTest(200);   // its gas, as if piped
			GasPowerScenes.panel(helper, 8, 0).setWantRun(true);
			cable(helper, -1, U, 6, 20, U, 2);
			auto(helper, -2, U, 5);
			auto(helper, 20, U, 2);
			land(helper, plc, -2, U, 5, 0, "%QX16.0");
			land(helper, plc, -2, U, 5, 1, "%IX0.0");
			land(helper, plc, 20, U, 2, 0, "%QW48");
			land(helper, plc, 20, U, 2, 1, "%IX0.2");
			land(helper, plc, 20, U, 2, 2, "%IW32");
			run(helper, plc, """
					PROGRAM MAIN
					VAR
						lamp AT %QX16.0 : BOOL; lampAuto AT %IX0.0 : BOOL;
						steps AT %QW48 : WORD; bankAuto AT %IX0.2 : BOOL; watts AT %IW32 : WORD;
						wantLamp : BOOL; bankSteps : INT; kw : REAL;
					END_VAR
					lamp := wantLamp;
					steps := INT_TO_WORD(bankSteps * 3200);
					kw := INT_TO_REAL(WORD_TO_INT(watts)) / 32000.0 * 10000.0;
					END_PROGRAM""");
		});
		helper.runAfterDelay(60, () -> {
			check(helper, on(plc, "MAIN.LAMPAUTO") && on(plc, "MAIN.BANKAUTO"), "the PLC should read both in AUTO: " + read(helper, -1, U, 6));
			check(helper, !helper.getLevel().getBlockState(abs(helper, -2, U, 5)).getValue(dev.wildcatter.block.LoadBlock.LIT), "in AUTO with the PLC off, the lamp is dark: " + read(helper, -2, U, 5));
			plc.cpu().write("MAIN.WANTLAMP", true);
		});
		helper.runAfterDelay(100, () -> {
			log(scene, "lamp | " + read(helper, -2, U, 5));
			check(helper, helper.getLevel().getBlockState(abs(helper, -2, U, 5)).getValue(dev.wildcatter.block.LoadBlock.LIT), "the PLC should light the lamp: " + read(helper, -2, U, 5));
		});
		helper.runAfterDelay(340, () -> {
			String set = read(helper, 8, U, 0);
			check(helper, set.contains("RUNNING"), "the set should be running: " + set);
			plc.cpu().write("MAIN.BANKSTEPS", (long) 0);
		});
		helper.runAfterDelay(440, () -> {
			check(helper, ((LoadBankBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 20, U, 2))).steps() == 0, "0 % should be no steps: " + read(helper, 20, U, 2));
			plc.cpu().write("MAIN.BANKSTEPS", (long) 1);
		});
		helper.runAfterDelay(560, () -> {
			LoadBankBlockEntity bank = (LoadBankBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 20, U, 2));
			double kw = number(plc, "MAIN.KW");
			log(scene, String.format("bank | %s | PLC reads %.0f kW | set %s", read(helper, 20, U, 2), kw, read(helper, 8, U, 0)));
			check(helper, bank.steps() == 1, "one step's 4-20 mA should set one 1 MW step: " + read(helper, 20, U, 2));
			check(helper, kw > 50 && Math.abs(kw - bank.watts() / 1000) < Math.max(20, bank.watts() / 1000 * 0.05), "the PLC should read the bank's watts on its 4-20 mA: " + kw + " kW against " + bank.watts() / 1000);
			log(scene, "ok");
			helper.succeed();
		});
	}

	// ---- plc_steam ----------------------------------------------------------------------------------------------------------------
	static void steam(GameTestHelper helper) {
		String scene = "plc_steam";
		floor(helper, -8, -8, 26, 12);
		BlockPos gen = abs(helper, 10, U, 0);
		dev.wildcatter.block.SteamPlantLayout.layout().build(helper.getLevel(), gen, NORTH, 0);
		// a starter boiler on a firebox of coal, west
		put(helper, "firebox", -4, U, 8);
		put(helper, "boiler", -4, U + 1, 8);
		if (helper.getLevel().getBlockEntity(abs(helper, -4, U + 1, 8)) instanceof dev.wildcatter.block.BoilerBlockEntity boiler) {
			boiler.addWater(4000);
		}
		((dev.wildcatter.block.FireboxBlockEntity) helper.getLevel().getBlockEntity(abs(helper, -4, U, 8))).setItem(0, new ItemStack(Items.COAL, 64));
		supply(helper, -6, U, 4);
		put(helper, "junction_box", -2, U, 4);
		PlcCabinetBlockEntity plc = cabinet(helper, -1, U, 4, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AO4);
		TurbineGeneratorBlockEntity plant = (TurbineGeneratorBlockEntity) helper.getLevel().getBlockEntity(gen);
		helper.runAfterDelay(20, () -> {
			((dev.wildcatter.block.SteamPlantPartBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 9, U, -3))).insertFuel(new ItemStack(Items.COAL, 16));
			plant.setWaterForTest(Tuning.DRUM_WATER_KG * 0.8, Tuning.HOTWELL_KG);
			cable(helper, -1, U, 4, -4, U, 8);
			cable(helper, -1, U, 4, 10, U, 0);
			auto(helper, -4, U, 8);
			auto(helper, 10, U, 0);
			land(helper, plc, -4, U, 8, 0, "%QW32");
			land(helper, plc, -4, U, 8, 1, "%IX0.0");
			land(helper, plc, -4, U, 8, 2, "%IX0.1");
			land(helper, plc, 10, U, 0, 0, "%QX16.0");
			land(helper, plc, 10, U, 0, 1, "%IX0.2");
			land(helper, plc, 10, U, 0, 2, "%IX0.3");
			land(helper, plc, 10, U, 0, 3, "%IX0.4");
			run(helper, plc, """
					PROGRAM MAIN
					VAR
						rate AT %QW32 : WORD; burning AT %IX0.0 : BOOL; fireAuto AT %IX0.1 : BOOL;
						runCmd AT %QX16.0 : BOOL; running AT %IX0.2 : BOOL; tripped AT %IX0.3 : BOOL; plantAuto AT %IX0.4 : BOOL;
						ratePct : INT; want : BOOL; burnScans, scans : DINT;
					END_VAR
					rate := INT_TO_WORD(ratePct * 320);
					runCmd := want;
					scans := scans + 1;
					IF burning THEN burnScans := burnScans + 1; END_IF;
					END_PROGRAM""");
		});
		// the firebox at 0 %: nothing burns
		helper.runAfterDelay(260, () -> {
			log(scene, "0 % | " + read(helper, -4, U, 8));
			check(helper, on(plc, "MAIN.FIREAUTO") && on(plc, "MAIN.PLANTAUTO") && !on(plc, "MAIN.BURNING"), "at 0 % the burner should not burn: " + read(helper, -4, U, 8));
			plc.cpu().write("MAIN.RATEPCT", (long) 50);
			plc.cpu().write("MAIN.BURNSCANS", (long) 0);
			plc.cpu().write("MAIN.SCANS", (long) 0);
			plc.cpu().write("MAIN.WANT", true);
		});
		helper.runAfterDelay(300, () -> plant.setHeatForTest(0.99));
		helper.runAfterDelay(1060, () -> {
			double share = number(plc, "MAIN.BURNSCANS") / Math.max(1, number(plc, "MAIN.SCANS"));
			log(scene, String.format("50 %% | burning %.0f %% of the time | %s | plant %s", share * 100, read(helper, -4, U, 8), read(helper, 10, U, 0)));
			check(helper, share > 0.3 && share < 0.8, "at 50 % the burner should burn about half the time: " + share);
			check(helper, plant.wantRun(), "RUN should start the steam plant: " + read(helper, 10, U, 0));
		});
		await(helper, 2700, () -> helper.getLevel().getGameTime() > 0 && on(plc, "MAIN.RUNNING"), () -> {
			log(scene, "plant | " + read(helper, 10, U, 0));
			check(helper, on(plc, "MAIN.RUNNING") && !on(plc, "MAIN.TRIPPED"), "the PLC should read the turbine generator running: " + read(helper, 10, U, 0));
			plc.cpu().write("MAIN.WANT", false);
			helper.runAfterDelay(100, () -> {
				log(scene, "RUN off | " + read(helper, 10, U, 0));
				check(helper, !plant.wantRun() && !on(plc, "MAIN.RUNNING"), "RUN off should stop it: " + read(helper, 10, U, 0));
				log(scene, "ok");
				helper.succeed();
			});
		});
	}

	// ---- plc_rectifier ------------------------------------------------------------------------------------------------------------
	static void rectifier(GameTestHelper helper) {
		String scene = "plc_rectifier";
		floor(helper, -8, -4, 34, 12);
		GridSceneKit.genset(helper, 10, U, 0);
		GasPowerScenes.inline(helper, WildcatterBlocks.UNIT_SUBSTATION, 10, 0, Direction.EAST, false);   // 4.16 kV out at x 15
		for (int x = 15; x <= 17; x++) {
			put(helper, "cable_mv", x, U, 0);
		}
		place(helper, WildcatterBlocks.RECTIFIER, 18, U, 0, NORTH);   // x 18-20, z 0..3, + and - east at z 2
		for (int x = 21; x <= 28; x++) {
			helper.getLevel().setBlock(abs(helper, x, U + 2, 2), dev.wildcatter.WildcatterBlocks.DC_BUS_16K.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X), 3);
			helper.getLevel().setBlock(abs(helper, x, U, 2), dev.wildcatter.WildcatterBlocks.DC_BUS_16K.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X), 3);
		}
		place(helper, WildcatterBlocks.MAGNESIUM_CELL, 31, U, 5, Direction.SOUTH);   // x 29-31, z 0..5
		((dev.wildcatter.block.MagnesiumCellBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 31, U, 5))).addMgCl2(Tuning.MG_CELL_HOLD_KG);
		supply(helper, -6, U, 8);
		put(helper, "junction_box", -2, U, 8);
		PlcCabinetBlockEntity plc = cabinet(helper, -1, U, 8, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AI8, Type.AO4);
		RectifierBlockEntity rect = (RectifierBlockEntity) helper.getLevel().getBlockEntity(abs(helper, 18, U, 0));
		double top = Tuning.RECTIFIER_SETPOINTS_KA[Tuning.RECTIFIER_SETPOINTS_KA.length - 1] * 1000;
		helper.runAfterDelay(10, () -> {
			cable(helper, -1, U, 8, 18, U, 3);
			auto(helper, 18, U, 0);
			land(helper, plc, 18, U, 0, 0, "%QX16.0");
			land(helper, plc, 18, U, 0, 1, "%QW48");
			land(helper, plc, 18, U, 0, 2, "%IX0.0");
			land(helper, plc, 18, U, 0, 3, "%IX0.1");
			land(helper, plc, 18, U, 0, 4, "%IW32");
			land(helper, plc, 18, U, 0, 5, "%IW34");
			run(helper, plc, """
					PROGRAM MAIN
					VAR
						runCmd AT %QX16.0 : BOOL; setpoint AT %QW48 : WORD; tripped AT %IX0.0 : BOOL; isAuto AT %IX0.1 : BOOL;
						amps AT %IW32 : WORD; volts AT %IW34 : WORD;
						want : BOOL; wantKa : REAL; ka, v : REAL;
					END_VAR
					runCmd := want;
					setpoint := INT_TO_WORD(REAL_TO_INT(wantKa / %TOP% * 32000.0));
					ka := INT_TO_REAL(WORD_TO_INT(amps)) / 32000.0 * %TOP%;
					v := INT_TO_REAL(WORD_TO_INT(volts)) / 32000.0 * %VMAX%;
					END_PROGRAM""".replace("%TOP%", String.format("%.1f", top / 1000)).replace("%VMAX%", String.format("%.1f", Tuning.RECTIFIER_MAX_DC_VOLTS)));
		});
		GridSceneKit.start(helper, 10, U, 0, () -> {
			// in AUTO with no RUN the rectifier is off
			check(helper, on(plc, "MAIN.ISAUTO") && Math.abs(rect.dcAmps()) < 100, "in AUTO with RUN off the rectifier should be off: " + read(helper, 18, U, 0));
			plc.cpu().write("MAIN.WANTKA", 8.0);
			plc.cpu().write("MAIN.WANT", true);
			helper.runAfterDelay(300, () -> {
				double amps = rect.dcAmps();
				double ka = number(plc, "MAIN.KA");
				log(scene, String.format("8 kA | %s | cell %s | PLC reads %.2f kA, %.1f V", read(helper, 18, U, 0), read(helper, 31, U, 5), ka, number(plc, "MAIN.V")));
				check(helper, Math.abs(amps - 8000) < 8000 * 0.03, "the PLC's 8 kA setpoint should hold: " + amps);
				check(helper, Math.abs(ka * 1000 - amps) < 250 && number(plc, "MAIN.V") > 1, "the PLC should read its amps and volts: " + ka + " kA");
				plc.cpu().write("MAIN.WANTKA", 12.0);
				helper.runAfterDelay(200, () -> {
					check(helper, Math.abs(rect.dcAmps() - 12000) < 12000 * 0.03, "and follow it to 12 kA: " + rect.dcAmps());
					plc.cpu().write("MAIN.WANT", false);
					helper.runAfterDelay(100, () -> {
						log(scene, "RUN off | " + read(helper, 18, U, 0));
						check(helper, Math.abs(rect.dcAmps()) < 100 && !on(plc, "MAIN.TRIPPED"), "RUN off should stop it: " + rect.dcAmps());
						log(scene, "ok");
						helper.succeed();
					});
				});
			});
		});
	}
}
