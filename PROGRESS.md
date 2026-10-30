# Wildcatter: what is finished

Wildcatter is a Fabric mod for Minecraft 26.3 that works as a teaching tool for electrical work. It has a real circuit solver, three voltage tiers, safety devices, and power from steam, oil and gas. Everything below is implemented and exercised by the automated client game test (`./gradlew runClientGameTest`). The exceptions are noted at the end.

## Design rules
- Realism and teaching come first: voltages, amps, shocks, grounding and protection behave as they do in real life.
- Large objects are assembled from small parts: the multiblock rule. Examples are the 27-turbine MV generator, the 8-engine cube, the refinery tower and the cooling tower.

## Electrical grid
- **Circuit solver:** the grid is solved as a real circuit (modified nodal analysis). Wires join by role (hot or neutral) and voltage.
- **Voltage tiers:** 110 V, 4.16 kV and 138 kV.
- **Wires:** bare, wool (wears out), and plastic (never wears, safe to touch). There are hot and neutral versions at each tier.
- **Shocks:** touching a bare live conductor hurts. Bare MV and HV lines shock at a distance: 4.16 kV within 2 blocks, 138 kV within 4.
- **Shorts:** bare hot against bare neutral is a short circuit.
- **Grounding:** ground rods and ground wire, with "grounded / UNGROUNDED" shown on machines.
- **Transformers:** step between tiers, including the substation transformer.
- **Loads:** electric lamp and electric heater.
- **Power Meter:** right-click anything to read volts, amps, watts and status.
- **Field Manual:** an in-game book with a page for each system.

## Protection devices
| Device | Voltage | Notes |
|---|---|---|
| Circuit Breaker | 110 V | Ratings 15 / 20 / 30 / 50 A. Thermal trip on a mild overload, instant trip on a short. Resettable. |
| MV Breaker | 4.16 kV | Ratings 30 / 60 / 100 / 200 A. Blue. |
| HV Breaker | 138 kV | Ratings 100 / 200 / 400 / 600 A. Red. |
| Switch | 110 V | Yellow. Opens and closes the hot wire by hand and never trips. Protects nothing, so put a breaker or fuse before it. |
| Fuse | 110 V | One-shot and fast. Blows on a short and cannot be reset. Renew it with a copper ingot. |

All of them go in the hot wire: in at the back, out at the front. Sneak and right-click changes the rating.

## Power generation
- **Steam:**
  - A coal boiler (a multiblock with a firebox you load with fuel, or a hopper) feeds a steam turbine.
  - 27 turbines in a 3x3x3 cube make a 4.16 kV generator.
  - A pump and pipes bring water to the boilers. The turbine's drain returns condensate.
- **Cooling tower:**
  - A Cooling Tower Fan (110 V, 750 W) with 2 Cooling Tower Fill sections stacked on top, touching the water pipes.
  - Without a cooling tower, a turbine needs about 33% more steam for the same power.
  - The test confirmed the turbine reports "condensate cooled".
- **Diesel engine and gas turbine:**
  - Each is a 110 V set on its own.
  - Eight in a 2x2x2 cube make a 4.16 kV generator: 100 kW diesel, 250 kW gas.
  - They burn only the fuel the load uses.

## Oil and gas
- **Fields:** underground oil and gas fields come from the world seed. Wells on one field share its pressure and run down over time.
- **Seismograph:** an item that reports whether there is a field nearby and in which direction.
- **Drilling Rig:** a 110 V machine. It drills casing down and strikes the field, then becomes a **Pumpjack** (oil) or a **Gas Wellhead** (gas). A dry hole is reported.
- **Gas lines:** copper pipes that carry crude, diesel and gas between machines.
- **Refinery:**
  - A 110 V base is a slow pot still with a low diesel yield.
  - Two distillation columns on top make a full tower: faster, higher yield, and it also makes plastic sheets.
- **Fuel:** crude, diesel and gas buckets and lines feed fireboxes and engines.

## Utility poles
- **Utility Pole:** climbable. 3 poles from 3 logs.
- **Crossarm:** carries two insulators to hold the lines apart. 2 crossarms from planks plus an iron nugget.
- Both are structural and decorative. They do not conduct or enforce any support rule.

## Solver package
- `dev.wildcatter.solver` (`Solver`, `Network`) is plain Java with no Minecraft or Fabric imports. `circuit/Circuit` adapts block positions to it. `./gradlew build` fails if the package imports Minecraft.

## Phase 1a: phasor solver
- `solver/PhasorSolver` (+ `Complex`): AC steady-state complex nodal solver, any number of conductors, so balanced and unbalanced three-phase and split-phase are all just nodes. Sources and transformers carry impedance; inductors and capacitors follow frequency; banded LU after reverse Cuthill-McKee.
- `./gradlew test` runs 10 JUnit checks (Ohm, series/parallel, lagging PF, 3φ balanced and unbalanced, split-phase, Delta-Y +30°, power conservation, frequency). A 1,000-node three-phase network solves in about 0.4 ms (budget 5 ms).
- Not wired into the game yet: the blocks still use the real-valued `Solver`; wiring comes with 1c.
- Factorization caching is deferred: the budget is met without it.

## Phase 1b: electrical standard (part 1)
- `energy/ElectricalStandard` (North American 60 Hz or IEC 50 Hz) holds every voltage and the frequency; `Voltage` (LOW/MEDIUM/HIGH) reads its volts and label from the active standard. Low is 120 V or 230 V, medium 4.16 kV or 3.3 kV, high 138 kV or 132 kV. The distribution, generator-terminal and industrial/commercial low-voltage rows are in the table as data; their equipment, and a distribution tier, come in Phase 2.
- `energy/StandardData` stores the choice in the world and locks it at first load: the Create World choice if one was made, else `config/wildcatter.properties` (`standard=NORTH_AMERICAN` or `IEC`).
- Block names say LV / MV / HV; inspect text and the Field Manual print the active voltages.
- Game test: the same lamp runs on 230 V when the standard is switched to IEC and back to 120 V.
- Create World screen: a "Grid" button on the Game tab (under Allow Commands) picks North American or IEC. It is a client entrypoint (`client/WildcatterClient`) using Fabric's screen events, no mixins. The game test opens the real screen, checks the button, presses it and checks the choice changes. A real mouse click on it has not been exercised (the test harness's synthetic mouse did not reach the widget); the button is in the screen's children list and sits on the Game tab.
- A cancelled Create World leaves no choice behind (the Select World screen clears it). A world made before this change, or by a server, gets the config default.
- Not done yet: split-phase 120/240 V (comes with the conductor work in 1c).

## Phase 1e: steel pipe
- New **Steel Ingot**: iron ingot smelted in a blast furnace (coal is the fuel). **Steel Pipe** (block id `gas_line`, so existing worlds keep working) is crafted from steel ingots and replaces the copper gas line for crude, diesel and gas. The iron pipe is now called Water Pipe.
- New Ludo art for both; Field Manual text says steel pipe.
- Game test: the full oil, gas, refining and engine run still passes on the new pipe.

## Phase 1c: conductors and split-phase (single phase side)
- `Role` (hot/neutral) is now `Conductor` (A, B, C, N, G). A wire carries one conductor, chosen by right-clicking it with an empty hand; its band shows which (A red, B orange, C blue, N black, G green). Existing wires keep working (hot starts as A, neutral as N). G wires are tied to earth.
- North American worlds: a transformer's low side is centre-tapped, giving legs A and B 180 degrees apart (B on the transformer's side faces). Loads take A or B and measure against N; the shared neutral carries only the difference. IEC worlds have no leg B.
- Game test: a lamp on leg B lights, equal lamps on A and B leave the shared neutral at 0.0 A, and with only the A lamp it carries 0.5 A.
- Not done: wiring the phasor solver into the grid and three-phase (C), the three-pole breaker, a 240 V (A to B) load, and per-phase wear on cables. Wire textures are still script-drawn placeholders (not yet named `*_placeholder`).

## Phase 1d: dynamic frequency
- Each island of generators has one frequency (`solver/FrequencyIsland`, plain Java, unit tested): the swing equation with each set's inertia, a governor with 4% droop, and a slow secondary control (AGC) that brings it back to nominal. Load above the generators' rating drags it down; lost load lets it rise.
- Approved values (NA / IEC): 60 / 50 Hz; H 1 s diesel, 3 s steam; droop 4%; load shed at 59.3 / 49.0 Hz; generators trip below 57.0 / 47.5 Hz or above 61.5 / 51.5 Hz. Assumptions NOT in your table, easy to change in `Tuning`: gas turbine H = 3 s, AGC time constant 30 s, generator trip delay 1 s.
- New **Under-Frequency Relay (LV)**: an in-line breaker-style block that opens its feeder when the island frequency falls below the load-shed point. Right-click to close it again.
- Steam turbines and engines are `Generator`s: they report rating and output, trip on frequency (right-click to restart), and show the island frequency. Wires and relays show it too. Islands restart when the standard changes.
- Game test: a 2 kW turbine with a lamp and a 4.5 kW heater bank behind a relay sheds the heaters (island fell to 58.2 Hz), keeps running and recovers to 60.0 Hz; without the relay the turbine trips on under-frequency, restarts when the load is removed, and runs. The old overload-trips-the-breaker test now has a second turbine set, because a lone 2 kW set stalls before a breaker's heating can trip it.
- Not done: frequency effect on motors, MV feeder relays (Phase 2), reclosers, generator synchronisation.

## Phase 2a: distribution tier
- `Voltage.DISTRIBUTION` (12.47 kV NA / 11 kV IEC; a single-phase tap holds the line-to-neutral 7.2 kV). New: distribution wires (bare and plastic, selectable conductor), a distribution breaker (200/400/600/1200 A), and a distribution transformer (138 kV to 12.47 kV, a 20 MVA stand-in until the Phase 4 substation). Values and their real-world basis: `docs/phase2-distribution.md`.
- Game test: 120 V stepped up to 4.16 kV, 138 kV and down to a 12.47 kV line that holds 7,200 V.
- Next: 2b insulators and spans.

## Phase 2b: insulators and overhead spans
- **Insulators** (LV 1 kV, DIST 15 kV pin, HV 138 kV suspension) sit on top of a pole or crossarm (they pop off otherwise) and hold one conductor, chosen by right-click. **Conductor spools** (bare and covered, per tier) string a span between two insulators: right-click one, then the other. A spool pays out one block of its 256 per block of span.
- A span is a link stored on both insulators, solved with the circuit (the two ends are one island even far apart), with the tier's wire resistance per block, and drawn by a client renderer as a sagging line (`client/SpanRenderer`). Limits: 8 blocks at LV, 10 at DIST, 64 at HV (1 block = 4 m); both ends the same conductor; one kind of line per insulator; four spans at most. A span whose other end is gone is dropped. Bare spans shock like bare wire.
- A line above an insulator's rating gets a ground arc ("flashing over"); under load for 3 s the insulator shatters. Tested: the flag. The shattering depends on enough fault current, which a small generator cannot supply (it trips on frequency first), so that path is not exercised by the game test.
- Game test: a 9 block distribution span carries 7,200 V, a 16 block span is refused ("Too far"), insulators on different conductors are refused, and an under-rated insulator is flagged.
- Not done: span conductor ampacity (spans never burn), spans for the 4.16 kV tier (no MV spool), conductor sizes (one per tier).

## Phase 2c and 2d: pole transformer, fused cutout, service drop
- **Pole-Mount Transformer** (25 kVA, 12.47 kV (7.2 kV to neutral) to 120/240 V split-phase, or 230 V on IEC): hangs on a pole (one must stand beside it), feeder to its top (high side), leg A and neutral on the back and bottom, leg B on the sides. Loaded beyond 25 kVA it heats and at 150 % burns out in 10 s. Not done: the 2.5 % impedance (it uses the common winding model).
- **Fused Cutout** (DIST): an in-line fuse for the feeder with K-link ratings 6/10/15/25/40/65/100 A; it blows, its tube drops open, and a copper ingot fits a new link.
- **Service drop**: a low voltage span pair (hot and neutral) from the pole transformer to the house. Not done: tying the neutral to earth at the house (the transformer neutral is tied at the pole).
- Game test: feeder, cutout, pole transformer, a 7 block service drop and a house lamp that lights; the cutout blows (test shrinks its rating) and the house goes dark; a new link restores it.

## Phase 2e and 2f: receptacles, plug adapters, feeder relays
- **Receptacles and plug adapters** (NEMA 5-15, 5-20, 14-50; CEE 7; IEC 60309 blue): wire a receptacle, set the plug of the same type against it (it will not stay beside another type) and put an FE machine or cable beside the plug. The plug draws real current, as many watts as the attached machines demand (1 W = 1 FE/t, `wattsPerFe` in the config), has no protection, delivers nothing on the wrong voltage, and burns out with its receptacle after 30 s above 120 % of its rating. Two parts: `energy/EnergyBridge` + `HostEnergy` (plain Java) are the only meeting point with other mods.
- **Not done:** the Team Reborn Energy adapter (no confirmed 26.3 build; nothing is registered by default, so plugs deliver to no one until an adapter registers), and the three-phase plugs (Phase 3).
- **Under-Frequency Relay (DIST)** for 12.47 kV feeders: same behaviour as the low voltage relay.
- Game test: a plug hands 923 FE/t (92k FE in 100 ticks) to a stand-in machine; a CEE 7 plug is refused beside a NEMA receptacle and delivers nothing on 120 V; a plug above its rating burns with its receptacle; the feeder relay sheds a pole transformer's overload at 59.0 Hz and the turbine stays on.

## Phase 2 summary
Distribution tier, insulators and spans, pole transformer, fused cutout, service drop, receptacles and plugs, feeder relays are all built and covered by the game test. Known gaps: span ampacity, 4.16 kV spans, conductor sizes, transformer impedance, neutral earthing at the house, flashover shattering path untested, Team Reborn adapter.

## Tools
- `tools/process_art.py` and `tools/make_assets.py` generate textures, models, blockstates, lang, loot, tags and recipes. Run `process_art.py` first, then `make_assets.py`.
- `tools/recipe_sheet.py <dir>` renders every recipe as a crafting-grid picture.
- The client game test takes screenshots into `build/run/clientGameTest/screenshots/`.
- The test client needs the Mac display awake. Run `caffeinate -u -t 30` just before launching it.

## Known gaps
- The new art (cooling tower, fuse, breakers, poles) has been checked in test screenshots, not in a real play session.
- The pump and the pumpjack draw full power even when their tanks are full.
- Gas fields are large, so a gas well lasts hours.
- Breakers and fuses do not shock a player who touches them.
- There are no pole support rules and no pole-mounted transformers.

## Ideas not started
- Switches and panels.
- Better wire, ground and breaker art.
- Saving the Ludo sound sources.
- A Mekanism / NeoForge bridge.
