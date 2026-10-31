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

				"6. WIRE RATINGS\n\nEvery wire has a maximum current set by its SIZE: " + (int) dev.wildcatter.energy.ConductorSize.AWG_12.amps() + " A for the 12 AWG wire you start with. Past it the wire heats up. At twice the rating it burns out in about "
						+ (int) Tuning.WIRE_BURN_SECONDS_AT_2X + " seconds.\n\nA breaker protects the WIRE. Never fit a breaker rated higher than the wire it guards.\n\n"
						+ "Wire also has resistance: on a long run the voltage drops.",

				"7. SHORT CIRCUITS\n\nIf the hot and neutral touch with no load between them, the current is limited only by the wire: hundreds of amps. "
						+ "That is a short circuit.\n\nBare wires of opposite colour touching each other make one. A breaker trips at once. Without one, the wire burns out.",

				"8. WOOL INSULATION\n\nWool-wrapped wire is safe to touch and cannot short to a neighbour. But the wool wears: about "
						+ (int) (Tuning.WOOL_LIFE_TICKS / 24000) + " days of use, faster under heavy current, and " + (int) Tuning.WOOL_WET_WEAR
						+ "x faster in rain or water.\n\nWhen it is worn out it is bare wire again: it shocks and it can short. Check it. Replace it.\n\nBetter insulation needs plastic.",

				"9. VOLTAGE LEVELS\n\n" + Voltage.LOW.label() + ": lamps and small machines.\n" + Voltage.MEDIUM.label() + ": bigger generators and local lines.\n" + Voltage.HIGH.label() + ": long hauls.\n\n"
						+ "Wires only join at the same voltage. A TRANSFORMER joins two levels: high-voltage wires go on its front and top, low-voltage wires on its back and bottom (one hot, one neutral on each side). Higher voltage means fewer amps for the same power, which is why long lines use it. "
						+ "Windings lose a little to heat.",

				"10. SHOCKS\n\nBare live wire shocks anything near it. " + Voltage.LOW.label() + ": you must touch it. " + Voltage.MEDIUM.label() + ": " + (int) Tuning.BARE_REACH_MEDIUM + " blocks. " + Voltage.HIGH.label() + ": "
						+ (int) Tuning.BARE_REACH_HIGH + " blocks, so keep bare lines up on tall poles.\n\n" + Voltage.LOW.label() + ": " + hearts(Tuning.SHOCK_DAMAGE_LOW) + " heart. " + Voltage.MEDIUM.label() + ": "
						+ hearts(Tuning.SHOCK_DAMAGE_MEDIUM) + " hearts. " + Voltage.HIGH.label() + ": " + hearts(Tuning.SHOCK_DAMAGE_HIGH) + ".\nDouble if you are wet.",

				"11. GROUNDING\n\nPut a Ground Rod in the earth against a machine, or run Ground Wire from the machine to a rod. A grounded machine is safe to touch.\n\n"
						+ "An ungrounded machine with power in it may bite when you touch it: " + Math.round(Tuning.TOUCH_SHOCK_CHANCE_LOW * 100) + "% at " + Voltage.LOW.label() + ", "
						+ Math.round(Tuning.TOUCH_SHOCK_CHANCE_MEDIUM * 100) + "% at " + Voltage.MEDIUM.label() + ", always at " + Voltage.HIGH.label() + ".",

				"12. POWER METER\n\nRight-click a wire, machine or breaker to read its volts, amps and watts, how worn the wool is, and whether a machine is grounded. "
						+ "It also says why a machine has no power.\n\nThe meter is insulated: safe on live equipment.",

				"FIREBOX\n\nRight-click a Firebox to open it and add coal, charcoal or wood. A hopper can feed it from any side. Put it under a Boiler and it heats it like a campfire (" + Math.round(Tuning.BOILER_STEAM_PER_TICK * 20 * Tuning.JOULES_PER_STEAM / 1000) + " kW).\n\n"
						+ "It only burns while the boiler wants heat, so a full steam tank costs no fuel.",

				"STEAM PLANT\n\nA " + Math.round(Tuning.PLANT_WATTS / 1000) + " kW, " + Voltage.MEDIUM.label() + " plant built from parts at their real size: 6 x 7 m and 4 m high, every part on the ground. Facing north: BURNER, BOILER and SUPERHEATER in the back row; GENERATOR and TURBINE in the middle row, the turbine under the superheater; FEED PUMP and CONDENSER in the front row, the condenser beside the turbine. The generator's readout names the next part it needs and where.\n\n"
						+ "Feed the burner coal (a coal block is a cubic metre: one coal is " + Math.round(Tuning.COAL_JOULES / 1e9 * 10) / 10.0 + " GJ, about a quarter hour at full power) or pipe gas to it. Right-click the generator to light it: the drum takes about " + Math.round(Tuning.PLANT_HEATUP_SECONDS / 60) + " minutes to reach pressure.\n\n"
						+ "The FEED PUMP is a " + Voltage.INDUSTRIAL.label() + " motor, wired at the box on its side: the plant needs power to start (a genset will do). The generator's cables connect at its end away from the turbine. Pipe water to the condenser for makeup, and put a COOLING TOWER's basin on that pipe, or the plant makes only three quarters of its rating. Its output goes through a GENERATOR STEP-UP transformer (delta to wye, the line side 30 degrees ahead) to " + Voltage.DISTRIBUTION.label() + ".",

				"FULL-SIZE EQUIPMENT\n\nBig equipment is its real size: one block is a metre. Hold one and a green box shows where it will go (red if something is in the way); it faces away from you. Break any part of it and the whole thing comes down as one item.\n\n"
						+ "GENERATOR STEP-UP: 3 m across, 2 m deep, 3 m to the bushing tops. The " + Voltage.MEDIUM.label() + " cable goes into the box low on the back, in the middle; the " + Voltage.DISTRIBUTION.label() + " leaves from the top of the three brown bushings: a cable on top of them, or beside them at the front.",

				"WATER\n\nPUMP: a " + Voltage.LOW.label() + " motor (" + Math.round(Tuning.PUMP_WATTS) + " W, " + String.format("%.1f", Tuning.PUMP_WATTS / Voltage.LOW.nominal()) + " A). Put it touching water, wire a hot and a neutral to it, and run PIPES to your boilers. "
						+ "It fills every boiler on its pipes. Low voltage slows it: its delivery follows the square of the voltage.\n\n"
						+ "DRAIN: a pipe touching a Steam Turbine carries its used steam back as water to the boilers on the same pipes. " + Math.round(Tuning.CONDENSATE_RETURN * 100) + "% comes back, so a loop still needs the pump. With no pipe the water is lost.",

				"FINDING OIL\n\nOil and gas hide in pockets deep underground. Use the SEISMOGRAPH (right-click): it thumps the ground and tells you which way the nearest pocket lies, how far, and how deep its top is. "
						+ "It cannot tell oil from gas: only a drill finds that out.\n\nPockets are about " + (Tuning.FIELD_MIN_RADIUS * 2) + "-" + (Tuning.FIELD_MAX_RADIUS * 2) + " blocks across. Walk toward a stronger echo.",

				"DRILLING\n\nSet a DRILLING RIG on the ground over a pocket and feed it " + Voltage.INDUSTRIAL.label() + " three-phase: its drive is a " + Math.round(Tuning.RIG_WATTS / 1000) + " kW motor (about " + Math.round(Tuning.RIG_WATTS / (3 * Voltage.INDUSTRIAL.nominal() * Tuning.MOTOR_POWER_FACTOR)) + " A a phase; use a 60 A three-pole breaker and three-phase cable). It sinks a steel-cased hole, a block a second at full voltage. Low voltage drills slowly.\n\n"
						+ "Reach the pocket and it STRIKES: the rig becomes a wellhead. Miss, and you get a DRY HOLE: break the rig and try again.",

				"OIL AND GAS WELLS\n\nGAS WELLHEAD: gas comes up by its own pressure, no power needed. Put a Firebox against it, or run STEEL PIPES (the dark pipes, crafted from STEEL INGOTS: smelt iron ingots in a blast furnace) to fireboxes. It only opens when a firebox is low, so gas is only used as you burn it.\n\n"
						+ "PUMPJACK (oil): a " + String.format("%.1f", Tuning.PUMPJACK_WATTS / 1000) + " kW, " + Voltage.INDUSTRIAL.label() + " three-phase motor that runs only while its tank has room. Right-click it with a bucket for CRUDE OIL, or send it down steel pipes to fireboxes. A bucket of crude burns as long as lava.\n\n"
						+ "Wells on the same pocket share it, and the PRESSURE drops as it empties: the flow slows. Read it with the Power Meter.",

				"REFINING\n\nREFINERY BASE: a " + Math.round(Tuning.REFINERY_WATTS) + " W, " + Voltage.LOW.label() + " unit. Pour in a bucket of crude, or put it on a STEEL PIPE with a pumpjack. Alone it is a POT STILL: slow, and only " + Math.round(Tuning.POT_STILL_YIELD * 100)
						+ "% of the crude comes out as diesel.\n\nStack " + Tuning.REFINERY_COLUMNS + " DISTILLATION COLUMNS on top and it becomes a full TOWER: twice the speed, " + Math.round(Tuning.DIESEL_YIELD * 100)
						+ "% diesel, and PLASTIC (a sheet per " + Tuning.CRUDE_PER_PLASTIC + " crude). Right-click with an empty hand for the plastic, with a bucket for diesel.",

				"GENSETS\n\nA genset is an engine and an alternator on one skid, built from parts you craft: an ENGINE (block casting, pistons, crankshaft, cylinder head and injectors, or spark plugs for gas) and an ALTERNATOR (stator winding, rotor field, exciter). Everything is its real size: the diesel set is 5 x 2 m and 4 m high, the gas set 7 x 2 m.\n\nPut down the base first: the SUB-BASE FUEL TANK (diesel) or the BASE FRAME (gas). Then click each part anywhere on top of it: RADIATOR, ENGINE, ALTERNATOR, CONTROL PANEL. Each goes to its own place on the skid. Click the SILENCER on top of the engine.\n\nDiesel " + Math.round(Tuning.GENSET_DIESEL_WATTS / 1000) + " kW, gas " + Math.round(Tuning.GENSET_GAS_WATTS / 1000) + " kW, both " + Voltage.INDUSTRIAL.label() + " three-phase. The cables connect low on the panel's end, away from the engine. Right-click the panel to start: it cranks to speed in " + (int) Tuning.GENSET_START_SECONDS + " s and closes its breaker. Diesel: a bucket on any part of the tank (1,000 L) or a pipe from a refinery. Gas: a pipe from a wellhead to any part of the engine.",
				"SUBSTATION\n\nA yard of true-size parts, 16 x 9 m, " + Voltage.HIGH.label() + " side (west when facing north) first: " + Voltage.HIGH.label() + " BREAKER, DISCONNECT and BUS, the POWER TRANSFORMER (" + Math.round(Tuning.SUBSTATION_VA / 1e6) + " MVA, delta to wye), then the " + Voltage.DISTRIBUTION.label() + " BUS, DISCONNECT and FEEDER BREAKER, with the RELAY PANEL north of the transformer. Every part's terminals sit on the yard's centre line, so placed in their spots they touch. Nothing snaps: the transformer's readout names the next part it needs and where. The transformer is energized only once the line is complete. A CHAIN-LINK FENCE with a BARBED-WIRE TOP keeps people out.\n\nThe relay panel trips both breakers on a fault: a time-overcurrent relay (51) above " + Math.round(Tuning.RELAY_51_PICKUP * 100) + " percent of full load, sooner the heavier it is, and an instantaneous one (50) at " + Math.round(Tuning.RELAY_50_MULTIPLE * 100) + " percent. Right-click the panel to reset. Open a DISCONNECT only after its breaker: under load it draws an arc that burns anyone near.",
				"SYNCHRONIZING\n\nA set closes its breaker at once onto a dead bus. Onto a live one it must match the bus first: the same voltage, the same frequency and the same phase angle. In AUTO (the default) the panel does it, running a hair fast and closing when it is within " + (int) Tuning.SYNC_MAX_DEGREES + " degrees. In MANUAL (sneak and right-click the stopped panel) you watch the SYNCHROSCOPE on the panel, sneak and right-click to trim the speed, and right-click to close. Close it out of step and the bus slams the alternator into step: the breaker trips with a bang, and a bad enough close wrecks the alternator. Sets on one bus share the load.",

				"PLASTIC WIRE\n\nCraft wire with plastic sheets: it never wears out, at any voltage. Plastic hot and neutral wire are safe to touch, and a plastic " + Voltage.MEDIUM.label() + " or " + Voltage.HIGH.label() + " line has no arc reach: you can run it at ground level.\n\nIt carries no more current than bare wire of the same voltage: the rating is the copper.",

				"HIGH VOLTAGE\n\nBREAKERS come for " + Voltage.MEDIUM.label() + " (ratings " + java.util.Arrays.toString(Tuning.BREAKER_AMPS_MEDIUM) + " A) and " + Voltage.HIGH.label() + " (" + java.util.Arrays.toString(Tuning.BREAKER_AMPS_HIGH)
						+ " A). They work like the " + Voltage.LOW.label() + " one: in the hot wire, in at the back, out at the front. A 100 kW generator at " + Voltage.MEDIUM.label() + " makes only 24 A, so size the breaker to the line, and put it at the generator.\n\n"
						+ "POLES: a WOOD POLE stands its real height (see POLES AND ARMS) and carries a CROSSARM. Its insulators hold the lines apart. A bare " + Voltage.MEDIUM.label() + " line shocks within " + (int) Tuning.BARE_REACH_MEDIUM + " blocks, " + Voltage.HIGH.label() + " within " + (int) Tuning.BARE_REACH_HIGH
						+ ": hang them higher than that above where anyone walks. Poles can be climbed, which puts you next to the line. Or use plastic-insulated line.",

				"SWITCH\n\nA SWITCH opens and closes a hot wire by hand, for servicing: open it before you work on a line. It has no protection and never trips, so a switch alone will not save you from an overload. Put a breaker or fuse ahead of it. (A switch also does not make a line safe by itself: a worker still ground-tests the wire.)",

				"DISTRIBUTION\n\nBetween the " + Voltage.HIGH.label() + " transmission line and the houses sits the distribution voltage, " + Voltage.DISTRIBUTION.label() + ": feeders that run along the roads to the pole transformers. A single-phase tap of the feeder carries line to neutral, " + Math.round(Voltage.DISTRIBUTION.nominal()) + " V, which is the " + Voltage.DISTRIBUTION.label() + " divided by 1.73.\n\nA SUBSTATION steps " + Voltage.HIGH.label() + " down to " + Voltage.DISTRIBUTION.label() + " (see SUBSTATION); a small plant's GENERATOR STEP-UP puts its " + Voltage.MEDIUM.label() + " straight onto it. DISTRIBUTION line is rated by its conductor (4/0 ACSR carries " + (int) dev.wildcatter.energy.ConductorSize.ACSR_PENGUIN.amps() + " A) and a bare line shocks within " + (int) Tuning.BARE_REACH_DISTRIBUTION + " blocks. Feeder breakers: " + java.util.Arrays.toString(Tuning.BREAKER_AMPS_DISTRIBUTION) + " A.",

				"OVERHEAD SPANS\n\nOverhead lines are not wire blocks: they are SPANS strung between INSULATORS. Put an insulator on a crossarm or a pole top (a " + Voltage.HIGH.label() + " string hangs under an H-frame's crossbeam), right-click it with an empty hand to choose its conductor (A, B, C, N or G), then right-click one insulator and the next with a CONDUCTOR SPOOL. The spool pays out one block of its length per block of span.\n\nA block of span is " + (int) Tuning.SPAN_METERS_PER_BLOCK + " m. Longest spans: " + Tuning.SPAN_MAX_LOW + " blocks at " + Voltage.LOW.label() + ", " + Tuning.SPAN_MAX_DISTRIBUTION + " at " + Voltage.DISTRIBUTION.label() + ", " + Tuning.SPAN_MAX_HIGH + " at " + Voltage.HIGH.label() + ": put another pole between. Both ends must carry the same conductor. The line sags about 2.5 % of its span.\n\nInsulators are rated: LV 1 kV, DIST 15 kV, HV 138 kV. A line above an insulator's rating FLASHES OVER: it arcs to earth, trips protection, and the insulator shatters. A bare span shocks like bare wire; a COVERED spool makes a plastic-covered span that is safe to touch.",

				"POLES AND ARMS\n\nPoles are true size, one item each: a 30 ft WOOD POLE stands 8 m (service drops), a 40 ft one 10 m (" + Voltage.DISTRIBUTION.label() + " feeders), a 75 ft one 20 m. Click a CROSSARM onto a pole's side near its top: it bolts on just under the top, across the line, and a pin insulator goes on each end. A third insulator sits on top of the pole, a little higher. Clicked lower, an arm goes where you click (for secondaries).\n\nA " + Voltage.HIGH.label() + " line rides an H-FRAME: two 75 ft poles 4 m apart, a CROSSBEAM clicked onto their tops, an X-BRACE below, and three insulator strings hanging 4 m apart. Click a POLE TRANSFORMER onto a pole and it hangs on it. Break a pole and its arms and insulators come down with it.",

				"POLE-MOUNT TRANSFORMER\n\nThe last step before a house: a 25 kVA transformer that hangs on a pole (it must stand beside one) and steps the " + Voltage.DISTRIBUTION.label() + " feeder down to " + Voltage.LOW.label() + ". Bring the feeder to its TOP (the high side); the low side is the back, with leg A and the neutral, and the two sides carry leg B (North American worlds: 120/240 V split-phase).\n\nIt is rated: the power on the secondary legs together must stay under 25 kVA. Above that it heats, and at 150 percent it burns out in " + (int) Tuning.POLE_TRANSFORMER_FAIL_SECONDS + " seconds. Click it to read its loading.",

				"FUSED CUTOUT\n\nThe pole-top fuse of a " + Voltage.DISTRIBUTION.label() + " feeder: put it in the hot line (in at the back, out at the front) ahead of a pole transformer so a fault takes out the one transformer and not the whole feeder. K-link ratings: " + java.util.Arrays.toString(Tuning.CUTOUT_K_LINKS) + " A (sneak and right-click to choose). When it blows the tube drops open; right-click it with a copper ingot to fit a new link.\n\nA cutout is BARE: its contacts sit at line voltage, and touching a live one is a full shock, grounded or not (linemen use a hot stick). Open the line upstream before you touch it. Breakers, starters and the MCC are enclosed and safe to handle.\n\nSERVICE DROP: from the pole transformer's low side, run wire up to a low voltage insulator, string a conductor spool to an insulator at the house (at most " + Tuning.SPAN_MAX_LOW + " blocks), and bring wire down to the loads. The transformer neutral is tied to earth at the pole.",

				"RECEPTACLES AND PLUGS\n\nPower leaves the grid only through a RECEPTACLE and a PLUG ADAPTER. Wire the receptacle like any load point (leg A or B and the neutral), set the plug adapter of the SAME TYPE against it (it will not stay beside any other), and put an FE machine or cable of another mod beside the plug.\n\nThe plug draws real current: the machines' demand in watts (1 W for each FE per tick, set in the config), no more than the circuit gives. It has no protection of its own: if the machines pull too much, the breaker or fuse upstream must trip. Held above its amp rating for " + (int) Tuning.RECEPTACLE_BURN_SECONDS + " seconds the plug and receptacle burn out. On the wrong voltage a plug delivers nothing.\n\nTypes: NEMA 5-15 (120 V 15 A), 5-20 (120 V 20 A), 14-50 (240 V 50 A, both legs), CEE 7 (230 V 16 A), IEC 60309 blue (230 V 32 A). Three-phase: NEMA L21-30 (208Y/120 V 30 A, on the low-voltage tier), L22-30 (480Y/277 V 30 A), IEC 60309 red (400 V, 32 A or 63 A): wire them with three-phase cable.",

				"FREQUENCY\n\nEvery island of generators shares one frequency (" + (int) dev.wildcatter.energy.ElectricalStandard.active().hertz() + " Hz on this world). Load that is larger than the generators can supply pulls it DOWN; spare generation lets it rise. Heavy spinning machines (steam turbines, large engines) change it slowly, light ones quickly. A governor opens the valve as the frequency falls (droop " + Math.round(Tuning.GOVERNOR_DROOP * 100) + "%), and a slow secondary control then brings it back to nominal.\n\nUNDER-FREQUENCY RELAY: put it in the hot wire of a feeder. If the frequency falls below " + dev.wildcatter.energy.ElectricalStandard.active().loadShedHertz() + " Hz it opens, shedding that load so the generators survive. A generator whose island stays outside " + dev.wildcatter.energy.ElectricalStandard.active().generatorLowHertz() + " to " + dev.wildcatter.energy.ElectricalStandard.active().generatorHighHertz() + " Hz for " + (int) Tuning.GENERATOR_TRIP_SECONDS + " s trips off: fix the overload and right-click it to restart. Wires, generators and relays show the island frequency.",

				"FUSES\n\nA FUSE is the cheap cousin of the breaker: a strip of metal that melts when too much current flows. It goes in the hot wire like a breaker (in at the back, out at the front), blows faster, and cannot be reset: fix the fault, then right-click it with a copper ingot to fit a new element. Sneak and right-click picks its rating. Use one to protect a branch you do not want to rely on a breaker for.",

				"COOLING TOWER\n\nA steam turbine hands its waste heat to the condensate in its drain. Without cooling, back pressure means it needs " + Math.round(100 / Tuning.UNCOOLED_EFFICIENCY - 100) + "% more steam for the same power.\n\nA tower cell is 4 x 4 m and 6 m high. Put down the COLD-WATER BASIN (your water pipes join it), then click the COOLING FILL, DRIFT ELIMINATORS and FAN DECK on top of it: each stacks into place. The 3 m fan has a " + Math.round(Tuning.COOLING_FAN_WATTS / 100) / 10.0 + " kW " + Voltage.INDUSTRIAL.label() + " three-phase motor; run a cable up to the front of the fan deck. It only cools while the fan has power; you can watch it turn. Click the turbine to see if it is cooled.",

				"CONDUCTORS\n\nA wire carries ONE conductor, shown by its coloured band: A red (the hot of a single-phase circuit), B orange, C blue, N black (neutral), G green (ground). Right-click a wire with an empty hand to change which conductor it carries. A circuit needs its conductors running out to the load and back.\n\nSPLIT-PHASE (North American worlds): a transformer's low side is centre-tapped. A and B are the two legs, 180 degrees apart. Each leg is " + Voltage.LOW.label() + " to the neutral N, and A to B is double that. A load on leg A and a load on leg B share the neutral, and it carries only the DIFFERENCE between them: equal loads leave it empty. IEC worlds have a single phase and no leg B.\n\nThe ground conductor G is the earth itself: a bare wire carrying another conductor that touches it is a ground fault.",

				"CONDUCTOR SIZES\n\nWire comes in sizes, each its own item. The bigger the conductor, the more current it carries and the less voltage it loses over a long run:\n12 AWG 20 A, 10 AWG 30 A, 6 AWG 65 A, 2 AWG 115 A, 4/0 230 A, 500 kcmil 380 A (copper building wire, 120 V to 480 V). An 800 A BUSWAY carries a genset's output.\n\nAbove 480 V lines are ACSR (aluminium on a steel core): #2 180 A, 1/0 240 A, 4/0 340 A, 336 kcmil 530 A, 795 kcmil 900 A. Heavy conductors sag more, so their spans are shorter. Craft a bigger size from smaller wire of the same kind; the breaker must never be rated above the wire it guards.",
				"THREE-PHASE\n\nGenerators make three phases, A, B and C, a third of a turn (120 degrees) apart, plus the neutral N. Each phase is " + Voltage.LOW.label() + " to the neutral; between two phases it is 1.73 times that (" + Math.round(Voltage.LOW.lineToLine()) + " V).\n\nSpread single-phase loads over A, B and C: when the three are equal, their currents cancel in the neutral and it carries nothing. A lamp or a one-pole breaker takes whichever ONE phase reaches it.\n\nThe big transformers are three-phase. Voltages above " + Voltage.LOW.label() + " are named line to line (4.16 kV, 12.47 kV, 138 kV); a wire shows its line-to-neutral volts too.\n\nA big three-phase generator (a " + Voltage.MEDIUM.label() + " cube) shares its rating among its three phases: each stator phase carries only a THIRD of it. (A lone " + Voltage.LOW.label() + " engine or turbine is a small single-phase set: any one phase may take all of it.) Pile the load on one phase and that winding overheats (" + Math.round(Tuning.STATOR_I2T_SECONDS / 0.69) + " s at 130 percent, about 12 s at twice its share) and the machine TRIPS. Right-click it to restart once the load is spread out.",
				"480 V AND CABLE\n\nIndustry runs on " + Voltage.INDUSTRIAL.label() + " three-phase (" + Math.round(Voltage.INDUSTRIAL.nominal()) + " V to neutral). A PAD-MOUNT transformer steps the " + Voltage.DISTRIBUTION.label() + " feeder down to it (300 kVA), or to 208Y/120 V for shops (75 kVA). A UNIT SUBSTATION takes it from " + Voltage.MEDIUM.label() + " in a plant (1,000 kVA). Both are true size: a pad-mount fills 2 x 2 x 2 m, its high side on the left as you face its doors and its low side on the right, each at the bottom front cell's outer side or underneath; the unit substation is a 4 m lineup, " + Voltage.MEDIUM.label() + " at one end and " + Voltage.INDUSTRIAL.label() + " at the other.\n\nTHREE-PHASE CABLE carries A, B, C, N and G in one insulated block: lay it like wire. A single wire touching a cable takes the one conductor it carries, so a cable can split into three phase wires (that is how a 138 kV line leaves a substation transformer for three insulators: HIGH-VOLTAGE CABLE off its bushings, then a riser per phase). A THREE-POLE BREAKER sits in the run and opens all three phases at once when any one of them faults.\n\nThe THREE-PHASE HEATER (15 kW) needs all of A, B and C.",
				"MOTORS\n\nPumps, fans, pumpjacks, rigs and the refinery run on induction motors. A motor draws nothing while its machine has no work. Starting, it pulls " + Math.round(Tuning.MOTOR_LOCKED_ROTOR_MULTIPLE) + " times its running current for " + Math.round(Tuning.MOTOR_START_SECONDS) + " seconds (the inrush), then settles at a power factor of " + Tuning.MOTOR_POWER_FACTOR + ".\n\nA three-phase motor turns the way its phases come round. Swapped phases run it BACKWARD: sneak and right-click it with an empty hand to swap two leads at its terminal box. Lose one phase and a running motor STALLS, drawing its starting current until something trips.",
				"MOTOR CONTROL\n\nA MOTOR STARTER sits between the 480 V line (its back) and one motor (its front). Right-click or a redstone signal starts and stops it. Inside: a motor circuit protector that opens at once on a short (" + Math.round(Tuning.MCP_MULTIPLE) + " times the setting), a contactor, and an OVERLOAD RELAY. Set its dial (sneak and right-click) to the motor's full-load amps; it trips on a sustained overload, within " + Math.round(Tuning.OVERLOAD_CLASS_SECONDS) + " s at six times the setting, never at or below 115 %.\n\nA MOTOR CONTROL CENTER is an INCOMING SECTION (main breaker, fed on its back or top) with up to " + Tuning.MCC_MAX_BUCKETS + " BUCKETS in a line to its right, all facing the same way. Each bucket is a starter for one motor: its cable leaves from the bucket's back or top. A gap in the line ends the bus.",
				"COMING\n\nFuses, switches and panels.\n\nThis book will grow as they do."
		);
	}

	private static String hearts(float damage) {
		float hearts = damage / 2;
		return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
	}
}
