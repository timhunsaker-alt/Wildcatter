# Steam plant (APPROVED 2026-10-30)

## Real-world counterpart
A small industrial **condensing steam turbine plant**: package water-tube boiler with burner, superheater, turbine,
generator, surface condenser, boiler feed pump and cooling tower. Typical size: a 1 MW turbine-generator on about
10,000 lb/h of steam (about 10 lb of steam per kWh for a small condensing set).

## Values (DECISION: approve or change)
| Item | Value | Real-world basis |
|---|---|---|
| Generator output | 1,250 kVA, 0.8 PF, 1,000 kW | Standard industrial TG set |
| Terminal voltage | Industrial MV: 4.16 kV NA, 3.3 kV IEC | ELECTRICAL_MODEL standards table |
| Boiler | 10,000 lb/h, 150 psig saturated | Package boiler |
| Superheater | to about 400 C | Small industrial superheat |
| Steam use | about 10 lb/kWh | Small condensing turbine |
| Condenser | about 2.5 in Hg absolute | Surface condenser |
| Feed pump | boiler feed pump, motor load | Motor load per ELECTRICAL_MODEL |
| Fuel | coal or gas at the burner | Existing fuels |

## Added while building (PROPOSED, wait for Tim)
| Item | Value | Real-world basis |
|---|---|---|
| Drum water | 2,000 kg; a low drum level trips the plant | boiler low-water cutoff |
| Condensate returned | 90 % with cooling (as the small turbine); the rest is made up from the water pipe | small plants lose blowdown and leaks |
| Minimum fire | 10 % of full fuel while hot and idle | burners hold a low fire |

## Build values (Phase 4 plan, approved 2026-10-30)
- Fuel at the burner: coal (a coal block is 1 m³ = 1,300 kg at 24 MJ/kg, so one coal = 3.5 GJ), charcoal (0.8 GJ), or pipeline gas (1 unit = 1 m³ at 38 MJ).
- 25 % overall from fuel to electricity: about one coal per 15 minutes at 1 MW.
- Cold start to full pressure: 5 minutes (real package boilers take 30 to 60; compressed for play).
- The feed pump is a 3.7 kW (5 hp) 480 V three-phase motor. The plant needs 480 V station service before it can start; a genset can black-start it.
- 2-pole generator: 3,600 rpm at 60 Hz, 3,000 rpm at 50 Hz. Output goes through the GSU (gsu.md).
- Migration: the boiler cube, firebox floor and 27-turbine cube are removed now (Tim, 2026-10-30). The single campfire boiler and the
  120 V turbine stay as the small starter plant.

## Parts
| Char | Block id | Real part | Notes |
|---|---|---|---|
| `B` | `wildcatter:boiler_burner` | Burner and firebox | Takes fuel (hopper, gas line, hand) |
| `D` | `wildcatter:boiler_drum` | Steam drum and water tubes | Holds the water and steam |
| `S` | `wildcatter:superheater` | Superheater bank | Raises steam temperature; more steam energy per unit |
| `T` | `wildcatter:turbine_casing` | Turbine casing and rotor | Passes steam, spins the shaft |
| `G` | `wildcatter:turbine_generator` | Synchronous generator | Makes the 3φ power; **the controller** |
| `C` | `wildcatter:condenser` | Surface condenser | Turns exhaust steam back to water; needs cooling water |
| `P` | `wildcatter:feed_pump` | Boiler feed pump | Motor load; returns condensate to the drum |
| `R` | `wildcatter:pipe` | Steel pipe | Steam and water links between parts |

## Layout (bottom layer first, controller `G` marked, NORTH facing; fixed with the models in Phase 4)
The approved sketch doubled the burner, drum, superheater and turbine, which tiles identical blocks (VISION: never). As built, one of
each part, along the steam path: the burner fires the drum above it, the drum feeds the superheater, the superheater the turbine, and
the turbine turns the generator (the controller). The condenser sits under the turbine exhaust and the feed pump beside it returns the
condensate to the drum.
Layer 1:
```
 B P C .
```
Layer 2:
```
 D S T G
```
Water pipe to the condenser brings makeup water and, through a cooling tower's basin on the same pipe, cooling water.
Turning parts: the turbine rotor and the coupling to the generator (seen from the sides), the feed pump coupling.

## Controller
`turbine_generator` (`G`). It validates the structure, holds the steam, water and fuel state, and owns the three
electrical terminals (A, B, C plus N and G once Phase 1 lands).

## Rotation
Four horizontal facings, set by the controller block's placement.

## Formation
All parts present and connected face to face (or by `R` pipe where the layout shows a gap). The controller checks
every 20 ticks. When formed: structure sound and the generator becomes live.

## Breaking
Removing any part dissolves the structure. The generator output drops to zero. The drum keeps its water; a
spilled drum floods the cells it was in.

## Game test
Build the structure, check it forms, burn fuel, check output rises to rating, remove the condenser and check
output falls (back pressure), remove a part and check it dissolves.

## Migration
Existing boiler, turbine and firebox blocks stay for one release as a deprecated 110 V small plant, then are
removed. The cooling tower becomes part of the plant (see cooling-tower.md).
