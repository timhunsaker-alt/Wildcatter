# Cooling tower (APPROVED 2026-10-30)

## Real-world counterpart
A **mechanical-draft (induced draft) cooling tower**: basin, fill, drift eliminators, fan deck and fan with a motor.
Rejects waste heat from the steam condenser to the air.

## Values (DECISION)
| Item | Value | Real-world basis |
|---|---|---|
| Fan motor | 750 W in-game (as built), a motor load once Phase 3 lands | Existing |
| Effect | An uncooled steam plant needs about 33 percent more steam | As built |

## True size (Phase 4.5c, approved 2026-10-31)
One induced-draft cell, 4 x 4 m and 6 high. Every part is one item that fills its real size.
| Char | Block id | Real part | Size | Real-world basis |
|---|---|---|---|---|
| `W` | `wildcatter:tower_basin` | Concrete cold-water basin; takes the pipes | 4 x 4 x 1 | basin under an induced-draft cell about 4.3 m square |
| `F` | `wildcatter:cooling_fill` | PVC film fill with its casing | 4 x 4 x 2 | film fill about 1.5 to 1.8 m deep |
| `E` | `wildcatter:drift_eliminator` | Drift eliminators, hot-water distribution and nozzles | 4 x 4 x 1 | |
| `M` | `wildcatter:cooling_tower` | Fan deck, 3 m fan in its stack, 7.5 kW motor and gearbox, handrail; **controller** | 4 x 4 x 2 | a 3 m fan with a 10 hp motor for about 3.5 MW of heat; handrail per OSHA at 1.07 m |

## Values (Phase 4.5c, approved 2026-10-31)
| Item | Value | Real-world basis |
|---|---|---|
| Fan motor | 7.5 kW (10 hp), 480 V three-phase (400 V IEC), through a gearbox | typical for a 3 m tower fan |

## Layout
The parts stack with their main cells in one column (basin at the bottom, then fill, drift eliminators and the fan deck),
all facing the same way. Place the basin first; each part clicked on top of the column snaps onto it. A ladder runs up
the front. The motor's terminals are on the front face of the fan deck's main cell, where a real motor disconnect is.
The small 120 V starter turbine cannot run a tower any more.

## As built (Phase 4d)
The spec's column: basin, fill, drift eliminators, fan deck on top (the controller). The fan turns (about 200 rpm, shown at 1/20)
and whirs while it cools. A machine is cooled when the basin is on its water pipe. Tested on the starter turbine and the 1 MW plant.

## Game test
Build the column, check it forms, check the turbine reports cooled, remove the drift eliminators and check it still cools but loses water faster (DECISION).
