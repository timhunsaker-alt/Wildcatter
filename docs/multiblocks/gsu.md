# Generator step-up transformer (APPROVED 2026-10-30 with the Phase 4 plan)

One part, not a multiblock: a 1.5 MVA generator step-up transformer is one oil-filled tank. Phase 4.5: it is a true-size part
(README, "Big parts"), 3 m across, 2 m deep and 3 m to the bushing tops.

## Real-world counterpart
The GSU of a small plant: a delta winding on the generator side, a grounded wye on the line side. Plants this size connect at
distribution voltage.

## Values
| Item | Value | Real-world basis |
|---|---|---|
| Voltages | 4.16 kV delta / 12.47 kV grounded Y (IEC 3.3 kV / 11 kV) | decided with Tim; small plant interconnection |
| Rating | 1,500 kVA | sized for the 1,250 kVA steam plant generator |
| Impedance | 5.75 %Z, X/R 5 | typical for this size |
| Phase shift | the Y side leads by 30 degrees (ANSI Dy1 convention, high side leads) | ANSI C57.12 |

## Model
Each delta winding sits between two line terminals of the 4.16 kV side; each wye winding between a 12.47 kV phase and the grounded
neutral. Turns ratio per winding = V(line to line, delta side) / V(line to neutral, wye side). The delta side has no neutral: a
single-phase load on it to ground carries no current.

## Size and model (As built, Phase 4.5a)
Drawn from a typical liquid-filled substation-type unit of this rating (`tools/models/gsu_transformer.py`):
- the tank: 1.5 m wide, 1.2 m deep and 1.6 m tall, on skid channels
- radiators: a bank of panels on each side, about 2.2 m across them
- 12.47 kV side: three 15 kV class porcelain bushings (95 kV BIL, four sheds) on the cover over the front half, and the H0
  neutral bushing strapped to a ground pad
- 4.16 kV side: an air-terminal cable throat low on the back
- fittings: liquid-level gauge, thermometer, pressure-vacuum gauge, nameplate, DANGER signs, drain valve, pressure relief
  device and lifting lugs

Footprint, facing north (the 12.47 kV side is the front, away from whoever places it):
```
layer 0, 1 and 2 (bottom to top), each:
  F F F     F: front row (bushings over the middle cell, on the top layer)
  B M B     M: main cell (bottom layer only; above it, back-row pieces)  B: back row
```

## Placement
- 4.16 kV delta comes in on the back face of the main cell, through the cable throat.
- 12.47 kV terminals are the top and front faces of the top cell in the middle of the front row, where the bushings are.
- It heats on sustained overload like the pad-mounts.

## Game test
The steam plant (or a genset through a unit substation) feeds it; check 12.47 kV (7.2 kV to neutral) out and the 30 degree shift on the meter.
