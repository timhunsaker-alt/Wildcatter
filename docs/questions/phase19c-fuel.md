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

## 19c2: HF recovery, IFBA and the real hardware

### 9. Steel keeps working in existing worlds: the `fuelNickelHardware` rule
- **Decided on recommendation; for Tim.** The spec retires steel for the fixture's hardware and the loader's plugs once this wave lands
  (specs-wildcatter 81). Since that would stop a built fuel line, it is a world rule, `fuelNickelHardware`: on in new worlds (16 kg of
  304 stainless and 4 kg of nickel-chromium alloy an assembly, the loader's plugs of the alloy; NO STAINLESS, NO NICKEL ALLOY), off in
  existing worlds (steel, exactly as before). Assemblies already made and those in a reactor are unchanged either way.
- **Real counterpart:** 17x17 top and bottom nozzles of 304, end grids and springs of Inconel (fuel.md).

### 10. The HF recovery condenser's place and output
- **Decided on recommendation; for Tim.** Against the IDR kiln, box north (its west side is the kiln's); the kiln's HF waits for it (up to
  five minutes' worth) instead of going to the scrubber; 70 % aqueous HF (a new pipe product, `aqueous HF (70 %)`, counted in kg, kept in
  stock tanks) out of the south face of its bottom cells. At the line's rate about 29 kg/h of HF, 42 kg/h of aqueous acid.
- **Real counterpart:** IDR off-gas HF is condensed as aqueous HF (textbook, unverified).

### 11. The HF column's recovery
- **Decided on recommendation; for Tim.** The conversion plant's HF column takes aqueous HF off a pipe on the north face of its bottom
  cells and distils it over sulfuric acid drawn from its HF kiln's acid pipe (0.5 kg of acid a kg of HF, as the spec), first, into its
  anhydrous HF hold; the kiln's own HF then has that much less room, so it roasts that much less fluorspar. Nothing changes until aqueous
  HF reaches the column.
- **Real counterpart:** aqueous to anhydrous HF by extractive distillation over strong sulfuric acid (textbook).

### 12. The IFBA coater's use and the loader's setting
- **Decided on recommendation; for Tim.** Sneak-click on the rod loader steps its IFBA rods an assembly through 0, 32, 64, 104 (0 at first,
  so nothing changes); the first N rods of each 264 are coated. The coater (against the loader, box north) uses about 4 g of zirconium
  (Zircaloy tubes, at a tube's cladding mass) and 5.7 g of boric acid a rod; without it, or empty, the loader waits (NO IFBA, NO
  ZIRCONIUM, NO BORIC ACID). Coated rods go through the welder, the rod line and the fixture first, and the fixture writes their count on
  the assembly.
- **Real counterpart:** a ZrB2 coat about 10 um thick on the central pellets: about 15 mg of ZrB2 a pellet, 5 g a rod (estimate from the
  pellet's 2.5 cm2 face and ZrB2's 6.1 g/cm3; unverified).

### 13. IFBA in the reactor
- **Decided on recommendation; for Tim.** Each assembly's reactivity is its own (assay, less burnup), less 3,000 pcm x (IFBA rods / 104)
  x (1 - burnup / 12 GWd/t) while positive; the core averages its nine as before. A whole core of 104-rod IFBA fuel is 3,000 pcm lower, the
  spec's 300 ppm less boron at -10 pcm/ppm (`ReactorIfbaTest`). A vessel's saved core keeps each assembly's IFBA count.
- **Real counterpart:** Westinghouse IFBA (the spec's source); worths unverified.

### 14. Not built
- Gadolinia (no rare-earth plant; the spec). The new parts use the mod's materials; no new item icons in 19c2.

