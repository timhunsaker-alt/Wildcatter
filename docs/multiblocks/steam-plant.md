# Steam plant (PROPOSED)

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

## Layout (bottom layer first, controller `G` marked, NORTH facing)
Layer 1 (floor):
```
 B B D D .
 . . . . .
 . P C . .
```
Layer 2:
```
 . . S S T T G
```
(Parts in a line along the shaft: burner, drum, superheater, turbine, generator. Condenser and feed pump sit
under the turbine exhaust. Exact grid to be fixed with the models.)

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
