# 10 MVA power transformer, 4.16/138 kV (Phase 10d2, proposed 2026-11-04)

A rated, true-size transformer between a plant's 4.16 kV and a 138 kV line. It replaces the one-block, unrated
`substation_transformer`, which is removed. It works either way:
- as a step-up, from a plant's 4.16 kV onto a 138 kV line;
- as a step-down, from a 138 kV yard to a 4.16 kV plant bus (the potline's fan).

**Decided with Tim on 2026-11-04:** 10 MVA, 7.5 %Z; delta at 4.16 kV, grounded wye at 138 kV; about 5 x 5 x 4.

## Real-world counterpart
A 10 MVA, ONAN, liquid-filled power transformer of a small plant or industrial yard: a 138 kV grounded-wye winding with three
138 kV porcelain bushings (650 kV BIL) on the cover, a 4.16 kV delta winding brought out to an air-terminal cable throat low on
the back, radiator banks on both sides, a conservator, and a de-energized tap changer.

| Item | Value | Basis |
|---|---|---|
| Voltages | 4.16 kV delta / 138 kV grounded Y (IEC 3.3 kV / 132 kV) | decided with Tim |
| Rating | 10 MVA ONAN | decided with Tim |
| Impedance | 7.5 %Z, X/R about 15 | typical for this class |
| Phase shift | the wye (138 kV) side leads by 30 degrees (ANSI) | ANSI C57.12 |
| Short-time overload | IEEE C57.109, as every rated transformer (ELECTRICAL_MODEL.md) | |

## Part: `wildcatter:substation_transformer_10`
5 x 5 x 4: 5 across, 5 up to the bushing tops, 4 deep. It faces its 138 kV side, away from whoever places it. Facing north,
bottom layer first, north (front, 138 kV) to south (back, 4.16 kV):
```
layers 0 to 3               layer 4
R T T T R                   . . H . .
R T T T R                   . . . . .
R T T T R                   . . . . .
R T M T R                   . . . . .
```
- `M`: main cell, holding the block entity. It is the bottom middle cell of the back row, where the 4.16 kV cable throat is.
- `T`: the tank and core.
- `R`: the radiator banks on each side.
- `H`: the 138 kV bushings, over the front row's middle cell.
- **Terminals:**
  - 4.16 kV: the back face of the main cell. A `cable_mv` comes in there.
  - 138 kV: the top and front faces of the `H` cell. A `cable_hv` or the risers to a line go there.
- **All four facings.**
- Its footprint and terminals are the 25 MVA GSU's (gas-turbine.md), at a smaller rating.

## Readout (right-click)
The rated transformer's readout: its two voltages, each side's current, its loading against 10 MVA, and its C57.109 heating ("within
its rating", or "OVERLOADED at 140 %, heating 12 %, fails in 7 min at this load").

## Breaking
Breaking it drops it, as other true-size parts do. Both sides go dead.

## Recipes
- It takes the old `substation_transformer` recipe, with the 4.16 kV pole unit (`pole_transformer_mv`) in place of the one-block
  `transformer`.
- It is an ingredient of the `power_transformer` and the `substation_transformer_60`, in place of the old block.

## Game tests
- **`overheadThreePhase`:** a genset, a unit substation and this transformer step 480 V up to 138 kV for the H-frame line.
- **`potline`:** it steps the yard's 138 kV down to 4.16 kV for the fan's soft starter.
