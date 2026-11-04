# Ring crane (Phase 10c3, proposed 2026-11-04)

A ring-based super heavy lift crane: the reactor vessels, steam generators and modules that a crawler cannot set, and the far
side of a big site. It builds from a laydown chest and takes a site apart again, as the tower and crawler cranes do
(tower-crane.md, crawler-crane.md). It runs on its own diesel engines. One true-size part, its ring. The upper works, the boom and
the hook are drawn above it by a renderer.

## Real-world counterpart
The **Sarens SGC-250** ("Big Carl", which set Hinkley Point C's reactor dome and modules). Values are from Sarens' SGC brochure
(sgr.d.marc.0180) and the Sarens, Caterpillar and Bosch Rexroth releases for the crane, unless marked *proposed*. (The ROADMAP's
first note named Mammoet; the SGC-250 is Sarens'.)

| Item | Value | Source |
|---|---|---|
| Load moment, capacity | 250,000 tm; 5,000 t | brochure |
| Outer ring | 48.5 m across; 128 wheel bogies on two ring beams | brochure |
| Ring mats | 29.5 to 52.8 m across; 30 t/m² | brochure |
| Counterweight | 5,200 t, in boxes of 100 t filled on site | brochure |
| Main boom | 118 m or 160.5 m | brochure |
| Main hoist | 6 winches; 20 m/min on layer 1; 536 kN single line pull; 50 mm rope | brochure |
| Boom hoist | 4 winches; 20 m/min; 536 kN | brochure |
| Slewing | 6 degrees/min, 360 degrees | brochure |
| Main hook | 3,200 t SWL; 6 x 5 sheaves, 60 parts of line; 104.6 t; 14.5 m tall. Its mass is part of the load | brochure |
| Engines | 12 Cat diesels of 310 kW (3.72 MW) in 6 power packs, driving the hydraulics | Sarens, Cat |
| Wind limit | 14 m/s | brochure |
| Parts of line | `ceil(t x g / 536 kN)`, rounded up to a multiple of 6 (one share for each winch), 6 to 60 | *proposed* |
| Hydraulic efficiency | 0.75, engine to hook, as the crawler | *proposed* |
| Fuel use | 220 g/kWh: about 975 L/h at 3.72 MW; 15 % of that idling | *proposed* |
| Fuel tank | 6,000 L, 1,000 L for each power pack | *proposed* |
| Slewing and luffing drives | 600 kW and 750 kW | *proposed* |
| Luffing | 0.1 degrees a second | *proposed* |

**Corrections to the plan:**
- **Power:** the plan said about 2 MW of diesel generator sets. The crane has 12 diesels of 310 kW, 3.72 MW, driving hydraulic
  pumps. Bosch Rexroth's release says two 280 kW engines a pack; Sarens and Cat say 310 kW, which is used.
- **Hoist speed:** the plan's "2 to 10 m/min heavy" fits. The hook moves at 6 x 20 m/min divided by its parts of line: 20 m/min
  on 6 parts, 2 m/min on 60.

**The boom is set by the player:**
- Sneak-right-click with an empty hand steps it between the 118 m and the 160.5 m main boom.
- It changes only while the crane has no load on its hook.
- The 118 m boom lifts more; the 160.5 m one reaches farther.
- The heavy-duty jib (40.5 to 99.5 m) is left out: its chart is only partly published.
- (Decided with Tim, 2026-11-04.)

**The lift chart** (t at radius m; blank: outside that boom's range):

| r | 118 m | 160.5 m |
|---|---|---|
| 40 | 5,000 | |
| 45 | | 3,114 |
| 50 | 4,362 | 3,017 |
| 60 | 3,831 | 2,850 |
| 70 | 3,327 | 2,694 |
| 80 | 2,746 | 2,349 |
| 90 | 2,321 | 2,089 |
| 100 | 2,001 | 1,866 |
| 110 | 1,756 | 1,621 |
| 120 | 1,554 | 1,424 |
| 130 | | 1,260 |
| 140 | | 1,126 |
| 150 | | 1,009 |
| 160 | | 913 |
| 170 | | 838 |

**Reading the chart:**
- As the crawler's: the farther radius's figure, never interpolated.
- It works from the boom's first radius to its last.
- **The main hook caps it at 3,200 t** *(proposed)*. The brochure's 5,000 t needs a hook arrangement it does not describe. No part
  in the mod comes near either.

**The geometry** *(proposed)*:
- The boom foot pivots 18 m ahead of the slewing centre, on the front platform, and 6 m up.
- The radius is 18 m plus the boom length times cos(angle). The 118 m boom stands at 80 degrees at 40 m and 30 degrees at 120 m.
- The height under the hook is 6 m plus the boom length times sin(angle), less 18.5 m (the 14.5 m hook block and 4 m of head).
  That is about 104 m at 40 m on the 118 m boom.

## The part
`wildcatter:ring_crane`, one item, the two ring beams on their mats, 1 m high: the 616 cells of a 49 x 49 square whose centres lie
20 to 24.25 m from its middle. North quarter, facing north (the boom points north at rest):
```
layer 0 (the north rows; the ring closes the same way round)
. . . . . . . . . . . . . . . . . . . . . # # # M # # # . . . . . . . . . . . . . . . . . . . . .
. . . . . . . . . . . . . . . . . # # # # # # # # # # # # # # # . . . . . . . . . . . . . . . . .
. . . . . . . . . . . . . . # # # # # # # # # # # # # # # # # # # # # . . . . . . . . . . . . . .
. . . . . . . . . . . . # # # # # # # # # # # # # # # # # # # # # # # # # . . . . . . . . . . . .
. . . . . . . . . . . # # # # # # # # # # # # # # # # # # # # # # # # # # # . . . . . . . . . . .
. . . . . . . . . # # # # # # # # # . . . . . . . . . . . . . # # # # # # # # # . . . . . . . . .
. . . . . . . . # # # # # # # # . . . . . . . . . . . . . . . . . # # # # # # # # . . . . . . . .
. . . . . . . # # # # # # # . . . . . . . . . . . . . . . . . . . . . # # # # # # # . . . . . . .
```
- `M`: main cell. It is the ring's north cell, in the middle column, and holds the block entity.
- `#`: the ring: its mats, its two ring beams and their rails.
- `.`: not part of it. The inside of the ring and the corners stay free.
- All four facings. The slewing centre is the middle of the square, 24.5 m south of the main cell's north edge.

**Drawn above it, turning with the slew:**
- the bogies on both ring beams;
- the front platform carrying the boom foot, and the rear platform with its counterweight boxes (52 boxes of 100 t);
- the double-legged lattice main boom at its length and angle;
- the backmast and its pendants to the boom head;
- the 14.5 m hook block on its falls.
- The renderer is drawn from 256 m away, so the 160.5 m boom is not culled.

**Diesel:**
- It is piped to any cell of the ring (`Lines.pull`, `Product.DIESEL`, as the crawler is filled) into its 6,000 L tank.
- The engines run while the crane has work.
- It burns `975 L/h x (0.15 + 0.85 x load)`, where the load is the share of 3.72 MW its moving drives take.

## How it works a site
As the crawler crane (crawler-crane.md, "How it works a site"), with these differences:
- **The laydown chest** (decided with Tim, 2026-11-04):
  - Its chart starts at 40 m, so a chest against its ring is out of reach.
  - Its chest is the chest, barrel or hopper nearest its centre that stands within its chart's radius, as loads are delivered to a
    laydown area under a real one's boom.
  - It is found among the loaded block entities in range, kept, and looked for again at most every 5 s while there is none.
  - The pick is read at the chest's real radius and bearing.
- **What is on the hook:** the part, 2 % for slings, and the 104.6 t main hook block.
- **Hoist speed:**
  - **Parts of line:** the hook block is reeved for the load at 536 kN a line, in sixes, from 6 to 60.
  - **Hook speed:** 120 m/min divided by the parts.
  - **Power limit:** the hook speed is held to what 3.72 MW can lift at 0.75 efficiency.
  - **At 180 t:** a reactor vessel with its hook block (288 t) goes up on 6 parts at 20 m/min.
- **Moving the load out:** luffing at 0.1 degrees a second, while it slews at 6 degrees a minute.
- **Power drawn from the engines:**
  - hoisting: `m x g x v / 0.75`;
  - slewing: 600 kW;
  - luffing: 750 kW.

## Readout (right-click)
"Ring crane (5,000 t), 118 m boom: BUILDING PWR, lift 3 of 40: SLEWING Reactor vessel (180 t), 288.2 t on the hook, chart
3,200.0 t at 60 m; diesel 5,412 of 6,000 L".

It waits as the crawler does (WAITING, TOO HEAVY, OUT OF REACH, TOO HIGH, CHEST FULL, NO SITE, IN THE WAY, NO DIESEL). With no
laydown chest it reads `NO CHEST: set a chest within its reach, 40 to 120 m out`.

## Breaking
As the tower crane. A load on its hook goes back to its laydown chest, or drops there.

## Left out (Known gaps)
- **Relocation:** a real one moves between lifting positions on rails; this one stands where it is placed.
- **Configurations:** the heavy-duty jib, and the hook arrangement for its 5,000 t.
- **Wind and ground bearing:** the 14 m/s wind limit and the 30 t/m² mats are left out.
- **No collision:** the boom has no collision.
- **Assembly:** it is placed whole, with its counterweight.

## Game test (`heavyLift`)
1. The ring crane, on its 118 m boom, is filled with diesel from a stock tank.
2. Its readout names its chart at the vessel's 60 m: 3,831 t, capped at 3,200 t. Stepped to 160.5 m, it reads 2,850 t; it is
   stepped back.
3. It sets a reactor vessel from its laydown chest 45 m out at a site about 60 m out.
4. Screenshot: the ring crane mid-lift.
