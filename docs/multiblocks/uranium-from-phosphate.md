# Uranium from phosphoric acid

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **18b**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 57-59). Depends on the fertilizer plant (fertilizer.md, 18a) and the ADU calciner
> (adu-yellowcake.md, 16a).

Phosphate rock carries uranium (ores.md: 70-200 ppm real, **1,300 ppm** in the game, which the scintillometer hears). Most of it
dissolves into the wet-process phosphoric acid. From the late 1970s to the 1990s, Florida and Louisiana plants recovered it by
solvent extraction: the **DEPA-TOPO process** of Oak Ridge. This spec puts that on the fertilizer plant's filter acid: a small, steady
uranium by-product that needs no uranium mine.

Sources:
- The DEPA-TOPO process: a first extraction with DEPA-TOPO, reductive stripping with iron(II)-bearing phosphoric acid, re-extraction
  and stripping with dilute ammonium carbonate ([OSTI 5747921](https://www.osti.gov/biblio/5747921)); 0.5 M DEPA with 0.125 M TOPO in
  n-dodecane against 5-7 M H3PO4; an extract of about 0.3 g of U(VI) a litre; **five US plants recovered about 1,500 t of U3O8 a year**
  ([US 4,500,493](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/4500493);
  [US 4,302,427](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/4302427)).
- Beltrami et al., *Recovery of uranium from wet phosphoric acid by solvent extraction processes* (2014 review,
  [OSTI 1185453](https://www.osti.gov/pages/biblio/1185453)).
- Real acid carries about **0.1-0.2 g of U a litre** and about 70-90 % of the rock's uranium reaches it *(unverified, to check against
  Beltrami)*.

## Numbers *(derived from fertilizer.md and ores.md unless marked)*
- **Uranium in:** 70 t of rock a day at 1,300 ppm is **91 kg of U a day**; **80 %** reaches the filter acid *(proposed)*: 73 kg/day,
  3.0 kg/h.
- **In the acid:** 0.82 kg/s of filter acid, about 0.63 L/s at 1.3 kg/L: about **1.3 g of U a litre** (ten times real, like every
  game grade).
- **Recovered:** **90 %** *(proposed; the DEPA-TOPO literature's range, unverified)*: **2.7 kg of U an hour**, 3.2 kg of U3O8.
- **A drum** of ADU-route yellowcake (326 kg of U) in about **5 days**. Real plants were by-product plants too: the uranium paid for
  itself, not the plant.
- **Reagents:**
  - DEPA and TOPO in kerosene: a first charge of **5,000 L** *(proposed)*, then **0.05 L a m3** of acid *(proposed)*. DEPA (D2EHPA)
    and TOPO are made from **2-ethylhexanol and phosphorus chemistry**; the mod has neither: an **extractant drum** item is crafted
    from merchant phosphoric acid, LPG and iron *(proposed stand-in)*.
  - **Iron(II)** for the reductive strip: **iron ingots**, dissolved in the strip acid, **0.5 kg a kg of U** *(proposed)*.
  - **Ammonium carbonate** for the second strip, made in line from **ammonia and CO2** (the ammonia plant; the ISR's CO2 liquefier,
    isr-operations.md): **0.4 kg of NH3 and 0.5 kg of CO2 a kg of U** *(proposed)*.
- **The acid goes on:** the plant hands the acid back on its outlet, cooled and with its organic removed, to the evaporator; the
  fertilizer plant loses nothing but the uranium.

## Parts
Voltages are the world's standard tiers ("LV" 480 V or 400 V).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `acid_pretreatment` | 3 x 6 x 6 | **New. Controller.** Clarifier, cooler (to about 40 C) and oxidiser (air or hydrogen peroxide; here air) that make the filter acid's uranium U(VI) and take out its solids and humic matter. **45 kW at LV.** Filter acid in on its north face. |
| `depa_topo_circuit` | 6 x 3 x 16 | **New.** Two cycles of mixer-settlers: the first extracts from the acid and is stripped with iron(II) acid; the second re-extracts and is stripped with ammonium carbonate. **60 kW at LV.** Kerosene and the extractant by pipe and hopper; iron by hopper; ammonia and CO2 on its east face. **Treated acid** (filter acid) out of its south face back to the evaporator. Its **AUC slurry** (ammonium uranyl carbonate) goes to the calciner it touches. |
| `uranium_calciner` | 3 x 8 x 3 | **Existing** (adu-yellowcake.md). AUC calcines to U3O8 as ADU does. |
| `drum_filler` | 2 x 4 x 2 | **Existing** (uranium-mill.md): drums of "U3O8, 96 %". |

## Layout (facing north, plan view; the pretreatment's main cell at 0,0,0)
Layer y 0:
```
x:  0 1 2 3 4 5 6 7 8 ... 22
z0  T T T X X X X X X ... X        T acid pretreatment (y 0-5)     X DEPA-TOPO circuit (6 x 3 x 16, running east)
z1  T T T X X X X X X ... X
z2  T T T X X X X X X ... X
z3  T T T X X X X X X ... X        K calciner (y 0-7) against the circuit's east end, D drum filler against the calciner
z4  T T T X X X X X X ... X K K K D D
z5  T T T X X X X X X ... X K K K D D
```
The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `T` | `wildcatter:acid_pretreatment` |
| `X` | `wildcatter:depa_topo_circuit` |
| `K` | `wildcatter:uranium_calciner` |
| `D` | `wildcatter:drum_filler` |

## Controller, rotation, forming, removing
- **Controller:** the pretreatment; it finds the circuit by touching, and the circuit its calciner. Any rotation.
- **Removing the circuit:** the acid passes straight through the pretreatment to the evaporator (the fertilizer plant runs on).
- **Readouts:** U in, g/L, recovery; NO EXTRACTANT, NO IRON, NO AMMONIA, NO CO2, FILLING (first charge).

## Left out
- The OPAP (octylphenyl acid phosphate) process; ion exchange from phosphoric acid.
- Recovering the uranium from the gypsum (the 20 % the plant loses).

## Game tests
- `phosphateUranium`: the `phosphoric` scene's filter acid through the pretreatment and circuit (first charge and reagents by hook and
  test tanks); the calciner and filler fill a drum that a hook has brought near full; the treated acid reaches the evaporator; with the
  ammonia cut, NO AMMONIA.
- **Screenshot:** the circuit beside the phosphoric acid plant.

## Proposed wave
**18b**, after 18a and 16a.
