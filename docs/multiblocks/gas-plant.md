# Gas compression and the gas plant (APPROVED 2026-10-31 with the Phase 5b plan)

What turns a lease's separator gas into pipeline gas and LPG. Like the lease (lease.md), each part is a true-size big part placed on
its own and joined by steel pipe, one product to a run; only the refrigeration compressor works by touching (it cools the chiller
its skid stands against). Values are approximate, drawn from typical equipment of each class. The plant is sized to about 3 million
scf a day (1.0 m3/s): one compressor takes about one separator's gas.

```
separator gas -> COMPRESSOR -> TEG DEHYDRATOR -> NGL CHILLER (+ REFRIGERATION COMPRESSOR touching it) -> pipeline gas
                                                         \-> raw NGL -> STABILIZER -> LPG -> LPG BULLET
                                                                                \-> natural gasoline -> stock tank
```

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `gas_compressor` | 4 x 4 x 9 | Two-stage reciprocating gas compressor package (Ariel JGK/2 class): a 400 hp (300 kW) 4.16 kV three-phase six-pole motor at 1,190 rpm (990 on IEC worlds, from the standard), the frame and two cylinders, suction and interstage scrubbers, and a fan cooler driven off the motor. 125 psi suction to 1,000 psi discharge, 1.0 m3/s at full voltage. |
| `teg_dehydrator` | 3 x 8 x 5 | Packaged triethylene glycol dehydrator, 3 MMscfd: the 7.5 m contactor, the reboiler and still column, the surge tank and a glycol-powered (Kimray-type) pump, so it needs no electricity. The reboiler burns 0.5 % of the gas. |
| `ngl_chiller` | 3 x 3 x 7 | Mechanical refrigeration skid: the gas-gas exchanger, the propane chiller (to about -20 F) and the cold separator. Each m3 of gas gives 0.955 m3 of pipeline gas and 0.20 L of raw NGL (Y-grade): a medium-rich gas at about 1.5 gallons of NGL per Mcf, the gas shrinking about 4.5 %. |
| `refrigeration_compressor` | 3 x 3 x 6 | Propane screw compressor package, 150 hp (112 kW), 480 V three-phase, two-pole (3,560 rpm, 2,967 on IEC worlds). No pipe of its own: it cools any chiller its skid touches. |
| `stabilizer` | 2 x 12 x 2 | Debutanizer column, 0.9 m across and 12 m tall, its reboiler, overhead condenser and reflux drum at the base. Y-grade in; 80 % comes off overhead as LPG (propane and butane), 20 % from the bottom as natural gasoline (C5+). Up to 0.5 L/s. |
| `lpg_bullet` | 3 x 3 x 12 | 18,000 gallon (68 m3) horizontal propane tank, 250 psi, relief valves and gauges: 58,000 L of LPG at its 85 % fill. LPG only; LPG never goes in an atmospheric stock tank. |

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- Gas compressor: suction on the north face of the front cells (z 0, bottom layer); discharge on the south face of the back cells
  (z 8, bottom layer); the motor's 4.16 kV terminal box on the west face of the cell at x 0, z 4, bottom layer.
- Dehydrator: wet gas in on the north face of the front cells (z 0, bottom layer); dry gas out on the south face of the back
  cells (z 4, bottom layer).
- Chiller: gas in on the north face of the front cells (z 0, bottom layer); pipeline gas out on the south face of the back cells
  in layer 1; Y-grade out on the south face of the back cells in layer 0.
- Refrigeration compressor: the 480 V terminal box on the west face of its main cell. It cools a chiller any of whose cells is
  next to one of its own.
- Stabilizer: Y-grade in on the north face of its bottom cells; LPG out on their east face; gasoline out on their south face.
- LPG bullet: LPG in and out on the side faces of its bottom layer.

## Products
New on steel pipe: compressed gas (out of a compressor), dried gas (out of a dehydrator), pipeline gas (out of a chiller), raw NGL
(Y-grade), LPG and natural gasoline. A stock tank may hold natural gasoline; LPG goes only in a bullet.

## Who takes what
- Gas gensets: separator gas or pipeline gas.
- The steam plant's burner: pipeline gas only ("needs pipeline gas: process it in a gas plant").
- Fireboxes: any gas.
- The compressor takes separator gas; the dehydrator compressed gas; the chiller dried gas (compressed gas, still wet, freezes in
  its exchanger: HYDRATES, and it stops); the stabilizer Y-grade.

## How it runs
- Each process part pulls from its inlet run and holds a few seconds of output for whatever pulls from its outlet.
- The compressor's motor runs only while gas waits at its suction and its discharge has room; its throughput follows the motor's
  output (low voltage runs it slower).
- The refrigeration compressor runs while a chiller it touches has gas waiting. With none running the chiller reads NO
  REFRIGERATION and stops.

## Added while building (5b)
- The gas compressor unloads instead of stopping: it starts with gas waiting and half its discharge free, keeps turning with its
  cylinders unloaded when it has nothing to do, and stops after 15 s of that. Restarting a 400 hp motor every few seconds on a
  genset is not how compressors run, and each start drew six times full-load current.
- A flare stack takes the excess of any gas part, not only a separator's: a gas plant's residue gas with no buyer goes to it
  above three quarters full, as a real plant's relief does. Without one the plant backs up to what its buyers take.
- Wire heating and motor starts: the old wire-heat rule burned a cable at 2x its rating in 6 s, and burned a 150 hp motor's
  NEC-sized 4/0 on its start. Tim approved the fix (2026-10-31): a conductor's temperature rise follows the square of its current
  with a 120 s time constant and burns out at 1.8 times its rated rise (energy/WireHeat). A start passes; twice the rating burns
  in about 37 s, three times in about 13 s, a dead short in a quarter of a second.

## Game test
`gasPlant`: a gas well and separator; a 1 MW gas set on separator gas powers the plant (480 V to the refrigeration compressor, a
backward unit substation to 4.16 kV for the gas compressor); the train fills an LPG bullet and a stock tank of natural gasoline,
and a second gas set runs on its pipeline gas. With the dehydrator bypassed the chiller reads HYDRATES; with the refrigeration
compressor taken away, NO REFRIGERATION.
