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

## Phase 7 — Nuclear fuel chain — DONE (2026-11-01)
- Uranium ore → yellowcake → UF6 → centrifuge cascade enrichment → pellets → rods → assemblies.
- Decided with Tim on 2026-11-01:
  - Uranium from both ore blocks with a mill (early) and ISR wellfields (late).
  - The cascade's output is set by counting its centrifuges. This is a written exception to "never tile identical blocks", because a real cascade is exactly that.
  - A full-height 17×17 assembly.
  - Yellowcake precipitated with magnesia (from magnesite), about 80 % U3O8 (decided in the 7b plan).
  - ISR: alkaline Wyoming chemistry (oxygen), 10x the real head grade at the real flow per well (decided in the 7c plan).
  - Conversion: the rectifier is an AC load with its DC side worked out inside it, no DC tier in the solver; the plant is quarter
    size, 12 fluorine cells, about 0.7 MW at 4.16 kV (decided in the 7d plan).
  - Enrichment: Urenco TC-21 class centrifuges, each doing 1000x its real separative work at the real 50 kWh a SWU, about 571 kW
    a machine; at least 11 machines (one a stage) on 12.47 kV frequency converters (decided in the 7e plan).
  - Fuel fabrication is two waves, 7f the zirconium plant and 7g the fuel plant. The Kroll magnesium is recycled through a
    magnesium cell, make-up chlorine comes from a chlor-alkali cell on rock salt, make-up magnesium from chlorinated magnesia
    (decided in the 7f plan).
  - Fuel: helium from a recovery unit at the gas plant; steel stands in for the stainless and Inconel hardware until a nickel
    smelter exists, the mid grids Zircaloy from the pilger mill; the assembly a placeable full-height block (decided in the 7g plan).
  - Process chemicals from the existing chain plus real ores. Tim: "make sure we have all ore types from real world with exaggerated numbers".
- Waves (each is planned with its numbers before it is built):
  - 7a: every real ore type as worldgen, plus a gamma scintillometer (docs/ores.md). DONE.
  - 7b: a sulfuric acid plant and the uranium mill (yellowcake in drums; docs/multiblocks/acid-plant.md, uranium-mill.md). DONE.
  - 7c: ISR wellfields (late; docs/multiblocks/isr-wellfield.md). DONE.
  - 7d: conversion to UF6 (a methane reformer for hydrogen, HF from fluorspar, fluorine cells on a DC rectifier, 48Y cylinders; docs/multiblocks/conversion.md). DONE.
  - 7e: the centrifuge cascade (a high-frequency converter, 4.95 % LEU in 30B cylinders; docs/multiblocks/enrichment.md). DONE.
  - 7f: the zirconium plant: zircon to Zircaloy-4 cladding tube and guide thimbles by chlorination, hafnium separation, the Kroll
    process, VAR melting, extrusion and pilgering (docs/multiblocks/zirconium.md). DONE.
  - 7g: the fuel plant (UO2 by the dry route, pellets, sintering, rods with helium, the 17×17 assembly; docs/multiblocks/fuel.md). DONE.

## Phase 8 — Pressurized water reactor (PWR) — DONE (2026-11-02; waves 8a-8e; full suite 62 scenes passed)
- Reactor vessel, control rods, primary loop, pressurizer, steam generator, secondary loop to turbine.
- SCRAM, decay heat, spent fuel pool, dosimeter.
- Decided with Tim on 2026-11-01:
  - A core of 9 full-height 17×17 assemblies (3×3) at real power density, about 155 MW thermal. The small core goes critical as
    a full core would: a written exception, like the cascade's 1000x.
  - Burnup 100x: real power, the uranium used 100 times faster, so an assembly lasts about 320 h at full power and the core is
    refuelled a third at a time.
  - Boric acid in the coolant (a CVCS) plus control rods, the boric acid from borax ore.
  - Boron carbide rods, as in Combustion Engineering PWRs, from the same borax plant (hafnium would take about 1,400 h of the 7f
    line for one set).
  - Two loops, as a 2-loop Westinghouse plant: two steam generators and two reactor coolant pumps.
- Waves:
  - 8a: the nuclear island (vessel, CRDMs, console, two steam generators and pumps, pressurizer, CVCS, aux feed) and the boron
    plant (boric acid, B4C, RCCAs); docs/multiblocks/reactor.md, boron.md.
  - 8a DONE.
  - 8b: the turbine island (HP turbine, moisture separator reheater, double-flow LP turbine, condenser cooled once-through from a
    lake or river intake, condensate and feed trains, a 60 MVA 13.8 kV generator, the 60 MVA GSU, a unit auxiliary transformer);
    docs/multiblocks/turbine-island.md.
  - 8c: the hyperbolic natural-draft cooling tower (Tim asked for the iconic tower; Calder Hall's 50 MWe units each had an 88 m
    one), closing the circulating water loop. It needs a true-size shell rendered as one model.
  - 8c DONE.
  - 8d: after the trip: decay heat, xenon, the cooldown and residual heat removal, core damage and a hydrogen blast
    (docs/multiblocks/reactor.md).
  - 8d DONE.
  - 8e: the refuelling machine, irradiated fuel, the spent fuel pool and its cooling, radiation and the dosimeter
    (docs/multiblocks/fuel-handling.md).
- Decided with Tim on 2026-11-02 (the 8e plan): the refuelling machine runs the outage (REFUEL, DEFUEL); irradiated fuel is never an
  item; the vessel counts as inside its primary shield.
- Decided with Tim on 2026-11-01 (the 8d plan): two waves (8d, 8e); losing cooling ends in core damage and a hydrogen blast;
  radiation in 8e is a real dose with real effects (nausea and weakness at a few Sv, death about 8 Sv).
- Decided with Tim on 2026-11-01 (the 8c plan): the tower rises lift by lift as it is fed; a tenth of the real concrete (the steel
  about a hundredth); the wet bulb from the biome.
- Decided with Tim on 2026-11-01 (the 8b plan): the tower gets its own wave after the turbine island; a new 60 MVA two-pole
  hydrogen-cooled generator part.

## Phase 9 — Bridges — DONE (2026-11-02; 9a-9c; released as v0.2.0)
- NeoForge port and Mekanism / other mod energy bridges.
- **9a: Team Reborn Energy on Fabric.** Plugs feed Tech Reborn and other Team Reborn Energy mods (bundled, MIT). DONE 2026-11-02.
- **9b: the NeoForge port.** DONE 2026-11-02. One codebase, two jars:
  - a plain multi-loader project (common, fabric and neoforge subprojects, Loom and ModDevGradle);
  - a small platform interface of our own, and no extra mod for players.
- **9c: FE on NeoForge.** DONE 2026-11-02. Plugs feed NeoForge's energy capability. Mekanism takes FE, so it needs no code of its own. It is checked
  against a stand-in until Mekanism for 26.3 is released (on its dev branch, 10.8.0, as of 2026-11-02).
- **Testing:** the Fabric client game test stays the full suite. NeoForge gets a few GameTest smoke scenes and a runClient
  screenshot.
- Decided with Tim on 2026-11-02:
  - three waves, Team Reborn first;
  - plain multi-loader;
  - smoke scenes on NeoForge;
  - 1 W = 1 E/t, sharing the FE setting;
  - testers' builds: a tag `v*` publishes a GitHub Release with both jars and the kit datapack; `dist/` stays;
  - the first release, v0.2.0, at the end of Phase 9.

## Phase 10 — Machines and grid at scale — DONE 2026-11-05 (picked by Tim 2026-11-02, from Ideas)
- **10a: soft starters and drives for big motors** — DONE 2026-11-03 (specs: `docs/multiblocks/soft-starter.md`, `vfd.md`).
  - **Gear:** real cabinets wired between the breaker and the motor, at 480 V (standalone cabinets and MCC buckets) and 4.16 kV
    (a true-size soft-starter cabinet and a drive lineup).
  - **Soft starter:** SCRs hold the current at a dial setting of 2-5 x full-load amps (3.5 x default), then a bypass contactor
    closes at 1.25 x.
  - **VFD:** a 6-pulse front end at PF 0.95, 97 % efficient, a 10 s ramp, 1.5 x for 60 s; it takes constant power from the line.
  - **Drive houses:** the existing drive houses also take constant power.
  - **Docs cleanup:** stale known gaps and duplicate ideas.
- **10b: the DC tier and aluminium** — DONE 2026-11-03. Bus bars that can short and shock, in the solver; then a Bayer refinery and Hall–Héroult pots,
  a very large DC load (from the 7a bauxite). The DC tier also feeds the conversion fluorine cells. Three waves:
  - **10b1: the DC tier** — DONE 2026-11-03 (spec: `docs/multiblocks/dc-bus.md`):
    - DC bus bars on the solver (copper, 4 kA and 16 kA);
    - the rectifier becomes a current-regulated DC source with a setpoint dial and a ground-fault detector;
    - the fluorine, magnesium and chlor-alkali cells become DC loads on it. This supersedes 7d's "no DC tier in the solver".
  - **10b2: the Bayer refinery and AlF3** — DONE 2026-11-03 (specs: `docs/multiblocks/bayer-refinery.md`, `alumina-plant.md`). Bauxite
    to alumina, with caustic soda from the chlor-alkali cell (made a real pipe product) and lime from a lime kiln. AlF3 from the
    conversion HF. Decided with Tim on 2026-11-03:
    - 105 t a day of alumina, for a 40-pot line;
    - the full flowsheet, each step its own part;
    - process steam from a gas-fired package boiler;
    - surplus chlorine into 1-ton containers;
    - a new limestone rock;
    - red mud as filter cake for a dry stack;
    - the products in 1 t bulk bags.
  - **10b3: anodes and the potline** — DONE 2026-11-03. Two waves:
    - **10b3a: carbon and anodes** — DONE 2026-11-03 (specs: `docs/multiblocks/delayed-coker.md`, `coke-ovens.md`, `anode-plant.md`):
      - a delayed coker on the crude unit's residue, and a rotary calciner, for petroleum coke;
      - coal-tar pitch from by-product coke ovens and a tar still;
      - an anode plant that mixes, forms, bakes and rods the blocks, with butt recycle.
    - **Side wave: ore tooltips and JEI pages**, DONE 2026-11-03, at Tim's request before 10b3b:
      - every ore and mineral tooltip says where it is found, what mines it, and what uses it (`ore/OreUses.java`);
      - with JEI installed, U shows a page for each ore (`compat/WildcatterJeiPlugin`);
      - the QoL mod list is in `docs/recommended-mods.md`.
    - **10b3b: the potline** — DONE 2026-11-03 (spec: `docs/multiblocks/potline.md`):
      - the potline's rectiformers and aluminium bus;
      - 180 kA prebake Hall–Héroult pots at true size, the player choosing how many in series;
      - a fume treatment centre, a tapping crucible and a casting machine.
    - Decided with Tim on 2026-11-03:
      - up to 40 pots, about 180 V and 33 MVA;
      - 4 x 60 kA rectiformers on 138 kV, three carrying the line and one spare;
      - pots side by side join in series through their own risers, so bus is only placed at the row ends, crossovers and the
        rectifier run;
      - the pots have anode effects, freeze after about 4 h without current, and have a fume treatment centre;
      - a coker sized for the line, about 2 L/s of residue;
      - vanilla coal as coking coal in by-product coke ovens, the coke working wherever coal does and the gas going to a gas line;
      - a rodding shop with butt recycle;
      - a vacuum tapping crucible and a casting machine making 22.7 kg ingots.
- **10c: construction cranes** — DONE 2026-11-04. Tim's idea of 2026-11-01. Cranes build a plant from a blueprint. Three waves:
  - **10c1: blueprints** — DONE 2026-11-04 (spec: `docs/blueprints.md`):
    - a survey tool records any built plant onto a blueprint;
    - a library of the mod's plants, made from the game test scenes;
    - the ghost and the site marker;
    - the lift list, the pallets and the part masses.
  - **10c2: the tower and crawler cranes** — DONE 2026-11-04 (specs: `docs/multiblocks/tower-crane.md`, `crawler-crane.md`):
    - the tower crane (Liebherr 380 EC-B 16) on 480 V, and the crawler crane (Liebherr LR 1300) on its own diesel;
    - their load charts and real speeds;
    - building a site from the crane's chest, and dismantling it back.
    - From the real data sheets: the 380 EC-B 16 carries 3.0 t at its 75 m tip (3.4 t only in Litronic's power-plus mode).
      The LR 1300 lifts 286 t at 4.3 m on its 20 m boom, and 2.6 t at 80 m on an 86 m boom, not 25 t as first written.
  - **10c3: the ring crane and heavy lifts** — DONE 2026-11-04 (spec: `docs/multiblocks/ring-crane.md`):
    - the ring crane (Sarens SGC-250 "Big Carl", which set Hinkley Point C's reactor modules; the earlier note named Mammoet in
      error) on its own diesel engines;
    - cranes sharing one site, and tandem lifts.
    - From Sarens' SGC brochure: 5,000 t at 40 m on the 118 m main boom, 1,554 t at 120 m; 3,114 t at 45 m to 838 t at 170 m on the
      160.5 m boom; a 48.5 m ring and 5,200 t of counterweight; 6 degrees/min slewing; 20 m/min line speed on 6 main winches.
      Its power is 12 Cat diesels of 310 kW (3.72 MW) driving hydraulics, not the "about 2 MW of diesel sets" first written.
    - Decided with Tim on 2026-11-04: both main booms, stepped by sneak-click; a laydown chest within its chart range; a crane
      works ahead past a lift it cannot make, but never onto a lift resting on one not yet set; a tandem lift comes from the lead's
      chest, in equal halves, each at most 80 % of its crane's chart.
  - Decided with Tim on 2026-11-04:
    - a tower, a crawler and a ring crane, powered as real ones (the tower crane on 480 V, the others on their own diesel);
    - blueprints from both a library (power plants, oil and gas, nuclear, smelter) and a survey tool;
    - any block can be in a blueprint. Ordinary blocks go up on pallets of up to 64, and big parts one to a lift;
    - real lift speeds;
    - cranes can take a plant apart into their chest;
    - two cranes can share a site and tandem-lift a part;
    - a ghost of the blueprint at its site, locked with a site marker.
    - for 10c2: the crawler's boom is set by the player (20 to 95 m, the real chart's lengths), and DISMANTLE is the site marker's
      mode, which every crane on the site follows.
- **10d: the grid finish** — DONE (2026-11-04). Four waves:
  - **10d1: protection** (spec: `docs/multiblocks/gen-bus-protection.md`):
    - an IEEE C57.109 short-time rule for rated transformers ((I² − 1)·t = 1250 pu²·s) in place of the pole transformer's 150 % for
      10 s, so direct starts go with V² too (from 10a, 2026-11-02);
    - relay protection for the 13.8 kV bus: a bus relay panel (87B and 86B, 51V on each generator breaker, 50/51 on each 15 kV feeder
      breaker);
    - a grounding clamp set that faults a live bus or keeps a dead one safe.
  - **10d2: true-size gear:**
    - a 4.16 kV metal-clad switchgear lineup;
    - standalone DIST and HV breakers, each with its own relay cabinet (81U on DIST);
    - a rated 4.16 kV to 120/240 V pole unit, and a 10 MVA 4.16/138 kV transformer;
    - the one-block stand-ins (`mv_breaker`, `dist_breaker`, `hv_breaker`, `ufls_relay_dist`, `transformer`,
      `substation_transformer`) removed.
    - Specs: `docs/multiblocks/mv-switchgear.md`, `breaker-relay-cabinet.md`, `power-transformer-10.md`, and the 4.16 kV pole unit
      in `poles.md`.
    - Decided with Tim on 2026-11-04 (the 10d2 plan): switchgear sections that bus together, a MAIN and a FEEDER part; draw-out
      breakers that rack (CONNECTED, TEST, DISCONNECTED); a settable 51 pickup on every new relay, set with the Power Meter; the
      standalone breakers are the yard breakers worked by a relay cabinet; 81U reset by hand; the 4.16 kV pole unit 25 kVA.
  - **10d3: panels and switches:**
    - a 200 A 120/240 V load centre and a 480Y/277 V panelboard with clip-in branch breakers;
    - a 480 V fused safety switch and a 5 kV load-interrupter switch;
    - an automatic transfer switch.
    - Specs: `docs/multiblocks/panelboards.md`, `safety-switch.md`, `load-interrupter.md`, `ats.md`.
    - Decided with Tim on 2026-11-04 (the 10d3 plan): true-size panels with gutter blocks, one branch space each, the circuit out
      of the gutter's front; one branch-breaker item per rating; a GFCI breaker; an open-transition 480 V 3-pole ATS that starts a
      genset by redstone (the genset gets a redstone remote start); cartridge fuse items in the safety switch; a fused and a plain
      5 kV load-interrupter.
  - **10d4: art and sounds:**
    - better wire, ground and breaker art;
    - the Ludo sound sources kept.
    - Decided with Tim on 2026-11-04 (the 10d4 plan): grid gear only (wires, ground, the one-block breaker, switch, relay and fuse,
      `cable_mv`, and the 10d1/10d3 item placeholders); wires from a few Ludo materials tinted per conductor; the six sounds made
      again and replaced; a contact sheet reviewed before the commit.
  - Decided with Tim on 2026-11-04:
    - all four kinds of switches and panels;
    - the stand-ins removed outright;
    - 87B, 51V and feeder 50/51 on the 13.8 kV bus;
    - V² for every motor start;
    - four waves;
    - a relay cabinet for each standalone DIST and HV breaker;
    - a rated 4.16 kV pole unit in place of `transformer`;
    - a 10 MVA, 7.5 %Z 4.16/138 kV unit (delta at 4.16 kV, grounded wye at 138 kV) in place of `substation_transformer`;
    - a grounding clamp set for bus faults.
- Decided with Tim on 2026-11-03 (the 10b plan):
  - three waves (10b1, 10b2, 10b3);
  - 180 kA prebake pots;
  - anodes from a delayed coker and pitch;
  - the Bayer refinery's caustic from chlor-alkali and its lime from a lime kiln;
  - the DC system ungrounded with a ground-fault detector;
  - a cell touching its rectifier still works.
- Decided with Tim on 2026-11-02:
  - these 4, in this order;
  - for 10a, real separate cabinets at 480 V and 4.16 kV, with the values above.

## Phase 11 — Assayed slurry — DONE 2026-11-05 (Tim: "build it now", for Tailings R4; released as v0.3.0; full client suite passed)
- **Six concentrator slurries** on steel pipe, liquids counted in kg that no tank holds: mill discharge, fines slurry, conditioned
  slurry, concentrate slurry, thickened concentrate, tails slurry.
- **The assay:** the kg of each mineral per kg of slurry, keyed by the mineral's item id, so Tailings' own minerals ride too.
  Shares are clamped to 0..1 and never add past 1; the rest is water and gangue.
- **Where it lives:** pipes hold nothing in Wildcatter, so the assay is held and saved by the node that puts the slurry on the pipe
  (`fluid/SlurryHold`, `block/Assayed`). A pull (`block/Slurry.take`) blends the assays of the sources it drew from by mass.
- **The Power Meter** on a slurry pipe names its three richest minerals.
- API only: nothing in Wildcatter makes or uses the slurries; Tailings R4 is the first user. No in-world scene until then (the unit
  tests drive a pull with stand-in sources).
- Decided with Tim on 2026-11-05: assay keys are item ids; a v0.3.0 release for Tailings to compile against; the API only.

## Phase 12 — The lined tailings cell and the process water pump — DONE (2026-11-06; Tim: "do whatever is needed for wildcatter")
Spec: `docs/multiblocks/tailings-cell.md`. For Tailings' Phase 4 and the uranium mill's underflow. Approved by Tim as built. The full client suite passed on 2026-11-06 (2h 47m), and it is released as v0.4.0.

## Phase 13 — PLCs — done (Tim, 2026-11-07: "every type of thing you would run off of them", programming "as close to real life as possible")
Spec: `docs/multiblocks/plc.md`, nine waves, all approved in order: 13a language core; 13b hardware and text editor; 13c ladder and FBD; 13d SFC, device blocks, memory card, import and export (done); 13e transmitters and valves (done); 13f power equipment and HMI (done); 13g process plants and the reactor (done); 13h networks (done); 13i release and Tailings (done). The full client suite passed on 2026-11-07 (2h 53m), and it is released as v0.5.0.

## Phase 14 — The ammonia plant — DONE 2026-11-09 (for Tailings' open-pit blasting, its wave P3; built while Tim was away: "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back")
Spec: `docs/multiblocks/ammonia-plant.md` (approved 2026-11-09; "As built" lists the numbers added). Decisions for Tim: `docs/questions/ammonia-plant.md`.
- One steam reformer's 30 Nm3/h of hydrogen and a PSA nitrogen generator's 10 Nm3/h, through a syngas compressor (150 bar) and the
  ammonia converter against it: 15.2 kg/h of liquid ammonia into a pressure bullet.
- A nitric acid unit on a platinum-rhodium gauze: 45.4 kg/h of 60 % acid; the gauze loses platinum and is changed when worn.
- An AN neutraliser (34.6 kg/h of ammonium nitrate as 85 % solution) and an emulsion blender (46.1 kg/h of emulsion matrix, a pipe
  product in kg for Tailings' emulsion truck). Boosters and detonators stay in Tailings (vanilla TNT, Tim's decision).
- NeoForge scenes `ammonia_loop`, `nitric_acid`, `an_emulsion`; unit test `AmmoniaPlantTest`. Client scene `ammoniaPlant` (filmed, passes).

## Plant and crane gaps — 2026-11-09 (Known gaps wave; built while Tim was away)
Decisions for Tim: `docs/questions/plant-crane-gaps.md`. Branch `plant-crane-gaps`.
- Cranes: wind limits from the weather (tower 20, crawler 9, ring 14 m/s at the head), ground bearing under crawlers and ring (IBC
  presumptive values), slews checked against blocks and tower crane masts, the ring crane travelling on crane rail.
- Plants: the quench tower's 2,000 L sump; the stabilizer's fired reboiler; coke oven and coker gas in gas engines and turbines; the
  pots' auxiliaries on 480 V and live cut-out with shorting bars; a burner fed by a hopper at any cell.
- Planned, for Tim: residence holds, crane erection and the crawler's jibs, spent pot lining treatment, the BOP stack (with 15b2),
  drawing-only gaps.
- NeoForge scenes `crane_siting`, `ring_travel`, `quench_sump`, `stabilizer_fuel`, `cog_engine`, `pot_aux`, `burner_hopper`; client
  scene `ringTravel` (new) and `cranes`, `heavyLift`, `potline` changed, for the lead to run.

## Ideas (not scheduled)
- Check plugs against real Mekanism machines once Mekanism for 26.3 (10.8.0) is released.
- A sales gas pipeline and meter station, so a gas plant's residue gas has a buyer besides engines and the flare.
- Plants for the 7a ores that the fuel chain doesn't use:
  - chlor-alkali from rock salt;
  - a titanium pigment plant from ilmenite and rutile (the separator's);
  - lithium from spodumene;
  - ferroalloys (chromium, manganese, molybdenum, tungsten);
  - a nickel smelter;
  - fertilizer from phosphate and potash;
  - drilling mud from barite and bentonite.
- Salt domes that trap oil on their flanks, as on the Gulf Coast.
- Biogas as an engine fuel (proposed by Farmings, 2026-11-07): a gas genset that can burn biogas from a manure digester, mostly methane
  with carbon dioxide, at a lower heating value than pipeline gas. Farmings' digester would give about 48 kWh of electricity per tonne
  of dairy manure (Ontario OMAFA energy-yield table), so the flow is small. Needs: a biogas fuel type, a derate on the genset's
  rating, and a way for another mod to supply it. No change until Tim approves. See Farmings `docs/waste.md`.
- The purer yellowcake route: ADU precipitation on the Phase 14 ammonia and a calciner at 750-800 C (94-98 % U3O8).
- The acid plant's waste heat boiler steam to a steam turbine (about 2 kg/s of 40 bar steam).
- Uranium from phosphoric acid (phosphate rock).
- Ammonia plant (Phase 14 gaps): the ammonia leak hazard; AN solution setting in a cold pipe; an IBC for matrix; a PGM refinery so
  gauze packs are made from refined platinum and worn packs recovered; nitrous oxide abatement on the acid unit's tail gas; prill and ANFO.
- Conversion: the fluorine cells' cathode hydrogen to the reducer; retiring the fluorinator's ash bed; regenerating the KOH
  scrubbers; a full-size plant.
- ISR: bottled CO2 for the lixiviant, monitor-well rings and excursions, groundwater restoration (sweep, reverse osmosis,
  stability monitoring), a disposal well for the bleed, satellite IX with trucked resin.
- Enrichment: centrifuge spin-up and coast-down hours, machine crashes, cooling water to the casings, the product sampling autoclave
  and blending, cylinder heels, a choice of product assay.
- Zirconium: SiCl4 to fumed silica; uses for the chlor-alkali caustic soda; hafnium metal for control rods; Pidgeon magnesium from
  dolomite; the aluminothermic chromium step; scrap recycle and yields; Kroll and VAR batch timing.
- Fuel: the kiln's HF back to the conversion plant; zinc stearate lubricant and pore formers; a nickel smelter for real Inconel and
  stainless hardware; pellet inspection, rod X-ray and helium leak tests; IFBA and gadolinia burnable absorbers; rejects and scrap.
- Reactor: dry cask storage for the pool's oldest fuel; contamination and decontamination; fresh-fuel inspection; a containment building with hydrogen recombiners and igniters; low-temperature overpressure protection; the real 55 C/h heat-up limit; full OT-delta-T and OP-delta-T trips; the pressurizer relief tank; silver-indium-cadmium and hafnium rods; helium in the B4C rodlets; a buyer for the borax plant's sodium sulfate.
