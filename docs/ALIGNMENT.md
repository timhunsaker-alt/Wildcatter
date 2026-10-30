# Alignment review (Phase 0, step 2)

The current build compared with VISION.md, ELECTRICAL_MODEL.md, ROADMAP.md and CLAUDE.md. No code was changed for this review.

## Summary
The build is an effective **DC, single-phase, three-tier** grid with a solid protection layer, a complete fuel chain (survey, drill, pump, refine) and steam, diesel and gas generation. Most of the Phase 1 to 4 reworks are structural: the solver and the `Role` and `Voltage` types sit under almost every block. The multiblocks are the part that conflicts most with the vision (see below).

## What matches (keep)
| Area | State | Verdict in ROADMAP |
|---|---|---|
| MNA solver (`circuit/Solver.java`) | Real-valued, RMS volts, no frequency or phase. Solves on change. | Keep, extend to complex phasors |
| Breakers (110 V, 4.16 kV, 138 kV), fuse, switch | Thermal and magnetic trip, hot side only, per-voltage ratings | Keep |
| Grounding, shocks, bare-conductor reach | `safety/` package; reach 2 blocks at 4.16 kV and 4 at 138 kV | Keep |
| Power Meter | Right-click readout on every machine | Keep |
| Seismograph, fields, drilling rig, pumpjack, gas wellhead | Fields are a pure function of the world seed | Keep, shrink gas fields |
| Refinery tower | Base plus 2 columns (pot still or full tower, plastic) | Keep |
| Cooling tower | Fan plus 2 fill blocks; uncooled turbines need about 33% more steam | Keep, fold into the steam plant |
| Art pipeline | Ludo, then `process_art.py`, then `make_assets.py` | Matches, except placeholder naming (below) |

## What conflicts
### 1. Multiblocks tile identical blocks (VISION non-goal, CLAUDE rule)
| Structure | How it is built now | Conflict |
|---|---|---|
| Boiler (big) | 3x3x3 of identical `boiler` blocks | "No stack N identical blocks" |
| Firebox (big) | 3x3 of identical `firebox` blocks | same |
| MV generator | 3x3x3 of 27 identical `steam_turbine` blocks | same; also not a real steam plant |
| Diesel genset and gas turbine set | 2x2x2 of 8 identical engines at 4.16 kV | same; real small gensets are one machine at 480 V |
| Refinery tower | base plus 2 identical columns | Closest to the intent (two distinct parts), but no controller and no spec |
| Cooling tower | fan plus 2 identical fill blocks | Same pattern as the refinery |

Common problems: capacity comes from tiling (`Tuning.BIG_SIZE`), there is no controller block, there is no `docs/multiblocks/*.md` spec, and the formation logic is copied into four block entities (`BoilerBlockEntity`, `FireboxBlockEntity`, `SteamTurbineBlockEntity`, `EngineBlockEntity`).

### 2. Roles are hot and neutral, not A, B, C, N, G
`circuit/Role.java` has only `HOT` and `NEUTRAL`. Twelve files use the roles, and every wire, port and breaker is built around them. There is no ground conductor and no phase identity.

### 3. Voltages are hardcoded
`energy/Voltage.java` is a fixed enum: 110 V, 4.16 kV, 138 kV. Seventeen files reference `Voltage.LOW`, `MEDIUM` or `HIGH`. Nothing reads a per-world standard, there is no frequency anywhere, and 110 V is neither NA (120/240 V) nor IEC (230 V).

### 4. The solver package is not Minecraft-free
`circuit/Solver.java` has no Minecraft imports. `Circuit.java`, `CircuitManager.java` and `CircuitPart.java` do (`BlockPos`, `Level`, and so on), and so does `energy/Inspectable.java`. Phase 0 step 4 needs a split: a pure-Java solver and circuit model, and a thin Minecraft adapter.

### 5. Copper gas lines carry oil and gas
`PipeBlock.Kind.GAS` is an orange copper line used for gas, crude and diesel. The model calls for steel pipe.

### 6. Generators are at the wrong voltage
Diesel engines and gas turbines are 110 V alone and 4.16 kV in a cube. The model has small gensets at 480 V or 400 V three-phase and larger units at MV or 13.8 kV.

### 7. Poles are decorative
`PoleBlock` and `CrossarmBlock` carry no conductors, and no span limit or insulator rating exists. Overhead lines are wire blocks.

### 8. Pumps and drills draw power at all times
`PumpBlock`, `PumpjackBlock` and `DrillingRigBlock` draw full power even with full tanks. The model makes them motor loads that draw only while running.

### 9. Wording and process
- `PROGRESS.md` and some Field Manual pages describe the mod as a teaching tool. The vision says it is not. Needs a wording pass.
- Two textures are script-drawn without the `*_placeholder` name or a Known-gaps entry: the pole and crossarm woodgrain, and the fuse, switch and breaker bodies.
- Features were added tonight (cooling tower, fuse, switch, pole blocks) before these documents existed. The fuse, cooling tower and breakers are on the Keep list. The switch is under Ideas in the roadmap.

## What each rework touches
| Rework (ROADMAP) | Main code affected |
|---|---|
| Phase 1: phasor solver, A/B/C/N/G | `Solver`, `Circuit`, `Role`; every `CircuitPart` (wires, breakers, fuse, switch, transformers, loads, generators, pumps, rigs) |
| Phase 1: electrical standard | `Voltage` and `Tuning`; 17 files; wire, breaker and recipe data |
| Phase 1: dynamic frequency | New: governor, inertia, island frequency; every generator |
| Phase 1: steel pipe | `PipeBlock`, `Plumbing`, recipes, art; every gas-line machine |
| Phase 2: functional poles, spans | `PoleBlock`, `CrossarmBlock`; new insulator and span types; wire handling |
| Phase 3: motor loads | `PumpBlockEntity`, `PumpjackBlockEntity`, `DrillingRigBlockEntity`, refinery |
| Phase 4: real multiblocks | `BoilerBlock*`, `FireboxBlock*`, `SteamTurbineBlock*`, `EngineBlock*`, `RefineryBlock*`, cooling tower; shared formation code |

## Recipes
The recipe set is generated by `tools/make_assets.py` and only some recipes follow the multiblock parts. After the Phase 4 rework most recipes for tiled parts (turbine, boiler, engine) will be replaced by recipes for distinct parts, so fixing them before the multiblock specs exist would be done twice.

## Open questions (for Tim)
1. The request was to fix recipes and multiblocks. The roadmap puts that in Phase 4, behind the solver, standard and frequency work in Phase 1. ROADMAP says Phase 0 is CURRENT and "no code changes".
2. Do you want Phase 0 step 4 (move the solver into a Minecraft-free package) done next, or the multiblock specs first?
