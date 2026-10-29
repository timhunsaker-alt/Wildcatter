package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * The text of the Field Manual. One page per string; add a chapter here whenever the mod gains something to explain.
 * Numbers come from Tuning, so the book can't drift out of date when something is rebalanced.
 */
public final class Manual {
	private Manual() {
	}

	public static WrittenBookContent content() {
		List<Filterable<Component>> pages = new ArrayList<>();
		for (String page : pages()) {
			pages.add(Filterable.passThrough(Component.literal(page)));
		}
		return new WrittenBookContent(Filterable.passThrough("Wildcatter Field Manual"), "Wildcatter", 0, pages, true);
	}

	private static List<String> pages() {
		return List.of(
				"WILDCATTER\nField Manual\n\nPower from steam, gas and oil, built the way the real grid is.\n\nRead the safety pages before you touch anything.",

				"1. STEAM\n\nPut a Boiler on a lit campfire. Fill it with water buckets (" + Tuning.WATER_PER_BUCKET + " each). "
						+ "Put a Steam Turbine touching it.\n\nOne campfire makes " + (Tuning.BOILER_STEAM_PER_TICK * Tuning.TURBINE_RF_PER_STEAM)
						+ " RF a tick. Four buckets last about "
						+ (Tuning.BOILER_WATER_CAPACITY * Tuning.STEAM_PER_WATER / Tuning.BOILER_STEAM_PER_TICK / 20 / 60) + " minutes.",

				"2. VOLTAGE\n\n110 V: lamps and small machines.\n4.16 kV: bigger generators and local lines.\n138 kV: long hauls.\n\n"
						+ "Things only connect at the same voltage. A wire at one level will not join a wire at another: "
						+ "it draws no arm toward it.",

				"3. TRANSFORMERS\n\nA transformer joins two levels. The side it faces is the HIGH side. Power flows either way.\n\n"
						+ "Transformer: 110 V / 4.16 kV.\nSubstation: 4.16 kV / 138 kV.\n\nAbout "
						+ Math.round((1 - Tuning.TRANSFORMER_EFFICIENCY) * 100) + "% is lost as heat in each one.",

				"4. BARE vs INSULATED\n\nBare wire is cheap. While it carries power it SHOCKS anything that comes near it.\n\n"
						+ "Insulated cable is safe to touch. It costs slime (our rubber), and more of it the higher the voltage.",

				"5. REACH\n\nA bare wire reaches further the higher its voltage:\n\n110 V: you must touch it\n4.16 kV: " + (int) Tuning.BARE_REACH_MEDIUM
						+ " blocks\n138 kV: " + (int) Tuning.BARE_REACH_HIGH
						+ " blocks\n\nKeep bare lines up on tall poles, out of reach. Jumping adds more than a block.",

				"6. SHOCKS\n\n110 V: " + hearts(Tuning.SHOCK_DAMAGE_LOW) + " heart\n4.16 kV: " + hearts(Tuning.SHOCK_DAMAGE_MEDIUM)
						+ " hearts\n138 kV: " + hearts(Tuning.SHOCK_DAMAGE_HIGH)
						+ " hearts, which is all of them.\n\nDouble if you are wet. Water and rain are the worst place to be near a live wire.",

				"7. TWO CABLES\n\nA device needs at least " + Tuning.DEVICE_MIN_CABLES + " cables joined to it: one to bring the power and "
						+ "one to take it back. A lamp on a single wire is a dead end and stays dark.",

				"8. GROUNDING\n\nPut a Ground Rod in the earth against a machine, or run Ground Wire from the machine to a rod. "
						+ "A grounded machine is safe to touch.\n\nAn ungrounded machine with power in it may bite when you touch it: "
						+ Math.round(Tuning.TOUCH_SHOCK_CHANCE_LOW * 100) + "% at 110 V, " + Math.round(Tuning.TOUCH_SHOCK_CHANCE_MEDIUM * 100)
						+ "% at 4.16 kV, always at 138 kV.",

				"9. POWER METER\n\nRight-click any machine or cable to read what it holds and how much it is passing on. "
						+ "It also tells you whether a machine is grounded.\n\nThe meter is insulated. It is safe to use on live equipment.",

				"COMING\n\nGas and diesel turbines.\nDrilling rigs and wells.\nRefining.\nBreakers and fuses.\n\nThis book will grow as they do."
		);
	}

	private static String hearts(float damage) {
		float hearts = damage / 2;
		return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
	}
}
