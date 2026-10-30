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

				"FIREBOX\n\nRight-click a Firebox to open it and add coal, charcoal or wood. A hopper can feed it from any side. Put it under a Boiler and it heats it like a campfire (" + Math.round(Tuning.BOILER_STEAM_PER_TICK * 20 * Tuning.JOULES_PER_STEAM / 1000) + " kW).\n\n"
						+ "It only burns while the boiler wants heat, so a full steam tank costs no fuel.\n\nNine Fireboxes in a 3x3 floor join into one BIG FIREBOX with one shared fuel store.",

				"BIG MACHINES\n\nCOIL BOILER: build a 3x3x3 cube of 27 Boilers on top of a 3x3 floor of 9 Fireboxes. It joins up by itself (you will hear it). "
						+ "Each burning firebox does a ninth of the work, so nine make " + (Tuning.BOILER_STEAM_PER_TICK * Tuning.BIG_SIZE * Tuning.BIG_SIZE * Tuning.BIG_SIZE * 20 * (int) Tuning.JOULES_PER_STEAM / 1000)
						+ " kW of steam. Fill any boiler of the cube with water: they share one big tank.\n\n"
						+ "MEDIUM VOLTAGE GENERATOR: a 3x3x3 cube of 27 Steam Turbines, touching the coil boiler, makes 4.16 kV. Wire to ANY face. Step it down with a transformer for 110 V.\n\nBreak one block and the machine falls apart.",

				"WATER\n\nPUMP: a 110 V motor (" + Math.round(Tuning.PUMP_WATTS) + " W, " + String.format("%.1f", Tuning.PUMP_WATTS / Voltage.LOW.nominal()) + " A). Put it touching water, wire a hot and a neutral to it, and run PIPES to your boilers. "
						+ "It fills every boiler on its pipes. Low voltage slows it: its delivery follows the square of the voltage.\n\n"
						+ "DRAIN: a pipe touching a Steam Turbine carries its used steam back as water to the boilers on the same pipes. " + Math.round(Tuning.CONDENSATE_RETURN * 100) + "% comes back, so a loop still needs the pump. With no pipe the water is lost.",

				"FINDING OIL\n\nOil and gas hide in pockets deep underground. Use the SEISMOGRAPH (right-click): it thumps the ground and tells you which way the nearest pocket lies, how far, and how deep its top is. "
						+ "It cannot tell oil from gas: only a drill finds that out.\n\nPockets are about " + (Tuning.FIELD_MIN_RADIUS * 2) + "-" + (Tuning.FIELD_MAX_RADIUS * 2) + " blocks across. Walk toward a stronger echo.",

				"DRILLING\n\nSet a DRILLING RIG on the ground over a pocket and wire it for 110 V (" + Math.round(Tuning.RIG_WATTS) + " W, " + String.format("%.0f", Tuning.RIG_WATTS / Voltage.LOW.nominal())
						+ " A: use a 20 A breaker and good wire). It sinks a steel-cased hole, a block a second at full voltage. Low voltage drills slowly.\n\n"
						+ "Reach the pocket and it STRIKES: the rig becomes a wellhead. Miss, and you get a DRY HOLE: break the rig and try again.",

				"OIL AND GAS WELLS\n\nGAS WELLHEAD: gas comes up by its own pressure, no power needed. Put a Firebox against it, or run GAS LINES (orange pipes) to fireboxes. It only opens when a firebox is low, so gas is only used as you burn it.\n\n"
						+ "PUMPJACK (oil): a " + Math.round(Tuning.PUMPJACK_WATTS) + " W, 110 V pump. Right-click it with a bucket for CRUDE OIL, or send it down gas lines to fireboxes. A bucket of crude burns as long as lava.\n\n"
						+ "Wells on the same pocket share it, and the PRESSURE drops as it empties: the flow slows. Read it with the Power Meter.",

				"REFINING\n\nREFINERY BASE: a " + Math.round(Tuning.REFINERY_WATTS) + " W, 110 V unit. Pour in a bucket of crude, or put it on a GAS LINE with a pumpjack. Alone it is a POT STILL: slow, and only " + Math.round(Tuning.POT_STILL_YIELD * 100)
						+ "% of the crude comes out as diesel.\n\nStack " + Tuning.REFINERY_COLUMNS + " DISTILLATION COLUMNS on top and it becomes a full TOWER: twice the speed, " + Math.round(Tuning.DIESEL_YIELD * 100)
						+ "% diesel, and PLASTIC (a sheet per " + Tuning.CRUDE_PER_PLASTIC + " crude). Right-click with an empty hand for the plastic, with a bucket for diesel.",

				"ENGINES\n\nDIESEL ENGINE and GAS TURBINE: small 110 V generators. Put eight of one kind in a 2x2x2 cube and they join into one 4.16 kV GENERATOR: " + Math.round(Tuning.DIESEL_ENGINE_WATTS * 8 / 1000) + " kW diesel, "
						+ Math.round(Tuning.GAS_TURBINE_WATTS * 8 / 1000) + " kW gas turbine.\n\nThey burn only the fuel the load uses. Diesel: pour a bucket in, or run a gas line to a refinery. Gas: run a gas line to a gas wellhead. With no fuel they make nothing. Step the 4.16 kV down with a transformer.",

				"PLASTIC WIRE\n\nCraft wire with plastic sheets: it never wears out, at any voltage. Plastic hot and neutral wire are safe to touch, and a plastic 4.16 kV or 138 kV line has no arc reach: you can run it at ground level.\n\nIt carries no more current than bare wire of the same voltage: the rating is the copper.",

				"HIGH VOLTAGE\n\nBREAKERS come for 4.16 kV (ratings " + java.util.Arrays.toString(Tuning.BREAKER_AMPS_MEDIUM) + " A) and 138 kV (" + java.util.Arrays.toString(Tuning.BREAKER_AMPS_HIGH)
						+ " A). They work like the 110 V one: in the hot wire, in at the back, out at the front. A 100 kW generator at 4.16 kV makes only 24 A, so size the breaker to the line, and put it at the generator.\n\n"
						+ "POLES: stack UTILITY POLES and put a CROSSARM on top. Its insulators hold the lines apart. A bare 4.16 kV line shocks within " + (int) Tuning.BARE_REACH_MEDIUM + " blocks, 138 kV within " + (int) Tuning.BARE_REACH_HIGH
						+ ": hang them higher than that above where anyone walks. Poles can be climbed, which puts you next to the line. Or use plastic-insulated line.",

				"SWITCH\n\nA SWITCH opens and closes a hot wire by hand, for servicing: open it before you work on a line. It has no protection and never trips, so a switch alone will not save you from an overload. Put a breaker or fuse ahead of it. (A switch also does not make a line safe by itself: a worker still ground-tests the wire.)",

				"FUSES\n\nA FUSE is the cheap cousin of the breaker: a strip of metal that melts when too much current flows. It goes in the hot wire like a breaker (in at the back, out at the front), blows faster, and cannot be reset: fix the fault, then right-click it with a copper ingot to fit a new element. Sneak and right-click picks its rating. Use one to protect a branch you do not want to rely on a breaker for.",

				"COOLING TOWER\n\nA steam turbine hands its waste heat to the condensate in its drain. Without cooling, back pressure means it needs " + Math.round(100 / Tuning.UNCOOLED_EFFICIENCY - 100) + "% more steam for the same power.\n\nBuild a tower from small parts: put a COOLING FAN (" + Math.round(Tuning.COOLING_FAN_WATTS) + " W, 110 V) where it touches your water pipes, and stack " + Tuning.COOLING_FILL + " COOLING FILL sections on top. It only cools while the fan has power. Click the turbine to see if it is cooled.",

				"COMING\n\nFuses, switches and panels.\n\nThis book will grow as they do."
		);
	}

	private static String hearts(float damage) {
		float hearts = damage / 2;
		return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
	}
}
