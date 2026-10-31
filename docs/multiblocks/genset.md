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

## True size (Phase 4.5b, approved 2026-10-30)
Every part is one item that fills its real size (docs/multiblocks/README.md, "Big parts"). 1 block = 1 m.

| Char | Block id | Real part | Size L x W x H | Real-world basis |
|---|---|---|---|---|
| `F` | `wildcatter:fuel_tank` | Sub-base fuel tank, 2,000 L (diesel set) | 5 x 2 x 1 | Double-wall sub-base tank, about 0.6 m deep under a 4.4 x 1.6 m skid. Drawn 0.6 m deep, with the base rails and anti-vibration mounts up to 1 m. |
| `B` | `wildcatter:genset_base_frame` | Steel base frame (gas set) | 7 x 2 x 1 | Fabricated base frame. Drawn 0.4 m deep, with its mounts and a lube-oil make-up tank. |
| `R` | `wildcatter:radiator` | Radiator with a 1.2 m pusher fan | 1 x 2 x 2 | 500 kW core about 1.8 x 1.8 m. Fans are 1.1 to 1.3 m. |
| `E` | `wildcatter:genset_diesel_engine` | Diesel engine | 2 x 2 x 2 | 15 L inline six, about 1.9 m long and 1.5 m tall |
| `G` | `wildcatter:genset_gas_engine` | Gas engine | 4 x 2 x 2 | 1 MW lean-burn V20, about 3.8 m long |
| `A` | `wildcatter:alternator` | Alternator (both sets) | 1 x 2 x 2 | 500 kW frame about 1.2 m long. A 1 MW frame is about 1.6 m; it is drawn 1 m so one part fits both sets. |
| `K` | `wildcatter:genset_control_panel` | Control panel and output breaker; **controller** | 1 x 2 x 2 | Genset-mounted breaker enclosure with the controller on its face. 800 A for the diesel set (752 A at 480 V) and 1,600 A for the gas set (1,504 A). |
| `X` | `wildcatter:exhaust_stack` | Critical-grade exhaust silencer (diesel) | 2 x 1 x 1 | About 1.8 m long and 0.5 m in diameter for 500 kW |
| `X` | `wildcatter:exhaust_silencer_large` | Critical-grade exhaust silencer (gas) | 3 x 1 x 1 | About 2.5 m long and 0.7 m in diameter for 1 MW |

Overall sizes:
- **Diesel set:** 5 x 2 x 4. Real: 4.4 x 1.6 x 2.6 m, with the silencer on top at about 3.2 m.
- **Gas set:** 7 x 2 x 4. Real: 6.5 x 2.0 x 2.6 m.

The approved 5 x 2 x 3 and 7 x 2 x 3 became 4 high, because the sub-base takes the first metre and the silencer sits above
the 3 m radiator.

## Layout (bottom layer first, NORTH facing)
Each letter is a part's **main cell**; its other cells follow from its size. North, the readout side, is the top row. The
shaft runs west (radiator) to east (control panel). Every part must face the set's way.

Diesel, 5 x 2 x 4:
```
layer 0      layer 1      layer 2      layer 3
F....        RE.AK        .....        .....
.....        .....        .....        .X...
```
Gas, 7 x 2 x 4:
```
layer 0      layer 1      layer 2      layer 3
B......      RG...AK      .......      .......
.......      .......      .......      ..X....
```

How the set is built and used:
- **Building:**
  - Place the base (`F` or `B`) first.
  - A skid part clicked anywhere on top of the base goes to its own slot and turns to the base's facing.
  - The silencer goes into its slot when clicked on top of the engine.
- **Power out:**
  - The 480Y/277 V output terminals are on the control panel's east end face, on both lower cells, where cables enter a
    real breaker box.
  - The readout and the start button are on the panel's north face.
- **Fuel and gas:**
  - Diesel fills the tank from any of its cells, by bucket or pipe.
  - The gas line connects to any cell of the gas engine.
- **Turning parts,** drawn at real size:
  - the radiator fan
  - the crank pulley and fan belt
  - the flywheel coupling, seen through the adapter's guard window
  - the alternator's cooling fan, behind its screen

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
Four facings. Forms when every part's main cell is in the layout and faces the set's way; the readout names a part that is
missing or faces the wrong way. Breaking any piece of a part drops that one part and dissolves the set; the set stops. A
generator must synchronize before closing onto a live island (ELECTRICAL_MODEL).

## Game test
Build both sets, check they form, check output at rating, check the fuel tank empties and the set stops.

## Migration
Phase 4 one-block genset parts do not grow at true size (the layout changed): on load each drops as an item with a chat line,
and a one-block tank's diesel is lost.
The 120 V single engine (`diesel_engine`, `gas_turbine`) and the 8-engine cube are removed when this lands (Tim, 2026-10-30: remove now).
