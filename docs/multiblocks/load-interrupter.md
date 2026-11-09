# 5 kV load-interrupter switch (Phase 10d3, proposed 2026-11-04)

A metal-enclosed load-interrupter switch on a 4.16 kV (3.3 kV IEC) circuit: the primary switch of a unit substation, or a sectionalizing
switch on a plant's 4.16 kV cable. It opens load but not a fault; the fused one carries power fuses that clear faults.

**Decided with Tim on 2026-11-04:** two parts, a fused LIS and a plain one.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterparts
| Part | Counterpart | Values |
|---|---|---|
| `lis_5kv` | A 5 kV metal-enclosed load-interrupter switch (IEEE C37.20.3) | 600 A continuous and load-break; fault-close rated; 0.9 m wide, 2.3 m tall, 1.5 m deep |
| `lis_5kv_fused` | The same with three current-limiting power fuses | E-rated fuses 10E to 200E |
| `fuse_e_10`, `_25`, `_50`, `_100`, `_200` | E-rated current-limiting power fuses (ANSI C37.46) | melt in 300 s at 200 to 240 % (up to 100E), 600 s above 100E; current-limiting above 20 x *(proposed)* |

## Parts
1 x 3 x 2 (1 wide, 3 tall, 2 deep). Facing north, the front (the door, the handle and the viewing window) is north. Bottom layer
first, each layer north (front) to south (back):
```
layer 0   layer 1   layer 2
   M         S         S        front: the switch (M holds the block entity) and its operating mechanism
   L         F         K        back: the load terminal L, the fuses F, the line terminal K
```
- `K`: the line terminal, on the top face of the top back cell. The incoming `cable_mv` lands here.
- `L`: the load terminal, on the back face of the bottom back cell.
- **All four facings.**
- N and G run through, unswitched.

### `wildcatter:lis_5kv` and `wildcatter:lis_5kv_fused`
- **Controls:**
  - right-click opens or closes the switch. Closing into a fault is safe (fault-close rated): the fuses or the upstream breaker
    clear it;
  - on the fused one, right-clicking with an E fuse item, the switch open (the door interlock), fits it in the first empty or blown
    clip. Sneak and right-click with an empty hand, open, pulls the good fuses out.
- **Opening above 600 A *(proposed)*:** the switch can't interrupt that. It fails: an arc flash (the disconnect's arc: a flash, a
  crack, and anyone close is burned) and the switch is WRECKED: it stays open for good and must be broken and replaced. The readout
  warns while the current is over its load-break rating.
- **The fuses:** the safety switch's I²t heating, with 384 s at 200 % (so 300 s at 220 %; 768 s above 100E), and an instant melt
  above 20 x the rating. A blown fuse opens only its own phase.

## Readout
"Load-interrupter switch 4.16 kV (600 A load-break, fused 50E): CLOSED, 31 A; fuses A good, B good, C good".
"WRECKED: opened under 1,240 A, over its 600 A load-break rating; break it and fit a new one".

## Breaking
Drops itself and its good fuses.

## Left out (Known gaps)
- No ground switch; no key interlock with a downstream breaker.
- The E-rated fuses' melting curve is I²t-shaped (no published curve shape beyond it).

## As built: the protection gap wave (2026-11-09)
- The E-rated fuses are current-limiting above 20 x their rating, on the class J's law (safety-switch.md), with a 50 kA interrupting
  rating *(proposed, ANSI C37.46 class)*; the readout names the last fault's prospective current, peak let-through and I²t, and the
  coordination with the device upstream (a switchgear feeder relay, through its TripCurve).

## Game test (in `panels`)
1. A switchgear feeder feeds a `lis_5kv_fused` with 25E fuses, then a unit substation and its heaters: it carries the load.
2. The grounding set on the LIS's load cable blows its fuses, and the feeder's breaker stays closed.
3. A `lis_5kv` in the line opens under the normal load without harm and closes again.
4. With its load-break rating lowered for the test, opening it under load wrecks it.
