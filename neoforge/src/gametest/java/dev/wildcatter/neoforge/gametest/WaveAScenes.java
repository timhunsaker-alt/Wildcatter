package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.air;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.PlcKit.cabinet;
import static dev.wildcatter.neoforge.gametest.PlcKit.number;
import static dev.wildcatter.neoforge.gametest.PlcKit.on;
import static dev.wildcatter.neoforge.gametest.PlcKit.run;
import static dev.wildcatter.neoforge.gametest.PlcKit.supply;

import dev.wildcatter.control.MemoryCardItem;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem.Type;
import dev.wildcatter.plc.Cpu;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Wave A (Tim's answers of 2026-11-19, docs/questions/wave-a.md):
 * <ul>
 * <li>{@code plc_card_boot}: a running CPU's project and its RETAIN values copied to a memory card; the card seated in a blank CPU whose
 * supply is dead; the supply comes on, and the CPU boots from the card in RUN with the retained count carried over and the plain one from
 * zero.</li>
 * <li>{@code plc_crane_lift}: a crawler crane under a PLC (docs/multiblocks/crane-plc-lift.md): in HAND, LIFT START does nothing; in
 * AUTO with RUN ENABLE off, one start makes one lift and the crane holds again, HOOK LOAD and RADIUS read it; a wind over its limit and
 * then a person in its swing area each refuse a start; calm and clear, a start works again.</li>
 * </ul>
 */
final class WaveAScenes {
	private WaveAScenes() {
	}

	private static final int U = 2;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("plc_card_boot", WaveAScenes::cardBoot);
		scenes.put("plc_crane_lift", WaveAScenes::craneLift);
		return scenes;
	}

	// ---- plc_card_boot --------------------------------------------------------------------------------------------------------------
	static void cardBoot(GameTestHelper helper) {
		String scene = "plc_card_boot";
		floor(helper, -8, -3, 4, 10);
		supply(helper, -6, U, 0);
		put(helper, "junction_box", -2, U, 0);
		PlcCabinetBlockEntity first = cabinet(helper, -1, U, 0, Type.CPU_SMALL);
		supply(helper, -6, U, 6);
		air(helper, -4, U, 6);    // the second plant's hot wire, put in once the card is seated
		put(helper, "junction_box", -2, U, 6);
		PlcCabinetBlockEntity blank = cabinet(helper, -1, U, 6, Type.CPU_SMALL);
		helper.runAfterDelay(40, () -> run(helper, first, """
				PROGRAM MAIN
				VAR RETAIN
					kept : DINT;
				END_VAR
				VAR
					plain : DINT;
				END_VAR
				kept := kept + 1;
				plain := plain + 1;
				END_PROGRAM"""));
		double[] copied = new double[1];
		helper.runAfterDelay(100, () -> {
			ItemStack card = new ItemStack(BuiltInRegistries.ITEM.getValue(dev.wildcatter.Wildcatter.id("memory_card")));
			check(helper, first.seatCard(card) == null, "the card should seat");
			String refused = first.toCard();
			check(helper, refused == null, "the project should go to the card: " + refused);
			copied[0] = number(first, "MAIN.KEPT");
			String said = MemoryCardItem.describe(first.card());
			log(scene, "card: " + said + " | kept " + copied[0]);
			check(helper, said.contains("1 retained value"), "the card should carry the one RETAIN variable: " + said);
			check(helper, copied[0] > 10, "the first CPU should have been counting: " + copied[0]);
			ItemStack out = first.unseatLast();
			check(helper, blank.seatCard(out) == null, "the card should seat in the blank cabinet");
			check(helper, blank.cpu() == null && blank.project().isEmpty(), "the second CPU holds no project");
			put(helper, "plastic_wire_hot", -4, U, 6);
		});
		helper.runAfterDelay(160, () -> {
			log(scene, "after power-up | " + read(helper, -1, U, 6));
			check(helper, blank.cpu() != null && blank.cpu().mode() == Cpu.Mode.RUN, "the blank CPU should boot from its card in RUN: " + read(helper, -1, U, 6));
			double kept = number(blank, "MAIN.KEPT");
			double plain = number(blank, "MAIN.PLAIN");
			log(scene, "kept " + kept + ", plain " + plain);
			check(helper, kept > copied[0] && plain < kept - copied[0] + 2, "RETAIN kept " + kept + " should carry on from " + copied[0] + ", plain " + plain + " from zero");
			log(scene, "ok");
			helper.succeed();
		});
	}

	// ---- plc_crane_lift -------------------------------------------------------------------------------------------------------------
	static void craneLift(GameTestHelper helper) {
		String scene = "plc_crane_lift";
		floor(helper, -12, -24, 28, 16);
		helper.getLevel().getServer().setWeatherParameters(36_000, 0, false, false);
		net.minecraft.core.BlockPos main = dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 0, 1, 0);
		net.minecraft.world.level.block.state.BlockState state = dev.wildcatter.WildcatterBlocks.CRAWLER_CRANE.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH);
		helper.getLevel().setBlock(main, state, 3);
		dev.wildcatter.multiblock.BigParts.grow(helper.getLevel(), main, state);   // x 0-8, z 0-8
		dev.wildcatter.block.CraneBlockEntity crane = (dev.wildcatter.block.CraneBlockEntity) helper.getLevel().getBlockEntity(main);
		crane.testFuel(500);
		crane.testSpeedup(8);   // the lift's real speeds, eight times over, so the scene's four starts fit
		PlcKit.device(helper, 0, 1, 0).cycleHoa(main);   // HAND to OFF before it first looks for work: no operator has given it to the PLC
		// its site 17 m north, surveyed from stone bricks (two lifts), its bricks in a chest east of its base (as plc_crane)
		for (int x = 3; x <= 5; x++) {
			for (int z = -14; z <= -12; z++) {
				for (int yy = 1; yy <= 2; yy++) {   // two courses: at least two lifts
					helper.getLevel().setBlock(dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, x, yy, z), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
				}
			}
		}
		net.minecraft.core.BlockPos origin = dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 2, 1, -15);
		String data = dev.wildcatter.item.SurveyToolItem.record(helper.getLevel(), origin, dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 6, 2, -11)).compressed();
		for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(origin, dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 6, 2, -11))) {
			helper.getLevel().setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		}
		helper.getLevel().setBlock(origin, dev.wildcatter.WildcatterBlocks.SITE_MARKER.defaultBlockState(), 3);
		((dev.wildcatter.block.SiteMarkerBlockEntity) helper.getLevel().getBlockEntity(origin)).set(data, 0);
		helper.getLevel().setBlock(dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 9, 1, 4), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
		((net.minecraft.world.Container) helper.getLevel().getBlockEntity(dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 9, 1, 4))).setItem(0, new ItemStack(net.minecraft.world.item.Items.STONE_BRICKS, 64));
		supply(helper, 16, U, 8);
		put(helper, "junction_box", 20, U, 8);
		PlcCabinetBlockEntity cabinet = cabinet(helper, 21, U, 8, Type.CPU_SMALL, Type.DI16, Type.DO16, Type.AI8);
		PlcKit.line(helper, "control_cable", 20, 1, 8, 20, 1, 6);
		PlcKit.line(helper, "control_cable", 19, 1, 6, 9, 1, 6);
		helper.runAfterDelay(20, () -> {
			PlcKit.land(helper, cabinet, 0, 1, 0, 0, "%QX16.0");
			PlcKit.land(helper, cabinet, 0, 1, 0, 1, "%IX0.0");
			PlcKit.land(helper, cabinet, 0, 1, 0, 2, "%IX0.1");
			PlcKit.land(helper, cabinet, 0, 1, 0, 3, "%QX16.1");
			PlcKit.land(helper, cabinet, 0, 1, 0, 4, "%IX0.2");
			PlcKit.land(helper, cabinet, 0, 1, 0, 5, "%IW32");
			PlcKit.land(helper, cabinet, 0, 1, 0, 6, "%IW34");
			PlcKit.land(helper, cabinet, 0, 1, 0, 8, "%IX0.3");
			run(helper, cabinet, "PROGRAM MAIN VAR enable AT %QX16.0 : BOOL; start AT %QX16.1 : BOOL; carrying AT %IX0.0 : BOOL; auto AT %IX0.1 : BOOL;"
					+ " refused AT %IX0.2 : BOOL; clear AT %IX0.3 : BOOL; load AT %IW32 : INT; radius AT %IW34 : INT; go : BOOL; lifts : INT; seen : R_TRIG; END_VAR"
					+ " start := go; seen(CLK := carrying); IF seen.Q THEN lifts := lifts + 1; END_IF; END_PROGRAM");
		});
		// 1: in OFF a start does nothing (and is not refused: the PLC has not been given the crane)
		helper.runAfterDelay(30, () -> {
			cabinet.cpu().write("MAIN.GO", true);
		});
		helper.runAfterDelay(40, () -> {
			cabinet.cpu().write("MAIN.GO", false);
			check(helper, !crane.liftRefused() && !on(cabinet, "MAIN.AUTO") && !crane.carrying(), "in OFF a start should be ignored, not refused");
			PlcKit.auto(helper, 0, 1, 0);   // the operator gives the crane to the PLC: the permissive
		});
		// 2: AUTO, no RUN ENABLE: the crane holds; one start makes one lift
		helper.runAfterDelay(120, () -> {
			String held = crane.inspect().getString();
			log(scene, "AUTO, no start | " + held);
			check(helper, on(cabinet, "MAIN.AUTO") && !crane.carrying(), "in AUTO with no start it should hold: " + held);
			cabinet.cpu().write("MAIN.GO", true);
		});
		helper.runAfterDelay(125, () -> cabinet.cpu().write("MAIN.GO", false));
		double[] seenLoad = {-1};
		for (int t = 130; t <= 1300; t += 5) {
			helper.runAfterDelay(t, () -> {
				if (seenLoad[0] < 0 && crane.carrying()) {
					seenLoad[0] = crane.readMa(5);
					String at = crane.inspect().getString();
					double tonnes = (crane.readMa(5) - 4) / 16 * crane.chart().capacity(crane.chart().minRadius());
					log(scene, "one lift, carrying | " + at + " | HOOK LOAD " + String.format("%.2f mA (%.2f t)", crane.readMa(5), tonnes)
							+ ", RADIUS " + String.format("%.2f mA", crane.readMa(6)) + " | raw " + number(cabinet, "MAIN.LOAD") + ", " + number(cabinet, "MAIN.RADIUS"));
					check(helper, at.contains("BUILDING") && on(cabinet, "MAIN.CARRYING"), "a start should make a lift: " + at);
					check(helper, at.contains(String.format("%.1f t", tonnes)) || Math.abs(tonnes - parseTonnes(at)) < 0.5, "HOOK LOAD should read the tonnes on the hook: " + tonnes + " | " + at);
					check(helper, crane.readMa(6) > 4.5 && number(cabinet, "MAIN.LOAD") > 0 && number(cabinet, "MAIN.RADIUS") > 0, "RADIUS and the PLC's raw counts should read");
				}
			});
		}
		// 3: after its one lift it holds: no second lift without a second start
		helper.runAfterDelay(1400, () -> {
			String after = crane.inspect().getString();
			log(scene, "after one lift | " + after + " | lifts " + number(cabinet, "MAIN.LIFTS"));
			check(helper, seenLoad[0] > 4 && number(cabinet, "MAIN.LIFTS") == 1 && !crane.carrying() && !after.contains("BUILDING"),
					"one start should make exactly one lift, then hold: " + after + " | lifts " + number(cabinet, "MAIN.LIFTS"));
			crane.testWind(40);   // a gale: over the crawler's limit at its boom head
			cabinet.cpu().write("MAIN.GO", true);
		});
		helper.runAfterDelay(1405, () -> cabinet.cpu().write("MAIN.GO", false));
		helper.runAfterDelay(1420, () -> {
			String gale = crane.inspect().getString();
			log(scene, "start in a gale | " + gale);
			check(helper, crane.liftRefused() && on(cabinet, "MAIN.REFUSED") && gale.contains("LIFT REFUSED") && gale.contains("WIND"), "a start in a gale should be refused: " + gale);
			crane.testWind(2);
		});
		// 4: a person in the swing area refuses a start, and AREA CLEAR reads false
		net.minecraft.server.level.ServerPlayer[] person = new net.minecraft.server.level.ServerPlayer[1];
		helper.runAfterDelay(1430, () -> {
			person[0] = helper.makeMockServerPlayerInLevel();
			net.minecraft.core.BlockPos spot = dev.wildcatter.neoforge.gametest.GridSceneKit.abs(helper, 4, 1, -6);
			person[0].teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		});
		helper.runAfterDelay(1440, () -> cabinet.cpu().write("MAIN.GO", true));
		helper.runAfterDelay(1445, () -> cabinet.cpu().write("MAIN.GO", false));
		helper.runAfterDelay(1460, () -> {
			String someone = crane.inspect().getString();
			log(scene, "start with a person under the boom | " + someone + " | AREA CLEAR " + on(cabinet, "MAIN.CLEAR"));
			check(helper, crane.liftRefused() && someone.contains("SWING AREA NOT CLEAR") && !crane.carrying(), "a person in the swing area should refuse the start: " + someone);
			check(helper, !on(cabinet, "MAIN.CLEAR"), "AREA CLEAR should read false with a person in it");
			person[0].discard();
		});
		// 5: calm and clear, a start works again
		helper.runAfterDelay(1480, () -> cabinet.cpu().write("MAIN.GO", true));
		helper.runAfterDelay(1485, () -> cabinet.cpu().write("MAIN.GO", false));
		helper.runAfterDelay(1520, () -> {
			String again = crane.inspect().getString();
			log(scene, "calm and clear | " + again + " | AREA CLEAR " + on(cabinet, "MAIN.CLEAR"));
			check(helper, !crane.liftRefused() && again.contains("BUILDING") && on(cabinet, "MAIN.CLEAR"), "calm and clear, a start should lift again: " + again);
			log(scene, "ok");
			helper.succeed();
		});
	}

	/** The first "N.N t on the hook" in a readout, or NaN. */
	private static double parseTonnes(String text) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]+\\.[0-9]) t on the hook").matcher(text);
		return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
	}
}
