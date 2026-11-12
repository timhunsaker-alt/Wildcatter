# Phase 19c, fuel fabrication extensions: decisions taken while Tim was away (2026-11-12)

Spec: `docs/multiblocks/fuel-fabrication-extensions.md` (DRAFT, decided on recommendation; its "As built" section lists what differs). The
spec's own choices are `docs/questions/specs-wildcatter.md`, items 79-84. Each item below was decided on the best real-world
recommendation; for Tim. Format as in `docs/questions/README.md`.

## 19c1: inspection, scrap and lubricant

### 1. A world rule, `fuelLineDefects`, for what would change an existing fuel line
- **Decided on recommendation; for Tim.** Cracked pellets without zinc stearate, the grinder's swarf going out in scrap cans, and the
  leak chance on assemblies made without inspection all change what a line already built does, so they are behind a new world rule,
  `fuelLineDefects`: on in new worlds, off in existing ones (as `fuelHandlingDamage`, `docs/questions/world-rules.md`). The new parts
  (inspection bench, rod inspection, scrap oxidizer) work in every world, since they only act once placed. With the rule off a press
  still burns zinc stearate if it has some, and nothing cracks.
- **Real counterpart:** none; the brief's rule for existing worlds.

### 2. Scrap goes out in 5 kg cans
- **Decided on recommendation; for Tim.** Dry scrap gathers in each part as kg of U at its assay and goes out in cans of 5 kg of U
  (`uo2_scrap_can`, carrying its assay) to a hopper under the press, the grinder and the inspection parts. A full slot never stops the
  line (the scrap waits, counted). The oxidizer takes cans by hopper on top, one assay at a time.
- **Real counterpart:** scrap is handled in small cans or trays in criticality-safe batches (a few kg of U); 5 kg is a proposed size.

### 3. The oxidizer's rate and box
- **Decided on recommendation; for Tim.** 5 kg of U an hour (a tenth of the line's 50 kg/h, so it keeps up with the 10 % cap), 30 kW at
  480 V, its motor box on the north face of its main cell (its west side stands against the granulator; a 2-wide part's east face of its
  main cell is inside it).
- **Real counterpart:** a batch tray furnace at 400-450 C in air (textbook); rate proposed.

### 4. The leak chance: 0.5 % for each inspection part missing
- **Decided on recommendation; for Tim.** With the rule on, the fixture writes on the assembly a leak chance of 0.5 % for each of the
  two inspection parts its line lacks (the rod inspection line before it; the pellet bench before the rod loader that feeds its welder):
  1 % with neither, as the spec says. The chance is rolled once, when the assembly is loaded into a core (the vessel or the refuelling
  machine): a leaking rod from the start, which 19b's coolant activity counts. The item's tooltip names the chance.
- **Real counterpart:** fuel leaks run about 1 in 10^5 rods with full inspection (industry data, unverified); the spec's 1 % stands for
  a line without it.

### 5. One rod in two hundred, exactly
- **Decided on recommendation; for Tim.** The rod line rejects 0.5 % by a running count (every 200th rod), not by chance, so it is the
  same every time; the pellet bench rejects 1 % of the U it passes.

### 6. Zinc stearate's recipe
- **Decided on recommendation; for Tim.** A zinc ingot and any item tagged `c:animal_fats` make a 5 kg sack (shapeless); Wildcatter puts
  rotten flesh in `c:animal_fats` as the stand-in, and another mod's tallow can join the tag.
- **Real counterpart:** zinc stearate is made from zinc oxide and stearic acid (from tallow); the spec's choice.

### 7. The scenes
- `fuel_quality` is a NeoForge scene of its own (its rule on, in a batch of its own) rather than an extension of the `fuel` client scene,
  which films the 7g line as it was. The pellets' rates are real; the rod line's 200 rods are run by a hook. Client scene
  `fuelInspection` (screenshots of the bench, the rod line and the oxidizer).

### 8. Placeholders
- `zinc_stearate_placeholder` and `uo2_scrap_can_placeholder` (item icons) are drawn by script. Listed in PROGRESS.md, Known gaps.
