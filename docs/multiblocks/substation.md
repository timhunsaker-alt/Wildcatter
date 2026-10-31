# Substation (APPROVED 2026-10-30)

## Real-world counterpart
A **distribution substation**: 138 kV in, 12.47 kV out (both from the ELECTRICAL_MODEL NA table). Power
transformer, HV breaker, disconnect switches, buswork, low-side breaker and a relay panel in a control enclosure.

## Values (DECISION)
| Item | Value | Real-world basis |
|---|---|---|
| Transformer | 138/12.47 kV, Delta-Y, 20 MVA ONAN | Typical distribution substation transformer |
| Impedance | 8 percent Z | Typical for this class |
| HV breaker | 138 kV, 1,200 A | Standard HV breaker |
| LV breaker | 12.47 kV, 1,200 A | Standard MV breaker |

## Parts
| Char | Block id | Real part |
|---|---|---|
| `T` | `wildcatter:power_transformer` | Power transformer (controller) |
| `H` | `wildcatter:hv_breaker` | HV circuit breaker (exists) |
| `L` | `wildcatter:mv_breaker` | Low-side breaker (exists) |
| `D` | `wildcatter:disconnect_switch` | Disconnect switch |
| `B` | `wildcatter:bus` | Buswork |
| `K` | `wildcatter:relay_panel` | Relay panel |

## Layout (bottom layer first, NORTH facing)
```
 H D B T B D L
 . . . K . . .
```

## True size (Phase 4.5d, approved 2026-10-31)
Every part is one item that fills its real size (docs/multiblocks/README.md, "Big parts"). 1 block = 1 m. Layout: NORTH
facing, the line along x (138 kV west, 12.47 kV east), z across; each letter is a part's main cell, all on the ground.
```
x: 0123456789012345
z0 H.D.B...K.......
z1 ................
z2 ......T.........
z3 ............bdL.
z4 ................   <- centre line: every part's terminals
z5 ................
z6 ................
z7 ................
z8 ................
```
| Char | Block id | Real part | Size (along x high x across) | Real-world basis |
|---|---|---|---|---|
| `H` | `wildcatter:hv_breaker_3p` | 138 kV breaker, 3-pole dead-tank SF6 | 2 x 5 x 9 | Three tanks and six bushings about 5 m to the tops, with a mechanism cabinet. 138 kV phase spacing is about 3 m. |
| `D` | `wildcatter:disconnect_hv` | 138 kV disconnect, 3-phase centre-break | 2 x 6 x 9 | Galvanized structure about 4 m high. Two rotating insulator stacks per phase, their arms (about 1.4 m) meeting in the middle. |
| `B` | `wildcatter:bus_hv` | 138 kV station bus | 2 x 6 x 9 | Aluminium tube on post insulators on a structure, with droppers |
| `T` | `wildcatter:power_transformer` | Power transformer, 20 MVA, delta-wye, ONAN; **controller** | 6 x 7 x 5 | About 6 x 4 m plan plus radiators, and 7 m to the bushing tops. Conservator, Buchholz relay, tap-changer and control cabinets, arresters. |
| `b` | `wildcatter:bus_dist` | 12.47 kV station bus | 1 x 4 x 3 | 0.9 m phase spacing (15 kV class) |
| `d` | `wildcatter:disconnect_dist` | 12.47 kV disconnect, 3-phase vertical-break | 1 x 5 x 3 | 15 kV class, 0.9 m blades |
| `L` | `wildcatter:dist_breaker_3p` | 12.47 kV feeder breaker, 3-pole | 2 x 4 x 3 | 15 kV outdoor breaker on its stand |
| `K` | `wildcatter:relay_panel` | Relay panel | 2 x 2 x 1 | Outdoor relay cabinet (NEMA 3R): the 50/51 relays, the lockout and the meters |

The yard is 16 x 9 m and 7 high. With a fence round it, it is about 20 x 13 m.

How the yard works:
- **Terminals:**
  - Every part's terminals are on its west and east faces, at the bottom cell of the centre line (z 4). Placed in the
    layout, the parts' terminals touch.
  - The 138 kV line comes in at the breaker's west face. The 12.47 kV feeder leaves at the feeder breaker's east face.
- **Building:** the yard has nothing to snap to. Place the parts freely with the ghost outline. The transformer's
  readout names the next missing part, or a part turned the wrong way, with its x y z.
- **Moving parts:**
  - The 138 kV disconnect's six arms swing open, two per phase.
  - The 12.47 kV disconnect's three blades lift.
- **Fence (decoration):** chain-link fence and a barbed-wire top, each one block. They join like iron bars; stack the top
  on the fence for a 2.1 m substation fence (NESC).
- **Pad-mounts and the unit substation** become true size in wave 4.5e, with the rest of the line and building gear.

## Controller / rotation / formation / breaking
`power_transformer` (`T`) is the controller. Four facings. Forms when the parts touch; the transformer only
energizes when both breakers can see their buses. Removing a part dissolves it and the transformer de-energizes.

## Protection (Phase 4 plan, approved 2026-10-30)
The relay panel runs a time-overcurrent relay (51, moderately inverse, pickup 150 % of the transformer's full-load current) and an
instantaneous relay (50, 800 %) on the transformer, and opens both breakers. Opening a disconnect switch under load draws an arc;
disconnects are opened only after the breakers. The disconnect blades swing open (moving part).

## As built (Phase 4e)
- One line of parts, the 138 kV side first, the relay panel in front of the transformer:
```
 H D B T b d L
 . . . K . . .
```
  `H` `hv_breaker_3p`, `D` `disconnect_hv`, `B` `bus_hv`, `T` `power_transformer` (its front faces the 138 kV side), `b` `bus_dist`,
  `d` `disconnect_dist`, `L` `dist_breaker_3p`, `K` `relay_panel`. The breakers are three-pole and relay-operated (no trip elements of
  their own). The two disconnects and two buses are on different voltages: distinct parts, not tiling.
- The transformer is 138 kV delta / 12.47 kV grounded wye (the high side leads by 30 degrees) and is energized only while the line stands.
- The station buses are rated 2,000 A (PROPOSED: tubular aluminium station bus, a typical rating).
- A disconnect opened above 10 A draws an arc that burns anyone within 3 blocks (PROPOSED: disconnects break only magnetizing and charging current).

## Game test
Build it, check it forms, energize the HV side, check 12.47 kV appears on the LV bus, trip a relay and check
the breaker opens.

## Migration
Phase 4 one-block yard parts drop as items with a chat line on load (the layout changed with true size).
The single-block 138/12.47 kV transformer (`distribution_transformer` in the code) is removed. (`substation_transformer` in the code is the 4.16 kV to 138 kV step-up; it stays until Phase 6.)
