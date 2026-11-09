# The purer yellowcake route: ADU precipitation and a calciner

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **16a**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 23-26). Depends on the ammonia plant (ammonia-plant.md, approved 2026-11-09).

The uranium mill (uranium-mill.md) precipitates with magnesia and spray-dries a **magnesium diuranate at about 80 % U3O8**. Most real
mills precipitate **ammonium diuranate (ADU)** with ammonia and **calcine** it at 750-800 C to **U3O8 at 94-98 %**. The mill spec left
this for later, and the ammonia plant names it as one of its users. This spec adds it: the precipitation tank takes ammonia as its
reagent, and a **calciner** takes the dryer's place.

Sources:
- AAEC pilot plant work on ADU: precipitation with ammonia at **pH 7.5 and 50 C** gives a filterable precipitate without stringent
  control; pH 7.2-8.0 at 50 C was used ([ANSTO repository, *Pilot plant development of processes for the production of ammonium
  diuranate*](https://apo.ansto.gov.au/items/aeb3b687-ab5c-76a5-e053-150a9d89ded9/full)).
- Calcining a uranium concentrate in one step at **600-900 C, 750 C for about an hour**, gives U3O8 powder (patent US 8,628,733,
  [USPTO](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/8628733)).
- Heating ADU gives U3O8 ([RSC Education](https://edu.rsc.org/download?ac=15883)).
- uranium-mill.md (approved): ADU and a calciner give **94-98 %**; ASTM C967 asks for at least 65 % U.

## Numbers *(proposed unless marked)*
- **Ammonia:** **0.2 kg of NH3 a kg of U3O8**: 0.072 is the ADU itself (one NH3 a U), the rest neutralises the strip liquor's
  free acid *(proposed, unverified)*.
- **The catch:** the ammonia plant makes **15.2 kg/h**, enough for **76 kg of U3O8 an hour**. That is all of an ISR wellfield (six
  patterns make about 27 kg/h, isr-wellfield.md) but only about 5 % of the mill at full rate (about 1,400 kg/h). The tank precipitates
  as fast as its ammonia allows; a mill that wants all of its output by ADU needs a bigger ammonia supply (questions file).
- **Product:** **96 % U3O8** in the same 400 kg net drum: **384 kg of U3O8, 326 kg of U** (the magnesia drum holds 271 kg of U).
- **Downstream it is cleaner** (conversion.md):
  - the reducer: U3O8 + 2 H2 -> 3 UO2, **5.65 g of hydrogen a kg of U** (derived) against 8.47 for the U(VI) diuranate;
  - the hydrofluorinator: **no 0.084 kg of HF a kg of U for magnesia**; its 0.43 kg stays;
  - the fluorinator: about a tenth of the ash *(proposed: the 4 % that is not U3O8)*.
- **By-product:** ammonium chloride (from the salt strip) or ammonium sulfate (from ISR eluate) in the barren liquor, counted.

## Parts
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `precipitation_tank` | 4 x 5 x 4 | **Existing**, with a second reagent: **ammonia** on a new nozzle (the east face of its bottom cells). Ammonia in: ADU at pH 7.5, 50 C (its 15 kW agitator unchanged). Without ammonia but with magnesia: magnesium diuranate as today. It never mixes the two in one batch: a batch is what it held when its reagent changed. |
| `uranium_calciner` | 3 x 8 x 3 | **New.** A multiple-hearth calciner, six hearths with a rabble arm, at **750-800 C**, fired on pipeline gas or LPG at **0.6 MW** *(proposed)*; its rabble drive and off-gas fan **15 kW at 480 V** *(unverified)*. Its off-gas (ammonia and water) goes to a scrubber, counted. It stands where the dryer stands, touching the tank and the drum filler. Up to **0.45 kg of U3O8 a second**, the mill's full rate. |
| `yellowcake_dryer` | 3 x 8 x 3 | **Existing**: still dries magnesium diuranate. With a calciner touching the tank and filler instead, the line makes ADU-route drums. |
| `drum_filler` | 2 x 4 x 2 | **Existing.** The drum item now carries its **grade**: "magnesium diuranate, 80 % U3O8" or "U3O8, 96 %", and its kg of U. |

## Layout (facing north, plan view; the precipitation tank's main cell at 0,0,0)
Layer y 0:
```
x:  0 1 2 3 4 5 6 7 8
z0  P P P P K K K D D      P precipitation tank (y 0-4)  K calciner (y 0-7)  D drum filler (y 0-3)
z1  P P P P K K K D D
z2  P P P P K K K . .
z3  P P P P . . . . .
```
Layer y 1-4: the tank and calciner continue; the drum filler's hopper on its top at y 4. Layers y 5-7: the calciner's upper hearths
and its off-gas stack.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `P` | `wildcatter:precipitation_tank` |
| `K` | `wildcatter:uranium_calciner` |
| `D` | `wildcatter:drum_filler` |

## Controller, rotation, forming, removing
- As the mill's precipitation unit: the three parts form a unit by touching; the tank is the unit's controller. Any rotation.
- The tank reads which finisher it has: **DRYER** (magnesia route) or **CALCINER** (ADU route). With both touching, the calciner wins
  for ADU batches and the dryer for magnesia batches.
- **Removing the calciner:** ADU waits in the tank (**NO CALCINER**).
- **Removing the ammonia:** the tank finishes its batch and reads **NO AMMONIA**; with magnesia in its hopper it goes back to the
  magnesia route.

## How it runs
- The tank takes strip liquor (or ISR eluate) and its reagent; it precipitates as fast as the reagent and its rated U3O8 rate allow.
- The calciner takes the wet ADU, fires to temperature (**5 min from cold**, *proposed*), and hands U3O8 to the filler.
- The filler fills a 400 kg drum at the grade of what it holds.
- The conversion plant's reducer, hydrofluorinator and fluorinator use each drum's grade (the numbers above).

## Left out
- **Hydrogen peroxide precipitation** (uranyl peroxide), which real Wyoming ISR plants use, dried at low temperature.
- Recovering the ammonia from the calciner's off-gas and the barren liquor.
- ASTM C967 impurity penalties (the converter's surcharges for excess Mo, V, B and so on).

## Game tests
- `aduYellowcake`: the mill's precipitation unit with a calciner; ammonia from a test-filled pressurised tank; strip liquor by hook.
  - The drum filler fills a drum of "U3O8, 96 %" holding 326 kg of U (a hook brings it near full, as the mill's test does).
  - With ammonia cut, NO AMMONIA; magnesia then in its hopper, and a dryer against it, gives an 80 % drum.
  - In the `conversion` scene, an ADU drum in the reducer draws **5.65 g of hydrogen a kg of U** (± 5 %) and the hydrofluorinator no
    magnesia HF.
- **Screenshot:** the calciner.

## Proposed wave
**16a**, once the ammonia plant is built (Tailings' P3 brings it).
