# Phase 2: distribution and plugs (APPROVED 2026-10-30, with the Phase 2 plan)

Every number and mechanic below that is not in ELECTRICAL_MODEL is a proposal with its real-world counterpart. Approving the plan approved these.

## Steps
2a distribution tier, 2b insulators and spans, 2c pole-mount transformer, 2d fused cutout and service drop, 2e receptacles and plugs, 2f feeder relays.

## Values
| Item | Value | Real-world basis |
|---|---|---|
| Distribution tier | 12.47 kV line to line, 7.2 kV line to neutral (11 kV IEC); single-phase taps carry the line-to-neutral voltage | ELECTRICAL_MODEL table |
| Distribution wire | 0.8 ohm per block, 400 A | between the 4.16 kV (0.5 ohm, 200 A) and 138 kV (2.0 ohm, 600 A) wire; one conductor size per tier |
| Distribution breaker ratings | 200, 400, 600, 1200 A | feeder breakers are 600 to 1200 A |
| Distribution shock | 12 damage (6 hearts), bare reach 3 blocks, touch chance 0.8 | between 4.16 kV and 138 kV |
| Game scale for spans | 1 block of span = 4 m | a 12 m pole is 3 blocks |
| Max span, low voltage drop | 8 blocks (32 m) | service drops are 15 to 30 m |
| Max span, 12.47 kV | 10 blocks (40 m) | typical distribution span 30 to 50 m |
| Max span, 138 kV | 64 blocks (256 m) | transmission spans 200 to 400 m |
| Span limits | per voltage tier, not per conductor size | one conductor size per tier today |
| Insulator ratings | low voltage 1 kV, distribution 15 kV (pin), 138 kV (suspension) | standard insulator classes |
| Span resistance | the tier's wire resistance per block times the span length | existing wire model |
| Pole-mount transformer | 25 kVA, 7.2 kV to 120/240 V (NA) or 230 V (IEC), 2.5 % impedance | common single-phase pole-top unit |
| Transformer overload | above 100 % it heats; fails at 150 % for 10 s | simplified cooling limit |
| Fused cutout (K link) | 6, 10, 15, 25, 40, 65, 100 A | standard K-link ratings |
| Receptacle burn | sustained 120 % of the plug rating for 30 s | simplified thermal limit |
| 138 kV to 12.47 kV transformer | 20 MVA stand-in block | the Phase 4 substation multiblock replaces it |
| Plug starting set (single and split phase) | NEMA 5-15 (120 V, 15 A), 5-20 (120 V, 20 A), 14-50 (120/240 V, 50 A), CEE 7 (230 V, 16 A), IEC 60309 blue (230 V, 32 A) | ELECTRICAL_MODEL plug table; three-phase plugs wait for Phase 3 |
| Watts to FE | 1 W = 1 FE/t, configurable | ELECTRICAL_MODEL decision |

## Notes
- Under-rated insulators flash over: a span whose tier is above its insulator's rating shorts to ground and trips protection; the insulator then shatters.
- Team Reborn Energy: the newest Maven release (5.0.0, Jan 2026) is not confirmed for Minecraft 26.3. Plugs go through a plain-Java `HostEnergy` interface; the Team Reborn adapter is added only if it builds against 26.3.
- The single-phase pair at 4.16 kV and 138 kV still carries the full tier voltage (older convention); Phase 3 reconciles it with line-to-line and line-to-neutral for three-phase.
