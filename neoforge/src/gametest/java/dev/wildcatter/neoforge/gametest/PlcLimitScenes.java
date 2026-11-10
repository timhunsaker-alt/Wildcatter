package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.abs;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.air;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.PlcKit.auto;
import static dev.wildcatter.neoforge.gametest.PlcKit.cabinet;
import static dev.wildcatter.neoforge.gametest.PlcKit.land;
import static dev.wildcatter.neoforge.gametest.PlcKit.line;
import static dev.wildcatter.neoforge.gametest.PlcKit.run;
import static dev.wildcatter.neoforge.gametest.PlcKit.supply;

import dev.wildcatter.control.ControlNet;
import dev.wildcatter.control.ControlSupplyBlockEntity;
import dev.wildcatter.control.OperatorDeviceBlock;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem.Type;
import dev.wildcatter.control.RemoteIoDropBlockEntity;
import dev.wildcatter.control.SharedOutputs;
import dev.wildcatter.multiblock.BigParts;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The PLC's limits, enforced (docs/questions/plc-gaps.md):
 * <ul>
 * <li>{@code plc_supply}: 30 horns on a 24 V, 10 A supply draw 8.35 A and sound; the program turning 14 more on takes it past 10 A, and the
 * supply goes into hiccup: off, every PLC on it dark, then on again, the CPU's warm restart clearing the extra horns.</li>
 * <li>{@code plc_cores}: 34 stack lights behind one 50-pair cable: the 99th signal is refused, as the cable's 98 signal cores are taken; a
 * second cable to the same box takes it; cut that cable, and the signals past the first cable's cores go dead.</li>
 * <li>{@code plc_owners}: a rack is owned by the first PLC to reach it, and a second's landing on it is refused until the owner's
 * connection is cut; a lamp landed on two PLCs is lit by either (relay contacts in parallel); a control valve's loop driven by both reads
 * as failed.</li>
 * </ul>
 */
final class PlcLimitScenes {
	private PlcLimitScenes() {
	}

	private static final int U = 2;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("plc_supply", PlcLimitScenes::supplyLimit);
		scenes.put("plc_cores", PlcLimitScenes::cores);
		scenes.put("plc_owners", PlcLimitScenes::owners);
		return scenes;
	}

	/** A DO16 channel's address: the k-th output counted from slot {@code first}. */
	static String out(int first, int k) {
		int slot = first + k / 16;
		int channel = k % 16;
		return "%QX" + (slot * PlcCabinetBlockEntity.BYTES_PER_SLOT + channel / 8) + "." + channel % 8;
	}

	static int lamps(GameTestHelper helper, int x, int y, int z) {
		return helper.getLevel().getBlockState(abs(helper, x, y, z)).getValue(OperatorDeviceBlock.LAMPS);
	}

	static ControlSupplyBlockEntity supplyAt(GameTestHelper helper, int x, int y, int z) {
		return (ControlSupplyBlockEntity) helper.getLevel().getBlockEntity(abs(helper, x, y, z));
	}

	// ---- plc_supply ---------------------------------------------------------------------------------------------------------------
	static void supplyLimit(GameTestHelper helper) {
		String scene = "plc_supply";
		floor(helper, -8, -3, 24, 3);
		supply(helper, -6, U, 0);
		put(helper, "junction_box", -2, U, 0);
		PlcCabinetBlockEntity cabinet = cabinet(helper, -1, U, 0, Type.CPU_SMALL, Type.DO16, Type.DO16, Type.DO16);
		int horns = 44;
		for (int x = 0; x < horns / 2; x++) {
			put(helper, "junction_box", x, U + 1, 0);    // along the cabinet's door: terminals on one rail, no cable
			put(helper, "horn", x, U + 1, -1);
			put(helper, "horn", x, U + 1, 1);
		}
		helper.runAfterDelay(40, () -> {
			StringBuilder vars = new StringBuilder("PROGRAM MAIN\nVAR\n");
			StringBuilder code = new StringBuilder();
			for (int k = 0; k < horns; k++) {
				int x = k / 2;
				int z = k % 2 == 0 ? -1 : 1;
				auto(helper, x, U + 1, z);
				land(helper, cabinet, x, U + 1, z, 0, out(0, k));
				vars.append("h").append(k).append(" AT ").append(out(0, k)).append(" : BOOL;\n");
				code.append("h").append(k).append(" := ").append(k < 30 ? "TRUE" : "big").append(";\n");
			}
			vars.append("big : BOOL;\nEND_VAR\n");
			run(helper, cabinet, vars + code.toString() + "END_PROGRAM");
		});
		ControlSupplyBlockEntity[] psu = new ControlSupplyBlockEntity[1];
		helper.runAfterDelay(100, () -> {
			psu[0] = supplyAt(helper, -3, U, 0);
			double amps = psu[0].amps(helper.getLevel().getGameTime());
			log(scene, "30 horns | " + read(helper, -3, U, 0) + " | " + read(helper, -1, U, 0));
			// 0.4 A the CPU, 0.05 A each module, 30 x (0.25 A a horn + 0.01 A its relay)
			check(helper, Math.abs(amps - 8.35) < 0.05, "30 horns and the rail should draw 8.35 A: " + read(helper, -3, U, 0));
			check(helper, lamps(helper, 0, U + 1, -1) == 1 && lamps(helper, 20, U + 1, -1) == 0, "the first 30 horns should sound, the rest not");
			check(helper, psu[0].overloads() == 0 && read(helper, -1, U, 0).contains("RUN"), "and the supply should hold: " + read(helper, -1, U, 0));
			cabinet.cpu().write("MAIN.BIG", true);
		});
		boolean[] dark = new boolean[1];
		for (int t = 101; t <= 160; t++) {
			helper.runAfterDelay(t, () -> dark[0] |= read(helper, -1, U, 0).contains("OVERLOADED"));
		}
		helper.runAfterDelay(160, () -> {
			log(scene, "44 horns | " + read(helper, -3, U, 0));
			check(helper, psu[0].overloads() >= 1, "44 horns, 11.99 A, should overload the 10 A supply: " + read(helper, -3, U, 0));
			check(helper, dark[0], "the cabinet should say its supply is overloaded while it is off");
		});
		helper.runAfterDelay(260, () -> {
			double amps = psu[0].amps(helper.getLevel().getGameTime());
			log(scene, "after the hiccup | " + read(helper, -3, U, 0) + " | " + read(helper, -1, U, 0));
			check(helper, read(helper, -1, U, 0).contains("RUN") && !psu[0].overloaded(), "the supply should come back and the CPU run again: " + read(helper, -1, U, 0));
			check(helper, Math.abs(amps - 8.35) < 0.05 && lamps(helper, 20, U + 1, -1) == 0, "a warm restart clears BIG, so 30 horns again: " + read(helper, -3, U, 0));
			log(scene, "ok");
			helper.succeed();
		});
	}

	// ---- plc_cores ----------------------------------------------------------------------------------------------------------------
	static void cores(GameTestHelper helper) {
		String scene = "plc_cores";
		floor(helper, -8, -3, 20, 12);
		supply(helper, -6, U, 0);
		put(helper, "junction_box", -2, U, 0);
		PlcCabinetBlockEntity cabinet = cabinet(helper, -1, U, 0, Type.CPU_SMALL, Type.DO16, Type.DO16, Type.DO16, Type.DO16, Type.DO16, Type.DO16, Type.DO16);
		line(helper, "control_cable", -1, U, 1, -1, U, 7);           // cable A: the cabinet to the box row
		int lights = 34;
		int[][] at = new int[lights][];
		for (int x = -1; x <= 16; x++) {
			put(helper, "junction_box", x, U, 8);
		}
		for (int i = 0; i < lights; i++) {
			int x = -1 + i / 2;
			at[i] = i % 2 == 0 ? new int[] {x, U, 9} : new int[] {x, U + 1, 8};
			put(helper, "stack_light", at[i][0], at[i][1], at[i][2]);
		}
		String[] refused = new String[1];
		helper.runAfterDelay(40, () -> {
			check(helper, read(helper, -1, U, 0).contains(lights + " devices reached"), "the cabinet should reach every stack light: " + read(helper, -1, U, 0));
			int k = 0;
			for (int i = 0; i < lights && refused[0] == null; i++) {
				auto(helper, at[i][0], at[i][1], at[i][2]);
				for (int point = 0; point < 3 && refused[0] == null; point++, k++) {
					BlockPos main = ControlNet.mainOf(helper.getLevel(), abs(helper, at[i][0], at[i][1], at[i][2]));
					String why = cabinet.land(main, point, out(0, k));
					if (why != null) {
						refused[0] = why;
						check(helper, k == ControlNet.SIGNAL_CORES, "the first cable should take " + ControlNet.SIGNAL_CORES + " signals, refused at " + k + ": " + why);
					}
				}
			}
			log(scene, "refused: " + refused[0]);
			check(helper, refused[0] != null && refused[0].contains("CABLE FULL"), "the 99th signal should find the cable full: " + refused[0]);
			// cable B: the cabinet's east face, along and down to the same box row
			line(helper, "control_cable", 0, U, 0, 3, U, 0);
			line(helper, "control_cable", 3, U, 1, 3, U, 7);
		});
		helper.runAfterDelay(60, () -> {
			for (int k = ControlNet.SIGNAL_CORES; k < lights * 3; k++) {
				int[] c = at[k / 3];
				land(helper, cabinet, c[0], c[1], c[2], k % 3, out(0, k));
			}
			StringBuilder vars = new StringBuilder("PROGRAM MAIN\nVAR\n");
			StringBuilder code = new StringBuilder();
			for (int i = 0; i < lights; i++) {
				vars.append("g").append(i).append(" AT ").append(out(0, i * 3 + 2)).append(" : BOOL;\n");   // each light's green
				code.append("g").append(i).append(" := TRUE;\n");
			}
			run(helper, cabinet, vars + "END_VAR\n" + code + "END_PROGRAM");
		});
		helper.runAfterDelay(100, () -> {
			log(scene, "two cables | " + read(helper, -1, U, 0));
			check(helper, (lamps(helper, at[0][0], at[0][1], at[0][2]) & 4) != 0 && (lamps(helper, at[33][0], at[33][1], at[33][2]) & 4) != 0,
					"with two cables every green should light: " + read(helper, -1, U, 0));
			check(helper, !read(helper, -1, U, 0).contains("CABLE FULL"), "nothing should be over: " + read(helper, -1, U, 0));
			air(helper, 3, U, 4);   // cable B cut
		});
		helper.runAfterDelay(130, () -> {
			String status = read(helper, -1, U, 0);
			log(scene, "cable B cut | " + status);
			check(helper, status.contains("CABLE FULL"), "with one cable the cabinet should name it full: " + status);
			check(helper, (lamps(helper, at[0][0], at[0][1], at[0][2]) & 4) != 0, "the first lights keep their cores: " + status);
			check(helper, (lamps(helper, at[33][0], at[33][1], at[33][2]) & 4) == 0, "the last light's signals have no core, so it goes dark");
			log(scene, "ok");
			helper.succeed();
		});
	}

	// ---- plc_owners ---------------------------------------------------------------------------------------------------------------
	static void owners(GameTestHelper helper) {
		String scene = "plc_owners";
		floor(helper, -8, -3, 8, 8);
		supply(helper, -6, U, 0);
		put(helper, "junction_box", -2, U, 0);
		line(helper, "junction_box", -2, U, 1, -2, U, 4);
		PlcCabinetBlockEntity a = cabinet(helper, -1, U, 0, Type.CPU_SMALL, Type.DO16, Type.AO4, Type.COMM);
		PlcCabinetBlockEntity b = cabinet(helper, -1, U, 4, Type.DO16, Type.AO4, Type.COMM);   // its CPU comes later
		a.setStation("PLC-A");
		b.setStation("PLC-B");
		BlockPos dropAt = abs(helper, -1, U, 2);
		BlockState drop = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("remote_io_drop")).defaultBlockState();
		if (drop.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			drop = drop.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		}
		helper.getLevel().setBlock(dropAt, drop, 3);
		BigParts.grow(helper.getLevel(), dropAt, drop);
		RemoteIoDropBlockEntity rack = (RemoteIoDropBlockEntity) helper.getLevel().getBlockEntity(dropAt);
		rack.seat(Type.DO16);
		put(helper, "pilot_light", 0, U, 2);              // on the rack
		put(helper, "pilot_light", -2, U + 1, 2);         // on the boxes both cabinets reach
		put(helper, "control_valve", -2, U + 1, 3);       // a loop both can drive
		line(helper, "network_cable", 0, U + 1, 0, 0, U + 1, 4);
		helper.runAfterDelay(40, () -> {
			log(scene, "A | " + read(helper, -1, U, 0) + " | rack | " + read(helper, -1, U, 2));
			check(helper, rack.owner(helper.getLevel().getGameTime()).equals("PLC-A"), "PLC-A should own rack 1: " + read(helper, -1, U, 2));
			land(helper, a, 0, U, 2, 0, "%QX192.0");
			b.seat(Type.CPU_SMALL);
			auto(helper, 0, U, 2);
			auto(helper, -2, U + 1, 2);
			auto(helper, -2, U + 1, 3);
			land(helper, a, -2, U + 1, 2, 0, "%QX0.0");
			land(helper, a, -2, U + 1, 3, 0, "%QW16");
			run(helper, a, "PROGRAM MAIN VAR lamp AT %QX0.0 : BOOL; want : BOOL; rackLamp AT %QX192.0 : BOOL; signal AT %QW16 : INT; END_VAR lamp := want; rackLamp := TRUE; signal := 16000; END_PROGRAM");
		});
		helper.runAfterDelay(60, () -> {
			String refused = b.land(abs(helper, 0, U, 2), 0, "%QX192.0");
			log(scene, "B on rack 1: " + refused + " | B | " + read(helper, -1, U, 4));
			check(helper, refused != null && refused.contains("belongs to PLC-A"), "a second PLC's landing on rack 1 should be refused: " + refused);
			land(helper, b, -2, U + 1, 2, 0, "%QX0.0");
			land(helper, b, -2, U + 1, 3, 0, "%QW16");
			run(helper, b, "PROGRAM MAIN VAR lamp AT %QX0.0 : BOOL; want : BOOL := TRUE; signal AT %QW16 : INT; rackLamp AT %QX192.0 : BOOL; END_VAR lamp := want; signal := 16000; rackLamp := TRUE; END_PROGRAM");
		});
		helper.runAfterDelay(90, () -> {
			log(scene, "both | " + read(helper, -1, U, 0) + " | " + read(helper, -1, U, 4) + " | valve " + read(helper, -2, U + 1, 3));
			check(helper, lamps(helper, -2, U + 1, 2) == 1, "B's contact closed and A's open: the lamp lights, as contacts in parallel do");
			check(helper, lamps(helper, 0, U, 2) == 1, "A lights its rack's lamp");
			check(helper, SharedOutputs.fighting(helper.getLevel(), abs(helper, -2, U + 1, 3), 0, helper.getLevel().getGameTime()), "two PLCs on one loop should fight");
			check(helper, (read(helper, -1, U, 0) + read(helper, -1, U, 4)).contains("TWO PLCs ON ONE LOOP"), "and a cabinet should say so");
			b.cpu().write("MAIN.WANT", false);
			a.cpu().write("MAIN.WANT", true);
		});
		helper.runAfterDelay(110, () -> {
			check(helper, lamps(helper, -2, U + 1, 2) == 1, "A's contact alone lights it too");
			a.cpu().write("MAIN.WANT", false);
			air(helper, 0, U + 1, 1);   // A's network cut: its connection to the rack times out
		});
		helper.runAfterDelay(130, () -> check(helper, lamps(helper, -2, U + 1, 2) == 0, "with both contacts open the lamp is dark"));
		helper.runAfterDelay(170, () -> {
			String refused = b.land(abs(helper, 0, U, 2), 0, "%QX192.0");
			log(scene, "after the cut: " + refused + " | rack | " + read(helper, -1, U, 2));
			check(helper, refused == null, "with A's connection gone B should take the rack: " + refused + " | " + read(helper, -1, U, 2));
		});
		helper.runAfterDelay(200, () -> {
			check(helper, rack.owner(helper.getLevel().getGameTime()).equals("PLC-B") && lamps(helper, 0, U, 2) == 1, "B should own the rack and light its lamp: " + read(helper, -1, U, 2));
			log(scene, "ok");
			helper.succeed();
		});
	}
}
