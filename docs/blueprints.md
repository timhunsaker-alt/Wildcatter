# Blueprints (Phase 10c1, approved with the 10c plan 2026-11-04)

A blueprint is a plant written down: every block and every true-size part in a box, with its facing, so that it can be built again
somewhere else. Each blueprint lists its lifts, and the construction cranes (10c2, 10c3: tower-crane.md, crawler-crane.md,
ring-crane.md) build from that list. It is also a guide for building by hand: the site marker draws the plant as a ghost.

**Decided with Tim on 2026-11-04:**
- **Sources:** both a library of the mod's plants and a survey tool for your own.
- **Contents:** any block.
- **Pallets:** ordinary blocks go up on pallets, up to 64 blocks of one area a lift. Big parts go one to a lift.
- **Preview:** a ghost of the blueprint at its site, locked with a site marker.
- **The library:** power plants, oil and gas, nuclear, smelter.

**Real counterparts:**
- the general arrangement drawing and the lift plan of a real project;
- a rigging study lists each lift with its mass, its radius and the crane that makes it, and the erection sequence orders them;
- on site, ordinary materials (block, brick, pipe, cable drums) go up on pallets and in skips;
- the heavy equipment goes up piece by piece.

A figure marked *(proposed)* is an estimate or a proposal.

## What a blueprint holds (`blueprint/Blueprint`, plain Java)
- **Size:** its size (x, y, z), written facing north from its north-west bottom corner, the **origin**.
- **Palette:** the block states it uses, as strings (`wildcatter:cable_mv`, `minecraft:stone_bricks`, `wildcatter:dc_bus_180k[axis=x]`).
- **Blocks:** one entry for each ordinary block (its offset and its palette index).
  - Air is left out.
  - So are fluids, fire and blocks with no item.
- **Parts:** one entry for each true-size part: its id, its facing, and the offset of its main cell. The part's other cells are not
  listed; they grow from it when it is set.
- **Conductor sizes:** the size of each wire and cable that has one, so it is built in the same size.
- **Not recorded:**
  - what a block holds (a chest's items, a tank's oil, a pot's metal);
  - the state of a machine (a genset running, a breaker open).
  - A built blueprint starts empty and stopped, as a hand-placed plant does.

### The lift list (`Blueprint.lifts()`)
1. Layer by layer, bottom up, by each lift's lowest cell.
2. In a layer, the big parts first, then the pallets.
3. A part that rests on another part comes after it; resting means a cell of it is directly above a cell of the other.
4. Ordinary blocks go into pallets:
   - each pallet is up to 64 blocks of one layer, inside one 5 x 5 m area;
   - its mass may not pass the lightest chart limit of the crane taking it, and a heavy pallet splits;
   - a layer's pallets go west to east, then north to south.
5. Dismantling takes the list backward.

### Masses (`blueprint/Masses`) *(proposed)*
**A part's mass is its own figure where one is known:**

| Part | Mass | Basis |
|---|---|---|
| `reactor_vessel` | 180 t | a 3 m x 10 m forged vessel of about 200 mm wall with its heads, at this 155 MWt core |
| `steam_generator` | 150 t | a 3 m x 12 m U-tube steam generator for one of two loops of this size |
| `pressurizer` | 50 t | a 2 m x 8 m pressurizer, empty |
| `reactor_coolant_pump` | 40 t | a canned-motor RCP, its motor included |
| `hp_turbine` | 40 t | a 51 MW machine's HP section |
| `lp_turbine` | 120 t | its double-flow LP section with its hood |
| `tg_generator_60` | 150 t | a 60 MVA hydrogen-cooled generator (stator about 100 t) |
| `gsu_60`, `substation_transformer_60` | 90 t | 60 MVA, 138 kV, oil-filled |
| `rectiformer` | 85 t | a 14 MVA rectiformer with its oil |
| `pot` | 60 t | a 180 kA prebake pot's shell, cathode and superstructure |
| `aero_turbine` | 20 t | an LM6000-class gas generator and power turbine on its skid |
| `aero_generator`, `gt_generator` | 80 t | a 45-60 MVA air-cooled generator |
| `natural_draft_tower` | by its kind | the tower's controller; it pours the shell itself (cooling-tower.md), so the shell is never lifted |

**Every other part:** its footprint box (W x H x L, in m³) times the density of its kind *(proposed)*:

| Kind | Which parts | t/m³ of box |
|---|---|---|
| Transformers | every part whose id holds `transformer`, `gsu_`, `padmount_`, `unit_substation`, `drive_mv_transformer` | 0.5 |
| Rotating machines | ids holding `turbine`, `generator`, `compressor`, `pump`, `mill`, `fan`, `blower`, `engine`, `alternator` | 0.4 |
| Cabinets and gear | ids holding `breaker`, `cabinet`, `starter`, `mcc`, `drive_`, `control`, `panel`, `relay` | 0.3 |
| Structures | ids holding `tower`, `stack`, `derrick`, `rig`, `house`, `filter`, `quench` | 0.05 |
| Everything else | vessels, columns, kilns, process skids | 0.15 |

**A pallet's mass is its blocks' masses, by what the block is made of** (read from its sound type, *proposed*):
- stone, deepslate, brick, concrete, sand and gravel: 2.4 t a block;
- metal and anvils: 7.8 t (a block of iron);
- copper: 8.9 t;
- glass: 2.5 t;
- wood, bamboo, cherry and nether wood: 0.6 t;
- wool, carpet, leaves, plants and snow: 0.05 t;
- a Wildcatter wire, cable, pipe, bus bar or one-block part: 0.1 t;
- anything else: 1 t.

**What a crane lifts** is the mass plus its own rigging:
- the hook block and slings, 2 % of the load *(proposed)*;
- at least 0.5 t.

## The items and the site marker
- **Survey tool** (`wildcatter:survey_tool`):
  - crafted from a compass, a spyglass and paper;
  - right-click a block to set the first corner and another for the second; the readout gives the box's size;
  - with an empty blueprint in the off hand, a third right-click records everything in the box onto it;
  - at most 128 x 96 x 128 blocks; past that it reads TOO BIG;
  - sneak-right-click clears the corners.
- **Blueprint** (`wildcatter:blueprint`):
  - crafted from paper and blue dye;
  - a new one is blank: survey onto it, or sneak-right-click in the air to step it through the library;
  - a surveyed one does not step; survey again to replace what it holds;
  - **its tooltip:**
    - its name ("Potline (library)", or "Surveyed: 111 x 15 x 85");
    - its parts by kind;
    - its blocks;
    - its heaviest lift ("heaviest: Reactor vessel, 180 t");
    - its lift count;
  - a surveyed blueprint is a stack of 1 and carries its data on the stack (a `blueprint` component, compressed). A library one
    carries only its name.
- **The ghost** (`client/BlueprintGhost`):
  - holding a blueprint, you see it drawn where it would stand: its origin at the block you look at, turned to face the way you
    face;
  - ordinary blocks are drawn as translucent cubes in the block's map colour, within 32 m of you, up to 4,096 of them;
  - parts are drawn as outline boxes, green where they fit and red where something is in the way, as a single part's placement
    ghost is.
- **Site marker** (`wildcatter:site_marker`):
  - right-click with the blueprint (not sneaking) places a site marker at the origin;
  - the blueprint stays in your hand, and the marker holds a copy;
  - the marker draws the ghost for every player within 128 m of the site's box until each block and part of it stands;
  - its readout: "Site: Potline, 312 lifts, 41 done; next: Pot (60 t) at 66, 64, -6313";
  - breaking the marker drops it and leaves what is built.
  - **its mode (10c2):** sneak-right-click switches it between BUILD and DISMANTLE (decided with Tim, 2026-11-04). Every crane
    working the site follows it. In DISMANTLE the cranes take its lifts down, last first, and the readout counts what still stands:
    "Site: Potline, DISMANTLE, 312 lifts, 270 still stand; next: Pot (60 t) at 66, 64, -6313", then "cleared" when none stand.

## The library
- **Where it lives:** `common/src/main/resources/assets/wildcatter/blueprints/<name>.json`, read from the jar, so both the server and
  the client have it.
- **How it is made:** from the game test scenes, not by hand. With `WILDCATTER_EXPORT_BLUEPRINTS=<dir>` set:
  - each scene below records its box with the survey tool's code once its plant is built, before its fault checks;
  - it writes the JSON to that folder;
  - the files are committed;
  - a scene's test-only extras are recorded with it (the hoppers that catch its output, its fuel tanks). They are part of a working
    plant.
- **The plants and their scenes** (the box is the one each scene clears: from y up to its top, x and z as listed):

| Blueprint | Scene | Box (x; height above ground; z from the scene's z) |
|---|---|---|
| `steam_plant` | `steamPlant` | 0-24; 9; z-6 to z+9 |
| `genset` | `gensets` | 4-22; 5; z-10 to z+4 |
| `combined_cycle` | `gasTurbine` | 2-36; 13; z-8 to z+27 |
| `aeroderivative` | `peaker` | 2-44; 17; z-14 to z+22 |
| `substation` | `substationParts` | 0-40; 10; z-16 to z+12 |
| `land_rig` | `landRig` | 0-47; 7; -452 to -424 |
| `lease` | `lease` | 4-46; 13; z-10 to z+16 |
| `gas_plant` | `gasPlant` | 4-50; 14; z-12 to z+8 |
| `crude_unit` | `crudeUnit` | 2-34; 15; z-10 to z+26 |
| `cracker` | `cracker` | 0-46; 17; z-10 to z+10 |
| `acid_plant` | `acidPlant` | -2-40; 23; z-12 to z+10 |
| `uranium_mill` | `uraniumMill` | -20-62; 12; z-14 to z+13 |
| `conversion` | `conversion` | -4-42; 13; z-11 to z+27 |
| `enrichment` | `cascade` | -30-44; 17; z-40 to z+22 |
| `zirconium` | `zirconium` | -20-47; 15; z-12 to z+52 |
| `fuel_plant` | `fuel` | -20-24; 15; z-12 to z+66 |
| `boron` | `boron` | -20-16; 11; z-12 to z+24 |
| `pwr` | `reactor` | -26-20; 16; z-20 to z+20 |
| `turbine_island` | `turbineIsland` | -10-80; 21; z-20 to z+40 |
| `cooling_tower` | `coolingTower` | -35-40; 11; z-70 to z+5 |
| `bayer_refinery` | `bayer` | -27-44; 25; z-16 to z+80 |
| `alumina_plant` | `alf3Lime` | -20-40; 17; z-16 to z+24 |
| `coker` | `coker` | -27-30; 25; z-16 to z+50 |
| `coke_ovens` | `cokeOvens` | -34-30; 25; z-16 to z+40 |
| `anode_plant` | `anodes` | -27-30; 17; z-22 to z+32 |
| `potline` | `potline` | 0-110; 15; z-50 to z+34 |

- **Left out: the ISR wellfield.** Its wells must stand on the roll front the world gave it, so it is not a plant to copy.
- **The cooling tower:** its blueprint is the tower's controller and its hopper. The controller pours the shell itself, as it does
  when placed by hand (cooling-tower.md).
- **The PWR:** its blueprint is the nuclear island as its scene builds it. Its fuel (the assemblies) is not in it; it is loaded by
  hand as now.

## Rotation, placing, removal
- **Rotation:** a blueprint is built facing the way it was placed.
  - Its blocks turn with it (a log's axis, a stair's facing).
  - Its parts turn their facing, as a hand-placed part does.
- **Placing by hand:** you may build a site by hand. The marker counts what stands, and each block or part that matches is done. A block matches when it is the same block set the same way; the sides a cable, wire,
  fence or pane joins to are not compared, since it redraws them from its new neighbours.
- **Changes:** a block in the way is not broken; the marker's readout names it ("IN THE WAY: minecraft:oak_log at 12, 64, 30").

## Sharing and tandem lifts (10c3, decided with Tim 2026-11-04)
Several cranes may work one site: every crane in reach of a marker takes its lifts from it. This follows a real project's lift
plan, where each lift is given to the crane whose chart holds it, and the heaviest go up in tandem.

**Who takes which lift:**
- No two cranes ever take the same lift: the marker keeps a claim on each lift a crane has on its hook.
- **Working ahead:** a crane takes the earliest lift it can make, and may pass over one it cannot (too heavy, out of reach, too
  high, or not in its chest). It may not take a lift that **rests on** a lift that is not yet set, or that is on another crane's
  hook.
  - **Rests on:** lift A rests on an earlier lift B when their x-z boxes overlap and A's bottom is above B's bottom.
- **Dismantling, the same backward:** a crane takes the last lift that still stands. It passes over one it cannot take. It may not
  take a lift while a later lift resting on it still stands or is on a hook.
- So the heavy lifts wait for the crane whose chart holds them, and a lighter crane keeps building around them.
- **When a crane has nothing:** it reads the first lift it could not make and why, as one crane alone does.
- **The marker's readout** adds the cranes working it: "Site: Potline, 312 lifts, 41 done, worked by 2 cranes; next: ...".

**Tandem lifts:**
- **When:** a part is too heavy for one crane alone at its radius.
- **The partner:** the crane that found the lift (the lead) looks for a second crane on the same site. The second crane must be
  idle and have power or diesel.
- **The lead's chest:** the part comes from the lead's chest. Both cranes must reach that pick and the part's place, and both must
  be tall enough.
- **Equal halves:** each crane is hooked at one end of the part, and the part's centre is its middle, so each carries half of what
  is on the hook, plus its own hook block (the ring crane's 104.6 t; the others' is in the 2 %).
- **25 % to spare:** each half may be at most 80 % of that crane's chart, read at the farther of its pick and set radii. This is
  the usual tandem derating.
- **Together:** both cranes run the same steps at once, and each step takes the longer of the two cranes' times, so they slew,
  luff, trolley and hoist at the slower crane's speed.
  - The hooks are at the part's two ends, along the line between the two cranes' centres.
  - If either crane loses power or diesel, both hold.
  - If either is broken, or the world is saved mid-lift, the lift is called off and the part goes back to the lead's chest.
- **Readouts:**
  - Lead: "TANDEM with the Crawler crane at 80, 64, -7190: 45.9 of 91.8 t, chart 89.5 t at 16 m, 80 % 71.6 t".
  - Follower: "TANDEM with the Crawler crane at ...: SLEWING ...".
  - No partner: "TOO HEAVY: GSU 60 (90 t), 91.8 t on the hook, chart 89.5 t at 16 m; no second crane in reach can take half with
    25 % to spare".

**Game test (`heavyLift`, part 2):**
- Two crawlers, both on their 32 m booms, share one site. The site holds a GSU 60 about 16 m from each and three pallets of stone
  bricks.
- Alone, the GSU reads TOO HEAVY (91.8 t against 89.5 t). In tandem each crane takes 45.9 t, under its 71.6 t.
- The site is built. The GSU goes up in tandem, each crawler sets at least one lift, and no lift is set twice.
- Screenshot: the tandem lift.

## Left out (Known gaps)
- Contents and states are not recorded.
- A blueprint cannot be edited; survey it again.
- Entities (item frames, armour stands, mobs) are not recorded.

## Game test
`blueprints` (z near -6500):
1. A small plant is built and surveyed onto a blueprint: a diesel genset, a unit substation, cable and a few stone-brick blocks. The
   blueprint must hold each block and the two parts, with their facings and the cable's size.
2. Placed facing east 40 m away, its site marker's lift list is checked: the parts come before the blocks on their layer, and the
   pallets hold at most 64 blocks.
3. The parts are built there by hand through the marker's list. The marker must then read the site done.
4. Every library blueprint loads, and every part id and block in it is known.
- **Screenshot:** the ghost of a library blueprint (the potline) at its site.
