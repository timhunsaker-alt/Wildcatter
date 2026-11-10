# Phase 15, the oilfield: decisions taken while Tim was away (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back ...
lets get all ideas done or planned out". Specs: `docs/salt-dome-oil.md` (15a) and `docs/multiblocks/drilling-mud.md` (15b1, 15b2). The
spec-level choices are items 14-22 of `docs/questions/specs-wildcatter.md`; these are the ones taken while building. Format as in
`docs/questions/README.md`.

### 1. Which biome decides whether a grid dome is there?
- **Chosen:** the biome at the dome's centre column (at y 64): plains, sunflower plains, swamp, mangrove swamp or any beach. Game code asks
  `ReservoirData.field(level, x, z)`, which checks it (it works for unloaded chunks: the biome source answers from the seed).
  `Reservoir.at(seed, x, z)` without a level counts every grid dome, whatever its biome (tests and tools that only have a seed).
- **Why:** the spec said "a dome in 60 % of the cells whose centre biome is" one of those; the biome is the one thing the seed alone does
  not give without the world's biome source, so the caller supplies it, as `Drilling.rollFront` does for roll-fronts.
- **Alternatives:** no biome check (domes and their oil in oceans and mountains); a biome check at every column (a dome cut in half by
  a river).

### 2. How the feature draws a grid dome
- **Chosen:** the `salt_dome` placed feature now runs once in every overworld chunk (no rarity, no biome filter) and draws only the part
  of its cell's dome inside that chunk. The NeoForge biome modifier and Fabric's placement put it in `#minecraft:is_overworld`; the dome
  checks its own centre's biome. Placed by hand (`/place feature`), it draws the grid dome's part in the chunk it is placed in.
- **Why:** a dome spans up to four chunks; drawing only inside the chunk being generated never writes outside the generation region,
  and every chunk of the dome is drawn by its own pass.
- **Alternatives:** draw the whole dome from its centre chunk (writes into neighbours the region may not hold).
- **Tim (2026-11-10):** "merge, with the 192-block dome grid ... as per-world rules: on in new worlds, off in existing ones" (answer 2). Built as the world rule `saltDomeGrid192`: on, as here; off (worlds from before), the dome is placed exactly as before Phase 15 and has no oil fields. To keep the old placement the placed feature went back to its old biomes; its own modifier runs the old four with the rule off and hands the chunk to the grid dome with it on (`world-rules.md` 4).

### 3. Where the flank fields lie against the salt
- **Chosen:** a flank field's inner edge is one block inside the salt's radius at the field's top (the salt is wider low down); it runs
  4-12 blocks out. Its arcs are laid one after another round the dome with at least 10 degrees between them, so they never overlap.
- **Why:** the spec wants "a hole on a flank field can pass through the salt's edge": real flank sands pinch out under the salt's
  overhang. With the edge exactly at the salt a vertical hole never touches salt (the salt narrows upward).
- **Alternatives:** the inner edge at the salt (no hole meets salt; washout only from evaporite beds).

### 4. The caprock pool's fall
- **Chosen:** full pressure for its first 10 %; then 35 % of full, falling with what is left. A pumpjack on it pumps at 99 % first, then
  43 % (the pumpjack's rate is pressure x 1.25, at most 1).
- **Why:** the spec asks for "full rate for its first 10 % and then falls fast", and its test for "under half after its first 10 %".
- **Alternatives:** a smooth fall (a cube of what is left), harder to read in play.

### 5. What a hole down the dome's middle finds
- **Chosen:** within the caprock's footprint (salt radius + 1) only the caprock pool, if the dome has one; no ordinary shallow or deep
  field under the salt. Flank fields are checked first.
- **Why:** salt is impermeable and the spec says "no oil below the caprock pool".
- **Alternatives:** a deep field under a dome too (subsalt plays are real, but late and deep).

### 6. The seismograph over a dome
- **Chosen:** over the middle: "SALT DOME: ... about N blocks across ..., its crest at y=N under 4 m of caprock", plus the caprock pool's
  echo if there is one. Over a flank field: "STRONG ECHO ... on a salt dome's flank, N blocks across the band. Its top is at y=N". A
  distant flank field points to the middle of its band, not the dome's axis.
- **Why:** the spec's readout; pointing at the axis would walk the player onto the salt.
- **Alternatives:** none considered.

### 7. The mound
- **Chosen:** 2 blocks high within half the caprock's radius, 1 block to its edge; only on soil or sand under open air (no mound in a
  lake or on stone); the top block kept (grass, sand, mud).
- **Why:** a low hill like Spindletop's that does not change the biome's look.
- **Alternatives:** a wider, gentler hill (more blocks changed per chunk).

### 8. Game tests and the shared test seed
- **Chosen:** a scene that forces a seed now does so for its own area (`ReservoirData.TEST_REGIONS`), so scenes running side by side in
  one batch (the dome, the two well-control scenes) each keep their own field. `testSeed` still works for the client scenes.
- **Why:** with one global test seed the well-control scenes and the dome scene overwrote each other's fields.
- **Alternatives:** run them in separate batches (slower, and the runner decides batches).

### 9. The oilfield scenes stand at y 40
- **Chosen:** the server scenes build on a floor at world y 40, whatever height the runner's padding gives the origin (it gave y 125).
- **Why:** the small rig reaches 110 m; from y 125 no flank field is in reach.
- **Alternatives:** a test-only depth hook on the small rig (the salt dome scene still uses `testDepth` to skip the dull metres).

### 10. The scenes take themselves down
- **Chosen:** each oilfield scene clears its blocks to air before it succeeds.
- **Why:** a land rig left drilling with its sets running stopped the next batch's test from ever finishing (the server ran out of
  memory loading chunks).
- **Alternatives:** none found; the cause inside the runner is not known.

### 11. Mud weight from what is in the tanks
- **Chosen:** the tanks keep water (L) and kg of barite, bentonite and rock salt. Volume = water + barite/4.1 + bentonite/2.6 + dissolved
  salt/2.7; density = mass / volume; weight in ppg = kg/L x 8.345. Fresh water reads 8.34 ppg. Salt dissolves to 360 kg a m3 of water
  (10.0 ppg brine); salt past that is not dissolved. Drilling takes 80 L of mud a metre (water and solids alike); water topping up thins it.
- **Why:** the weight-up formula of the spec falls out of mass and volume (from 8.6 ppg, about 1,030 kg of 4.1 barite a m3 gives 13.75 ppg,
  a little short of the 4.2-grade 13.8); the apparent volume of dissolved NaCl (0.37 L/kg) gives saturated brine its real 1.20 kg/L.
- **Alternatives:** a single "weight" number the hopper raises (no dilution, no volume limit).

### 12. The tanks' volume limits weighting
- **Chosen:** the 60 m3 holds the mud's whole volume; barite adds volume, so a full tank of water cannot be weighted: let the level down
  first (about 40-45 m3 of water to reach 13.8 ppg with room to spare).
- **Why:** real pits fill as barite goes in; the driller builds weight in a partly full system or dumps some.
- **Alternatives:** solids take no room (then 62 t of barite fits into a full tank).

### 13. LOW GEL for plain water
- **Chosen:** as the spec: below 15 lb/bbl of bentonite the rig drills at half rate, and plain water is below it. This halves the land
  rig's rate in every world until bentonite is mixed.
- **Why:** the spec's choice; it gives bentonite its use.
- **Alternatives:** only below 15 lb/bbl once some bentonite has been mixed (water stays full speed); or a smaller penalty.
- **Tim (2026-11-10):** "the LOW GEL mud rule as a per-world rule: on in new worlds, off in existing ones" (answer 2). Built as the world rule `mudGel`: off, plain water drills at full rate, as before. Kicks, NO BOP and the washout went behind a third rule, `wellControl`, so an existing land rig drills as before too (`world-rules.md` 3).

### 14. Barite in a block, and bags per weight-up
- **Chosen:** a barite block is 2.6 t at 90 %, so 2,340 kg of barite (ores.md's density, not the spec's 3.8 t); a weight-up of the land
  rig's system from 40 m3 of water takes about 42 bags (18 blocks).
- **Why:** the ore's density in `Ore.java` is the number of record; the spec's 3.8 t was marked unverified.
- **Alternatives:** raise barite's block density to 4.2 (it is 4.5 for pure barite; 2.6 is low).

### 15. Rock salt in the mud hopper
- **Chosen:** the hopper takes bags of barite and bentonite and blocks of rock salt (2.1 t each, about two minutes of mixing); it mixes a
  tonne a minute whatever the solid, and stops adding salt once the mud is saturated.
- **Why:** rock salt already exists as a mineral; a bagged salt item would add nothing.
- **Alternatives:** a salt bag from a crusher.

### 16. The mud hopper's place and facing
- **Chosen:** against the mud tanks' east face, facing south, so its motor's box (the west of its main cell, as every plant part) faces
  away from the tanks. It finds its rig by touching the tanks.
- **Why:** facing north its box would be against the tanks, where no cable reaches.
- **Alternatives:** put its box on the east for this part only.

### 17. Washout
- **Chosen:** a land rig about to drill a block of rock salt with unsaturated mud stops (WASHOUT), and any blocks of salt it had drilled
  at the bottom of the hole fill back in with rock salt; saturated (97 % of 360 kg a m3), it drills on. Any rock salt counts: a dome's,
  an evaporite bed's, a placed one.
- **Why:** the spec's mechanic, read for any salt the hole meets.
- **Alternatives:** only a dome's salt.

### 18. The mud plant's motors and cables
- **Chosen:** the barite mill 150 kW, the bentonite dryer's mill and fan 75 kW, the mud hopper 37 kW, all at the world's industrial tier,
  boxes on the west of the main cell. The scene feeds the mill on 500 kcmil: the default 2 AWG cable burns out under its 220 A.
- **Why:** the spec's values; the cable is the player's to size.
- **Alternatives:** none.

### 19. The BOP stack's size and place (15b2)
- **Chosen:** 2 x 4 x 2, standing on the wellhead cell (the hole) under the rotary table, so the wellhead still goes into the hole cell on
  a strike. Clicked near a rig it snaps there, facing the rig's way.
- **Why:** the substructure's open BOP space is 5 high, and the hole cell is where the wellhead goes; a real stack is nippled up on the
  wellhead. The spec's 2 x 5 x 2 would have taken the hole cell.
- **Alternatives:** 2 x 5 x 2 with the wellhead set under it (a cellar below ground level).

### 20. The accumulator's link (15b2)
- **Chosen:** within 20 blocks of the substructure (it finds the rig itself every second); no hydraulic line is drawn yet.
- **Why:** the spec's "joined by a hydraulic line drawn as steel pipe" needs a new drawn run between two parts; the distance rule is the
  play. Listed in Known gaps.
- **Alternatives:** a pipe of `pipe` blocks that must connect them.

### 21. When a deep field kicks (15b2)
- **Chosen:** at the strike into a deep field, mud under 13.8 ppg (13.5 pore pressure + 0.3 trip margin) kicks. Shallow and flank fields
  never kick. Without a BOP stack and accumulator the rig will not drill into a deep field at all (NO BOP), rather than stopping at a
  fixed depth.
- **Why:** the spec's "at least 13.8 ppg" to drill into a deep field; "will not drill below the shallow fields' depth" as a fixed y would
  have stopped rigs short of the deepest flank fields (y -50), which are normally pressured.
- **Alternatives:** kick only under 13.5 (underbalanced), the margin left to the player.

### 22. Shut-in pressures and the kill (15b2)
- **Chosen:** SIDPP = 0.052 x (13.5 - mud ppg) x depth in feet; kill weight 13.8 ppg. Once the mud reaches it, one minute of circulation
  at full power (slower when the rig is short of power) kills the well and the rig strikes. A kick's pit gain grows 0.05 m3 a second.
- **Why:** the real driller's formula; the game's depths are short, so the pressures read small (about 80 psi at 96 m).
- **Alternatives:** SICP and a choke schedule (left out by the spec).

### 23. Blowouts (15b2)
- **Chosen:** five free-flowing gas wells' rate (7.5 units a tick) out of the field; a fire, lava or lit campfire within 16 blocks
  (checked each second) lights it: fire round the hole under the floor, BLOWOUT ON FIRE. Shut in by its own BOP if it stands with a close
  left, or killed by a relief well: another land rig 20 blocks or more away striking the same deep field with mud at the kill weight.
  Running engines without a spark arrestor and lightning do not light it yet.
- **Why:** the spec's rules, with the sources of ignition the game already has as blocks.
- **Alternatives:** an explosion that breaks the rig (VISION: safety is a consequence, not the point).

### 24. Art
- **Chosen:** Ludo icons for the two bags (`createImage`, item-icon, Photorealistic 3D, 2 variations each, the first kept; 2 credits): a
  white FIBC with a red band (barite) and an orange band (bentonite), as the art wave's bags. The five parts are true-size models in the
  mod's existing materials. No new placeholders.
- **Why:** docs/questions/art-wave.md's style for bags.
- **Alternatives:** placeholders.

### 25. Not done
- The relief well's kill is in code but has no game test (it needs a second land rig and its power in one scene).
- The PLC's SHUT IN command point and the two 4-20 mA points are wired but only KICK is read in a scene.
- No client scene was filmed for Phase 15 (the client was held by the lead's suite); the client `landRig` and `ores` scenes were updated
  for 15a/15b2 and not run.
