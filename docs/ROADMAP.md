# Wildcatter — Roadmap

Exactly one phase is marked **CURRENT**. Each phase is done when:
`./gradlew runClientGameTest` passes with new coverage for the phase, PROGRESS.md is updated,
and the Field Manual has a page for each new system.

## Triage of existing content
| Built | Verdict |
|---|---|
| MNA circuit solver | Keep. Extend to AC phasor, three-phase. |
| 110 V tier, hot/neutral roles | Rework → 120/240 V split-phase; roles → A/B/C/N/G. |
| 4.16 kV and 138 kV tiers | Keep. Add 12.47 kV distribution. |
| Breakers, fuses, grounding, shocks, Power Meter | Keep. |
| Seismograph, fields, drilling rig, pumpjack, gas wellhead | Keep. Shrink gas fields. |
| Refinery tower | Keep. Expand products in Phase 5. |
| Cooling tower | Keep. Becomes part of the steam plant multiblock. |
| 27-turbine cube, 8-engine cube | Rework → multiblocks of distinct real parts (Phase 4). |
| Diesel engine / gas turbine at 110 V | Rework → 480 V 3φ gensets with real ratings. |
| Copper "gas lines" carrying crude and diesel | Rework → steel pipe for oil and gas. |
| Utility poles (decorative) | Rework → functional (Phase 2). |

## Phase 0 — Realign — DONE
1. Add VISION.md, ELECTRICAL_MODEL.md, ROADMAP.md, CLAUDE.md.
2. Claude Code reviews the existing code against these docs and writes `docs/ALIGNMENT.md`:
   what matches, what conflicts, what each rework above touches. No code changes.
3. Design decisions are settled (see the table in ELECTRICAL_MODEL.md).
4. Move the solver into a package with no Minecraft or Fabric imports.

## Phase 1 — AC solver, standards, frequency — DONE (phasor solver built and tested but not yet wired into the blocks; that happens with the first three-phase equipment in Phase 3)
- Complex phasor MNA, full unbalanced three-phase, A/B/C/N/G conductors.
- Per-world electrical standard (North American 60 Hz / IEC 50 Hz); all voltages read from the standard.
- 110 V → 120/240 V split-phase (NA) or 230 V (IEC). Existing content keeps working.
- Dynamic frequency per island: inertia, governor droop, generator over/under-frequency trips.
- Steel pipe replaces copper gas lines for oil and gas.

## Phase 2 — Distribution and plugs — CURRENT
- Distribution voltage (12.47 kV NA / 11 kV IEC); overhead lines as spans between rated insulators;
  functional poles with span limits.
- Pole-mount transformer, fused cutout, service drop.
- Receptacles and plug adapters (starting set in ELECTRICAL_MODEL.md) exporting FE to other mods.
- Under-frequency load shedding relays on feeders.

## Phase 3 — Three-phase industry
- Three-phase LV service (480Y/277 and 208Y/120 NA; 400Y/230 IEC), pad-mount transformers.
- Three-phase receptacles and plugs (L21-30, L22-30, IEC 60309 red).
- Motor loads; pumps and pumpjacks become motors and stop drawing power when idle.
- Motor control center.

## Phase 4 — Real multiblocks
- Steam plant rebuilt from distinct parts (firebox, drum, superheater, turbine, generator,
  condenser, feed pump, cooling tower).
- Gensets at 480 V; generator synchronization; generator step-up transformer.
- Substation as a multiblock.

## Phase 5 — Oil and gas depth
- Wellhead separator (oil / gas / water), gas processing, compressor stations.
- Refinery products: LPG, gasoline, kerosene, diesel, residue. Steam cracker → ethylene → plastic.

## Phase 6 — Gas turbine and combined cycle
- Utility gas turbine + heat recovery steam generator (HRSG) feeding the steam turbine.

## Phase 7 — Nuclear fuel chain
- Uranium ore → yellowcake → UF6 → centrifuge cascade enrichment → pellets → rods → assemblies.

## Phase 8 — Pressurized water reactor (PWR)
- Reactor vessel, control rods, primary loop, pressurizer, steam generator, secondary loop to turbine.
- SCRAM, decay heat, spent fuel pool, dosimeter.

## Phase 9 — Bridges
- NeoForge port and Mekanism / other mod energy bridges.

## Ideas (not scheduled)
- Switches and panels. Better wire, ground and breaker art. Saving the Ludo sound sources.
