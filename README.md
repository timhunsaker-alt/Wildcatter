# Wildcatter

A Minecraft Java 26.3 mod (Java 25) for **Fabric and NeoForge**: a realistic power and industry mod. You find oil and gas, raise steam, build
gensets, turbines and a real grid, and work up through the nuclear fuel chain to a pressurized water reactor. Every machine is a
true-size multiblock of real parts, with real voltages, ratings and chemistry. The electrical model (three-phase phasor solve,
frequency, protection) is in `docs/ELECTRICAL_MODEL.md`. What is built, phase by phase, is in `PROGRESS.md`. What comes next is in
`docs/ROADMAP.md`. Each machine's spec is in `docs/multiblocks/`.

## What's in it
- **The grid:** wires solved by a full unbalanced three-phase solver; breakers, fuses and relays.
  - Transformers from 120 V to 138 kV, North American or IEC standards chosen per world.
  - True-size poles, spans, pad-mounts, unit substations and a 138/12.47 kV substation.
  - Service drops and receptacles with plugs.
  - Shocks, arc reach and grounding.
- **Gensets and synchronizing:** diesel and gas gensets built part by part, paralleled with a synchroscope. Motors, MCCs and
  starters.
- **Oil and gas:**
  - seismic surveys, a land rig and wells;
  - the lease (separator, tanks, pumpjacks);
  - a gas plant (with helium recovery);
  - crude units, a steam cracker and polyethylene.
- **Thermal power:**
  - a 1 MW steam plant with cooling tower and GSU;
  - a 16.5 MW gas turbine with HRSG and 5 MW steam turbine (combined cycle);
  - a 45 MW aeroderivative peaker.
- **The nuclear fuel chain:**
  - every real ore type and a scintillometer;
  - a sulfuric acid plant and a uranium mill, ISR wellfields;
  - conversion to UF6 and a centrifuge cascade to 4.95 % LEU;
  - a zirconium plant, a fuel plant making full-height 17×17 assemblies.
- **The reactor (Phase 8):**
  - a boron plant (boric acid, boron carbide control rods);
  - the nuclear island of a 9-assembly, two-loop PWR that goes critical and makes steam (8a);
  - the turbine island: a 51 MW turbine generator onto 13.8 kV and the 60 MVA GSU, cooled once-through from a lake (8b);
  - an 88 m hyperbolic natural-draft cooling tower, built lift by lift, closing the circulating water loop (8c);
  - after the trip: decay heat, xenon, a COOLDOWN to cold shutdown on the RHR, and core damage with a hydrogen blast if
    cooling is lost (8d).
  - refuelling at outages, a spent fuel pool, and real radiation dose with a dosimeter (8e).
- **Other mods:** plug adapters feed Team Reborn Energy machines (Tech Reborn and others) on Fabric at 1 W = 1 E/t (Phase 9a).
  On NeoForge they feed FE machines (Mekanism and others) at 1 W = 1 FE/t (9b, 9c).
- **The Field Manual** item explains every machine. Art and sounds are made with Ludo (`art/`).

## Install
Testers get the jars from the GitHub **Releases** page (or `dist/`):
- **Fabric:** `wildcatter-fabric-0.2.0.jar` in `mods/` with Fabric Loader 0.20.1+ and Fabric API for 26.3. Tech Reborn (6.2.1,
  with RebornCore 6.2.0) is optional: plugs feed its machines.
- **NeoForge:** `wildcatter-neoforge-0.2.0.jar` in `mods/` with NeoForge 26.3.0.101 or newer.

Use one jar, not both.
`dist/README.md` has the player notes.

## Testing in game: the kits
`dist/wildcatter_kit/` is a datapack of kits: each gives you the parts and supplies for one plant. Copy the folder into a world's
`datapacks/` folder, run `/reload`, then use the commands below in creative mode. The Field Manual and each spec in
`docs/multiblocks/` say how to lay the parts out.

| Command | What it gives |
|---|---|
| `/function wildcatter:kit/land_rig` | the land rig, to drill oil, gas and ISR wells |
| `/function wildcatter:kit/lease` | the lease: separator, stock and water tanks, flare, injection pump and disposal well, pumpjack |
| `/function wildcatter:kit/gas_plant` | the gas plant: compression, dehydration, NGL chiller, stabilizer, flare |
| `/function wildcatter:kit/refinery` | the crude units |
| `/function wildcatter:kit/cracker` | the steam cracker and polyethylene line |
| `/function wildcatter:kit/gas_turbine` | the gas turbine, HRSG and steam turbine (combined cycle) |
| `/function wildcatter:kit/peaker` | the 45 MW aeroderivative peaker |
| `/function wildcatter:kit/plugs` | every receptacle and plug adapter, wire, a breaker and the 120 V starter plant |
| `/function wildcatter:kit/ores` | one of every ore, the minerals and the scintillometer |
| `/function wildcatter:kit/mill` | the sulfuric acid plant and the uranium mill |
| `/function wildcatter:kit/isr` | the ISR wellfield |
| `/function wildcatter:kit/conversion` | conversion to UF6 |
| `/function wildcatter:kit/enrichment` | the centrifuge cascade |
| `/function wildcatter:kit/zirconium` | the zirconium plant |
| `/function wildcatter:kit/fuel` | the fuel plant and the helium recovery unit |
| `/function wildcatter:kit/boron` | the boron plant (boric acid, B4C, RCCAs) |
| `/function wildcatter:kit/reactor` | the nuclear island (with the RHR pump and exchanger), 9 fuel assemblies, 5 RCCAs and boric acid |
| `/function wildcatter:kit/fuel_handling` | the refuelling machine, transfer system, spent fuel pool and a dosimeter |
| `/function wildcatter:kit/cooling_tower` | the natural-draft cooling tower's controller and its materials (concrete, steel, plastic) |
| `/function wildcatter:kit/turbine_island` | the turbine island: turbines, 60 MVA generator, condenser, feed train, CW pumps, UAT |

**Useful vanilla commands:**
- `/gamemode creative`;
- `/time set day`;
- `/effect give @s minecraft:slow_falling 600` (the plants are tall);
- `/tp @s <x> <y> <z>`.

**Reading a machine:** right-click any part with an empty hand. Sneak-right-click changes a setting where a part has one: the
console's power setpoint, the CVCS boron target, a coolant pump's switch, the HP turbine's trip reset, the refuelling machine's REFUEL/DEFUEL, a genset's sync mode, the pilger mill's dies.

## The code: one codebase, two loaders
- `common/`: nearly everything (the solver, circuits, plants, blocks, items, renderers, the Field Manual), compiled against
  plain Minecraft, so it cannot reach a loader's API. Its assets and data are shared. Its unit tests live here.
- `fabric/`: the Fabric entry points, Fabric's biome API for the ores, Team Reborn Energy, and the client game test.
- `neoforge/`: the NeoForge entry points, a mixin that tells the circuit manager when block entities load and unload, the ores'
  biome modifiers, and the NeoForge smoke scenes.
- `dev.wildcatter.platform.Platform` is the small seam between them (the config folder, which mods are loaded).

## Testing: automated
JDK 25 required. NeoForge's build tools also need a JDK 21, which Gradle finds or downloads.

```bash
./gradlew build                                   # all three projects, unit tests, jars in fabric/ and neoforge/build/libs/
./gradlew :common:test                            # unit tests only (plain-Java physics and chemistry)
./gradlew :fabric:runClientGameTest               # every in-game scene on Fabric, about an hour and a half
WILDCATTER_ONLY=reactor ./gradlew :fabric:runClientGameTest           # one scene
WILDCATTER_ONLY=boron,reactor ./gradlew :fabric:runClientGameTest     # several scenes
./gradlew :neoforge:runGameTestServer             # the NeoForge smoke scenes (registers, lamp, plug, ores)
./gradlew :neoforge:runClient                     # play on NeoForge
```
On a Mac, run `caffeinate -u -t 30` just before launching the test client: it needs the display awake. Screenshots land in
`fabric/build/run/clientGameTest/screenshots/`.

**A release for testers:** `git tag v0.2.0 && git push --tags`. GitHub Actions builds both jars and publishes them with the kit
datapack (`wildcatter_kit.zip`) as a Release. Every push also builds and keeps the jars as a run artifact.

**The game-test scenes**, by area:
- **Other mods:** `teamRebornEnergy` (a plug fills a real Team Reborn Energy storage).
- **NeoForge smoke scenes:** `registers`, `lamp`, `plug`, `ores`.
- **Basics:**
  - `lowVoltageLoop`, `transformerChain`, `steamLoop`, `wells`;
  - `refining` (the 4.16 kV tier).
- **The grid:**
  - `frequency`, `distribution`, `spans`, `poleTransformer`, `serviceDrop`, `plugs`;
  - `feederRelay`, `threePhase`, `solverBudget`, `overheadThreePhase`, `conductorSizes`.
- **Industry and motors:** `industrial`, `motors`, `motorControlCenter`, `motorStarter`, `statorLimit`.
- **Gensets:** `gensets`, `synchronizing`, `gensetParts`, `gensetFrameRate`.
- **Thermal power and parts:**
  - `steamPlant`, `gasTurbine`, `peaker`;
  - `bigParts`, `towerParts`, `substationParts`, `lineParts`, `buildingGear`.
- **Oil and gas:** `landRig`, `lease`, `gasPlant`, `toppingUnit`, `crudeUnit`, `cracker`.
- **The fuel chain:** `ores`, `acidPlant`, `uraniumMill`, `isrWellfield`, `conversion`, `cascade`, `zirconium`, `fuel`.
- **The reactor:** `boron`, `reactor`, `turbineIsland`, `coolingTower`, `reactorSafety`, `fuelHandling`.

## Grid performance

Each level solves its grid when something changes and every 4 ticks otherwise. Solving takes at most about 10 ms per tick
(`Tuning.SOLVE_BUDGET_NANOS`); groups past that wait for the next tick. Radial feeders solve in about 1 ms at 2,000–4,000 nodes.
A dense 4,000-node mesh takes about 25 ms. Measurements and limits are in [docs/grid-performance.md](docs/grid-performance.md).

## Art and assets
- **Redo the art:** `python3 tools/process_art.py && python3 tools/make_assets.py` (needs Pillow). Ludo prompts are in
  `art/source/PROMPTS.md`.
- **Models:** the part models come from `tools/models/*.py` at real size.
- **Tuning:** the numbers are in `Tuning.java`. The solver has a standalone check in `tools/solvercheck/`.
