# The nickel smelter, the nickel refinery and the alloy melt shop

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **17b** (17b1 smelter and
> refinery; 17b2 the alloy melt shop). Decisions are in `docs/questions/specs-wildcatter.md` (items 47-51). Depends on 17a's furnace
> transformer (ferroalloys.md).

The 7a nickel ore (Sudbury pentlandite) has no plant, and the fuel plant uses **steel** for the 304 stainless nozzles and the Inconel
end grids and springs "until a nickel smelter exists" (fuel.md). This spec is that smelter, the refinery after it, and a small melt
shop that makes **304 stainless** and a **nickel-chromium alloy** from Wildcatter's own nickel and ferroalloys.

The flowsheet follows Sudbury's Xstrata (now Glencore) smelter and the Falconbridge Nikkelverk refinery in Norway:
- **roast** the sulfides in a fluid bed, sending the SO2 to an acid plant;
- **smelt** the calcine in an **electric furnace** to a matte;
- **convert** it in a **Peirce-Smith converter** to Bessemer matte;
- **leach** the matte with **chlorine**, purify the liquor, and **electrowin** nickel, the cell's chlorine going back to the leach.

Sources:
- Xstrata Nickel's Sudbury smelter: "fluid bed roasting of Ni-Cu sulphide concentrate, followed by electric furnace reductive smelting
  and Peirce-Smith converting to produce a Bessemer matte for electro refining, along with sulphuric acid produced from smelter gas";
  about **67,000-75,000 t of nickel a year** in matte from 550,000 t of feed
  ([OneMine, *Advanced metallurgical modeling of Ni-Cu smelting at the Xstrata Nickel Sudbury Smelter*](https://onemine.org/documents/advanced-metallurgical-modeling-of-ni-cu-smelting-at-the-xstrata-nickel-sudbury-smelter)).
- Falconbridge's Sudbury acid plant: about **1,180 t a day** of 93 % acid; 70-75 % of the sulfur fixed as acid
  ([Falconbridge 40-F, 2002](https://www.sec.gov/Archives/edgar/data/0000948840/000104746903019189/a2110940zex-1.htm);
  [Northern Miner](https://northernminer.com/news/falconbridge-in-sudbury-the-fine-tuned-smelter/1000136006)).
- Nikkelverk's chlorine leach and nickel electrowinning; Faraday's 1.095 g of nickel an ampere-hour; chloride electrowinning at about
  3.5 V a cell *(textbook, unverified)*.
- Induction melting about 550-650 kWh/t; AOD decarburisation of stainless with oxygen and an inert gas *(textbook, unverified)*.

## Feed
- The ore's **60 % nickel sulfides** (ores.md) carry, *proposed*: **12 % Ni, 4 % Cu, 0.4 % Co**, the rest iron sulfides and rock: a
  concentrate grade. A block is about **4,000 kg** *(unverified density)*, **480 kg of Ni**. The roaster takes ore by hopper, ground in
  its own feed mill *(simplified)*.
- **Tailings' concentrate:** the roaster also takes thickened concentrate slurry carrying pentlandite in its assay (Phase 11), so a
  Tailings concentrator can feed it.

## Size *(proposed)*: 2 t of feed an hour, 230 kg of nickel an hour
- **Roaster:** 2 t/h; about **0.6 t of sulfur an hour** (30 % S, *unverified*), 70 % of it to the acid plant as SO2: **0.42 t/h of S**,
  enough for **1.3 t/h of acid**, about a fifth of the 7b acid plant (derived).
- **Electric furnace:** about **500 kWh a tonne** of calcine *(unverified)*: **1 MW**.
- **Bessemer matte:** 50 % Ni, 25 % Cu, 20 % S *(proposed)*, about 0.46 t/h.
- **Electrowinning:** 95 % recovery to **230 kg of Ni an hour**: at 95 % current efficiency, **220 kA·cell**, so **18 cells at 12.3 kA**
  and 3.5 V: **63 V, 0.78 MW DC** (derived).
- **By-products:** copper (as cathode copper, which the refinery casts into **vanilla copper ingots**, *proposed*), cobalt (counted as
  cobalt carbonate), slag (counted).

## Parts
Voltages are the world's standard tiers ("MV" 4.16 kV or 3.3 kV; "LV" 480 V or 400 V; "distribution" 12.47 kV or 11 kV).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `fluid_bed_roaster` | 4 x 10 x 4 | **New.** A fluid-bed roaster at about 700 C *(unverified)* with its feed mill, fluidizing blower (**110 kW at MV** *(proposed)*) and cyclones. Ore or concentrate by a hopper on top or slurry on its north face. Its off-gas goes on the pipe as **burner gas** to an acid plant's converter (as the moly roaster's, ferroalloys.md): NO ACID PLANT stops it. It hands calcine to the furnace it touches. |
| `nickel_electric_furnace` | 6 x 6 x 12 | **New. The smelter's controller.** A rectangular six-in-line electric furnace with Söderberg electrodes, **1 MW** from a `furnace_transformer` (ferroalloys.md) at a low tap. Furnace matte to the converter it touches; slag counted; electrode paste by hopper. |
| `furnace_transformer` | 4 x 5 x 5 | **Existing** (17a): against the electric furnace. |
| `peirce_smith_converter` | 5 x 5 x 10 | **New.** A horizontal converter drum with its tuyere line, blowing air through the matte to oxidise the iron: furnace matte in, Bessemer matte out by touching, its slag back to the furnace. Its **air blower, 250 kW at MV** *(proposed)*, is part of it. Its off-gas goes on the pipe as burner gas too. A blow is a batch of **2 hours** *(proposed)*. |
| `chlorine_leach_reactor` | 3 x 8 x 3 | **New. The refinery's controller.** Bessemer matte (as granules, handed by touching or by the 1 t bags of the converter's bagging station) leached with **chlorine** at about 110 C *(unverified)*. **30 kW at LV.** Chlorine on its east face; the tankhouse's anode chlorine comes back by touching. |
| `ni_purification` | 4 x 4 x 10 | **New.** Copper cementation and an amine solvent extraction for cobalt, with the iron removed by oxidation and precipitation *(textbook)*. Kerosene first charge 5,000 L *(proposed)*. **40 kW at LV.** |
| `nickel_tankhouse` | 6 x 4 x 14 | **New.** **18 chloride electrowinning cells in series** with titanium anode hoods collecting chlorine, **12.3 kA, 63 V DC**, on a DC bus from the conversion plant's `rectifier` (conversion.md, dc-bus.md). Cathode nickel out to a hopper under it as **nickel cathode** (1 t bags, through a bagging station). Its chlorine returns to the leach reactor it touches; make-up chlorine off the pipe. |

### The alloy melt shop (17b2)
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `induction_furnace` | 3 x 3 x 3 | **New. Controller.** A **2 t** coreless medium-frequency induction furnace with its power cabinet: **1.5 MW at MV, PF 0.95** *(proposed: the inverter's front end)*, about **600 kWh a tonne** *(unverified)*: 2.5 t/h melted. Charge by a hopper on top. |
| `aod_converter` | 3 x 6 x 3 | **New.** An argon-oxygen decarburisation vessel: oxygen (from the ISR's PSA `oxygen_generator`, isr-wellfield.md) and nitrogen (from the ammonia plant's `nitrogen_generator`) through side tuyeres, taking the 7 % carbon of HC ferrochrome down under **0.08 %**. Argon for the low-nitrogen finish is left out. **30 kW at LV.** |
| `ingot_caster` | 2 x 3 x 6 | **New.** Bottom-poured ingot moulds. **100 kg ingots**, as the fuel plant counts steel. |

**Grades** *(chemistry from the standards; recipes proposed)*:
| Grade | Chemistry | Charge for 1 t |
|---|---|---|
| **304 stainless** | 18 % Cr, 8 % Ni, 1-2 % Mn, under 0.08 % C, the rest Fe | 0.28 t HC FeCr, 0.08 t nickel cathode, 0.02 t FeMn, 0.62 t iron (ingots or scrap) |
| **316 stainless** | as 304 with 12 % Ni and 2.5 % Mo | adds 0.04 t FeMo and more nickel |
| **Nickel-chromium alloy (Alloy 600 type)** | 72 % Ni, 15.5 % Cr, 8 % Fe | 0.72 t nickel cathode, 0.24 t HC FeCr (decarburised) |
| **Alloy 718** | needs **4.75-5.5 % niobium**, which no Wildcatter ore gives | not made (questions file) |

## Layout (facing north, plan view)
### The smelter (the furnace's main cell at 0,0,0)
Layer y 0:
```
x:  -5-4-3-2-1 0 1 2 3 4 5 6 7 8 9 10 11 ... 16
z0   X X X X . E E E E E E E E E E E  E  P P P P P P P P P P      E electric furnace (6 x 6 x 12, running east)
z1   X X X X . E E E E E E E E E E E  E  P P P P P P P P P P      X furnace transformer, against its west face
z2   X X X X . E E E E E E E E E E E  E  P P P P P P P P P P      P Peirce-Smith converter (10 long), against its east end
z3   X X X X . E E E E E E E E E E E  E  P P P P P P P P P P
z4   X X X X . E E E E E E E E E E E  E  P P P P P P P P P P
z5   . R R R R E E E E E E E E E E E  E  . . . . . . . . . .      R fluid-bed roaster (4 x 10 x 4), against its south-west
```
### The refinery (the leach reactor's main cell at 0,0,0)
```
x:  0 1 2 3 4 5 ... 16
z0  L L L N N N ... N       L chlorine leach reactor (y 0-7)    N nickel tankhouse (6 x 4 x 14, east)
z1  L L L N N N ... N       U purification (4 x 4 x 10), against the leach reactor's south face
z2  L L L N N N ... N       Q rectifier against the tankhouse's east end (or DC bus bars to it)
z3  U U U U . .
```
### The melt shop
```
 I I I A A A C C        I induction furnace   A AOD converter   C ingot caster, each against the next
```
The grids are sketches; the game tests are the layout of record.

### Legend
| Char | Block id |
|---|---|
| `E` | `wildcatter:nickel_electric_furnace` |
| `X` | `wildcatter:furnace_transformer` |
| `P` | `wildcatter:peirce_smith_converter` |
| `R` | `wildcatter:fluid_bed_roaster` |
| `L` | `wildcatter:chlorine_leach_reactor` |
| `U` | `wildcatter:ni_purification` |
| `N` | `wildcatter:nickel_tankhouse` |
| `Q` | `wildcatter:rectifier` |
| `I` | `wildcatter:induction_furnace` |
| `A` | `wildcatter:aod_converter` |
| `C` | `wildcatter:ingot_caster` |

## Controllers, rotation, forming, removing
- Three controllers: the electric furnace (roaster, transformer, converter), the leach reactor (purification, tankhouse) and the
  induction furnace (AOD, caster). Each finds its parts by touching; any rotation; NOT BUILT names a missing part.
- **Removing:** the furnace's transformer: the furnace holds its bath and freezes after 4 hours (as ferroalloys.md). The converter: matte
  waits in the furnace (FULL). The tankhouse's DC: NO DC, and the leach stops when its chlorine is not coming back. Nothing is lost but
  what each part holds.

## How it runs
- Each stage takes what the one before it holds, as the mod's process lines do, with readouts naming what stops it: NO FEED, NO ACID
  PLANT, NO POWER, FULL, NO CHLORINE, NO DC, NO KEROSENE.
- **The converter** blows a batch every 2 hours while the furnace holds matte; its blower is a 250 kW MV start.
- **The tankhouse** is a DC load on the circuit; its nickel follows the current through it, as the magnesium cell's does.
- **The melt shop** melts a heat of 2 t when its charge for the chosen grade is in its hopper (sneak-click sets the grade), decarburises
  in the AOD and casts twenty 100 kg ingots.

## What it changes elsewhere
- **The fuel plant** (fuel-fabrication-extensions.md): the assembly fixture takes **304 stainless** ingots for its nozzles and
  **nickel-chromium alloy** ingots for its end grids and springs instead of steel.
- **Ores:** nickel ore's tooltip and JEI page name the smelter; cobalt ore stays "No use yet".

## Left out
- Flash smelting (Vale's Copper Cliff, Outotec); the Mond carbonyl process.
- Copper refining beyond casting the by-product into ingots; cobalt metal; precious metals from the anode slimes.
- Niobium, and so Alloy 718.

## Game tests
- `nickelSmelter`: an acid plant's converter on the roaster's and converter's off-gas pipe (test-filled), the furnace transformer on
  12.47 kV, ore by hopper:
  - the roaster makes calcine and burner gas; the furnace draws about **1 MW** and makes matte; the converter blows Bessemer matte;
  - with the acid plant's converter broken, the roaster reads NO ACID PLANT.
- `nickelRefinery`: Bessemer matte by hook, chlorine from a test-filled supply, a rectifier at 12.3 kA: the tankhouse reads about **63 V**
  and fills a bag of nickel cathode a hook has brought near full.
- `meltShop` (17b2): a 304 heat from FeCr, nickel, FeMn and iron by hopper; the AOD with oxygen and nitrogen; twenty 304 ingots in the
  hopper under the caster.
- **Screenshot:** the smelter.

## Proposed waves
- **17b1:** roaster, electric furnace, converter, leach, purification, tankhouse.
- **17b2:** the alloy melt shop, and the fuel plant's hardware (fuel-fabrication-extensions.md, item 3).

## As built
### 17b1 (2026-11-12, branch `w-17b`; decisions `docs/questions/phase17b-nickel.md`)
- Built as specified, with these differences and additions:
  - **Frames:** each part is drawn facing north with its length along z (the electric furnace 6 wide and 12 long, the tankhouse 6 by
    14); the scenes turn nothing. `nickel_smelter`: the transformer against the furnace's west side, the roaster against its south end,
    the converter against its east side; `nickel_refinery`: the leach reactor, the tankhouse against its south side, the purification
    circuit against the east sides of both, the rectifier against the tankhouse's south end.
  - **Chlorine** comes on the leach reactor's **north** face (the layout's tankhouse takes its east face; question 8). The tankhouse's
    terminals are on its south end; the purification circuit's motor box on its north face (question 12).
  - **Numbers added** (each in the questions file): a block of ore is 4 t of feed (1); concentrate counted kg for kg (2); calcine 1 t a
    tonne of feed (3); the furnace holds its own 1 MW at the transformer's PF 0.80 (4); furnace matte 25 % Ni, slag 0.7 t/t, paste
    2.5 kg/t, FULL at 10 t (5); a blow 0.5-4 t of furnace matte (6); converter off-gas 0.35 kg of S a kg of Ni (7); leach 0.5 t/h and
    the buffers (9); the cells' 1.6 V back-EMF (11).
  - **Readouts:** NO FEED, NO ACID PLANT, FULL, NOT BUILT (with the missing part), NO CHLORINE, NO KEROSENE, NO DC, FROZEN, ELECTRODES SHORT.
  - The converter's 2-hour blow goes through `TimeScale.scaled`. No existing world changes: every behaviour is in new parts; the
    furnace transformer finds the new furnace by touching and an SAF against it runs as before.
  - **Tests:** `nickel_smelter` (the roaster at 2 t/h, its gas made into acid; the furnace at 1.00 MW; a blow; the acid plant's converter
    broken: NO ACID PLANT) and `nickel_refinery` (12.3 kA at 63.0 V, 230 kg/h; a bag of cathode nickel; the rectifier broken: NO DC and
    the leach's NO CHLORINE). The blowers' MV comes from diesel and gas sets stepped up through unit substations (the 250 kW blower's
    start trips a 500 kW diesel set on frequency, so it is on a 1 MW gas set).
### 17b2 (2026-11-12, branch `w-17b`)
- Built as specified: the induction furnace (controller, 3 x 3 x 3, 1.5 MW at MV, PF 0.95, box west), the AOD converter (3 x 6 x 3,
  30 kW, box west) and the ingot caster (2 x 3 x 6), each against the next (the scene runs them along z). The grade is set by sneak-click
  on the furnace: 304, 316 or the nickel-chromium alloy. Items `stainless_304_ingot`, `stainless_316_ingot`, `nickel_chromium_ingot`.
- **Differences:** 316 takes 260 kg of FeCr a tonne, not 304's 280 (question 14); the nickel-chromium alloy adds 40 kg of iron to make a
  tonne (15); the AOD's nitrogen comes on its east face and its oxygen from PSA generators set against it; its gas numbers (17). The
  caster needs no power (18). Numbers in `fluid/MeltShop` (`MeltShopTest`).
- **Test:** `melt_shop`: a 304 heat by hopper melts at 1.50 MW; the AOD blows about 49 Nm3/h of oxygen and 16 of nitrogen; twenty 304
  ingots reach the hopper under the caster. The furnace's melt, the AOD's blow and the caster's pour are shortened by hooks.
- The fuel plant's hardware (fuel-fabrication-extensions.md, item 3) is built in 19c2.

