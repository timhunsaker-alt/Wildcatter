# Wildcatter: what is finished

Wildcatter is a Fabric mod for Minecraft 26.3: a realistic power and oil mod built on real electrical principles (it is not a teaching or training tool). It has a real circuit solver, three voltage tiers, safety devices, and power from steam, oil and gas. Everything below is implemented and exercised by the automated client game test (`./gradlew runClientGameTest`). The exceptions are noted at the end.

## Design rules
- Realism comes first: voltages, amps, shocks, grounding and protection behave as they do in real life.
- Large objects are assembled from small parts: the multiblock rule. Examples are the 27-turbine MV generator, the 8-engine cube, the refinery tower and the cooling tower.

## Electrical grid
- **Circuit solver:** the grid is solved as a real circuit (modified nodal analysis). Wires join by role (hot or neutral) and voltage.
- **Voltage tiers:** 110 V, 4.16 kV and 138 kV.
- **Wires:** bare, wool (wears out), and plastic (never wears, safe to touch). There are hot and neutral versions at each tier.
- **Shocks:** touching a bare live conductor hurts. Bare MV and HV lines shock at a distance: 4.16 kV within 2 blocks, 138 kV within 4.
- **Shorts:** bare hot against bare neutral is a short circuit.
- **Grounding:** ground rods and ground wire, with "grounded / UNGROUNDED" shown on machines.
- **Transformers:** step between tiers: pole units at 4.16 kV and 12.47 kV, pad-mounts, unit substations, GSUs, the 138/12.47 kV substation and a 10 MVA 4.16/138 kV unit (10d2).
- **Loads:** electric lamp and electric heater.
- **Power Meter:** right-click anything to read volts, amps, watts and status.
- **Field Manual:** an in-game book with a page for each system.

## Protection devices
| Device | Voltage | Notes |
|---|---|---|
| Circuit Breaker | 110 V | Ratings 15 / 20 / 30 / 50 A. Thermal trip on a mild overload, instant trip on a short. Resettable. |
| 4.16 kV switchgear | 4.16 kV | Metal-clad main and feeder sections that bus together; draw-out vacuum breakers with 50/51 (10d2). |
| Breaker relay cabinet | 12.47 kV, 138 kV | Works the yard breaker it touches: 50/51, and 81U on 12.47 kV (10d2). |
| Switch | 110 V | Yellow. Opens and closes the hot wire by hand and never trips. Protects nothing, so put a breaker or fuse before it. |
| Fuse | 110 V | One-shot and fast. Blows on a short and cannot be reset. Renew it with a copper ingot. |

The 110 V devices go in the hot wire: in at the back, out at the front. Sneak and right-click changes the rating.

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
- Not done: wiring the phasor solver into the grid and three-phase (C), the three-pole breaker, a 240 V (A to B) load, and per-phase wear on cables. Wire textures were script-drawn until 10d4, which gave them Ludo art.

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
- A line above an insulator's rating gets a ground arc ("flashing over"); under load for 3 s the insulator shatters. Tested: the flag. The shattering depends on enough fault current, which a small generator cannot supply (it trips on frequency first), so that path was not exercised by the game test (it is since the protection gap wave: scene `mv_span_flashover`).
- Game test: a 9 block distribution span carries 7,200 V, a 16 block span is refused ("Too far"), insulators on different conductors are refused, and an under-rated insulator is flagged.
- Not done then: span ampacity, 4.16 kV spans and conductor sizes (sizes came in Phase 4.1, the rest in the protection gap wave).

## Phase 2c and 2d: pole transformer, fused cutout, service drop
- **Pole-Mount Transformer** (25 kVA, 12.47 kV (7.2 kV to neutral) to 120/240 V split-phase, or 230 V on IEC): hangs on a pole (one must stand beside it), feeder to its top (high side), leg A and neutral on the back and bottom, leg B on the sides. Loaded beyond 25 kVA it heats and at 150 % burns out in 10 s. (The 2.5 % impedance came in Phase 3a; the short-time rule became C57.109 in 10d1.)
- **Fused Cutout** (DIST): an in-line fuse for the feeder with K-link ratings 6/10/15/25/40/65/100 A; it blows, its tube drops open, and a copper ingot fits a new link.
- **Service drop**: a low voltage span pair (hot and neutral) from the pole transformer to the house. (The house's neutral is earthed at its service since the protection gap wave: a panel with a ground rod.)
- Game test: feeder, cutout, pole transformer, a 7 block service drop and a house lamp that lights; the cutout blows (test shrinks its rating) and the house goes dark; a new link restores it.

## Phase 2e and 2f: receptacles, plug adapters, feeder relays
- **Receptacles and plug adapters** (NEMA 5-15, 5-20, 14-50; CEE 7; IEC 60309 blue): wire a receptacle, set the plug of the same type against it (it will not stay beside another type) and put an FE machine or cable beside the plug. The plug draws real current, as many watts as the attached machines demand (1 W = 1 FE/t, `wattsPerFe` in the config), has no protection, delivers nothing on the wrong voltage, and burns out with its receptacle after 30 s above 120 % of its rating. Two parts: `energy/EnergyBridge` + `HostEnergy` (plain Java) are the only meeting point with other mods.
- **Not done then:** the Team Reborn Energy adapter. It came in Phase 9a. The three-phase plugs came in Phase 3e.
- **Under-Frequency Relay (DIST)** for 12.47 kV feeders: same behaviour as the low voltage relay.
- Game test: a plug hands 923 FE/t (92k FE in 100 ticks) to a stand-in machine; a CEE 7 plug is refused beside a NEMA receptacle and delivers nothing on 120 V; a plug above its rating burns with its receptacle; the feeder relay sheds a pole transformer's overload at 59.0 Hz and the turbine stays on.

## Phase 2 summary
Distribution tier, insulators and spans, pole transformer, fused cutout, service drop, receptacles and plugs, feeder relays are all built and covered by the game test. Its known gaps were closed in the protection gap wave (2026-11-09): span ampacity, 4.16 kV spans and the service's neutral earthing were built, the flashover shattering path is tested, and conductor sizes (Phase 4.1) and transformer impedance (Phase 3a/3c) had been done already.

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

## Phase 4.5f: building gear and the starter plant at true size
- **Small gear** (docs/multiblocks/building-gear.md) is one cell, drawn at its real size; terminals and ratings are unchanged.
  - **In-line gear** (breaker, switch, LV frequency relay, LV fuse, 3-pole breakers, motor starter) gets a `mount`: clicked
    against a wall it hangs on it, its line along the wall (in from the left, out to the right); on the floor it stands on a
    Unistrut stand. Conduit stubs run to its two terminal faces; handles, the pullout and the starter's lights show its state.
  - **Gear wired on every face** (9 receptacles, 9 plugs, the lamp, both heaters) gets an `attach`: the wall, floor or ceiling
    it was clicked on. Receptacles sit on steel boxes (floor boxes on the floor), IEC sockets with their lids, plugs on cords.
- **Motor control center** (Tim's choice): tall NEMA sections, 1 × 3 × 1 — the incoming section (main breaker, fed on top or
  at the back) and new **starter sections** with three bucket spaces each. The **MCC bucket** is now an item clicked into a
  space; each bucket's motor leads leave the back of its cell; breaking a section drops its buckets. The bucket logic is one
  `Starter` class shared by the sections and the standalone starter.
- **Starter plant:** the boiler is a 1 × 2 × 1 small vertical fire-tube boiler with its stack and fittings; the 2 kW
  turbine-generator, the 750 W pump and the firebox are drawn at true size and gain a facing.
- **Old worlds:** one-block MCC buckets drop as bucket items with a chat line; one-block incoming sections and boilers grow
  where they have room; other gear keeps working with the floor look.
- **Models:** `enclosure.py`, `fixtures.py`, `mcc.py`, `starter_plant.py` with `gearlib`; Ludo bulb glass and a pushbutton
  plate (1 credit).
- **Game tests:** `motorControlCenter` rebuilt on tall sections with buckets clicked in (overload trip, a broken section cuts the
  bus and drops its bucket, an old bucket drops); new scene `buildingGear` (wall, floor and ceiling mounts by click, a lamp lit
  through a wall breaker, screenshots).

## Phase 4.5g: the well site at true size
- **Spec:** docs/multiblocks/well-site.md (approved with the plan, 2026-10-31). Ratings unchanged: the rig's 30 kW and the
  pumpjack's 7.5 kW 480 V three-phase motors.
- **Small drilling rig** (Tim's choice: a small rig now, a full land rig later): a skid-mounted top-head-drive rig, 3 × 10 × 6,
  its 9 m mast over the hole at the main cell, its power unit's terminal box low on the back. On a strike it **rigs down**: it
  drops as its item and leaves the wellhead on the hole. A dry or blocked hole leaves it standing.
- **Oil wellhead** (new, one cell): casing head, tubing head, pumping tee and stuffing box. Steel pipe joins it on any side; buckets
  and pipes reach the pumpjack's tank through it.
- **Pumpjack:** API 80-119-54, 3 × 5 × 7 (3 wide, not the plan's 2, because the 1.8 m base straddles the well's centre line).
  Clicked on an oil wellhead it snaps its horsehead over the well. Its motor's terminal box is the bottom cell at the back, wired
  on the back or either side (the cells beside it are open). Off a well it says so and pumps nothing.
- **Christmas tree:** the gas wellhead is a 1 × 2 × 1 tree with its master, wing and swab valves, choke and gauge.
- **Pipes:** drawn at real size, 4 in steel line for oil and gas and 2 in water pipe; the click box is 0.25 m.
- **Moving parts** (docs/kinetics-and-sound.md amended): `Kinetic.Rotor` takes a `Motion` for parts on a linkage, and slow
  machines show at real speed. The pumpjack's crank turns at 9 strokes a minute, the pitmans, walking beam and horsehead follow
  (`kinetic/BeamLinkage`, unit tested: a 1.39 m stroke), and the polished rod rides up and down; with its leads swapped it runs
  backward. The rig's top head turns the pipe at 80 rpm and slides down the mast through each 6 m joint. The starter turbine's
  coupling and flyball governor turn at 120·f/2 (3,600 rpm at 60 Hz) and coast down; the pump is now frame-mounted, its jaw
  coupling turning at 3,450 rpm under an open guard.
- **Old worlds:** a one-block rig grows over its hole where it has room; a one-block pumpjack on its casing becomes an oil
  wellhead and drops a pumpjack item (its crude is lost); a one-block gas wellhead grows upward.
- **Models:** `drilling_rig.py`, `pumpjack.py`, `wellheads.py`, new rotors, the starter plant's pump and turbine guards. No
  Ludo credits.
- **Game tests:** `wells` rebuilt (the rig drills and its head turns on the client, strikes, rigs down and drops; a pumpjack
  clicked on the wellhead snaps over it, pumps, its crank turning at 9 strokes a minute on the client; an old pumpjack becomes a
  wellhead); the motor, MCC and starter scenes use true-size pumpjacks; the steam loop checks the turbine and pump turning.
- **Next (4.5h):** the full land rig, a machine of distinct parts with new power numbers; its spec comes to Tim first.

## Phase 4.5h: the land rig and deep fields
- **Spec:** docs/multiblocks/land-rig.md (approved with the plan, 2026-10-31): a 1,000 hp AC land rig, fed 4.16 kV.
- **Parts** (`RigPartBlock`, laid out by `LandRig`): the substructure (9 × 6 × 9, the controller, the hole under its rotary
  table), the 43 m mast and the 1,000 hp drawworks on its floor, two 1,000 hp triplex mud pumps, 60 m³ mud tanks and the drive
  house (a 2,500 kVA 4.16 kV / 600 V rig transformer and the drives) on the ground. Each part snaps into its place when clicked
  on the floor or on the ground within 16 blocks; the substructure's readout names what is missing. The top drive (250-ton,
  400 hp) is an item clicked into the mast.
- **Electrical:** the drive house is a `DriveLoad`: a balanced 4.16 kV load at PF 0.95 drawing 1.9 MW drilling, 850 kW in a
  connection, 100 kW idle. Its power management keeps the island at 59.5 Hz (99.2 %) or above, picking load up at 10 % a second
  and shedding it at once; below 90 % voltage the drives stop. On the test's 500 kW set it settled near 190 kW at 59.4 Hz and
  drilled at about 10 % of full rate, the set running throughout.
- **Drilling:** 4 ticks a block at full power (five times the small rig), a 15 s connection after each 27 m stand, 80 L of mud
  water a metre from its 60,000 L tanks (a pump on water pipe fills them; with none it stops: NO MUD). No depth limit. On a
  strike it sets the wellhead in its cellar: WELL COMPLETE.
- **Deep fields:** a second field grid (128-block cells, half with a field, radius 16 to 24, tops y -58 to -50; oil 0.4 to
  1 million units, gas 1 to 2.5 million). Shallow fields are unchanged. The seismograph reads DEEP ECHO. The small rig now stops
  110 m down: TOO DEEP.
- **Added while building:** `cable_mv`, a 4.16 kV three-phase cable (the 4.16 kV tier had only single wires), and, at Tim's
  request, unbreakable well casing (cemented steel, like bedrock: a miner who meets it knows it is a well; the fields are not
  blocks, so mining near a well takes nothing).
- **Moving parts:** the top drive and its quill turn at 120 rpm and ride down the mast through each stand, back up in a
  connection; the stand turns below them; the drawworks drum turns in a connection; each pump's sheave and bull wheel turn
  while it pumps.
- **Models:** `land_rig.py` (six parts, five rotors). No Ludo credits.
- **Game tests:** new scene `landRig` (built by clicks, the readout naming missing parts, the top drive clicked in, a starter
  plant's pump filling the tanks, 4.16 kV from a 500 kW set through a backward unit substation, power-limited drilling with the
  frequency held and the set on line, the top drive turning on the client, NO MUD, a pump taken away unbuilds it, a connection,
  the strike and its wellhead, casing to the deep field, DEEP ECHO); `wells` checks a small rig stopping TOO DEEP.

## Phase 5a: the lease
- **Spec:** docs/multiblocks/lease.md (approved with the plan, 2026-10-31).
- **Well streams:** a pumpjack lifts oil, water and gas together: the water cut is 10 % on a new field and rises to 80 % as it
  drains, with 100 m³ of gas per m³ of oil; only the oil counts against the field. A gas well's gas comes up wet: 10 bbl of
  condensate and 2 bbl of water per million scf. The pumpjack's tank is now its 4,000 L flowline.
- **One product a run:** each joined run of steel pipe carries the one product its sources put on it (`fluid/Run`, plain Java),
  or is BLOCKED where two meet; the Power Meter on a pipe says which. Machines pull from their runs, process outlets before
  tanks; a tank fills only from outlets (`block/Lines`, `FluidNode`). Fireboxes now pull their own fuel (a well's stream, wet or
  dry gas, crude, diesel); gas gensets and the steam plant burner take only separated gas (WET GAS otherwise); diesel sets take
  diesel from a refinery or a diesel tank; a refinery takes crude on its base's sides and gives diesel off its top.
- **Parts** (`LeasePartBlock`): a 36 in x 10 ft three-phase separator (no power; 20 L/s of liquid, 1.5 m³/s of gas; inlet at the
  front, gas, oil and water out of the back), 400 bbl steel stock tanks (64,000 L, one product each, buckets in and out),
  400 bbl fiberglass water tanks, a 10 m flare stack (burns only what the separator's back-pressure valve lets go above three
  quarters full, with flame and roar), a 100 hp (75 kW) 480 V H-pump for saltwater disposal (10 L/s, a level switch at 2,000 L,
  its coupling turning at 3,560 rpm, 2,967 on IEC worlds) and a disposal wellhead on a dry hole's casing. With nowhere for its
  gas, oil or water to go the separator SHUTS IN and the wells stop.
- **Models:** `lease.py` (six parts and the coupling rotor). No Ludo credits.
- **Game tests:** new scene `lease` (the stream at 10 % water, the stock tank filling, the gas set on separator gas, the flare
  burning the rest, the H-pump injecting and turning on the client, a bucket in and out, oil meeting water BLOCKED, shut in on
  gas with the flare and engine gone, a pumpjack shut in on a full flowline); `wells` now reads a gas set refusing a gas
  well's wet gas, and `refining` takes its diesel off the column.
- **FTB kit:** New World (2) has a `wildcatter_kit` datapack: `/function wildcatter:kit/land_rig` gives the land rig and its power.

## Phase 5b: the gas plant
- **Spec:** docs/multiblocks/gas-plant.md (approved with the plan, 2026-10-31).
- **Parts** (`GasPlantPartBlock`): a two-stage reciprocating gas compressor on a 400 hp (300 kW) 4.16 kV six-pole motor (125 to
  1,000 psi, 1.0 m³/s; it unloads when idle and stops after 15 s), a packaged TEG dehydrator (no power; its reboiler burns 0.5 %),
  an NGL chiller (0.955 m³ of pipeline gas and 0.20 L of raw NGL a m³), the 150 hp 480 V propane refrigeration compressor that
  cools the chiller its skid touches, a 12 m debutanizer stabilizer (80 % LPG, 20 % natural gasoline) and an 18,000 gallon LPG
  bullet (58,000 L). Wet gas in the chiller reads HYDRATES; with no refrigeration, NO REFRIGERATION.
- **Products:** compressed gas, dried gas, pipeline gas, raw NGL, LPG, natural gasoline. The steam plant burner takes pipeline gas
  only; gas gensets take separator or pipeline gas; fireboxes any gas; stock tanks also hold natural gasoline; LPG only goes in a
  bullet. A flare takes the excess of any gas part above three quarters full.
- **Moving parts:** the compressor's coupling and cooler fan (1,190 rpm, shown at rpm / 10), the refrigeration coupling.
- **Models:** `gas_plant.py` (six parts, three rotors). No Ludo credits.
- **Game tests:** new scene `gasPlant` (a gas set on separator gas powers the plant: 4.16 kV through a backward unit substation to
  the compressor, 480 V to the refrigeration compressor; 0.93 m³/s through the train; LPG and natural gasoline in their tanks; a
  second set on pipeline gas; the residue flared; NO REFRIGERATION and HYDRATES).

## Side wave: ore tooltips and JEI pages (docs/ores.md, "Tooltips and JEI")
- **Tooltips:** every ore block and its mineral now say:
  - where the ore is found;
  - the tool that mines it;
  - each plant that takes it and what that plant makes, or "No use yet". Twelve ores have no user yet.
- **One source:** the lines come from `ore/OreUses.java`. Its switches are exhaustive, and `OreUsesTest` checks every ore is
  covered.
- **JEI:** with JEI installed, U over an ore, a mineral, the four furnace ingots or magnesia opens a Wildcatter information page.
  - JEI is optional and not bundled: Wildcatter compiles against its loader-neutral API only.
  - NeoForge finds the plugin by `@JeiPlugin`, Fabric by the `jei_mod_plugin` entrypoint.
- **Game test:** the `ores` scene checks three tooltips: rock salt, galena ore and barite.
  - It takes a screenshot of the hovered tooltip.
  - When JEI is in the run (`-Dfabric.addMods`), it presses U and takes a screenshot of the JEI page.
- **Recommended mods:** `docs/recommended-mods.md` lists the quality-of-life mods with 26.3 builds for Fabric and NeoForge.
  - The Fabric set ran the `coker` and `anodes` scenes alongside Wildcatter, Sodium included, and both passed.

## Phase 11: assayed slurry (v0.3.0, for Tailings)
- **Six concentrator slurries** on steel pipe (`Product`): mill discharge, fines slurry, conditioned slurry, concentrate slurry,
  thickened concentrate and tails slurry. Liquids, counted in kg, held by no tank.
- **Assays** (`fluid/SlurryAssay`): kg of each mineral per kg, keyed by item id; mass blending, save/load, a meter readout.
- **Holding and pulling:** a source node keeps a `fluid/SlurryHold` and answers `block/Assayed`; `block/Slurry.take` pulls from the
  runs on a machine's faces, shared among sources as `Lines.pull` shares them, and returns the kg with their blended assay.
- **Power Meter** on a slurry pipe: what it carries plus its richest minerals. Field Manual: THE LEASE page, SLURRY.
- **Tests:** `SlurryTest` (products, blending, clamps, save/load, the readout, the hold) and `SlurryPullTest` (a pull from one and
  two stand-in sources, never from itself). Nothing in Wildcatter makes a slurry yet, so there is no client scene; Tailings R4 adds
  the first in-world use.

## Phase 10d4: art and sounds; Phase 10 done
- **Ludo art for the grid gear** (decided with Tim, 2026-11-04: grid gear only; wires from a few materials; Tim reviewed a contact
  sheet first). Sources and prompts in `art/source/` and `PROMPTS.md`; `tools/process_art.py`'s 10d4 section makes:
  - every wire from five materials (bare copper, aluminium and steel strand, braided wool, PVC jacket), banded in its conductor's
    colour: the bare wires per tier, wool at its four wear levels (holes through to the copper, from a fixed seed), plastic per tier;
  - the ground wire (green-and-yellow) and the copper-clad ground rod;
  - the one-block breaker's face (and the switch and UFLS relay tinted from it), the fuse's tube, blown tube and brass cap;
  - `cable_mv` from the cable sheath;
  - item icons for the branch, GFCI and panelboard breakers, the class J and E fuses and the grounding set.
- **The panel gutter** no longer draws a conduit stub out of its front: its circuit leaves in branch cable.
- **Sounds:** `boiler_hiss`, `breaker_click`, `breaker_trip`, `structure_formed`, `turbine_hum` and `zap` were made again in Ludo
  (their old sources were lost), kept in `art/sound_source/`, and processed by `tools/process_sounds.py`. All 20 sounds now have a
  kept source.
- **The full client suite** ran at the end of Phase 10. It found one regression from 10d1: in `coker`, the 400 kW jet pump starting
  direct on line on the 500 kW diesel set now takes long enough (every start goes with V²) that the set trips on stator
  overcurrent, so no coke is cut. The scene's 4.16 kV now comes from the 1 MW gas set. The coker blueprint in the library moved to the
  gas set on 2026-11-06 (Tim: as recommended). It needs a gas line run to the engine: blueprints hold no fuel or wells.

## Phase 10d3: panels and switches (specs: docs/multiblocks/panelboards.md, safety-switch.md, load-interrupter.md, ats.md)
- **Load centre and panelboard** (`load_centre` 1 x 1 x 1, 200 A main, 40 spaces, 120/240 V; `panelboard_480` 1 x 2 x 1, 225 A
  main, 42 spaces, 480Y/277 V), on a wall, the service in on the top or back:
  - **gutters** (`panel_gutter`) set beside the panel each hold one branch space (decided with Tim, 2026-11-04); the chain runs up,
    down or sideways, its spaces numbered from the panel, each space's stab alternating A, B (A, B, C on the panelboard);
  - **branch breakers**, one item per rating: 1-pole 15 and 20 A, 2-pole 30 and 50 A, a 1-pole 20 A GFCI (5 mA; an RCD at 30 mA
    under IEC), and on the panelboard a 1-pole 277 V 20 A and 3-pole 20, 30, 60 and 100 A. Thermal-magnetic, the one-block
    breaker's curve; the GFCI compares the hot current with the neutral coming back through it;
  - **branch cable** (`branch_cable`, `branch_cable_industrial`): armored cable laid along one axis that joins only at its two
    ends, so the runs out of neighbouring gutters stay apart (asked of Tim while building).
- **480 V fused safety switches** (`safety_switch_100` and `_200`): three class J time-delay fuse items (30, 60, 100, 200 A; 500 % for
  10 s), fitted and pulled only with the handle OFF; a blown fuse opens its own phase and the switch names it.
- **5 kV load-interrupter switches** (`lis_5kv`, `lis_5kv_fused`, 1 x 3 x 2): 600 A load-break, fault-close; opened over that it is
  wrecked in an arc flash. The fused one takes E-rated fuses (10E to 200E; 300 s at 220 %, current-limiting above 20 x).
- **Automatic transfer switch** (`ats_480`, 1 x 2 x 1, 400 A, open transition): normal fails below 85 %; TDES 3 s, then a redstone
  engine-start signal; emergency acceptable at 90 % and 95 % of frequency; TDNE 1 s, a 1 s neutral position; retransfer after TDEN
  30 s, cooldown TDEC 30 s (both scaled from 30 min and 5 min). A sneak right-click runs a test.
- **Genset remote start:** a redstone signal at the control panel runs the set; its dropping stops it.
- **Field Manual:** new pages LOAD CENTRE AND PANELBOARD, SAFETY SWITCH, LOAD-INTERRUPTER SWITCH, TRANSFER SWITCH; GENSETS gains
  remote start.
- **Game test `panels`:**
  - a pole unit feeds a load centre with four gutters; the spaces read A, B, A-B, B; the 2-pole reads 239 V across its poles;
  - a 15 A breaker on two heaters trips on overload at 24 A after 160 ticks, the main staying on;
  - grounds on the GFCI's branch trip it on ground fault; space 1 stays on;
  - on a genset's 480 V, a panelboard's 3-pole 30 A trips at 53.7 A after 110 ticks, and its 277 V breaker trips at 7,497 A;
  - a safety switch carries its 15 kW heater at 18 A; a fault on its load blows its fuses; a fuse won't fit with the handle ON;
    new fuses bring the heater back;
  - a fused LIS (10E) and a plain LIS carry 4 A at 4.16 kV; the plain one opens its load, and opened over a lowered load-break
    rating it is wrecked; a fault between them blows the fused one's fuses, the genset staying on.
- **Game test `ats`:** genset 1 normal, genset 2 standby on the ATS's redstone; genset 1 stopped, the load is on emergency in 310
  ticks; genset 1 back, it retransfers after TDEN and genset 2 stops after TDEC; the two sources were never closed together.
- **ModelLint** allows a one-cell part's blockstate to list piece 0 and 1 (the piece property's two values); `BigParts.shape`
  clamps the piece for it.

## Phase 10d2: true-size gear; the one-block stand-ins removed (specs: docs/multiblocks/mv-switchgear.md, breaker-relay-cabinet.md, power-transformer-10.md, poles.md)
- **Removed outright** (decided with Tim, 2026-11-04): `mv_breaker`, `dist_breaker`, `hv_breaker`, `ufls_relay_dist`, `transformer`
  and `substation_transformer`. Older worlds lose them. Recipes that took them now take the new parts.
- **4.16 kV metal-clad switchgear** (`mv_switchgear_main`, `mv_switchgear_feeder`, 1 x 3 x 3, VCP-W 5 class):
  - sections side by side, facing the same way, join their main bus; a lineup takes exactly one main;
  - the main's cable comes in low on its back, each feeder's leaves its roof;
  - a draw-out vacuum breaker, 1,200, 2,000 or 3,000 A frame, that racks CONNECTED, TEST, DISCONNECTED, only while open;
  - its relay: 51 moderately inverse at time dial 1, 50 at 8 x, the pickup set from 5 to 1,200 A with the Power Meter.
- **Breaker relay cabinet** (`breaker_relay_cabinet`, 1 x 2 x 1, SEL-351 class): it works the `dist_breaker_3p` or `hv_breaker_3p`
  it touches, with 50/51 and, on 12.47 kV, 81U at the load-shed frequency. Right-click resets it and recloses the breaker.
- **4.16 kV pole transformer** (`pole_transformer_mv`): 25 kVA, 2.4 kV to 120/240 V, hung on a pole, rated by C57.109.
- **10 MVA power transformer** (`substation_transformer_10`, 5 x 5 x 4): 4.16 kV delta / 138 kV grounded wye, 7.5 %Z, either way.
- **Field Manual:** new pages 4.16 KV SWITCHGEAR and BREAKER RELAY CABINET; VOLTAGE LEVELS, FREQUENCY, SUBSTATION and POLE-MOUNT
  TRANSFORMER updated.
- **Game test `switchgear`:**
  - a genset and unit substation feed a main and two feeders; the main reads 1 main and 2 feeders;
  - feeder 1's 51, its pickup dropped to 5 A under an 8 A load, trips it after 110 ticks, and the main stays closed;
  - grounds on feeder 3's cable trip it alone by its 50 at 1,196 A;
  - racked to DISCONNECTED, it refuses to close.
- **Moved scenes:**
  - `refining`: its 4.16 kV fault now trips a switchgear feeder (50, 1,196 A) with the main staying closed;
  - `transformerChain`: two 4.16 kV pole transformers on poles, 2,397 V to neutral on the line;
  - `spans` and `feederRelay`: a `dist_breaker_3p` and its cabinet; 81U opens it at 58.1 Hz and the reset recloses it;
  - `potline` and `overheadThreePhase`: the 10 MVA unit, step-down for the fan and step-up to the 138 kV line.
  - `overheadThreePhase` gives the player slow falling: its last camera spot dropped them to their death, and the next scene's
    machines then never ticked;
  - the potline blueprint in the library was exported again with the 10 MVA unit.

## Phase 10d1: protection (spec: docs/multiblocks/gen-bus-protection.md; ELECTRICAL_MODEL.md updated)
10d is four waves (Tim, 2026-11-04): 10d1 protection, 10d2 true-size gear, 10d3 panels and switches, 10d4 art and sounds.
- **Transformer short-time rule** (`circuit/TransformerHeating`):
  - every rated transformer now heats on the IEEE C57.109 through-fault curve: it fails when `(I² − 1)·t` reaches 1,250 pu²·s, I
    being its most loaded winding's current over rated;
  - 150 % lasts about 17 minutes, 200 % about 7, a 6 x inrush 36 s, 25 x 2 s; below rating it cools back over 10 minutes;
  - it replaced the 150 %/10 s rule; the readout gives how long it has at its present load.
- **Every motor start goes with V²** (decided with Tim): a direct start on a sagging supply runs longer, as a soft start already did.
- **Generator bus protection:**
  - **The panel:** a bus relay panel (`bus_relay_panel`, 1 x 3 x 1) stands against the 13.8 kV bus duct, and finds its zone up to
    the generator breakers and feeder breakers.
  - **87B bus differential:** the current going into a fault on the bus (by Kirchhoff, what enters it and leaves at no breaker)
    against a percentage restraint trips every breaker on the bus, and 86B locks out until the panel is reset.
  - **51V:** on each generator breaker (pickup 1.5 x full load, restrained down to a quarter by the voltage, IEEE very inverse).
  - **50/51:** on each feeder breaker (51 at its rating, moderately inverse; 50 at 8 x).
  - **The panel's reset** releases 86B, closes the feeders it opened and resets the generators it tripped.
- **The 15 kV feeder breaker** (`gen_feeder_breaker`, 3 x 3 x 1): a metal-clad vacuum cubicle, 1,200, 2,000 or 3,000 A, opened
  only by the panel.
- **The grounding clamp set** (`grounding_set`, ASTM F855 class): right-click a cable, busway or bus duct to put A, B and C to
  ground. On a dead conductor it keeps it at earth; on a live one it is a bolted fault with a flash.
- **Field Manual:** a new page, GENERATOR BUS PROTECTION; POLE-MOUNT TRANSFORMER and MOTORS follow the new rules.
- **Unit tests:** `TransformerHeatingTest`, `BusProtectionTest` (87B, voltage restraint, the C37.112 curves, the time element),
  and `DrivesTest`'s direct start now slows on a weak supply.
- **Game test `busProtection`:**
  - a gas turbine on its bus with two feeder breakers to load banks, and the panel;
  - grounds on the bus trip the generator and both feeders by 87B and lock out, and a reset brings them back;
  - grounds past feeder 1 trip it alone by its 50;
  - the same grounds on the now dead section read GROUNDED and trip nothing.
- **Other scenes:** `poleTransformer` checks the new readout at 4 x and a burn-out at 25 x.

## Phase 10c3: the ring crane, shared sites and tandem lifts (specs: docs/multiblocks/ring-crane.md; docs/blueprints.md); 10c done
- **The ring crane** (`ring_crane`, after the Sarens SGC-250 "Big Carl", from Sarens' SGC brochure):
  - its 48.5 m ring at true size: 616 cells of mats, two ring beams and their rails, 1 m high, the inside left free;
  - drawn above it, turning with the slew:
    - 128 bogies;
    - the front platform with the boom foot, and the rear platform with 52 counterweight boxes of 100 t;
    - six power packs;
    - the double lattice main boom, the backmast and pendants;
    - the 14.5 m, 104.6 t main hook block;
  - both main booms by sneak-click (decided with Tim, 2026-11-04):
    - 118 m: 5,000 t at 40 m down to 1,554 t at 120 m;
    - 160.5 m: 3,114 t at 45 m down to 838 t at 170 m;
    - capped at the main hook's 3,200 t;
  - 6 winches at 20 m/min, reeved in sixes at 536 kN a line: 20 m/min light, 2 m/min on 60 parts; slewing 6 degrees a minute;
  - 12 diesels of 310 kW (3.72 MW) on a 6,000 L tank, piped to any cell of its ring. The plan's "about 2 MW of diesel sets" was
    corrected from the real crane;
  - its chest is a **laydown** (decided with Tim): its chart starts 40 m out, so it picks from the chest nearest it within its
    chart, read at that chest's real radius.
- **Sharing a site** (`SiteMarkerBlockEntity`, `CraneBlockEntity.findWork`; decided with Tim):
  - every crane in reach of a marker works it, and no lift is taken twice;
  - a crane passes over a lift it cannot make, but never onto one that rests on a lift not yet set, or on another crane's hook
    (`Blueprint.restsOn`). Dismantling works the same way backward;
  - the marker's readout counts the cranes working it.
- **Tandem lifts** (decided with Tim): a part too heavy for every crane alone goes up on two.
  - The crane with it in its chest leads.
  - Each crane carries half, plus its own hook block, within 80 % of its chart (25 % to spare).
  - Both run the same steps at the slower crane's times (`LiftCycle.together`).
  - The hooks are at the part's two ends; the lead draws the load between them.
  - Both hold if either loses power. If either is broken the lift is called off, and the part goes back to the lead's chest.
- **Field Manual:** a new page, HEAVY LIFTS; CRANES says how a crane works ahead.
- **Unit tests:**
  - `LoadChartTest`: the ring charts and the hook cap;
  - `LiftCycleTest`: reeving in sixes, slewing, hook height, diesel, and a tandem's merged times and shares;
  - `BlueprintTest`: rests-on.
- **Game test `heavyLift`:**
  - the ring crane reads 2,850 t at 60 m on its 160.5 m boom, and 3,831 t capped at 3,200 t on its 118 m boom;
  - it sets a reactor vessel (288.2 t with its hook block) 60 m out from its laydown chest 45 m out;
  - two crawlers on their 32 m booms share a site: alone each reads TOO HEAVY for its GSU 60 (91.8 t against 89.5 t at 16 m); in
    tandem each takes 45.9 t against 80 % of 89.5 t; between them they set the three pallets, and no lift is set twice;
  - screenshots of the ring crane mid-lift and of the tandem lift.

## Phase 10c2: the tower and crawler cranes (specs: docs/multiblocks/tower-crane.md, crawler-crane.md)
- **The tower crane** (`tower_crane`, after the Liebherr 380 EC-B 16, from its BKL data sheet):
  - a 6 x 3 x 6 cross base with ballast and the mast foot;
  - drawn above it: the 355 HC mast to 59.7 m under the hook, the 75 m jib, the counter-jib, the trolley and the hook;
  - its real chart: 16 t to 19.6 m, then 3.0 t at 75 m. The 3.4 t headline is Litronic's power-plus mode and is left out;
  - its hoist gears: 16 t at 36 m/min up to 1.9 t at 205 m/min; the trolley at 95 m/min; slewing at 0.8 rpm;
  - one frequency-converter load (`DriveLoad`) on the industrial voltage at its switch cabinet's west face: the hoist at m g v / 0.85
    (at most 110 kW), the trolley 10.5 kW, slewing 15 kW, 3 kW standby.
- **The crawler crane** (`crawler_crane`, after the Liebherr LR 1300, from its technical data):
  - 9 x 2 x 9 crawlers and carbody; drawn above them: the upper works, the lattice boom at its angle, the pendants and the hook;
  - its real main-boom chart with the 104 t counterweight: 286.2 t at 4.3 m on 20 m, down to 2.6 t at 80 m on 86 m;
  - the boom set by sneak-click, 20 to 95 m, between lifts (decided with Tim, 2026-11-04);
  - parts of line for the load at 150 kN each (1 to 22): the hook at 138 m/min over the parts, held to what 390 kW lifts;
  - slewing at 1.8 rpm, luffing 15 to 86 degrees in 127 s;
  - its own diesel: a 750 L tank filled from a steel line to any cell of its crawlers, burning 102 L/h at full load, 15 % idling.
- **How a crane works** (`crane/LoadChart`, `crane/CraneKind`, `crane/LiftCycle`, plain Java; `block/CraneBlockEntity`):
  - it takes the nearest site marker in its reach, and the marker's next lift (in DISMANTLE, its last standing one);
  - it checks the load on the hook against its chart at the lift's radius, read at the farther radius as an operator does; its
    height under the hook; what its chest holds; and its power;
  - each lift's steps run at its real speeds: to the pick, rigging (30 s), hoisting clear, slewing and moving out together,
    lowering, setting (30 s);
  - a part is placed and grown, a pallet's blocks set, as a hand would (`Surveys.setLift`);
  - its readout names the step, the load against its chart, or what it waits for: TOO HEAVY, OUT OF REACH, TOO HIGH, WAITING (what
    the chest lacks), IN THE WAY, CHEST FULL, NO POWER, NO DIESEL, NO SITE.
- **Dismantling** is the site marker's mode: sneak-click it to switch between BUILD and DISMANTLE (decided with Tim, 2026-11-04).
  The cranes take the site down, last lift first, into their chests: a part as its item, a pallet's blocks as theirs.
- **The marker** keeps a claim on each lift a crane has taken, so two cranes never work the same lift (for 10c3).
- **The renderer** (`client/CraneRenderer`) draws the lattice from textured beams, turning with the slewing angle and moving through
  each step, with the load hung from its own block models.
- **Field Manual:** a new page, CRANES; BLUEPRINTS names the marker's mode.
- **Unit tests:** `LoadChartTest` (the charts, the farther-radius rule, the 15 degree reach) and `LiftCycleTest` (hoist gears, parts
  of line and the engine limit, the steps' times and power, diesel).
- **Game test `cranes`:**
  - a tower crane on a diesel set's 480 V finishes a surveyed site (a stone-brick pad and a 480 V pad-mount) from its chest, then
    in DISMANTLE lifts the pad-mount back into it;
  - a crawler fed diesel from a stock tank reads TOO HEAVY for a GSU 60 at 9 m on its 86 m boom, and sets it on its 32 m boom;
  - screenshots of each crane mid-lift.

## Phase 10c1: blueprints (spec: docs/blueprints.md)
10c is three waves (Tim, 2026-11-04): 10c1 blueprints, 10c2 the tower and crawler cranes, 10c3 the ring crane and heavy lifts.
- **The format** (`blueprint/Blueprint`, plain Java):
  - a box facing north from its north-west bottom corner, holding a palette of block states and every ordinary block;
  - each true-size part as one entry (its id, facing, main cell and box), and each wire's and cable's conductor size;
  - JSON, or gzipped in base64 on an item;
  - turning by quarter turns.
- **The lift list:**
  - bottom up, the parts first on each layer;
  - then pallets of at most 64 blocks of one 5 x 5 m square, at most 16 t each (the tower crane's most).
- **Masses** (`blueprint/Masses`): the heavy parts' own figures (reactor vessel 180 t, GSU 60 90 t, pot 60 t, and so on); the rest
  by kind from their footprint box; ordinary blocks by material from their sound type; 2 % rigging on the hook, at least 0.5 t.
- **The survey tool** records any box of up to 128 x 96 x 128 onto a blueprint in the off hand. Contents and machine states are not
  recorded.
- **The blueprint** item: blank, a library plant (sneak-right-click steps through 26), or a survey. Its tooltip gives size, parts,
  blocks, the heaviest part and the lift count.
- **The ghost** (`client/BlueprintGhost`, on the same gizmo hook as the part placement ghost):
  - held, a blueprint is drawn where it would stand, turned the way you face;
  - parts are outline boxes, red where something is in the way;
  - blocks not yet standing are translucent cubes in their map colour, within 32 m, at most 4,096.
- **The site marker:** right-click with a blueprint sets it at the origin.
  - It keeps the ghost up for every player within 128 m of the site's box until the site is built.
  - A block counts as built when it is the same block set the same way; the sides a cable or fence joins to are not compared,
    since it redraws them from its new neighbours.
  - Every 2 s it counts the lifts that stand; its readout names the next lift, its mass, and what is IN THE WAY.
  - `Surveys.setLift` sets a lift as a hand placement would; the cranes will use it.
- **The library:** 26 plants, from the steam plant to the potline, exported from their game test scenes with
  `WILDCATTER_EXPORT_BLUEPRINTS` and shipped as JSON in the jar. The ISR wellfield is left out (its wells must stand on its ore).
- **Field Manual:** a new page, BLUEPRINTS.
- **Unit tests:**
  - `BlueprintTest`: JSON and the compressed form, turning, the lift order, the pallets' cap and squares;
  - `MassesTest`.
- **Game test `blueprints`:**
  - a genset, a unit substation, cable and a stone-brick apron are surveyed;
  - the site is set 50 m east, facing east, and its lift list checked;
  - it is built lift by lift and reads built;
  - every library blueprint loads with every block and part known;
  - the potline's ghost is drawn at a marker, and the client must hold the marker with its blueprint.

## Phase 10b3b: the potline (spec: docs/multiblocks/potline.md; dc-bus.md updated)
The smelter's second wave (Tim, 2026-11-03): the pots that turn 10b2's alumina and 10b3a's anodes into aluminium. All parts are true
size (`block/PotlinePartBlock`), sized for a line of up to 40 pots.
- **Rectiformers** (`RectiformerBlockEntity`): 60 kA and up to 220 V each, on 138 kV.
  - Set side by side they form a yard, joined inside at their DC terminals. The yard holds one line setpoint (60 to 180 kA), split
    among the units online, at most 60 kA each: with 4 online each carries 45 kA, and with one tripped the other three carry 60 kA.
  - `DcSupply` now takes its AC tier, maximum volts, resistance, ramp and efficiency; the 10b1 rectifier is unchanged.
- **180 kA bus:** aluminium (0.55 m², 0.063 µΩ/m) and copper (0.18 m², 0.12 µΩ/m), larger bars that heat, burn and fault as the
  10b1 bars do.
- **Pots** (`PotBlockEntity`), 180 kA prebake:
  - each is a DC cell of 1.65 V back-EMF, 4.0 V at 180 kA. Pots set side by side join through 1.67 µΩ of risers, so bus is only
    needed at a row's ends and crossovers;
  - each makes 56.8 kg/h at 94 % current efficiency, from 1.93 kg of alumina a kg (reacted bags first), 20 kg of AlF3 a t with its
    gas going to the duct (45 venting), and 0.41 kg of carbon;
  - it changes an anode every 39 h from rodded anodes in its hopper, dropping the spent one below. An overdue change costs 2 points
    of current efficiency;
  - with no alumina it has an anode effect: the back-EMF rises so it reads 30 V, it makes no metal, and it counts CF4 (IPCC slope).
    It clears 2 minutes after alumina is back;
  - it holds 2 days of metal (FULL: TAP IT);
  - below 18 kA for 4 h it freezes and opens the line. Broken, it leaves spent pot lining.
- **The fume treatment centre:** a pot row's gas passes along its pots' joined ducts to the row's end, and a pipe carries it
  (POT_GAS) to the centre. With a 250 kW main fan (4.16 kV) against it, it turns a fresh alumina bag into a reacted one for each
  45,600 m³ of gas treated.
- **Metal:** the tapping crucible item (8 t, its kg on the stack in a new `metal_kg` component) taps a pot and pours into the ingot
  casting machine (10 t/h, 480 V), which casts 22.7 kg aluminium ingots.
- **Items:** aluminium ingot and block, reacted alumina bag, spent pot lining, tapping crucible. Recipes: the 180 kA bars (7
  aluminium or copper blocks over 2 bricks), the aluminium anode rod.
- **`DcCell`:** its back-EMF can change while it runs, and its resistance takes out the solver's 1 µΩ source impedance, so a pot reads
  its rated 4.0 V at 180 kA (a 0.18 V error at potline current, a few mV on the 10b1 cells).
- **Models:** `tools/models/potline.py`, 5 parts; the 180 kA bars are drawn in `make_assets.py`.
- **Field Manual:** new pages THE POTLINE and TAPPING AND CASTING.
- **Unit tests:** `PotlineTest` (the 40-pot balance, the anode life of 29.3 days, the current efficiency, the voltages against the
  rectiformers, the units' shares, the fume treatment centre); `DcSolveTest`: 40 pots on 4 rectiformers sharing equally, one
  tripped, and an anode effect needing 26 V more.
- **Game test `potline`:**
  - the aeroderivative through its GSU 60 at 138 kV feeds a yard of 4 rectiformers and 10 pots in two rows. The line holds 180 kA at
    45 V, 45 kA a unit, about 9 MW;
  - the fume treatment centre (its fan through a 4.16/138 kV step-down and a 4.16 kV soft starter) fills a reacted alumina bag;
  - a pot drops a spent anode;
  - a full pot is tapped and poured, and the casting machine drops ingots;
  - with one unit tripped, the line holds 180 kA on three at 60 kA each;
  - a pot without alumina reads an anode effect at 30 V and clears;
  - a pot off the line freezes, and breaks into spent pot lining.

## Phase 10b3a: coke, pitch and anodes (specs: docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md)
10b3 is two waves (Tim, 2026-11-03): 10b3a the carbon and anodes, 10b3b the potline. All parts are true size (`block/CarbonPartBlock`),
sized for a 40-pot line.
- **The delayed coker:** 169 t of residue a day (about 2 L/s, a fifth of the big crude unit's):
  - the fractionator, the heater (495 C, 1.1 MJ/kg of gas, coker gas or LPG), two coke drums on the coke pit, and the pit's 4.16 kV
    jet pump that cuts the drum not filling;
  - yields 15 % coke, 65 % gas oil (the diesel line), 13 % naphtha (gasoline) and 7 % coker gas;
  - the drums switch once the filling one holds a tonne and the other is cut out.
- **The coke calciner:** a rotary kiln (78 % yield, 1.5 MJ/kg of gas) against a rotary cooler, bagged by a bagging station.
- **The coke ovens:** 240 t of vanilla coal a day:
  - the coal tower, a battery of 13 slot ovens with its pusher machine and coke quench tower;
  - the by-product plant (4.16 kV exhauster) keeps 40 % of the gas for the underfiring and sends out coke oven gas (18 MJ/m3) and
    tar; the tar still makes 55 % pitch, the tar oils onto the residue line;
  - metallurgical coke (112.5 kg, one per coal) works wherever coal does (`block/SolidCarbon`): the chlorinators' and the B4C
    furnace's carbon, the steam plant's burner.
- **The anode plant:** 25 anodes a day:
  - the paste plant (4.16 kV; 65 % calcined coke, 20 % crushed butts, 15 % pitch; with no butts it makes them up with coke);
  - the forming press (1,275 kg green anodes), the ring baking furnace (2.4 MJ/kg) and the rodding shop;
  - the rodding shop strips a spent anode into its rod and 303 kg of crushed butts, and rods a baked anode with an iron ingot;
  - the anode rod is three copper ingots over a steel ingot (the aluminium recipe comes in 10b3b).
- **Gas burners** (`GasFiring`) now fire on coker gas and coke oven gas by their heating value.
- **New pipe products:** coker gas, raw coke oven gas, coke oven gas, coal tar and pitch (tar and pitch in kg, both tanked).
- **Items:** green petroleum coke, calcined coke bags, metallurgical coke, green, baked, rodded and spent anodes, crushed butts,
  anode rods.
- **`FillerBlockEntity`** takes more than one input slot (the rodding shop's four).
- **Models:** `tools/models/carbon.py`, 16 parts.
- **Field Manual:** new pages PETROLEUM COKE AND PITCH and ANODES.
- **Unit tests:** `CarbonTest`: the yields add up, the coker's calcined coke and the ovens' pitch cover the anodes, the anode recipe
  and the baking fuel.
- **Game test `coker`:** the fractionator takes 2.24 kg/s of residue, the heater cokes on 2.5 MW of LPG, one drum fills while the pit
  cuts the other; gas oil and naphtha fill their tanks; a piece of green coke drops under the pit and a calcined coke bag under the
  bagging station (both seeded almost full); the kiln calcines 950 kg/h.
- **Game test `cokeOvens`:** the battery cokes 3.27 kg/s of coal, the by-product plant cleans 1.26 m3/s of raw gas and the still makes
  230 kg/h of pitch into its tank; coke drops under the quench tower; with the exhauster unpowered the battery stops, its gas with
  nowhere to go.
- **Game test `anodes`:** the paste plant mixes 1.5 t/h, the press drops a green anode and the furnace a baked one; the rodding shop
  strips a spent anode into crushed butts and rods a baked anode on the freed rod.

## Phase 10b2: the Bayer refinery, lime, AlF3 and chlorine (specs: docs/multiblocks/bayer-refinery.md, alumina-plant.md)
- **The refinery:** 105 t of alumina a day (Tim, 2026-11-03), enough for a 40-pot 180 kA line. It is the full flowsheet as true-size
  parts (`block/AluminaPartBlock`):
  - **the line** (`BayerStageBlockEntity`): ball mill, digester (145 C, process steam) against the flash tanks, settler, mud washer,
    polishing filters, precipitator, evaporator and package boiler;
  - **the parts that move solids:** the residue filter press (red mud blocks, for a dry stack), the hydrate filter (sneak-click sends
    a share to bags), the gas-suspension calciner (4.16 kV fan) and the causticizer.
- **The mass balance** (`fluid/Bayer`): each stream carries its alumina in a fixed share, so a part's output is its input times a
  ratio. Per kg of alumina:
  - 2.057 kg of bauxite (a block gives 1,021 kg);
  - 16.25 kg of strong liquor;
  - 0.1875 kg of 32 % caustic (60 kg of NaOH a t);
  - 40 g of lime;
  - 1.98 kg of steam;
  - 3.0 MJ at the calciner;
  - 0.752 kg of red mud cake.
- **The liquor loop:** the evaporator returns strong liquor only as fast as its caustic and lime allow, so short supply slows the whole
  plant. The first fill is waived: the evaporator starts with 10 minutes of strong liquor in storage, and banks what comes back over.
- **Lime, AlF3 and chlorine:**
  - limestone, a new rock (`Ore.LIMESTONE`, plains, forests and rivers);
  - the lime kiln (10 t/d, 4.0 GJ a t);
  - the AlF3 reactor (50 kg/h, on the HF column's anhydrous HF);
  - the bagging station (1 t bulk bags of alumina, hydrate, AlF3 or quicklime, from whatever it touches);
  - the chlorine filler (907 kg ton containers).
- **The chlor-alkali cell:** its NaOH is now 32 % caustic soda out of the west face of its back cells. It stops when the caustic has
  nowhere to go, and caustic a saved cell had counted comes into its hold. Stock tanks hold caustic, and their readout now names
  the product's unit.
- **New pipe products:** caustic soda, process steam, and the eight Bayer slurries and liquors (kg; only caustic is tanked).
- **Items:**
  - the empty bulk bag;
  - alumina, hydrate, AlF3 and quicklime bags;
  - empty and full chlorine ton containers;
  - the red mud block.
- **Models:** `tools/models/alumina.py`, 17 parts.
- **Field Manual:** new pages ALUMINA: THE BAYER PROCESS and LIME, ALF3 AND CHLORINE. The chlorine-and-magnesium page now notes
  the caustic outlet.
- **Unit tests:** `BayerTest`, covering:
  - the streams against the spec;
  - the loop's balance, both the liquor and the total mass;
  - 11 chlor-alkali cells for the full rate;
  - one boiler;
  - lime per block and AlF3 stoichiometry.
- **Game test `bayer`:**
  - **the whole refinery on four diesel sets:** every part runs in flow order. The mill grinds 2.9 kg/s of bauxite in 22.8 kg/s of
    strong liquor, and the calciner makes 1.36 kg/s of alumina on 4.1 MW of LPG, both still catching up;
  - **products:** an alumina bag drops under the bagging station and a red mud block under the press; red mud places as a block;
  - **caustic cut off:** the evaporator reads NO CAUSTIC.
- **Game test `alf3Lime`:**
  - the lime kiln fills a quicklime bag (407 kg/h of lime on 452 kW of gas);
  - the AlF3 reactor fills an AlF3 bag (48.9 kg/h on 34.9 kg/h of HF);
  - the chlor-alkali cell on its rectifier (2.80 kA) sends chlorine to the filler, which fills a ton container, and its caustic to
    a tank.

## Phase 10b1: the DC tier (spec: docs/multiblocks/dc-bus.md)
- **DC on the same solver:**
  - a DC network is resistors and EMFs at 0°, solved in the same group as the rectifier's AC side;
  - DC terminals use their own role and voltage key (`Circuit.dcPort`), so nothing AC joins them;
  - the solver's smallest impedance is now 1 nΩ, so a 180 kA string of 40 pots on nΩ bus solves to 0.1 % (`DcSolveTest`).
- **The rectifier** (`circuit/DcSupply`):
  - **regulation:** a current-regulated source with a setpoint dial (1-16 kA, 6 kA default). It rises at most 15 V/s, up to
    150 V, and comes down at once;
  - **diode bridge:** it is off while below the load's own voltage;
  - **trips:** on DC overcurrent, 1.25 x for 1 s, or 2 x at once when its terminals collapse (a bolted short). A cell closing
    onto the open-circuit 150 V overshoots for a moment and is pulled back, not tripped;
  - **AC side:** the DC output / 0.97 at PF 0.95. The fluorine house is back at 668 kW, as in 7d;
  - **ground-fault detector:** 10 kΩ from each pole to earth; it names the faulted pole.
- **Cells** (`circuit/DcCell`, `block/Electrolysis`):
  - the fluorine house, the magnesium cell and the chlor-alkali cell are now DC loads, each a back-EMF behind a resistance;
  - what they make goes with the current through them;
  - an idle cell opens its circuit;
  - + and − terminals two cells apart on one face;
  - **touching:** a cell set against its rectifier still works, joined directly, and the rectifier takes its rated current.
- **Bus bars:** `dc_bus_4k` and `dc_bus_16k`, bare copper on porcelain standoffs, laid along an axis.
  - They join any bar or DC terminal they touch.
  - Against a ground wire, a bar is a ground fault.
  - They heat and burn open as wires do.
  - From 120 V DC to earth they shock, at half the 480 V tier's strength per volt (`Shock.zapDc`).
- **Field Manual:** a DC: RECTIFIERS AND BUS BARS page; the fluorine and the chlorine-and-magnesium pages point to it.
- **Unit tests:**
  - `DcSolveTest`: the potline string; a ground fault puts the other pole at full voltage;
  - `DcTest`:
    - 6 kA held through 1, 4 and 12 cells;
    - capped at 150 V;
    - the detector's ±V/2 and fault;
    - open circuit at 150 V;
    - a cell closing onto it does not trip;
    - a short trips, and resets.
- **Game test `dcTier`:**
  - **on the bus:** a magnesium cell 8 m away on 16 kA bus. The rectifier holds 14.00 kA at 5 V, 77 kW of AC, and the poles read
    ±3 V. The cell makes 5.7 kg/h of magnesium;
  - **ground fault:** a ground wire against the − bus is named, with no trip;
  - **burn:** a 4 kA bar in the run burns open;
  - **open:** the rectifier rises to 150 V. With − faulted, the + bus is at 150 V and shocks a pig;
  - **short:** a bar across the runs trips it at 1,308 kA;
  - **touching:** a magnesium cell against its rectifier takes 14 kA.
- **Reruns pass:**
  - conversion: 108 V, 6.00 kA, 668 kW;
  - zirconium: chlor-alkali 2.80 kA, magnesium 14.0 kA;
  - fuel, cascade, plugs;
  - the NeoForge smoke scenes.
- **Changed from the plan while building:**
  - DC uses a hidden role key, not a new `Conductor` value: that would have added a state to every cable block;
  - + sits at y 2 and − at y 0, so the first bars off them don't touch;
  - the scene runs the magnesium cell on the bus, not the fluorine house: anhydrous HF can't be stocked in a tank;
  - the rectifier's internal resistance is 0.1 mΩ (0.5 mΩ was planned): at 0.5 mΩ, its own drop was counted as power on top of
    its 97 %.

## Phase 10a: soft starters and drives for big motors (specs: docs/multiblocks/soft-starter.md, vfd.md)
- **Motors** (`circuit/Motor`):
  - behind a ramping soft starter (`Circuit.softStarted`) the starting torque goes with V², so the start takes longer. A direct
    start keeps its flat 2 s: Tim's choice, 2026-11-02. With V² everywhere, the reactor scene's direct-started 500 kW pumps
    burned their 1 MVA unit substation under the 150 %/10 s rule;
  - a motor wired to a drive's output (`Circuit.driveFed`, a conductor-only search in `Network.reaches`) accelerates at its rated
    current instead of locked-rotor current, and runs once its drive is at full voltage.
- **One `Starter`, three kinds:**
  - **direct:** the combination starter, as before;
  - **soft:**
    - while ramping, each pole is a series reactance it sets every solve to hold the current at its limit dial (2-5 x the
      full-load amps, 3.5 x default);
    - the bypass closes at 1.25 x;
  - **drive:**
    - the line side is a constant-power star at PF 0.95 drawing the output / 0.97;
    - the motor side is its own three-phase source, ramping over 10 s;
    - above 1.5 x the full-load amps the ramp holds, and after 60 s it trips;
    - below 90 % line voltage it stops and restarts;
  - all three have the class 10 overload relay.
- **Gear (all true size, `tools/models/drives.py`):**
  - the 480 V soft starter and drive cabinets (1 x 2 x 1);
  - soft-starter and drive buckets for the MCC;
  - the 4.16 kV soft starter (1 x 3 x 1);
  - the 4.16 kV drive lineup: input transformer section, power cell section, output and control section (5 x 3 x 2, checked by
    the control section; `DriveLineup`).
  - **Terminals:**
    - a cabinet's line goes in on the top or the back of its top cell, and its motor out of the back of its bottom cell;
    - the lineup's line goes in at the transformer section's outer end or its top, and its motor out at the control section's
      outer end.
- **Constant power for the existing drives:** `DriveLoad` now sizes its impedance from the voltage it measures, so the rig drive
  house, the header house, the frequency converter and the rectifier draw more current as the bus sags.
- **Field Manual:** a SOFT STARTERS AND DRIVES page. The MOTORS and MOTOR CONTROL pages point to it.
- **Unit tests (`DrivesTest`, which now has Minecraft types on the classpath):**
  - direct on line draws about 6 x;
  - the soft starter holds 3.5 x, starts in 4-10 s and bypasses;
  - the drive keeps within 1.5 x, runs after its 10 s ramp, PF > 0.94, and its line power is the motor's / 0.97;
  - a direct start takes the same time on a weak bus;
  - `DriveLoad` takes the same power at 95 % voltage, at 1/0.95 the current.
- **Game test `drives`:**
  - **250 hp charge pump on one 500 kW set:**
    - direct on line: 172 A peak;
    - behind the 4.16 kV soft starter: held at 112 A (3.5 x 32 A), then BYPASSED;
    - behind the 4.16 kV drive: a 28.9 A line peak at PF 0.95, then RUNNING at 60 Hz;
    - breaking the cell section stops it and names the section.
  - **Two 500 kW coolant pumps on one three-set island, each on its own drive:** both run, and all three sets stay on line.
  - **A 480 V MCC:** a soft-starter bucket and a drive bucket each run a 60 hp pump.

## Phase 9c: plugs feed FE on NeoForge (Phase 9 done)
- **`NeoForgeEnergy`:** a plug adapter on NeoForge hands its power to any FE energy handler beside it, found through
  `Capabilities.Energy.BLOCK` on the face toward the plug. Mekanism, and most NeoForge tech mods, use this capability.
  - Its demand is a simulated insert in a root transaction that is never committed.
  - The rate is 1 W = 1 FE/t (the config's `wattsPerFe`).
  - It mirrors 9a's Team Reborn Energy provider; the plug's tick-by-tick delivery is shared.
- `neoforge.mods.toml` lists Mekanism as optional (no code needs it).
- **NeoForge smoke scene `plug`:** a gold block registered as a real `SimpleEnergyHandler` (1,000,000 FE, 1,000 FE a tick) fills
  at 930 FE/t (8.0 A at 116 V), and the plug draws 0 A once it is full.
- **The Field Manual's plugs page** names FE machines on NeoForge.
- **Release:** Phase 9 ends with the first GitHub Release, v0.2.0.

## Phase 9b: the NeoForge port (one codebase, two jars)
- **Layout:** a multi-loader project after jaredlll08's MultiLoader-Template for 26.3. Every file moved with `git mv`, so its history
  follows.
  - `common/`: nearly everything, compiled against plain Minecraft (NeoForm 26.3-1), with the shared assets and data, and the unit
    tests.
  - `fabric/`: the entry points, Fabric's biome API for the ores, Team Reborn Energy, and the client game test.
  - `neoforge/`: the entry points (NeoForge 26.3.0.101), a `LevelChunk` mixin that tells the circuit manager when block
    entities load and unload (Fabric API's block entity events hook the same places), the ores' biome modifiers, and the smoke scenes.
- **The seam:** `dev.wildcatter.platform.Platform` (config folder, loaded mods), found by `ServiceLoader`. `Wildcatter` and
  `WildcatterClient` in common hold the handlers both loaders call; each loader's entry point wires its own events to them.
- **NeoForge registration:** everything registers in the first `RegisterEvent`, while NeoForge has the registries open; the
  creative tab registers there too.
- **Ores:** `OreGen.placements()` and `OreGen.biomes()` are now the table. Fabric adds the features in code; `tools/make_assets.py`
  writes one NeoForge `add_features` biome modifier per feature (25). `OreGenTest` checks they agree.
- **Sized conductor recipes** (224) use each loader's component ingredient (`fabric:components`, `neoforge:components`), so
  `make_assets.py` writes a copy into each loader's resources instead of the shared data.
- **Two vanilla calls replace Fabric API extensions:** `ItemStack.getCraftingRemainder()` became the item's
  `getCraftingRemainder()` (firebox ash), and the creative tab's contents take a plain consumer.
- **Jars:** `wildcatter-fabric-0.2.0.jar` (Team Reborn Energy bundled) and `wildcatter-neoforge-0.2.0.jar`. The version went to
  0.2.0.
- **CI and releases:** each push builds both jars, runs the unit tests and the NeoForge smoke scenes, and keeps the jars as a run
  artifact. Pushing a tag `v*` publishes a GitHub Release with both jars and `wildcatter_kit.zip` (`.github/workflows/release.yml`).
- **NeoForge smoke scenes** (`./gradlew :neoforge:runGameTestServer`), all 5 passing (4 of ours and vanilla's `always_pass`):
  - `registers`: 297 blocks, 368 items, 116 block entity types, the creative tab, and the Field Manual's pages;
  - `lamp`: the starter plant lights a lamp at 120 V, 0.50 A, 60 W;
  - `plug`: a NEMA 5-15 plug hands 930 W to a stand-in (92,975 in 100 ticks, the same as on Fabric);
  - `ores`: 25 biome modifiers, and the salt domes are in the plains.
- **Fabric:** the full client game test passes on the new layout. Two scenes (`plugs`, `solverBudget`) now teleport first so they
  run on their own.

## Phase 9a: Team Reborn Energy on Fabric (Phase 9 waves in ROADMAP)
- **Plugs feed Team Reborn Energy:** a plug adapter now hands its power to any Team Reborn Energy storage beside it (Tech Reborn's
  machines, batteries and cables, and other Fabric mods on that API). It finds them through `EnergyStorage.SIDED` on the face toward
  the plug (`energy/TeamRebornEnergy`).
  - Its demand is what the storage would take this tick (a simulated insert that is never committed).
  - The rate is 1 W = 1 E/t, sharing the FE setting (`wattsPerFe` in `config/wildcatter.properties`).
  - The readout names the unit: "delivering 930 W (930 E/t)".
- **Bundled:** Team Reborn Energy 5.0.0 (MIT) is inside the jar, so nothing extra needs installing. Tech Reborn is recommended,
  not required.
- **Tick by tick:** the plug still measures and draws on its 4-tick interval, but now hands the energy over evenly each tick. Before,
  it gave four ticks' worth at once, and a real storage, which caps each insert at its own rate, would have lost three quarters.
- **The game test's stand-in machines** are kept apart from the real providers (`EnergyBridge.registerForTest`), so a scene no
  longer wipes the Team Reborn one.
- **Game test `teamRebornEnergy`:**
  - a gold block registered as a real `SimpleEnergyStorage` (1,000,000 E, 1,000 E a tick) beside a NEMA 5-15 plug gains
    92,975 E in 100 ticks: 930 E/t, all the starter plant gives;
  - the plug draws 8.0 A at 116 V;
  - once the storage is full, the plug draws 0 A.
  - `plugs` and `threePhase` still pass.
- **Unit test:** `HostEnergyTest` (the per-tick split loses no remainder).
- Resolves Phase 2's "Not done: the Team Reborn Energy adapter".

## Phase 8e: refuelling, the spent fuel pool and radiation (spec: docs/multiblocks/fuel-handling.md); Phase 8 done
- **Tim's decisions:**
  - the refuelling machine runs the outage;
  - irradiated fuel is never an item;
  - the vessel counts as inside its primary shield;
  - real dose and effects.
- **REFUELLING MACHINE** against the vessel (30 kW, 480 V), working at cold shutdown, a move a minute:
  - REFUEL: the 3 most-burned to the pool, a 3-move shuffle, 3 fresh from its hopper;
  - DEFUEL: the whole core, which clears a damaged one.
- **FUEL TRANSFER SYSTEM** between the machine and the **SPENT FUEL POOL**.
- **The pool's controller** (75 kW, 480 V):
  - it digs a 6 × 6 m pool 12 m deep in front of itself, lines it and fills it;
  - its 36 racks are drawn with their assemblies (Cherenkov blue while hot);
  - each assembly keeps its own decay heat;
  - cooling is on circulating water; without it the pool heats, boils off and loses its top layers; make-up off a pump.
- **Radiation** (`fluid/Radiation.java`, `radiation/RadiationTicker`, `DoseData`):
  - **Sources, each second:** each assembly (1,000 Sv/h at 1 m per MW of decay heat), the vessel at power (10 mSv/h at 1 m at
    full power), a damaged core (1 Sv/h at 1 m), and ore (by the scintillometer's counts).
  - **Shielding:** water ×1/300 and other full blocks ×1/1000 a block, with inverse square.
  - **Dose:** lifetime, and an acute dose that halves daily.
  - **Effects:** nausea and weakness from 1 Sv, hunger and slowness from 3, wither from 6, death by radiation at 8.
  - **DOSIMETER** (Ludo icon): it reads and alarms.
- **Game test** `fuelHandling`, on a 16 m platform (the test world's floor is only a few blocks down):
  - REFUEL puts 3 in the pool and leaves 3 fresh and 6 burned in the core;
  - the full pool reads 0 µSv/h over it, and drained about 1.7 Sv/h;
  - 3.5 Sv brings weakness and slowness;
  - DEFUEL empties a damaged core and clears it (12 in the pool).

## Phase 8d: after the trip (spec: docs/multiblocks/reactor.md, "After the trip")
- **Tim's decisions:**
  - two waves (8d after the trip; 8e refuelling, spent fuel and radiation);
  - losing cooling ends in core damage and a hydrogen blast;
  - radiation in 8e is a real dose with real effects.
- **Decay heat:**
  - Way-Wigner fitted as 8 groups on the power history: 6.2 % at 1 s, 1.2 % at 1 h, 0.64 % at a day;
  - it takes its place in the core's heat, so a tripped core keeps heating the RCS.
- **Xenon:** iodine and xenon (real yields and half-lives, σφ 6×10⁻⁵ /s); −2,800 pcm at full power, peaking about 8 h after a trip
  near −4,600 pcm.
- **COOLDOWN** on the console (between TRIP and 0):
  - the dump valves lower the temperature at 55 °C/h (compressed as the heat-up), with auxiliary spray lowering the pressure;
  - below 177 °C and 2.9 MPa the RHR PUMP (200 kW, 4.16 kV) and the RHR HEAT EXCHANGER (UA 60 kW/°C, on circulating water with its
    temperature) take it to 50 °C.
- **Core damage:**
  - the RCS boils off through the PORVs, and below 60 % of its water the core uncovers;
  - the uncovered share of the decay heat goes into the fuel;
  - over 1,200 °C CORE DAMAGE latches (no reset);
  - Zircaloy and steam make hydrogen, and at 18 kg (4 % of 5,000 m³) it explodes, power 6, at the pressurizer;
  - the vessel and head (blast resistance 1,200) stand.
- **Game test** `reactorSafety`, by hooks:
  - the decay heat about 5-6 % just after the trip, with xenon at −2,800 pcm;
  - at 170 °C the RHR takes about 9 MW into a CW pump's water;
  - drained to a quarter with the fuel at 1,150 °C, the cladding fails, about 19 kg of H2 explodes, and the vessel stands.

## Phase 8c: the hyperbolic natural-draft cooling tower (spec: docs/multiblocks/natural-draft-tower.md)
- **Tim's decisions:**
  - the tower rises lift by lift as it is fed;
  - a tenth of the real concrete (the steel about a hundredth, as offered);
  - the wet bulb from the biome.
- **The tower** (`fluid/CoolingTower.java`, unit-tested):
  - Calder Hall's 88 m;
  - a hyperboloid from 30 m radius at 8 m to a 17 m throat at 62 m and 20.8 m at the top;
  - 54 lifts of 1.5 m, 0.40-0.18 m thick;
  - about 430 gray concrete and 44 steel ingots in all, and 64 plastic sheets for the fill.
- **Cooling:**
  - wet bulb (Stull) + 8 °C × (Q / 104 MW)^0.6, with a 2-minute lag;
  - evaporation Q / 2.45 MJ/kg (42 kg/s at full load), blowdown a quarter of it, make-up off a pump;
  - no fans, no power.
- **NATURAL-DRAFT COOLING TOWER:** the controller at the basin's edge, the centre 34 m in front.
  - Materials by a hopper on top.
  - It builds the basin and digs and fills a 3 m pond, then the raker columns, then the shell lift by lift, then the fill.
  - Invisible TOWER SHELL blocks make it solid.
  - CoolingTowerRenderer draws it as one smooth model, with a cloud plume over the top while it rejects heat.
- **The loop:**
  - COOLING_WATER carries its temperature;
  - a CW pump in the tower's basin gives the tower's cold water, and in a lake 20 °C;
  - the condenser counts the inlet temperature for its vacuum and gives the warmed water back on its south side, to the
    tower's riser on top of the controller.
- **Game test** `coolingTower`:
  - the basin is poured from the hopper and the pond fills;
  - the shell blocks stand at 30, 17 and 20.7 m at 8.5, 62.5 and 87.5 m;
  - a pump in the basin draws from the tower;
  - at about 102 MW the cold water sits 8.0 °C over a 10.5 °C wet bulb, with 41.6 kg/s of evaporation, and the plume is on;
  - screenshot.

## Phase 8b: the turbine island (spec: docs/multiblocks/turbine-island.md)
- **Tim's decisions:** the hyperbolic natural-draft tower gets its own wave (8c), so 8b cools the condenser once-through from a
  lake or river; a new 60 MVA hydrogen-cooled generator. Waves: 8b turbine island, 8c tower, 8d SCRAM and safety. The
  construction crane idea is in ROADMAP Ideas.
- **The cycle** (`fluid/TurbineCycle.java`, unit-tested):
  - 6.1 kg of steam a kWh, so 86 kg/s makes 51 MW and the condenser rejects 104 MW into about 2,500 kg/s of water;
  - the vacuum follows the water (5.6 kPa at full flow, the output down 1 % a kPa over it);
  - the low-vacuum trip at 25 kPa.
- **Parts:** TURBINE VALVE CHEST, HP TURBINE (controller), MOISTURE SEPARATOR REHEATER, LP TURBINE, 60 MVA TURBINE GENERATOR
  (13.8 kV), MAIN CONDENSER, 2 CONDENSATE PUMPS (150 kW, 480 V), DEAERATOR (30 t, make-up off a pump), 2 MAIN FEED PUMPS
  (500 kW, 4.16 kV), HP FEEDWATER HEATER (FEEDWATER out), 2 CIRCULATING WATER PUMPS (300 kW, 4.16 kV; their bottom 3 cells in
  water), UNIT AUXILIARY TRANSFORMER (13.8/4.16 kV, 7.5 MVA). Reuses the 3,000 A breaker and bus duct and the 60 MVA GSU.
- **How it runs:**
  - the steam generators offer steam above a 7.0 MPa header;
  - the governor admits what the generator can use, and a steam dump up to 40 % takes the rest;
  - the turbine rolls up in 2 minutes and the generator synchronizes.
- **Trips:**
  - low vacuum, deaerator low, a generator trip or a lost part trips the turbine (sneak-click to reset);
  - the steam generators hear of it through their steam pipe, and the reactor trips on it above P-9 (50 %).
- **New product:** COOLING_WATER (kg).
- **Game test** `turbineIsland`:
  - **Layout:** the 8a island with the turbine island beside it, the CW pumps in a dug pool.
  - **Power:** the 16.5 MW gas turbine on the 13.8 kV bus as offsite power, the UAT feeding the plant's 4.16 kV and, through a
    unit substation, 480 V.
  - **Checks:**
    - both coolant pumps, both CW pumps and the feed and condensate pumps run;
    - at 25 % the turbine rolls, its generator synchronizes and carries more than 8 MW;
    - cutting the CW pumps' power trips the turbine on low vacuum while the reactor stays up below P-9.

## Phase 8a: the nuclear island and the boron plant (specs: docs/multiblocks/reactor.md, boron.md)
- **Tim's decisions:** a core of 9 full-height assemblies at real power density (155 MW thermal), burnup 100x, boric acid plus
  control rods, boron carbide rods (hafnium would take about 1,400 h of the 7f line), two loops. Waves: 8a the nuclear island,
  8b the turbine island, 8c SCRAM and safety.
- **Plant model** (`fluid/Reactor.java`, `PointKinetics.java`, `Steam.java`, unit-tested):
  - six-group point kinetics with the prompt jump (a +100 pcm step gives the inhour 55 s period);
  - reactivity from the assemblies' burnup (+16,000 pcm fresh, -380 a GWd/t), the rods' S-curves (6,000 pcm in 5 RCCAs), boron
    (-10 pcm/ppm), Doppler and the moderator;
  - the loops' heat balance, two steam generators (UA 3.2 MW/K each; 43 kg/s of steam each at full power), the pressurizer
    (heaters and spray hold 15.51 MPa), IAPWS-IF97 saturation;
  - trips: manual, high flux (109 % and 25 % below P-10), low flow, a pump lost above P-8, pressurizer pressure low and high,
    overtemperature, steam generator low-low level, CRDM power lost;
  - heat-up compressed 80x while the plant gains heat: cold to hot standby on the pumps in about half an hour.
- **Parts:** REACTOR VESSEL (controller; assemblies and RCCAs by right-click while cold), CRDM HEAD, REACTOR CONSOLE (sneak-click
  steps the setpoint, TRIP resets on leaving it), STEAM GENERATOR (STEAM out of its top, FEEDWATER in on its east side, dump
  valve), REACTOR COOLANT PUMP (500 kW, 4.16 kV, flywheel coastdown, sneak-click stop/start), PRESSURIZER (150 kW heaters), CVCS
  SKID (fills, borates and dilutes; boric acid bags by hopper; sneak-click steps the boron target), AUX FEED PUMP (5 kg/s, 56 kW).
- **Boron plant:** BORAX DISSOLVER (borax, sulfuric acid, water: 50 kg/h of boric acid), BORIC ACID CRYSTALLIZER (25 kg bags),
  B4C FURNACE (boric acid and coal: 5 kg/h of B4C in 1 kg trays), RCCA BENCH (9 trays and 20 kg of steel make an RCCA).
- **New products:** STEAM and FEEDWATER on pipes, in kg. **New items:** Boric Acid, B4C Pellets, RCCA (Ludo icons, 3 credits).
- **Game tests:**
  - `boron`: two sets on a busway; the dissolver and crystallizer run on borax, a tank of acid and pumped water; a bag, a tray
    and an RCCA drop.
  - `reactor`: the island laid out as the spec. Pump A on three sets through a backward unit substation, pump B on two more
    through a second one, 480 V on a busway, the aux feed pump on its own set. At hot standby (1,500 ppm, 15.5 MPa) both pumps
    run; with the shutdown bank out the core goes critical and holds 2 % (3.1 MW) on bank D at about 104 steps, the dump valves
    venting while the aux feed holds the levels; one pump stopped below P-8 it runs on; a manual trip drops the rods and power;
    after a reset, cutting the CRDM head's power trips it.

## Phase 7g: the fuel plant (spec: docs/multiblocks/fuel.md)
- **LEU UF6 to 17x17 assemblies**, 50 kg of U an hour: a pellet is 5.31 g of UO2 (8.19 x 9.83 mm, 95 % dense), 372 to a rod
  (1.741 kg of U), 264 rods to an assembly (459.6 kg of U, about 667 kg) in about 9 hours; a 30B is about 31 hours and 3.3
  assemblies (`fluid/Fuel.java`, unit-tested).
- **Tim's decisions:** helium from a recovery unit at the gas plant; steel for the stainless and Inconel hardware, Zircaloy mid
  grids from the pilger mill; the assembly a placeable 1x5x1 block.
- **The line**, each part against the next (uranium passed in kg):
  - UF6 AUTOCLAVE (30B by hopper, the empty out below);
  - IDR KILN (0.141 m3 of hydrogen and 0.45 L of water a kg of U; 0.504 kg of HF counted);
  - GRANULATOR, PELLET PRESS;
  - SINTERING FURNACE (1,750 C, 15 m3/h of hydrogen);
  - PELLET GRINDER (0.5 % swarf counted);
  - ROD LOADER (tubes and steel by hopper), ROD WELDER (helium at 2 MPa, 0.4 L a rod; cylinders by hopper; a 264-rod rack);
  - ASSEMBLY FIXTURE (25 thimbles, 6 spacer grids, 20 kg of steel), the assembly out below.
- **FUEL ASSEMBLY:** a 1x5x1 block, 214 mm square and 4.06 m tall; its item carries it.
- **Helium:** a HELIUM RECOVERY UNIT at the gas plant treats pipeline gas (0.4 % helium) on its pipe and fills 8.5 m3 cylinders.
- **The pilger mill** has a third die, grid strip, for spacer grids.
- **New items:** Spacer Grid, Helium Cylinder and its empty, Fuel Assembly. Ludo icons for the first three (3 credits).
- **Game tests:**
  - `fuel`: two sets on a busway down the line, each part's stub connected once the sets are on line; a steam reformer on LPG
    for the hydrogen. Every stage runs (the kiln at 57 kg of U an hour, the furnace at 237 kW); the fixture builds its skeleton
    from its hopper; with 263 rods in it and one in the rack, an assembly drops into the hopper under it; the emptied 30B drops
    under the autoclave; an assembly stands for the screenshot.
  - `gasPlant`: the helium recovery unit treats the pipeline gas and fills a cylinder; the gas set still runs through it.

## Phase 7f: the zirconium plant (spec: docs/multiblocks/zirconium.md)
- **Zircon to Zircaloy-4 tube**, 10 kg an hour: an assembly's 264 cladding tubes (0.404 kg, 9.50 x 0.57 mm, 3.85 m) and 25 guide
  thimbles in about 12 hours (`fluid/Zirconium.java`, unit-tested).
- **Tim's decisions:** two waves (7f zirconium, 7g fuel); the Kroll magnesium recycled through a magnesium cell, make-up chlorine
  from a chlor-alkali cell, make-up magnesium from chlorinated magnesia.
- **The line**, each part against the next (metal passed in kg):
  - MINERAL SEPARATOR: heavy mineral sand by hopper; 90 % of the zircon on; ilmenite, rutile and monazite counted.
  - CHLORINATOR: 2.01 kg of zircon, 0.58 kg of carbon (coal or charcoal) and 3.11 kg of chlorine a kg of Zr; SiCl4 and CO counted.
  - HAFNIUM COLUMN: extractive distillation; 2 % of the metal out as HfCl4.
  - KROLL REACTOR: magnesium from the magnesium cell at 25 % excess, the MgCl2 back to it.
  - VACUUM DISTILLATION: the excess magnesium back to the cell.
  - VAR FURNACE: 1.5 % tin, 0.21 % iron, 0.10 % chromium from tin ingots, iron ingots and chromite.
  - EXTRUSION PRESS (4.16 kV), PILGER MILL (sneak-click for cladding or thimble dies), VACUUM ANNEAL FURNACE (tubes out below).
- **The chlorine and magnesium loop:**
  - MAGNESIUM CELL: 14 kA at 5 V on a rectifier, 5.7 kg of Mg an hour, its chlorine out on a pipe.
  - MAGNESIA CHLORINATOR: tops up the cell's MgCl2 from magnesia, coal and chlorine.
  - CHLOR-ALKALI CELL: 6 membrane cells at 2.8 kA on a rectifier, 21 kg of chlorine and 6.7 m3 of hydrogen an hour from rock salt
    and pumped water; caustic soda counted.
  - Both cells reuse the 7d RECTIFIER.
- **New pipe product:** chlorine (kg). **New items:** Zircaloy-4 Cladding Tube and Guide Thimble, with Ludo icons (2 credits).
- **Game test** `zirconium`:
  - two paralleled 480 V sets on an 800 A busway down the line; two more through a backward unit substation to 4.16 kV;
  - the chlor-alkali cell makes 19 kg/h of chlorine and 6 m3/h of hydrogen at 2.5 kA; the magnesia chlorinator starts the
    magnesium cell (12.6 kA, 5.1 kg/h of magnesium);
  - the separator and chlorinator run on their feeds; with a hook's kilogram in each stage, every next stage runs (the Kroll
    reactor at 9.8 kg/h of sponge);
  - a nearly rolled cladding tube, and after a die change a guide thimble, drop into the hopper under the anneal furnace;
  - without the hafnium column the Kroll reactor reads NOT BUILT.

## Phase 7e: the centrifuge cascade (spec: docs/multiblocks/enrichment.md)
- **Natural UF6 to 4.95 % LEU** in 30B cylinders, tails at 0.25 % in 48Ys: 10.20 kg of feed and 7.81 SWU a kg of product
  (`fluid/Enrichment.java`, unit-tested).
- **Tim's decisions:** Urenco TC-21 class machines; each does 1000x its real 100 SWU a year (11.4 SWU/h) at the real 50 kWh a
  SWU, so 571 kW a machine.
- **Parts**, all on one steel-pipe header:
  - CENTRIFUGE (1 x 6 x 1, joined by its top cell). A cascade needs at least 11, one a stage (Glaser's 1.74 separation factor);
    more add separative work. This is the written exception to "never tile identical blocks".
  - FREQUENCY CONVERTER (12.47 kV): drives up to 12 machines at 1,225 Hz along the header, the land rig's drive load at 0.95 PF.
  - FEED STATION (30 kW heater, 480 V): a natural 48Y by hopper, sublimed onto the header, the emptied 48Y out below. It runs the
    cascade: TOO FEW STAGES, NO DRIVE, TOTAL REFLUX (a take-off missing or not ready), NO FEED.
  - PRODUCT TAKE-OFF (15 kW) into 30Bs and TAILS TAKE-OFF (30 kW) into 48Ys, empties by hopper, full ones out below.
- **New pipe product:** UF6 (kg). **New items:** Empty 30B Cylinder, 30B Cylinder of UF6 (4.95 % LEU), 48Y Cylinder of UF6
  (0.25 % tails), with Ludo icons (3 credits).
- **Game test** `cascade`:
  - the peaker's aeroderivative, 60 MVA GSU and 60 MVA substation transformer to 12.47 kV for the converter; a diesel set's 480 V
    for the stations;
  - ten machines read TOO FEW STAGES (10 of 11); the 11th starts it;
  - the converter takes 6.07 of its 6.28 MW at 12,258 V (301 A); the machines run at 1,184 Hz and the cascade does 121 SWU/h,
    23 kg of LEU UF6 an hour from 234 kg of feed;
  - a 30B and a tails 48Y topped up by hook are finished and drop into their hoppers; a nearly empty feed 48Y comes out empty;
  - breaking the product take-off gives TOTAL REFLUX; breaking the converter gives NO DRIVE.

## Phase 7d: conversion to UF6 (spec: docs/multiblocks/conversion.md)
- **The Metropolis dry process at quarter size:** 12 fluorine cells, 0.075 kg of U a second, a 48Y cylinder (12,501 kg of UF6)
  about every 31 hours.
- **Main line**, each part against the next:
  - REDUCTION REACTOR: yellowcake drums in by hopper, empties out under it, 8.47 g of hydrogen a kg of U.
  - HYDROFLUORINATOR: 0.51 kg of anhydrous HF a kg of U, the magnesia's share included.
  - FLUORINATOR: 0.18 kg of fluorine a kg of U, the feed's impurities kept as ash.
  - COLD TRAPS and DISTILLATION COLUMNS, at 480 V.
  - CYLINDER FILLER: empty 48Y cylinders in by hopper, full ones out.
- **Reagent plants:**
  - STEAM REFORMER: 30 Nm3/h of hydrogen on gas or LPG at 0.45 m3 a m3, with pumped water.
  - HF KILN and HF COLUMN: fluorspar by hopper, acid and fuel by pipe, anhydrous HF out. Per kg of HF: 2.165 kg of fluorspar,
    2.6 kg of acid, 5 MJ of fuel; anhydrite counted.
  - RECTIFIER (4.16 kV, 12-pulse, 0.95 power factor) against the FLUORINE CELL HOUSE: 12 KF·2HF cells at 9 V and 6 kA,
    108 V DC, about 0.67 MW from the AC side.
- **The rectifier and the AC grid:** it is an AC load on the grid (Tim's decision). The DC side is worked out in it and shown in
  its readout. (Superseded in 10b1: the DC side is now on the solver, with bus bars.) `circuit/DriveLoad` now takes a power factor and a frequency floor; the rectifier has no floor, so it does not shed
  load.
- **New pipe products:** hydrogen, anhydrous HF (no stock tank holds it), fluorine.
- **New items:** Empty 48Y Cylinder and 48Y Cylinder of UF6, with Ludo icons (2 credits).
- **Game test** `conversion`:
  - three diesel sets, two of them paralleled through a backward unit substation to 4.16 kV for the rectifier;
  - the rectifier drives 5.4 kA (603 kW at 59.5 Hz), the cells make 44 kg/h of fluorine, the line carries uranium through;
  - a cylinder topped up by hook to 3 kg short is finished and drops into the hopper under the filler;
  - the empty drum comes out under the reducer.

## Phase 7c: the ISR wellfield (spec: docs/multiblocks/isr-wellfield.md)
- **Roll-front deposits** (`well/RollFront.java`): seed-placed crescent bands of uranium in desert and badlands sandstone.
  - Shape: arcs of radius 40-80 blocks, 15-30 blocks across and 3-6 thick.
  - Depth: top at y -10 to -45.
  - Grade: 1 % eU3O8 along the middle, 0.3 % at the edges.
  - Recovery: 70 % can be won. One pool per front, shared by every producer on it (`RollFrontData`).
- **Gamma log:** both rigs complete a hole through a front as an **ISR well** and report the ore's depth, thickness, grade and
  grade-thickness against a 0.6 m·% cut-off. A miss logs background. A front above an oil field comes first.
- **ISR wells:** an injector by default. A **submersible pump** (7.5 hp, 480 V) clicked in makes it a producer of 2.5 L/s.
  - The head grade follows the 5-spot: 500 mg/L U3O8 for four injecting injectors within 25 blocks, times the oxygen they
    carried, the ore's grade at the well, and the front's uranium left.
  - A full pattern makes about 4.5 kg of U3O8 an hour.
- **Header house** (480 V, its drives a variable load of 5.6 kW a running pump):
  - runs up to 25 producers on its west pipe;
  - sends their water to the IX columns;
  - doses the returning lixiviant with oxygen from the **oxygen generator** (PSA, 50 Nm3/h, 25 kW) against it;
  - holds back 1 % bleed;
  - meters each injector on its east pipe its share.
- **IX columns:** take 95 % of the uranium onto resin, elute it in salt brine (rock salt by hopper), and acidify the eluate
  (sulfuric acid by pipe). The eluate, at 15 g/L U3O8, goes to the mill's precipitation tank, which now takes it as well as
  strip liquor.
- **Left out** (ROADMAP Ideas): CO2, monitor wells, groundwater restoration, a bleed disposal well, satellite IX.
- **Game test** `isrWellfield`:
  - set on a forced seed, the ground turned to desert;
  - the rig logs 5.9 m·% of ore and leaves an ISR well;
  - a 5-spot round it reads 493 mg/L with four injectors;
  - the IX columns elute and the precipitation tank precipitates the eluate;
  - with the oxygen generator gone the head grade falls to 0.
- **Art:** the submersible pump icon from Ludo (1 credit).

## Phase 7b: the sulfuric acid plant and the uranium mill (specs: docs/multiblocks/acid-plant.md, uranium-mill.md)
- **Acid plant** (150 t/day, sulfur-burning double contact, double absorption), eight true-size parts:
  - the SULFUR MELTER (sulfur by hopper, melted on the boiler's steam, or gas or LPG from cold);
  - the DRYING TOWER, the MAIN BLOWER (250 kW, 4.16 kV) and the SULFUR FURNACE, which form by touching;
  - the WASTE HEAT BOILER (water from a pump; 1.2 t of steam a tonne of acid);
  - the CONVERTER with the INTERPASS and FINAL ABSORBERS against it: 98.5 % acid out to a stock tank.
  - 0.57 kg/s of sulfur makes 1.74 kg/s of acid. Without the interpass absorber the plant converts 95 % instead of 99.8 %, and the
    stack's SO2 rises 25 times.
- **Uranium mill** (White Mesa size, 20.8 kg/s of ore), eight true-size parts:
  - JAW CRUSHER (75 kW, 480 V) against the SAG MILL (522 kW, 4.16 kV);
  - LEACH TANK (70 kg of acid and 1.5 kg of pyrolusite a tonne of ore, 93 % of the uranium dissolved);
  - CCD THICKENER (wash ratio 2.5, tailings counted);
  - SX CIRCUIT (amine in a 10,000 L kerosene charge, stripped with salt water to 25 g/L);
  - PRECIPITATION TANK (magnesia), with the YELLOWCAKE DRYER (0.5 MW of gas or LPG) and the DRUM FILLER against it.
  - A drum holds 400 kg of concentrate at 80 % U3O8. About 7½ uranium-ore blocks fill one; the mill fills about 4.4 an hour.
    Pitchblende is fed no faster than SX takes its uranium, so it saves digging, not time.
- **New pipe products:** molten sulfur, burner gas, sulfuric acid, ore and leach slurry, pregnant and strip liquor. The slurries
  and liquors carry their uranium with them; no stock tank holds them, nor molten sulfur. A stock tank holds acid.
- **New items:** Magnesia (a furnace calcines magnesite), Empty Drum (208 L, from iron), Yellowcake Drum. Ludo icons, 4 credits.
- **Hoppers** feed the parts that take solids (sulfur, ore, pyrolusite, salt, magnesia, drums) from on top; the drum filler's
  full drums go to a hopper under it.
- **Water pumps** now fill any machine that takes water (the boiler, the final absorber, the SAG mill and the thickener).
- **Big motors:** a 250 kW blower or a 522 kW SAG mill started across the line takes a lone 500 kW diesel set down on
  frequency, as it would in real life. The game tests parallel two and three sets. Soft starters and drives stay in ROADMAP Ideas.
- **Game tests** `acidPlant` and `uraniumMill`, with screenshots. The mill test gives the drum filler 380 kg by a hook rather than
  wait 14 minutes for a first drum; the mill's own yellowcake finishes it.

## Phase 7a: the ores (spec: docs/ores.md; Phase 7 waves in ROADMAP)
- **27 real ores**, one block each, drawn as the mineral in its real host rock:
  - uranium ore and pitchblende, native sulfur, rock salt, potash, fluorspar, heavy mineral sand, bauxite;
  - galena, sphalerite, tin, nickel, chromitite, platinum reef, molybdenum;
  - magnesite, dolomite, graphite, borax, spodumene, barite, bentonite;
  - silver, tungsten, cobalt, manganese, phosphate rock.
  - Mining one gives its mineral item (Fortune gives more, Silk Touch the block). The tooltip shows the formula, this ore's
    grade and kg per block, and the real grade.
- **Grades** (`ore/Ore.java`): about ten times a typical mined grade, capped at 80 % of the ore mineral; ores already richer keep
  their real grade. For example, uranium ore is 2 % U3O8 (46 kg a block) against 0.05-0.4 % real, and pitchblende is 60 % (1,500 kg).
- **Worldgen:**
  - ordinary veins by biome and depth (uranium in desert and badlands sandstone, pitchblende below y -40, tin and molybdenum
    in mountains, heavy mineral sand on beaches, bauxite in jungle and savanna, phosphate in oceans);
  - three new feature types:
    - salt domes (a rock-salt column from y -60 under a calcite cap that is 30 % native sulfur, about 1 in 150 chunks);
    - evaporite beds (rock salt and potash under deserts);
    - the Bushveld reef (chromitite seams with the platinum reef above).
  - Only new chunks get ores.
- **Smelting:** galena → lead, cassiterite → tin, sphalerite → zinc, acanthite → silver (furnace and blast furnace). The other
  minerals wait for their plants (7b onward, or ROADMAP Ideas).
- **Scintillometer:** reads counts per second (50 cps background, plus uranium within 16 blocks by grade over distance squared)
  and says which way the count rises.
- **Art:** 27 ore textures and 32 icons from Ludo (61 jobs of two variations each, 61 credits; prompts in `art/source/PROMPTS.md`).
  `tools/make_assets.py` reads the ore list from `Ore.java`.
- **Game test** (`ores`): every vein and feature is placed into rock and leaves its ore; every ore drops its mineral; a
  furnace smelts galena to lead; the scintillometer reads background over barren rock and points east at a pitchblende pod.
  Screenshots: the ore wall and a cut salt dome.

## Phase 6c: the Ludo art pass on the plant parts
- **New Ludo art** (12 pictures plus regenerations, about 17 credits; prompts in `art/source/PROMPTS.md`):
  - six materials: acoustic enclosure panel with a louvre band, inlet filter house face, transformer radiator fins, bar grating,
    finned-tube cooler bundle, high-temperature aluminium paint;
  - eleven decals: a turbine control panel, an ANSI arc-flash label, the NFPA 704 diamond, a DANGER FLAMMABLE GAS sign, an
    equipment rating plate and six ASME A13.1 pipe markers (crude oil, natural gas, LPG, produced water, diesel, steam).
- **Repainted** (no shape, footprint or game change):
  - both gas turbine enclosures (louvre panels, a control panel), both inlet houses (filter face);
  - the stacks, flare riser, fired heaters and cracking furnace (high-temperature aluminium);
  - the air-cooled condenser, overhead condenser, topping air cooler and compressor coolers (finned tube);
  - platforms and the condenser fan deck (grating);
  - the radiators of every big transformer (radiator fins);
  - generator and GSU rating plates, arc-flash labels on the generator breakers;
  - NFPA diamonds on the stock tank, separator and LPG bullet, a flammable gas sign on the flare and gas compressor;
  - pipe markers on outlet stubs with a fixed product.
- `tools/process_materials.py` keeps a fixed window for patterned materials (the cleanest window would drop the pattern) and
  crops one copy of a repeated sign.
- **Game tests:** the repainted scenes reran for screenshots (`lease`, `gasPlant`, `toppingUnit`, `crudeUnit`, `cracker`,
  `gasTurbine`, `peaker`).

## Phase 6b: the aeroderivative peaker (Phase 6 done)
- **Spec:** docs/multiblocks/aeroderivative.md (approved with the plan, 2026-11-01).
- **45 MW aeroderivative** (LM6000 class, the same `GasTurbineBlockEntity` sized by its package): the 5 x 5 x 14 package, its inlet
  house, a 56.25 MVA two-pole generator and its exhaust stack. Heat rate 8,800 kJ/kWh (41 %), 25 % of full fuel at no load. It
  purges 30 s, runs up in 60 s and on line may take 25 % of its rating a minute: full load about five minutes after the button.
- **Ramp limit:** both turbines now ramp after their breaker closes; the 16.5 MW unit at 10 % a minute (the 6a gap).
- **3,000 A:** a 15 kV 3,000 A, 50 kA generator breaker, and a 3,000 A size of the generator bus duct (three 1,200 A ducts and a
  copper block).
- **60 MVA transformers** (`Gsu60TransformerBlock`, 6 x 6 x 5, 10 %Z): the GSU (13.8 kV delta / 138 kV grounded wye) and a
  substation transformer (138 kV delta / 12.47 kV grounded wye), so a big plant's output reaches distribution.
- **Models:** `aeroderivative.py` (seven parts). No Ludo credits.
- **Game test:** new scene `peaker` (the purge on the diesel set's 4.16 kV; onto the dead bus over 3,000 A duct; held at its ramp;
  four load banks, 37.3 MW at 59.0 Hz; 138 kV on the line; 6.95 kV a phase on the 12.47 kV side). `gasTurbine` still passes with
  the ramp (its hook finishes the ramp too).

## Phase 6a: the gas turbine and combined cycle
- **Specs:** docs/multiblocks/gas-turbine.md and combined-cycle.md (approved with the plan, 2026-10-31).
- **13.8 kV tier:** `Voltage.GENERATOR` (11 kV IEC), 1,200 A generator bus duct, the 15 kV generator circuit breaker (it carries the
  generator's synchronizing breaker and the bus voltage transformers that ground-reference the bus), and a 25 MVA 13.8/138 kV GSU.
- **Gas turbine** (`TurbinePartBlock`, a unit by touching): the 16.5 MW package (controller; heat rate 10,230 kJ/kWh, 30 % of full
  fuel at no load), its inlet house, a 20.6 MVA generator and an exhaust stack or HRSG. A 300 hp 4.16 kV starter purges it a
  minute, it lights and runs up in two (a real start is ten), and its generator synchronizes. Pipeline gas, diesel or kerosene.
- **Combined cycle:** the HRSG (65 % of the exhaust heat, two minutes to warm), the 5 MW steam turbine with its 6.25 MVA generator,
  the air-cooled condenser (149 kW of fans) and the feed pump (45 kW). 21.7 MW at 46 % at full load.
- **Load bank:** 0 to 10 MW at 13.8 kV in 1 MW steps.
- **Models:** `gas_turbine.py` (six parts and the GSU) and `combined_cycle.py` (five parts). No Ludo credits.
- **Game test:** new scene `gasTurbine` (no 4.16 kV: no start; the purge; at speed onto the dead bus; 138 kV on the GSU; 10 MW then
  20 MW on two load banks; the steam turbine on line; 42 % at 18.5 MW). The purge-to-speed and the HRSG's warm-up are skipped by hooks.

## Phase 5c and 5d: crude units, the cracker and plastic
- **Specs:** docs/multiblocks/crude-units.md and cracker.md (approved with the plan, 2026-10-31). The Phase 1 one-block refinery and
  its columns are removed (refinery-tower.md is marked replaced); in an older world they vanish.
- **Assay:** a light sweet, WTI-like crude gives 2 % LPG, 30 % gasoline, 14 % kerosene, 26 % diesel, 28 % residue; a gas well's
  condensate 5/60/15/15/5. A separator fed only by gas wells now gives condensate; with an oil well on it, the condensate blends
  into the crude.
- **Early: 1,000 bbl/day topping unit** (`RefineryPartBlock`, a unit by touching): the 12 m column (controller), a 3 MMBtu/h
  vertical heater that burns the unit's own LPG first (it covers it on crude), and the pump and air-cooler skid (11.2 kW, 480 V).
  1.84 L/s at full voltage.
- **Late: 20,000 bbl/day crude unit:** the 30 m column (controller, it names missing parts), a 60 MMBtu/h box heater on gas or
  residue, side strippers, the overhead fin-fan condenser (75 kW, 480 V; its four fans turn), the 250 hp 4.16 kV charge pump
  against the heater and the electrostatic desalter (40 kW, 480 V) against the pump. 36.8 L/s. The column starts the desalter and
  fans first and the charge pump after them.
- **Cracker and polyethylene** (`PetrochemPartBlock`): the cracking furnace (1 L/s of LPG or gasoline, dilution steam from water a
  pump brings it, fuel gas or LPG), the quench tower (pyrolysis gasoline and fuel oil drop out), the 300 hp 4.16 kV cracked gas
  compressor, the cold box with a refrigeration compressor against it (ethylene, fuel gas back to the furnace, LPG back to the
  bullet), the polyethylene reactor (75 kW, 480 V) and its 250 hp 4.16 kV pelletizer: about 13 plastic sheets a minute into a bin
  a hopper can empty. Cracked gas carries its make-up from the furnace to the cold box, since it depends on the feed.
- **Fuels:** residue (fuel oil, 38 MJ/L) fires the steam plant burner and the crude heaters; gasoline, kerosene, condensate and
  residue burn in fireboxes; stock tanks hold every liquid product except LPG.
- **Models:** `crude_units.py` (nine parts, two fan rotors) and `cracker.py` (six parts). No Ludo credits.
- **Game tests:** new scenes `toppingUnit` (crude from a tank, four product tanks filling, the heater on its own LPG), `crudeUnit`
  (built part by part, two diesel sets synchronized, 35 L/s on residue fuel, a part removed stops it) and `cracker` (LPG to plastic
  sheets in a hopper); `gasPlant` checks a gas well's separator gives condensate; `refining` keeps its genset and MV fault checks.

## Wire heating (approved by Tim, 2026-10-31)
- A conductor's temperature rise follows the square of its current with a 120 s time constant and burns out at 1.8 times its
  rated rise (`energy/WireHeat`, unit-tested). A motor's start no longer burns the cable sized for it; twice the rating burns in
  about 37 s, three times in about 13 s, a dead short in a quarter of a second. The game tests that burn wires now wait for it.

## Tools
- `tools/process_art.py` and `tools/make_assets.py` generate textures, models, blockstates, lang, loot, tags and recipes. Run `process_art.py` first, then `make_assets.py`.
- `tools/make_assets.py` clears the folders it writes. **Anything made by hand goes in `art/hand_assets`**, which it copies back, or the next run deletes it without a word. 13b's first commit lost Phase 12's lined cell and pump assets this way; they were restored in 13c, and the client suite now fails if any block or item has no assets.
- `tools/recipe_sheet.py <dir>` renders every recipe as a crafting-grid picture.
- `tools/process_materials.py` makes the model materials, and `tools/models/build.py [part]` builds the true-size part models and previews (make_assets runs it).
- The client game test takes screenshots into `build/run/clientGameTest/screenshots/`.
- The test client needs the Mac display awake. Run `caffeinate -u -t 30` just before launching it.

## Known gaps
- Phase 18 (phosphate): the `tsp_bag`, `dap_bag`, `mop_bag` and `extractant_drum` icons are script-drawn `*_placeholder`. The ten parts
  use the mod's materials. Not built: the DEPA-TOPO second cycle (ammonium carbonate strip, AUC to the uranium calciner; it needs Phase
  16a and 16d1, on the `phase16` branch, planned in `docs/multiblocks/uranium-from-phosphate.md`), so the uranium stays in the strip;
  the filter acid's uranium is not carried as an assay; fluorosilicic acid recovery; the hemihydrate process; potash by flotation;
  Farmings reading `c:fertilizers` (Farmings' side). See `docs/questions/phase18-phosphate.md`.
- Phase 17a (ferroalloys): the seven bulk bag icons (`ferrochrome_bag`, `ferromanganese_bag`, `ferrosilicon_bag`, `moly_oxide_bag`,
  `scheelite_concentrate_bag`, `ferromolybdenum_bag`, `ferrotungsten_bag`) are script-drawn `*_placeholder` (the Ludo bulk bag with a
  swatch). Not built: chromium metal (needs 17c2's chromic oxide), an automatic var controller, capacitor switching inrush and harmonic
  resonance, a slag item, a frozen-furnace drop, Soderberg paste as its own item (green anodes stand in). Tailings' dense-medium plant
  keeps its placeholder ferrosilicon recipe. See `docs/questions/phase17-ferroalloys.md`.
- Phase 16 (uranium routes and plant operations, 2026-11-10; `docs/questions/phase16-uranium.md`): script-drawn icons
  `u3o8_drum_placeholder` (16a), `fluorinator_ash_drum_placeholder`, `synthetic_fluorspar_placeholder` (16b1). The chlor-alkali cell on
  potash has no scene (16b1). A fuel assembly stood up as a block keeps no assay and reads 4.95 % when broken (16c1). The producer's CO2
  factor is unit-tested, not run in a scene (16d1). Not built in 16a: uranyl peroxide precipitation, ammonia recovery, ASTM C967 impurity penalties.
  16b2, 16c2 and 16d2 are planned in ROADMAP, not built.
- Phase 14 (the ammonia plant): the `gauze_pack` and `emulsifier_pail` icons are Ludo art since the art wave (2026-11-09); no
  placeholder is left. The six parts use the mod's materials. Not built: the ammonia leak hazard, AN solution setting in a cold
  pipe, an IBC for matrix, a PGM refinery (a gauze pack is crafted straight from platinum reef ore and a worn pack is not recovered),
  the purge gas, nitrous oxide abatement. The client scene `ammoniaPlant` passes (filmed 2026-11-09). See
  `docs/questions/ammonia-plant.md`.

- Phase 14b-14d (gas and power quick wins): no new script-drawn textures (the six parts use the mod's materials). Not built: line pack
  and pressure on the sales line, a delivery station fed from the set it supplies (the scenes run it on its battery), the condenser's fans
  staged at part load, biomethane upgrading, a small biogas set. The acid turbine reads 1.44-1.52 MW in the scene, the low end of the
  spec's 1.4-1.8 MW, because its 480 V fan and pump motors run at about 95 % voltage on two diesel sets. An unloaded 13.8 kV delta behind
  a backward transformer can solve to nonsense currents (the `biogas` scene sets its load bank before the set starts). The client scenes
  `salesGas`, `acidSteam` and `biogas` are written and compiled, not yet run or filmed. See `docs/questions/phase14-gas-power.md`.
- Art (2026-11-09 art wave): no `*_placeholder` texture is left in the jar. Every placeholder named here before now has Ludo art
  (`art/source/PROMPTS.md`, "Art wave"), and `tools/make_assets.py` leaves out any placeholder nothing names. `memory_card` has its own icon.
  Still drawn by script without the `*_placeholder` name: the breaker levers and the insulator caps, glaze and pins (flat colour
  markers; `docs/questions/art-wave.md`, 7).
- Phase 13i: Tailings' sorter routing and its TBM's course are not on a signal (Tailings' Known gaps).
- Phase 13 after the PLC gap wave (2026-11-09; `docs/multiblocks/plc.md`, "The PLC gap wave", "Planned, not built";
  `docs/questions/plc-gaps.md`, 12). Every signal family now runs under a real PLC in a server scene. Cable cores, the supply's 10 A and a
  rack's single owner are enforced. The modules are drawn, and ladder jumps, labels, RETURN and FUNCTIONs and FBD wire routing are built.
  - **Still open, appearance and sound:**
    - no drawn HOA position on a device;
    - the horn borrows a note block sound;
    - the module items borrow `decal_relays` and the control key the trial key, with their recipes proposed;
    - the rack is drawn in the mod's materials, with no new texture and no placeholder.
  - **Still open, the NeoForge screen:** the NeoForge jar has not opened the PLC screen in a world by hand.
  - **Still open, SFC:**
    - ladder and FBD conditions and macro steps in a chart;
    - no chart safety check;
    - no SFC reset or pause.
  - **Still open, the card and PLCopen:**
    - the card carries no retained values and does not boot a blank CPU;
    - XML loses comments in variable blocks;
    - import has no undo;
    - a jump wire is removed only from its target;
    - PLCopen import has been tried only on hand-written files.
  - **Still open, the supply's mains draw:** it stays 100 W whatever its 24 V load.
  - **Still open, process models first:**
    - pipe pressure, vibration and dampers;
    - CT and PT voltage on a cable;
    - speed references on soft starters and MCC drives;
    - a valve's air per stroke;
    - boiler pressure and three-element drum control;
    - a gas turbine's load setpoint and a steam turbine's valve;
    - feedwater and a well choke on a signal;
    - a unit's own flows and temperatures;
    - a crane's lift start and its load and boom readings;
    - HMI piping graphics and pages.
  - **Still open, contactors:** FAIL_STOP comes only from a welded-contactor test hook.
- Phase 12: the process water pump borrows the water pump's model.
- Phase 10b1:
  - the bus bars' standoffs stand on the floor of their own cell, so a run carried up in the air shows them hanging (no
    supports or structure yet);
  - the chlor-alkali caustic soda is still only counted (fixed in 10b2: it is a pipe product).
- The protection gap wave (2026-11-09; what is left of the Phase 10d1-10d3 gaps; decisions in `docs/questions/protection-gaps.md`):
  - no arc-fault (AFCI) breakers: an arc's signature is not on a phasor solver (decision 22);
  - no series ratings; a molded-case breaker past its interrupting rating still opens (only the switchgear and the 15 kV feeders
    fail to open past theirs);
  - the ATS's two sources share one frequency in the solver, so its in-phase monitor and sync check see no slip in play (a test hook
    gives slip); no load shed before transfer;
  - relays are untimed in the protection race (they act as before), relays carry no coordination readout of their own, and the
    coordination check stops at transformers;
  - nothing in play sticks a breaker's mechanism (a test hook does); the switchgear main and the relay cabinet have no 50BF above them;
  - 87T and 87G see faults made with the grounding set only; internal winding faults are not modelled;
  - a 13.8 kV bus fed only through a GSU's delta, with no generator breaker closed on it, has no ground reference (its voltages to
    earth read wrong; line to line is right);
  - the 87B slope and the 51V dial are fixed; the relay settings use the IEEE curves only (no IEC curves);
  - the new settings have been exercised in NeoForge server scenes, and the Fabric client scenes that changed have not been run.
- Phase 10c3 (wind, ground bearing and relocation on crane rail done in the plant-crane-gaps wave):
  - it carries no heavy-duty jib, and no hook arrangement for its 5,000 t: its chart is capped at the 3,200 t main hook;
  - a tandem lift's hooks keep to the line between the two cranes; the part does not turn as two real hooks would turn it;
  - it travels only along one straight row of crane rail; no turning, no switches;
- Phase 10c2 (wind, ground bearing and boom and jib collision done in the plant-crane-gaps wave):
  - the cranes are placed whole: no erection, climbing or boom assembly (planned: `docs/questions/plant-crane-gaps.md`, 12);
  - the crawler does not travel;
  - a crawler has only its main boom and the 104 t counterweight chart: no derrick, superlift or luffing jib;
  - the mast, jib and boom are drawn, not blocks: a slew is checked against blocks and tower crane masts before a lift, but two
    tower cranes' jibs are not checked against each other, and nothing stops a player walking through them;
  - a world saved mid-lift puts the load back in the chest, and the lift is made again;
- Phase 10c1:
  - a blueprint does not record what blocks hold or how machines are set, nor entities; it cannot be edited, only surveyed again;
  - the site marker stands at the origin corner, so a blueprint with a block in that corner cannot be finished until the marker
    is broken;
  - the ghost draws blocks as coloured cubes, not their models;
  - a site's ghost shows only while its marker's chunk is loaded on the client, so a big plant seen from far beyond its marker
    (past the server's view distance) shows none;
- Phase 10b3b (auxiliary power and live cut-out done in the plant-crane-gaps wave):
  - the pots change their own anodes, standing in for the pot tending machine until 10c's cranes; the tapping crucible item stands
    in for the crucible and its transport vehicle, and the metal does not cool in it;
  - a new pot starts hot and full (no bake-in or start-up), and potroom magnetics, heat balance, bath chemistry and cathode life are
    not modelled; C2F6 is not counted;
  - spent pot lining has no treatment and no use (planned: `docs/questions/plant-crane-gaps.md`, 13);
  - the pot's 6 x 6 x 12 cell box stands in for the potroom building, which is not drawn;
- Phase 10b3a:
  - residence times are not modelled: the 18 h coking, the drums' 12-24 h fill and the furnace's two-week firing run as steady
    flow (the drums switch at a tonne); left for Tim, with a plan: `docs/questions/plant-crane-gaps.md`, 11;
  - most of the metallurgical coke has no user in the mod beyond carbon and burner fuel (coke oven gas and coker gas now also run
    gas engines and gas turbines);
  - no ammonia, light oil or gas desulfurizing at the by-product plant; no vacuum unit ahead of the coker;
  - the rodded anodes are used by 10b3b's pots;
- Phase 10b2:
  - the Bayer liquor's first fill is waived: the evaporator starts with 10 minutes of strong liquor;
  - each stream carries its alumina in a fixed share. Silica, soda in the mud and liquor strength are not tracked, and the soda
    loss is one make-up figure;
  - residence times (36 h precipitation, 1 h digestion) are not modelled: the plant runs as a steady flow with 5 s holds (left for
    Tim, with a plan: `docs/questions/plant-crane-gaps.md`, 11);
  - the alumina and AlF3 now go to 10b3b's pots; the ton containers and the chlorine still have no buyer.
- Phase 9c: Mekanism has not been tried: it is not yet released for 26.3. Plugs were checked against a real NeoForge energy
  handler on a stand-in block.
- Phase 9b: the NeoForge client loads with no errors (`:neoforge:runClient`), but no in-world screenshot was taken: quick-play into a
  prepared world did not open it, and window capture is not permitted here. It has not been played. NeoForge's build tools need a JDK 21 beside the JDK 25. The `solverBudget` scene's 5 ms limit can trip when the machine
  is busy (6.1 ms once while a download ran; 1.0 ms alone).
- Phase 9a: plugs feed Team Reborn Energy on Fabric only. Mekanism for 26.3 is
  unreleased. Tech Reborn itself has not been tried in a real play session, only the API with a stand-in storage.
- Phase 8e: the pool digs 13 m down from its controller: near the world's floor (or a superflat world) it cannot. Irradiated
  fuel has no casks yet (ROADMAP Ideas). Dose is a point-source line-of-sight sum; there is no contamination. The test sets the
  core's history and the moves' time by hooks.
- Phase 8d: hydrogen goes into a notional 5,000 m³ of building air; there is no containment building, recombiners or igniters
  (ROADMAP Ideas). A damaged core waits for 8e's refuelling machine. The game test reaches damage by hooks, not hours of boil-off.
- Phase 8c: the tower's shell is drawn by one renderer and made solid by invisible blocks; no visible block model. Humidity is a
  three-step proxy, as the biome's downfall is private. The game test builds the shell by hook rather than over 13 minutes, and
  does not run the whole plant on the tower (turbineIsland still uses a lake).
- Phase 8b: the turbine island's game test cools once-through from a pool. The turbine island's game test starts on a gas
  turbine's 13.8 kV bus, not a 138 kV grid line. The governor follows the generator's last output, so a sudden load step is met
  over a few seconds.
- Phase 8a: without the turbine, steam is vented and the plant runs at a few percent on the aux feed. The feedwater is counted at
  227 C even from the aux feed. Starting the second 500 kW coolant pump direct on line on the same three-set island as the first drops the island. On
  4.16 kV drives two run on one three-set island (10a, the `drives` scene), but the reactor scene still feeds each pump its own
  sets and unit substation. The boron scene's single set on a busway, which never closed its breaker, was the solver failure fixed in v0.4.1:
  re-tested 2026-11-07, one set closes and runs the plant at 470 V (the scene still uses two).
- Phase 7 is complete through 7g. Tim checks the fuel chain in a real session. The fuel test connects each part's 480 V after
  the sets are on line: closed onto the whole plant at once, the first set tripped.
- Helium is taken from all pipeline gas at 0.4 %; real fields differ (most hold far less).
- The zirconium line runs as a steady flow: the real Kroll retort and VAR heats are batches. Its game test gives each stage a
  kilogram by hook rather than waiting hours for the metal to arrive. The plant's 480 V load needs a busway: a single cable
  burns.
- Phase 6 is complete (6a and 6b); Tim checks the peaker in a real session. The turbines' generator couplings do not turn on
  screen (the condenser fans and feed pump do). The test skips each turbine's start and ramp with hooks.
- The aeroderivative's exhaust stack is drawn as an octagon; its SCR housing is blank (no NOx chemistry).
- Phase 5 is complete with 5c and 5d (the lease, the gas plant, the crude units, the cracker). Tim checks each in a real session.
- Gasoline and kerosene have no engine of their own yet: kerosene and diesel are for the Phase 6 gas turbine.
- The client `cracker` scene still tops up the furnace's water with a hook; a pump filling a furnace is exercised in the NeoForge
  scene `cracker_pump` (misc gaps wave).
- Phase 10a: the 250 hp charge pump does start direct on line on one 500 kW set (172 A peak at 4.16 kV); the older gap saying it
  needed two sets no longer holds (the crude unit scene still uses two). The soft starter and drive look the same in every state
  (no run lights on their models), and the MCC's soft-starter and drive buckets show the combination starter's door. The drive's
  motor side is solved at the island's frequency; only its readout and the motor's ramp follow the drive's own.
- Pipe arms join every face of a lease part, though only its nozzles connect; the Power Meter on a pipe says what a run carries.
- The land rig's power limit works on frequency, so on a small set it holds back well below the set's rating until the
  frequency control catches up (30 s); a real rig's power management also reads the gensets' kW.
- The land rig's motors are inside the drive house's load; the cables to the pumps and drawworks are drawn (misc gaps wave), not
  circuits of their own.
- There is no blowout preventer stack in the cellar yet: it comes with well control, Phase 15b2 of `docs/multiblocks/drilling-mud.md`.
- Wall-mounted gear sits 0.55 m up its cell; a player wanting it higher places it a block up.
- Pumpjack and rig collision is the still frame: the moving beam, crank and pipe do not push anything (drawing-only; questions 15).
- The pumpjack's linkage and the rig's head have been checked in test screenshots, not watched in a real play session.
- A lead runs only from an insulator to a cutout or a pole unit; the cutout-to-transformer link is still by touching faces.
- A pole transformer placed by command keeps its old facing rule, so in some test scenes its brackets face away from the pole (planned: protection-gaps 21).
- There are no guy wires (planned: protection-gaps 20; Tim to choose what an unguyed pole does).
- The frame-rate check runs at the client's 120 fps cap, so it shows no drop but not how much headroom is left.
- The test skips the steam plant's five-minute warm-up with a hook (it checks the drum starts rising, then jumps to pressure).
- A small genset cannot drive enough fault current through the 20 MVA substation to reach real relay settings; the test scales the relays down. In play, such a source's own protection clears a feeder fault first.
- The 1/0 and 4/0 MV cable ampacities are from the NEC 310.60 table family but not checked line by line (500 kcmil, 450 A, is).
- Genset sounds and turning parts have been checked in test screenshots and logs, not heard in a real play session.
- The new art (cooling tower, fuse, breakers, poles) has been checked in test screenshots, not in a real play session.
- The starter 120 V turbine can carry its full rating on one phase (decided: it stands in for a single-phase set).
- Gas fields are large, so a gas well lasts hours.
- Only the fused cutout shocks on touch; enclosed gear is dead-front by design.

- Plant and crane gaps wave (2026-11-09): the quench sump's FULL, the stabilizer's own-LPG reboiler and the pots' bath alumina
  without auxiliaries have run only in NeoForge server scenes. A pot without auxiliary power going to an anode effect has not been
  run in a test (it needs line current; the client `potline` scene runs the powered case).
- Misc gaps wave (2026-11-09): the steam plant's feedwater line, the land rig's cables, the 1 MW alternator and the new spool icons
  have been checked in model previews and NeoForge server scenes, not yet in the client (no client scene was run in the wave).
  Coker gas keeps a gas set's full rating (its heavier hydrocarbons' knock is not modelled). No new script-drawn textures.

## Ideas not started
Ideas live in one list: `docs/ROADMAP.md`, "Ideas (not scheduled)". Phase 10 took switches and panels, the art, drives, the DC tier
and cranes from it.

## Phase 12: the lined tailings cell and the process water pump (2026-11-06)

Spec: docs/multiblocks/tailings-cell.md. Written ahead of a plan, then approved by Tim as built. `cell_liner` and `decant_tower` make a cell of any shape; the tower finds its basin, fills it with visible `settled_tails`, returns water on a water pipe, and reads BREACHED or FULL. The uranium mill's thickener has an optional tails outlet. `process_water_pump` is the water pump on a 37 kW, 480 V motor. Scene `tailingsCell`. Tailings' scenes will take tails over a real pipe.

Phase 12 closed on 2026-11-06: the full client suite passed (BUILD SUCCESSFUL in 2h 47m) and it is released as v0.4.0. No phase is CURRENT until Tim picks the next.

## Phase 13: PLCs (2026-11-07)
Spec: `docs/multiblocks/plc.md`, decided by Tim 2026-11-07: nine waves.

**13a, the language core (2026-11-07):** IEC 61131-3 structured text, compiled to a bounded bytecode and run by a CPU in plain Java, with no game face yet. 31 unit tests run real programs: the scan's input image, every statement and type, timers and counters against the scan's clock, tasks at their intervals, a PID holding a level with anti-windup and bumpless transfer, forcing, retained values, the watchdog stopping a runaway program. Nothing a player can place yet: that is 13b.

**13b, the hardware and the editor (2026-11-07):** a PLC cabinet with CPU, 16-channel input and relay output modules; a 24 V control supply; multi-core control cable and junction boxes; pushbutton, selector, e-stop, pilot light, stack light, horn; a keyed HAND / OFF / AUTO selector on every starter (standalone, MCC bucket, soft starter, drive). The cabinet's screen is a real code editor, terminals, online tags and forcing, and a diagnostic buffer. Game scene `plc`, filmed. In HAND a starter is exactly as before.

**13c, ladder and FBD (2026-11-07):** projects of program units in structured text, ladder and function block diagrams, calling each other; both graphical languages drawn with a toolbar and mouse; live power flow from the CPU's own values; forcing from the ONLINE tab; online change; large downloads in pieces. 18 more unit tests; the `plc` scene now draws a ladder, runs the motor from it, makes an online change, adds an FBD and forces an input.

**13d, charts, device blocks, card and files (2026-11-07):** the fourth language, SFC, with parallel and choice branches, all eleven action qualifiers and named action bodies; `MOTOR_CTRL` and `VALVE_CTRL`; a memory card; a PROJECT tab exporting and importing PLCopen XML and structured text. 46 more unit tests. The new `plcSfc` scene draws a conveyor start-up with the real mouse and runs it on three real starters, stops it, faults it, carries it on a card, and exports and reimports it, running. See `docs/multiblocks/plc.md`, "As built: 13d".

**13e, analog, drive speed, valves (2026-11-07):** 4-20 mA signals with AI8, AO4, RTD4 and HSC2 modules; transmitters and real numeric readings; a drive's speed reference, with flow following speed and power its cube; control, on-off and motor-operated valves on water and steel; instrument air; a water day tank. The new `plcAnalog` scene holds a tank's level with a PID on a real drive-fed pump, and fails a loop, the air and a valve's power. See `docs/multiblocks/plc.md`, "As built: 13e".

**13f, power equipment and HMI (2026-11-07):** signals on the power equipment, a network cable and COMM module, and an HMI panel with a laid-out screen, ISA-18.2 alarms and trends. The new `plcPower` scene starts two real sets from an HMI, closes them in sync, loads them, sheds past a limit and shifts their shares. See `docs/multiblocks/plc.md`, "As built: 13f".

**13g, process plants and the reactor (2026-11-07):** a run enable on every plant unit, and rods, boron and a manual trip on the reactor, its trips still beating them. The `reactor` and `toppingUnit` scenes now run under real programs.

**13h, networks (2026-11-07):** remote I/O drops, PLC-to-PLC tag exchange, an HMI by station, and a marker as a register. The new `plcNetwork` scene runs a starter on a drop 60 blocks away and cuts the network.

**13i, the release and Tailings (2026-11-07):** the full client suite passed (BUILD SUCCESSFUL in 2h 53m, 0 unsolved circuits) and Phase 13 is released as v0.5.0. The suite found that a world could not be saved with a program unit over 64 KB in a cabinet, broken since 13c; units are now saved in pieces, and the `plc` scene writes a 150 KB unit as a save does. Tailings' plant motors, sorter gates, mine switches, block signals and TBM cable junction take PLC signals on v0.5.0 (Tailings' `DESIGN.md`, "PLC signals").

Phase 13 closed on 2026-11-07. No phase is CURRENT until Tim picks the next.

v0.5.1 (2026-11-07), one fix found by Tailings' playthrough:
- **A control cable reaches as far as it says.** A cabinet's walk along its cables stopped after 8,192 blocks looked at, and the blocks beside each cable counted, so a trunk was cut off after about 1,600 cables: Tailings' site, 2,076 cables to 37 devices, reached 26. Now only cable and junction boxes count. The `plcNetwork` scene lays a 2,447-cell trunk and lights a lamp at its end; on 0.5.0 it fails.
- The full client suite passed on 0.5.1 (2 h 53 min).

v0.4.1 (2026-11-06), two fixes found by Tailings' playthrough; the full client suite passed on them (2h 45m):
- **The solver no longer gives up on a floating bus.** A busway's neutral on a dead bus has low resistance and no path to ground, so the small leak each node carries rounded away and the factorisation stopped with "Could not solve the circuit". The solve for that whole group was then dropped, so the generator's synchroniser never read the bus and its breaker never closed. The node now gets its leak back. Unit test `aFloatingBusStillSolves`. The warning came 241 times in the v0.4.0 suite's log, on a suite that passed, and 0 times in this one.
- **A process water pump reads its own motor.** Its readout gave "750 W, 110 V" for every pump; it now gives the motor's watts and voltage.

## Phase 14: the ammonia plant (2026-11-09)
Spec: `docs/multiblocks/ammonia-plant.md` (approved by Tim 2026-11-09; "As built" lists the numbers added). Built while Tim was away;
the decisions taken are in `docs/questions/ammonia-plant.md` for him to go through. For Tailings' open-pit blasting (its wave P3).
- **Six true-size parts**, all on the world's industrial voltage except the neutraliser:
  - NITROGEN GENERATOR (PSA, 2 x 3 x 3): 10 Nm3/h of 99.9 % nitrogen, 3 kW;
  - SYNGAS COMPRESSOR (3 x 3 x 5): the reformer's hydrogen and the nitrogen, 3:1, to 150 bar, 40 kW;
  - AMMONIA CONVERTER (3 x 10 x 3), against the compressor: iron catalyst at 400-450 C, its circulator and chiller 10 kW; 15.2 kg/h of
    liquid ammonia (24.9 L/h);
  - NITRIC ACID UNIT (4 x 12 x 4): burns 51.5 % of the ammonia on a platinum-rhodium gauze at 94 % conversion, 45.4 kg/h of 60 % acid with
    process water off a pump; its blower 15 kW; the gauze loses 0.15 g a tonne of HNO3;
  - AN NEUTRALISER (3 x 5 x 3): the rest of the ammonia and the acid to 34.6 kg/h of AN as 85 % solution, on its own heat;
  - EMULSION BLENDER (3 x 3 x 4): AN solution, diesel, emulsifier and a little water to 46.1 kg/h of emulsion matrix (75/18/6/1), 5.5 kW.
- **New pipe products:** nitrogen (Nm3), ammonia (L, only the pressure bullet holds it), nitric acid 60 % (L), ammonium nitrate
  solution (kg) and emulsion matrix (kg). The matrix is the hand-over to Tailings' emulsion truck; boosters and detonators stay in
  Tailings.
- **New items:** Platinum-Rhodium Gauze Pack (20 g, from platinum reef ore) and Emulsifier Pail (20 kg, four from a plastic sheet, a
  diesel bucket and an iron ingot). Both go in by a hopper on top or by hand.
- **The bullet** now holds LPG or ammonia ("Pressure Bullet"; it reads "Ammonia bullet" when it holds ammonia).
- **Readouts:** NO HYDROGEN, NO NITROGEN, NO SYNGAS, COLD, NO AIR, NO AMMONIA, NO ACID, NO WATER, NO GAUZE, NO AN SOLUTION, NO DIESEL,
  NO EMULSIFIER, FULL. PLC points RUN, RUNNING, AUTO and RATE 4-20 mA on the converter, the acid unit and the neutraliser.
- **Field Manual:** two pages, AMMONIA PLANT and AMMONIUM NITRATE AND EMULSION.
- **Tests:** unit test `AmmoniaPlantTest` (5 tests: the spec's rates, the split, the water balances, the gauze and the pail). NeoForge
  server scenes `ammonia_loop` (14.8 kg/h of ammonia into a bullet; NO HYDROGEN without the hydrogen pipe, COLD without power),
  `nitric_acid` (27 kg/h of HNO3 into a stock tank, the gauze losing platinum; NO AIR, NO GAUZE) and `an_emulsion` (34.6 kg/h of AN,
  46.1 kg/h of matrix into a stock tank; NO DIESEL, NO ACID). All 8 NeoForge scenes pass (5 before).
- **Client scene `ammoniaPlant`** (filmed): the whole chain on one diesel set, each readout in a screenshot, the matrix tank, then the
  hydrogen pipe cut (NO HYDROGEN, then NO SYNGAS). Passed on its first run (2026-11-09): reformer 30 Nm³/h, nitrogen 9.7 Nm³/h, acid 43.8 kg/h of 60 %, matrix in the tank, then NO HYDROGEN and NO SYNGAS.

## Phase 14b-14d: gas and power quick wins (2026-11-09)
Specs (DRAFT, decided on recommendation, built while Tim was away; not approved): `docs/multiblocks/sales-gas.md`,
`docs/multiblocks/acid-plant-steam.md`, `docs/biogas.md`, each with an "As built" section. Phase 14 was already the ammonia plant
(now called 14a), so the plan's 14a-14d are renumbered 14b-14e. Decisions: `docs/questions/phase14-gas-power.md`.
- **14b, the sales line:** five true-size parts: METER BUILDING (controller; flow computer, chromatograph, RTU; 2 kW single-phase at the
  low tier), FILTER SEPARATOR, ULTRASONIC METER RUN, REGULATOR SKID, PIPELINE TAP. Sneak-click sets RECEIPT or DELIVERY. An off-map line
  per dimension (`SalesGasLine`, saved): receipts book m3 and GJ less 2 % fuel; deliveries put pipeline gas out of the regulator skid
  while the account holds energy (0.2 % to the line heater). 5 m3/s a station. Readouts: OFF SPEC: RICH, WATER, CO2; NOT BUILT: NO
  METER RUN / NO TAP / NO FILTER / NO REGULATOR; NO POWER on the RTU battery; METER FAILED after 8 hours; NO GAS ON THE LINE; the run
  ticket in m3, GJ and MMBtu. PLC points RUN, RUNNING, SHUT IN, AUTO, FLOW and HEATING VALUE 4-20 mA. Closes the gap "Gas has no sales
  pipeline yet".
- **14c, the acid plant's steam:** an ACID SUPERHEATER (3 x 6 x 3) against the converter takes the waste heat boiler's steam (a new STEAM
  nozzle on its top) and the 5 MW turbine against it makes about 1.6 MW at part load; the melter's coils take their share first; the
  feed pump touching the boiler returns 90 % as condensate; LOW STEAM TEMPERATURE trip when the converter stops; through the generator
  breaker and the unit auxiliary transformer onto the plant's 4.16 kV bus, it carries the plant with the diesel sets stopped.
- **Solver:** a generator or genset closing onto a live bus takes the bus's angle to the nearest 30 degrees (`Synchronizer.angle`), so a
  machine behind a delta-wye transformer parallels with the others; without it the acid turbine and the diesel sets drove about 2 MW
  round between them. Machines with no such transformer between them stay at 0 and share load as before.
- **14d, biogas:** `Product.BIOGAS` ("biogas, 60 % CH4", 23 MJ/m3). Another mod offers it through `FluidNode` on steel pipe. A gas set
  burns it after separator and pipeline gas, derated to 850 kW (its governor holds it there) at 407 m3/h; fireboxes burn it by heating
  value; the steam plant burner reads NEEDS PIPELINE GAS; the sales line refuses it. Ships in 0.7.0.
- **Field Manual:** three pages, SALES LINE, ACID PLANT STEAM, BIOGAS.
- **Tests:** unit test `GasPowerTest` (7 tests). NeoForge server scenes `sales_gas`, `acid_steam`, `biogas` with a stand-in gas source
  block in the test mod (another mod's `FluidNode`). Client scenes `salesGas`, `acidSteam`, `biogas` written for filming
  (`WILDCATTER_ONLY=...`), not yet run.
- **14e, the Mekanism check,** stays a plan: Mekanism for 26.3 is not released.

## The protection gap wave (2026-11-09; built while Tim was away; decisions: `docs/questions/protection-gaps.md`)
Closes the Known gaps of Phase 10d1, 10d2, 10d3 and Phase 2's distribution, except guy wires and the command-placed pole transformer's
facing (planned, questions 20 and 21) and AFCI (left out: question 22).
- **Relay settings (10d1, 10d2):** every feeder relay (switchgear section, breaker relay cabinet, 15 kV feeder breaker) has a settable
  pickup (5 to 3,000 A), curve (IEEE C37.112 MI, VI, EI), time dial (0.5 to 15) and 50 (off, 4 to 20 x), set from its front panel with
  the Power Meter: right-click the relay to move the cursor, sneak and right-click to step the selected setting (`circuit/RelaySettings`).
- **Switchgear (10d2):** racking takes 15 s a position and can be done from a PLC (RACK IN, RACK OUT); 50BF opens the rest of the
  lineup when a breaker fails to open (beyond 41 kA, or a stuck mechanism) and 86BF holds it; the main carries the lineup's 87B, and the
  grounding set goes on any section's bus.
- **Breaker relay cabinet (10d2):** 79 recloses after a 50/51 trip (2, 15, 30 s; 3 shots on 12.47 kV, 1 on 138 kV; lockout after the
  last; reset after 60 s closed).
- **Generator bus (10d1):** each feeder's relay is settable; a feeder that fails to open trips the whole bus (50BF). 86B transfer-trips
  the 138 kV breakers of a GSU straight off the bus, so the line cannot back-feed a bus fault.
- **87T and 87G (10d1, 10d2):** the 25 and 60 MVA GSUs and the 10 MVA 4.16/138 kV unit have 87T (0.3 pu, 25 % slope; zone to the
  breakers on each side; 86T, reset at the transformer); the big generators have 87G (0.1 pu, 10 % slope), faulted with the grounding set
  on the generator.
- **Grounding clamps (10d1):** ASTM F855 grade 3, 35 kA for 15 cycles; past their I²t they burn off and the conductor reads UNGROUNDED.
- **Panels and switches (10d3):** a shock puts a 1,000 ohm body path to earth, so a GFCI or RCD trips on it; the ATS runs factory timers
  (or the short set) with open, in-phase or closed transition; class J and E-rated fuses are current-limiting with a let-through peak and
  I²t; on a fault only the fastest breakers and fuses act (the protection race); breakers and fuses read the device upstream and how far
  they are selective; the load centre's and panelboard's main handle is drawn ON, OFF or TRIPPED.
- **Distribution (Phase 2):** spans and leads heat on their conductor's rating (10 min time constant) and burn down at 3 x the rated
  rise; 4.16 kV spans on MV conductor spools; leads strung from a line insulator to a cutout or pole transformer; a load centre or
  panelboard with a ground rod is the service (N bonded to G, rod 25 ohm); an under-rated insulator shatters at 50 A·s of arc, even when
  a relay clears the fault at once; a pole transformer or cutout hung on a pole comes down with it.
- **Tests:** unit tests `RelaySettingsTest`, `DifferentialTest`, `TimeCurrentTest`, a span-heat test in `WireHeatTest` (316 in all). NeoForge
  server scenes `switchgear_settings`, `switchgear_bf_87b`, `cabinet_reclose`, `unit_differentials`, `gsu_backfeed`, `clamp_burn`,
  `panel_gfci_shock`, `fuse_let_through`, `ats_timers`, `ats_closed_in_phase`, `span_burns`, `mv_span_flashover`, `house_earth` (21 scenes
  pass). Client scenes changed but not yet run: `switchgear` (racking hook), `busProtection` (a 50BF step), `ats` (short timers).

## The PLC gap wave (2026-11-09; built while Tim was away; decisions: `docs/questions/plc-gaps.md`)
Closes the "Not yet" items of Phase 13b-13h that could be done properly. The rest are planned in `docs/multiblocks/plc.md`, "The PLC gap
wave", "Planned, not built".
- **Limits enforced (13b, 13h):**
  - **Cable:** a control cable is 50-pair, 98 signal cores. A discrete signal takes a core, a loop or pulse a pair, counted along each
    signal's way, with side-by-side runs adding. A landing that finds a cable full is refused, and a cut that leaves signals past the
    cores names the full cable.
  - **Supply:** the 24 V supply holds 10 A, then hiccups (off 1 s and on). The 24 V draw is counted from the CPU, the modules, the inputs
    that are on, the relays and the field devices they power (`ControlDevice.outputAmps`), and the loops.
  - **Racks:** a rack has one owner, the first PLC to reach it, until its connection times out.
  - **Shared points:** a device point wired to two PLCs ORs a discrete output and fails a shared loop.
- **Drawn (13b, 13h):** the modules seated, in the cabinet's and the drop's door window (`client/PlcRackRenderer`), with the CPU's RUN
  lamp and the card.
- **Ladder and FBD (13c):**
  - rung labels, forward jumps and RETURN;
  - a ladder or FBD as a FUNCTION;
  - a clearer error for another program's variable, with the standard's ways (direct address, VAR_GLOBAL AT and VAR_EXTERNAL) tested;
  - FBD wires routed around blocks (`plc.lang.WireRouter`).
- **Relay flags (13f):** TRIP 50, 51, 87B and 50BF on each switchgear section.
- **Fixed:**
  - a lamp or heater on an open relay stayed lit;
  - a firebox held back by its firing rate read BURNING;
  - the rig in AUTO told the player to click the console.
- **Tests:**
  - 18 more unit tests in `PlcGraphicalTest` and `PlcOpenTest`.
  - 14 new NeoForge server scenes, each with a real PLC: `plc_supply`, `plc_cores`, `plc_owners`, `plc_switchgear`, `plc_ats`,
    `plc_gas_turbine`, `plc_loads`, `plc_steam`, `plc_rectifier`, `plc_units`, `plc_crane`, `plc_motor_faults`, `plc_rig`, `plc_rcp`.
    `WILDCATTER_ONLY=a,b` now picks NeoForge server scenes too.
  - Client scenes: `plc` (rack screenshot, a ladder jump and a skipped rung).

## Plant and crane gaps (2026-11-09)
Built while Tim was away, on the best recommendation; the decisions are in `docs/questions/plant-crane-gaps.md` for him to go
through. Branch `plant-crane-gaps`.
- **Wind** (`crane/Siting`, `CraneKind.windLimit`): the weather's wind 10 m up (4 m/s fair, 8 in rain, 18 in a thunderstorm), risen
  by the 1/7 power law to the jib or boom head, against each crane's limit: tower 20 m/s, crawler 9, ring 14. Over it a crane starts
  no lift and holds one under way: `WIND: 21.5 m/s at its boom head, over its 9 m/s limit`.
- **Ground bearing** (`block/CraneGround`): the blocks under a crawler's tracks or the ring's mats read as soils with IBC Table
  1806.2's bearing (rock and concrete 58.6 t/m2, fill 19.5, gravel 14.6, sand 9.8, dirt and clay 7.3); the crawler needs 14.5, the
  ring 30. `SOFT GROUND: ...: set it on gravel, rock or concrete`.
- **Collision:** a lift's slews are swept for the jib (61-63 m up, counter-jib to tip) or the boom (foot to head) against blocks and
  other tower cranes' masts; `BLOCKED: its boom would strike minecraft:obsidian at x, y, z as it slews the load`, looked at again
  every 5 s.
- **The ring crane travels** on a new block, **Crane Rail** (`crane_rail`), laid in a row under its slewing centre: with no lift in
  reach it skids along the rails at 10 m/h to the stop that brings the nearest unfinished site 10 m beyond its least radius, one
  metre at a time, and stops on a block in its path or the rails' end.
- **The quench tower's sump** holds 2,000 L each of pyrolysis gasoline (east face) and fuel oil (west face); full, it reads FULL and
  stops, where before it lost them past 20 L.
- **The stabilizer's reboiler** burns 450 kJ a litre of Y-grade at 80 %: gas, LPG, coker gas or coke oven gas on the west face of its
  base, or else its own LPG (about 2.8 %).
- **Coke oven gas and coker gas** run gas engine sets (counted by heat against natural gas) and gas turbines.
- **The pots' auxiliaries** take 3 kW at the industrial voltage on the front face of each pot's main cell. Without it the feeders stop:
  the pot runs on its bath's 60 kg of alumina, then goes to an anode effect (`NO AUX POWER`).
- **A pot is cut out live** with a sneak-right-click: its shorting bars (1 µΩ) carry the line past it. A frozen pot stays cut out.
- **The steam plant's burner** takes coal from a hopper against any of its cells.
- **Field Manual:** CRANES and HEAVY LIFTS (wind, ground, blocked slews, the rails), THE POTLINE (auxiliaries, cut-out), GAS PLANT
  (the stabilizer's fuel), STEAM CRACKER (the sump), and the gas engine on carbon gases.
- **Tests:** unit tests `SitingTest` (5) and one more in `GasPlantTest`. NeoForge server scenes `crane_siting` (SOFT GROUND on dirt,
  NO SITE on stone, WIND in an 18 m/s gale, BLOCKED by a pillar, BUILDING once it is gone), `ring_travel` (101 m along its rails to
  its site), `quench_sump`, `stabilizer_fuel`, `cog_engine`, `pot_aux`, `burner_hopper`. All 31 NeoForge scenes pass after merging master's 24 (`ring_travel` runs with 240 blocks' padding, as the sales scene does).
- **Client scenes for the lead to run** (compiled, not run here): `cranes` and `heavyLift` (now set clear weather: rain is over the
  crawler's 9 m/s); `potline` (a diesel set's 480 V along both rows' fronts for the auxiliaries; a pot cut out live with the line
  holding 180 kA); the new `ringTravel` at z -9000 (screenshots of the ring crane on its rails); `gasPlant` and `cracker` (readouts changed;
  checks unchanged).
- **Planned, not built:** residence holds, crane erection and assembly, the crawler's jibs and travel, spent pot lining's treatment,
  the BOP stack (with Phase 15b2), and the drawing-only gaps (`docs/questions/plant-crane-gaps.md`, 11-16).

## Misc gaps (2026-11-09)
Built while Tim was away, on the best recommendation; the decisions are in `docs/questions/misc-gaps.md` for him to go through.
Branch `misc-gaps`.
- **Runs drawn between parts** (`kinetic/DrawnRuns`, `client/RunsRenderer`; `KineticRenderer` draws them too): a formed steam plant's
  generator draws the NPS 2 feedwater line from the feed pump's new discharge flange, up 2.1 m over the generator on two posts, to the
  boiler's new feedwater flange (its stop and check valves now on the boiler's south side); the main steam line meets flange to flange
  at the boiler-superheater and superheater-turbine edges in the part models. A formed land rig's substructure draws a 0.45 m tray from
  the drive house's east end along the pumps' east side, with three cables up to each pump motor's terminal box, and a tray up the back
  of the substructure with the drawworks motor's cables. Both controllers now send `formed` to the client.
- **The alternator's frame:** `GensetPartBlock.Alternator`, a `frame` state (kw500, mw1) set by the control panel when its set forms:
  1 m on the 500 kW diesel set, 1.6 m on the 1 MW gas set, reaching 0.6 m back over the gas engine's flywheel housing (the gas engine
  is now drawn 3.4 m long, its turbochargers over the alternator's adapter). Rotors move with the frame.
- **Coke oven gas (`fluid/LeanGas`):** a gas set on coke oven gas is held to 864 kW (5,642 / 6,528 kW, the Profusa coke gas plant), at
  the same heat rate, its readout `COKE OVEN GAS: 864 kW MAX`. The aeroderivative takes a gas only within 10 % of pipeline gas's Wobbe
  index (49.1 MJ/m3): coke oven gas (29.6, 40 % off) is refused with a WRONG GAS readout naming its Wobbe index, coker gas (49.8)
  runs; the 16.5 MW turbine takes both.
- **Art:** `mat_brass` made seamless in `tools/process_materials.py` (one tile's face, offset-blended, the old tone); all eight
  conductor spool icons from Ludo (`art/source/PROMPTS.md`).
- **Tools:** `tools/make_assets.py` had deleted the hand-made crane rail; it is in `art/hand_assets` now, with its name and tags.
- **Field Manual:** GENSETS (the derate on coke oven gas), PEAKER (no coke oven gas, the Wobbe index).
- **Tests:** unit test `LeanGasTest` (3). NeoForge server scenes `cog_derate` (860 kW held against a 1 MW bank), `aero_lean_gas`,
  `cracker_pump` (a furnace filled 0 to 200 L by a process water pump on a diesel set), `alternator_frames`, `drawn_runs`.

## Phase 16: uranium routes and plant operations (2026-11-10; built while Tim was away; decisions: `docs/questions/phase16-uranium.md`)
Plan: `docs/ideas-plan.md`. Branch `phase16`. Nothing changes what an existing world already does.
- **16a, ADU yellowcake** (`docs/multiblocks/adu-yellowcake.md`, "As built"):
  - URANIUM CALCINER (3 x 8 x 3, a mill part): a six-hearth calciner against the precipitation tank and the drum filler, in the dryer's
    place. Burners 0.6 MW on gas or LPG (east face, bottom); rabble drive and off-gas fan 15 kW on the industrial supply, its box on the
    north face of its main cell. FIRING UP for 5 minutes from cold, then up to 0.45 kg of U3O8 a second.
  - The precipitation tank takes liquid ammonia on the east face of its bottom cells: ADU at pH 7.5, 0.2 kg of NH3 a kg of U3O8, held
    apart from the magnesia precipitate. NO AMMONIA, NO CALCINER. Without ammonia the tank runs on magnesia as before.
  - New item U3O8 DRUM (96 %): 384 kg of U3O8, 326 kg of U. The drum filler holds the two concentrates apart.
  - Conversion: the reducer takes both drums; the share from U3O8 drums follows the uranium down the line: 5.65 g of hydrogen a kg of
    U, HF 0.43 kg (no MgF2), ash 0.049 kg.
  - Field Manual page ADU AND THE CALCINER.
  - Tests: NeoForge scene `adu_yellowcake` passes; unit test `UraniumOperationsTest` (16a: H2 derivation, the drums' uranium, one
    ammonia plant feeds 76 kg/h of U3O8).
- **16b1, conversion's by-products** (`docs/multiblocks/conversion-extensions.md`, "As built (16b1)"):
  - CATHODE GAS SCRUBBER (2 x 4 x 2, 10 kW, box north): against the fluorine cell house, its 28.6 Nm3/h of cathode hydrogen to a pipe
    for the reducer; what the pipe cannot take is vented, as is all of it with no scrubber.
  - The fluorinator's bed: RETIRE BED (sneak-click, or a PLC point; also BED FULL and RETIRING): ten minutes, the ash into Fluorinator Ash
    Drums (400 kg, 3 % U), a new bed of 500 kg of fluorspar. BED FULL at 2 t stops it only with `fluorinatorAshBed=true` in
    `config/wildcatter.properties` (new class `Rules`; default off). The leach tank takes ash drums by hopper and leaches their uranium
    with the ore.
  - KOH SCRUBBER (3 x 8 x 3, 15 kW): against the hydrofluorinator; first charge of 2,000 kg of KOH as caustic potash (new pipe product,
    32 %) from a chlor-alkali cell given potash; quicklime regenerates it; CaF2 out in 1 t bags of Synthetic Fluorspar (the HF kiln and a
    new bed take them); SPENT stops the hydrofluorinator.
  - Field Manual pages CONVERSION'S BY-PRODUCTS and KOH SCRUBBER.
  - Tests: NeoForge scene `conversion_extensions` passes; `UraniumOperationsTest` adds the hydrogen, KOH and ash balances.
- **16c1, enrichment operations** (`docs/multiblocks/enrichment-operations.md`, "As built (16c1)"):
  - The feed station's sneak-click steps its assays (product 3.0-4.95 %, tails 0.20-0.30 %); `fluid/Enrichment` works from any pair.
  - New item component `wildcatter:assay` on 30Bs and fuel assemblies (none means 4.95 %); the fuel plant, rod welder and assembly fixture
    carry it; the reactor keeps each assembly's (fresh reactivity 4,000 pcm less a percent below 4.95 %).
  - CASCADE COOLING SKID (3 x 3 x 6, 55 kW): against the header, circulating water in front, warm water out the back; a cascade that has
    had one reads CASING HOT after 5 minutes without cooling and holds its feed.
  - Spin-up and coast-down with `cascadeSpinUp=true` (default off): COASTING machines keep separating as they slow.
  - Field Manual page CASCADE OPERATIONS.
  - Tests: NeoForge scene `cascade_operations` passes; `UraniumOperationsTest` adds the spec's assay table, fresh reactivity, cooling water.
- **16d1, ISR operations** (`docs/multiblocks/isr-operations.md`, "As built (16d1)"):
  - CO2 LIQUEFIER (3 x 4 x 6, 75 kW) against a lime kiln: up to 150 kg/h of its CO2 as liquid CO2 (new pipe product, kg).
  - CO2 STORAGE TANK (3 x 3 x 8, 30 t) against the header house: 0.5 g/L into the injection header; injectors carry the dose and the
    producers' head grade rises by up to 30 %; NO CO2 with the tank empty.
  - The bleed out of the header house's back, one up, as wellfield bleed (new pipe product) to the injection pump and a disposal well.
  - IX columns SATELLITE / ELUTION (sneak-click); RESIN TRAILER (2 x 3 x 6 block, one item carrying its loading, 800 kg of U3O8).
  - Field Manual page ISR: CO2, BLEED, SATELLITES.
  - Tests: NeoForge scene `isr_operations` passes; `UraniumOperationsTest` adds the kiln's CO2, the dose, the CO2 factor, the trailer.
- **End of the wave set (2026-11-10):** the full NeoForge server suite passes, 54 of 54 (53 Wildcatter scenes, 4 of them Phase 16's, and
  vanilla's `always_pass`), twice in a row; a first run failed `mv_span_flashover` once (it passed alone and in both reruns: flaky, not
  Phase 16 code). Unit tests: 360, 2 skipped, 0 failed. The runner can hang and run out of memory after the last scene when vanilla's
  batch happens to run after Wildcatter's (seen with `cascade_operations` alone and with `mv_span_flashover` alone; both had passed).
- **Client scene `aduYellowcake`** (filmed, passes): the ADU line on one diesel set, FIRING UP, the calciner calcining, a 96 % drum, then
  NO AMMONIA; screenshots `wildcatter-adu-*`. The other Phase 16 waves have server scenes only; the client `conversion`, `cascade` and
  `isrWellfield` scenes are unchanged and were not run.

## Phase 17a: ferroalloys (2026-11-10; built while Tim was away; decisions: `docs/questions/phase17-ferroalloys.md`)
- **Furnace transformer** (10 MVA, distribution tier to 150 V, 15 taps) and the **submerged arc furnace** against it: 8 MW at PF 0.80, a
  constant-power load in the phasor solver; HC ferrochrome (2.1 t/h), HC ferromanganese (3.2 t/h), ferrosilicon 75 (0.94 t/h) from
  hopper-fed bins; taps every 2 hours into a bagging station; green anodes as Soderberg paste; ELECTRODES SHORT; freezing; PLC points.
- **Capacitor bank** (3 Mvar): any bus; against the furnace transformer, the feeder's PF rises from 0.80 to 0.935, metered by the
  transformer.
- **Moly roaster** (0.25 t/h of moly oxide; SO2 to an acid plant's converter; scheelite concentrate dried) and **aluminothermic vessel**
  (ferromolybdenum, ferrotungsten).
- Seven 1 t bulk bag items, `c:ferroalloys/*` tags. Field Manual: SUBMERGED ARC FURNACE, TAPPING AND PASTE, CAPACITOR BANKS, MOLY ROASTER
  AND VESSEL.
- Tests: `FerroalloysTest` (7, including the solver's feeder current with and without the bank); NeoForge `ferroalloys`, `alumino`;
  client `ferroalloys` (filmed).

## Phase 18: phosphate (2026-11-10; built while Tim was away; decisions: `docs/questions/phase18-phosphate.md`)
- **18a, fertilizer** (`docs/multiblocks/fertilizer.md`): **phosphate mill** (wet ball mill, 200 kW at 4.16 kV; rock by hopper, water,
  rock slurry out), **phosphoric reactor** (dihydrate attack, 150 kW; rock slurry north, sulfuric acid south) with the **tilting pan
  filter** against it (250 kW at 4.16 kV; filter acid south, gypsum tails west to a lined cell, NO GYPSUM STACK), **acid evaporator**
  (75 kW; 28 % to 54 % P2O5 on process steam, fluorine counted), **granulation plant** (150 kW, 0.8 MW dryer; TSP or DAP by sneak-click,
  1 t bags at a bagging station); **potash dissolver**, **vacuum crystallizer** and **potash dryer** (55, 110 and 75 kW; 5 t/h of MOP, salt
  tails to a cell). 20 t of P2O5 a day. New pipe products rock slurry, filter acid, merchant acid; TSP, DAP and MOP bags tagged
  `c:fertilizers`. The decant tower names phosphogypsum and salt. Phosphate rock and potash ore now have uses.
- **18b, uranium from phosphoric acid** (`docs/multiblocks/uranium-from-phosphate.md`): **acid pretreatment** (45 kW) and the
  **DEPA-TOPO circuit** against it (60 kW): first charge of kerosene and extractant drums (FILLING), then 2.7 kg/h of uranium into the
  strip on iron ingots; treated acid (a new pipe product) on to the evaporator; the acid bypasses the circuit when it cannot take it.
  The second cycle waits on Phase 16. New item: the extractant drum.
- **Field Manual:** PHOSPHORIC ACID, FERTILIZER, URANIUM FROM PHOSPHATE.
- **Tests:** `PhosphateTest` (5); NeoForge `phosphoric`, `potash`, `phosphate_uranium`; client `phosphatePlant` (filmed).
  At the end of the phase (2026-11-10): the full NeoForge server suite, 53 of 53 passed (49 earlier Wildcatter scenes, `always_pass`
  and the three new); unit tests 354, none failed (2 skipped).
- **Existing worlds:** unchanged (question 1); text only: the two ore tooltips, the bagging station's NOT BUILT line, the tower's readout
  when it holds gypsum or salt.
