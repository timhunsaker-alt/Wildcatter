# Phase 1c design: conductors (SUPERSEDED, see the decision below)

**Decided 2026-10-30 (after the cable-block proposal below showed that every machine terminal and in-line device would need reworking):**
one **selectable single-conductor wire**. Same wire blocks as before; a `conductor` blockstate property (A, B, C, N, G) is stepped by right-clicking with an empty hand,
and the coloured band shows which. Old wires keep working: `*_wire_hot` starts as A and `*_wire_neutral` as N, and a world's saved wires load with that default.
Bare wires keep today's short-circuit behaviour. Multi-conductor cable blocks can be built on top of this later.
Split-phase first, three-phase after. Built in Phase 1c: `Conductor` enum, selectable wires, North American split-phase low side on transformers
(leg A on the back and bottom, leg B on the sides, N on all four, 180 degrees apart), loads accept A or B, `G` wires tie to earth.
Three-phase (C) and the three-pole breaker come with the phasor solver wiring.

---
The original proposal follows, kept for the record.
# Phase 1c design: conductors and cable blocks (PROPOSED)

Phase 1a (phasor solver) and 1b (electrical standard) are done. 1c is the step that finally puts the phasor solver in the game,
so it changes every wire and every machine. CLAUDE.md says to propose rather than invent, so this is a proposal with the
decisions that are yours marked **DECISION**.

## Real-world counterpart
- Residential service: **split-phase**, two 120 V legs 180 degrees apart plus a neutral and a ground (NA 120/240 V). IEC: single-phase 230 V, line + neutral + earth.
- Industrial and distribution: **three-phase** with four or five conductors (A, B, C, N, G), phases 120 degrees apart.
- Ground (G, "earth") is the safety conductor, bonded to the neutral only at the transformer and at the service entrance (ELECTRICAL_MODEL).

## Conductors
`Role` (HOT, NEUTRAL) becomes `Conductor`: **A, B, C, N, G**. Joining rules stay as today: ports meet across a block edge, same conductor
and same tier join through the wire's resistance; two bare, different conductors touching make a short. G joins G; N and G are
bonded only where the standard says.

## Cable blocks (decided: one cable block per voltage tier)
One cable block per (tier, insulation) with a conductor set carried by the *block id*, not by a property:

| Block | Conductors | Used for |
|---|---|---|
| `cable_1p` (per tier, bare / wool / plastic) | A, N, G (IEC) or A, B, N, G (NA split-phase) | Houses, small machines |
| `cable_3p` (per tier, bare / wool / plastic) | A, B, C, N, G | Industry, distribution |

That is 3 tiers x 2 sets x 3 insulations = 18 blocks, the same count as today's 3 x 2 roles x 3. A cable model draws its conductors as
coloured stripes (A red, B black, C blue, N white, G green/yellow) so you can read the cable.

## Connections
- Cable to cable: conductor by conductor. A 1p cable meeting a 3p cable joins only the conductors they share (A, N, G).
- Machine terminals name their conductors per face: a lamp or heater is **A to N** (120 V on NA, 230 V on IEC); a 240 V load is **A to B**; a three-phase machine uses **A, B, C** (+ N where it needs one).
- A single-phase transformer with a centre-tapped secondary (NA) makes A and B 180 degrees apart with N at the tap: that is how 120/240 V exists.
- Sources are voltage sources behind impedance at the right angle: A at 0, B at -120 (three-phase) or 180 (split-phase), C at +120.

## Solver
`Network` switches to `PhasorSolver`: conductors are nodes; the frequency is the standard's (60 or 50 Hz); resistive and reactive loads follow. Shock and
short logic read per-conductor volts. Protection devices act on the phasor current magnitude.

## Existing blocks and worlds (**DECISION**)
| Option | What happens | Risk |
|---|---|---|
| **A. Legacy aliases (recommended)** | The old `wire_hot` / `wire_neutral` ids stay registered for one release and behave as single-conductor A and N wires; new cables are added beside them. Nothing breaks when a world loads. | Two kinds of wire for a while; old wires cannot carry B/C/G. |
| B. One-time converter | A data fixer rewrites every old wire block to the matching cable block on load. | A bug here can break builds in existing worlds; needs its own game test that loads old blocks. |

## Machines (all use the 1p cable's A and N, or A and B, today)
Lamp, heater, pump, drilling rig, pumpjack, refinery, cooling tower, breakers, fuse, switch, transformers, steam turbine, engines. Breakers, fuse and switch
become per-conductor (a breaker for each phase, or ganged). **DECISION:** ganged (one device opens all phases together, as a real three-pole breaker) or one device per phase.

## Open decisions for you
1. Legacy aliases or a one-time converter (above).
2. Ganged three-pole breakers or one per phase (above).
3. Whether 1c ships the NA split-phase cable (A, B, N, G) first and three-phase later, or both together. Splitting it halves the risk of one giant change.

## Game test (added with the code)
A 120/240 V split-phase house circuit (A-N, B-N and A-B loads, neutral current is the difference), a three-phase load with a motor-style resistive
load balanced across A, B, C, an unbalanced case, and an old world's wire blocks loading and working (under the chosen migration option).
