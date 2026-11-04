# Breaker relay cabinet (Phase 10d2, proposed 2026-11-04)

A standalone 12.47 kV or 138 kV breaker, one that is not part of a substation yard's protection, needs a relay of its own. The
true-size yard breakers (`dist_breaker_3p`, `hv_breaker_3p`, substation.md) never trip on their own current: a relay opens them.
This cabinet is that relay, set beside the breaker it works. It replaces the one-block stand-ins `dist_breaker`, `hv_breaker` and
`ufls_relay_dist`, which are removed.

**Decided with Tim on 2026-11-04:**
- The standalone breakers are the existing yard breakers, each worked by a relay cabinet that touches it.
- **Elements:** 50/51 on both voltages, and 81U on a 12.47 kV breaker.
- **Pickup:** the 51 pickup is settable with the Power Meter.
- **81U resets by hand** at the cabinet, as `ufls_relay_dist` did.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterpart
| Part | Counterpart | Values |
|---|---|---|
| Breaker relay cabinet | A breaker's protection and control cabinet: a feeder relay (SEL-351 class) with its test switches, lamps and control switch, on the breaker's bushing CTs | 0.6 m wide, 1.8 m tall, 0.5 m deep *(proposed)* |

## The relay
- **51, time overcurrent:**
  - IEEE moderately inverse, with time dial 1, as on the 4.16 kV switchgear (mv-switchgear.md).
  - **Pickup:** set in steps of 5, 10, 25, 50, 100, 200, 400, 600 and 1,200 A, up to the breaker's rating. A new cabinet starts at
    100 A.
- **50, instantaneous:** at 8 x the pickup.
- **81U, under-frequency (12.47 kV only):**
  - It opens the breaker when the island falls below the world standard's load-shed frequency (59.3 Hz NA, 49.0 Hz IEC). This is
    the 12.47 kV under-frequency load shedding that `ufls_relay_dist` did.
  - Its readout gives the island's frequency and the shed point.
- **What it trips:** only the breaker it touches.

## Part: `wildcatter:breaker_relay_cabinet`
1 x 2 x 1, facing north, bottom layer first:
```
layer 0   layer 1
   M         P
```
- `M`: main cell, holding the block entity: the cabinet's lower half, the test switches and the control switch.
- `P`: the relay, the lamps, and the nameplate.
- **How it forms:**
  - It works the `dist_breaker_3p` or `hv_breaker_3p` that one of its cells touches. It is found again whenever the circuit is
    rebuilt.
  - Touching two breakers, it works the first found and says so.
- **No breaker beside it:** it reads `NO BREAKER: set it against a 12.47 kV or 138 kV breaker`.
- **All four facings.** It has no terminals: the relay reads the breaker's CTs.
- **Controls:**
  - **Right-click:** after a trip, it resets and recloses the breaker. Otherwise it reads the cabinet.
  - **Sneak and right-click with the Power Meter:** steps the 51 pickup.
- A breaker inside a substation yard is still worked by the yard's relay panel (the power transformer's 50/51). A cabinet beside
  such a breaker adds its own elements too.

## Readout (right-click)
- "Breaker relay cabinet: 12.47 kV breaker (600 A), CLOSED, 42 A; 51 pickup 100 A (MI, TD 1), 50 at 800 A; 81U at 59.3 Hz,
  island 60.0 Hz".
- **After a trip:** "TRIPPED: 81U under-frequency, the island fell to 58.9 Hz, below 59.3 Hz; right-click to reclose once the
  frequency has recovered".

## Breaking
- Breaking the cabinet drops it. The breaker is left as it stands: closed, it stays closed, but nothing will trip it.

## Left out (Known gaps)
- No automatic reclosing: a recloser's shots are not modelled.
- No 81U restoration: it is reset by hand.
- The curve and dial are fixed.

## Game tests
- **`spans`:** the 12.47 kV line's breaker is a `dist_breaker_3p` with a cabinet. The line carries 7.2 kV through it.
- **`feederRelay`:**
  - A cabinet on a `dist_breaker_3p` feeds a pole transformer. Heaters overload the turbine.
  - The cabinet's 81U opens the breaker, and the generator stays on line.
  - A right-click recloses the breaker once the heaters are gone.
