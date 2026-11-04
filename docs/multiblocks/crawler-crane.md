# Crawler crane (Phase 10c2, proposed 2026-11-04)

A lattice-boom crawler crane for the heavy lifts: a transformer, a turbine casing or a pot that the tower crane's 16 t cannot take.
It builds from its chest and takes a site apart again, as the tower crane does (docs/multiblocks/tower-crane.md). It runs on its own
diesel engine. One true-size part, its crawlers and carbody. The upper works, the boom and the hook are drawn above it by a
renderer.

## Real-world counterpart
The **Liebherr LR 1300**, a 300 t crawler crane. Values are from Liebherr's LR 1300 technical data (main boom No. 2821.xx, lift
chart with 104 t counterweight and 57 t carbody counterweight; technical description), unless marked *proposed*.

| Item | Value | Source |
|---|---|---|
| Maximum load | 286.2 t at 4.3 m on the 20 m boom; 300.5 t at 5 m on the 23 m boom | lift chart, 104 t counterweight |
| Main boom | 20 to 95 m in the chart's 15 lengths | lift chart |
| Working range | boom angle 15 to 86 degrees | working range diagram |
| Engine | Liebherr D 946 A7-04 diesel, 390 kW at 1,700 rpm | technical description |
| Fuel tank | 750 L | technical description |
| Main winches | rope speed 0 to 138 m/min; line pull 150 kN (7th layer), 28 mm rope | technical description |
| Slewing | 0 to 1.8 rpm | technical description |
| Boom hoist | 127 s from 15 to 86 degrees | technical description |
| Crawlers | 9.65 m long, 8.0 m across them, 1.2 m shoes | dimension drawing |
| Hook block | 300 t, 11 sheaves (up to 22 parts of line) | technical description |
| Fuel use | about 102 L/h at full load (220 g/kWh); 15 % of that idling, as the gensets | *proposed* |
| Hydraulic efficiency | 0.75, engine to hook | *proposed* |

**The boom is set by the player:**
- Sneak-right-click with an empty hand steps it through the chart's lengths: 20, 23, 26, 32, 38, 44, 50, 56, 62, 68, 74, 80, 86,
  92, 95 m.
- It changes only while the crane has no load on its hook.
- A short boom lifts heavy close in, a long one reaches far.
- (Decided with Tim, 2026-11-04.)

**The lift chart** (t at radius m, 104 t counterweight; blank: outside that boom's range):

| r \ boom | 20 | 23 | 26 | 32 | 38 | 44 | 50 | 56 | 62 | 68 | 74 | 80 | 86 | 92 | 95 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 4.3 | 286.2 | | | | | | | | | | | | | | |
| 5 | 286.2 | 300.5 | 296.8 | | | | | | | | | | | | |
| 6 | 258.4 | 259.3 | 257.5 | 245.6 | 237.4 | 212.6 | | | | | | | | | |
| 7 | 225.6 | 225.6 | 222.7 | 216.1 | 207.6 | 198.5 | 189.6 | 169.5 | | | | | | | |
| 8 | 196.1 | 196.0 | 195.7 | 191.4 | 185.2 | 179.2 | 169.5 | 162.2 | 150.4 | 127.8 | | | | | |
| 9 | 173.1 | 173.0 | 172.7 | 172.4 | 166.5 | 161.0 | 153.6 | 145.9 | 138.9 | 125.7 | 109.1 | 97.2 | 83.6 | | |
| 10 | 154.6 | 154.6 | 154.3 | 154.0 | 151.6 | 144.3 | 137.4 | 131.1 | 125.1 | 118.3 | 104.7 | 96.5 | 83.6 | 73.1 | 67.8 |
| 16 | 89.3 | 89.5 | 89.4 | 89.5 | 88.7 | 85.6 | 82.5 | 79.6 | 76.8 | 74.1 | 71.4 | 68.9 | 66.4 | 62.2 | 59.0 |
| 20 | 65.4 | 65.7 | 65.7 | 65.7 | 65.4 | 65.2 | 64.1 | 62.0 | 59.9 | 57.9 | 55.9 | 54.0 | 52.1 | 50.3 | 49.4 |
| 24 | | 51.0 | 51.1 | 51.2 | 50.9 | 50.6 | 50.2 | 49.8 | 48.4 | 46.8 | 45.2 | 43.7 | 42.1 | 40.6 | 39.8 |
| 26 | | | 45.7 | 45.9 | 45.6 | 45.3 | 44.8 | 44.5 | 44.0 | 42.5 | 41.0 | 39.6 | 38.1 | 36.7 | 36.0 |
| 32 | | | | 34.2 | 34.0 | 33.8 | 33.3 | 32.9 | 32.4 | 31.9 | 31.4 | 30.2 | 29.0 | 27.8 | 27.2 |
| 38 | | | | | 26.3 | 26.2 | 25.8 | 25.4 | 24.8 | 24.4 | 23.8 | 23.3 | 22.5 | 21.5 | 21.0 |
| 44 | | | | | | 20.7 | 20.4 | 20.0 | 19.5 | 19.0 | 18.5 | 18.0 | 17.4 | 16.8 | 16.2 |
| 50 | | | | | | | 16.3 | 16.0 | 15.5 | 15.1 | 14.6 | 14.1 | 13.5 | 13.0 | 12.7 |
| 55 | | | | | | | | 13.4 | 13.0 | 12.5 | 12.0 | 11.5 | 10.9 | 10.4 | 10.1 |
| 60 | | | | | | | | | 10.7 | 10.3 | 9.8 | 9.3 | 8.7 | 8.2 | 7.9 |
| 65 | | | | | | | | | | 8.5 | 8.0 | 7.5 | 6.9 | 6.4 | 6.1 |
| 70 | | | | | | | | | | | 6.4 | 5.9 | 5.3 | 4.8 | 4.6 |
| 75 | | | | | | | | | | | | 4.6 | 4.0 | 3.4 | 3.0 |
| 80 | | | | | | | | | | | | | 2.6 | | |

**Reading the chart:**
- Between two radii the crane takes the farther radius's figure, and never interpolates.
- Its smallest radius is the boom's first figure. Its largest is the boom's last, and no more than its 15 degree reach.
- The chart's own minimum radius stands in for the 86 degree limit.

**The geometry** *(proposed, from the dimension drawing)*:
- The boom foot pivots 1.5 m ahead of the slewing centre and 2.7 m up.
- The radius is 1.5 m plus the boom length times cos(angle).
- The height under the hook is 2.7 m plus the boom length times sin(angle), less 4 m for the boom head and hook block.

**Corrections to the earlier plan:**
- The plan said 300 t at 4.3 m and 25 t at 80 m on an 84 m boom. The real chart gives 286.2 t at 4.3 m on a 20 m boom, and 2.6
  t at 80 m on an 86 m boom.
- Slewing is 1.8 rpm, not 1.4, and the rope speed is 138 m/min, not 120.

## The part
`wildcatter:crawler_crane`, one item, 9 x 2 x 9 m: the two crawlers and the carbody between them. Footprint facing north (the boom
points north at rest), bottom layer first:
```
layer 0                      layer 1
M T T T T T T T T            T T T T T T T T T
T . . . . . . . T            . . . . . . . . .
T . . . . . . . T            . . . . . . . . .
T . . K K K . . T            . . . R R R . . .
T . . K K K . . T            . . . R R R . . .
T . . K K K . . T            . . . R R R . . .
T . . . . . . . T            . . . . . . . . .
T . . . . . . . T            . . . . . . . . .
T T T T T T T T T            T T T T T T T T T
```
The diagram is drawn schematically. In the model, the crawlers run north to south down the west and east sides, and the carbody
spans between them.

- `M`: main cell. It is the north-west end of the west crawler and holds the block entity.
- `T`: the crawlers' track frames and shoes.
- `K`: the carbody, with its 57 t of carbody counterweight.
- `R`: the slewing ring.
- `.`: piece cells with no solid shape.
- All four facings. The slewing centre is the middle of the box.

**Drawn above it:**
- the upper works: the turntable, the engine house, the cab, the 104 t counterweight and the A-frame;
- the lattice boom at its length and angle, with its pendants;
- the hook block on its parts of line.

**Diesel:**
- It is piped to any cell of the crawlers (`Lines.pull`, `Product.DIESEL`, as a genset's tank is filled) into its 750 L tank.
- The engine runs while the crane has work.
- It burns `102 L/h x (0.15 + 0.85 x load)`, where the load is the share of 390 kW its moving drives take.

## How it works a site
As the tower crane (tower-crane.md, "How it works a site"), with these differences:
- **Reach:** the boom's chart, out to 95 m on the longest boom.
- **Height under hook:** set by the boom and its angle at the set radius.
- **Moving the load out:** luffing the boom, not a trolley.
  - The boom's angle follows the radius at 0.56 degrees a second, which is 127 s for 71 degrees.
  - It slews at 1.8 rpm, at the same time as it luffs.
- **Hoist speed:**
  - **Parts of line:** the hook block is reeved with as many parts of line as the load needs at 150 kN each, from 1 to 22.
  - **Hook speed:** 138 m/min divided by the parts.
  - **Power limit:** the hook speed is held to what 390 kW can lift at 0.75 efficiency.
  - **At 300 t:** the hook rises at about 6 m/min.
- **Power drawn from the engine:**
  - hoisting: `m x g x v / 0.75`;
  - slewing: 40 kW;
  - luffing: 60 kW.
  - The slewing and luffing figures are *proposed*.

## Readout (right-click)
"Crawler crane (300 t), 32 m boom: BUILDING Turbine island, lift 12 of 48: SLEWING GSU 60, 91.8 t on the hook, chart
154.0 t at 10 m, boom 74 degrees; diesel 612 of 750 L".

It waits as the tower crane does (WAITING, TOO HEAVY, OUT OF REACH, TOO HIGH, CHEST FULL, NO SITE, IN THE WAY), and with
`NO DIESEL: its tank is dry, pipe diesel to its crawlers`.

**Too heavy names the boom:** "TOO HEAVY: Reactor vessel 183.6 t, chart 137.4 t at 10 m on the 50 m boom: a shorter boom lifts
more".

## Breaking
As the tower crane.

## Left out (Known gaps)
- **Travel:** the crane stands where it is placed (the real one crawls at 1.3 km/h).
- **Rigging the crane:** derrick and superlift (the LR 1300 SX), the luffing jib, the 124 t counterweight chart, and boom assembly
  time.
- **Wind and ground bearing:** wind limits are left out, and so is ground bearing (1.45 kg/cm2 with mats).
- **No collision:** the boom has no collision.

## Game test (`cranes`)
1. The crawler is filled with diesel from a stock tank.
2. On its 32 m boom it sets a GSU 60 (90 t) at about 10 m.
3. On its 95 m boom the same lift reads TOO HEAVY.
4. Screenshot: the crawler mid-lift.
