# Genset: diesel and gas (APPROVED 2026-10-30)

## Real-world counterpart
A skid-mounted **engine-generator set**: engine, alternator, radiator, fuel tank, exhaust, and a control panel.
Standby diesel sets are commonly 500 kW and 625 kVA at 480Y/277 V three-phase; gas sets 1 MW at the same voltage.

## Values (DECISION)
| Item | Diesel | Gas |
|---|---|---|
| Rating | 500 kW, 625 kVA, 0.8 PF | 1,000 kW, 1,250 kVA, 0.8 PF |
| Voltage | 480Y/277 V NA, 400Y/230 V IEC (industrial LV) | same |
| Inertia constant | about 1 s | about 1 s |
| Governor droop | 4 percent | 4 percent |
| Fuel | diesel, stored in the skid tank | pipeline gas |

## Parts
| Char | Block id | Real part |
|---|---|---|
| `E` | `wildcatter:genset_engine` | Engine block (diesel or gas variant) |
| `A` | `wildcatter:alternator` | Alternator |
| `K` | `wildcatter:genset_control_panel` | Control panel; **controller** |
| `F` | `wildcatter:fuel_tank` | Skid fuel tank (diesel only) |
| `R` | `wildcatter:radiator` | Cooling radiator |
| `X` | `wildcatter:exhaust_stack` | Exhaust |

## Layout (bottom layer first, NORTH facing)
Layer 1:
```
 F E A K
 . R . .
```
Layer 2:
```
 . . . X
```
The gas set omits `F`. A larger set adds a second engine and alternator pair, not more copies of one block.

## Controller
`genset_control_panel` (`K`). It holds the fuel, the governor and the synchronizer, and owns the output terminal.

## Rotation / formation / breaking
Four facings. Forms when all parts touch in the layout. Removing a part dissolves it and the set stops. A
generator must synchronize before closing onto a live island (ELECTRICAL_MODEL).

## Game test
Build both sets, check they form, check output at rating, check the fuel tank empties and the set stops.

## Migration
The 110 V single engine and the 8-engine cube are removed when this lands.
