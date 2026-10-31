# Phase 3: three-phase industry (APPROVED 2026-10-30, with the Phase 3 plan)

Every number and mechanic below that is not in ELECTRICAL_MODEL is a proposal with its real-world counterpart. Approving the plan approved these;
the rows added while building were approved by Tim on 2026-10-30. Rows marked **PROPOSED (gap fill)** came with the gap-fill plan, approved the same day.

## Decisions taken with the plan
- A new **Industrial tier**: 480Y/277 V (NA), 400Y/230 V (IEC). 208Y/120 V rides on the existing low tier (same 120 V line to neutral).
- **Pumpjack and drilling rig move to 480 V three-phase.** Existing 120 V hookups show "needs 480 V three-phase" until rewired.
- Indoor and ground-level three-phase runs use **one cable block carrying A, B, C, N and G** (ELECTRICAL_MODEL). Overhead lines stay one conductor per span.
- Three-pole breakers are **ganged**: one handle opens all three phases together.

## Steps
3a phasor solver in the blocks (no new content), 3b industrial tier, cable and three-pole breaker, 3c pad-mount and unit-substation transformers,
3d motors, 3e three-phase plugs, 3f motor starter and motor control center (`docs/multiblocks/motor-control-center.md`).

## Values
| Item | Value | Real-world basis |
|---|---|---|
| Industrial LV | 480Y/277 V NA, 400Y/230 V IEC | ELECTRICAL_MODEL table |
| Commercial LV | 208Y/120 V NA on the low tier | ELECTRICAL_MODEL table |
| Tier nominal | line to neutral for every tier (4.16 kV → 2.40 kV, 138 kV → 79.7 kV; IEC 3.3 kV → 1.91 kV, 132 kV → 76.2 kV); the meter shows L-N and L-L | how three-phase voltages are quoted |
| Generator phases | A 0°, B −120°, C +120° (ABC rotation) | standard positive sequence |
| Existing transformers | three-phase grounded Y–Y; single-phase builds use phase A | common NA distribution connection; Δ–Y comes with the Phase 4 multiblocks |
| Transformer %Z | pole 25 kVA 2.5 %; pad-mount 75 kVA 3 %; 300 kVA 5 %; unit substation 1,000 kVA 5.75 % | typical nameplate impedances |
| Pad-mount sizes | `padmount_208` 75 kVA, `padmount_480` 300 kVA | common pad-mount sizes (75 to 2,500 kVA) |
| Industrial breakers | 20, 30, 60, 100, 225, 400 A | standard 480 V frame and trip sizes |
| Three-pole low-voltage breaker | 15, 20, 30, 50 A | standard 208 V three-pole sizes |
| Cable | the same resistance and ampacity per block as the tier's wire | one conductor size per tier |
| Motor running PF | 0.80 | ELECTRICAL_MODEL |
| Motor start | 6 × full-load current at PF 0.35 for 2 s | NEMA design B locked-rotor current |
| Overload relay | class 10: trips at 6 × FLA within 10 s; never below 115 % | NEMA overload classes |
| Pumpjack motor | 7.5 kW (10 hp), 480 V 3φ, about 11 A | small pumpjacks run 5 to 50 hp |
| Drilling rig motor | 30 kW (40 hp), 480 V 3φ, about 45 A | small workover and drilling rig drives |
| Pump, fan, refinery | today's watts, 120 V (230 V IEC) single-phase motors | small single-phase motors |
| Plugs | NEMA L21-30 (208Y/120 3φ 30 A), L22-30 (480Y/277 3φ 30 A), IEC 60309 red 32 A and 63 A (400 V 3φ) | ELECTRICAL_MODEL table |
| Industrial wire | 0.01 ohm per block, 100 A | added while building, approved 2026-10-30: about 3 AWG copper; lower resistance than the 20 A low-voltage wire because it carries five times the current |
| Three-phase heater | 480 V, 15 kW, delta elements | added while building, approved 2026-10-30: an industrial duct or unit heater, used to load the tier |
| Transformer X/R | 5 (resistance = %Z / 5 for rated units) | added while building, approved 2026-10-30: typical of distribution transformers; replaces the old fixed 0.2 ohm windings for rated units |
| Overload relay dial | 1.5, 3, 5, 8, 11, 16, 22, 32, 45, 63 A; default 11 A | added while building, approved 2026-10-30: adjustable relays are set to the motor's nameplate full-load amps |
| Motor circuit protector | trips at once at 13 x the overload setting | added while building, approved 2026-10-30: NEC 430.52 allows an instantaneous-trip breaker up to 1,300 % of full-load current |
| Pump level switch | starts below 80 % full, stops when full | added while building, approved 2026-10-30: an ordinary on/off level control |
| Industrial shock | 4 damage (2 hearts), bare reach 0.5 blocks, touch chance 0.3 | added while building, approved 2026-10-30: between 120 V and 4.16 kV; 480 V arc flash is the real hazard |
| Generator kVA | rated kW / 0.8 | **PROPOSED (gap fill)**: gensets and turbine generators are nameplated at 0.8 PF |
| Rated phase current | kVA / (3 x V line to neutral), for the 4.16 kV cubes (and later the 480 V gensets); a lone 120 V machine stands in for a single-phase set and has no per-phase limit (Tim, 2026-10-30) | **PROPOSED (gap fill)**: each stator phase carries a third of the machine |
| Stator thermal limit | trips when the integral of (r^2 - 1) dt reaches 37.5 s, r = phase amps / rated phase amps; cools below rating | **PROPOSED (gap fill)**: IEEE C50.13 armature short-time capability (130 % for 60 s, 226 % for 10 s); it cools from the limit in 120 s below rating, as the overload relay does |
| Fused cutout | bare and live: touching it shocks like the distribution line | **PROPOSED (gap fill)**: real cutouts are operated only with a hot stick; enclosed breakers, starters and the MCC are dead-front |

## What stays for later
- Δ–Y transformers and the 30° shift: Phase 4 substation and generator step-up.
- 480 V gensets: Phase 4 (`docs/multiblocks/genset.md`).
- Conductor sizes, cable ampacity tables, motor slip and torque curves.
