# The titanium pigment plant: ilmenite to slag, the chloride process, TiO2

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **20b**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 64-67). Depends on the chlor-alkali plant (20a) for chlorine and on 17a's furnace.

The zirconium plant's mineral separator (zirconium.md) takes the zircon from heavy mineral sand and only **counts** its ilmenite and
rutile, though they are 60 % of the heavy minerals. Real mineral-sand producers sell those to **titanium dioxide pigment** plants. This
spec makes them: ilmenite smelted to **titanium slag** and pig iron, slag and rutile **chlorinated** to TiCl4, the TiCl4 purified and
**burned in oxygen** to TiO2, the chlorine coming back to the chlorinator.

Sources:
- The chloride process: the feed is chlorinated with coke at **about 1,000 C** to TiCl4; per tonne of TiO2 about **370 kg of coke,
  350 Nm3 of oxygen and 360 kWh** ([Chloride process, Wikipedia mirror](https://bafybeiemxf5abjwjbikoz4mc3a3dla6ual3jsgpdr4cjr3oz3evfyavhwq.ipfs.4everland.io/wiki/Chloride_process.html),
  whose provenance needs checking against Kirk-Othmer; the oxygen agrees with the stoichiometry: 280 Nm3 plus excess).
- Fluidized-bed chlorination of rutile ([OneMine](https://www.onemine.org/documents/fluidized-bed-chlorination-of-rutile)); value-in-use
  of titania feedstocks, where iron, calcium and magnesium in the feed cost chlorine
  ([University of Pretoria](https://repository.up.ac.za/handle/2263/50191)).
- Ilmenite smelting to **85 % TiO2 slag** and pig iron in electric furnaces (Richards Bay, Sorel) *(textbook, unverified; about 2.2 MWh a
  tonne of slag, unverified)*.

## Size *(proposed)*: 0.25 t of TiO2 an hour, 6 t a day
- **Feed:** 60 % of the TiO2 from slag and 40 % from rutile *(proposed: the separator gives eleven times more ilmenite than rutile, but
  smelting is the slow step)*.
- **Slag:** 0.18 t/h of 85 % slag from about 0.31 t/h of ilmenite (50 % TiO2, *unverified*), with about **0.12 t/h of pig iron**,
  cast as **vanilla iron ingots** *(proposed)*. The furnace draws about **0.4 MW** *(derived from 2.2 MWh/t)*.
- **Rutile:** about 0.105 t/h at 95 % TiO2 *(proposed)*.
- **Chlorinator:** **0.59 t/h of TiCl4** (189.7 / 79.9 a tonne of TiO2, derived); coke **93 kg/h** (370 kg/t).
- **Chlorine:** **0.44 t/h gross** comes back from the oxidiser; **make-up about 0.1 t/h** for the iron, calcium and magnesium in the feed
  *(proposed: 0.4 t a tonne of TiO2 on this blend, unverified)*: about a tenth of the chlor-alkali plant.
- **Oxygen:** **88 Nm3/h** (350 a tonne).
- **Power:** the plant about **90 kW** (360 kWh/t) besides the smelter.

## Parts
Voltages are the world's standard tiers ("distribution" 12.47 kV or 11 kV; "LV" 480 V or 400 V).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `mineral_separator` | 3 x 4 x 6 | **Existing** (zirconium.md). Its ilmenite and rutile now go out to hoppers under it as **ilmenite** and **rutile** items (500 kg each, *proposed*) instead of being counted. |
| `submerged_arc_furnace` + `furnace_transformer` | | **Existing** (ferroalloys.md), with a fourth recipe, **Ti slag**: ilmenite and coke (anthracite in real plants) to 85 % slag and pig iron, **2.2 MWh a tonne of slag**. Real ilmenite smelters are open-bath furnaces; the SAF stands in *(proposed)*. Slag goes out in 1 t bags. |
| `ti_chlorinator` | 4 x 12 x 4 | **New. Controller.** A fluid-bed chlorinator at about 1,000 C: slag or rutile and coke by a hopper on top, chlorine on its east face. Its blower **30 kW at LV**. The metal chlorides other than TiCl4 (iron, magnesium, calcium) go to its cyclone as waste, counted. |
| `ticl4_purification` | 3 x 12 x 3 | **New.** Condensers and a distillation column taking out SiCl4 and, with a small oil or copper treatment, VOCl3: **pure TiCl4** to the oxidiser it touches. Reboiler **40 kW at LV**. |
| `ticl4_oxidiser` | 3 x 4 x 10 | **New.** TiCl4 preheated and burned with preheated **oxygen** (on its north face) in a toluene-fired reactor *(simplified: the burner fires pipeline gas)* at about 1,500 C, quenched in a flue cooler: raw TiO2 to the finishing it touches, **chlorine** back to the chlorinator on its pipe. Preheaters **0.2 MW** of gas *(proposed)*. |
| `vpsa_oxygen` | 3 x 4 x 6 | **New.** A vacuum-swing adsorption oxygen unit, **100 Nm3/h at 93 %**, **35 kW at LV** *(proposed: about 0.35 kWh a Nm3, unverified)*. Oxygen on its south face. Also serves the alloy melt shop's AOD. |
| `pigment_finishing` | 4 x 5 x 10 | **New.** Wet milling, surface treatment with alumina (the Bayer refinery's hydrate, by hopper: **30 kg a tonne**, *proposed*), filtration, drying and a steam micronizer on **process steam** (about **2 t a tonne**, *unverified*). **75 kW at LV.** `Bagged`: **TiO2 pigment** in 1 t bags. |

## Products
- **TiO2 pigment, rutile grade**, 1 t bags. A bag crafts into **64 white dye** *(proposed: the only vanilla use; real pigment is in every
  white paint)*. Tagged for other mods.
- **Pig iron:** vanilla iron ingots from the smelter.
- **Counted:** chlorinator waste chlorides, SiCl4.

## Layout (facing north, plan view; the chlorinator's main cell at 0,0,0)
Layer y 0:
```
x:  0 1 2 3 4 5 6 7 8 9 10 11 12 13 ... 20
z0  C C C C P P P O O O O  O  O  O ...  F F F F       C ti chlorinator (y 0-11)   P TiCl4 purification (y 0-11)
z1  C C C C P P P O O O O  O  O  O ...  F F F F       O oxidiser (3 x 4 x 10, east)   F pigment finishing, against its east end
z2  C C C C P P P O O O O  O  O  O ...  F F F F
z3  C C C C . . . . . . .  .  .  . ...  F F F F       X vpsa oxygen, piped to the oxidiser's north face (not drawn)
```
Upper layers: the chlorinator and the purification column to y 11; the oxidiser's quench flue at y 1-3; the finishing's dryer to y 4.
The smelter (ferroalloys.md's furnace and transformer) and the separator stand elsewhere and ship by hopper and bag.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `C` | `wildcatter:ti_chlorinator` |
| `P` | `wildcatter:ticl4_purification` |
| `O` | `wildcatter:ticl4_oxidiser` |
| `F` | `wildcatter:pigment_finishing` |
| `X` | `wildcatter:vpsa_oxygen` |

## Controller, rotation, forming, removing
- **Controller:** the chlorinator; it finds the purification, oxidiser and finishing by touching (NOT BUILT names a missing one). Any
  rotation.
- **The chlorine loop:** the oxidiser's chlorine goes back on its pipe; the chlorinator takes it first and the make-up after. With the
  oxidiser stopped the chlorinator runs on make-up alone until its TiCl4 hold is full (FULL).
- **Removing a part** stops what needs it; nothing is lost but what each part holds.

## Left out
- The sulfate process (ilmenite in sulfuric acid), which the acid plant could feed: the chloride process is chosen for its chlorine loop.
- **Titanium sponge** by the Kroll process from the same TiCl4: the zirconium plant's Kroll reactor and magnesium loop could make it, for
  titanium condenser tubes (questions file).
- Synthetic rutile (Becher process).

## Game tests
- `tiPigment`: rutile and slag by hook, coke by hopper, chlorine from a test-filled supply, oxygen from a VPSA unit on a 480 V set,
  process steam from a package boiler, hydrate by hopper:
  - TiCl4 flows to the oxidiser; chlorine comes back to the chlorinator;
  - a bag of TiO2 (brought near full by hook) drops under the bagging station; it crafts into white dye;
  - the oxygen unit broken: the oxidiser reads NO OXYGEN.
- `ferroalloys` (extended): the furnace on the Ti slag recipe makes slag bags and iron ingots.
- **Screenshot:** the plant.

## Proposed wave
**20b**, after the chlor-alkali plant (20a).
