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
| Watts → Team Reborn E | 1 W = 1 E/t, the same setting as FE (Tim, 2026-11-02) |

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
  - **Short-time rule (10d1, IEEE C57.109):** a rated transformer fails when `(I² − 1)·t` reaches 1,250 pu²·s, I being its
    most loaded phase's current over its rated current: 150 % for about 17 minutes, 200 % for 7, a 6 x motor inrush for 36 s,
    25 x for 2 s. Below its rating it cools back over 10 minutes. It replaced the 150 %/10 s rule.
  Connections: Δ–Y (substation, generator step-up), Y–Y, single-phase pole-mount
  (NA: 7.2 kV → 120/240 center-tap), three-phase pad-mount.
- **Generators:** voltage source behind impedance, rated kVA, inertia, governor.
- **Loads:** resistive (lamp, heater); motor (inductive, PF ≈ 0.8, inrush current on start).
  - **Start time:** a start takes 2 s at full voltage and goes with V² below it: a start at 80 % takes about 3 s, drawing its
    locked-rotor current all that time. This holds for direct starts and soft starts alike (decided with Tim on 2026-11-04, with
    the C57.109 rule; until then a direct start kept a flat 2 s).
  - **Soft starter:** SCRs hold the starting current at 2-5 x full-load amps, then a bypass contactor closes
    (`docs/multiblocks/soft-starter.md`).
  - **Drive (VFD):**
    - the line side is a constant-power load at PF 0.95 and 97 % efficiency;
    - the motor side is the drive's own three-phase source, ramping over 10 s, so a drive-fed motor never draws locked-rotor current
      (`docs/multiblocks/vfd.md`).
  - **Drive houses:** the rig drive house, the header house, the frequency converter and the rectifier also take constant power.
  Pumps and pumpjacks become motor loads and draw power only while running.
- **Losses:** I²R in conductors; transformer copper and core losses. Conductor ampacity depends on
  size and material; exceeding it heats and eventually damages the conductor.

## DC (electrolysis; Phase 10b1, `docs/multiblocks/dc-bus.md`)
- **On the same solver:** a DC network is resistors and EMFs at 0°, so the phasor solver solves it exactly. A rectifier's DC side is
  in the same group as its AC side, with no branch between them; its AC side is a constant-power load taking the DC watts of
  the last solve / 0.97 at PF 0.95.
- **Terminals:** DC ports use their own role and voltage key, so nothing AC joins them. A bus bar has no polarity: its voltage
  is whatever the cells along it make it.
- **The rectifier:**
  - it regulates current: each solve it moves its EMF toward its setpoint (at most 15 V/s, up to 150 V);
  - while its EMF is below the load's own voltage it is off, as a diode bridge is;
  - it trips on DC overcurrent: 1.25 x for 1 s, or 2 x at once.
- **Cells:** a back-EMF in series with a resistance; what they make goes with the current through them.
- **Grounding:**
  - ungrounded: a detector ties each pole to earth through 10 kΩ, so the poles sit at ±V/2;
  - a pole that touches earth (a ground wire) alarms the detector, and puts the other pole at full voltage to earth.
- **Shock:** a bare bar at 120 V DC or more to earth shocks (the IEC ripple-free ELV limit), at half the AC 480 V tier's strength
  per volt.
- **Small impedances:** the solver's smallest impedance is 1 nΩ (it was 1 µΩ), so bus bars of a few µΩ/m solve true.

## Receptacles and plugs (link to other mods)
- A **receptacle** is a grid block wired like any load point (phase conductors + N + G as needed).
- A **plug adapter** is crafted to match one receptacle type. It only fits a receptacle of the same
  type, as in real life. The adapter outputs FE to adjacent machines or to other mods' cables;
  many machines can share one plug.
- On Fabric it feeds any **Team Reborn Energy** storage (Tech Reborn and others), found through
  `EnergyStorage.SIDED` on the face toward the plug. It asks each one what it would take this tick and
  hands the energy over evenly tick by tick.
- On NeoForge it feeds any **FE** energy handler (Mekanism and others), found through
  `Capabilities.Energy.BLOCK` on the face toward the plug, the same way (Phase 9c).
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
- The 13.8 kV generator bus (10d1, docs/multiblocks/gen-bus-protection.md): a bus relay panel with 87B bus differential and lockout,
  51V on each generator breaker, and 50/51 on each 15 kV feeder breaker.
- 4.16 kV (10d2, docs/multiblocks/mv-switchgear.md): metal-clad switchgear, a main and feeder sections that bus together, each a
  draw-out vacuum breaker (CONNECTED, TEST, DISCONNECTED) with its own 50/51.
- A standalone 12.47 kV or 138 kV breaker (10d2, docs/multiblocks/breaker-relay-cabinet.md) is a yard breaker worked by a breaker
  relay cabinet: 50/51, and 81U under-frequency load shedding on a 12.47 kV breaker.
- Panels (10d3, docs/multiblocks/panelboards.md): a 120/240 V load centre and a 480Y/277 V panelboard, each a thermal-magnetic
  main, with clip-in thermal-magnetic branch breakers in gutter blocks beside it, one space each. The GFCI breaker trips at 5 mA of
  hot-to-neutral imbalance (an RCD at 30 mA under IEC).
- Fuses (10d3): class J time-delay fuses in the 480 V safety switch (safety-switch.md), E-rated current-limiting power fuses in the
  fused 5 kV load-interrupter (load-interrupter.md). A load-interrupter opens load up to 600 A but not a fault.
- Transfer (10d3, docs/multiblocks/ats.md): a 480 V open-transition ATS starts a standby genset by redstone and moves the load
  over; the two sources are never paralleled through it.
- The relays' 51 pickup is settable, as a real relay's CT ratio and tap set it: 5 to 1,200 A, up to the breaker's frame, with 50 at
  8 x the pickup and the IEEE moderately inverse curve at time dial 1 (the defaults; curve, dial and 50 settable since the gap wave).
- Protective grounds: a grounding clamp set puts three phases to ground on a cable, busway or bus duct. On a dead conductor it keeps
  it safe to work on; on a live one it is a bolted fault.
- The protection gap wave (2026-11-09, docs/questions/protection-gaps.md):
  - Feeder relays: pickup, IEEE C37.112 curve (MI, VI, EI), time dial (0.5-15) and 50 (off, 4-20 x) are settable; the breaker relay
    cabinet adds 79 (2, 15, 30 s; 3 shots on 12.47 kV, 1 on 138 kV). 50BF (2 A, 0.15 s) on the switchgear and the 13.8 kV feeders;
    the 4.16 kV lineup has 87B in its main.
  - 87T on every true-size transformer of 10 MVA and up but the yard's (0.3 pu, 25 % slope, zone to the bounding breakers, 86T);
    87G on the big generators (0.1 pu, 10 % slope). 86B transfer-trips the 138 kV breakers of a GSU straight off a generator bus.
  - The grounding clamp set is ASTM F855 grade 3 (35 kA for 15 cycles, I²t 3.06 x 10^8 A²s) and burns off past it.
  - GFCI/RCD: a shock puts a 1,000 ohm body path (IEC 60479-1) from the touched conductor to earth.
  - Current-limiting fuses (class J, E-rated) clear inside half a cycle above 15 x or 20 x their rating; peak let-through
    sqrt(2) x Ith x (I/Ith)^(1/3). On a fault only the fastest timed devices act (magnetic 1 cycle, GFCI 1.5, current-limiting fuse under
    0.5); relays act as before. Breaker and fuse readouts give the selectivity limit against the device upstream.
  - ATS: factory timers; open, in-phase (15 degrees) or closed (5 degrees, 0.2 Hz, 5 % V, at most 100 ms) transition, limited by one
    frequency per connected group.
- Frequency relays for load shedding and generator protection (see above).
- Coordination: the device closest to a fault should trip first. The Power Meter or a relay screen
  shows which device tripped and why.
- Grounding: neutral bonded to ground only at the transformer and at the service entrance. The service (gap wave): a load centre or
  panelboard with a ground rod, its main bonding jumper N to G and its electrode 25 ohm (NEC 250.24, 250.53).
  Ground faults return through the ground path. GFCI / RCD: the panel's GFCI breaker (10d3).

## Poles and overhead lines
- Overhead MV/HV lines are spans strung between insulators, not wire blocks.
- A span has a maximum length per conductor size; longer runs need another pole.
- Insulators are rated per voltage; using an under-rated insulator causes flashover.
- Poles carry transformers, fused cutouts and reclosers.
- Cable blocks stay for LV and indoor runs.
- Gap wave: a span heats on its conductor's ampacity (10 min time constant) and parts at 3 x its rated rise. Leads run from a line
  insulator to a cutout or a pole transformer's H1. An under-rated insulator's arc shatters it at 50 A·s.
