# The anode plant (Phase 10b3a, approved with the 10b3 plan 2026-11-03)

A prebake pot hangs carbon anodes in its bath and burns them away: about 0.41 kg of carbon for each kg of aluminium. The anode
plant makes them:
- a **paste plant** crushes and sizes calcined coke and recycled butts and mixes them with hot pitch;
- a **forming press** (vibro-compactor) presses the paste into green anode blocks;
- an open-top **ring baking furnace** bakes them slowly to about 1,100 °C, carbonizing the pitch;
- a **rodding shop** casts each baked anode onto an anode rod with cast iron in its stub holes, and strips the spent anodes
  that come back from the pots: the rod is reused and the butt is cleaned and crushed for the paste.

**Decided with Tim on 2026-11-03:** with a rodding shop and butt recycle.

**Sources:**
- Prebake anode paste is about 65 % petroleum coke, 15-20 % recycled butts and 13-16 % coal-tar pitch. Baked density is
  1.55-1.60 t/m³. A ring furnace takes 2-3 GJ of fuel a t of anodes, firing over 2-3 weeks (Fischer and Perruchoud, *Anodes for
  the Aluminum Industry*, 1995; Sadler, Light Metals).
- A 180 kA prebake pot carries about 18 anodes, each about 1.6 x 0.8 x 0.6 m, and changes each after about 26-30 days. About
  20-25 % comes back as butt.
- Cast iron sets the steel stubs in their holes; most of it is recovered when the butt is stripped.

A figure marked *(proposed)* is an estimate or a proposal.

## Size: 25 anodes a day *(proposed values)*
That is enough for 40 pots of 18 anodes, each lasting 29 days (pots: potline.md, 10b3b).

| Item | Value | How |
|---|---|---|
| Baked anode | **1,213 kg**, 1.6 x 0.8 x 0.6 m | 0.768 m³ at 1.58 t/m³ |
| Green anode | **1,275 kg** | 5 % of its weight, the pitch's volatiles, burns off in baking |
| Paste for a green anode | 829 kg of calcined coke, 255 kg of crushed butts, 191 kg of pitch | 65 / 20 / 15 % |
| Butt left after 29 days | **303 kg**, a quarter of the anode | |
| At full rate | 20.7 t of calcined coke, 6.4 t of butts and 4.8 t of pitch a day | 25 anodes |
| Baking fuel | **2.4 MJ/kg** of baked anode, 2.9 GJ an anode; about 0.85 MW of gas at full rate | |
| Rodding | **1 iron ingot** a rodding, for the thimbles; at most 60 anodes an hour | |

**Surplus butts:** a butt is 303 kg and the paste takes 255 kg an anode. The extra butts stay in the rodding shop's outlet as
crushed butts, to bag or store.

## Parts (cells W x H x L; each a true-size big part; all values *(proposed)*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `paste_plant` | 5 x 10 x 6 | Crushing and screening, the dry-aggregate preheater and a continuous heated mixer at about 160 °C: **200 kW at 4.16 kV**.<br>• Calcined coke bags and crushed butts by a hopper on top.<br>• Pitch in at the east face of its bottom cells.<br>• Paste to the forming press it touches. |
| `forming_press` | 4 x 5 x 4 | A vibro-compactor with its mould table: **110 kW at 480 V**.<br>• It presses a green anode from each 1,275 kg of paste.<br>• Green anodes go out to a hopper under it, or to an empty hand. |
| `baking_furnace` | 10 x 5 x 24 | An open-top ring baking furnace of about 16 sections. Its fire runs from section to section under its burner ramps to about 1,100 °C; exhaust fan **75 kW at 480 V**.<br>• Green anodes by a hopper on top.<br>• Gas, coker gas, coke oven gas or LPG in at the east face of its bottom cells.<br>• Baked anodes go out to a hopper under it, or to an empty hand. |
| `rodding_shop` | 4 x 5 x 12 | The rodding shop: the butt stripper and cleaning station, the butt crusher, the rod line and an induction furnace for the cast iron: **200 kW at 480 V**.<br>• By a hopper on top: baked anodes, anode rods, iron ingots and spent anodes.<br>• It strips each spent anode into its rod, which goes back on the rod line, and **crushed butts** (303 kg).<br>• It casts a rodded anode from each baked anode, rod and iron ingot.<br>• Rodded anodes and crushed butts go out to a hopper under it, or to an empty hand. |

## Items
- **Green anode, baked anode, rodded anode, spent anode** (the butt still on its rod). They stack to 16.
- **Crushed butts:** a butt's worth, 303 kg. Stacks to 16.
- **Anode rod:** an aluminium stem with a steel yoke and stubs. It is reused round and round.
  - Crafted from 3 copper ingots (the stem) over a steel ingot (the yoke).
  - Real rods are aluminium, but no aluminium exists before the first potline runs, and early smelters used copper. 10b3b adds an
    aluminium recipe.
- **Calcined coke bag**, 1,000 kg, from the coke cooler's bagging station (delayed-coker.md).

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Motor boxes:** on the west face of every part's main cell. The paste plant's is **4.16 kV**; the others are 480 V.
- **Pipes:** pitch in at the paste plant's east face, and fuel in at the furnace's east face, both at the bottom.
- **Touching:** the paste plant's back (south) end touches the forming press.
- **Hoppers:**
  - in from on top: the paste plant's coke bags and butts, the furnace's green anodes, the rodding shop's inputs;
  - out to a hopper under it: the press, the furnace and the rodding shop.

## How it runs
- **The paste plant** mixes paste up to the plant rate with a fifth more, while its motor runs, as far as its coke, butts and
  pitch allow. It holds up to two anodes' worth of paste for the press.
  - With no crushed butts it reads SHORT OF BUTTS and makes paste of coke and pitch only, the butt share made up with coke.
  - A new plant has no butts until the first anodes come back from the pots.
- **The forming press** presses an anode each time the paste plant holds 1,275 kg.
- **The furnace** bakes the green anodes it holds at up to the plant rate, scaled by its fuel. It holds at most 16 green anodes
  in its pits.
- **The rodding shop** works one anode a minute while its motor runs:
  - it strips a spent anode first, so its rod is free;
  - otherwise it rods a baked anode, if it has a rod and an iron ingot.
- **What each part reads when it stops:**
  - NO COKE, NO PITCH, NO PASTE, NO ANODES, NO RODS, NO IRON and NO FUEL;
  - NOT BUILT (set it against the X);
  - FULL: take the anodes.
- The 14-day firing is not modelled: the furnace bakes at its steady rate.

## Left out (ROADMAP Ideas)
- Anode quality: density, air and CO2 reactivity, cracks.
- The fume treatment of the furnace's pitch volatiles.
- Cast iron recovery from the thimbles. The 1 ingot a rodding is the net use.
- Bath recovered from the butts.
- Rod repair (straightening, stub replacement).

## Game test
`anodes` (z near −5900):
- **Power:**
  - a diesel set's 480 V on a busway;
  - a second set through a backward unit substation for the paste plant.
- **Feeds:**
  - calcined coke bags and crushed butts by hopper;
  - a stock tank of pitch;
  - an LPG bullet to the furnace;
  - anode rods, iron ingots and a spent anode by hopper to the rodding shop.
- **Checks:**
  1. The paste plant mixes and the press drops a green anode. The paste plant is first given almost an anode's paste.
  2. The furnace, given almost a baked anode, drops a baked anode, which is carried to the rodding shop's hopper.
  3. The rodding shop, its minute's work mostly done, drops a rodded anode.
  4. The spent anode comes back as crushed butts.
- **Screenshot:** the anode plant.
