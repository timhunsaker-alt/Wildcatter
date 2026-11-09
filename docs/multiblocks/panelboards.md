# Load centre, panelboard and branch breakers (Phase 10d3, proposed 2026-11-04)

The gear between a transformer's low side and the loads of a building: a house's 120/240 V load centre (230 V consumer unit IEC)
and a plant's 480Y/277 V panelboard (400Y/230 V IEC). Each holds a main breaker; its branch breakers clip into **gutter** blocks set
beside it, one branch space each.

**Decided with Tim on 2026-11-04:**
- **Branches:** the panel is true size. Gutter blocks beside it each add one branch space, and its circuit leaves the gutter's
  front face.
- **Branch breakers:** one item per rating.
- **GFCI:** a GFCI breaker is added (an RCD under IEC).
- **Branch cable** (asked while building): neighbouring gutters' front cells touch, and cables join on every face, so a new
  branch cable carries each branch: it joins only at its two ends.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterparts
| Part | Counterpart | Values |
|---|---|---|
| `load_centre` | A 200 A, 40-space main-breaker load centre (QO class) | 0.36 m wide, 1.0 m tall; main 2-pole 200 A; 10 kA |
| `panelboard_480` | A 225 A, 42-circuit main-breaker lighting panelboard (PRL4 class) | 0.5 m wide, 1.8 m tall; main 3-pole 225 A; 14 kA |
| `panel_gutter` | A wireway section beside the panel, here holding one branch space *(proposed)* | 1 x 1 x 1 |
| Branch breakers, load centre | Plug-on thermal-magnetic breakers, 120/240 V | 1-pole 15 and 20 A; 2-pole 30 and 50 A |
| `gfci_breaker_1p_20` | A 1-pole 20 A GFCI breaker (UL 943 Class A) | trips at 5 mA imbalance (4 to 6 mA); IEC: a 30 mA RCBO |
| Branch breakers, panelboard | Bolt-on thermal-magnetic breakers, 480Y/277 V | 1-pole 20 A 277 V; 3-pole 20, 30, 60 and 100 A |
| `branch_cable`, `branch_cable_industrial` | Armored branch-circuit cable (type MC; SWA under IEC) in a straight run | A, B, C, N and G; joins only at its two ends |

## Parts
All three are wall-mounted single cells: the back is to the wall, the front (the door, or the gutter's cover) faces you. All four
facings. The panelboard is 1 x 2 x 1 (its main cell is the bottom one).

```
 side view of a wall, facing it:      G  G        G = panel_gutter (any of them, up, down or sideways)
                                      G  P  G     P = load_centre (or the bottom cell of panelboard_480)
                                         G
```

### `wildcatter:load_centre` and `wildcatter:panelboard_480`
- **Feed:** the service cable lands on the panel's top face or its back face: A, B, N and G on a load centre (A, N and G under IEC);
  A, B, C, N and G on a panelboard.
- **Main breaker:** thermal-magnetic on the largest phase current, with the same curve as the one-block breaker
  (`BREAKER_TRIP_SECONDS_AT_2X`, magnetic at `BREAKER_MAGNETIC_MULTIPLE` x). The neutral and ground run through it unswitched.
- **The bus:** behind the main, it leaves the panel's four faces in the wall's plane (up, down and both sides) on a private key that
  only panels and gutters use.
- **Controls:** right-click toggles the main, and resets it after a trip. Right-click with the Power Meter reads the panel.

### `wildcatter:panel_gutter`
- **How a chain forms:** a gutter touching the panel in the wall's plane, facing the same way, joins it. A gutter touching a gutter
  already in the chain joins too. Spaces are numbered in the order the chain reaches them from the panel (nearest first; down, up,
  left, right at each step).
- **Phases:** each space's bus stab, as on a real panel's bus, alternates with its number:
  - load centre: A, B, A, B and so on (all on L under IEC);
  - panelboard: A, B, C, A and so on;
  - a 2-pole breaker takes its space's phase and the next one, and a 3-pole the next three.
- **Spaces:** the poles of the breakers in a chain add up to at most 40 (load centre) or 42 (panelboard). A breaker past that, a
  gutter that touches no panel, or one whose chain reaches two panels, reads so and carries nothing.
- **Its circuit:** the breaker's hot pole(s), the neutral and the ground leave the gutter's front face. Run it out in BRANCH CABLE
  (residential or industrial, to match the panel): placed against a face, a branch cable runs along that face's axis and joins only
  at its two ends, so the runs out of neighbouring gutters stay apart. An ordinary cable there would join its neighbours. A 1-pole breaker's hot joins
  whichever single phase meets it; a 2-pole breaker gives A and B (L and N under IEC: it switches the line and the neutral), and a
  3-pole A, B and C.
- **Controls:**
  - right-clicking an empty gutter with a branch-breaker item clips it in, when it fits that panel (load-centre breakers only on a
    load centre, panelboard breakers only on a panelboard) and the panel has the spaces;
  - right-click with an empty hand toggles the breaker, and resets it after a trip;
  - sneak and right-click with an empty hand pulls an open breaker back out as its item. A closed breaker won't come out;
  - breaking the gutter drops its breaker.

### The branch breakers
- **Thermal:** the one-block breaker's I²t curve.
- **Magnetic:** at `BREAKER_MAGNETIC_MULTIPLE` x the rating, on the solve in which the fault appears.
- **GFCI:** the neutral of a GFCI branch runs through the breaker, as a real one's pigtail does. When the current out on the hot and
  the current back on the neutral differ by more than 5 mA (30 mA under IEC), it trips "ground fault". That check runs before the
  magnetic one.

## Readouts
- **Panel:** "Load centre 120/240 V, main 200 A: ON, 31 A; 4 spaces of 40 used (3 breakers)".
- **Gutter:** "Space 3 (phase A), 20 A 1-pole GFCI: ON, 1.2 A; imbalance 0.0 mA", or "TRIPPED (ground fault: 4.8 A out, 0.0 A back);
  right-click to reset", or "EMPTY: clip a branch breaker in".
- **2-pole:** also its volts across the two poles ("240 V").

## Breaking
- A panel drops itself. Its gutters go dead (no panel) but keep their breakers.
- A gutter drops itself and its breaker. The gutters beyond it, if no longer reached, go dead.

## As built: the protection gap wave (2026-11-09; docs/questions/protection-gaps.md, C-entries)
- **A shock is current to earth.** A person or animal shocked by a bare live wire, or by an ungrounded lamp or heater, puts IEC
  60479-1's hand-to-feet body impedance, 1,000 ohm, from that conductor to earth for the next second of solves. Upstream it is real
  imbalance: 120 mA on a 120 V branch, far over a GFCI's 5 mA (an RCD's 30 mA), so the GFCI trips "ground fault". The shock still
  lands: a GFCI limits how long it lasts, not the first bite.
- **The main's handle is drawn by state** (`main` = on, off, tripped on `load_centre` and `panelboard_480`): up ON, down OFF,
  centred with its trip flag TRIPPED, through a window in the dead front.
- **The protection race.** On a solve with a fault, only the fastest breakers and fuses act, and the circuit is solved again
  without the fault. A molded-case breaker's magnetic trip clears in about a cycle, a GFCI on a heavy ground fault in 1.5, a
  current-limiting fuse in under half. Two breakers both in their magnetic range tie and both open, as real molded-case breakers
  do: a branch fault over the main's magnetic pickup (8 x 200 A) still opens the main. Untimed devices (relays, motor starters)
  act as before.
- **The coordination readout.** The Power Meter on a branch breaker, a panel main, a one-block breaker or fuse, a safety switch or a
  fused LIS names the device upstream (a gutter's is its panel's main; otherwise the first breaker, fuse or relay up the
  conductors from its line side, stopping at a transformer or source) and says up to what fault current the pair is selective:
  "coordination: under the 200 A main, selective only up to 1,603 A: above that the 200 A main clears as fast". The study runs from
  the downstream's rating to its interrupting rating (10 kA load centre, 14 kA panelboard).
- **AFCI: not built.** An arc fault breaker (UL 1699) trips on an arc's signature: broadband noise and the current's shoulders at
  each zero crossing, at kHz. The solver is a 50/60 Hz phasor solve: an arc in it is only a resistance, the same as any other
  fault, so an AFCI could only be a breaker that trips on a made-up rule. Left out on purpose.
- **Series ratings:** left out. A series-rated combination (a fuse or breaker protecting a breaker below its interrupting rating) is
  a tested listing of pairs, not something to derive from curves.

## Left out (Known gaps)
- No arc-fault (AFCI) breakers (above: not credible on a phasor solver).
- No series ratings, and no interrupting-rating failure: a breaker past its rating still opens.

## Game test (`panels`)
1. A `pole_transformer` feeds a load centre with four gutters: 1-pole 20 A to a heater, GFCI 20 A to a lamp, 2-pole 30 A, and
   1-pole 15 A to two heaters.
2. The chain reads 5 poles used; the spaces read phases A, B, A, B.
3. The 15 A branch trips "overload" on two 1,500 W heaters, and the main stays on.
4. The grounding set on the GFCI branch trips it "ground fault".
5. The 2-pole breaker reads 240 V (NA).
6. A closed breaker won't come out.
7. A unit substation feeds a panelboard: a 3-pole 30 A branch to three 15 kW heaters trips "overload"; the grounding set on a
   1-pole 277 V branch trips it "short circuit".
