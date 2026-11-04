# 4.16 kV metal-clad switchgear (Phase 10d2, proposed 2026-11-04)

A plant's 4.16 kV (3.3 kV IEC) lineup: the sections that take the unit substation's or UAT's 4.16 kV and give it to the feeders,
each through a draw-out vacuum breaker with its own relay. It replaces the one-block `mv_breaker` stand-in, which is removed.

**Decided with Tim on 2026-11-04:**
- **Build:** sections that bus together side by side, any number of them.
- **Two parts:** a MAIN section, where the source comes in, and a FEEDER section, where a feeder leaves. A lineup has one main.
- **Racking:** the breakers rack out: CONNECTED, TEST, DISCONNECTED.
- **Pickup:** the 51 pickup is settable, as a real relay's CT ratio and tap set it, with the Power Meter.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterparts
| Part | Counterpart | Values |
|---|---|---|
| Switchgear section, main or feeder | A 5 kV metal-clad section with a draw-out vacuum breaker (Eaton VCP-W 5 class, IEEE C37.20.2) | 0.9 m wide, 2.4 m tall, 2.3 m deep; breaker frame 1,200, 2,000 or 3,000 A |
| Its relay | A feeder protection relay in the section's door (SEL-751 class), on the breaker's CTs | 50/51; pickup 5 to 1,200 A |

## The relay
- **51, time overcurrent:**
  - IEEE C37.112 moderately inverse, `t = TD x (0.0515 / (M^0.02 − 1) + 0.114)`, with time dial 1. These are the curve and dial of
    10d1's 13.8 kV feeders.
  - **Pickup:** set in steps of 5, 10, 25, 50, 100, 200, 400, 600 and 1,200 A, up to the breaker's frame *(proposed: a 50:5 CT on a
    0.5 A tap gives the bottom step)*. A new section starts at 100 A.
- **50, instantaneous:** at 8 x the pickup, on the solve in which the fault appears.
- **What it trips:** only its own breaker. A fault on a feeder's cable is cleared by that feeder, and the main sees it pass
  through. The main's own relay backs it up: set the main's pickup above the feeders'.

## Parts
Both sections are 1 x 3 x 3 (1 wide along the lineup, 3 tall, 3 deep). Facing north, the front (the doors, the breaker) is north
and the lineup runs west to east. Bottom layer first, each layer north (front) to south (back):
```
layer 0   layer 1   layer 2
   M         R         R        front: the breaker compartment (M) and the relay and control door above it (R)
   B         B         B        middle: the main bus compartment
   T         C         K        back: the cable compartment; its terminal T (main) or K (feeder)
```
- `M`: main cell, holding the block entity: the breaker compartment and its draw-out vacuum breaker.
- `R`: the relay and control compartment, the relay in its door.
- `B`: the main bus compartment. The bus runs through the side faces (west and east when facing north) of the **bottom** `B` cell.
- `C`: the cable compartment.
- `T` (main section): the incoming terminal, on the back face of the bottom back cell. The 4.16 kV cable (`cable_mv`) from the
  transformer comes in here.
- `K` (feeder section): the outgoing terminal, on the top face of the top back cell. The feeder's `cable_mv` leaves here.
- **All four facings.**

### `wildcatter:mv_switchgear_main` and `wildcatter:mv_switchgear_feeder`
- **How a lineup forms:**
  - Sections touching side by side, facing the same way, join their main buses. The bus has no impedance of its own.
  - A lineup is the run of sections joined that way. Its sections are numbered from the left as you face the front.
- **The lineup rule:**
  - A lineup needs exactly one main.
  - With none, or more than one, every breaker in it stays open and reads `NO MAIN` or `TWO MAINS` *(proposed)*.
- **The breaker:** three poles. The neutral and ground run through, unswitched, as on a three-pole breaker. Closed and
  CONNECTED, each phase links the bus to the section's cable terminal through the breaker's contacts.
- **Racking:** the draw-out truck has three positions:
  - **CONNECTED:** the primary disconnects are engaged, and the breaker carries the circuit when closed.
  - **TEST:** the primaries are parted. The breaker can be closed and opened to test it, but carries nothing.
  - **DISCONNECTED:** the truck is drawn out. The breaker cannot be closed.
  - It racks only while the breaker is open (interlocked, as a real truck is).
- **Controls:**
  - **Right-click:** opens a closed breaker, and closes an open one. On a tripped breaker, it resets the relay and closes.
  - **Sneak and right-click with an empty hand:** racks the breaker CONNECTED → TEST → DISCONNECTED → CONNECTED.
  - **Sneak and right-click with the Power Meter:** on the top cell, it steps the frame (1,200, 2,000, 3,000 A); on any other cell,
    it steps the 51 pickup *(proposed)*.
  - **Right-click with the Power Meter:** reads the section.

## Readout (right-click)
- **Feeder:** "4.16 kV switchgear, FEEDER 2 of 3 (1,200 A frame, vacuum): CLOSED, CONNECTED, 18 A; 51 pickup 25 A (MI, TD 1),
  50 at 200 A".
- **Main:** "4.16 kV switchgear, MAIN 1 of 3 (2,000 A frame, vacuum): CLOSED, CONNECTED, 36 A; 51 pickup 200 A (MI, TD 1), 50 at
  1,600 A; lineup: 1 main, 2 feeders".
- **After a trip:** "TRIPPED (51 time overcurrent, 31 A on a 25 A pickup); right-click to reset and close", or "(50
  instantaneous, 4,820 A)".
- **Racked out:** "OPEN, DISCONNECTED (racked out: it cannot close)".

## Breaking
- Breaking a section drops it.
- The lineup splits there: the sections on either side form their own lineups, and the rule above applies to each.

## Left out (Known gaps)
- Racking is instant: there is no racking time, and no remote racking.
- No breaker failure protection (50BF): a breaker always opens when its relay trips it.
- No bus differential on the 4.16 kV bus. A fault on the bus itself is cleared by the main's 51, or by the source's protection.
- The relay's curve and dial are fixed. Only the pickup and the frame are set.

## Game test (`switchgear`)
1. **The supply:** a diesel genset and a unit substation run backward give 4.16 kV to a MAIN section. Two FEEDER sections stand to
   its east. Feeder 1's cable runs to a unit substation with 480 V heaters.
2. **The lineup:** the main reads 1 main and 2 feeders.
3. **An overload:** feeder 1's pickup is set to 5 A, below its load. Its 51 trips it after the MI time, and the main stays closed.
   Right-click resets it.
4. **A fault:** the grounding set on feeder 2's cable. Its 50 trips feeder 2 alone.
5. **Racking:** feeder 2 is racked to DISCONNECTED. A close is refused, and it reads DISCONNECTED.
6. **Screenshot:** the lineup.
