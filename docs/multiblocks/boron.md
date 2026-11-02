# The boron plant: boric acid and boron carbide control rods (APPROVED 2026-11-01 with the Phase 8a plan)

Borax ore (ores.md) feeds two lines:
- **Boric acid** for the reactor's coolant (reactor.md, the CVCS).
- **Boron carbide** for its control rods: RCCAs of 24 B4C rodlets, as in Combustion Engineering PWRs.

Decided with Tim on 2026-11-01: boric acid and rods both come from borax. Boron carbide was chosen over hafnium, which would take about
1,400 h of the 7f line for one set of rods, and over silver-indium-cadmium.

Sources:
- **Boric acid:** Na2B4O7·10H2O + H2SO4 -> 4 H3BO3 + Na2SO4 + 5 H2O at about 90 °C, then cooling crystallization, a centrifuge and a
  dryer (US Borax / Rio Tinto Boron practice).
- **Boron carbide:** 2 B2O3 + 7 C -> B4C + 6 CO by carbothermic reduction in an electric resistance (Acheson-type) furnace at about
  2,000 °C. The boric acid is first dried to B2O3. About 30 kWh a kg *(unverified)*.
- **The rodlet:** B4C pellets at 70 % of theoretical density (2.52 g/cm³), 8.66 mm across in a 3.6 m stack *(textbook, unverified)*.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

```
borax (hopper) -> BORAX DISSOLVER -> BORIC ACID CRYSTALLIZER -> bags of boric acid (hopper under)
                  ^ H2SO4 pipe, water                              |
                                                                   v (by hopper)
               boric acid + coal (hopper) -> B4C FURNACE -> trays of B4C pellets -> RCCA BENCH (steel) -> RCCA
```

## Numbers
- **Boric acid, per kg:** 1.54 kg of borax and 0.40 kg of H2SO4 (0.22 L of 98 % acid), plus 0.30 L of water for the liquor *(proposed)*; 0.57 kg of Na2SO4 counted.
  - A borax ore item holds 1,680 kg of borax (ores.md), about 1,090 kg of boric acid.
- **B4C, per kg:** 4.48 kg of boric acid (2.52 kg of B2O3) and 1.52 kg of carbon from coal (a piece of coal is 150 kg at 80 % carbon, as in zirconium.md); 3.04 kg of CO counted.
- **Rates *(proposed)*:**
  - **50 kg of boric acid an hour**: a 25 kg bag every 30 min;
  - **5 kg of B4C an hour**: a 1 kg tray every 12 min.
- **A rodlet:** 0.589 cm² × 360 cm × 1.76 g/cm³ = **0.37 kg** of B4C.
- **An RCCA:** 24 rodlets, **9 kg of B4C** (9 trays), and **20 kg of steel** *(unverified)* for the rodlet cladding, the end plugs and the spider. Steel stands in for 304 stainless and Inconel, as in fuel.md.
  - An ingot is 100 kg *(proposed in 7g)*.
  - The bench builds one in **1 h** *(proposed)*.

## Parts (cells W x H x L, sizes and watts *proposed*; each against the next)
| Part | Cells | Real basis |
|---|---|---|
| `borax_dissolver` | 3 x 4 x 3 | An agitated, steam-heated tank at 90 °C.<br>• Borax by a hopper on top; sulfuric acid off the pipe at the east face of its bottom cells; water off a pump.<br>• **30 kW at 480 V**. |
| `boric_acid_crystallizer` | 3 x 6 x 3 | A cooling crystallizer, a pusher centrifuge and a dryer, against the dissolver.<br>• **25 kg bags of boric acid** out to a hopper under it.<br>• **40 kW at 480 V**. |
| `b4c_furnace` | 3 x 4 x 6 | An Acheson-type resistance furnace with its furnace transformer and the pellet press.<br>• Boric acid bags and coal by a hopper on top.<br>• **1 kg trays of B4C pellets** out to a hopper under it.<br>• **150 kW at 480 V**. |
| `rcca_bench` | 3 x 3 x 8 | It loads 9 kg of pellets into 24 steel rodlets, welds their end plugs and mounts them on the spider.<br>• Trays and steel by a hopper on top.<br>• The **RCCA** out to a hopper under it.<br>• **30 kW at 480 V**. |

The furnace and the bench stand on their own and are fed by hopper.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **480 V boxes:** the west face of each part's main cell.
- **Sulfuric acid:** in on the east face of the dissolver's bottom cells.
- **Water:** the dissolver takes it off a pump.
- **Hoppers:**
  - on top of: the dissolver (borax), the furnace (boric acid and coal), the bench (trays and steel);
  - under: the crystallizer (bags), the furnace (trays), the bench (RCCAs).

## How it runs
- **The dissolver** takes borax, acid and water at the line rate while its motor runs. It holds up to 100 kg of dissolved boric acid.
- **The crystallizer** pulls that liquor from the dissolver it touches. It bags every 25 kg.
- **What stops a part** (its readout names it):
  - NO BORAX, NO ACID, NO WATER;
  - NO BORIC ACID, NO COAL;
  - NO PELLETS, NO STEEL;
  - FULL, when the hopper under it is full;
  - NOT BUILT, for the crystallizer with no dissolver against it.

## Game test
`boron`:
- **Feeds:**
  - borax by hopper, sulfuric acid from a tank by pipe, water off a pump;
  - boric acid and coal into the furnace by hopper;
  - trays and steel into the bench.
- **Checks** (the hooks shorten the waits):
  - the dissolver runs and the crystallizer drops a bag of boric acid;
  - the furnace drops a tray of B4C;
  - the bench drops an RCCA.
- **Power:** a 480 V diesel set on a busway.

## Added while building (8a)
- **Game test:** two diesel sets on a busway, as in the fuel scene. Each part's stub is 500 kcmil: the furnace's 150 kW start burns
  a 2 AWG stub.
