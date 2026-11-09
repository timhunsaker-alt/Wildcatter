# Fertilizer: phosphoric acid, TSP and DAP from phosphate rock; muriate of potash from potash

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **18a**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 52-56).

The 7a phosphate rock and potash have no plant. A real fertilizer complex sits on a sulfuric acid plant: it attacks phosphate rock
with the acid to make **phosphoric acid** and a mountain of **phosphogypsum**, then granulates the acid into **TSP** (with more rock) or
**DAP** (with ammonia). Potash is dissolved and recrystallized as **muriate of potash** (KCl). The phosphoric acid is also where
uranium from phosphate comes from (uranium-from-phosphate.md), and the bags are what Farmings' crops can use.

Sources:
- Dihydrate wet-process acid: precipitation at **25-30 % P2O5 and 70-80 C**; **4-5 t of phosphogypsum a tonne of P2O5** (4.5-5.5 t dry
  by one patent); 4-6 % P2O5 losses, mostly co-crystallized in the gypsum
  ([Grzmil, *Polish Journal of Chemical Technology* 2012](https://yadda.icm.edu.pl/baztech/element/bwmeta1.element.baztech-article-BPS2-0065-0049/c/Polish_Journal_of_Chemical_Technology_2_2012_Grzmil.pdf);
  [US 4,452,770](https://patents.google.com/patent/US4452770/en); [FIPR](https://fipr.floridapoly.edu/wp-content/uploads/2014/12/01-002-001Final.pdf)).
- Commercial acid from Florida rock runs **28-30 % P2O5** ([US 4,302,427](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/4302427)).
- Sulfuric acid about **2.6-2.8 t a tonne of P2O5**; merchant acid at 52-54 % P2O5; DAP 18-46-0 and TSP 0-46-0; potash by hot leaching
  and vacuum crystallization to 60 % K2O *(textbook, unverified)*.
- ores.md: phosphate rock at 80 % francolite (real 18-40 % P2O5); potash at 80 % KCl (real 20-30 % K2O).

## Size *(proposed)*: 20 t of P2O5 a day, 5 t of potash an hour
- **Rock:** 80 % francolite at about 37 % P2O5 *(unverified)* is **30 % P2O5**; with 95 % recovery, **70 t of rock a day**, about
  **0.81 kg/s**. A block of about 2,900 kg *(unverified density)* lasts about an hour.
- **Acid:** **2.7 t of H2SO4 a tonne of P2O5** *(unverified)*, **54 t a day, 0.63 kg/s**: about a third of the 7b acid plant.
- **Phosphogypsum:** **4.5 t a tonne of P2O5**, 90 t a day, as a slurry to a lined cell.
- **Filter acid:** 28 % P2O5, about **71 t a day** (0.82 kg/s), most of it concentrated to **54 % merchant acid** (0.43 kg/s).
- **Granules:** either **TSP**, 0-46-0, from rock and acid (no ammonia), or **DAP**, 18-46-0, from acid and **0.22 t of ammonia a
  tonne** (derived). The ammonia plant's **15.2 kg/h** makes only **69 kg of DAP an hour**: TSP is the main product until there is more
  ammonia (questions file).
- **Potash:** **5 t/h of MOP**, 60 % K2O (95 % KCl): about **6.2 t/h of ore** at 80 % KCl and 95 % recovery, about **2 blocks an hour**
  *(unverified block mass)*.

## Parts
Voltages are the world's standard tiers ("MV" 4.16 kV or 3.3 kV; "LV" 480 V or 400 V).

### Phosphoric acid and granules
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `phosphate_mill` | 3 x 4 x 6 | **New.** A wet ball mill grinding rock to about 150 µm *(unverified)*: **200 kW at MV**. Rock by a hopper on top; water off a pipe; **rock slurry** (a new pipe product, kg) out of its south face. |
| `phosphoric_reactor` | 8 x 6 x 8 | **New. Controller.** A single-tank dihydrate attack reactor with its agitators and flash cooler, at 75 C: rock slurry on its north face, sulfuric acid on its east face. **150 kW at LV.** It hands its slurry to the filter it touches. |
| `tilting_pan_filter` | 6 x 4 x 10 | **New.** A tilting-pan vacuum filter with three washes: **filter acid** (28 % P2O5, a new pipe product, L) out of its south face; gypsum out of its west face as **tails slurry** with a gypsum assay (Phase 11's assayed slurry) to a lined tailings cell (tailings-cell.md). Its vacuum pump **250 kW at MV** *(proposed)*. |
| `acid_evaporator` | 3 x 10 x 3 | **New.** A forced-circulation vacuum evaporator concentrating filter acid to **54 % merchant acid** (a new pipe product, L). Heated by **process steam** (the Bayer refinery's package boiler, bayer-refinery.md): about **2 t of steam a tonne of P2O5** *(unverified)*. Circulation pump **75 kW at LV**. Its fluorine off-gas (SiF4) is counted. |
| `granulation_plant` | 4 x 5 x 12 | **New.** A pre-neutraliser, a rotating granulation drum, a gas-fired dryer (**0.8 MW** of gas or LPG, *proposed*), screens and a cooler. Sneak-click: **TSP** (rock slurry and merchant acid) or **DAP** (merchant acid and ammonia, on its east face). **150 kW at LV**. It is `Bagged`: a bagging station against it fills **1 t bags**. |
| `bagging_station` | 2 x 4 x 2 | **Existing** (alumina-plant.md). |

### Potash
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `potash_dissolver` | 4 x 5 x 4 | **New. Controller.** Crushed potash dissolved in hot recycle brine near 100 C, heated by process steam; the halite stays undissolved and goes out as **tails slurry** (salt tails, an assayed slurry) *(simplified: real mines pile it)*. **55 kW at LV.** Potash ore by a hopper on top. |
| `vacuum_crystallizer` | 3 x 10 x 3 | **New.** Multi-stage vacuum cooling crystallizers: KCl crystallizes as the brine cools; the brine returns to the dissolver it touches. Steam ejectors on process steam; **110 kW at LV** of pumps. |
| `potash_dryer` | 3 x 4 x 8 | **New.** A fluid-bed dryer and compaction: **0.6 MW** of gas or LPG *(proposed)*, **75 kW at LV**. `Bagged`: **MOP, 0-0-60**, in 1 t bags. |

## Layout (facing north, plan view)
### Phosphoric acid (the reactor's main cell at 0,0,0)
Layer y 0:
```
x:  -6-5-4-3-2-1 0 1 2 3 4 5 6 7 8 9 10 11 12 13
z-6  . . M M M . . . . . . . . . . . .  .  .  .     M phosphate mill (3 x 4 x 6, running south), rock slurry out at z -1
z0   . . . . . . R R R R R R R R F F F  F  F  F      R phosphoric reactor (8 x 6 x 8)
z1   . . . . . . R R R R R R R R F F F  F  F  F      F tilting pan filter (6 x 4 x 10, running south), against the reactor's east face
...  (to z7)                                           V acid evaporator, against the filter's south end
z10  . . . . . . . . . . . . . . V V V  G  G  G  ... G granulation plant (4 x 5 x 12), fed by pipe; B its bagging station
```
### Potash (the dissolver's main cell at 0,0,0)
```
 D D D D Y Y Y P P P P P P P P S S      D dissolver   Y crystallizer   P potash dryer (8 long)   S bagging station
```
Upper layers: the reactor and filter to y 5 and 3; the evaporator and crystallizer to y 9; the granulator drum at y 1-3 with its
dryer stack to y 4.

The grids are sketches; the game tests are the layout of record.

### Legend
| Char | Block id |
|---|---|
| `M` | `wildcatter:phosphate_mill` |
| `R` | `wildcatter:phosphoric_reactor` |
| `F` | `wildcatter:tilting_pan_filter` |
| `V` | `wildcatter:acid_evaporator` |
| `G` | `wildcatter:granulation_plant` |
| `D` | `wildcatter:potash_dissolver` |
| `Y` | `wildcatter:vacuum_crystallizer` |
| `P` | `wildcatter:potash_dryer` |
| `S`, `B` | `wildcatter:bagging_station` |

## Controllers, rotation, forming, removing
- **Controllers:** the phosphoric reactor (it finds the filter by touching; the mill, evaporator and granulator are on pipe and work on
  their own) and the potash dissolver (crystallizer and dryer by touching).
- **Rotation:** any of the four.
- **Removing:** the filter: the reactor reads NO FILTER and holds. The tailings cell full or missing: the filter reads NO GYPSUM STACK
  and stops, as a real plant cannot run without its stack. The ammonia gone: the granulator finishes its batch of DAP and reads NO
  AMMONIA (TSP still runs).

## Products
- **New pipe products:** rock slurry (kg), filter acid (L), merchant acid (L; stock tanks may hold it).
- **New bags (1 t):** TSP (0-46-0), DAP (18-46-0), MOP (0-0-60). They carry the item tag **`c:fertilizers`** *(proposed: the
  convention tag Farmings can read)*. Wildcatter itself gives them **no** use on crops (it is not a farming mod).
- **Counted:** fluorine off-gas, P2O5 lost to the gypsum.

## Left out
- The hemihydrate process; fluorosilicic acid recovery (which real plants turn into AlF3, a second source for the potline).
- Nitrogen fertilizers other than DAP (urea, UAN): the ammonia plant's own spec leaves them out.
- Potash by flotation (the Saskatchewan mills' main route) and solution mining.
- Radium in phosphogypsum.

## Game tests
- `phosphoric`: rock by hopper, sulfuric acid from a test-filled stock tank, process steam from a package boiler, water by hook:
  - filter acid flows and merchant acid fills a stock tank;
  - gypsum slurry reaches a lined cell whose readout names gypsum;
  - the granulator on TSP fills a bag (brought near full by hook); switched to DAP with ammonia from a test-filled tank, a DAP bag;
  - the cell broken: NO GYPSUM STACK.
- `potash`: potash by hopper and process steam: MOP bags under the bagging station; salt tails slurry on its pipe.
- **Screenshot:** the phosphoric acid plant.

## Proposed wave
**18a**, then uranium from phosphoric acid (18b) on its filter acid.
