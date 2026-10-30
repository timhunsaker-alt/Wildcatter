# Substation (PROPOSED)

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

## Game test
Build it, check it forms, energize the HV side, check 12.47 kV appears on the LV bus, trip a relay and check
the breaker opens.

## Migration
The current single `substation_transformer` block is removed.
