# The lined tailings cell and the process water pump (Phase 12, 2026-11-06)

What Tailings' Phase 4 and Wildcatter's uranium mill both need: somewhere for tails to go, and water for a plant without a 110 V supply. Tim's
words: "do whatever is needed for wildcatter". He decided the breach on 2026-11-06.

**Sources:** ICOLD Bulletin 139; the Global Industry Standard on Tailings Management (2020). A figure marked *(proposed)* is a textbook value,
a scaled value, or a proposal.

## The cell
| Block | What it is |
|---|---|
| `cell_liner` | One block of 2 mm HDPE geomembrane on its bedding. Lay a floor and walls, any shape |
| `decant_tower` | A concrete decant tower, placed on the floor. It takes tails slurry off any steel pipe touching it, and returns the water on a water pipe |
| `settled_tails` | What fills the cell as it settles. Not an item |

A cell is any shape the player lays. This one, 3 high, seen from the side and from above (`L` `cell_liner`, `D` `decant_tower`, `.` open):
```
 layer 3   L L L L L        from above, layers 1 to 3:   L L L L L
 layer 2   L . . . L                                     L . . . L
 layer 1   L . D . L                                     L . D . L
 floor 0   L L L L L                                     L . . . L
                                                         L L L L L
```
There is no controller but the tower, and any rotation works. It forms when the tower's scan closes; removing a liner block breaches it.

- **The basin** is found from the tower: every open cell on its layer it can reach, with a liner floor under each and liner walls around them, at most 4,096 cells *(proposed)*. The walls' lowest point sets the height.
- **It holds** 2.2 t of settled solids a block *(proposed, dry density of settled tails is 1.4 to 2.2)*. Slurry is taken as 40 % solids *(proposed)*.
- **The fill is visible:** a `settled_tails` block appears for each 2.2 t, layer by layer from the tower outward.
- **Decant:** three quarters of the slurry's water *(proposed)* goes back on a water pipe to any water user on it. It runs by gravity and has no motor *(a game choice)*. Water with nowhere to go is lost.
- **What stops it:**
  - FULL: the cell is at its height. The bank upstream stops when its tails pipe fills;
  - BREACHED: a floor block is missing, or a wall was broken below what the cell holds. It stops taking tails, and seeps where the breach is, shown as dripping. No ground is contaminated (Tim, 2026-11-06). Put the liner back and it goes on.
- **Breaking settled tails** gives nothing. Its mass is held by the tower.

## The uranium mill
`ccd_thickener` gets a tails outlet: the west face of its third row, bottom layer. With a pipe on it, the washed solids leave as tails slurry at 50 % solids, and a full pipe stops the thickener. With no pipe it counts them as before, so built mills go on working.

## The process water pump
`process_water_pump`, one block: the water pump with a **37 kW, 480 V three-phase** motor *(proposed)*, for plants with no 110 V supply. It lifts the same water, from the water touching it, to the same users.

## Left out
- Seepage collection, piezometers, dust, rehabilitation and capping; dam raising by the player's own berms.
- Contaminated ground (Tim, 2026-11-06).

## Test
`tailingsCell` (Fabric client scene): a built cell takes tails, shows the fill, reads BREACHED with a floor block broken, and goes on when it is put back. Tailings' scenes take tails over a real pipe.
