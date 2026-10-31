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
- **Gensets (Phase 4b; they replace the 120 V diesel engine, gas turbine and the 8-engine cube):**
  - A diesel set is 500 kW and a gas set 1 MW, both 480Y/277 V three-phase (400Y/230 V on IEC worlds), built as a skid of distinct parts (see Phase 4b).

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
- **Not done:** the Team Reborn Energy adapter (no confirmed 26.3 build; nothing is registered by default, so plugs deliver to no one until an adapter registers). The three-phase plugs came in Phase 3e.
- **Under-Frequency Relay (DIST)** for 12.47 kV feeders: same behaviour as the low voltage relay.
- Game test: a plug hands 923 FE/t (92k FE in 100 ticks) to a stand-in machine; a CEE 7 plug is refused beside a NEMA receptacle and delivers nothing on 120 V; a plug above its rating burns with its receptacle; the feeder relay sheds a pole transformer's overload at 59.0 Hz and the turbine stays on.

## Phase 2 summary
Distribution tier, insulators and spans, pole transformer, fused cutout, service drop, receptacles and plugs, feeder relays are all built and covered by the game test. Known gaps: span ampacity, 4.16 kV spans, conductor sizes, transformer impedance, neutral earthing at the house, flashover shattering path untested, Team Reborn adapter.

## Phase 3a: phasor solver in the blocks (no commit yet; held until after Tim's workday)
- Every block now solves on the complex phasor solver (`solver/Network` builds a `PhasorSolver`; the old real-valued `Solver` is gone). The whole Phase 0 to 2 game test passes unchanged on it.
- **Generators are three-phase:** A, B and C at 0, -120 and +120 degrees, N grounded (`circuit/ThreePhaseSource`). Each phase can carry the full rating on its own; the governor and the frequency limit the total.
- **Three-phase transformers:** the substation, distribution and (on IEC worlds) the 120/230 V to 4.16 kV transformers are grounded Y-Y banks. The pole transformer and the North American split-phase units stay single-phase and take whichever phase they are hung on.
- Transformer windings are now one series impedance (resistance plus the nameplate leakage reactance; the pole unit's 2.5 % is modelled).
- **Every tier's voltage is line to neutral** (4.16 kV is 2.40 kV to neutral, 138 kV is 79.7 kV). Wires show both, e.g. "4.16 kV, 2402 V to neutral".
- A single-phase load or a one-pole breaker takes **whichever one phase** reaches it, never two (`Circuit.hotPort`), so a lamp against a three-phase generator no longer bridges phases.
- Island frequency nets the generators' power, so current circulating between machines at start-up is not counted as load.
- Game test: a lamp on each phase lights, the neutral carries ~0 A balanced and one lamp's current with a lamp removed; a 1,032-node network solves in 0.98 ms (budget 5 ms). Unit tests: `NetworkTest` (6 three-phase tests).

## Phase 3b and 3c: 480 V tier, three-phase cable, three-pole breakers, pad-mounts
- **Industrial tier** (`Voltage.INDUSTRIAL`): 480Y/277 V on North American worlds, 400Y/230 V on IEC. Bare and plastic 480 V wire (0.01 ohm per block, 100 A; shock 2 hearts, reach 0.5 blocks: proposed values, see `docs/phase3-three-phase.md`).
- **Three-phase cable** (`cable_lv`, `cable_industrial`, `cable_dist`): one insulated block carrying A, B, C, N and G. It joins another cable or a three-phase machine on all five conductors and a single wire only on the one that wire carries. It never shorts. Each conductor heats on its own current.
- **Three-pole breakers** (`breaker_3p_lv` 15/20/30/50 A, `breaker_3p_industrial` 20/30/60/100/225/400 A): one handle opens A, B and C together. It trips on the largest phase current, and N and G pass straight through.
- **Pad-mount transformers** `padmount_208` (12.47 kV to 208Y/120 V, 75 kVA, 3 %) and `padmount_480` (12.47 kV to 480Y/277 V, 300 kVA, 5 %), and the **unit substation** (4.16 kV to 480Y/277 V, 1,000 kVA, 5.75 %). All are three-phase grounded Y-Y and heat on overload.
- Rated transformers now take their resistance from the nameplate %Z (X/R 5), not the old fixed 0.2 ohm windings, so a fully loaded pole transformer no longer sags far below its rating.
- **Three-phase heater** (480 V, 15 kW, delta elements): a plain three-phase load to test the tier with.
- Two transformers placed face to face join on all three phases. That is how three-phase reaches 138 kV today, because there is no HV cable or three-conductor overhead line yet.
- Game test: a 4.16 kV diesel set feeds a substation and distribution transformer (face to face), then 12.47 kV cable, a 480 V pad-mount, a three-pole breaker and cable to the heater. The heater runs at 476 V between each pair of phases (14.8 kW), the cable carries 17.9 A on each phase and 0.0 A on N, and the unit substation runs a second heater. A phase-to-phase short trips the three-pole breaker at 2,272 A.

## Phase 3d: motors
- `circuit/Motor`: an induction motor. Running, each phase draws full-load current at PF 0.8. Starting, it draws 6 times that at PF 0.35 for 2 s. Stopped, it draws nothing. A three-phase motor is a floating star across A, B, C, and a single-phase motor sits between whichever phase reaches it and N.
- **Pumpjack (7.5 kW) and drilling rig (30 kW) are now 480 V three-phase motors.** On the old 120 V wiring they say "NO POWER: it needs 480 V three-phase". The pumpjack runs only while there is oil and tank room; the rig only while drilling.
- **Water pump, cooling-tower fan and refinery are 120 V single-phase motors** at their old watts. The pump has a level switch: it starts below 80 % and stops when the boilers are full. The refinery runs only with crude and room for its products. The fan runs once the tower is built.
- **Phase rotation:** the motor reads the order of its phases. Sneak and right-click a pumpjack or rig with an empty hand to swap two leads at its terminal box. With leads swapped it runs backward and the pumpjack lifts nothing.
- **Single-phasing:** with one phase lost, a running motor stalls and draws locked-rotor current. Its own overload relay comes with the motor starter (3f).
- Game test: the wells scene now feeds the rig and pumpjack from a diesel set, a unit substation and a 480 V cable pole. The motors scene feeds a pumpjack from three separate plastic wires: it pumps at 11.2 A (479 V, 7.5 kW), stops drawing with its tank full, runs backward with leads swapped, and stalls at 58 A with phase C cut.

## Phase 3e: three-phase plugs
- NEMA L21-30 (208Y/120 V, 30 A, low-voltage tier), NEMA L22-30 (480Y/277 V, 30 A), IEC 60309 red 32 A and 63 A (400 V). Each takes A, B, C and N, and the plug draws a balanced three-phase load equal to the FE its machines pull.
- Fixed: a single-line receptacle (5-15, 5-20, CEE 7, IEC blue) now takes whichever one phase reaches it. Before, against a three-phase source it would have bridged A and B.
- Game test: an L22-30 plug on the unit substation's cable hands over 9,903 W (9,903 FE/t, 12.0 A at 478 V), and an L21-30 plug does not fit its receptacle.

## Phase 3f: motor starter and motor control center
- **Motor starter** (`motor_starter`): a combination starter in a 480 V three-phase run, with the line on its back and the motor on its front. It holds a motor circuit protector (opens at once at 13 times the overload setting, the NEC 430.52 limit), a contactor (right-click or redstone starts and stops it) and a class 10 overload relay. The relay's full-load-amps dial runs 1.5 to 63 A (sneak and right-click), defaults to 11 A, never trips at or below 115 %, trips within 10 s at 6 times the setting, and takes a couple of minutes at twice it.
- **Motor control center** (`docs/multiblocks/motor-control-center.md`): `mcc_incoming` (the controller, with a main three-pole breaker fed on its back or top) and up to 6 `mcc_bucket` starters in a line to its right, all facing the same way. The buckets share a hidden bus that nothing else can connect to. A bucket's motor cable leaves from its back or top. The incoming section's readout lists every bucket.
- Game test: an incoming section fed by a unit substation on top of it, plus three buckets. Bucket 1 runs a pumpjack at 11.3 A. Bucket 3's pumpjack is wired with only two phases: it cannot start, stalls at 57.9 A, and bucket 3's overload relay trips while bucket 1 keeps running. Taking out the middle bucket cuts bucket 3 off the bus.

## Phase 3 summary
Every block now runs on the AC phasor solver. Generators and the big transformers are three-phase. There is a 480 V industrial tier with cable, three-pole breakers, pad-mounts and a unit substation. Pumps, fans, the refinery, pumpjacks and rigs are induction motors that draw only while working, with inrush, phase rotation and single-phasing. Three-phase plugs, a motor starter and a motor control center are in, and everything is covered by the game test (1,032 nodes solve in about 1 ms). Values added while building (industrial wire and shock, the three-phase heater, the overload dial and MCP multiple, transformer X/R, the pump level switch) were approved by Tim on 2026-10-30.

## Phase 3 gap fill
- **High-voltage cable** (`cable_hv`, 138 kV / IEC 132 kV): A, B, C, N and G in one block, the same resistance and ampacity as 138 kV wire. It carries all three phases off a substation transformer's bushings and splits into a riser per phase, so a **138 kV three-phase overhead line** on three insulators now works. Face-to-face transformers are no longer the only way.
- **Fused cutouts shock.** Their contacts are bare and at line voltage, so a hand on a live cutout (right-click, a copper ingot, or a hit) is a full distribution shock, grounded or not. A blown cutout's top contact is still live. Enclosed fuses, breakers, starters and the MCC stay safe to touch.
- **Generator stator limit** (4.16 kV cubes; the 480 V gensets will get it in Phase 4): each phase is rated for a third of the machine's kVA (rated kW / 0.8). Above that the winding heats on the IEEE C50.13 curve, (I^2 - 1) t = 37.5 s, and the machine trips "on stator overcurrent". The readout shows the hottest phase and its heating. A lone 120 V turbine or engine stands in for a small single-phase set and has no per-phase limit (Tim, 2026-10-30).
- **Real art** for the Phase 3 blocks: Ludo drawings for the cable jacket (tinted per tier), the three-pole breaker case (orange for 480 V), pad-mount, unit substation, three-phase heater, and the starter/MCC door (pilot lamp green, grey or red by state).
- Game test, new scenes: a standalone motor starter (stopped 0 A; the button runs it at 11.2 A; redstone starts and stops it; tripped at 3 A by overload; ignores redstone once tripped; reset by hand). A 138 kV overhead line with 79.4 kV on each phase, and the far heater at 478 V between each pair of phases. A live cutout touch leaves 8 of 20 health and a dead one leaves full health. Three heaters on phase A of a diesel cube trip it on stator overcurrent at 1.7x, not on frequency; spread over A, B and C, they run.

## Phase 4.1: conductor sizes
- Wire comes in sizes, each its own item: the size rides on the stack (a `wildcatter:conductor_size` component), goes into the placed block, and comes back when it is broken. The block-state count does not grow.
  - Building wire and three-phase cable (120/208 V and 480 V): 12, 10, 6 and 2 AWG, 4/0 and 500 kcmil (NEC 310.16 at 75 C).
  - Overhead and MV wire: ACSR #2 Sparrow to 795 kcmil Drake.
  - 12.47 kV and 138 kV cable: 1/0, 4/0 and 500 kcmil MV cable.
  - **800 A busway** (`busway_lv`, `busway_industrial`).
- **Resistance is real**, per metre (NEC Chapter 9 Table 8), with one block = 1 m and one span block = 4 m. The old fixed per-tier wire values are gone. Heavier ACSR spans are shorter (4/0 -10 %, 336 -20 %, 795 -30 %).
- Recipes: the smallest size keeps the old recipe; a bigger size is crafted from 2 or 3 of the next smaller one; plastic, wool, cable and spools keep the size they are made from (Fabric component ingredients). 304 recipe files are generated; the recipe sheets show the smallest of each and three upsizing chains.
- Old worlds: a wire, cable or span saved before sizes takes the size nearest its old rating.
- A **Wildcatter creative tab** lists every machine, then every conductor in every size grouped by tier. The Phase 3 items were missing from the old tab and are now in it.
- Game test: a heater 40 m out gets 108 V on 12 AWG and 113 V on 6 AWG. A cable block carrying 89 A holds as 2 AWG (115 A) and heats and burns as 6 AWG (65 A). A 10-block 1/0 ACSR distribution span strings, and 4/0 is refused ("9 blocks at most"). An unsized wire reads 12 AWG.

## Phase 4a and 4b: foundations and gensets
- **Foundations:**
  - `multiblock/MultiblockLayout` is a spec grid in plain Java, turned to four facings and unit-tested. `multiblock/Multiblock` matches it in the world and names the first missing part.
  - `kinetic/Kinetic`, `kinetic/Spin` and `client/KineticRenderer` draw rotor models (`rotor` block states: radiator fan, crank pulley, coupling) turning at real rpm / 20. The server sends speed only when it changes by more than 2 %.
  - `kinetic/Sounding` and `client/MachineSound` play looping machine sounds: pitch follows speed, volume follows load, and they fade out when the machine stops. Ludo sources are now kept in `art/sound_source/`.
- **Gensets** (docs/multiblocks/genset.md):
  - **Crafted parts:** engine block casting, pistons, crankshaft, cylinder head, and fuel injectors (diesel) or spark plugs (gas) make the engine. A stator winding, rotor field and exciter make the alternator.
  - **The skid:** radiator, engine, alternator and control panel in a line, with the sub-base fuel tank under the engine and the exhaust stack on top. The panel is the controller.
  - **Starting:** the set cranks to 1,800 rpm (1,500 at 50 Hz) in 10 s, then closes its output breaker.
  - **Fuel:** 15 % of full-load fuel at no load, rising to 130 L/h diesel or 265 m3/h gas at full load. The tank takes a diesel bucket (1,000 L) or diesel piped from a refinery; the gas engine takes gas piped from a wellhead.
  - **Limits:** the Phase 3 stator limit applies at 625 or 1,250 kVA.
  - **What moves and sounds:** the radiator fan, crank pulley and coupling turn, and exhaust smoke follows load. Sounds: diesel idle and load loops, a gas engine loop, alternator whine, fan, and an engine crank one-shot.
  - **Art:** Ludo textures for all parts and icons for every crafted part. The old engine drawing now dresses the genset engine.
- The old `diesel_engine` and `gas_turbine` blocks and the 8-engine cube are removed (Tim: remove now). Every game-test scene that used a cube now uses a genset; where 4.16 kV is needed, a unit substation runs backward.
- Game test: a diesel set forms and runs at 1,800 rpm carrying a 15 kW heater, and the client sees the engine turning at about 1,800 rpm. A dry tank stops it. Taking the radiator away dissolves it. A gas set forms, and one on a gas well runs on well gas.

## Phase 4c: synchronizing
- With its breaker open, a running set reads the bus through potential transformers (1 MOhm to earth on each phase). It closes at once onto a dead bus.
- Onto a live bus, AUTO trims to +0.05 Hz fast and closes inside the sync-check window: 10 degrees, 0.1 Hz, 5 % volts.
- MANUAL: sneak + right-click steps the speed trim (-0.2 to +0.2 Hz), and right-click closes the breaker. The readout is a synchroscope, e.g. "0.08 Hz fast, 34 degrees ahead". Sneak + right-click while stopped switches AUTO and MANUAL.
- A close outside the window is a fault of 2 sin(theta/2) / X''d per unit (X''d 0.15). The breaker trips with a bang, and above 3 per unit the alternator is wrecked and must be replaced (break it and place a new one).
- Sets on one bus share load by governor droop.

## Phase 4d: steam plant, cooling tower and GSU
- **1 MW steam plant** (docs/multiblocks/steam-plant.md), one of each part along the steam path: burner under the drum, feed pump under the superheater, condenser under the turbine; drum, superheater, turbine and turbine generator (the controller) above.
  - The burner takes coal (3.5 GJ each; a coal block is 1 m3), charcoal or coal blocks by hand or hopper, or pipeline gas.
  - Lit, the drum rises to pressure in 5 minutes at full fire. At pressure, with water, the turbine rolls up to 3,600 rpm in 30 s, synchronizes (same synchronizer as the gensets) and makes 4.16 kV.
  - The feed pump is a 3.7 kW 480 V three-phase motor, so the plant needs station service to start (a genset black-starts it). It returns condensate from the hotwell to the drum; a low drum trips the plant. The water pump sends makeup water to a condenser on its pipe.
  - Without a cooling tower on the condenser's water the plant makes at most three quarters of its rating.
  - Turning parts: the turbine coupling and the feed pump coupling. Sounds: turbine whine, burner roar, motor hum.
- **Cooling tower** rebuilt as the spec's column: basin, fill, drift eliminators, fan deck on top. The fan turns.
- **GSU** (`gsu_transformer`): 4.16 kV delta / 12.47 kV grounded wye, 1,500 kVA, 5.75 %Z. The line side leads by 30 degrees (shown on the meter); unit-tested in `NetworkTest`.
- **Removed:** the coil boiler, the firebox floor and the 27-turbine cube (and their FORMED states). The campfire boiler, firebox and 120 V turbine stay as the starter plant.
- Game test: the plant forms cold; lit, the drum rises; at pressure but short of water and with no station service it waits; a genset black-starts the feed pump; it rolls up, closes onto the dead bus at 3,600 rpm, and through the GSU (+30 degrees) and a pad-mount runs a heater. It is "NOT COOLED" until a cooling tower goes on the condenser's water. Taking the superheater out dissolves it.

## Phase 4e: substation
- **Distribution substation** (docs/multiblocks/substation.md): 138 kV breaker, disconnect, bus, power transformer (138 kV delta / 12.47 kV wye, 20 MVA, 8 %Z), bus, disconnect, feeder breaker, and the relay panel. Energized only while built.
- The relay panel trips both (relay-operated, three-pole) breakers on a time-overcurrent relay (51, IEEE moderately inverse, pickup 150 % of full load) or an instantaneous one (50, 800 %), and locks out until reset at the panel.
- Disconnect switches swing their blades open (a moving part); opening one under load draws an arc.
- **Removed:** the single-block `distribution_transformer`. Scenes that only needed 12.47 kV from the starter plant now use a 75 kVA pad-mount run backward; the industrial scene uses the GSU; the 138 kV line scene ends in a real substation.
- Game test: the substation forms at the end of the 138 kV line with its high side 30 degrees ahead and runs the far heater; a feeder fault trips both breakers by relay; a reset brings the feeder back; a disconnect opened after its breaker opens without an arc.

## Phase 4f: sounds
- Looping sounds for the pump, pumpjack, drilling rig (motor hum), refinery (fan) and the starter turbine (turbine whine), kept in step with the client by `kinetic/RunningSound`. One-shots: fuse pop, insulator flashover, power-breaker bang (4.16 kV and up), disconnect arc. All from Ludo, sources in `art/sound_source/`.

## Phase 4.5a: true-size parts, the model tool, proved on the GSU
- **Big parts** (`multiblock/Footprint`, `BigPart`, `BigParts`, `BigPartEntity`): one item that, placed, fills its real size. Each cell is the same block with a `piece` number; piece 0 holds the block entity.
  - **Placing:** placed in front of the player only if every cell is free, with a green or red ghost outline (`client/GhostOutline`, drawn as a gizmo) before the click.
  - **Breaking:** breaking any piece breaks the whole part and drops one item, and only piece 0 has loot.
  - **Clicks and wires:** a click on any piece reads the part, and cables find terminals on any piece (`CircuitPart.portAt`, `hasPortAt`). Circuit grouping walks every cell of a big part.
  - **Collision:** each piece uses the real shape from the model tool.
  - **Older worlds:** a one-block part grows into its cells if there is room, and otherwise drops as an item with a chat line saying where.
- **The model tool** (`tools/models/mlib.py`, `build.py`, one script per part):
  - Shapes are written in metres: boxes, many-sided cylinders (boxes turned to any angle, which 26.3 allows), flanges, bolt circles, NPS pipe, fins, ladders, handrails and decals.
  - It cuts a part into its cells and writes the piece models, blockstate, a shrunken item model, collision shapes, and a preview in `build/model-previews/`.
  - It refuses a box outside −16..32 of its cell or a face with no texture. `ModelLintTest` re-checks the output.
  - `make_assets.py` runs it last.
- **Materials:** 16 Ludo material swatches and 3 decals (nameplate, DANGER HIGH VOLTAGE, gauge), processed by `tools/process_materials.py` to 32 px per metre.
  - The processor takes the cleanest window of each picture, because Ludo often draws a grid of tiles, and cross-fades the edges so the swatch tiles.
  - The plan said 16 px per metre. The mod already used 32 px per block everywhere, so 32 was kept.
- **GSU at true size**, 3 × 2 × 3 m, with 409 boxes:
  - a tank on skid channels, with panel radiators on both sides
  - three 15 kV porcelain bushings with sheds, plus an H0 neutral bushing and its strap
  - a 4.16 kV cable throat on the back
  - gauges, nameplate, danger signs, a drain valve, the pressure relief device, lifting lugs and ground pads
  - Terminals: 4.16 kV on the back of the bottom-middle cell; 12.47 kV on the top and front of the cell over the bushings.
- **Game test `bigParts`:** places a GSU like a player and checks the ghost outline, all 18 cells, a read through a corner piece and the bushing terminals. It takes three screenshots. Breaking one piece must leave one item and no cells. A blocked cell must refuse the placement. A lone GSU must drop when there is no room and grow when there is. `industrial` and `steamPlant` now run through the true-size GSU.

## Phase 4.5b: gensets at true size
- **Skids at real size** (docs/multiblocks/genset.md):
  - The 500 kW diesel set is 5 × 2 × 4: sub-base tank, radiator, engine, alternator, control panel and silencer.
  - The 1 MW gas set is 7 × 2 × 4: base frame, radiator, V20 engine, alternator, control panel and large silencer.
  - Every part is one item that fills its real size. New items: the genset base frame and the large silencer.
  - The approved height of 3 became 4, because the silencer sits above the 3 m radiator.
- **Building** (`block/GensetSkid`, `BigPart.snap`, `item/BigPartItem`):
  - Place the base first. A part clicked anywhere on it, or on a part already on it, goes to its slot and turns to the base's way.
  - The ghost outline shows the slot.
  - The panel matches each part's main cell and facing, and names a part that is missing or turned the wrong way.
- **Terminals:**
  - The 480 V output is on the panel's east end face, on both lower cells, instead of every face but the front.
  - Diesel fills the tank from any of its cells, by bucket or pipe. The gas line reaches the engine through any of its cells.
- **Older worlds:** a Phase 4 one-block genset part drops as an item with a chat line instead of growing (`BigPart.growsOnLoad`), because the layout changed. A one-block tank's diesel is lost.
- **Models** (`tools/models/`):
  - One script per part: `fuel_tank`, `genset_base_frame`, `radiator`, `genset_diesel_engine`, `genset_gas_engine`, `alternator`, `genset_control_panel`, `silencer` (both sizes). From 89 to 652 boxes each.
  - Turning parts drawn at real size (`rotors.py`): the 1.2 m radiator fan, the crank pulley, the flywheel coupling behind the adapter's guard windows, and a new alternator cooling fan.
  - New shapes in `mlib`: `ring` (hoops, shrouds, ribs, windows) and `fan`.
- **Decals:** two more from Ludo, a generic controller display and an engine data plate (1 credit). The exhaust smoke now leaves the silencer's tail pipe.
- **Game tests:**
  - **Moved scenes:** all 12 scenes that build a genset now build true-size skids through `GensetSkid.build`.
  - **Rebuilt scenes:** `industrial`, `motors` and the MCC scene turn their sets to face north, so the cable leaves the panel's end face. The MCC set stands beside the incoming section rather than on it. In the wells scene the wellhead touches one of the engine's far cells.
  - **New scene `gensetParts`:**
    - builds a diesel skid by player clicks; the engine, clicked off its slot, snaps in
    - forms the set
    - fills the tank with a bucket on its far corner
    - names an alternator turned the wrong way
    - runs both sets, with screenshots from three sides
    - breaks one engine piece: one engine item drops and the set dissolves
    - drops an older world's one-block radiator
  - **New scene `gensetFrameRate`:** ten running diesel sets. The client held its 120 fps cap with them in view.

## Phase 4.5c: steam plant and cooling tower at true size
- **The 1 MW steam plant** (docs/multiblocks/steam-plant.md) is 6 × 7 m and 4 high, every part on the ground:
  - burner 1 × 3 × 3
  - D-type boiler 4 × 3 × 4 with the steam drum along the top
  - superheater and stack 1 × 3 × 4
  - generator 3 × 2 × 2 (the controller)
  - turbine 2 × 2 × 2, right under the superheater's steam line
  - side-exhaust condenser 3 × 2 × 2 beside the turbine (Tim's choice)
  - feed pump 2 × 1 × 1 against the condenser's hotwell
  The plant has no base to snap to; the generator's readout names the next missing or wrongly turned part and its x y z.
- **Terminals:**
  - The generator's 4.16 kV terminals are on its non-drive end, both lower cells.
  - The feed pump's 480 V terminal box is on the motor's south side.
  - The burner takes coal on any cell and gas through any cell; the condenser takes its water pipe on any cell.
- **The cooling tower cell** (docs/multiblocks/cooling-tower.md) is 4 × 4 m and 6 high: concrete basin, PVC fill behind air-inlet louvres, drift eliminators with the hot-water header, and the fan deck.
  - The 3 m fan turns in its stack. The deck has the handrail, the ladder, the motor and gearbox, and the disconnect.
  - Parts clicked on top of the basin snap into the column.
  - The fan motor is now **7.5 kW 480 V three-phase**, with its terminals at the front of the deck, as Tim chose. The 120 V starter turbine can no longer run a tower.
- **Shared code:**
  - `block/PartLayout` generalises the genset skid's snapping and checking. Instances: `GensetSkid`, `TowerColumn`, `SteamPlantLayout`.
  - `block/BigPartBlock` is a common base for true-size part blocks.
  - `block/BigPartBlockEntity` (type `big_part`) serves plain parts that only remember they have grown.
  - `Plumbing.machines` finds a big machine through any of its cells.
  - `Motor.build` can put its terminals on chosen faces.
- **Models:** 11 scripts (77 to 484 boxes each) and three new turning parts: the 3 m tower fan, the turbine coupling and the feed pump's jaw coupling.
- **Old worlds:** Phase 4 one-block plant and tower parts drop as items with a chat line.
- **Game tests:**
  - `steamPlant` is rebuilt on the true-size plant:
    - the GSU sits on the generator's end
    - a genset black-starts the pump and feeds the tower fan
    - a true-size tower on the condenser's water cools the plant
    - five screenshots
  - The starter loop keeps its "NO COOLING TOWER" check.
  - New scene `towerParts`:
    - the column is built by clicks
    - a missing eliminator is named
    - one fill piece breaks the whole fill and drops one
    - an old one-block condenser drops

## Phase 4.5d: the substation yard at true size
- **The yard** (docs/multiblocks/substation.md) is 16 × 9 m and 7 high, every part on the ground along the centre line:
  - 138 kV dead-tank breaker 2 × 5 × 9, centre-break disconnect 2 × 6 × 9 (six arms swing open), 138 kV bus 2 × 6 × 9
  - 20 MVA power transformer 6 × 7 × 5 (the controller), with radiators, conservator, arresters and cabinets
  - 12.47 kV bus 1 × 4 × 3, vertical-break disconnect 1 × 5 × 3 (three blades lift), feeder breaker 2 × 4 × 3
  - relay panel 2 × 2 × 1 north of the transformer
  The yard has nothing to snap to; the transformer's readout names the next missing or wrongly turned part and its x y z.
- **Terminals** are on each part's west and east faces at the centre line's bottom cell, so parts in their spots touch.
- **Fence (decoration):** chain-link fence and barbed-wire top blocks, see-through, joining like iron bars.
- **Shared code:** `BigBreakerBlock`, `BigDisconnectBlock`, `StationBusBlock` (a 2,000 A station bus), `SubstationLayout`,
  `BigPart.terminalCell` and `BigParts.lineTerminal`.
- **Old worlds:** Phase 4 one-block yard parts drop as items with a chat line.
- **Game tests:** `overheadThreePhase` ends in the true-size yard (relay trip, reset, disconnects opened after their
  breakers); new scene `substationParts` (placed by clicks, a missing bus named, one disconnect piece breaks it whole).
- This section and the Field Manual's substation page were missed in the 4.5d commit and added with 4.5e.

## Phase 4.5e: the overhead line at true size
- **Span scale** is 1 m per block (Tim's choice), with the same limits in metres: 32 m low voltage, 40 m at 12.47 kV (36 m
  for 4/0 ACSR), 256 m at 138 kV; a spool holds 1,024 m; the drawn sag is 2.5 % of the span.
- **Poles** (docs/multiblocks/poles.md), one item each: 30 ft (8 m), 40 ft (10 m) and 75 ft (20 m) creosoted wood poles
  with steps, brand tag and ground wire moulding, climbable.
  - **Crossarm**, 8 ft fir on two braces: clicked on a pole within 2 m of its top it bolts on 0.3 m below the top, across
    the line; lower, where clicked. It fills the cells either side of the pole (the first footprint with a gap).
  - **H-frame** for 138 kV: two 75 ft poles 4 m apart, a 9 m crossbeam clicked onto their tops, an X-brace below.
  - Hardware needs its poles: break a pole and its arm, crossbeam or brace drops, and their insulators with them.
- **Insulators** now know their mount: on an arm, on a pole-top pin (0.3 m higher), or a 138 kV string of nine bells
  hanging 1.5 m from a crossbeam. The span is drawn, and its shock reach measured, from where each conductor really sits.
- **Pole transformer and fused cutout** are drawn at true size in their cell; a pole transformer clicked onto a pole hangs
  with its brackets on it.
- **Pad-mounts** (75 and 300 kVA) are 2 × 2 × 2 and the **unit substation** a 4 × 3 × 2 lineup (docs/multiblocks/padmount.md),
  with terminals at the bottom front cells; an older world's one-block unit grows into its cells where there is room.
- **Old worlds:** the one-block pole and crossarm keep working with their spans, but are no longer crafted or in the
  creative tab. Spans strung before keep their length in blocks, so their resistance is now a quarter.
- **Shared code:** `Footprint.of` (footprints with gaps), `PoleHardwareBlock`, `BigTransformerBlock` (the GSU now extends
  it), `PadmountBlock`, `InsulatorBlock.Mount`; the model tool draws a box across a footprint's gap from a neighbouring cell.
- **Models:** 14 scripts plus `linelib` and `padlib`; Ludo creosote and fir wood textures (1 credit).
- **Game tests:** `spans`, `serviceDrop`, `conductorSizes`, `refining` and `overheadThreePhase` (two H-frames) are strung
  on the new poles; every pad-mount and unit-substation scene is rewired to the new terminals; new scene `lineParts`.

## Tools
- `tools/process_art.py` and `tools/make_assets.py` generate textures, models, blockstates, lang, loot, tags and recipes. Run `process_art.py` first, then `make_assets.py`.
- `tools/recipe_sheet.py <dir>` renders every recipe as a crafting-grid picture.
- `tools/process_materials.py` makes the model materials, and `tools/models/build.py [part]` builds the true-size part models and previews (make_assets runs it).
- The client game test takes screenshots into `build/run/clientGameTest/screenshots/`.
- The test client needs the Mac display awake. Run `caffeinate -u -t 30` just before launching it.

## Known gaps
- Phase 4.5: the GSU, gensets, steam plant, cooling tower, substation yard and overhead line are true size. The building gear (breakers, MCC, receptacles, loads), the starter plant and the oil field are still one block per part until waves 4.5f and 4.5g.
- The pole transformer and cutout are wired as before (wire blocks to their faces); there are no jumper or riser leads drawn from an insulator down to them.
- A pole transformer placed by command keeps its old facing rule, so in some test scenes its brackets face away from the pole.
- Breaking a pole does not take down a pole transformer hung on it (it stays, as before).
- There are no guy wires.
- The steam plant's main steam line and feedwater line are drawn only within each part: they meet at the parts' edges, and the feedwater line stops at the boiler.
- A hopper feeds the burner only through its main cell.
- The frame-rate check runs at the client's 120 fps cap, so it shows no drop but not how much headroom is left.
- The alternator is drawn 1 m long for both sets; a real 1 MW frame is about 1.6 m.
- The old genset `*_placeholder` textures are still generated (the rotor texture is still used by the tower fan and disconnect blades).
- The brass swatch keeps a faint tile line.
- The 4.16 kV to 138 kV step-up is still the single-block `substation_transformer` (Y-Y, unrated windings) until bigger plants (Phase 6).
- The test skips the steam plant's five-minute warm-up with a hook (it checks the drum starts rising, then jumps to pressure).
- A small genset cannot drive enough fault current through the 20 MVA substation to reach real relay settings; the test scales the relays down. In play, such a source's own protection clears a feeder fault first.
- The 1/0 and 4/0 MV cable ampacities are from the NEC 310.60 table family but not checked line by line (500 kcmil, 450 A, is).
- Genset sounds and turning parts have been checked in test screenshots and logs, not heard in a real play session.
- The new art (cooling tower, fuse, breakers, poles) has been checked in test screenshots, not in a real play session.
- The starter 120 V turbine can carry its full rating on one phase (decided: it stands in for a single-phase set).
- Gas fields are large, so a gas well lasts hours.
- Only the fused cutout shocks on touch; enclosed gear is dead-front by design.

## Ideas not started
- Switches and panels.
- Better wire, ground and breaker art.
- A Mekanism / NeoForge bridge.
