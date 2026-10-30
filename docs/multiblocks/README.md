# Multiblock specs

Every multiblock has a spec here **before any code is written** (CLAUDE.md). A spec has: the real-world counterpart, a
layered ASCII grid (bottom to top), a legend, the controller, rotation, how it forms, what happens when a part is
removed, and the game test.

**Status of every spec below: PROPOSED.** The numbers are real-world values chosen to be plausible; each one marked
**DECISION** needs Tim's approval before code. Nothing here is built except where a spec says "As built".

| Spec | Roadmap phase | Replaces |
|---|---|---|
| [steam-plant.md](steam-plant.md) | 4 | 3x3x3 boiler, 3x3 firebox, 27-turbine cube |
| [genset.md](genset.md) | 4 | 2x2x2 diesel engine and gas turbine cubes |
| [substation.md](substation.md) | 4 | single `substation_transformer` block |
| [refinery-tower.md](refinery-tower.md) | 5 | refinery base plus two identical columns |
| [cooling-tower.md](cooling-tower.md) | 4 | fan plus two identical fill blocks |

## Shared rules
- One **controller** block validates the structure and holds all state. Parts have no logic of their own beyond
  exposing a face to the controller, so the four copies of formation code collapse into one shared `MultiblockSpec` helper.
- Parts are distinct blocks with distinct models and recipes. No spec tiles one block to get capacity.
- Capacity comes from a bigger or better structure (a larger drum, a second superheater), never from more copies.
- Parts keep their own block ids so the Power Meter can name what it is looking at.
- A rotation is one of four horizontal facings of the controller. The grid is written for NORTH; other facings rotate it.
- Formation is checked by the controller every 20 ticks and when a part changes. Removing a part dissolves the
  structure; fluids and fuel in the controller tank are kept until it re-forms, then spill if the tank shrinks.
