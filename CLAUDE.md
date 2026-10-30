# Wildcatter — instructions for Claude Code

## Read first, every session
1. `docs/VISION.md` — what the mod is and is not
2. `docs/ELECTRICAL_MODEL.md` — voltages, phases, solver, protection
3. `docs/ROADMAP.md` — the CURRENT phase
4. `PROGRESS.md` — what is already built

If a request conflicts with these docs, point out the conflict and ask before doing anything.

## Scope
- Work only on the phase marked CURRENT in ROADMAP.md. New ideas go under "Ideas" in ROADMAP.md, not into code.
- Do not invent mechanics, items, voltages, ratings or numbers that are not in the docs.
  If one is needed, stop and propose it with its real-world counterpart and values.
- Settled decisions are in the table at the top of ELECTRICAL_MODEL.md. Do not reopen them.
  Any new line marked **DECISION** is open: ask, never choose.
- Wildcatter follows real electrical principles but is not a teaching tool. Never describe it as
  training or certification material.

## How to work
- Start every task in plan mode: files to change, multiblock specs needed, game test to add.
  Wait for approval before writing code.
- For any piece of equipment, state its real-world counterpart and the values used (voltage, kVA, PF, etc.).
- No abstract energy units in grid code. Conversion to other mods' energy happens only in plug adapters.
- Never hardcode a voltage or frequency. Read them from the world's electrical standard (NA or IEC).
- Keep the solver package free of Minecraft and Fabric imports.

## Multiblocks
- Write `docs/multiblocks/<name>.md` before any code: layered ASCII grid (bottom to top),
  legend mapping characters to block IDs or tags, controller position, valid rotations,
  how it forms, what happens when a part is removed.
- Build from distinct real parts. Never tile identical blocks to make a bigger machine.
- Add a game test that builds the structure and checks that it forms.

## Art
- Final textures come from Ludo sources, processed by `tools/process_art.py`, then `tools/make_assets.py`.
- Textures drawn by script are placeholders: name them `*_placeholder` and list them in PROGRESS.md under Known gaps.

## Definition of done
- `./gradlew runClientGameTest` passes and covers the new feature
  (on the Mac, run `caffeinate -u -t 30` just before launching the test client).
- PROGRESS.md updated. Field Manual page added or updated.
