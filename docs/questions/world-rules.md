# Tim's answers 2, 15, 20-23: decisions taken while building them (2026-11-10)

Tim's answers are in Tailings' `docs/questions/answers-2026-11-10.md`. Branch `w-rules`, cut from `w-next`. Format as in `README.md`
here. These are the choices made while building the answers; none of them reopens an answer.

### 1. How a world saved before this version is told from a new one
- **Chosen:** the world's rules are saved with the overworld (`data/wildcatter/rules.dat`, `RulesData`). When a world loads with no rules
  file, the overworld's level data decides: vanilla marks a world *initialized* once it has placed its spawn, before its first save, so
  a world still being created is not initialized and every world saved before (by any version) is. Not initialized: a NEW world, each
  rule's new-world default; initialized: an EXISTING world, every rule off. The verdict is saved as `origin` and logged ("World rules: no
  rules saved yet; the overworld is not yet initialized, so this is a NEW world"). A rule missing from a saved file (added by a later
  version) is off.
- **Why:** it is the game's own flag for exactly this, set by every version, and it does not depend on the world's age, on what other
  mods saved or on a player having joined. The rules load as the overworld loads, before its spawn chunks generate, so the dome grid
  rule is known to worldgen from the first chunk.
- **Alternatives:** the game time at load (0 for a new world; nearly as good, but a world can be opened and saved without ticking);
  the presence of another Wildcatter file such as the electrical standard (absent for a vanilla world the mod is added to).

### 2. The game test worlds start as existing worlds
- **Chosen:** the NeoForge server tests and the Fabric client tests set `RulesData.testWorldsAsExisting`, so their (new) worlds start
  with every rule off. The scenes that test a rule switch it on: by `/wildcatter rule` (the uranium and reactor scenes), or for a whole
  batch by a test environment (`RuleScenes.RulesOn`: the five oilfield scenes with `saltDomeGrid192`, `mudGel` and `wellControl`;
  `residence_holds` with `realResidenceTimes`). The world is still found NEW and logged as such (`world_rules` checks it).
- **Why:** every scene before this was written against the rules off; a new world's defaults (the real heat-up, bent grids, LOW GEL)
  would change dozens of scenes that do not test them.
- **Alternatives:** let the test world take a new world's defaults and rewrite the scenes that break.

### 3. A third oilfield rule: `wellControl`
- **Chosen:** besides `saltDomeGrid192` and `mudGel`, Phase 15b2's kicks, NO BOP, blowouts and the washout in rock salt are behind a rule
  `wellControl`, on in new worlds and off in existing ones.
- **Why:** the brief's rule that nothing may change an existing world except through a rule that is off there: a land rig in an existing
  world drilling toward a deep field would otherwise stop at NO BOP, and one drilling through an evaporite bed at WASHOUT.
- **Alternatives:** leave them always on (an existing rig's behaviour changes on update); fold them into `mudGel`.

### 4. Exactly the old dome placement with `saltDomeGrid192` off
- **Chosen:** the placed feature `wildcatter:salt_dome` keeps its id, its biomes (plains, sunflower plains, swamps and beaches again,
  as before Phase 15, on both loaders) and its place in the feature order. Its four modifiers became one, `wildcatter:salt_dome_placement`,
  which with the rule off runs the same four (rarity 1 in 150, in square, y 0, the biome check) on the same random numbers, and with it
  on hands the chunk to the grid dome. The feature keeps the old sizes and runs master's code with the rule off. `world_rules` checks
  3,000 chunks' placement against the four modifiers, random number for random number.
- **Why:** a feature's random numbers come from its index among the features of its step; adding a second placed feature, or adding the
  dome to more biomes, would have moved every later feature in existing worlds' new chunks.
- **Alternatives:** two placed features (the grid one added to every biome): simpler, but it shifts the features after it.
- **Note:** with the rule on, a grid dome is drawn in chunks whose neighbourhood holds a dome biome (the decoration's biome set reaches a
  chunk round); a dome (at most 16 blocks across) lies within a chunk of its centre, so it is drawn whole.

### 5. Residence holds
- **Chosen:** `fluid/Holdup`, a first-in first-out of time buckets (a minute for the digester, half an hour for the precipitators and the
  ovens), as `plant-crane-gaps.md` 11 planned. With `realResidenceTimes` on, the coke oven battery charges until its ovens hold 18 h of
  coal (240 t/day's worth) and pushes each charge after 18 h; its gas comes off as it cokes. The digester holds an hour of slurry, its
  steam taken at the feed; the precipitators 36 h of liquor. Turned off again, what is in process comes out as the next part takes it.
  With the rule off and nothing held, the old code runs unchanged. The coker drums' 12-24 h fill is not part of the answer and stays a
  tonne switch.
- **Why:** the plan; the steady rate stays the same, only the first product waits.
- **Alternatives:** scaled holds (minutes for hours).

### 6. Cask cooling counted 100x
- **Chosen:** each assembly's record keeps its seconds since discharge; the station's limits check its decay heat at 100 times that
  time. A record saved before has no count and starts from 0 (it becomes eligible as it cools from now). The heat the module and the
  readouts carry stays the real one.
- **Why:** Tim's answer 20, for eligibility only.
- **Alternatives:** guess an age for old records from their heat (the decay groups do not give one uniquely).

### 7. A placed assembly's assay
- **Chosen:** the main cell's block entity already keeps the item's components when it is placed (vanilla does); the loot table copies
  the assay and the custom data (handled, bent grid, inspected) back onto the drop, and a click reads the standing assembly's assay.
- **Why:** no new block entity type (an existing world's placed assemblies keep theirs), and the bent-grid flag comes with it.
- **Alternatives:** a fuel assembly block entity of its own.

### 8. 18b's second cycle
- **Chosen:** ammonia (L) and liquid CO2 (kg) on the east face of the circuit's east end's front three cells; the calciner against the
  east end's back cells takes AUC as it takes ADU (`takeAuc`), the drum filler against the calciner. The calciner prefers a touching
  precipitation tank if it has both. The AUC's off-gas (its NH3 and CO2) goes to the calciner's scrubber count. U is 0.848 of U3O8.
- **Why:** the spec's plan, with the nozzles placed where the calciner does not cover them.
- **Alternatives:** the calciner pulling AUC by pipe.

### 9. Not done
- The salt dome film shows the dome placed by command in a flat test world on a built-up stone column; worldgen in a real seed is
  checked by `world_rules` and `salt_dome`, not filmed.
