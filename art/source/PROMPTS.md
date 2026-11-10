# Ludo prompts

The prompts behind the Ludo pictures in this folder, so a picture can be remade or a new one matched. Earlier pictures
(Phases 3 to 4.5) were made before this record was kept.

Ludo `createImage`, model klimt. `tools/process_materials.py` turns each picture into a 32 px texture; its CROP and PATTERN
tables say which part of the picture is used.

## Phase 6c (2026-11-01)
Materials: image_type `texture`, art_style `Photorealistic 3D`, 2 variations each, the better one kept.

| File | Prompt |
|---|---|
| mat_enclosure_louvre | Seamless flat texture, straight-on orthographic view, even lighting: industrial gas turbine acoustic enclosure wall panel, light grey painted standing-seam steel sheeting with vertical seams every 30 cm and one horizontal band of louvred ventilation slats, clean, no text, no logos |
| mat_filter_house | Seamless flat texture, straight-on orthographic view, even lighting: face of a gas turbine air inlet filter house, a grid of grey pleated cylindrical filter cartridges set in a light grey painted steel frame, small angled weather hoods over each row, no text, no logos |
| mat_radiator_fins | Seamless flat texture, straight-on orthographic view, even lighting: power transformer cooling radiator panel, closely spaced vertical pressed-steel fins painted medium blue-grey (ANSI 70 grey), slight shadows between fins, no text (drawn too dark: brightened and desaturated in processing) |
| mat_grating | Seamless flat texture, top-down orthographic view, even lighting: galvanized steel bar grating walkway, parallel load bars with cross rods forming small rectangular openings, dull zinc grey, dark gaps between bars |
| mat_finned_tube | Seamless flat texture, straight-on orthographic view, even lighting: air-cooled heat exchanger finned tube bundle, rows of horizontal tubes covered in thin closely spaced aluminium fins, silver, seen from the side, no text |
| mat_hitemp_aluminium | Seamless flat texture, straight-on orthographic view, even lighting: steel plate painted with silver high-temperature aluminium heat-resistant paint, slightly weathered with faint heat discoloration, plain, no seams, no text |

Decals. The `texture` image type repeats a sign across the picture, so the three below are cropped to one copy:

| File | Prompt |
|---|---|
| decal_annunciator | Front view, flat, filling the frame: industrial gas turbine control panel door, a small dark operator screen showing a green turbine diagram at the top, below it a grid of 4 by 4 square backlit alarm annunciator windows (some lit amber, most off), a few round pushbuttons at the bottom, grey painted steel, generic, no brand names, no logos |
| decal_arc_flash | Flat front view filling the frame: an ANSI Z535 style arc flash warning label sticker, orange WARNING header band with the black triangle exclamation symbol, white body with a few lines of small black text about arc flash and shock hazard and required PPE, generic, no company names |
| decal_nfpa | Flat front view filling the frame: an NFPA 704 hazard diamond placard, a square rotated 45 degrees divided into four smaller diamonds, blue on the left with the number 1, red on top with the number 4, yellow on the right with the number 0, white at the bottom blank, thick black outlines, on a white sign plate |

Single signs: image_type `asset`. With `texture` the text came out garbled.

| File | Prompt |
|---|---|
| decal_flammable | A single flat rectangular industrial safety sign seen straight on, filling the whole frame edge to edge, no background: red top band with the white word DANGER, white lower body with the bold black words FLAMMABLE GAS on one line and NO SMOKING on a second line, large simple lettering, OSHA style |
| decal_rating_plate | A single brushed stainless steel equipment rating nameplate seen straight on, filling the whole frame edge to edge, no background: four round rivets in the corners, a black engraved title bar at the top, below it a neat ruled table of six rows of small black engraved text and numbers, plain and generic with no brand name or logo |
| decal_pipe_crude (and _gas, _lpg, _water, _diesel, _steam) | A single industrial pipe marker label seen straight on, filling the whole frame edge to edge, no background: a wide yellow rectangle with bold black text CRUDE OIL and a black arrow pointing right, ASME A13.1 style. The others use NATURAL GAS, LPG and STEAM (black on yellow), PRODUCED WATER (white on green) and DIESEL (white on brown) |

## Phase 7a (2026-11-01): the ores (art/source/ore/)
Ore blocks (`<key>_ore.webp`): image_type `texture`, art_style `Photorealistic 3D`, 2 variations, the better kept. Each prompt is
"Seamless flat texture, straight-on orthographic view, even lighting: rough mined rock face of <host> with <mineral>, natural
rock, no text". `tools/process_materials.py` shrinks the whole picture to 32 px and makes it tile.

| Key | Host and mineral in the prompt |
|---|---|
| uraninite | pale tan arkose sandstone, scattered black uraninite grains, patchy yellow-green uranium oxide staining |
| pitchblende | dark grey basement rock, large black glossy botryoidal pitchblende, brick-red hematite alteration |
| sulfur | pale grey-white calcite caprock, many bright lemon-yellow native sulfur crystals and crusts |
| halite | rock salt wall in a salt mine, translucent white to pale grey halite, cubic faces, faint grey banding |
| sylvite | sylvinite potash, brick-red and orange sylvite with translucent white halite |
| fluorite | grey limestone cut by veins of purple and honey-yellow cubic fluorite |
| heavy_mineral | (second try) dark heavy mineral beach sand, mostly black ilmenite with bold pale quartz streaks and red garnet, top-down |
| bauxite | reddish-brown lateritic bauxite, pea-sized pisolites in orange-tan clay |
| galena | grey dolostone, silvery lead-grey cubic galena |
| sphalerite | tan dolostone, resinous dark brown to black sphalerite with amber glints |
| cassiterite | light grey granite, white quartz veins with dark brown-black cassiterite |
| pentlandite | dark grey norite, brassy bronze nickel sulfide (pentlandite and pyrrhotite) |
| chromite | banded chromitite, granular jet-black chromite with thin pale grey-green pyroxene layers |
| platinum | coarse grey-brown feldspathic pyroxenite reef, thin black chromitite stringers, fine brassy sulfide specks |
| molybdenite | pale pinkish granite porphyry, quartz veinlet stockwork lined with bluish-silver molybdenite |
| magnesite | dense chalky white magnesite, porcelain-like cauliflower texture |
| dolomite | pale buff-pink sugary crystalline dolomite, small vugs |
| graphite | grey mica schist full of shiny black graphite flakes along the foliation |
| borax | (second try) borate ore face, chunky white and glassy borax and kernite masses in dark grey-green clay seams |
| spodumene | coarse pegmatite, pale greenish-white spodumene laths, white feldspar, grey quartz, silver muscovite |
| barite | grey chert and shale with thick white to cream tabular barite |
| bentonite | freshly cut waxy pale olive-cream bentonite clay with popcorn drying cracks |
| acanthite | white quartz vein in grey andesite, dark lead-grey acanthite and wiry native silver |
| scheelite | green and brown garnet-pyroxene skarn, pale cream to honey scheelite |
| cobaltite | dark grey rock, silvery pinkish-white cobaltite, bright pink-purple erythrite bloom |
| pyrolusite | sooty black and steel-grey pyrolusite with fern-like dendrites on brown banded iron rock |
| phosphate | grey-brown pelletal phosphorite, dark phosphate pellets and pale fossil shell fragments |

Minerals, ingots and the scintillometer (`<key>.webp`): image_type `item-icon`, art_style `Photorealistic 3D`, 2 variations.
Each is "A single <lump / cluster / crystal> of <the mineral as above>, mineral specimen, game item icon, plain white (or dark
grey for white minerals) background". Heavy minerals are "a small canvas sample bag spilling dark heavy mineral sand". The
ingots are "a single cast <lead / tin / zinc slab / silver bullion> ingot". The scintillometer is "a yellow handheld gamma-ray
scintillometer survey meter with a carrying handle, a cylindrical detector probe on the side and a small analog dial, generic,
no brand names".

## Phase 7b (2026-11-01): the mill's items (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations, the better kept:
- `magnesia`: "A single lump of white caustic-calcined magnesia (MgO) powder cake, chalky, slightly porous, mineral sample, game
  item icon, dark grey background" (a).
- `empty_drum`: (second try; the first came out squat like a bass drum) "A single tall empty 55 gallon industrial steel oil drum
  (barrel), taller than it is wide (0.88 m tall, 0.57 m across), painted dark blue, two raised rolling hoops around the middle,
  closed lid with bolt ring, standing upright, game item icon, plain white background, no text" (a).
- `yellowcake_drum`: "A single sealed 55 gallon steel drum of uranium ore concentrate (yellowcake), lid open slightly showing
  bright yellow powder, black and yellow radioactive trefoil label on the side, game item icon, plain white background, no other
  text" (b).

## Phase 7c (2026-11-01): the submersible pump (art/source/ore/)
`submersible_pump`: `item-icon`, `Photorealistic 3D`, 2 variations, a kept: "A single 4 inch stainless steel electric submersible
water well pump, long slim cylinder with the motor section below the pump stages and intake screen, a flat black power cable
coming off the top, lying diagonally, game item icon, plain white background, no text".

## Phase 7d (2026-11-01): the 48Y cylinders (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations, the side views (b) kept so the pair match:
- `empty_48y_cylinder`: "A single empty 48Y uranium hexafluoride shipping cylinder: a long horizontal 48 inch steel pressure
  cylinder with domed ends, painted grey-white, two stiffening rings, a small valve protector at one end, no markings, game item
  icon, plain white background, no text".
- `uf6_cylinder_48y`: the same, "full of UF6 ... a yellow and black radioactive trefoil label and a white UN 2978 placard with a
  black 7".

## Phase 7e (2026-11-01): the cascade's cylinders (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations each:
- `empty_30b_cylinder` (a kept): "A single empty 30B uranium hexafluoride shipping cylinder seen from the side: a short horizontal 30
  inch steel pressure cylinder with domed ends, painted grey-white, two stiffening skirts at the ends, a small valve at one end, no
  markings, game item icon, plain white background, no text".
- `leu_cylinder_30b` (b kept): the same, "full ... a yellow and black radioactive trefoil label and a white UN 3507 placard with a
  black 7, a green stripe for enriched".
- `tails_cylinder_48y` (a kept): "A single weathered 48Y depleted uranium hexafluoride storage cylinder seen from the side ... dull
  dark grey paint with some rust streaks, two stiffening rings, a small valve protector at one end, a white UN 2978 placard with a
  black 7".

## Phase 7f (2026-11-01): the zirconium tubes (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations each, a kept:
- `zircaloy_tube`: "A small bundle of three long thin seamless metal tubes, dull silver-grey zirconium alloy nuclear fuel
  cladding, open ends showing thin walls, lying diagonally, game item icon, plain white background, no text".
- `guide_thimble`: "A single long thin metal guide tube for a nuclear fuel assembly, dull silver-grey zirconium alloy, slightly
  wider than a pencil, with a narrower dashpot section near the lower end and a threaded end plug, lying diagonally, game item
  icon, plain white background, no text".

## Phase 7g (2026-11-01): the fuel plant's items (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations each:
- `spacer_grid` (b kept): "A single square nuclear fuel assembly spacer grid seen at an angle: an egg-crate lattice of thin
  interlocking dull silver zirconium alloy strips forming a 17 by 17 grid of square cells, with small springs and mixing vanes,
  game item icon, plain white background, no text".
- `helium_cylinder` (a kept): "A single tall industrial compressed gas cylinder for helium, painted brown with a steel valve and a
  protective cap removed, standing upright, slightly angled, game item icon, plain white background, no text".
- `empty_helium_cylinder` (a kept): "A single tall industrial compressed gas cylinder, plain grey steel, empty, with a closed valve
  and a steel protective cap screwed on, standing upright, slightly angled, game item icon, plain white background, no text".
The fuel assembly's item is its block model.

## Phase 8a (2026-11-01): the boron plant's items (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations each:
- `boric_acid` (a kept): "A single white woven polypropylene 25 kg industrial chemical sack, filled and sealed, slightly angled,
  plain with no printing, game item icon, plain white background, no text".
- `b4c_pellets` (b kept): "A small shallow stainless steel tray holding many small dark grey matte cylindrical ceramic boron carbide
  pellets in neat rows, seen at an angle, game item icon, plain white background, no text".
- `rcca` (a kept): "A nuclear reactor rod cluster control assembly: a small stainless steel spider hub with radial vanes at the top
  holding a cluster of 24 long thin parallel silver rods hanging down, seen diagonally, game item icon, plain white background, no text".

## Phase 8e (2026-11-02): the dosimeter (art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations:
- `dosimeter` (a kept): "A single small handheld electronic personal radiation dosimeter: a compact rugged yellow and dark grey
  plastic device with a small LCD display showing digits, a pocket clip and a radiation trefoil symbol, seen at a slight angle, game
  item icon, plain white background, no text".

## Launcher backgrounds (2026-11-02)
Ludo `createImage`, image_type `splash`, art_style `Voxel Art`, aspect 16:9, 2 variations each (sources in `art/source/launcher/`,
the picks upscaled with `upscaleImage` and cropped to 1920x1080 and 2560x1440 in `art/final/launcher/` and `dist/launcher/`).
Every prompt opens with "Wide cinematic landscape in a blocky voxel world (cube-built terrain like a block sandbox game):" and ends
with "no text, no logos, no people".
- **cooling_tower** (pick `tower_a`): a giant hyperbolic natural-draft concrete cooling tower releasing a tall white steam plume at
  golden hour, beside a pressurized water reactor plant with a domed containment and turbine hall, high-voltage transmission towers
  across rolling green hills, warm sunset sky.
- **oilfield** (pick `oilfield_a`): a West Texas style oil field at dusk, a tall steel drilling rig derrick lit with work lights,
  several pumpjacks nodding in a row, storage tanks and a gas flare burning orange, desert scrub and red mesas, purple and orange sky.
- **grid** (pick `grid_b`): an electrical substation at blue hour with large power transformers, steel gantries, insulators and
  circuit breakers, wooden utility poles with crossarms carrying wires along a country road to a small village with warm lit
  windows, a small brick steam power plant with a smokestack in the distance.
- **refinery** (pick `refinery_a`): an oil refinery and gas plant at night, tall distillation columns, pipe racks and spherical
  storage tanks lit by hundreds of small lights, a flare stack burning, a gas turbine power plant with exhaust stacks beside it,
  reflections on a calm river in the foreground, starry dark blue sky.
- **More oil fields** (Tim's son's favourite, 2026-11-02), same opening and ending:
  - **oilfield_night** (pick `oil_night_b`): a tall steel drilling rig derrick at night lit up with bright work lights from top to
    bottom, a gas flare burning orange beside it lighting the ground, pumpjacks silhouetted nearby, storage tanks, a starry dark
    blue sky with a crescent moon over desert mesas.
  - **oilfield_sunrise** (pick `oil_sunrise_a`): a long row of oil pumpjacks nodding across golden prairie grass at sunrise, a
    wooden lease road with steel pipelines alongside, oil storage tanks and a separator in the middle distance, a windmill, long
    shadows and a bright orange sun on the horizon.
  - **oilfield_rig** (pick `oil_rigfloor_a`): a low-angle view looking up at a huge land drilling rig from its rig floor, the steel
    lattice mast towering into a dramatic sunset sky, drill pipe stands racked in the derrick, the drawworks and rotary table in
    the foreground, mud tanks and a diesel engine house beside it.
  - The FTB instance picture is a 256x256 crop of **oilfield** (`wildcatter_oilfield_square.png`).

## Phase 10d4 (2026-11-04): the grid gear's art, and the six sounds that had no kept source
Ludo `createImage`, model klimt, 2 variations each, the better one kept (the `_1`/`_2` pick in brackets). `tools/process_art.py`
(the 10d4 section) turns them into the wire, ground, breaker, fuse and cable textures and the item icons.

Materials: image_type `texture`, art_style `Photorealistic 3D`; each prompt opens "Seamless flat texture, straight-on orthographic
view, even lighting:" and ends "no text, no logos".

| File | Prompt (after the opening) |
|---|---|
| wire_copper (1) | close-up of bare stranded copper electrical conductor, many twisted bright copper strands laid side by side in a helical lay running horizontally across the frame, warm orange-copper metal with slight oxidation, no insulation |
| wire_aluminium (2) | close-up of an overhead power line ACSR conductor, bright dull-silver aluminium strands twisted in a helical lay running horizontally across the frame, slightly weathered matte aluminium |
| wire_steel (2) | close-up of galvanized steel stranded wire, grey zinc-coated steel strands twisted in a tight helical lay running horizontally across the frame, dull grey with faint spangle |
| wire_wool (2) | close-up of old cloth-insulated electrical wire, tightly braided off-white cotton and wool yarn insulation in a diagonal woven braid pattern, slightly fuzzy fibres, light cream colour |
| wire_pvc (1) | close-up of the smooth white PVC plastic jacket of an electrical cable, slightly glossy, faint lengthwise extrusion lines running horizontally, very subtle surface scuffs, plain white, no printing |
| ground_rod (1) | close-up of the surface of a copper-clad steel ground rod, smooth reddish copper plating with faint lengthwise drawing marks running vertically and a little brown tarnish |

Gear faces: image_type `asset`, `Photorealistic 3D`.

| File | Prompt |
|---|---|
| breaker (1) | A single small molded-case single-pole circuit breaker seen straight on from the front, filling the whole frame edge to edge, no background: dark charcoal grey moulded plastic body, a rectangular toggle slot in the middle, a small white rating label below it, two mounting screws, plain and generic, no brand name, no logo (cropped to the breaker in processing) |
| fuse_cartridge (2) | A single cylindrical cartridge fuse lying horizontally seen straight on from the side, filling the whole frame edge to edge, no background: a tan brown vulcanized fibre tube in the middle with two shiny brass end caps, plain, generic, no text, no brand name, no logo (the tube and a cap are cropped from it) |

Item icons: image_type `item-icon`, `Photorealistic 3D`; each ends "three-quarter view, centred, plain white background, generic, no
brand name, no logo".

| File | Prompt |
|---|---|
| branch_breaker (1) | A single plug-on single-pole residential circuit breaker, dark grey moulded plastic, black toggle handle on top, small white rating label |
| gfci_breaker (2) | A single plug-on single-pole ground-fault (GFCI) circuit breaker, white and light grey moulded plastic body, black toggle handle, a small yellow TEST push button and a coiled white neutral pigtail wire |
| panelboard_breaker (1) | A single three-pole bolt-on industrial molded-case circuit breaker, black moulded body three poles wide with one wide ganged toggle handle, brass terminal screws at the bottom, small rating label |
| fuse_class_j (1) | A single class J time-delay cartridge fuse, a short cream-coloured fibre tube with two flat silver blade terminals at the ends, a small printed band |
| fuse_e (2) | A single medium-voltage current-limiting power fuse, a long white glass-reinforced tube with silver metal end ferrules and a small red blown-fuse indicator pin at one end |
| grounding_set (2) | A lineman's protective grounding clamp set: three heavy aluminium C-clamps with T-handle screws joined by clear-jacketed copper ground cables to a single ground clamp, coiled together |

Sounds (`art/sound_source/`): Ludo `createSoundEffect`, one take each; `tools/process_sounds.py` makes the `.ogg`.

| File | Loop | Duration | Prompt |
|---|---|---|---|
| breaker_click.mp3 | no | 0.6 s | a small molded-case circuit breaker toggled by hand, one crisp plastic snap click |
| breaker_trip.mp3 | no | 0.8 s | an electrical circuit breaker tripping on overload, a sharp spring-loaded mechanical snap followed by a dull plastic thunk |
| boiler_hiss.wav | yes | 4 s | steady hiss of pressurised steam escaping from a boiler safety valve, constant, no music |
| turbine_hum.wav | yes | 4 s | a steam turbine generator running at speed, low steady mechanical hum with a faint high whine, constant, industrial, no music |
| zap.mp3 | no | 0.8 s | a short electric shock, a sharp crackling buzz of electricity arcing for a moment |
| structure_formed.mp3 | no | 1.5 s | heavy steel machine parts seating into place, a solid metallic clunk followed by a short settling rattle and a low resonant ring |


## Art wave (2026-11-09): the last placeholders (art/source/ore/ and art/source/)
Tim, 2026-11-09: "close all known gaps". Ludo `createImage`, model klimt, art_style `Photorealistic 3D`, 2 variations each, the better
kept (the letter after each name). Icons are `item-icon` (Ludo returns them on transparency) and go through `tools/process_art.py`'s
`icon()` to 32 px; blocks are `texture` and go through `tools/process_materials.py` (whole picture to a 32 px tile).

Bulk bags (`<item>_bag`, all a): "A single full white woven polypropylene FIBC bulk bag (one tonne super sack) with four lifting loops
and a broad <colour> printed band, top spout open showing <contents>, standing upright, game item icon, plain dark grey background, no
text". The band tells the bags apart in the inventory:

| Item | Band | Contents |
|---|---|---|
| alumina_bag | blue | fine bright white alumina powder |
| hydrate_bag | green | damp cream-coloured aluminium hydroxide powder |
| alf3_bag | purple | fine pale grey aluminium fluoride powder |
| quicklime_bag | yellow | chalky white quicklime pebbles |
| reacted_alumina_bag | brown | light grey-tan reacted alumina powder |
| calcined_coke_bag | black | granular black calcined petroleum coke (plain white background) |

The other items, "..., game item icon, plain white background, no text" unless a dark ground is noted:

| File | Prompt |
|---|---|
| empty_bulk_bag (a) | A single empty white woven polypropylene FIBC bulk bag (one tonne super sack) folded flat, four lifting loops at the corners, lying diagonally (dark grey background) |
| empty_ton_container (b) | A single empty one-ton chlorine ton container seen from the side: a long horizontal steel cylinder with concave dished ends and rolling hoops, painted plain silver grey, a domed valve protection hood at one end, no markings |
| chlorine_ton_container (a) | the same, full, "painted yellow-green ... a green and white diamond gas placard" |
| green_petroleum_coke (a) | A small heap of lumpy dull black green petroleum coke, porous sponge coke chunks |
| metallurgical_coke (a) | A single fist-sized lump of silver-grey metallurgical coke, hard, porous and cellular with a slight metallic sheen |
| green_anode (b) | A single rectangular unbaked green carbon anode block for aluminium smelting, matte black pitch and coke, three round stub holes in its top face, seen from above at an angle |
| baked_anode (a) | A single rectangular baked carbon anode block for aluminium smelting, hard dark charcoal grey with a dusty surface and a little packing coke stuck on, three round stub holes in its top face, seen from above at an angle |
| rodded_anode (a) | A rodded anode for an aluminium smelting pot: a dark grey baked carbon block with a tall square aluminium rod standing on it, joined by a three-stub steel yoke cast into the top of the block |
| spent_anode (b) | A spent anode from an aluminium smelting pot: a thin worn eroded butt of dark grey carbon crusted with white frozen bath, still held by its three-stub steel yoke under a tall square aluminium rod |
| crushed_butts (a) | A small heap of crushed carbon anode butts, angular dark grey-black carbon pieces with a few flecks of white bath crust |
| anode_rod (a) | A single anode rod assembly for aluminium smelting, lying diagonally: a long square-section aluminium rod ending in a steel yoke with three short round steel stubs |
| aluminium_ingot (a) | A single cast primary aluminium ingot, a trapezoid bar with sloped sides, bright satin silver with a slightly rippled cast top surface (dark grey background) |
| spent_pot_lining (a) | A small heap of broken spent pot lining from an aluminium smelter: black carbon cathode block fragments mixed with pale grey refractory brick pieces and white fluoride crust |
| tapping_crucible (a) | A single aluminium smelter tapping crucible: a squat round steel ladle painted dark grey with a bolted lid, a long angled siphon tube spout on the lid and two lifting trunnions on its sides |
| blueprint (a) | A single engineering blueprint sheet, half unrolled from a roll, deep blue paper with crisp white line drawings of an industrial plant layout and a title block, no readable text |
| survey_tool (a) | A single construction surveyor's total station instrument without its tripod: a yellow body with a dark telescope on a rotating yoke, a small screen and keypad, generic, no brand names |
| limestone (a) | A single lump of pale grey fine-grained limestone with a few tiny fossil shell fragments, mineral specimen (dark grey background) |
| memory_card (a) | A single industrial PLC memory card: a small black plastic card with gold edge contacts and a blank white label, lying diagonally, generic, no brand names |

Blocks, `texture`, each "Seamless flat texture, <straight-on / top-down> orthographic view, even lighting: ..., no text":

| File | Prompt |
|---|---|
| ore/red_mud (a) | pressed red mud filter cake from bauxite refining, brick-red to rust-orange fine clay, slightly damp, faint cloth marks and fine drying cracks |
| ore/aluminium_block (a) | face of a solid block of cast aluminium, satin bright silver metal with faint cast grain and a few machining marks, plain, no seams |
| ore/site_marker (a) | fluorescent orange surveyor's marking flag cloth, fine woven nylon fabric with soft creases |
| ore/limestone_ore (b) | rough mined rock face of pale grey bedded limestone, faint horizontal bedding, scattered small fossil shell fragments, natural rock |
| cell_liner (a) | black textured HDPE geomembrane liner of a mine tailings cell, slightly glossy pebbled surface, one straight welded seam, a little dust |
| settled_tails (b) | settled mine tailings beach, fine light grey silt and sand, damp, with shallow drying cracks and faint ripple marks |
| decant_tower (b) | wall of a tailings dam decant tower, weathered grey precast concrete riser with a vertical steel channel slot holding stacked dark timber stop-logs, water stains |
| mat_paint_crane_yellow (b) | steel plate painted bright crane yellow, slightly weathered gloss enamel with a few small scratches and faint dirt, plain, no seams |
| rotor (b) | dark grey painted machined steel of a fan hub and shaft, slightly worn paint with faint circular machining marks, plain (`tools/process_art.py` paints the yellow stripe across it, as the placeholder had, so the turning shows) |

## Misc gaps wave: the conductor spools (2026-11-09, art/source/ore/)
`createImage`, `item-icon`, `Photorealistic 3D`, model klimt, 2 variations each, the better kept; `tools/make_assets.py` lays them over
the drawn icons. "A single small wooden cable reel (drum) with round plywood flanges, tightly wound with <conductor>, <end>, seen at an
angle, generic, no brand, game item icon, plain <white | dark grey> background, no text":

| File | Conductor and end |
|---|---|
| conductor_spool_lv (a) | bare bright orange-copper stranded conductor wire, a loose end of the copper strands sticking out |
| conductor_spool_mv (a) | bare dull bronze-tan weathered stranded overhead line conductor, a loose end of the strands sticking out |
| conductor_spool_dist (b) | bare pale silver-grey stranded aluminium overhead line conductor, a loose end of the strands sticking out |
| conductor_spool_hv (a) | thick bare steel-grey ACSR transmission line conductor, a loose end showing the darker steel core strands |
| covered_conductor_spool_lv (a) | white plastic-covered insulated conductor, the cut end showing bright orange copper strands (dark ground) |
| covered_conductor_spool_mv (a) | white polyethylene-covered overhead conductor, the cut end showing dull bronze-tan strands (dark ground) |
| covered_conductor_spool_dist (a) | white polyethylene-covered overhead conductor, the cut end showing pale silver aluminium strands (dark ground) |
| covered_conductor_spool_hv (b) | thick white polyethylene-covered transmission conductor, the cut end showing aluminium and steel core strands (dark ground) |

## Art wave, Phase 14's items (2026-11-09, art/source/ore/)
`item-icon`, `Photorealistic 3D`, 2 variations each, "..., generic, no brand, game item icon, plain dark grey background, no text":
- `gauze_pack` (b): "A small stack of round woven fine platinum-rhodium wire gauze pads, catalyst gauze for nitric acid, shiny silver metal
  mesh discs, slightly fanned".
- `emulsifier_pail` (a): "A single 20 kg white plastic pail with a tight lid and a wire handle, holding emulsifier for explosives, a plain amber
  band label with no writing".
