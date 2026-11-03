# Lime, AlF3, bags and chlorine (Phase 10b2, approved with the 10b2 plan 2026-11-03)

These parts serve the Bayer refinery (bayer-refinery.md) and the 10b3 potline:
- a **lime kiln** for the causticizer's quicklime;
- an **AlF3 reactor** for the pots' bath, on the conversion plant's anhydrous HF;
- a **bagging station** that fills 1 t bulk bags;
- a **chlorine filler** for the surplus chlorine from the chlor-alkali cells that make the refinery's caustic.

**Decided with Tim on 2026-11-03:**
- a new limestone rock in worldgen;
- 1 t bulk bags;
- the chlorine packed into 1-ton containers.

**Sources:**
- Lime: CaCO3 -> CaO + CO2 at about 900-1,000 °C, which is 0.560 kg of CaO a kg of CaCO3. Small vertical shaft kilns make
  10-100 t a day on 3.6-4.5 GJ a t of lime (EU BREF, *Cement, Lime and Magnesium Oxide*, 2013).
- AlF3, the dry fluid-bed process: Al(OH)3 + 3 HF -> AlF3 + 3 H2O at 500-600 °C, which is 0.715 kg of HF and 0.929 kg of
  hydrate a kg of AlF3 (the Buss / Lurgi process; Ullmann's, *Fluorine Compounds, Inorganic*).
- A prebake potline uses 15-25 kg of AlF3 a t of aluminium (IAI).
- Ton containers: 907 kg (2,000 lb) of liquid chlorine in a welded steel container (The Chlorine Institute, Pamphlet 17).
  Liquefying chlorine takes about 100 kWh a t (Euro Chlor).
- FIBC bulk bags: 500-2,000 kg of a free-flowing solid (ISO 21898).

A figure marked *(proposed)* is an estimate or a proposal.

## Limestone
- **The rock:** `wildcatter:limestone_ore` ("Limestone"). It drops a limestone item, one block's worth: **95 % CaCO3 at 2.6 t a m³,
  so 2,470 kg of CaCO3**, which gives **1,383 kg of CaO**.
- **Where:** thick beds as large veins (size 48, 1 a chunk, y 40 to 90) in plains, forests and rivers, replacing stone and dirt.
  This follows the 7a dolomite *(proposed)*.
- **Counterpart:** high-calcium limestone (Rogers City, Michigan; Buxton, England).

## Parts (cells W x H x L; true-size big parts; values *(proposed)*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `lime_kiln` | 3 x 14 x 3 | A small vertical shaft kiln, **10 t of quicklime a day** (0.116 kg/s); **4.0 GJ a t**, about 463 kW of gas or LPG at the east face of its bottom cells.<br>• Combustion-air blower **45 kW at 480 V**.<br>• Limestone by a hopper on top.<br>• Quicklime to the bagging station it touches.<br>• CO2 is counted. |
| `alf3_reactor` | 3 x 10 x 3 | A dry fluid-bed AlF3 reactor at about 550 °C, **50 kg of AlF3 an hour**, enough for 40 pots at 20 kg a t of aluminium.<br>• Hydrate bags by a hopper on top.<br>• Anhydrous HF and gas or LPG in at the east face of its bottom cells, each on its own run.<br>• **2.0 MJ a kg** of AlF3 to dry and heat the hydrate.<br>• Fluidizing fan **55 kW at 480 V**.<br>• AlF3 to the bagging station it touches. |
| `bagging_station` | 2 x 4 x 2 | An FIBC filling station; **5 kW at 480 V**.<br>• Empty bulk bags by a hopper on top.<br>• Each time it holds 1,000 kg from the part it touches, it fills a bag, which goes out to a hopper under it or to an empty hand.<br>• It fills alumina bags from a calciner, hydrate bags from a hydrate filter, AlF3 bags from an AlF3 reactor, and quicklime bags from a lime kiln.<br>• Set it against one of them. |
| `chlorine_filler` | 3 x 4 x 3 | A chlorine liquefier (refrigeration compressor **30 kW at 480 V**) and ton-container filling, up to **360 kg an hour**.<br>• Chlorine in at the north face of its front cells.<br>• Empty ton containers by a hopper on top.<br>• Full ones go out to a hopper under it, or to an empty hand. |

## Items
- **Empty bulk bag:** woven polypropylene FIBC. It is crafted from string and white wool, which stand in for woven polypropylene
  *(proposed)*.
- **Bags of 1,000 kg each:** alumina bags, hydrate bags, AlF3 bags and quicklime bags. They stack to 16.
- **Ton containers:** an empty chlorine ton container (steel, about 680 kg tare) and a full one (907 kg of Cl2). The full one
  stacks to 1.
- **Red mud:** a block of 1,800 kg of filter cake, for a dry stack (bayer-refinery.md).

## The chlor-alkali cell's caustic (changes zirconium.md)
- The cell's NaOH, 1.128 kg a kg of Cl2, now leaves as **32 % caustic soda** (3.53 kg a kg of Cl2) out of the **west face of its
  back cells (z 5, bottom)**.
- It holds 500 kg, and like its chlorine, it stops when the caustic has nowhere to go.
- Caustic a saved cell had counted comes into that hold.
- A tank holds caustic soda.

## How it runs
- **The kiln and the reactor** run like the HF kiln. Each scales its output by the fuel it got against what it needs, and reads
  NO FUEL, NO LIMESTONE, NO HYDRATE or NO HF.
- **The bagging station** reads NO BAGS, or FULL when its outlet hopper is full.
- **The chlorine filler** reads NO CONTAINERS.

## Game test
`alf3Lime` (z near −5300):
1. A lime kiln on limestone and gas fills a quicklime bag under its bagging station. The kiln is first given almost a bag.
2. An AlF3 reactor on hydrate bags, an HF column's line (with the HF kiln on fluorspar, acid and gas) and gas fills an AlF3 bag
   under its bagging station.
3. A chlor-alkali cell on its rectifier pipes caustic to a tank and chlorine to a chlorine filler, which fills a ton container.
   The filler is first given almost a container's worth.
- **Screenshot:** the lime and AlF3 corner.
