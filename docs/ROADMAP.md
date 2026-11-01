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

## Phase 2 — Distribution and plugs — DONE
- Distribution voltage (12.47 kV NA / 11 kV IEC); overhead lines as spans between rated insulators;
  functional poles with span limits.
- Pole-mount transformer, fused cutout, service drop.
- Receptacles and plug adapters (starting set in ELECTRICAL_MODEL.md) exporting FE to other mods.
- Under-frequency load shedding relays on feeders.

## Phase 3 — Three-phase industry — DONE (spec: `docs/phase3-three-phase.md`; all values approved 2026-10-30; gap fill done)
- Three-phase LV service (480Y/277 and 208Y/120 NA; 400Y/230 IEC), pad-mount transformers.
- Three-phase receptacles and plugs (L21-30, L22-30, IEC 60309 red).
- Motor loads; pumps and pumpjacks become motors and stop drawing power when idle.
- Motor control center (`docs/multiblocks/motor-control-center.md`).

## Phase 4 — Real multiblocks — DONE 2026-10-30 (4.0 specs, 4.1 conductor sizes, 4a foundations, 4b gensets, 4c synchronization, 4d steam plant and GSU, 4e substation, 4f sounds; a few values added while building wait for Tim)
- Conductor sizes (added 2026-10-30 at Tim's request): NEC building wire 12 AWG to 500 kcmil, an 800 A busway, ACSR overhead conductors;
  real resistance per metre; each size its own item; span limits by conductor.
- Steam plant rebuilt from distinct parts (firebox, drum, superheater, turbine, generator,
  condenser, feed pump, cooling tower).
- Gensets at 480 V; generator synchronization; generator step-up transformer.
- Substation as a multiblock.
- Moving parts and sound (added 2026-10-30 at Tim's request; decided to fold into this phase):
  - A shared renderer for rotating parts (crankshaft, flywheel, fan, turbine rotor, generator rotor, pump gear),
    spinning at a speed tied to the machine's real state (load, trip, coast-down; generators follow grid frequency).
  - The diesel engine and its generator are separate machines. The engine is assembled from crafted parts
    (block, pistons, crankshaft, radiator, injector) and the alternator is its own part.
  - Sound for each machine (engine idle and load, turbine whine, fans, breaker clunk, fuse pop, flashover, transformer hum),
    with pitch and volume following machine state. Specs go in `docs/multiblocks/` first; sound sources come from Ludo.

## Phase 4.5 — True-scale machines — DONE 2026-10-31 (added 2026-10-30 at Tim's request: "as realistic as Minecraft will allow, not just textures on blocks")
- True scale, 1 block = 1 m. Each real component is one item that fills its real size when placed (like a bed), with a ghost
  outline showing where it will go. Breaking any piece breaks the part and drops one item. Specs are redone with real dimensions.
- Detailed models built from many boxes (round parts as many-sided shapes, fins, bolts, pipes, gauges) by the model tool in
  `tools/models/`, drawn in Ludo material textures. No meshes.
- Waves: 4.5a foundations, proved on the GSU; 4.5b gensets; 4.5c steam plant and cooling tower; 4.5d the substation
  yard and its fence; 4.5e the overhead line (one-piece true-height poles, crossarms, insulators, spans at 1 m per block, the
  pole transformer and cutout) with the pad-mounts and the unit substation (moved from 4.5d); 4.5f the building gear and the
  starter plant (breakers, the MCC, receptacles, loads, the boiler and 120 V turbine); 4.5g the well site (the small mast
  rig, the wellheads, the pumpjack with its moving linkage, real-size pipe, the starter turbine's and pump's turning parts);
  4.5h the full land rig, a late-game machine of distinct parts (decided with Tim on 2026-10-31: a small rig early, a land rig
  late), with deep fields only it reaches and unbreakable well casing. The split of the old
  4.5e into 4.5e and 4.5f was decided with Tim on 2026-10-31. The refinery waits for Phase 5.
- Tim checks each wave in a real session before the next starts.

## Phase 5 — Oil and gas depth — DONE 2026-10-31 (all four waves built; Tim checks them in play)
- Wellhead separator (oil / gas / water), gas processing, compressor stations.
- Refinery products: LPG, gasoline, kerosene, diesel, residue. Steam cracker → ethylene → plastic.
- Waves (decided with Tim on 2026-10-31: follow the chain; wells give a real mixed stream; each run of steel pipe carries one
  product): 5a the lease (separator, stock and water tanks, flare, injection pump and disposal well; docs/multiblocks/lease.md);
  5b gas processing (dehydration, NGL and LPG recovery) and a motor-driven compressor (decided with Tim: a 4.16 kV electric drive;
  dry pipeline gas plus LPG; only big burners need processed gas; docs/multiblocks/gas-plant.md); then 5c and 5d together
  (decided with Tim: remove the one-block refinery; an early 1,000 and a late 20,000 bbl/day unit; a WTI-like crude; residue as
  burner fuel; docs/multiblocks/crude-units.md, cracker.md): 5c the crude distillation unit at
  true size with a real crude assay (LPG, gasoline, kerosene, diesel, residue); 5d a steam cracker, ethylene, polyethylene and
  plastic.
- Tim checks each wave in a real session before the next starts.

## Phase 6 — Gas turbine and combined cycle — DONE (2026-11-01)
- Utility gas turbine + heat recovery steam generator (HRSG) feeding the steam turbine.
- Waves (decided with Tim on 2026-10-31: an early and a late turbine; the HRSG feeds a new 5 MW steam turbine; 13.8 kV generators
  through a 25 MVA GSU to 138 kV; an electric starter): 6a the 13.8 kV tier, a 16.5 MW industrial gas turbine (Titan 130 class),
  the HRSG and the 5 MW steam turbine with its air-cooled condenser, the generator breaker, bus duct and GSU, and a load bank
  (docs/multiblocks/gas-turbine.md, combined-cycle.md); 6b a 45 MW aeroderivative (LM6000 class) peaker in simple cycle with its
  own 60 MVA GSU and a 60 MVA 138/12.47 kV substation transformer, full load five minutes after the button (decided with Tim,
  2026-11-01; docs/multiblocks/aeroderivative.md); 6c an art pass with new Ludo materials and signs on the Phase 5 and 6
  parts (Tim, 2026-11-01).

## Phase 7 — Nuclear fuel chain — CURRENT (started 2026-11-01)
- Uranium ore → yellowcake → UF6 → centrifuge cascade enrichment → pellets → rods → assemblies.
- Decided with Tim on 2026-11-01:
  - Uranium from both ore blocks with a mill (early) and ISR wellfields (late).
  - The cascade's output is set by counting its centrifuges. This is a written exception to "never tile identical blocks", because a real cascade is exactly that.
  - A full-height 17×17 assembly.
  - Yellowcake precipitated with magnesia (from magnesite), about 80 % U3O8 (decided in the 7b plan).
  - ISR: alkaline Wyoming chemistry (oxygen), 10x the real head grade at the real flow per well (decided in the 7c plan).
  - Conversion: the rectifier is an AC load with its DC side worked out inside it, no DC tier in the solver; the plant is quarter
    size, 12 fluorine cells, about 0.7 MW at 4.16 kV (decided in the 7d plan).
  - Process chemicals from the existing chain plus real ores. Tim: "make sure we have all ore types from real world with exaggerated numbers".
- Waves (each is planned with its numbers before it is built):
  - 7a: every real ore type as worldgen, plus a gamma scintillometer (docs/ores.md). DONE.
  - 7b: a sulfuric acid plant and the uranium mill (yellowcake in drums; docs/multiblocks/acid-plant.md, uranium-mill.md). DONE.
  - 7c: ISR wellfields (late; docs/multiblocks/isr-wellfield.md). DONE.
  - 7d: conversion to UF6 (a methane reformer for hydrogen, HF from fluorspar, fluorine cells on a DC rectifier, 48Y cylinders; docs/multiblocks/conversion.md). DONE.
  - 7e: the centrifuge cascade (a high-frequency converter, 4.95 % LEU in 30B cylinders).
  - 7f: fuel fabrication (pellets, Zircaloy tube from zircon by the Kroll process, rods, the 17×17 assembly).

## Phase 8 — Pressurized water reactor (PWR)
- Reactor vessel, control rods, primary loop, pressurizer, steam generator, secondary loop to turbine.
- SCRAM, decay heat, spent fuel pool, dosimeter.

## Phase 9 — Bridges
- NeoForge port and Mekanism / other mod energy bridges.

## Ideas (not scheduled)
- Switches and panels. Better wire, ground and breaker art. Saving the Ludo sound sources.
- Soft starters and drives for big motors, so a 250 hp pump can start on a small island.
- A sales gas pipeline and meter station, so a gas plant's residue gas has a buyer besides engines and the flare.
- Plants for the 7a ores that the fuel chain doesn't use:
  - aluminium (the Bayer process, then Hall–Héroult pots, a very large DC load);
  - chlor-alkali from rock salt;
  - a titanium pigment plant from ilmenite and rutile;
  - lithium from spodumene;
  - ferroalloys (chromium, manganese, molybdenum, tungsten);
  - a nickel smelter;
  - fertilizer from phosphate and potash;
  - drilling mud from barite and bentonite.
- Salt domes that trap oil on their flanks, as on the Gulf Coast.
- The purer yellowcake route: an ammonia plant on pipeline gas, ADU precipitation and a calciner at 750-800 C (94-98 % U3O8).
- The acid plant's waste heat boiler steam to a steam turbine (about 2 kg/s of 40 bar steam).
- Lined tailings cells for the mill's thickener underflow.
- Uranium from phosphoric acid (phosphate rock).
- A DC tier in the solver (bus bars that can short and shock), for the fluorine cells and later aluminium pots.
- Conversion: the fluorine cells' cathode hydrogen to the reducer; retiring the fluorinator's ash bed; regenerating the KOH
  scrubbers; a full-size plant.
- ISR: bottled CO2 for the lixiviant, monitor-well rings and excursions, groundwater restoration (sweep, reverse osmosis,
  stability monitoring), a disposal well for the bleed, satellite IX with trucked resin.
