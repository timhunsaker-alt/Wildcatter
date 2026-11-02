# Wildcatter

A Fabric mod for Minecraft Java 26.3 (Java 25): a realistic power and industry mod. You find oil and gas, raise steam, build
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
- **The reactor (Phase 8, in progress):**
  - a boron plant (boric acid, boron carbide control rods);
  - the nuclear island of a 9-assembly, two-loop PWR that goes critical and makes steam (8a).
  - Next: the turbine island (8b), then SCRAM and safety (8c).
- **The Field Manual** item explains every machine. Art and sounds are made with Ludo (`art/`).

## Install
Copy `dist/wildcatter-0.1.0.jar` into your instance's `mods/` folder with Fabric Loader and Fabric API for 26.3.
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
| `/function wildcatter:kit/ores` | one of every ore, the minerals and the scintillometer |
| `/function wildcatter:kit/mill` | the sulfuric acid plant and the uranium mill |
| `/function wildcatter:kit/isr` | the ISR wellfield |
| `/function wildcatter:kit/conversion` | conversion to UF6 |
| `/function wildcatter:kit/enrichment` | the centrifuge cascade |
| `/function wildcatter:kit/zirconium` | the zirconium plant |
| `/function wildcatter:kit/fuel` | the fuel plant and the helium recovery unit |
| `/function wildcatter:kit/boron` | the boron plant (boric acid, B4C, RCCAs) |
| `/function wildcatter:kit/reactor` | the nuclear island, 9 fuel assemblies, 5 RCCAs and boric acid |

**Useful vanilla commands:**
- `/gamemode creative`;
- `/time set day`;
- `/effect give @s minecraft:slow_falling 600` (the plants are tall);
- `/tp @s <x> <y> <z>`.

**Reading a machine:** right-click any part with an empty hand. Sneak-right-click changes a setting where a part has one: the
console's power setpoint, the CVCS boron target, a coolant pump's switch, a genset's sync mode, the pilger mill's dies.

## Testing: automated
JDK 25 required.

```bash
./gradlew build                                   # compile, unit tests, jar in build/libs/
./gradlew test                                    # unit tests only (plain-Java physics and chemistry)
./gradlew runClientGameTest                       # every in-game scene, about an hour
WILDCATTER_ONLY=reactor ./gradlew runClientGameTest           # one scene
WILDCATTER_ONLY=boron,reactor ./gradlew runClientGameTest     # several scenes
```
On a Mac, run `caffeinate -u -t 30` just before launching the test client: it needs the display awake. Screenshots land in
`build/run/clientGameTest/screenshots/`.

**The game-test scenes**, by area:
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
- **The reactor:** `boron`, `reactor`.

## Art and assets
- **Redo the art:** `python3 tools/process_art.py && python3 tools/make_assets.py` (needs Pillow). Ludo prompts are in
  `art/source/PROMPTS.md`.
- **Models:** the part models come from `tools/models/*.py` at real size.
- **Tuning:** the numbers are in `Tuning.java`. The solver has a standalone check in `tools/solvercheck/`.
