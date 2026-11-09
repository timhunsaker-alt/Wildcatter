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

### The 4.16 kV pole transformer (Phase 10d2, decided with Tim on 2026-11-04)
- **Part:** `wildcatter:pole_transformer_mv`. It replaces the one-block, unrated `transformer`, which is removed.
- **Counterpart:** a 25 kVA single-phase conventional pole-top unit for an older 4.16Y/2.4 kV circuit.
  - The primary is 2,400 V, line to neutral (IEC: 3.3 kV lines, 1.9 kV to neutral).
  - The secondary is 120/240 V split-phase (230 V IEC).
  - It is a smaller tank than the 12.47 kV unit, with a 5 kV class H1 bushing (60 kV BIL, three sheds).
- **Rated:** 25 kVA, with the 12.47 kV unit's impedance and the same IEEE C57.109 overload rule.
- **Hanging and terminals:** as the 12.47 kV unit. It hangs on a pole, and one must stand beside it. The 4.16 kV comes to its top
  (H1). The low side is the back, with leg A and the neutral, and the two sides carry leg B.
- **It runs backward too.** A 120 V source on its low side gives 2.4 kV to neutral on its high side (the `transformerChain` scene).

## The protection gap wave (2026-11-09, built while Tim was away; decisions in `docs/questions/protection-gaps.md`)
### Leads (jumpers)
- **Counterpart:** the primary lead (a jumper of line conductor) from the line's insulator down to a cutout's top contact or a pole
  transformer's H1 bushing, as a lineman ties it in with a hot-line clamp.
- **How:** the same conductor spool: right-click the insulator, then the cutout or the pole transformer (either order). A lead is at
  most 6 m (`Tuning.LEAD_MAX_BLOCKS`). An insulator holds two leads; a cutout or a pole unit takes one.
- **What it joins:** the insulator's conductor to the cutout's line contact (the back) or the unit's H1 (its high-side node). The
  insulator owns the lead (saved on it, drawn by the span renderer, slack); the part remembers which insulator feeds it, so the two
  solve as one circuit. A lead is the spool's tier and must match the part (a 4.16 kV unit takes a 4.16 kV lead).
- **Heating:** a lead heats on its conductor's rating as a span does.
- Wire blocks to the parts' faces still work, as before.
- **Not drawn:** a lead from the cutout's bottom to the transformer (they still join by touching), and arresters' leads.

### Pole hardware falls with its pole
A pole transformer or a fused cutout that has hung beside a pole (latched the first time it is solved there) comes down, dropping as
an item, when no pole stands beside it any more. One set down away from a pole is not touched.

### Span ampacity
A span (and a lead) heats on its conductor's rating (the size's amps: the ACSR table at 75 C conductor, 25 C air), its temperature
rise following the square of its loading with a 10-minute time constant (bare 4/0 ACSR in still air, IEEE 738). It parts and falls
when the rise reaches 3 x the rated rise, about 175 C, past ACSR's 100-150 C emergency limit, where the aluminium anneals. Twice the
rating lasts about 11 minutes, five times about 50 seconds. The insulator's readout gives the most loaded span's amps and heating.

### 4.16 kV spans
`conductor_spool_mv` and `covered_conductor_spool_mv`, wound from MV wire in the ACSR sizes, string 4.16 kV (3.3 kV IEC) spans, as
long as distribution spans, on DIST (15 kV) insulators.

### The flashover's arc
An under-rated insulator flashes over to earth. Its power-follow arc is counted in amp-seconds, on the solve the fault appears in
(before any relay clears it): a fault a relay clears at once still arced for 5 cycles at the island's frequency. At 50 A·s the
porcelain shatters (a few kA for a few cycles), as does the old 3 s of arc above 1 A.

### Guy wires (planned, not built)
See `docs/questions/protection-gaps.md`, entry D6: a down guy (3/8 in EHS strand, 15,400 lbf) from the pole top to a screw anchor,
and a pole loading check against the ANSI O5.1 class strength. Planned, with the reasons.

### A pole transformer placed by command (planned)
See entry D7: the rule (it turns its back to the pole) would rewire the seven client scenes that set one by command beside a pole.

## Older worlds
- The one-block `pole` and `crossarm` stay and keep working with their spans; they are no longer crafted or in the creative tab.
- Insulators still stand on them (as they did); placed on the new hardware they take the new mounting.

## Game test
- NeoForge server scenes (protection gap wave): `span_burns` (a 12.47 kV span and a lead to a pole unit carry two heaters; the span
  heats and burns down at a shrunk ampacity; breaking the pole drops the unit and the insulator), `mv_span_flashover` (a 4.16 kV span
  and lead light a 4.16 kV pole unit's lamp; a span to a 1 kV insulator flashes over and shatters it).
- `spans`, `conductorSizes`, `serviceDrop`, `overheadThreePhase` and `refining` are strung on the new poles.
- `lineParts`: a pole placed by a click, an arm clicked on it snaps to the top, insulators on the arm, breaking the pole drops
  the pole, arm and insulators; an old stacked pole's span still carries current; an H-frame with its strings forms.
