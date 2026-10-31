# Well site at true size (APPROVED 2026-10-31 with the Phase 4.5g plan)

The small drilling rig, the two wellheads, the pumpjack and the steel pipe, each a true-size big part (README, "Big parts").
Values are approximate and drawn from typical equipment of each class. Ratings and power do not change from Phase 3: the rig is a
30 kW (40 hp) and the pumpjack a 7.5 kW (10 hp) 480 V three-phase motor. The full land rig is its own wave, 4.5h, with its own
spec.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `drilling_rig`, small mast rig | 3 x 10 x 6 | Skid-mounted top-head-drive rig of the class used for water wells and shallow oil wells: a 40 hp electric power unit driving the hydraulics, a skid 2.4 x 6 m, a 9 m mast (one 6 m drill pipe joint plus the head) at the front end over the hole |
| `oil_wellhead` | 1 | Rod-pumping wellhead: casing head, tubing head, pumping tee with its flowline outlet and the stuffing box, about 1.1 m tall |
| `pumpjack` | 3 x 5 x 7 | Conventional beam pumping unit, API size 80-119-54 (54 in = 1.37 m stroke) with its 10 hp motor, about 7 m long and 5 m tall. The base is about 1.8 m wide and straddles the well's centre line, so it takes 3 cells across. The horsehead hangs over the wellhead, which is a gap in its footprint |
| `gas_wellhead`, Christmas tree | 1 x 2 x 1 | 2 1/16 in 5,000 psi tree, about 2 m tall: lower and upper master valves, flow cross, wing valve and choke, swab valve and tree cap |
| `gas_line`, steel pipe | 1 | 4 in NPS (114 mm) gathering line with welded elbows and tees |
| `pipe`, water pipe | 1 | 2 in NPS (60 mm) Schedule 40 steel pipe |

Pipes stay one block each: their click and collision box stays 0.25 m across so a thin pipe can still be clicked.

## Footprints (facing north; x east, z south; M is the main cell)
Small rig, the hole under its main cell at the north end:
```
layer 0 (the skid)        layer 1                   layers 2 to 9
  z0  S M S                 . A .                     . A .     A: the mast (the top head rides it over the hole)
  z1  S S S                 R . C                     . . .     R: pipe rack   C: driller's console
  z2  S S S                 R . .                     . . .     S: skid deck
  z3  S S S                 R . .                     . . .     P: power unit (motor, hydraulic pump, its terminal box
  z4  S S S                 P P P                     . . .        on the bottom cell below it, T)
  z5  S T S                 P P P                     . . .
```
Pumpjack, the wellhead W one cell north of its main cell:
```
layer 0                     layers 1 to 4
  z-1  . W .                  . H .     H: the horsehead, bridle and polished rod over the well
  z0   B M B                  F F F     F: frame (Samson post near z2, walking beam above, crank and reducer near z4,
  z1..z4  B B B               F F F        motor at z5)
  z5   . T .                  F F F     T: the motor's terminal box cell, wired on its back or either side (the cells
                                          beside it are left open; the motor's slide rails run back under it)
```
Christmas tree: two cells, the bottom one the main cell. Oil wellhead: one cell.

## How it works
1. **Drilling:** the rig drills straight down from under its main cell, as before (`Tuning.DRILL_TICKS_PER_BLOCK`, `DRY_HOLE_Y`).
   Its power unit's terminal box is cell T, wired on its back face.
2. **Strike:** the rig rigs down. It leaves the hole, drops as its item, and the right wellhead is left on the hole: an
   `oil_wellhead` for oil, a Christmas tree for gas. A dry or blocked hole leaves the rig standing, as before.
3. **Pumpjack:** clicked on an oil wellhead, it snaps into place with its horsehead over the well, its front the way the player
   faces. Placed anywhere else it stands but pumps nothing and says it is not over a well. Its motor's terminal box is cell T,
   wired on its back or either side.
   It pumps the field under its wellhead into its 4,000-unit tank (standing in for a flowline and stock tank until Phase 5).
   Buckets work on the pumpjack or its wellhead.
4. **Lines:** steel pipe joins an oil wellhead on any side (its flowline outlet) and a Christmas tree on any side of its bottom
   cell. Fireboxes, refineries and gensets find a pumpjack through its wellhead.
5. **Breaking:** any piece breaks the part and drops its item. Breaking the wellhead under a pumpjack leaves the pumpjack
   standing with nothing to pump.

## Moving parts (amends docs/kinetics-and-sound.md)
Parts slower than 120 rpm show at their real speed; faster parts at rpm / 20 as before.
- **Pumpjack:** 9 strokes a minute at full speed (the crank at 9 rpm; slip and low voltage slow it). The crank and counterweights
  turn, the pitman arms follow the crank pins, the walking beam and horsehead rock (solved as a four-bar linkage in
  `kinetic/BeamLinkage`), and the bridle, carrier bar and polished rod go up and down. It stops when tripped, full or dry.
  Geometry: front arm 2.8 m (saddle bearing to the horsehead face), back arm 2.4 m, crank radius 0.59 m (a 1.37 m stroke),
  pitman 2.9 m, saddle bearing 4.3 m up, crank centre 1.5 m up.
- **Small rig:** the top head and drill pipe turn at 80 rpm while drilling, and the head slides down the mast through each 6 m
  joint before going back up for the next.

## Older worlds
- A one-block rig grows into its cells where it has room, its main cell over its hole; otherwise it drops, with a chat line.
- A one-block pumpjack becomes an `oil_wellhead` in place and drops a pumpjack item, with a chat line to place it against the
  wellhead. Crude in its tank is lost.
- A one-block gas wellhead grows upward where the cell above is free.

## Game test
`wells`: the rig drills to the field and strikes, leaving an oil wellhead on a cased hole and its item; a pumpjack clicked on
the wellhead snaps over it, runs on 480 V and fills its tank, its beam rocking on the client. A Christmas tree feeds a firebox
and a gas genset. An old one-block pumpjack becomes a wellhead and drops its item.
