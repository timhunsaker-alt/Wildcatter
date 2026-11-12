# Fuel fabrication extensions: HF recovery, lubricant, real hardware, inspection, IFBA, scrap

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **19c** (19c1 inspection, scrap
> and lubricant; 19c2 HF recovery, IFBA and the real hardware). Decisions are in `docs/questions/specs-wildcatter.md` (items 79-84).

Six items the fuel plant spec (fuel.md, approved 2026-11-01) left in ROADMAP Ideas. Each has its real basis and a proposed scope. Numbers
use fuel.md's: a line of 50 kg of U an hour; 0.504 kg of HF a kg of U to the kiln's scrubber; a pellet of 5.31 g; 372 pellets a rod;
264 rods an assembly.

## 1. The kiln's HF back to the conversion plant
**Real basis.** The IDR kiln's off-gas is HF and steam; real plants condense it as **aqueous HF** and sell or recycle it *(textbook,
unverified)*. Anhydrous HF is made from aqueous by distilling it over strong sulfuric acid, which the conversion plant's HF column
already scrubs with.

**Numbers:** 0.504 kg of HF a kg of U is **25 kg/h** at the line's rate (derived): about **13 %** of the conversion plant's 191 kg/h
(0.053 kg/s).

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `hf_recovery_condenser` | 2 x 6 x 2 | **New.** Graphite condensers and a small column on the kiln's off-gas, against the IDR kiln: **aqueous HF, 70 %** *(proposed)* (a new pipe product, L) out of its south face. **10 kW at LV.** |
- The conversion plant's **HF column** takes aqueous HF on its north face and gives anhydrous HF, using **0.5 kg of sulfuric acid a kg of
  HF** *(proposed)* from the kiln's acid supply. The kiln then needs that much less fluorspar.

## 2. Zinc stearate lubricant and pore formers
**Real basis.** UO2 powder is pressed with about **0.2-0.3 % zinc stearate** to lubricate the die; **pore formers** or recycled U3O8 are
added to set the sintered density *(textbook, unverified)*.

**Proposed scope:**
- **Zinc stearate:** an item (5 kg sacks) crafted from a **zinc ingot** (sphalerite, ores.md) and an **animal fat** (any item tagged
  `c:animal_fats`, which Farmings' tallow would carry; rotten flesh stands in without Farmings) *(proposed)*. The granulator takes it by
  hopper at **2.5 g a kg of UO2** *(proposed)*: one sack lasts about 30 hours.
- **Without it:** the pellet press reads **NO LUBRICANT** and runs, but **10 %** of its green pellets crack and go to scrap *(proposed)*.
- **Pore former:** the recycled U3O8 from item 6 is the pore former, as in real plants; the density is not modelled downstream, so it only
  closes the scrap loop.

## 3. A nickel smelter for real Inconel and stainless hardware
**Real basis.** The 17×17 assembly's nozzles are 304 stainless; its end grids and holddown springs Inconel (fuel.md). The nickel smelter
and alloy melt shop (nickel-smelter.md, 17b2) make **304 stainless** and a **nickel-chromium alloy (Alloy 600 type)** in 100 kg ingots.

**Proposed scope:**
- The **assembly fixture** takes **16 kg of 304** (nozzles) and **4 kg of nickel-chromium alloy** (end grids, springs and plugs) for its
  20 kg of hardware, in place of steel. The **rod loader** takes its plugs from the same alloy *(proposed)*.
- **Steel stops working** for both once this wave lands *(proposed)*; assemblies already made, and those in a reactor, are unchanged.
  The readout reads NO STAINLESS, NO NICKEL ALLOY.
- Real grids are Alloy 718, which needs niobium; Wildcatter has none (nickel-smelter.md).

## 4. Pellet inspection, rod X-ray and helium leak tests
**Real basis.** Every pellet is checked for size and chips; every rod's end welds are radiographed, its helium checked in a vacuum leak
test, and its enrichment gamma-scanned *(textbook)*.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `pellet_inspection` | 2 x 3 x 4 | **New.** An automated laser-gauge and camera inspection bench between the grinder and the rod loader, touching both: it rejects **1 %** of pellets *(proposed)* to scrap. **10 kW at LV.** |
| `rod_inspection` | 3 x 3 x 10 | **New.** Weld X-ray, a vacuum chamber with a helium mass-spectrometer leak detector, and a passive gamma scanner, between the welder and the fixture, touching both: it rejects **0.5 %** of rods *(proposed)*. **25 kW at LV.** |
- **Without them** the line runs, but each assembly made carries a **1 %** chance *(proposed)* of a **leaking rod**, which in the reactor
  raises the coolant's activity (reactor-extensions.md, item 2).
- Rejected rods are cut up and their pellets go to scrap (item 6).

## 5. IFBA and gadolinia burnable absorbers
**Real basis.** Westinghouse fuel uses **IFBA**: a thin zirconium diboride coating on some pellets; other vendors mix **gadolinia** into
some rods. Both hold down a fresh core's excess reactivity and burn out over the first part of the cycle, so the coolant needs less boron
*(textbook; rod counts and worths unverified)*.

**Proposed scope:**
- **IFBA only** *(proposed)*: gadolinia needs gadolinium from monazite, and Wildcatter has no rare-earth plant (questions file).
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `ifba_coater` | 3 x 3 x 6 | **New.** A sputter coater putting ZrB2 on pellets, beside the line between the inspection bench and the rod loader. Zirconium as Zircaloy scrap (zirconium-extensions.md) or tube, and **boric acid** bags (boron.md) by hopper. **50 kW at LV.** A sneak-click on the rod loader sets the assembly's **IFBA rods: 0, 32, 64, 104** *(proposed)*. |
- **In the reactor:** an assembly with 104 IFBA rods starts **3,000 pcm** lower *(proposed)*, the absorber burning out in a straight line by
  **12 GWd/t**. A fresh core of IFBA fuel needs about **300 ppm less boron** (−10 pcm/ppm, reactor.md) at the start of its cycle.

## 6. Rejects and scrap recycle
**Real basis.** Dry scrap (rejected pellets, grinder swarf, pellets from cut-up rods) is oxidised in air at about 400-450 C to U3O8 and
blended back into the powder *(textbook, unverified)*.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `scrap_oxidizer` | 2 x 3 x 4 | **New.** A tray furnace oxidising scrap UO2 to **U3O8** in air: **30 kW at LV.** It takes the line's scrap (the grinder's 0.5 % swarf, today counted; the press's cracked pellets; inspection rejects) from hoppers, and gives U3O8 to the granulator it touches, at most **10 %** of the granulator's feed *(proposed)*. |
- The uranium in scrap stays at its assay; scrap of different assays is never mixed (the oxidizer holds one at a time).

## Layout (the 7g line, facing north, plan view; new parts marked)
Layer y 0:
```
 A A A K K K ... K G G G P P P S S S ... S N N N i i L L L ... L r r r W W W x x x ... x F F F ...
           h h                                 o o                     I I I ... I
 A UF6 autoclave  K IDR kiln  h HF recovery condenser (new)  G granulator  o scrap oxidizer (new, against G)
 P pellet press  S sintering furnace  N grinder  i pellet inspection (new)  L rod loader  r IFBA coater (new, beside L)
 W rod welder  x rod inspection (new)  F assembly fixture
```
The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `h` | `wildcatter:hf_recovery_condenser` |
| `o` | `wildcatter:scrap_oxidizer` |
| `i` | `wildcatter:pellet_inspection` |
| `r` | `wildcatter:ifba_coater` |
| `x` | `wildcatter:rod_inspection` |
| `K`, `G`, `L`, `W`, `F` ... | the existing fuel.md parts |

## Controller, rotation, forming, removing
The fuel line has no single controller: each stage takes what the one before it holds. The inspection parts sit between two stages and
pass on what they accept; removed, the stages before and after them must touch again (NOT BUILT otherwise), as before this wave. Any
rotation.

## Game tests
- `fuel` (extended, 19c1): the press without zinc stearate reads NO LUBRICANT and sends cracked pellets to scrap; the scrap oxidizer
  turns them to U3O8 into the granulator; the inspection parts reject their share; an assembly made with no inspection parts carries
  its leak chance on its item.
- `fuel` (extended, 19c2): the HF recovery condenser fills a test tank with aqueous HF that the conversion plant's HF column turns
  anhydrous; an assembly with 104 IFBA rods reads 3,000 pcm lower in a vessel; the fixture takes 304 and nickel alloy.
- **Screenshot:** the inspection bench.

## Proposed waves
- **19c1:** inspection, scrap and lubricant.
- **19c2:** HF recovery, IFBA, the real hardware (needs 17b2).

## As built
### 19c1 (2026-11-12, branch `w-17b`; decisions `docs/questions/phase19c-fuel.md`)
- **Parts:** `pellet_inspection` (2 x 3 x 4, 10 kW, 1 % of the U to scrap), `rod_inspection` (3 x 3 x 10, 25 kW, every 200th rod cut up to
  scrap), `scrap_oxidizer` (2 x 3 x 4, 30 kW, box north; 5 kg of U an hour of scrap to U3O8, the granulator takes up to a tenth of its
  feed). The rod loader takes pellets from a touching bench before the grinder; the fixture takes rods from a touching rod line before
  the welder. Items `zinc_stearate` (5 kg sacks, a zinc ingot and `#c:animal_fats`) and `uo2_scrap_can` (5 kg of U, its assay).
- **Existing worlds:** cracking, swarf in cans and the leak chance are behind the new world rule `fuelLineDefects` (on in new worlds,
  off in existing ones). The new parts work in any world once placed.
- **Leaking rods:** the fixture writes a leak chance on the assembly (0.5 % for each inspection part missing); it is rolled when the
  assembly is loaded into a core, and a leaker from it counts in 19b's coolant activity from the start.
- **Test:** `fuel_quality` (a NeoForge scene of its own, not an extension of the `fuel` client scene): NO LUBRICANT and cracked pellets;
  the oxidizer's U3O8 into the granulator; the bench's 1 %; the rod line's one in 200; a bare line's 1 % leak chance; with zinc
  stearate nothing cracks. Client scene `fuelInspection` (screenshots).

