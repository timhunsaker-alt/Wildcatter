# Misc gaps wave (2026-11-09): decisions taken while Tim was away

Tim (2026-11-09): "make decisions best recommendation ... close all known gaps". This wave takes the leftover Known gaps of
PROGRESS.md that need no Tim answer and no Mekanism: the drawing-only gaps of `plant-crane-gaps.md` 15, the coke oven gas engine and
the aeroderivative (`plant-crane-gaps.md` 7), the cracker test's water, the spool icons and a few small ones. Branch `misc-gaps`.
Format as in `README.md` here.

### 1. The steam plant's main steam and feedwater lines between parts
- **Chosen:** the main steam line is drawn in the part models flange to flange: the boiler's outlet now runs to a flange on its
  rear edge, the superheater's inlet starts with a mating flange there, and the superheater's drop to the turbine and the turbine's
  inlet meet with a flange pair at their shared edge (they stopped 0.1-0.2 m short of it before). The feedwater line, which belongs to
  no one part, is drawn by the generator (the plant's controller) once the plant is formed: NPS 2 steel from a new flange on the feed
  pump's discharge, up 2.1 m, north over the generator on two pipe posts, to a new flange on the boiler's south edge; inside the
  boiler it runs along the south side through its stop and check valves and up into the drum (it started from nothing at the ground
  before). A new `DrawnRuns` interface and `RunsRenderer` draw such runs (pipes, cable trays, cable drops, posts) in the mod's
  material textures.
- **Why:** the plant's layout is fixed relative to the generator, so the runs between parts can be drawn as one piece; drawing them
  only on a formed plant means a lone part never shows a pipe hanging into the air. Over the generator is the one route that does not
  cross the burner or the turbine; 2.1 m clears the generator's enclosure (1.76 m).
- **Alternatives:** a floor-level route round the generator's west end (through the cell kept free for the GSU cable); new pipe-rack
  blocks the player places; or drawing the line in the part models (it would show on a lone part).

### 2. The alternator's length
- **Chosen:** the alternator block has a `frame` state: `kw500` (the 500 kW diesel set's, 1 m, as before) and `mw1` (the 1 MW gas
  set's, 1.6 m from adapter to exciter end, frame 0.96 m across). The 1.6 m frame reaches 0.6 m back into the gas engine's last cell,
  and the gas engine is drawn 3.4 m long to its flywheel housing (3.8 m before), its turbochargers and charge-air cooler now over the
  alternator's adapter. The control panel sets the frame when the set forms; the coupling and fan turn 0.6 m further west on the 1 MW
  frame. Layouts, footprints, items and recipes are unchanged, so built sets keep working (an old gas set's alternator redraws at
  1.6 m on its next check).
- **Why:** a 1 MW 4-pole alternator is about 1.6 m long (questions 15), and a 1 MW lean-burn V20 engine about 3.3 m; the set's 7 m
  base already held both. Growing the alternator's footprint would have moved the panel and broken every built set.
- **Alternatives:** a separate 1 MW alternator item (a new recipe, and old gas sets would no longer form); a longer gas set base (the
  same break).

### 3. The brass swatch's tile line
- **Chosen:** `tools/process_materials.py` makes `mat_brass` from one tile's face of the Ludo picture, blended with itself shifted
  half a swatch (so its edges are the picture's own middle and repeat with no seam), held at the old swatch's average colour.
- **Why:** Ludo drew brass as large tiles with dark grout; no 34 % window misses a grout line, and the old edge cross-fade left one.
  Keeping the tone keeps every brass valve and gland reading as before.
- **Alternatives:** a new Ludo picture asked for as plain metal (credits, and a new look).

### 4. The land rig's cables from the drive house
- **Chosen:** drawn, not modelled as cable runs: the substructure (the rig's controller) draws them once the rig is formed. A 0.45 m
  galvanised ladder tray on the ground out of the drive house's east end, east past the mud tanks and north along the pumps' east
  side, three cables rising from it to each pump motor's terminal box; and a tray out of the drive house's north side, up the back of
  the substructure to the drill floor, the drawworks motor's three cables over to its terminal box. The motors stay inside the drive
  house's load.
- **Why:** on a real rig the VFD house feeds each motor on its own cable from its own drive; in the mod the drive house is already one
  4.16 kV load with the drives' front end, and the rig's layout is fixed, so the cables carry no new physics. Modelling them as real
  runs would mean six 600 V cable circuits the player could cut, with nothing in the drives to read them.
- **Alternatives:** real cable blocks from the drive house to each motor (a player could cut a pump's feed; the drive house's load
  would split by motor); cable "spider" boxes on the ground.

### 5. Coke oven gas in the gas engine and the aeroderivative
- **Chosen:** a gas set on coke oven gas is held to 86.4 % of its rating, 864 kW (its governor holds it there, as on biogas), with the
  same heat rate; coker gas keeps the full rating. The aeroderivative refuses coke oven gas: it takes a gas only within 10 % of
  pipeline gas's Wobbe index (heating value over the root of specific gravity). Coke oven gas is 29.6 MJ/m3 against pipeline gas's
  49.1 (40 % off, and about 55 % hydrogen); coker gas is 49.8 (+1.5 %) and runs. The 16.5 MW turbine still takes both.
  Its readout: "WRONG GAS: coke oven gas's Wobbe index is 30 MJ/m3, 40 % off the pipeline gas its fuel nozzles are tuned to (49)".
- **Why:** the Profusa coke gas plant in Bilbao runs 12 lean-burn V16 sets at 5,642 kW on 100 % coke gas against 6,528 kW on natural
  gas (86.4 %; the hydrogen's knock, not the heating value, sets it). An aeroderivative's dry low-emission combustor is tuned to one
  Wobbe index: Siemens' fuel gas specification allows 5 % (more only with special controls), and high-hydrogen gas is outside DLE
  limits; steel works burn their gases in heavy-duty frames with diffusion combustors. 10 % is allowed here, so coker gas runs.
  Specific gravities: pipeline gas 0.60 (typical), coke oven gas 0.37 (55 % H2, 25 % CH4, 6 % CO, 6 % N2, the rest CO2 and
  ethylene: 0.45 kg/m3), coker gas 1.0 kg/m3 on air's 1.225 (Tuning).
- **Alternatives:** a derate in proportion to heating value alone (would give coke oven gas about 47 %, far below real practice); a
  10-20 % range (the earlier decision's words) without a source; letting the aeroderivative take coke oven gas with water injection (a
  diffusion-flame aeroderivative can, at much higher NOx: the mod's has an SCR but no water injection).

### 6. The cracker test's water
- **Chosen:** a new NeoForge server scene `cracker_pump`: a cracking furnace, a process water pump over a sump on a 500 kW diesel set's
  480 V, water pipe to the furnace's west side; the furnace fills from dry to 200/200 L with no hook. The Fabric client scene `cracker`
  still tops its furnace up by hook: the client could not be run in this wave (another suite held it), and an unrun change to a
  passing scene was not worth the risk.
- **Why:** the gap was that a pump feeding a furnace was never exercised; the server scene exercises exactly that path
  (`PumpBlockEntity` to a `WaterUser`).
- **Alternatives:** change the client scene to lay a pump, sump and pipe from its 480 V set (left for the next client run: the
  set at x 35-40 is 30 m from the furnace).

### 7. The spool icons
- **Chosen:** all eight conductor spool icons (bare and covered, LV, MV, DIST, HV) are Ludo art now: `createImage`, `item-icon`,
  `Photorealistic 3D`, two variations each (8 credits), the better kept, through `tools/process_art.py`'s `icon()`; prompts in
  `art/source/PROMPTS.md`. A wooden reel wound with bright copper (LV), dull bronze (MV), pale aluminium (DIST) or steel-grey ACSR
  (HV); the covered ones in white covering with the strands showing at the cut end.
- **Why:** the gap named the MV spools, but no spool had Ludo art; doing all eight keeps them alike in the inventory.
- **Alternatives:** MV only (the two MV icons would be the only photographic spools).

### 8. Small ones taken in this wave
- **Crane rail kept by `make_assets.py`:** running `tools/make_assets.py` deleted the plant-crane-gaps wave's `crane_rail`
  (blockstate, model, item, loot, recipe, its name and tags), which were written by hand. Its files now live in `art/hand_assets`, and
  the script adds its name and tags.
- **PROGRESS.md:** the stale Phase 14 entry that still listed the gauze pack and emulsifier pail as placeholders is gone (they are
  Ludo art since the art wave).

### 9. Not done in this wave
- Guy wires and the command-placed pole transformer's facing (wait on Tim: protection-gaps 20 and 21); residence times
  (plant-crane-gaps 11); anything that needs Mekanism.
- Pumpjack and rig collision on their moving parts, and the potroom building (questions 15): not small.
- The client scenes that changed (`cracker` unchanged; no client scene was run here): the steam plant's feedwater line, the rig's
  cables and the 1 MW alternator have been checked in model previews and server scenes, not yet in the client.
