# Art wave (2026-11-09): decisions taken while Tim was away

Tim (2026-11-09): "close all known gaps". Every `*_placeholder` texture Wildcatter still used now comes from a Ludo picture
(`art/source/PROMPTS.md`, "Art wave"). Format as in `README.md` here.

### 1. Which Ludo style for the new art?
- **Chosen:** `Photorealistic 3D`, model klimt, box-filtered down to the mod's 32 px, as every earlier Wildcatter picture was. Items
  are `item-icon` (cut out by Ludo), blocks `texture` (seamless).
- **Why:** the new icons sit in the same inventory as Phase 7's minerals, ingots and cylinders, and the new blocks beside the ores and
  the materials. A pixel-art style would make them the only flat-shaded things in the mod.
- **Alternatives:** Ludo's `16-Bit Pixel` or `Free Pixel` reads crisper at 32 px, but would want the whole mod redone to match.

### 2. How are the six bulk bags told apart?
- **Chosen:** each white FIBC bag has a broad printed band in its own colour (alumina blue, hydrate green, AlF3 purple, quicklime
  yellow, reacted alumina brown, calcined coke black), and its open spout shows the contents.
- **Why:** real bulk bags of white powders look alike; plants mark them with printed bands and labels. At 32 px the band is the
  only thing that reads.
- **Alternatives:** different bag colours (real bags are nearly all white), or text labels (unreadable at 32 px).

### 3. Where do the new pictures live?
- **Chosen:** the items and the three plain blocks of 10b (red mud, aluminium block, site marker flag) in `art/source/ore/`, where
  `tools/make_assets.py` already looked for them (`ORE_ART`), so no item or block code changed; the lined cell's three blocks and the
  rotor paint in `art/source/`; the crane paint as a material, `mat_paint_crane_yellow`.
- **Why:** each was already wired to take its art from those places once it existed: only the pictures were missing.
- **Alternatives:** new folders per phase, which would have meant new lookups in `make_assets.py`.

### 4. What about placeholders that art had already replaced?
- **Chosen:** `tools/make_assets.py` now deletes, at its end, any `*_placeholder` texture that no model, blockstate or Java class names.
  That took 40 dead placeholders (the old genset, cabinet, cable, heater and breaker ones and the old grounding set) out of the jar.
- **Why:** they were still drawn (some only to keep the noise sequence of later textures the same) and shipped, though nothing used them.
- **Alternatives:** stop drawing each one, which would shift the random noise of every script texture drawn after it.

### 5. The rotor texture
- **Chosen:** a Ludo dark painted steel, with the placeholder's yellow stripe painted across it by `tools/process_art.py`.
- **Why:** the stripe is what shows the tower fan and the disconnect blades turning.
- **Alternatives:** ask Ludo for a striped hub (a stripe that tiles straight across the face is hard to get from a picture).

### 6. The pole and crossarm woodgrain (docs/ALIGNMENT.md, section 9)
- **Chosen:** drawn in the existing Ludo `mat_wood_creosote`; the script's woodgrain is no longer saved.
- **Why:** distribution poles are creosote- or penta-treated wood, and the material was already in the mod. It was the last
  script-drawn texture on a known-gaps list.
- **Alternatives:** a new Ludo pole picture.

### 7. Not done in this wave
- The breaker levers (`breaker_lever_*`) and the insulator caps, glaze and pins are small script-drawn textures without the
  `*_placeholder` name, on no gaps list. They are flat colour markers (phase colours, lever states) rather than materials, so they
  were left, and are now listed in PROGRESS.md's Known gaps. Tell us if they should become Ludo art or take the `*_placeholder` name.
