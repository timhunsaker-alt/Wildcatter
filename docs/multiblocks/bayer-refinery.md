# The Bayer refinery (Phase 10b2, approved with the 10b2 plan 2026-11-03)

The 7a bauxite (gibbsite) becomes smelter-grade alumina for the 10b3 potline. The process:
- a wet ball mill grinds the bauxite into caustic liquor;
- low-temperature digestion at about 145 °C (gibbsite dissolves; the boehmite of other bauxites would need 240 °C);
- a flash train cools the slurry and heats the feed;
- a settler and washer take off the red mud, and a filter press dries it for a dry stack;
- polishing filters clear the liquor;
- seeded precipitation grows the gibbsite back out;
- a pan filter takes the hydrate, and a gas-suspension calciner burns it to alumina at about 1,000 °C;
- the spent liquor is evaporated, causticized with lime and sent back to the mill.

**Decided with Tim on 2026-11-03:**
- the size, 105 t a day of alumina, enough for a 40-pot 180 kA potline;
- the full flowsheet, each step its own part;
- process steam from a gas-fired package boiler;
- the red mud as filter cake for a dry stack;
- the products in 1 t bulk bags (alumina-plant.md).

**Sources:**
- Gibbsite Al(OH)3 is 65.4 % Al2O3, by mass of 2 Al(OH)3 = Al2O3 + 3 H2O.
- Low-temperature digestion: 140-150 °C. Liquor productivity: 70-90 kg Al2O3 a m³ (Hind, Bhargava and Grocott, *The surface
  chemistry of Bayer process solids*, 1999; the IAI Bayer process overview).
- Soda loss: 40-80 kg NaOH per t of alumina, most of it to desilication product in the mud. Lime: 30-50 kg CaO per t (IAI;
  Authier-Martin et al., JOM 2001).
- Energy, gibbsite refinery: about 2 t of steam per t of alumina for digestion and evaporation; 3.0-3.2 GJ per t in a
  gas-suspension calciner (IAI energy benchmarking).
- Red mud: 0.5-0.7 t dry per t of alumina for gibbsite bauxite. Dry stacking presses it to 65-75 % solids (Evans, *The history,
  challenges and new developments in the management of bauxite residue*, 2016).

A figure marked *(proposed)* is an estimate or a proposal.

```
bauxite (hopper) -> BALL MILL <- strong liquor --------------------------------------------------------------+
   -> bauxite slurry -> DIGESTER (process steam) = FLASH TANKS (touching) -> digested slurry -> SETTLER          |
        SETTLER overflow: green liquor --------+                                                                |
        SETTLER underflow: red mud slurry -> MUD WASHER (water) -> red mud slurry -> RESIDUE FILTER PRESS        |
        MUD WASHER overflow: green liquor -----+-> POLISHING FILTER -> polished liquor -> PRECIPITATOR            |
   -> hydrate slurry -> HYDRATE FILTER = CALCINER (touching; gas) = BAGGING STATION (touching) -> alumina bags |
        HYDRATE FILTER: spent liquor -> EVAPORATOR (process steam; caustic soda) = CAUSTICIZER (touching; lime)  |
        EVAPORATOR: strong liquor ----------------------------------------------------------------------------+
PACKAGE BOILER (gas or LPG, pumped water) -> process steam -> DIGESTER, EVAPORATOR
```
`=` means the parts touch. Every other link is a run of steel pipe.

## Size: 105 t of alumina a day *(proposed values; the size is Tim's)*
**The plant's rate:** 1.215 kg/s of alumina.

**Per kg of alumina** (all from the values below):
| Stream | kg | How |
|---|---|---|
| Bauxite | 2.057 | a block (2,100 kg, 80 % gibbsite) gives 1,680 × 0.654 × 0.93 = **1,021 kg** of alumina: a block every 14 min |
| Strong liquor to the mill | 16.25 | 1,000 / 80 kg a m³ × 1.30 kg/L |
| Bauxite slurry | 18.31 | bauxite plus liquor |
| Red mud (dry) | 0.526 | the bauxite less the 1.530 kg of gibbsite that dissolves |
| Mud slurry, settler underflow | 1.504 | at 35 % solids |
| Green liquor, settler overflow | 16.80 | the rest |
| Wash water (and the washer's overflow) | 2.00 | 2 m³ a t |
| Polished liquor, and hydrate slurry | 18.80 | both green liquors |
| Hydrate | 1.530 | Al(OH)3 back out |
| Spent liquor | 17.27 | |
| Caustic make-up | 0.1875 of 32 % caustic | 60 kg of NaOH a t |
| Quicklime | 0.040 | 40 kg of CaO a t |
| Water boiled off | 1.207 | spent liquor plus caustic, less strong liquor |
| Process steam | 1.98 | 1.5 at the digester (after the flash heat) plus 0.4 kg a kg boiled off in the 3-effect evaporator |
| Red mud cake | 0.752 | at 70 % solids; a block of red mud (1,800 kg) for every 2,394 kg of alumina |
| Calciner fuel | 3.0 MJ | about 79 L of gas a kg |

**At the full rate:**
- 2.5 kg/s of bauxite;
- 20 kg/s of strong liquor;
- 0.23 kg/s of 32 % caustic (820 kg/h, about 11 chlor-alkali cells);
- 175 kg/h of lime;
- 2.4 kg/s of steam;
- 3.6 MW of gas at the calciner.

**The liquor's first fill is waived *(proposed)*:**
- A real refinery is charged with tens of tonnes of NaOH before it starts.
- Here the evaporator's storage holds **10 minutes of strong liquor** when the evaporator is built. It tops up the mill while the
  loop fills, and banks what comes back over.
- Liquor lost from the loop, for example to a broken pipe, has to come back through the make-up.

## Parts (cells W x H x L; each a true-size big part; all values *(proposed)*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `ball_mill` | 4 x 4 x 8 | A wet overflow ball mill grinding bauxite in strong liquor, at about 10 kWh a t: **110 kW at 480 V**.<br>• Bauxite by a hopper on top.<br>• Strong liquor in at the north face of its front cells.<br>• Bauxite slurry out of the south face of its back cells. |
| `digester` | 5 x 8 x 5 | A steam-heated digester at 145 °C and about 0.45 MPa, about an hour of slurry; agitator **75 kW at 480 V**.<br>• Slurry in at the north face of its front cells.<br>• Process steam in at the east face of its bottom cells: **1.5 kg a kg of alumina**.<br>• It empties into the flash tanks it touches. |
| `flash_tanks` | 3 x 8 x 6 | A three-stage flash train down to atmospheric pressure. Its flash steam heats the mill's slurry, which is why the digester needs only 1.5 kg.<br>• Pump **37 kW at 480 V**.<br>• Digested slurry out of the south face of its back cells. |
| `settler` | 10 x 4 x 10 | A high-rate flocculated thickener; rake drive **7.5 kW at 480 V**.<br>• Digested slurry in at the north face of its front cells.<br>• Green liquor out of the south face of its back cells.<br>• Red mud slurry (35 % solids) out of the east face of its bottom cells. |
| `mud_washer` | 10 x 4 x 10 | A counter-current washing thickener washing the mud with water off a pump (**2 kg a kg of alumina**); rake **7.5 kW at 480 V**.<br>• Mud slurry in at the north face of its front cells.<br>• Its overflow, weak green liquor, out of the south face of its back cells.<br>• Washed mud out of the east face of its bottom cells. |
| `residue_filter_press` | 4 x 4 x 8 | A plate-and-frame press making 70 % solids cake for a dry stack; hydraulics **30 kW at 480 V**.<br>• Mud slurry in at the north face of its front cells.<br>• **Red mud** blocks (1,800 kg of cake each) out to a hopper under it, or to an empty hand. |
| `polishing_filter` | 3 x 5 x 6 | Pressure leaf (Kelly) filters taking the last fines out of the green liquor; feed pump **45 kW at 480 V**.<br>• Green liquor in at the north face of its front cells.<br>• Polished liquor out of the south face of its back cells. |
| `precipitator` | 12 x 18 x 12 | A 2,035 m³ draft-tube precipitator, seeded with fine hydrate and cooled from 75 to 60 °C over 36 h; agitator **75 kW at 480 V**.<br>• Polished liquor in at the north face of its front cells.<br>• Hydrate slurry out of the south face of its back cells. |
| `hydrate_filter` | 4 x 4 x 10 | A horizontal pan filter that washes the hydrate; vacuum pump **90 kW at 480 V**.<br>• Hydrate slurry in at the north face of its front cells.<br>• Spent liquor out of the east face of its bottom cells.<br>• Hydrate to the calciner it touches; the calciner stands at its back (south) end.<br>• **Sneak-click** sets the share sent to a touching bagging station as hydrate bags (for the AlF3 plant): 0, 10, 50 or 100 %. With no calciner against it, all of the hydrate goes to bags. |
| `calciner` | 6 x 20 x 6 | A gas-suspension calciner at about 1,000 °C: 2 Al(OH)3 -> Al2O3 + 3 H2O.<br>• **3.0 MJ a kg of alumina**, gas or LPG at the east face of its bottom cells.<br>• ID fan **250 kW at 4.16 kV**.<br>• Hydrate from the hydrate filter it touches; alumina to the bagging station it touches. |
| `evaporator` | 4 x 12 x 8 | A three-effect falling-film evaporator with the liquor storage; pumps **75 kW at 480 V**.<br>• Spent liquor in at the north face of its front cells.<br>• Process steam (**0.4 kg a kg boiled off**) and **32 % caustic soda** in at the east face of its bottom cells, each on its own run.<br>• Lime from the causticizer it touches.<br>• Strong liquor out of the south face of its back cells. |
| `causticizer` | 3 x 5 x 3 | A lime slaker and causticization tank: Na2CO3 + Ca(OH)2 -> 2 NaOH + CaCO3; slaker and agitator **15 kW at 480 V**.<br>• **Quicklime bags** by a hopper on top.<br>• It doses the evaporator it touches with 40 kg of CaO a t. |
| `package_boiler` | 4 x 5 x 9 | A 10 t/h package water-tube boiler, 10 bar saturated, 85 % efficient: about **2.75 MJ of gas a kg of steam** from 105 °C feed.<br>• Feed pump and FD fan **45 kW at 480 V**.<br>• Gas or LPG at the east face of its bottom cells.<br>• Water off a pump.<br>• Process steam out of the south face of its back cells. |

**Process steam:** a new pipe product (kg, a gas), separate from the reactor's main steam.

**Liquors and slurries:** pipe products in kg that no tank holds:
- bauxite slurry, digested slurry;
- green liquor, red mud slurry, polished liquor, hydrate slurry;
- spent liquor, strong liquor.

**Caustic soda:** 32 % NaOH in kg, which a tank holds.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Motor boxes:** on the west face of the main cell of every part. The calciner's is **4.16 kV**; the others are 480 V.
- **Pipe faces:**
  - in at the north face of the front cells (z 0, bottom);
  - out of the south face of the back cells (bottom);
  - the settler and washer's mud, and the hydrate filter's spent liquor, out of the east face of the bottom cells.
- **Steam, gas and caustic:** on the east face of the bottom cells. Where a part takes two of them (the evaporator), each goes on
  its own run, and the two runs must not touch.
- **Hoppers:**
  - in from on top: bauxite on the ball mill, quicklime bags on the causticizer;
  - out to a hopper under it: red mud from the filter press.
- **Water:** the mud washer and the package boiler take water off a pump.

## How it runs
- **Flow:** each part takes what its inlet run offers, up to the plant rate with a fifth more to catch up, while its motor runs.
  It holds about 5 s of its output at its nozzle.
- **Missing feeds:** a part reads what stops it: NO BAUXITE, NO LIQUOR, NO STEAM, NO WATER, NO FUEL, NO CAUSTIC, NO LIME,
  NOT BUILT (set it against the X), and "its output has nowhere to go".
- **Short supply:**
  - The evaporator returns strong liquor only as fast as its caustic and lime allow. Each scales its output by got / need, so the
    loop and the whole plant slow in proportion.
  - Steam short at the digester or evaporator, and fuel short at the calciner, do the same there.
- **Removing a part** stops the flow through it.

## Left out (ROADMAP Ideas)
- Boehmite and diaspore bauxites (high-temperature digestion).
- Silica and the desilication product (folded into the soda loss).
- Oxalate and organics removal.
- Seed recycle and the fine/coarse hydrate split.
- Condensate return to the boiler.
- Residue neutralisation and use.

## Game test
`bayer` (z near −5100):
- **Power:** diesel sets paralleled on a busway for 480 V, and through a backward unit substation to 4.16 kV for the calciner fan.
- **Feeds:**
  - bauxite by hopper;
  - water by hook;
  - a gas line to the boiler and calciner;
  - a stocked tank of 32 % caustic;
  - quicklime bags by hopper.
- **Checks:**
  - every part runs, in flow order;
  - alumina bags drop under the bagging station; to keep the test short, the calciner is first given almost a bag of alumina;
  - the filter press drops red mud, which places as a block;
  - with the caustic shut off, the evaporator slows and reads NO CAUSTIC.
- **Screenshot:** the refinery.
