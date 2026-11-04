# Tower crane (Phase 10c2, proposed 2026-11-04)

A flat-top tower crane that builds a blueprint's site from its chest, lift by lift, at its real speeds, and takes it apart again.
One true-size part: its cross base. The mast, the jib, the counter-jib, the cab, the trolley and the hook are drawn above it by
a renderer. They have no collision, and nothing else may stand in them (left out, see the end).

## Real-world counterpart
The **Liebherr 380 EC-B 16 Litronic**, a 16 t flat-top crane with a 75 m jib, free-standing on a cross base. Values are from
Liebherr's data as published by the rental firm BKL (380 EC-B 16 Litronic data sheet, September 2022: load chart LM1, driving
units Litronic) unless marked *proposed*.

| Item | Value | Source |
|---|---|---|
| Jib | 75 m (radius 76.8 m to the jib tip; the hook works 2.6 to 75.0 m) | BKL, chart LM1 |
| Maximum load | 16.0 t from 2.6 to 19.6 m | BKL, chart LM1 |
| Tip load | 3.0 t at 75 m | BKL, chart LM1 (the 3.4 t headline needs Litronic's power plus mode; left out) |
| Height under hook | 59.7 m, free-standing on 355 HC mast sections (9 + 1) | BKL, hook height table |
| Hoist gear | 110 kW on a frequency converter | BKL, driving units |
| Hoist speeds | gear 1: 16 t at 36 m/min, 7.5 t at 71; gear 2: 8.8 t at 61, 4.0 t at 123; gear 3: 4.9 t at 102, 1.9 t at 205 | BKL, driving units |
| Trolley | 0 to 95 m/min, 10.5 kW | BKL, driving units |
| Slewing | 0 to 0.8 rpm, 2 x 7.5 kW | BKL, driving units |
| Supply | 131 kVA connected; the world's industrial three-phase (480 V NA, 400 V IEC) | BKL (kVA); the voltage is read from the standard |
| Cross base | 6.0 x 6.0 m, with about 112 t of central ballast slabs | BKL drawing (6.0 m); ballast *proposed* |
| Counter-jib | about 22 m, with its ballast blocks | *proposed*, scaled from the 380 EC-B drawings |

**The load chart** (75 m jib, t at radius m), as the crane uses it:

| r | 19.6 | 20 | 25 | 30 | 35 | 40 | 45 | 50 | 55 | 60 | 65 | 70 | 75 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| t | 16.0 | 15.66 | 12.15 | 9.83 | 8.19 | 6.96 | 6.02 | 5.26 | 4.64 | 4.13 | 3.69 | 3.32 | 3.00 |

Between two radii the crane takes the **farther** radius's figure, as a crane operator reads a chart. It never interpolates.

## The part
`wildcatter:tower_crane`, one item, 6 x 3 x 6 m. Footprint facing north, bottom layer first:
```
layer 0                layer 1                layer 2
M . . . . C            . . . . . .            . . . . . .
. B B B B .            . W W W W .            . . . . . .
. B W W B .            . W T T W .            . . T T . .
. B W W B .            . W T T W .            . . T T . .
. B B B B .            . W W W W .            . . . . . .
C . . . . C            . . . . . .            . . . . . .
```
- `M`: main cell. It is the north-west corner pad, with the crane's switch cabinet. It holds the block entity.
- `C`: a corner pad with its spindle jack.
- `B`: the cross frame's beams.
- `W`: the ballast slabs.
- `T`: the mast foot, the first mast section's base.
- `.`: piece cells with no solid shape, but still part of the footprint, so nothing else stands in the base.
- All four facings. The slewing centre is the middle of the box: the mast's axis.

**Drawn above it:**
- the 355 HC mast to 59.7 m under the hook, about 2.3 m square;
- the slewing platform with the cab;
- the 75 m jib, the trolley and the hook block;
- the counter-jib with its ballast.

The superstructure turns with the slewing angle, the trolley runs out along the jib, and the hook hangs at its height with the
load under it.

**Power:**
- The supply comes in at the switch cabinet: the west face of the main cell, at the world's industrial voltage.
- The crane is one frequency-converter load (`DriveLoad`, power factor 0.95, no frequency shedding), drawing what its moving
  drives need.

## How it works a site
1. **Setup:**
   - place it within 75 m of a site marker;
   - set a chest, barrel or hopper against any cell of its base;
   - feed it at 480 V (400 V on IEC worlds).
2. **The site:**
   - It works the nearest site marker whose origin is within 75 m of its slewing centre.
   - It follows the marker's mode: BUILD or DISMANTLE (docs/blueprints.md, "Site marker").
3. **Each lift**, in the marker's order (docs/blueprints.md, "The lift list"):
   1. **Pick:** the hook goes to the chest's side at the smallest radius, 2.6 m. The chest stands for the laydown area.
      - The parts and blocks of the lift must be in the chest. They are taken out when the load leaves the ground.
      - The riggers sling the load: 30 s *(proposed: a prepared, single-point load)*.
   2. **Hoist:** to clear the site: to the site's top plus the load's own height plus 2 m, at most 59.7 m under the hook.
   3. **Slew and trolley together:**
      - the slewing angle at 0.8 rpm, and the trolley out to the set radius at 95 m/min;
      - the step lasts as long as the slower of the two.
   4. **Lower:** to the load's place.
   5. **Set:**
      - land and unhook: 30 s *(proposed)*;
      - the part is placed and grown, or the pallet's blocks are set, as a hand would (`Surveys.setLift`).
   6. The empty hook goes up again on the way to the next pick, at gear 3's light-hook speed.
4. **Hoist speed:**
   - the crane picks the fastest gear whose rated load holds the load on the hook;
   - within a gear, the speed falls in a straight line from the light load's figure to the full load's;
   - below the light load, it runs at the light load's speed.
5. **Load check:** before the pick, the load on the hook (the lift's mass, plus 2 % rigging, at least 0.5 t, docs/blueprints.md)
   must be within the chart at the set radius. The radius is measured from the slewing centre to the middle of the part's box or the
   pallet's blocks.

## Power drawn
The hoist draws `m x g x v / 0.85` while it hoists, at most 110 kW. Lowering is regenerated into its brake resistor and draws
nothing. The trolley draws 10.5 kW and the slewing gear 15 kW while they move. The cab and controls draw 3 kW while the crane
has a site. All of this is *proposed*: the efficiency, the regeneration and the 3 kW standby.

If the voltage is lost or below 90 % the crane stops where it is, holding its load on its brakes, and goes on when the power is back.

## Dismantling
With its marker in DISMANTLE, the crane takes the site's lifts down in reverse order, last lift first, into its chest:
1. it rigs the load at its place (30 s);
2. it hoists it clear and slews back;
3. it lands it at the chest (30 s);
4. a part goes in as its item, as a broken part drops; each block of a pallet goes in as its item.

Only what stands is taken: a block that is not the blueprint's is left. With the chest full the crane waits.

## Readout (right-click)
"Tower crane (16 t, 75 m jib): BUILDING Potline, lift 41 of 312: HOISTING a pallet of 25 blocks, 11.2 t on the hook, chart
12.2 t at 25 m; 480 V, 62 kW".

What it waits for:
- `WAITING: 2 x Pot in its chest`
- `TOO HEAVY: Pot 61.2 t, chart 3.3 t at 70 m`
- `OUT OF REACH: Rectiformer at 81 m (75 m jib)`
- `TOO HIGH: the site needs 63 m under the hook (59.7 m)`
- `NO POWER: 0 V at its switch cabinet`
- `CHEST FULL`
- `NO SITE within 75 m`
- `IN THE WAY: <block> at x, y, z`

## Breaking
Breaking any cell breaks the whole crane. It drops its item. A load on its hook goes back into its chest, or drops there if the
chest is full.

## Left out (Known gaps)
- **Erection and climbing:** the crane is placed whole at its free-standing height.
- **Wind:** the real crane stops above about 72 km/h and weathervanes.
- **The superstructure:** it has no collision, and nothing stops a jib passing through a building or another crane's jib.
- **The rest:** the 3.4 t power-plus tip load, rail travel and swing-sway damping.

## Game test (`cranes`)
1. A diesel set's 480 V feeds the crane.
2. It finishes the surveyed genset blueprint from its chest (the site seeded with all but its last lifts).
3. The marker then switches to DISMANTLE, and the crane lifts the last parts back into its chest.
4. A lift beyond the chart reads TOO HEAVY.
5. Screenshot: the crane mid-lift.
