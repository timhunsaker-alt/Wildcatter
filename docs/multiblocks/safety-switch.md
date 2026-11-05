# 480 V fused safety switch (Phase 10d3, proposed 2026-11-04)

A heavy-duty fusible safety switch: the disconnect beside a motor or a machine on a 480 V (400 V IEC) circuit, its three fuses
behind the door.

**Decided with Tim on 2026-11-04:** its fuses are cartridge fuse items.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterparts
| Part | Counterpart | Values |
|---|---|---|
| `safety_switch_100` | A heavy-duty 600 V fusible safety switch, 100 A frame (UL 98) | 0.3 m wide, 0.6 m tall; fuses 30, 60 or 100 A |
| `safety_switch_200` | The same, 200 A frame | 0.45 m wide, 1.1 m tall; fuses up to 200 A |
| `fuse_class_j_30`, `_60`, `_100`, `_200` | Class J time-delay fuses (UL 248-8) | carries 500 % for 10 s; current-limiting above 15 x *(proposed)* |
IEC: a fuse switch-disconnector with gG fuses.

## Parts
Wall-mounted. `safety_switch_100` is 1 x 1 x 1; `safety_switch_200` is 1 x 2 x 1, its main cell the bottom one. All four facings.
```
 safety_switch_200, facing it:   S    top cell: the line comes in on its top face
                                 M    main cell: the load leaves its bottom face
```
- **Terminals:** the line (A, B, C, N and G) comes in on the top face of the top cell; the load leaves the bottom face of the bottom
  cell. N and G pass through unswitched.
- **The handle:** right-click works it, ON or OFF. It breaks full load (horsepower rated).
- **The fuses:** three, one per phase.
  - Right-clicking with a fuse item fits it into the first empty or blown clip, with the handle OFF (the door interlock); a blown fuse
    comes out as nothing.
  - A fuse bigger than the frame won't fit.
  - Sneak and right-click with an empty hand, handle OFF, pulls the fuses out as items (the good ones).
  - The switch carries current only ON with all three fuses fitted and good.
- **The curve:** the one-block fuse's I²t heating, with 80 s at 200 % (so 10 s at 500 %), and an instant melt above 15 x the
  rating. A blown fuse opens only its own phase: the switch reads which one, and a three-phase motor on it single-phases.

## Readout
"Safety switch 480 V, 100 A frame: ON, 31 A; fuses 60 A class J: A good, B BLOWN, C good; handle OFF to change a fuse".

## Breaking
Drops itself and its fitted fuses (good ones as items).

## Left out (Known gaps)
- No fuse-and-breaker coordination study; no let-through (I²t) modelling of current limiting.

## Game test (in `panels`)
1. A unit substation feeds a `safety_switch_100` with three 30 A fuses, then a 15 kW heater: it carries the load.
2. The grounding set on one phase of the load side blows that phase's fuse, and the switch names it.
3. A fuse won't fit with the handle ON; with the handle OFF a new one fits and the heater runs again.
