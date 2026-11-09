# Generator bus protection (Phase 10d1, proposed 2026-11-04)

The 13.8 kV generator bus (11 kV IEC) is the bus duct between a generator's breaker and the transformers it feeds: the GSU and the
unit auxiliary transformer (gas-turbine.md, turbine-island.md). Until 10d nothing protected it. The generators tripped only on
their own stator overcurrent and frequency. This wave adds:
- the relays a real station has on that bus;
- a breaker for each feeder off it;
- the tool that faults it.

**Decided with Tim on 2026-11-04:**
- **The relays:**
  - 87B bus differential;
  - 51V voltage-restrained overcurrent on each generator breaker;
  - 50/51 on each feeder.
- **The panel:** one relay panel holds them all, and is reset by hand.
- **Bus faults:** made with a grounding clamp set, a real tool. Applied to a dead bus it keeps it safe to work on; applied to a
  live one it is a bolted fault the relays must clear.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterparts
| Part | Counterpart | Values |
|---|---|---|
| Bus relay panel | A generator-bus protection panel: a bus differential relay (SEL-487B class), a generator relay (SEL-700G class) and feeder relays (SEL-751 class), with a lockout relay (86B) | 0.8 m wide, 2.3 m tall, 0.6 m deep |
| Feeder breaker | A 15 kV metal-clad draw-out vacuum breaker cubicle (Eaton VCP-W 15 class, IEEE C37.20.2) | 1,200, 2,000 or 3,000 A; 0.9 m wide, 2.4 m tall, 2.4 m deep |
| Grounding clamp set | A temporary protective grounding cluster (ASTM F855): three clamps on 2/0 copper cables to a cluster bar | about 1 mOhm from each phase to ground *(proposed)* |

## The relay elements
**87B, bus differential:**
- **What it measures:** the bus has no load of its own, so the currents entering it at its boundary must sum to zero. By
  Kirchhoff, their sum is the current leaving the bus through a fault inside it. The boundary is each generator breaker and each
  feeder breaker.
- **When it operates:** when, on any phase, the fault current exceeds `max(0.2 x the largest breaker's rating, 0.3 x the sum of the
  boundary currents)`. This is a percentage-restrained characteristic *(proposed settings, SEL-487B class)*.
- **What it trips:** every breaker on the bus, at once. Lockout relay 86B then holds them open until the panel is reset.
- **A fault outside the bus** (past a feeder breaker, or inside a generator) passes through the bus. Its current enters at one
  boundary and leaves at another, so 87B does not operate.

**51V, voltage-restrained overcurrent, on each generator breaker (IEEE C37.102):**
- **Pickup:** 1.5 x the generator's full-load amps, times `max(0.25, V/Vn)` at its terminals. A sagging voltage (a fault) lowers
  the pickup.
- **Curve:** IEEE C37.112 very inverse, `t = TD x (3.922 / (M² − 1) + 0.0982)`, with time dial 1 *(proposed)*.
- **What it trips:** that generator's breaker.

**50/51 on each feeder breaker:**
- **51:** picks up at the breaker's rating; IEEE moderately inverse, `t = TD x (0.0515 / (M^0.02 − 1) + 0.114)`, time dial 1. These
  are the substation's settings *(proposed)*.
- **50:** instantaneous, at 8 x the breaker's rating.
- **What it trips:** only its own feeder breaker.

**Order:** the relays act on the solve in which the fault appears, before any heating (stator, wire, transformer) is counted. A
real relay acts in about a cycle.

## Parts
### `wildcatter:bus_relay_panel`: the bus relay panel
1 x 3 x 1 (1 wide, 3 tall, 1 deep), facing north, bottom layer first:
```
layer 0   layer 1   layer 2
   M         P         P
```
- `M`: main cell, holding the block entity.
- `P`: the panel: its relays, meters, the 86B handle and the lamps.
- **How it forms:** it protects the `bus_duct_gen` run it stands against: any cell of the panel touching a bus duct cell.
- **The zone:** every bus duct cell joined to that run, up to the breakers that bound it. The zone is found again whenever the
  circuit is rebuilt.
- **No bus duct beside it:** it reads `NO BUS: set it against the 13.8 kV bus duct`.
- **All four facings.** It has no terminals: the relays read the currents through their CTs.

### `wildcatter:gen_feeder_breaker`: the 15 kV feeder breaker
3 x 3 x 1 (3 along the line, 3 tall, 1 across). Facing north, the line runs west to east, as a substation breaker's does (substation.md):
```
layer 0      layer 1      layer 2
M B B        C C C        C C C
```
- `M`: main cell, holding the block entity, and the west terminal.
- `B`: the cubicle's bottom: the breaker on its draw-out truck. The east end cell is the east terminal.
- `C`: the cubicle above it: its relay and control compartment, and the bus compartment.
- **Terminals:** the bottom cell at each end of the line, where the bus duct meets it.
- It is three-pole and relay-operated: it never trips on its own current. Its relay is in the bus relay panel.
- **Rating:** sneak-right-click steps it through 1,200, 2,000 and 3,000 A.
- **Readout:** "Feeder breaker (13.8 kV metal-clad, vacuum, 2,000 A, opened by the bus relay panel): CLOSED, carrying 862 A", or "TRIPPED
  by the relay panel (50 instantaneous, 17,240 A)".

### `wildcatter:grounding_set`: the grounding clamp set (an item)
- **Applying:** right-click a cable, busway or bus duct cell with it. Three clamps go on that cell: phases A, B and C to ground,
  through the cluster.
  - The cell reads `GROUNDED (protective grounds applied)`.
  - Right-click it again with the set to take them off.
- **On a dead conductor:** the grounds hold it at earth. Nothing that energizes it later can lift its voltage, and the fault is
  cleared at once by the relays.
- **On a live conductor:** it is a bolted three-phase fault. The flashover sounds and sparks fly at the clamp. The relays (or a
  breaker's own trip, on other tiers) must clear it.
- The clamps are not destroyed.
- **Crafting:** three copper ingots, one iron ingot and a stick *(proposed)*.
- It works on every tier's cables, busways and bus duct; bare wires already fault by touching.

## The 15 kV feeder breaker's place on the bus
In a unit-connected plant the generator breaker feeds the GSU directly, and the bus between them is the zone. A feeder breaker goes
on each branch off the bus: to the GSU, to the UAT, or to a 13.8 kV load bank. Past it, the duct is the feeder's, not the bus's.
```
generator ─ gen_breaker ─┬─ bus duct ─ gen_feeder_breaker ─ GSU
                         ├─ bus duct ─ gen_feeder_breaker ─ load bank or UAT
                    bus_relay_panel
```
- The panel's zone is the duct between the generator breaker and the feeder breakers.
- A branch without a feeder breaker (a GSU or UAT straight off the bus) is inside the zone. 87B sees a fault in that transformer's
  bushings as a bus fault, and a fault inside the transformer is cleared by 51V.

## Readout (right-click)
"Bus relay panel: 13.8 kV bus, 1 generator breaker, 2 feeders; 87B ready (0 A difference); 51V gas turbine generator 862 A
of pickup 1,293 A; feeder 1 (2,000 A) 520 A; feeder 2 (1,200 A) 340 A".

**After a trip:**
- "87B BUS DIFFERENTIAL TRIP: 17,240 A in the bus, LOCKED OUT (86B); right-click to reset once the fault is cleared".
- Resetting with the fault still there trips again at once.
- **Resetting** (right-click) releases 86B, closes the feeder breakers it opened (by 87B or by their own 50/51), and resets the
  generators it tripped.
  - A generator trip is a unit trip, so its prime mover has run down: a gas turbine purges, lights and runs up again.
  - The generator then synchronizes and closes its own breaker.

## Breaking
- **The panel:** breaking it drops it; the bus is then unprotected again.
- **A feeder breaker:** breaking it drops it and opens the feeder. Either drops its item, as other true-size parts do.

## Left out (Known gaps)
- The relay settings are fixed. Only the feeder breaker's rating is set (the protection gap wave makes the feeder relays settable).

## As built: the protection gap wave (2026-11-09; decisions in docs/questions/protection-gaps.md)
- **87T, transformer differential (SEL-787 class),** on every true-size, three-phase transformer of 10 MVA and up: the 25 and 60 MVA
  GSUs and the 10 MVA 4.16/138 kV unit (the 20 MVA substation transformer keeps its yard relay panel).
  - **The zone:** the transformer and its leads on each side (cable, busway, bus duct, a station bus and its disconnects) up to the
    breakers whose CTs bound it: a 138 kV or 12.47 kV yard breaker, a 15 kV feeder breaker, a switchgear section, a generator breaker.
    It is found again each time the circuit is built. A GSU straight off a generator bus has the bus inside its zone.
  - **It operates** when the current into grounds on its leads, in per unit of the transformer's rated current on that side, is over
    `max(0.3, 0.25 x the through current in per unit)` (O87P 0.3, SLP1 25 %).
  - **It trips** every breaker bounding the zone at once (a generator breaker by a unit trip) and 86T locks them out: a breaker closed
    again while it is locked out opens at once. Right-click the transformer with an empty hand, once it is dead, to reset 86T; it
    closes what it opened and resets the generator to resynchronize.
  - **Readout:** "87T ready (0.00 per unit difference), zone bounded by 2 breakers", or "87T TRANSFORMER DIFFERENTIAL TRIP: 9.0 per
    unit in the zone (pickup 2.25), LOCKED OUT (86T)".
- **87G, generator differential (SEL-700G class),** on the big generators (gas turbine, steam turbine, aeroderivative, 60 MVA
  turbine generator). The grounding set goes on the generator itself (any of its cells): grounds on its terminals, between its
  neutral CTs and its breaker's. It operates over `max(0.1, 0.1 x the through current)` per unit of its full-load current and trips
  the unit at once (86G); the prime mover's reset releases it.
- **The GSU's line behind a bus fault:** the bus relay panel's zone now notes a GSU straight off the bus. 86B transfer-trips the
  breakers bounding the GSU's 138 kV side, so a 138 kV line can no longer back-feed a bus fault through it; the panel's reset closes
  them again. Its readout adds "1 line-side breaker behind a GSU (transfer trip)". Such a fault also lies inside the GSU's 87T zone,
  so 86T may lock out too, and both are reset.
- **The clamps burn:** a set is ASTM F855 grade 3 (2/0 copper), whose ultimate rating is 35 kA for 15 cycles, I²t 3.06 x 10^8 A²s.
  - Each solve the most loaded clamp's current is counted: a fault cleared on the solve it appeared in still flows for 5 cycles
    (the relay and a vacuum breaker); one left on flows for as long as it lasts. Below 300 A (the cable's continuous rating) it cools
    back over 5 minutes.
  - Past the rating the clamps burn off: a flash, molten copper, and the cell reads "UNGROUNDED: the grounding clamps BURNED OFF".
    The set item itself is not used up (it never was). While on, the cell shows the share of the rating used.
  - So a 40 kA bus fault cleared by 87B leaves the set whole, and 10 kA that nothing clears burns it in about 3 s.

## Game test (`busProtection`)
1. **The plant:** a gas turbine (20.6 MVA), its generator breaker, a bus duct to two feeder breakers, each feeding a 13.8 kV load
   bank, and the bus relay panel against the bus.
2. **The zone:** the panel's readout names 1 generator breaker and 2 feeders.
3. **A bus fault:** grounds on the bus between the breakers trip the generator and both feeders by 87B, and 86B locks out. Taking
   the grounds off and resetting the panel brings the bus back, and the load bank runs again.
4. **A feeder fault:** grounds on the duct past the load-bank feeder breaker. That feeder's 50 trips it alone; the generator stays
   on and 87B does not operate.
5. **A dead section:** with the feeder open, grounds on its dead duct read GROUNDED, and nothing trips.
6. **Screenshot:** the bus with its panel.
