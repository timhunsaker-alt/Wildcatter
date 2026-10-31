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

## True size (Phase 4.5c, approved 2026-10-31)
Every part is one item that fills its real size (docs/multiblocks/README.md, "Big parts"). 1 block = 1 m. Tim chose a
side-exhaust condenser beside the turbine rather than one under a raised turbine deck.

| Char | Block id | Real part | Size L x W x H | Real-world basis |
|---|---|---|---|---|
| `B` | `wildcatter:boiler_burner` | Burner, forced-draft fan, gas train, flame scanner | 1 x 3 x 3 | Packaged burner on the boiler's front, about 1.2 m deep and 2.5 m tall |
| `D` | `wildcatter:boiler_drum` | D-type watertube boiler: steam drum along the top, mud drum, furnace, convection bank, casing | 4 x 3 x 4 | Package D-type for 4.5 t/h (10,000 lb/h), about 5 x 2.5 x 3.5 m |
| `S` | `wildcatter:superheater` | Superheater bank, flue outlet box, stack stub, main steam stop valve | 1 x 3 x 4 | Rear outlet section of a package boiler |
| `T` | `wildcatter:turbine_casing` | Single-stage condensing turbine | 2 x 2 x 2 | 1 MW single-stage turbine, about 2 m long |
| `G` | `wildcatter:turbine_generator` | Generator, 1,250 kVA, 2-pole; **controller** | 3 x 2 x 2 | Two-pole generator about 2.5 m long |
| `C` | `wildcatter:condenser` | Side-exhaust surface condenser with its hotwell | 3 x 2 x 2 | Shell about 3 m long and 1.2 m in diameter |
| `P` | `wildcatter:feed_pump` | Feed pump, coupling and 5 hp motor | 2 x 1 x 1 | 5 hp end-suction pump skid |

## Layout (NORTH facing; x east, z south; each letter is a part's main cell, all on the ground)
```
x: 012345
z0 BD...S
z1 ......
z2 ......
z3 .G..T.
z4 ......
z5 .P.C..
z6 ......
```
The superheater stands right over the turbine's inlet, so the main steam line drops straight into it. The feed pump butts
onto the condenser's hotwell. The cell west of the generator is left free for the GSU or its cable.
6 x 7 m and 4 high.

How each part connects:
- **Fire and steam:** the burner fires into the boiler's front. The superheater is the boiler's rear section, and its
  main steam line drops into the turbine below it. The turbine drives the generator to its west, and exhausts sideways into the
  condenser to its south.
- **Generator output (4.16 kV):** the terminals are on the generator's west end face, the non-drive end, on both lower
  cells.
- **Feed pump power (480 V):** the terminals are on the motor's terminal box, on its south side, away from the generator.
- **Fuel:** coal by hand on any burner cell; a hopper into the burner's main cell; the gas line to any burner cell.
- **Water:** the water pipe connects to any condenser cell, and a cooling tower's basin goes on that pipe.
- **Building:** the plant has no base to snap to. Place the parts freely; the generator's readout names the next missing
  or wrongly turned part, with its x y z.
- **Turning parts:** the turbine-to-generator coupling and the feed pump coupling.

## Controller
`turbine_generator` (`G`). It validates the structure, holds the steam, water and fuel state, and owns the three
electrical terminals (A, B, C plus N and G once Phase 1 lands).

## Rotation
Four horizontal facings, set by the controller block's placement.

## Formation
Every part's main cell in the layout, facing the plant's way. The controller checks every 20 ticks. When formed:
structure sound and the generator becomes live.

## Breaking
Removing any part dissolves the structure. The generator output drops to zero. The drum keeps its water; a
spilled drum floods the cells it was in.

## Game test
Build the structure, check it forms, burn fuel, check output rises to rating, remove the condenser and check
output falls (back pressure), remove a part and check it dissolves.

## Migration
Phase 4 one-block plant parts drop as items with a chat line on load (the layout changed with true size).
Existing boiler, turbine and firebox blocks stay for one release as a deprecated 110 V small plant, then are
removed. The cooling tower becomes part of the plant (see cooling-tower.md).
