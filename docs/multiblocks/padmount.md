# Pad-mount transformers and the unit substation at true size (Phase 4.5e)

**Status: APPROVED by Tim on 2026-10-31 with the 4.5e plan.** Each is one item that fills its real size. Ratings, impedances and
connections are unchanged (phase3-three-phase.md).

| Part (id) | Cells (W × H × D) | Real-world basis |
|---|---|---|
| `padmount_208`, 75 kVA, 12.47 kV to 208Y/120 V, 3 %Z | 2 × 2 × 2 | about 1.37 W × 1.27 H × 1.25 D m: the tank with cooling fins behind, a front cabinet with the high-voltage compartment on the left and the low-voltage one on the right, penta-head bolted doors |
| `padmount_480`, 300 kVA, 12.47 kV to 480Y/277 V, 5 %Z | 2 × 2 × 2 | about 1.75 W × 1.40 H × 1.60 D m, the same arrangement, drawn larger within its cells |
| `unit_substation`, 1,000 kVA, 4.16 kV to 480Y/277 V, 5.75 %Z | 4 × 3 × 2 | one shipped lineup 2.3 m high and 1.7 m deep: a 4.16 kV fused load-break switch section (0.9 m), the transformer with radiators (2.0 m), and a 480 V section with its main breaker (0.9 m) |

Facing north the front (doors) faces north; the main cell is the bottom front-west cell.

## Terminals
- Pad-mounts: the cables come up from below into the two front compartments.
  - High side: the bottom front cell on the left as you face the doors (east when facing north): its outer side face and its underside.
  - Low side: the bottom front cell on the right (the main cell, west when facing north): its outer side face and its underside.
- Unit substation: the 4.16 kV terminal on the switch section's end (the main cell's west face when facing north, and its
  underside); the 480 V terminal on the breaker section's end (the bottom front east cell's east face, and its underside).

## Placing and older worlds
- Placed facing the player, with a ghost outline; it needs its whole size clear. Breaking any piece drops the one item.
- An older world's one-block unit grows into its cells where there is room, like the GSU; where there is not it drops as an
  item with a chat message.

## Game test
Every scene that used a pad-mount or the unit substation is rewired to these terminals and checks the same values as before;
`lineParts` checks that an older world's one-block pad-mount grows.
