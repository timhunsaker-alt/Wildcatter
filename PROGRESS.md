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
