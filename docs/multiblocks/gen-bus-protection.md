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
- The clamps never burn, however long a fault lasts (real clusters are rated, say, 43 kA for 15 cycles).
- There is no transformer differential (87T) and no generator differential (87G). A fault inside a GSU or a generator is cleared by
  51V or the generator's own stator trip.
- The relay settings are fixed. Only the feeder breaker's rating is set.
- A 138 kV source back-feeding the GSU onto a bus fault is not opened by 87B. It has no breaker on the bus side.

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
