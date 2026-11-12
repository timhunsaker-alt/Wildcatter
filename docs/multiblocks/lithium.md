# Lithium hydroxide from spodumene

> **20c BUILT (2026-11-12), decided on recommendation while Tim was away; not approved.** See "As built" at the end. Proposed phase **20c**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 68-71). Depends on the chlor-alkali plant (20a) for caustic.

The 7a spodumene pegmatite (Greenbushes) has no plant. Hard-rock lithium is made by the **sulfate route**: spodumene is **decrepitated**
at about 1,050 C (alpha to the reactive beta form), **roasted with sulfuric acid** at about 250 C to soluble lithium sulfate, leached,
purified, and turned into **lithium hydroxide** with caustic soda (or lithium carbonate with soda ash). Wildcatter has the acid and the
caustic, and no soda ash, so this plant makes **lithium hydroxide monohydrate** and, by a carbonation step, a little carbonate.

Sources:
- The conventional route: alpha-spodumene calcined at **1,050 C** then roasted with sulfuric acid at **250 C**, about **97 %** of the
  lithium recovered from beta-spodumene ([MDPI paper on lithium extraction from spodumene](https://mdpi.com/2673-4605/15/1/62); title not checked);
  decrepitation at 1,050 C for 30 minutes in a rotary kiln ([Polytechnique Montréal](https://publications.polymtl.ca/9414)); other
  sources give 1,070-1,100 C ([FEECO](https://feeco.com/?p=28712)).
- Soda ash and caustic soda remove impurities after the roast, and lithium carbonate crystallizes at 98-99 % purity (as above).
- Lithium sulfate with caustic soda to lithium hydroxide and sodium sulfate (Glauber's salt, crystallized by chilling), as the
  hydroxide refineries in Western Australia run *(textbook, unverified)*.
- Spodumene LiAlSi2O6 is 8.03 % Li2O pure; concentrates 6-7.5 % *(textbook)*.

## Numbers *(proposed unless marked)*
- **Ore:** 80 % spodumene at 7.5 % Li2O is **6 % Li2O**; a block of about **2,900 kg** *(unverified density)* holds **174 kg of Li2O**.
- **Product:** 2.81 kg of LiOH·H2O a kg of Li2O (derived); **90 %** overall recovery: **440 kg of LiOH·H2O a block**.
- **Size:** **0.5 t/h of LiOH·H2O**, 12 t a day (a real refinery makes 2-3 t/h): about **1.1 blocks an hour**, 3.3 t/h of ore.
- **Decrepitation:** about **1 GJ a tonne** of ore *(unverified)*: **0.9 MW** of gas or LPG.
- **Acid:** 3.28 kg of H2SO4 a kg of Li2O, with **30 % excess**: **0.82 t/h**, an eighth of the 7b acid plant.
- **Caustic:** 2.68 kg of NaOH a kg of Li2O reacted: **0.46 t/h of NaOH, 1.44 t/h of 32 % caustic**, about 40 % of the chlor-alkali plant.
- **Sodium sulfate:** 4.75 kg a kg of Li2O: **0.82 t/h**, in 1 t bags (questions file: it has no buyer, as the borax plant's has none).
- **Leach residue:** the aluminosilicate left, about 3 t/h, as **tails slurry** to a lined cell.

## Parts
Voltages are the world's standard tiers ("LV" 480 V or 400 V).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `decrepitation_kiln` | 3 x 4 x 12 | **New.** A direct-fired rotary kiln at **1,050 C** with its cooler: spodumene by a hopper on top, gas or LPG on its east face. Drive and fans **30 kW at LV**. It hands beta-spodumene to the roast kiln it touches. |
| `acid_roast_kiln` | 3 x 3 x 8 | **New.** A pug mill and an indirectly fired kiln at **250 C**: sulfuric acid on its east face, **0.2 MW** of gas *(proposed)*. **15 kW at LV.** |
| `lithium_leach` | 4 x 5 x 6 | **New. Controller.** Water leach, lime neutralisation (quicklime bags by hopper, **20 kg a tonne of ore**, *proposed*), a belt filter, and calcium removal with carbonate made in line from caustic and CO2. **37 kW at LV.** Residue out as tails slurry on its west face. |
| `lioh_crystallizer` | 3 x 10 x 4 | **New.** Causticisation of the lithium sulfate with caustic (on its east face), a chilling crystallizer for Glauber's salt, and an evaporative crystallizer for LiOH·H2O on **process steam** (about **3 t a tonne**, *unverified*). **110 kW at LV.** `Bagged` twice: lithium hydroxide, and sodium sulfate (each with its own bagging station). |
| `lithium_carbonator` | 2 x 4 x 3 | **New** (optional). LiOH solution and CO2 to **lithium carbonate**, 1 t bags: **10 kW at LV**. |
| `bagging_station` | 2 x 4 x 2 | **Existing**. |

## Uses
- **The potline** (potline.md): **lithium carbonate** added to the bath lowers its liquidus and raises its conductivity; a pot with
  lithium in its bath runs **0.1 V lower** *(proposed; real lithium-modified baths save a few percent of the energy, unverified)*,
  taking **1.5 kg of Li2CO3 a tonne of aluminium** *(unverified)* from a bag in a hopper on the line's fume treatment centre.
- **Lithium hydroxide:** tagged for other mods. A PWR's coolant uses **lithium-7** hydroxide, which needs isotope separation; left out.
- **Grid batteries** are not in Wildcatter; they are the obvious user (questions file proposes them as a new Idea).

## Layout (facing north, plan view; the leach's main cell at 0,0,0)
Layer y 0:
```
x:  -12 ... -4-3-2-1 0 1 2 3 4 5 6 7 8 9
z0   K  ...  K R R R L L L L Y Y Y Y S S      K decrepitation kiln (12 long, west)  R acid roast kiln (8 long)
z1   K  ...  K R R R L L L L Y Y Y Y S S      L lithium leach (y 0-4)  Y LiOH crystallizer (y 0-9)  S bagging station
z2   K  ...  K R R R L L L L Y Y Y Y . .
z3   . . . . . . . . L L L L . . . . . .       C carbonator and a second bagging station on the crystallizer's south face
```
The grid is a sketch; the game test is the layout of record. (R is drawn shortened; it is 8 long and touches both K and L.)

### Legend
| Char | Block id |
|---|---|
| `K` | `wildcatter:decrepitation_kiln` |
| `R` | `wildcatter:acid_roast_kiln` |
| `L` | `wildcatter:lithium_leach` |
| `Y` | `wildcatter:lioh_crystallizer` |
| `C` | `wildcatter:lithium_carbonator` |
| `S` | `wildcatter:bagging_station` |

## Controller, rotation, forming, removing
- **Controller:** the leach; it finds the roast kiln (and through it the decrepitation kiln) and the crystallizer by touching. Any
  rotation.
- **Readouts:** NO ORE, NO GAS, NO ACID, NO LIME, NO CAUSTIC, NO CO2, NO STEAM, NO TAILS CELL, FULL.
- **Removing a part** stops what needs it.

## Left out
- Lithium from brines (the salars) and from clays.
- Battery-grade purity steps beyond the crystallizer (ion exchange polishing).
- Tantalum and tin from the pegmatite.

## Game tests
- `lithium`: spodumene by hopper, gas from a bullet, acid and caustic from test tanks, lime and CO2, process steam from a package boiler:
  - a bag of lithium hydroxide (brought near full by hook) and a bag of sodium sulfate drop under their stations;
  - the carbonator fills a lithium carbonate bag; in a potline pot's hopper it lowers the pot's voltage by about 0.1 V;
  - the caustic cut: NO CAUSTIC.
- **Screenshot:** the kilns.

## Proposed wave
**20c**, last of the ore plants.

## As built (20c, 2026-11-12; decisions in `docs/questions/phase20-chemicals.md`, items 27-34)
- **Parts** (`ChemPartBlock`): decrepitation kiln (30 kW, box west; spodumene by hopper; gas on the east face of its back end, 1 GJ a tonne
  of ore), acid roast kiln (15 kW, box north; acid east at its back cell, 0.2 MW of gas south), lithium leach (the controller, 37 kW, box
  north; lime bags by hopper, CO2 and the residue as tails slurry on the south face of its back cells), LiOH crystallizer (110 kW, box north;
  caustic east, process steam south; sodium sulfate to a station on its north face, LiOH to one on any other), lithium carbonator (10 kW;
  LiOH bags by hopper, CO2, 100 kg/h of Li2CO3 at most). Layout as the `lithium` scene (item 28).
- **Numbers:** 0.5 t of LiOH.H2O an hour from 3.3 t/h of ore (the mod's 2,700 kg block, item 27); 0.84 t/h of acid, 1.44 t/h of 32 %
  caustic, 0.85 t/h of sodium sulfate, 0.9 MW of decrepitation gas, about 3 t/h of residue.
- **The pot:** a lithium carbonate bag in a pot's hopper lowers it 0.1 V while its bath holds lithium (item 32).
- **Tests:** NeoForge `lithium` (483 kg/h of LiOH.H2O; the three bags; the pot 1.728 V to 1.628 V; NO CAUSTIC); unit `LithiumTest`; client
  screenshots `lithium` (the plant, the kilns).
