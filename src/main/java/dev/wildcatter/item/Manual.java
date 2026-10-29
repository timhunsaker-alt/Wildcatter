package dev.wildcatter.item;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
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
		double volts = Voltage.LOW.nominal();
		return List.of(
				"WILDCATTER\nField Manual\n\nPower from steam, gas and oil, built the way the real grid is: volts, amps, breakers and all.\n\nRead the safety pages before you touch anything.",

				"1. STEAM\n\nPut a Boiler on a lit campfire. Fill it with water buckets (" + Tuning.WATER_PER_BUCKET + " each). "
						+ "Put a Steam Turbine touching it.\n\nOne campfire makes " + Math.round(Tuning.BOILER_STEAM_PER_TICK * 20 * Tuning.JOULES_PER_STEAM / 1000)
						+ " kW. Four buckets last about "
						+ (Tuning.BOILER_WATER_CAPACITY * Tuning.STEAM_PER_WATER / Tuning.BOILER_STEAM_PER_TICK / 20 / 60) + " minutes.\n\nThe turbine only burns steam for the power you actually use.",

				"2. THE LOOP\n\nElectricity needs a loop. Power leaves on a HOT wire (red) and comes back on a NEUTRAL wire (black). "
						+ "Every machine has to be joined to both.\n\nOne wire is a dead end: nothing flows and the lamp stays dark.",

				"3. OHM'S LAW\n\nVolts = Amps x Ohms\nWatts = Volts x Amps\n\nA " + Math.round(Tuning.LAMP_WATTS) + " W lamp on " + Math.round(volts)
						+ " V draws " + String.format("%.2f", Tuning.LAMP_WATTS / volts) + " A. A " + Math.round(Tuning.HEATER_WATTS) + " W heater draws "
						+ String.format("%.1f", Tuning.HEATER_WATTS / volts) + " A.\n\nRight-click anything with the Power Meter to see its real volts and amps.",

				"4. ADDING LOADS\n\nMachines wired across the same two wires are in parallel. Their amps ADD UP.\n\nTwo heaters: "
						+ String.format("%.1f", 2 * Tuning.HEATER_WATTS / volts) + " A. Add enough machines and the current passes what the wire "
						+ "and the breaker can take.",

				"5. BREAKERS\n\nPut a Breaker in the HOT wire: wire in at the back, out at the front. It opens the circuit when the current is too high.\n\n"
						+ "Overload: a little over its rating heats it until it trips; the further over, the faster.\nShort: " + (int) Tuning.BREAKER_MAGNETIC_MULTIPLE
						+ "x its rating trips it at once.\n\nRight-click: off/on, and reset. Sneak and right-click: change its rating.\n\nA breaker only guards what comes AFTER it. Put it first, right at the generator.",

				"6. WIRE RATINGS\n\nEvery wire has a maximum current: " + (int) Tuning.WIRE_AMPS_LOW + " A at 110 V. Past it the wire heats up. At twice the rating it burns out in about "
						+ (int) Tuning.WIRE_BURN_SECONDS_AT_2X + " seconds.\n\nA breaker protects the WIRE. Never fit a breaker rated higher than the wire it guards.\n\n"
						+ "Wire also has resistance: on a long run the voltage drops.",

				"7. SHORT CIRCUITS\n\nIf the hot and neutral touch with no load between them, the current is limited only by the wire: hundreds of amps. "
						+ "That is a short circuit.\n\nBare wires of opposite colour touching each other make one. A breaker trips at once. Without one, the wire burns out.",

				"8. WOOL INSULATION\n\nWool-wrapped wire is safe to touch and cannot short to a neighbour. But the wool wears: about "
						+ (int) (Tuning.WOOL_LIFE_TICKS / 24000) + " days of use, faster under heavy current, and " + (int) Tuning.WOOL_WET_WEAR
						+ "x faster in rain or water.\n\nWhen it is worn out it is bare wire again: it shocks and it can short. Check it. Replace it.\n\nBetter insulation needs plastic.",

				"9. VOLTAGE LEVELS\n\n110 V: lamps and small machines.\n4.16 kV: bigger generators and local lines.\n138 kV: long hauls.\n\n"
						+ "Wires only join at the same voltage. A TRANSFORMER joins two levels: high-voltage wires go on its front and top, low-voltage wires on its back and bottom (one hot, one neutral on each side). Higher voltage means fewer amps for the same power, which is why long lines use it. "
						+ "Windings lose a little to heat.",

				"10. SHOCKS\n\nBare live wire shocks anything near it. 110 V: you must touch it. 4.16 kV: " + (int) Tuning.BARE_REACH_MEDIUM + " blocks. 138 kV: "
						+ (int) Tuning.BARE_REACH_HIGH + " blocks, so keep bare lines up on tall poles.\n\n110 V: " + hearts(Tuning.SHOCK_DAMAGE_LOW) + " heart. 4.16 kV: "
						+ hearts(Tuning.SHOCK_DAMAGE_MEDIUM) + " hearts. 138 kV: " + hearts(Tuning.SHOCK_DAMAGE_HIGH) + ".\nDouble if you are wet.",

				"11. GROUNDING\n\nPut a Ground Rod in the earth against a machine, or run Ground Wire from the machine to a rod. A grounded machine is safe to touch.\n\n"
						+ "An ungrounded machine with power in it may bite when you touch it: " + Math.round(Tuning.TOUCH_SHOCK_CHANCE_LOW * 100) + "% at 110 V, "
						+ Math.round(Tuning.TOUCH_SHOCK_CHANCE_MEDIUM * 100) + "% at 4.16 kV, always at 138 kV.",

				"12. POWER METER\n\nRight-click a wire, machine or breaker to read its volts, amps and watts, how worn the wool is, and whether a machine is grounded. "
						+ "It also says why a machine has no power.\n\nThe meter is insulated: safe on live equipment.",

				"COMING\n\nPlastic insulation.\nGas and diesel turbines.\nDrilling rigs and wells.\nRefining.\nFuses, switches and panels.\n\nThis book will grow as they do."
		);
	}

	private static String hearts(float damage) {
		float hearts = damage / 2;
		return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
	}
}
