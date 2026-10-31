# Cooling tower (APPROVED 2026-10-30)

## Real-world counterpart
A **mechanical-draft (induced draft) cooling tower**: basin, fill, drift eliminators, fan deck and fan with a motor.
Rejects waste heat from the steam condenser to the air.

## Values (DECISION)
| Item | Value | Real-world basis |
|---|---|---|
| Fan motor | 750 W in-game (as built), a motor load once Phase 3 lands | Existing |
| Effect | An uncooled steam plant needs about 33 percent more steam | As built |

## Parts
| Char | Block id | Real part |
|---|---|---|
| `W` | `wildcatter:tower_basin` | Cold-water basin; takes the pipes |
| `F` | `wildcatter:cooling_fill` | Fill (exists) |
| `E` | `wildcatter:drift_eliminator` | Drift eliminators |
| `M` | `wildcatter:cooling_tower` | Fan deck, fan and motor; **controller** (exists as the fan base) |

## Layout (bottom layer first)
Layer 1: `W`
Layer 2: `F`
Layer 3: `E`
Layer 4: `M`

## Controller / rotation / formation / breaking
`cooling_tower` (`M`) is the controller. It looks down the column for the parts in order. The tower forms when the
column is complete and the basin touches water pipe; it cools only while the fan has power. Removing any part stops
cooling.

## As built (Phase 4d)
The spec's column: basin, fill, drift eliminators, fan deck on top (the controller). The fan turns (about 200 rpm, shown at 1/20)
and whirs while it cools. A machine is cooled when the basin is on its water pipe. Tested on the starter turbine and the 1 MW plant.

## Game test
Build the column, check it forms, check the turbine reports cooled, remove the drift eliminators and check it still cools but loses water faster (DECISION).
