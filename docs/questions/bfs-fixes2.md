# Built From Scratch fixes 2: judgement calls (2026-11-26, branch `bfs-fixes2`)

Three gaps found while filming Built From Scratch episode 3 (`videos/longform/built-from-scratch/ep03/decisions.md` items 6, 10 and 15).
Format as in `docs/questions/README.md`.

### 1. Crafting grids that gave two things
- **Found:** a new scene, `recipes_unique`, lays every grid each Wildcatter shaped and shapeless recipe takes (every key on its first
  item, then each key through every item its tag or list allows) and asks every crafting recipe loaded, the game's included, whether it
  matches; a shaped recipe matches its mirror image too. Before this branch it found 44 pairs in 29 groups: 27 among Wildcatter's own
  recipes, and two with the game's (the ground rod was the lightning rod's grid, the empty drum the cauldron's). The table crafts only one
  recipe of a pair, so the other item could not be made by hand (as the stock tank in episode 3).
- **Chosen:** each pair given its own grid (table below). Rules followed: real parts for what the item is, about the same cost (most
  changes swap one steel for a pipe, or move a pipe; none adds a material from a later chapter), the less-used item changed (a quest
  item kept where the other is not; the item more code uses kept where neither is), and the new grid checked against every other recipe,
  the game's too. Each change is in `tools/make_assets.py` (or `tools/oilfield_assets.py`) and its JSON by hand; the generators were not
  run. Every new ingredient is a vanilla item made from iron, copper, bricks or cobblestone, or steel pipe, all in hand at the Getting
  started and First power chapters.
- **Alternatives:** a recipe-priority list (NeoForge can order recipes) would pick a winner but leave one item uncraftable; renumbering
  counts (two of one, three of the other) is not possible, as a grid gives one result.
- **Needs Tim:** every grid below is a choice. Two touch filmed episodes or are arbitrary: the **ground rod** (episode 2 crafts and
  narrates three copper in a column; the game's lightning rod now crafts again) and the **480 V busway** (it and the 208Y/120 V busway
  are the same 800 A product, so its two iron ingots are only a marker).

| Item | Old grid | New grid | Why |
|---|---|---|---|
| LiOH Crystallizer (`lioh_crystallizer`) | `SPS` / `SCS` / `SXS` (S steel, P steel pipe, C copper block, X piston) | `SPS` / `SCS` / `SHS` (S steel, P steel pipe, C copper block, H hopper) | Was the phosphoric acid evaporator's grid. Crystals leave a crystallizer at the bottom: a hopper there in place of the agitator's piston. Neither is a quest item; the crystallizer is the later one (battery chain). |
| Aux Feed Pump (75 hp) (`aux_feed_pump`) | `SPS` / `SXS` / `SSS` (S steel, P steel pipe, X piston) | `SPS` / `SXS` / `SCS` (S steel, P steel pipe, X piston, C copper) | Was the uranium acid pretreatment's grid. A copper ingot for the pump's motor windings in place of one steel. |
| LFP Kiln (700 C) (`lfp_kiln`) | `SHS` / `BFB` / `SPS` (S steel, H hopper, B bricks, F furnace, P steel pipe) | `SHS` / `BFB` / `SBS` (S steel, H hopper, B bricks, F furnace) | Was the AlF3 reactor's and the magnesia chlorinator's grid. A roller-hearth kiln, brick floor (bricks in place of the steel pipe). Grid batteries are the latest chain. |
| Magnesia Chlorinator (`magnesia_chlorinator`) | `SHS` / `BFB` / `SPS` (S steel, H hopper, B bricks, F furnace, P steel pipe) | `SHS` / `BFB` / `PSP` (S steel, H hopper, B bricks, F furnace, P steel pipe) | Was the AlF3 reactor's grid (and the LFP kiln's). Two pipes at the base, chlorine in and the chloride's off-gas out. `SPS / BFB / SPS` was the TiCl4 oxidiser's. |
| Turbine Generator (4.16 kV, 1,250 kVA) (`turbine_generator`) | `SWS` / `WRW` / `SXS` (S steel, W stator winding, R rotor field, X exciter) | `SWS` / `WRW` / `PXP` (S steel, W stator winding, R rotor field, X exciter, P steel pipe) | Was the 480 V alternator's grid (both quest items: Three-phase power, Distribution). The alternator is used earlier and by more code, so the turbine generator changed: two steel pipes at the base for its air coolers' water. Same cost (a pipe is a steel). |
| Mud-Gas Separator (`mud_gas_separator`) | `SPS` / `S·S` / `SPS` (S steel, P steel pipe) | `SPS` / `SIS` / `SPS` (S steel, P steel pipe, I iron bars) | Was the topping column's grid (and the side strippers' and the AN neutraliser's). The topping column is a quest item (Refining) and kept. A mud-gas separator's inside is baffle plates: iron bars in the middle. |
| Side Strippers (kerosene and diesel) (`side_strippers`) | `SPS` / `S·S` / `SPS` (S steel, P steel pipe) | `PSP` / `S·S` / `PSP` (S steel, P steel pipe) | Same group. Small columns beside the main one, a draw and a return each: four pipes at the corners. Same cost. |
| Ammonium Nitrate Neutraliser (`an_neutraliser`) | `SPS` / `S·S` / `SPS` (S steel, P steel pipe) | `SPS` / `SKS` / `SPS` (S steel, P steel pipe, K cauldron) | Same group. The neutraliser is a vessel: a cauldron in the middle. (`SPS / SBS / SPS` with a bucket was the pressurizer relief tank's.) |
| Potash Dissolver (`potash_dissolver`) | `SHS` / `SXS` / `SPS` (S steel, H hopper, X piston, P steel pipe) | `SHS` / `SXS` / `SCS` (S steel, H hopper, X piston, C copper) | Was the brine saturator's grid. Potash is dissolved hot: a copper ingot (the heating coil) in place of the pipe. |
| Busway (480 V, A B C N G) (`busway_industrial`) | `SCS` / `CCC` / `SCS` (S steel, C copper block) | `SCS` / `CSC` / `SCS` (S steel, C copper block) | Was the 208Y/120 V busway's and the generator bus duct's grid. Two iron ingots in the housing in place of two steel, as the 480 V wire takes an iron ingot the 120 V wire does not. Both busways are the same 800 A product, so any difference is a marker. **Needs Tim.** |
| Generator Bus Duct (13.8 kV, 1,200 A, A B C N G) (`bus_duct_gen`) | `SCS` / `CCC` / `SCS` (S steel, C copper block) | `BCB` / `CCC` / `BCB` (B brick, C copper block) | Same group. The 13.8 kV duct stands its bars on porcelain standoffs: bricks (the mod's porcelain, as in the insulators) in place of the steel corners. Four steel fewer. |
| Generator Bus Relay Panel (87B, 51V, 50/51) (`bus_relay_panel`) | `SGS` / `SRS` / `SCS` (S steel, G glass pane, R comparator, C copper) | `SGS` / `SRS` / `SDS` (S steel, G glass pane, R comparator, D redstone) | Was the 4.16 kV switchgear feeder's grid (quest item, Distribution; kept). The panel's wiring: redstone in place of the copper. |
| IDR Kiln (dry route) (`idr_kiln`) | `SPS` / `BFB` / `SXS` (S steel, P steel pipe, B bricks, F blast furnace, X crankshaft) | `PSP` / `BFB` / `SXS` (S steel, P steel pipe, B bricks, F blast furnace, X crankshaft) | Was the calciner's grid (the calciner is used far more). Steam and hydrogen in at the top: two pipes in the top corners. Same cost. |
| Exhaust Silencer (500 kW) (`exhaust_stack`) | `S` / `S` / `S` (S steel) | `S` / `B` / `S` (S steel, B iron bars) | Was the casing joint's grid (quest item, Oil and gas; kept). A silencer's perforated baffles: iron bars in the middle. |
| Caustic Evaporator (to 50 %) (`caustic_evaporator`) | `SPS` / `SCS` / `SPS` (S steel, P steel pipe, C copper block) | `SPS` / `SCS` / `PSP` (S steel, P steel pipe, C copper block) | Was the HP feedwater heater's and the TiCl4 purification's grid. Steam in and condensate out at the base: pipes in the bottom corners. Same cost. |
| TiCl4 Purification (`ticl4_purification`) | `SPS` / `SCS` / `SPS` (S steel, P steel pipe, C copper block) | `SPS` / `SCS` / `SFS` (S steel, P steel pipe, C copper block, F furnace) | Same group. A still with its reboiler: a furnace at the bottom middle (the copper block stays: copper is what strips vanadium from crude TiCl4). |
| Titanium Chlorinator (controller) (`ti_chlorinator`) | `SHS` / `BFB` / `SPS` (S steel, H hopper, B bricks, F blast furnace, P steel pipe) | `SHS` / `BFB` / `PSP` (S steel, H hopper, B bricks, F blast furnace, P steel pipe) | Was the chlorinator's grid (the chlorinator is used far more). Chlorine in and TiCl4 vapour out: pipes in the bottom corners. Same cost. |
| RHR Pump (200 kW, 4.16 kV) (`rhr_pump`) | `SCS` / `SXS` / `SPS` (S steel, C copper block, X piston, P steel pipe) | `SCS` / `SXS` / `PSP` (S steel, C copper block, X piston, P steel pipe) | Was the circulating water pump's grid. Suction and discharge at the base corners. Same cost. |
| Cracked Gas Compressor (300 hp, 4.16 kV) (`cracked_gas_compressor`) | `SPS` / `CXC` / `SBS` (S steel, P steel pipe, C copper block, X crankshaft, B iron block) | `PSP` / `CXC` / `SBS` (S steel, P steel pipe, C copper block, X crankshaft, B iron block) | Was the gas compressor's grid (both Refining quest items, same chapter gate). The cracked gas machine is multi-stage: suction and interstage lines at the top corners. Same cost. |
| Emulsion Blender (`emulsion_blender`) | `SHS` / `PXP` / `SSS` (S steel, H hopper, P steel pipe, X piston) | `SHS` / `PXP` / `SCS` (S steel, H hopper, P steel pipe, X piston, C copper) | Was the CVCS skid's grid. The blender's mixer motor: a copper ingot at the bottom middle. |
| UF6 Distillation Columns (`distillation_columns`) | `SPS` / `S·S` / `SFS` (S steel, P steel pipe, F furnace) | `SPS` / `SPS` / `SFS` (S steel, P steel pipe, F furnace) | Was the stabilizer's grid (quest item, Refining; kept) and the hafnium column's. Two columns: a second pipe where the gap was (one steel pipe more). |
| Hafnium Column (extractive distillation) (`hafnium_column`) | `SPS` / `S·S` / `SFS` (S steel, P steel pipe, F furnace) | `SPS` / `S·S` / `BFB` (S steel, P steel pipe, F furnace, B bricks) | Same group. Extractive distillation in molten salt: a brick-lined reboiler (bricks either side of the furnace). |
| Empty 48Y Cylinder (`empty_48y_cylinder`) | `SSS` / `S·S` / `SSS` (S steel) | `SSS` / `S·S` / `SCS` (S steel, C copper) | Was the dry storage canister's grid (quest item, Nuclear; kept) and the fuel tank's. The cylinder's valve (a copper-alloy valve on one head): copper at the bottom middle. |
| Sub-Base Fuel Tank (2,000 L) (`fuel_tank`) | `SSS` / `S·S` / `SSS` (S steel) | `SSS` / `S·S` / `SPS` (S steel, P steel pipe) | Same group. The fuel line out of the base: a steel pipe at the bottom middle. (`SPS / S S / SSS` was the deaerator's.) Same cost. |
| Empty 30B Cylinder (`empty_30b_cylinder`) | `S·S` / `S·S` / `SSS` (S steel) | `SCS` / `S·S` / `SSS` (S steel, C copper) | Was the mud tanks' grid (an open tank, which keeps the open top). The 30B is a closed cylinder with a valve: copper on top. |
| Equipment Hatch (`equipment_hatch`) | `SSS` / `SIS` / `SSS` (S steel, I iron block) | `SSS` / `SDS` / `SSS` (S steel, D iron door) | Was the engine block casting's grid (quest item, Three-phase power; kept). A hatch is a door: an iron door in place of the iron block (seven iron cheaper). |
| Submerged Arc Furnace (8 MW) (`submerged_arc_furnace`) | `SHS` / `BFB` / `SBS` (S steel, H hopper, B bricks, F blast furnace) | `CHC` / `BFB` / `SBS` (S steel, C copper, H hopper, B bricks, F blast furnace) | Was the fluid-bed roaster's grid. An arc furnace's electrodes and their clamps: copper ingots in the top corners. |
| HCl Synthesis Unit (`hcl_synthesis_unit`) | `SPS` / `SBS` / `SPS` (S steel, P steel pipe, B blast furnace) | `PSP` / `SBS` / `SPS` (S steel, P steel pipe, B blast furnace) | Was the glycol dehydrator's grid. Hydrogen and chlorine in at the burner: pipes in the top corners. Same cost. |
| Rod Welder (helium, 2 MPa) (`rod_welder`) | `SCS` / `GRG` / `SSS` (S steel, C copper block, G glass, R redstone block) | `SCS` / `GRG` / `SPS` (S steel, C copper block, G glass, R redstone block, P steel pipe) | Was the IFBA coater's grid. The welder's helium line: a steel pipe at the bottom middle. |
| VPSA Oxygen Unit (100 Nm3/h) (`vpsa_oxygen`) | `SPS` / `SXS` / `SIS` (S steel, P steel pipe, X piston, I iron block) | `PSP` / `SXS` / `SIS` (S steel, P steel pipe, X piston, I iron block) | Was the lift air compressor's grid. Oxygen out and nitrogen vented: pipes in the top corners. Same cost. |
| Nickel Electric Furnace (1 MW, six-in-line) (`nickel_electric_furnace`) | `SHS` / `BFB` / `BBB` (S steel, H hopper, B bricks, F blast furnace) | `CHC` / `BFB` / `BBB` (C copper, H hopper, B bricks, F blast furnace) | Was the lime kiln's grid (quest item, Oil and gas; kept). An electric furnace's electrodes: copper ingots in the top corners. |
| Vacuum Crystallizer (`vacuum_crystallizer`) | `SPS` / `S·S` / `SXS` (S steel, P steel pipe, X piston) | `SPS` / `S·S` / `SHS` (S steel, P steel pipe, H hopper) | Was the polyethylene reactor's grid (quest item, Refining; kept). Crystals leave at the bottom: a hopper in place of the piston. |
| Top Drive (250-ton, 400 hp) (`top_drive`) | `SCS` / `CXC` / `SPS` (S steel, C copper, X crankshaft, P steel pipe) | `SNS` / `CXC` / `SPS` (S steel, N chain, C copper, X crankshaft, P steel pipe) | Was the refrigeration compressor's grid (quest item, Refining; kept). A top drive hangs from the travelling block: a chain at the top middle. |
| Empty Drum (208 L) (`empty_drum`) | `S·S` / `S·S` / `SSS` (S iron) | `SNS` / `S·S` / `SSS` (S iron, N iron nugget) | Was the game's own cauldron grid (seven iron in a U). A drum is closed, with a bung: an iron nugget in the top middle. |
| Ground Rod (`ground_rod`) | `C` / `C` / `C` (C copper) | `C` / `I` / `C` (C copper, I iron) | Was the game's own lightning rod grid (three copper in a column; the mod's rod won, so a lightning rod could not be crafted). Real ground rods are copper-clad steel: copper, iron, copper. Quest item (First power): iron is in hand by then. **Needs Tim:** episode 2 filmed and narrated "three copper in a column make two ground rods". |
| Receptacle IEC 60309 red (400 V 3-phase, 63 A) (`receptacle_iec_60309_red_63`) | shapeless: 1 iron, 4 copper, 1 red dye | shapeless: 2 iron, 4 copper, 1 red dye | Shapeless: one iron, four copper and red dye were also the 480 V red bare wire's (quest item, Three-phase power; kept). The 63 A housing is bigger: a second iron. |

Grids read top row to bottom row; `·` is an empty slot; a shaped grid also matches mirrored left to right.

### 2. A ground rod against any cell of a big part
- **Chosen:** `Grounding.isGrounded` starts from every cell of the machine (`BigParts.cellsAt`), not only the cell asked about. Every
  face of every cell is looked at first, outside the search's limit; the ground wire search that follows has the same reach as before
  (a one-block machine's search is unchanged). One place, so every readout, shock and panel check agrees: the rig's and the pumpjack's
  readouts, `Shock.touch` from whichever cell is clicked, any other big part with a readout.
- **Why:** a big part is one metal frame; the rod against the rig's skid or the pumpjack's terminal box bonds the whole of it.
- **Proof:** `bfs03_big_part_grounded`: the rig and the pumpjack read UNGROUNDED alone, then grounded with a rod on the floor against a cell
  six or seven blocks from the main one (the scene picks the free spot farthest from it). Before the fix the search looked only at the main
  cell's six faces (and ground wire from them).
- **Note:** a rod against any cell of a big part now protects it from shock, which is a change to every big part's shock rule.

### 3. Readouts too long for the screen
- **Chosen:** the game's action-bar layer is wrapped in both loaders (Fabric `HudElementRegistry.replaceElement`, NeoForge
  `RegisterGuiLayersEvent.wrapLayer`, both on the overlay message) by `dev.wildcatter.client.ReadoutOverlay`. A message wider than the
  screen less 12 px a side is drawn wrapped onto centred lines (the last where the game's line sits) over a dark band, held 2 s plus 40 ms
  a character (at most 12 s; a 170-character readout about 9 s) and faded over the last half second. A message that fits, or one the game
  animates (a record playing), is drawn by the game as before. Applies to every action-bar message, not only Wildcatter's.
- **Why:** the film's wrap (`Bfs03Episode.readoutHook`) read well; this is the same, in the game.
- **Proof:** Fabric client scene `readout` (`WILDCATTER_ONLY=readout`): the pumpjack's readout sent as the meter sends it, at the default
  854 x 480 window (gui 427 wide), is drawn in 3 lines, each inside the screen, every word there, still up after 5 s; a short message is
  left to the game. Screenshots `wildcatter-readout-wrapped`, `wildcatter-readout-held`.
- **Alternatives:** shorter readouts (would lose information); chat messages (scroll away, clutter chat).

### 4. Flaky deep-field scenes (not fixed)
`casing_lot`, `well_control`, `blowout` and the scenes on `OilfieldFollowOnScenes.deepField` (`oil_mud` failed so once on this
branch) fix the hole at a set place from the scene's origin and search seeds for a deep oil field under
it. A deep field's centre is at least its largest radius from its 128-block cell's edge, so a hole a few blocks from a cell edge is
seldom covered and the search can fail. Aiming the hole (as the shallow scenes' `aim()`) means moving the whole land-rig layout, which
these scenes build at fixed positions; left for now, rerun when it fails.
