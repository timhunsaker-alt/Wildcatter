# Poles, crossarms and the overhead line at true size (Phase 4.5e)

**Status: APPROVED by Tim on 2026-10-31 with the 4.5e plan.** At true scale, 1 block = 1 m, each pole is one item and fills its real
height when placed. The hardware (crossarm, crossbeam, X-brace, insulators, pole transformer, cutout) is each its own item, the
way a line crew hangs it.

## Span scale (supersedes the "1 block of span = 4 m" row of phase2-distribution.md)
| Value | Was | Now | Real-world basis |
|---|---|---|---|
| Metres per block of span | 4 | 1 | true scale |
| Longest span, low voltage | 8 blocks | 32 blocks (32 m) | service drops are 15 to 30 m (same metres as before) |
| Longest span, 12.47 kV | 10 blocks | 40 blocks (40 m) | distribution spans are 30 to 50 m |
| Longest span, 138 kV | 64 blocks | 256 blocks (256 m) | transmission spans are 200 to 400 m |
| Spool length | 256 blocks | 1,024 blocks (1,024 m) | same metres as before |
| Drawn sag | `min(2, 0.03 L + 0.1)` blocks | 2.5 % of the span | ACSR at typical tension sags 2 to 3 % of the span; only the picture |

A span strung in an older world keeps its length in blocks; since a block is now 1 m its resistance is a quarter of what it was.

## Parts
Sizes are across the line × up × along the line.

| Part (id) | Cells | Real-world basis |
|---|---|---|
| `wood_pole_30`, 30 ft class 5 | 1 × 8 × 1 | 30 ft set 5 ft deep (10 % + 2 ft) stands 7.6 m, rounded to 8; top 0.15 m, ground line 0.23 m across. Secondaries and service drops. |
| `wood_pole_40`, 40 ft class 4 | 1 × 10 × 1 | 40 ft set 6 ft deep stands 10.4 m, rounded to 10; top 0.17 m, ground line 0.27 m. 12.47 kV distribution. |
| `wood_pole_75`, 75 ft class 2 | 1 × 20 × 1 | 75 ft set 9.5 ft deep stands 20.0 m; top 0.20 m, ground line 0.38 m. Two make a 138 kV H-frame. |
| `crossarm_dist`, 8 ft crossarm | 3 × 1 × 1 less the pole's cell | 3.5 × 4.5 in by 8 ft (2.44 m) Douglas fir on two flat braces, its top 0.3 m below the pole top |
| `crossbeam_138`, H-frame crossbeam | 9 × 1 × 1 less the two pole cells | two 5.6 × 7.6 in timbers about 9 m long, through-bolted to both poles, their tops 0.6 m below the pole tops |
| `x_brace_138`, H-frame X-brace | 3 × 3 × 1 | two crossed timbers between the poles, below the strings |
| `insulator_lv`, secondary spool | 1 | 1 kV spool on a clevis pin, about 0.08 m across |
| `insulator_dist`, 15 kV pin | 1 | ANSI 55-5 porcelain pin insulator, about 0.13 m tall, on a steel pin (on the arm) or a pole-top pin |
| `insulator_hv`, 138 kV string | 1 (drawn 1.5 m tall, reaching up into the crossbeam's cell) | nine ANSI 52-3 bells, 146 mm each, with yoke and clamp: about 1.5 m. It hangs from the crossbeam; the same item still stands on an older world's pole sections. |
| `pole_transformer` | 1 | 25 kVA can, 0.46 m across and 0.84 m high, on its hanger bracket, with the H1 bushing on the cover, X1/X2/X3 on the side and a surge arrester |
| `fused_cutout` | 1 | 15 kV porcelain cutout on its bracket; the fuse tube swings down open when the link blows |

The poles carry the whole drawing: pole steps from 2.5 m up, the brand tag at about 3 m, the ground wire moulding down one side.

## How they go together
Distribution (a 40 ft tangent pole, line running along z):
```
y = top+1   I . I      I = insulator_dist on the arm (A and C), P = on a pole-top pin (B)
            . P .
y = top     A | A      A = crossarm_dist cells, | = the pole's top cell
y = ...     . | .
```
- The crossarm is clicked on the pole. Within the top 2 m it snaps to the standard place (its cells beside the pole's top cell);
  clicked lower it goes where clicked (a second arm for secondaries). It runs across the line, square to the player's view.
- It needs its pole: if the pole goes, the arm drops, and its insulators with it.
- The pin insulators sit on the arm's two cells, 2 m apart; the middle phase sits on a pole-top pin on top of the pole, 0.3 m
  higher, as on a real tangent pole.

138 kV H-frame (seen along the line):
```
x:   -4 -3 -2 -1  0  1  2  3  4
     B  B  |  B  B  B  |  B  B     B = crossbeam_138 cells (main at x 0), | = the two 75 ft poles
     S  .  |  .  S  .  |  .  S     S = insulator_hv strings, hanging 1.5 m: phases 4 m apart
     .  .  |  X  X  X  |  .  .     X = x_brace_138, 3 x 3 between the poles
```
- The crossbeam is clicked on a 75 ft pole's top; it finds the other pole 4 m away and centres between them. It needs both.
- Real 138 kV H-frames space the poles and the phases 4 to 5 m.

Where the wire attaches: each insulator gives the height its conductor really sits at, from its model: on an arm 0.26 m up its
cell, on a pole-top pin 0.56 m, at the clamp of a string 0.04 m, and the older one-block insulators 0.95 m as before. The drawn
span and the shock check run between those points. A span's length is still counted between the insulators' cells.

The pole transformer hangs on any pole cell, old or new, its bracket toward the pole when it is clicked onto one.

## Older worlds
- The one-block `pole` and `crossarm` stay and keep working with their spans; they are no longer crafted or in the creative tab.
- Insulators still stand on them (as they did); placed on the new hardware they take the new mounting.

## Game test
- `spans`, `conductorSizes`, `serviceDrop`, `overheadThreePhase` and `refining` are strung on the new poles.
- `lineParts`: a pole placed by a click, an arm clicked on it snaps to the top, insulators on the arm, breaking the pole drops
  the pole, arm and insulators; an old stacked pole's span still carries current; an H-frame with its strings forms.
