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

## Layout (bottom layer first, NORTH facing; fixed with the models in Phase 4)
The approved sketch put the fuel tank and radiator where they hid every turning part. As built, the skid is laid out like a real
genset: radiator, engine, alternator and control panel in a line along the shaft, the sub-base fuel tank under the engine, the exhaust on top.
Layer 1 (under the skid):
```
 . F . .
```
Layer 2 (controller `K` at the east end, facing north):
```
 R E A K
```
Layer 3:
```
 . X . .
```
The gas set has no `F` (pipeline gas comes to the engine). Turning parts are seen from the sides: the radiator fan behind its
grille, the crank pulley at the radiator end of the engine, and the coupling between engine and alternator.

## Crafted parts (Phase 4 plan, approved 2026-10-30)
The engine and the alternator are built from parts, as Tim asked: the engine and its generator are separate machines.
| Item | Real part | Goes into |
|---|---|---|
| `engine_block_casting` | Cast iron cylinder block | engine |
| `piston` | Piston and connecting rod | engine |
| `crankshaft` | Forged crankshaft | engine |
| `cylinder_head` | Cylinder head with valves | engine |
| `fuel_injector` | Diesel injector | diesel engine |
| `spark_plug` | Spark plug and ignition coil | gas engine |
| `stator_winding` | Copper stator winding | alternator |
| `rotor_field` | Rotor with field poles | alternator |
| `exciter` | Brushless exciter and voltage regulator | alternator |

## Added while building (PROPOSED, wait for Tim)
| Item | Value | Real-world basis |
|---|---|---|
| Fuel at part load | 15 % of full-load fuel at no load, rising in a straight line to full | published genset fuel charts (a 500 kW diesel: about 5 gal/h at no load, 35 gal/h at full) |
| Start | cranks to rated speed in 10 s, then closes its breaker | NFPA 110 Type 10 (rated voltage and frequency within 10 s) |
| Stop | coasts down in 10 s | |

## Fuel, speed and synchronization (Phase 4 plan, approved 2026-10-30)
- Diesel: 1 unit = 1 L at 38.6 MJ; about 130 L/h at 500 kW. The skid tank holds 2,000 L (about 15 h at full load).
- Gas: 1 unit = 1 m³ at 38 MJ; about 265 m³/h at 1 MW.
- 4-pole alternator: 1,800 rpm at 60 Hz, 1,500 rpm at 50 Hz (from the world's standard). Subtransient reactance X''d = 0.15 pu.
- Each stator phase carries a third of the kVA (the Phase 3 stator limit, IEEE C50.13).
- The control panel holds the output breaker. Onto a live island it closes only in sync: automatic (trims +0.05 Hz fast, closes inside
  ΔV ≤ 5 %, Δf ≤ 0.1 Hz, Δθ ≤ 10°) or manual (raise/lower speed, close by hand, watching the synchroscope). A close outside the window
  faults: about 2·sin(θ/2)/X''d per unit. The breaker trips, and above 3 pu the alternator is damaged and must be replaced.
- Moving parts: the flywheel and crank pulley, the alternator shaft and fan, and the radiator fan (docs/kinetics-and-sound.md).

## Controller
`genset_control_panel` (`K`). It holds the fuel, the governor and the synchronizer, and owns the output terminal.

## Rotation / formation / breaking
Four facings. Forms when all parts touch in the layout. Removing a part dissolves it and the set stops. A
generator must synchronize before closing onto a live island (ELECTRICAL_MODEL).

## Game test
Build both sets, check they form, check output at rating, check the fuel tank empties and the set stops.

## Migration
The 120 V single engine (`diesel_engine`, `gas_turbine`) and the 8-engine cube are removed when this lands (Tim, 2026-10-30: remove now).
