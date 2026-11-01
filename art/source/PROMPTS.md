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
