# Motor control center (APPROVED 2026-10-30, with the Phase 3 plan)

## Real-world counterpart
A **motor control center (MCC)**: a lineup of steel sections sharing one three-phase horizontal bus. One section takes the incoming feeder through
a main breaker. The other units are **combination starters** ("buckets"), each a breaker, a contactor and an overload relay feeding one motor.
Typical industrial MCCs run at 480Y/277 V (NA) or 400Y/230 V (IEC).

Buckets are separate starters for separate motors, each with its own load and its own trip. That is a lineup of distinct functional units, not identical
blocks tiled to make one bigger machine, so it meets the multiblock rule in CLAUDE.md.

## Values
| Item | Value |
|---|---|
| Voltage | the Industrial tier (480Y/277 V NA, 400Y/230 V IEC) |
| Main breaker | the three-pole industrial breaker ratings: 20, 30, 60, 100, 225, 400 A (selectable) |
| Buckets | 1 to 6 |
| Bucket protection | its own three-pole breaker (industrial ratings) plus a class 10 overload relay set to its motor's full-load current |
| Bus | inside the lineup; no resistance beyond the contact resistance of each section |

## Parts
| Char | Block id | Real part |
|---|---|---|
| `I` | `wildcatter:mcc_incoming` | Incoming section with the main breaker; **controller** |
| `S` | `wildcatter:mcc_bucket` | Combination starter bucket (breaker, contactor, overload relay) |

## Layout (one layer, facing NORTH, seen from above; the front is the north side)
```
 I S S S S S S
```
`I` at the left as you face the front; 1 to 6 `S` in a straight line to its right, all facing the same way. One layer high.

## Terminals
- `I`: the incoming three-phase feed (A, B, C, N, G) on its back or top face, from an industrial cable.
- Each `S`: its motor's output (A, B, C, G) on its back or top face, to an industrial cable running to one motor.

## Controller
`mcc_incoming` (`I`). It owns the main breaker and the bus, and shows each bucket's state on the Power Meter (running, stopped, tripped on overload or breaker).

## Rotation
Four horizontal facings. The buckets always extend to the controller's right as you face the front.

## How it forms
Every structure check, the controller looks to its right along the line for `mcc_bucket` blocks with the same facing, stopping at the first gap or the 6th bucket.
Those buckets are on the bus. A bucket not touching a formed lineup is unpowered and says "not on an MCC bus".

## Starting and stopping
Right-click a bucket's front, or apply redstone to it, to start; again (or no redstone) to stop. A tripped overload is reset by right-clicking with an empty hand.

## When a part is removed
- Removing a bucket: that bucket's motor stops. The bus ends at the gap, so buckets beyond it lose power until the gap is filled.
- Removing `I`: the lineup dissolves and every motor stops.
- The main breaker tripping stops every motor; a bucket breaker or overload tripping stops only its motor.

## Game test
Build `I` and two buckets on a 480 V supply, check it forms, start two motors, overload one motor until its overload relay trips, and check the other keeps running.

## True size (Phase 4.5f, approved 2026-10-31)
1 block = 1 m. The lineup is tall NEMA sections, 20 in (0.51 m) wide, 90 in (2.29 m) tall and 20 in deep, each a 1 × 3 × 1 part.

| Part | Cells (W × H × D) | Real part |
|---|---|---|
| `mcc_incoming` | 1 × 3 × 1 | incoming section with the main breaker; **controller**; cables enter at the top |
| `mcc_section` | 1 × 3 × 1 | starter section: three bucket spaces, one per metre of height, beside its vertical wireway |
| `mcc_bucket` | an item, clicked into a section's empty space | NEMA size 1 to 2 combination starter bucket (12 to 18 in): breaker, contactor, class 10 overload relay |

```
 seen from the front:   I S S      I incoming, S starter sections to its right, touching, facing the same way
                        I S S      (three cells high; each S cell holds one bucket)
                        I S S
```
- **Feed:** on the top of the incoming section's top cell, or at the back of its bottom cell.
- **Bus:** through the side faces of the sections' bottom cells, as before.
- **Motor leads:** each bucket's at the back of its own cell.
- **Buckets:** at most 6 on a lineup (two sections). Right-click a bucket's door to start or stop it, sneak and right-click to step
  its overload dial, right-click a tripped one to reset it; redstone on a cell runs that cell's bucket.
- **Breaking** a section drops it and its buckets. An older world's one-block bucket drops as an item; its one-block incoming
  section grows into its 1 × 3 × 1 where there is room.
