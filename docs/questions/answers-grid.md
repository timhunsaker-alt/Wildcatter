# Building Tim's answers 9, 12, 16, 17 and 18 (branch `w-grid`, 2026-11-10)

Tim's answers are in Tailings' `docs/questions/answers-2026-11-10.md`. These are the calls taken while building them, for his review.
The NeoForge scenes are in `GridAnswerScenes`; the client scene `guyWire` films the guy wire, the slurry pipe and the air header.

### 1. The firebox's one heat rate (answer 17)
- **Chosen:** 0.36 MW (`Tuning.FIREBOX_HEAT_WATTS`), a vanilla coal's 80 s at about 29 MJ, in every world. Every piped fuel lasts its
  heating value at that rate (`fluid/FireboxFuel`): natural gas, wet gas and well gas 38 MJ/m3 (2,111 ticks a m3), biogas 23 MJ/m3 (1,278),
  crude 38 MJ/L (new `CRUDE_JOULES_PER_L`, as fuel oil), diesel and kerosene 36 MJ/L, gasoline, natural gasoline and condensate 32 MJ/L
  (new `GASOLINE_JOULES_PER_L`), residue 38 MJ/L. The crude and diesel bucket items burn by the same rule: a bucket of crude is about 29 hours.
- **Why:** "one real heat rate" covers the liquids too; the bucket burning 100 times shorter than the same 1,000 L off a pipe would be
  inconsistent. Coal, wood and other item fuels keep their vanilla burn times (they set the rate).
- **What changes in an existing world (approved):** a firebox on a gas well or oil well burns about 260 (gas) or 105 (oil) times less for
  the same steam; a firebox on biogas or pipeline gas now stays lit.
- **Scenes changed:** server `biogas` and client `biogas` (the firebox took over 1 m3; it now stays lit on a fraction of a m3).

### 2. Guy wires (answer 16)
- **Chosen:** as planned in protection-gaps 20. A span pulls each end at 25 % of its conductor's rated breaking strength (ACSR from its
  tables; building wire as soft copper at 36 ksi, proposed). The pulls on a pole's insulators (within 2 m of it, an H-frame's middle one
  shared) add as vectors, each scaled by its height to the class load's point 0.6 m below the top. Guys (3/8 in EHS, 15,400 lbf, NESC
  factor 0.9, times lead over length) take the share pulling away from their anchors. Past the class load (30 ft class 5 1,900 lbf, 40 ft
  class 4 2,400, 75 ft class 2 3,700) a pole reads UNGUYED (or UNDER-GUYED with a guy) and leans a step every 10 minutes overloaded:
  1.5, 3 and 5 degrees. It never breaks. A guy that holds the pull draws a leaning pole plumb again.
- **The lean is drawn about the pole's top cell**, so the crossarm, insulators and spans stay where they are and the foot kicks out. A
  pole turned about its foot would leave its hardware hanging in the air.
- **Existing worlds:** a pole from an older save (no `leans` marker on its block entity) reads UNGUYED but never leans. The one-block
  `pole` sections take no guys and read nothing.
- **Parts:** `guy_anchor` (an 8 in screw anchor's rod eye, drawn in the mod's galvanized material, no new texture) and `guy_strand`, a
  60 m coil (click the pole near its top, then the anchor). One guy an anchor, four a pole; the anchor must be 2 m out and below the clamp.
  The guy is drawn as a straight line. `guy_strand_placeholder` is a script-drawn icon (Known gaps).
- **Times proposed:** 10 minutes a step (real soil creep takes days).

### 3. A pole transformer placed by command (answer 18)
- **Chosen:** a new pole unit looks once, on its first tick, for its pole. If it is in the default facing (north, what a command gives
  when it names none), has a pole beside it but not behind it, it turns its back to that pole. A facing named in the command is kept.
- **Marker:** `faced` on the transformer's block entity. A unit from an older save loads without it and counts as faced, so it keeps its
  terminals.
- **Why only the default facing:** a command that names a facing means it; and the seven client scenes that set units by command all name
  their facings, so none is rewired and none needs re-laying.

### 4. The 600 mm slurry pipe (answer 12)
- **Chosen:** `slurry_pipe`, rubber-lined steel, 600 mm (24 in), rated 1 m3/s (0.28 m2 at 3.5 m/s). A run of its own: it joins only
  slurry pipe (no reducer to the 4 in steel pipe) and carries no gas. Slurries counted in kg are rated at 1.5 t/m3 (75 kg a tick,
  5,400 t/h); liquids in litres at 1,000 L/s. Every pull through a run in one game tick shares its rating.
- **Arms:** it draws arms to slurry pipe, to the decant tower, and to any block of another mod that implements `PipeBlock.Joins`.

### 5. The decant tower's rate scales with its cell (answer 12)
- **Chosen:** "its size" read as the cell's floor (the pond the tower decants), not a stack of towers: CLAUDE.md forbids tiling one block
  to make a bigger machine, and a real pond's clarifying capacity goes with its surface (overflow rate). 20 kg a tick (400 kg/s,
  1,440 t/h of slurry) on up to 256 m2 of floor, and in proportion above: a 32 x 32 cell takes 80 kg a tick (5,760 t/h).
- **A pit line:** its 1,670 t/h of underflow (23 kg a tick) needs about 300 m2 of floor, a cell about 18 x 18.
- **Note for Tailings:** p7 #40 read the tower as 72 t/h; it was 1,440 t/h of slurry (20 kg a tick, every tick). The rate was never the
  limit for a single cell up to 1,440 t/h.
- **Existing worlds:** a tower from an older save (no `scales` marker) keeps 20 kg a tick whatever its cell.

### 6. The flotation air header (answer 12)
- **Chosen:** `air_header`, 600 mm galvanized, a network of its own (`PipeBlock.Kind.AIR_HEADER`), apart from instrument air tubing.
  About 5.7 m3/s at 20 m/s (`Tuning.AIR_HEADER_M3_S`), for Tailings to size its blowers against; Wildcatter puts no blower on it.

### 7. Zircon concentrate in the chlorinator (answer 9)
- **Chosen:** the tag `#wildcatter:zircon_feed` (Wildcatter ships it empty) and a kg file per item, read from data packs:
  `data/<namespace>/wildcatter/zircon_feed/<name>.json` = `{"item": "<item id>", "zircon_kg": <kg of ZrSiO4 in one item>}`. A tagged item
  without a kg file counts as 56.25 kg. The chlorinator's hopper takes it in a second slot (coal stays in the first), skipping the
  separator's ilmenite/rutile/monazite split; fed this way it needs no mineral separator. With both, the hopper's zircon goes first.
- **Why the chlorinator, not the separator:** the concentrate is already zircon; the separator's job is the split.

### Rules
None of these needed a per-world rule. Nothing changes in an existing world but the firebox (approved): built poles never lean, built
pole units keep their facing, built towers keep 20 kg a tick, and the new parts are new.
