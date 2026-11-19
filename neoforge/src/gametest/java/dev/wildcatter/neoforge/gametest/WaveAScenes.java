package dev.wildcatter.neoforge.gametest;

import static dev.wildcatter.neoforge.gametest.GridSceneKit.air;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.check;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.floor;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.log;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.put;
import static dev.wildcatter.neoforge.gametest.GridSceneKit.read;
import static dev.wildcatter.neoforge.gametest.PlcKit.cabinet;
import static dev.wildcatter.neoforge.gametest.PlcKit.number;
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
 * </ul>
 */
final class WaveAScenes {
	private WaveAScenes() {
	}

	private static final int U = 2;

	static Map<String, Consumer<GameTestHelper>> scenes() {
		Map<String, Consumer<GameTestHelper>> scenes = new LinkedHashMap<>();
		scenes.put("plc_card_boot", WaveAScenes::cardBoot);
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
}
