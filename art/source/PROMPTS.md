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
