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
The single-block 138/12.47 kV transformer (`distribution_transformer` in the code) is removed. (`substation_transformer` in the code is the 4.16 kV to 138 kV step-up; it stays until Phase 6.)
