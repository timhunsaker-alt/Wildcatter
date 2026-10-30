# Wildcatter — Vision

## What it is
A realistic modern-industry power mod for Minecraft Java 26.3 (Fabric). The player starts by
finding oil and gas and raising steam, works up through gas turbines and combined cycle, and
ends at nuclear power — and builds a real grid to carry it:

generator → step-up transformer → HV transmission → substation → MV distribution →
pole / pad-mount transformers → LV service (homes, plus three-phase industry), following North American
or IEC standards depending on the world

Wildcatter is the **power base** that other mods draw from (Mekanism, Direwolf20-pack mods and
similar) once they exist for this Minecraft version.

## Pillars (in priority order)
1. **Industry progression.** Fuel → generation → grid. Every feature must serve this chain.
2. **Realism.** Real equipment, real voltages, real ratings, real physics wherever it is playable.
   Realism is a constraint on *how* things work, not the product by itself.
3. **The grid is gameplay.** Designing transmission and distribution is a core activity, not wiring chores.
4. **Safety as consequence.** Shocks, faults and protection exist because the real thing has them.
   They support play; they are not the point of the mod.

Wildcatter is **not a teaching tool** and makes no claim to teach or certify electrical work. It uses
the same physical principles as real power systems. The Field Manual explains how the in-game
systems work; it does not drive design.

## Non-goals
- No abstract energy unit inside the grid. Volts, amps, watts, VA only. FE or other units appear
  only inside plug adapters.
- No generic "Tier 1–5 machine" ladder, no magic, no Mekanism clones (no ore multiplication,
  no all-in-one machines).
- No "stack N identical blocks" structures. Big equipment is a multiblock of distinct, real
  components (see Multiblock rules).
- Wildcatter does not try to be a consumer/tools mod. Its own loads exist to use and demonstrate
  the grid: lights, heaters, motors, pumps, plant auxiliaries.

## Progression (eras)
1. **Wildcatting.** Seismic survey, drilling, crude, coal steam, 120/240 V local power.
2. **Refining & gas.** Refinery, gas processing, diesel and gas gensets, first MV distribution.
3. **The grid.** Substations, HV transmission, three-phase industrial service, protection coordination.
4. **Big thermal.** Large steam plant, utility gas turbine, combined cycle (HRSG).
5. **Nuclear.** Uranium ore → yellowcake → UF6 → enrichment → fuel assemblies → PWR → spent fuel handling.

## Multiblock rules
- Each big machine is built from distinct parts that match the real thing. Example — steam plant:
  firebox/burner, boiler drum, superheater, turbine casing + rotor, generator, condenser, feed pump.
- One controller block validates the structure. Every multiblock has a spec in
  `docs/multiblocks/<name>.md` (layered ASCII grid, legend, controller, rotation, formation, breaking)
  before any code is written.
- Capacity grows by building a larger or better structure, not by tiling copies of one block.
- A formed machine may render as a single model.

## Interop
- Fabric now. Keep the core (circuit solver, fluid/fuel math) in plain Java with no Minecraft or
  Fabric imports so a NeoForge port is cheap later.
- Power leaves Wildcatter only through **receptacles and crafted plug adapters** (NEMA or IEC types,
  matching the world's standard). A plug converts watts to the host energy API — Team Reborn Energy on
  Fabric if available for 26.3, FE on NeoForge later — at 1 W = 1 FE/t by default (configurable).
  Many machines can share one plug; the plug draws real current and the upstream breaker trips on overload.
- Electrical standard (North American 60 Hz or IEC 50 Hz) is chosen per world.

## Art
- Block and item art comes from Ludo, post-processed to 16×16 with shared per-material palettes by
  `tools/process_art.py`.
- Any texture drawn by a Python script is a placeholder, named `*_placeholder`, and listed in
  PROGRESS.md under Known gaps.
