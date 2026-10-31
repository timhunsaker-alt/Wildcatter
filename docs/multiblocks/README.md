# Multiblock specs

Every multiblock has a spec here **before any code is written** (CLAUDE.md). A spec has: the real-world counterpart, a
layered ASCII grid (bottom to top), a legend, the controller, rotation, how it forms, what happens when a part is
removed, and the game test.

**Status: the layouts-and-values below were APPROVED by Tim on 2026-10-30.** Approval covers the real-world numbers and the
part lists. The ASCII grids are still sketches and are fixed with the models when each multiblock is built. Approval is
not a go-ahead to build: that waits for the roadmap phase (ROADMAP.md). Nothing here is built except where a spec says "As built".

| Spec | Roadmap phase | Replaces |
|---|---|---|
| [steam-plant.md](steam-plant.md) | 4 | 3x3x3 boiler, 3x3 firebox, 27-turbine cube |
| [genset.md](genset.md) | 4, 4.5b | 500 kW diesel and 1 MW gas skids at true size |
| [substation.md](substation.md) | 4 | single `substation_transformer` block |
| [refinery-tower.md](refinery-tower.md) | 5 | refinery base plus two identical columns |
| [cooling-tower.md](cooling-tower.md) | 4 | fan plus two identical fill blocks |
| [poles.md](poles.md) | 4.5e | stacked one-block pole sections and crossarms |
| [padmount.md](padmount.md) | 4.5e | one-block pad-mounts and unit substation |

## Shared rules
- One **controller** block validates the structure and holds all state. Parts have no logic of their own beyond
  exposing a face to the controller, so the four copies of formation code collapse into one shared `MultiblockSpec` helper.
- Parts are distinct blocks with distinct models and recipes. No spec tiles one block to get capacity.
- Capacity comes from a bigger or better structure (a larger drum, a second superheater), never from more copies.
- Parts keep their own block ids so the Power Meter can name what it is looking at.
- A rotation is one of four horizontal facings of the controller. The grid is written for NORTH; other facings rotate it.
- Formation is checked by the controller every 20 ticks and when a part changes. Removing a part dissolves the
  structure; fluids and fuel in the controller tank are kept until it re-forms, then spill if the tank shrinks.

## Big parts (Phase 4.5, true scale)
Decided with Tim on 2026-10-30: machines are true scale, 1 block = 1 m. Each real component is still **one item**, but placed it
fills its real size, the way a bed fills two spaces. This is not tiling: the cells are pieces of one component.
- Footprint: a box of cells drawn facing north with the main cell (piece 0) named. Piece 0 holds the block entity; the other
  pieces are the same block id with their own `piece` number, and point at piece 0 through the footprint.
- Placing: in front of the player, turned to face them, only if every cell is free. Holding the item shows a ghost outline,
  green where it fits and red where something is in the way.
- Breaking any piece breaks the part and drops one item. A click, a meter or a shock on any piece is on the part.
- Collision follows the real shape, a few boxes per cell, from the model tool.
- Terminals are on named faces of named pieces (where the bushings or cable boxes really are), not on every face.
- A multiblock layout lists each big part's main cell; the part must face the machine's way, and its footprint fills the rest.
- Older worlds: a part that was one block grows into its cells when there is room, and otherwise drops as an item and says where.
  Parts whose machine layout changed with true size (the genset parts) always drop.
- Models come from `tools/models/<part>.py` (real dimensions in metres) and are drawn in the material textures in
  `art/final/mat/`. The tool refuses a box more than a block past its cell and a face with no texture.
