# Wildcatter — Electrical model

Keep what works: the modified nodal analysis (MNA) solver, breakers, fuses, grounding, shocks,
the Power Meter. This document extends them to a real AC, three-phase grid.

The mod follows the same physical principles as real power systems. It is not a training course
and makes no claim to teach or certify electrical work.

## Decisions (settled 2026-10-30)
| Topic | Decision |
|---|---|
| Solver | Full unbalanced three-phase phasor solve |
| Overhead MV/HV lines | Spans strung between insulators on poles |
| Oil and gas transport | Steel pipe (rework the copper "gas lines") |
| Grid frequency | Dynamic from the start: sags/rises with generation vs. load |
| Electrical standard | Both North American and IEC, chosen per world |
| Link to other mods | Receptacle + crafted plug adapters |
| Plug overload | Plug draws real current; upstream breaker or fuse trips |
| Watts → FE | 1 W = 1 FE/t by default, configurable |

## Electrical standards (chosen per world, fixed after world creation)
| Level | North American (60 Hz) | IEC (50 Hz) | Phases |
|---|---|---|---|
| Generator terminals | 13.8 kV | 11 kV | 3φ |
| Transmission | 138 kV (later 345 kV) | 132 kV (later 400 kV) | 3φ |
| Distribution (MV) | 12.47 kV Y (7.2 kV L-N) | 11 kV | 3φ, single-phase taps (NA) |
| Industrial MV | 4.16 kV | 3.3 kV | 3φ |
| Industrial LV | 480Y/277 V | 400Y/230 V | 3φ |
| Commercial LV | 208Y/120 V | 400Y/230 V | 3φ |
| Residential | 120/240 V split-phase | 230 V single-phase | 1φ |

- Voltage levels, frequency, receptacles and plugs come from the world's standard. Recipes and
  items are data-driven per standard; code reads the active standard and never hardcodes a voltage.
- Small diesel/gas gensets output industrial LV 3φ (480 V or 400 V). Larger units output industrial
  MV or generator-terminal voltage.
- **Migration from current build:** 110 V → 120 V (NA) / 230 V (IEC); add split-phase for NA.
  4.16 kV keeps its role as NA industrial MV. Add the distribution level. 138 kV stays (NA).

Built as a tier (Phase 6a, 2026-10-31): generator terminals are `Voltage.GENERATOR`, 13.8 kV (11 kV IEC), carried by 1,200 A
generator bus duct from a generator's breaker to its 25 MVA step-up (docs/multiblocks/gas-turbine.md). The generator breaker's bus
voltage transformers (wye-grounded, 1 MΩ a phase) give the 13.8 kV bus its ground reference while the generator is off.

## Phases and conductors
- Every conductor has an identity: **A, B, C, N (neutral), G (ground)**. This replaces the current
  hot/neutral roles; hot becomes A, B or C.
- Overhead lines: one conductor per phase, strung between insulators. Ground level / indoor:
  multi-conductor cable (e.g. 3φ + N + G) as one block.
- Line-to-line voltage = √3 × line-to-neutral (480 = √3 × 277, 400 = √3 × 230, 208 = √3 × 120).
- Three-phase power: P = √3 × V_LL × I_L × PF.
- Phase rotation matters: swapping two phases makes a motor run backward.

## Solver
- AC steady-state phasor MNA (complex numbers) at the grid's current frequency.
- Full unbalanced three-phase: each phase is its own set of nodes; neutral current, phase imbalance
  and single-phase taps are solved, not approximated.
- Solve on change (topology, breaker state, load step, frequency step), not every tick.
  Cache the factorization.
- Electrically isolated islands solve independently.
- Performance budget: a 1,000-node network must solve in under 5 ms. Measure in the game test.
- The solver lives in a package with no Minecraft or Fabric imports.

## Frequency and supply/demand (dynamic)
- Each island has a frequency. Nominal is 60 Hz (NA) or 50 Hz (IEC).
- Load above generation makes frequency fall; generation above load makes it rise. Rate depends on
  the rotating mass (inertia) of the generators online.
- Generators have governors with droop: as frequency falls they open up and burn more fuel, up to
  their rating.
- Under-frequency load shedding: relays trip feeders in stages when frequency drops too far.
- Over/under-frequency protection trips generators outside safe limits.
- Generators must synchronize (voltage, frequency, phase angle) before closing onto a live island.
  Closing out of sync causes a fault.
- Exact thresholds and inertia constants: propose with real-world values, then confirm with Tim.

## Components
- **Transformers:** turns ratio + series impedance (%Z), kVA rating, overheating on sustained overload.
  Connections: Δ–Y (substation, generator step-up), Y–Y, single-phase pole-mount
  (NA: 7.2 kV → 120/240 center-tap), three-phase pad-mount.
- **Generators:** voltage source behind impedance, rated kVA, inertia, governor.
- **Loads:** resistive (lamp, heater); motor (inductive, PF ≈ 0.8, inrush current on start).
  Pumps and pumpjacks become motor loads and draw power only while running.
- **Losses:** I²R in conductors; transformer copper and core losses. Conductor ampacity depends on
  size and material; exceeding it heats and eventually damages the conductor.

## Receptacles and plugs (link to other mods)
- A **receptacle** is a grid block wired like any load point (phase conductors + N + G as needed).
- A **plug adapter** is crafted to match one receptacle type. It only fits a receptacle of the same
  type, as in real life. The adapter outputs FE to adjacent machines or to other mods' cables;
  many machines can share one plug.
- The plug draws real current equal to the power the connected machines pull, converted at the
  configured rate (default 1 W = 1 FE/t). It has no internal limit and no protection of its own:
  if the machines pull more than the circuit allows, the upstream breaker or fuse trips.
- A plug's amp rating is the most its receptacle is designed for; sustained draw above it heats
  the receptacle and plug.
- Starting set:

| North American (NEMA) | Rating | IEC | Rating |
|---|---|---|---|
| 5-15 | 120 V, 15 A | CEE 7 (Schuko) | 230 V, 16 A |
| 5-20 | 120 V, 20 A | IEC 60309 blue | 230 V, 32 A |
| 14-50 | 120/240 V, 50 A | IEC 60309 red 3φ | 400 V, 32 A |
| L21-30 | 208Y/120 V 3φ, 30 A | IEC 60309 red 3φ | 400 V, 63 A |
| L22-30 | 480Y/277 V 3φ, 30 A | | |

## Protection
- LV: breakers (thermal + magnetic), fuses — as built.
- MV: fused cutouts and reclosers on poles; breakers with relays at substations.
- Frequency relays for load shedding and generator protection (see above).
- Coordination: the device closest to a fault should trip first. The Power Meter or a relay screen
  shows which device tripped and why.
- Grounding: neutral bonded to ground only at the transformer and at the service entrance.
  Ground faults return through the ground path. GFCI / RCD later.

## Poles and overhead lines
- Overhead MV/HV lines are spans strung between insulators, not wire blocks.
- A span has a maximum length per conductor size; longer runs need another pole.
- Insulators are rated per voltage; using an under-rated insulator causes flashover.
- Poles carry transformers, fused cutouts and reclosers.
- Cable blocks stay for LV and indoor runs.
