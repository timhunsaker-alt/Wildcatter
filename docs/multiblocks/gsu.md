# Generator step-up transformer (APPROVED 2026-10-30 with the Phase 4 plan)

A single block, not a multiblock: a 1.5 MVA generator step-up transformer is one oil-filled tank.

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

## Placement
High-voltage terminals on the front and top, like the other transformers. Heats on sustained overload like the pad-mounts.

## Game test
The steam plant (or a genset through a unit substation) feeds it; check 12.47 kV (7.2 kV to neutral) out and the 30 degree shift on the meter.
