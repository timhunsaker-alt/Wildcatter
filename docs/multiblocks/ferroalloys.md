# Ferroalloys: the submerged arc furnace, the moly roaster and the aluminothermic vessel

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Phase **17a**, BUILT 2026-11-10 (see "As built").
> Decisions are in `docs/questions/specs-wildcatter.md` (items 41-46) and `docs/questions/phase17-ferroalloys.md`.

The 7a chromitite, manganese, molybdenum and tungsten ores have no plant (ores.md: "the rest go to the plants listed under Ideas").
Real ferroalloys are made two ways:
- **carbothermic**, in a **submerged arc furnace (SAF)**: ore, coke and flux in a shell, three carbon electrodes buried in the charge,
  tens of MVA. High-carbon ferrochrome, ferromanganese and ferrosilicon are made this way;
- **aluminothermic**, in a refractory vessel: an oxide and aluminium powder react by their own heat. Ferromolybdenum, ferrotungsten
  and chromium metal are made this way.

The SAF is the reason to build this: a **10 MVA, low power factor, three-phase load on the distribution tier**, on its own furnace
transformer, with a capacitor bank to correct it. It is the grid's next big industrial customer after the potline.

Sources:
- High-carbon ferrochrome in a SAF uses **2,400-4,700 kWh a tonne** of alloy by furnace and charge; one study gives **3,300-3,800**,
  lowest (3.31 MWh/t) at about 6 % SiO2 in the ore
  ([core.ac.uk 43603423](https://core.ac.uk/download/43603423.pdf); [MDPI Metals 16(3) 357](https://www.mdpi.com/2075-4701/16/3/357)).
- An Indian plant runs **5 furnaces totalling 72 MVA**: real SAFs are 9-80 MVA each
  ([Industrial Automation India](https://industrialautomationindia.in/articleitm/12000/Energy-Conservation-in-High-Energy-Intensity-Industry/articleitm/11999/Energy-Transition%20The-Change-of-a-Decade/articles)).
- Ferromanganese about **2,500 kWh/t** and 75 % ferrosilicon about **8,500 kWh/t**; Søderberg electrodes; SAF power factor about
  **0.75-0.85** *(textbook, unverified)*.
- Molybdenite roasting in multiple-hearth furnaces at about **600-650 C** to technical moly oxide; aluminothermic ferromolybdenum
  (60-70 % Mo) and ferrotungsten (75-80 % W) *(textbook, unverified)*.

A figure marked *(proposed)* is chosen for the game; *(unverified)* is from memory or a typical value not yet checked.

## Products
| Product | Grade | Made in | Per tonne of alloy *(proposed unless marked)* | Who uses it |
|---|---|---|---|---|
| **HC ferrochrome** | 65 % Cr, 7 % C | SAF | 0.62 chromitite block (85 % Cr recovery), 0.55 t coke, 0.2 t quartz; **3.8 MWh** | the alloy melt shop's stainless (nickel-smelter.md) |
| **HC ferromanganese** | 76 % Mn, 7 % C | SAF | about 1.6 t of manganese ore (80 % pyrolusite, unverified), 0.4 t coke, 0.15 t limestone; **2.5 MWh** | the alloy melt shop (steel and stainless); the uranium mill's oxidant stays pyrolusite |
| **Ferrosilicon 75** | 75 % Si | SAF | 1.8 t quartz, 0.9 t coke or charcoal, 0.25 t iron; **8.5 MWh** | Pidgeon magnesium (zirconium-extensions.md) |
| **Technical moly oxide** | 57 % Mo | moly roaster | 1.1 t of 90 % molybdenite concentrate a tonne of oxide (derived) | the aluminothermic vessel |
| **Ferromolybdenum** | 65 % Mo | aluminothermic vessel | 1.15 t of moly oxide, 0.5 t iron ore or mill scale, 0.53 t aluminium (derived from the reactions) | Alloy 718 and 316 stainless in the alloy melt shop |
| **Ferrotungsten** | 78 % W | aluminothermic vessel | scheelite concentrate holding 1.04 t of WO3, 0.3 t aluminium (derived) | no Wildcatter user yet (tagged for other mods) |
| **Chromium metal** | 99 % Cr | aluminothermic vessel | chromic oxide (zirconium-extensions.md), 0.52 t aluminium a t (derived) | the VAR furnace's chromium (zirconium-extensions.md) |

- **Quartz:** vanilla nether quartz stands in for quartzite lump *(proposed: Wildcatter has no quartzite rock)*.
- **Coke:** the coke ovens' metallurgical coke (coke-ovens.md), which "has no user in the mod beyond carbon and burner fuel": this
  gives it one.
- **Aluminium:** ingots from the potline (potline.md), 22.7 kg each.
- **Concentrates:** 3 % molybdenite and 6.5 % WO3 scheelite are too lean to roast or reduce. The roaster and vessel take **flotation
  concentrate**: from Tailings' concentrator as thickened concentrate slurry carrying molybdenite or scheelite in its assay
  (Phase 11's assayed slurry), dewatered by the roaster's own filter *(proposed)*. Without Tailings, molybdenum and tungsten wait.
- Each product goes out in **1 t bulk bags** through a bagging station (the `Bagged` interface, alumina-plant.md). Slag is counted.

## Parts
Voltages are the world's standard tiers: "distribution" is 12.47 kV (NA) or 11 kV (IEC); "LV" 480 V or 400 V.

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `furnace_transformer` | 4 x 5 x 5 | **New.** A **10 MVA** furnace transformer, distribution primary, a **150 V** secondary at full tap (about **38 kA** a phase), on-load tap changer of 15 steps, water-cooled. Its secondary is the furnace's delta closed at the electrodes by flexible cables and bus tubes *(unverified values from typical small SAFs)*. Its distribution box on its west face. Shared with the nickel smelter and the ilmenite smelter. |
| `submerged_arc_furnace` | 7 x 8 x 7 | **New. Controller.** A round shell about 7 m across with its taphole, three **Søderberg** electrodes (paste cylinders on hydraulic slipping and regulating gear), the charging chutes and a fume hood. **8 MW at PF 0.80** at full tap. Charge (ore, coke, flux) by a hopper on top; its electrode paste by a second hopper. The alloy taps every 2 hours *(proposed)* into its bagging station; slag is counted. Sneak-click sets the recipe (FeCr, FeMn, FeSi); a change empties the hearth first, 1 hour *(proposed)*. |
| `capacitor_bank` | 2 x 3 x 4 | **New.** A **3 Mvar** shunt capacitor bank at distribution voltage in its fenced rack, with its breaker and reactors. On the same bus it lifts the furnace's PF to about **0.94** *(derived: 8 MW at 0.80 draws 6 Mvar; with 3 Mvar back, 8 / 8.54 MVA)*. It is a grid part: any bus may have one. |
| `moly_roaster` | 3 x 9 x 3 | **New.** A multiple-hearth roaster at 600-650 C with its own drum filter for the concentrate slurry, **0.25 t/h of oxide** *(proposed)*, a 22 kW rabble drive and fans at LV *(proposed)*. Its off-gas (about 6 % SO2, *unverified*) goes on the pipe as **burner gas**, which the acid plant's converter takes. It is `Bagged`. |
| `aluminothermic_vessel` | 3 x 3 x 3 | **New.** A refractory-lined steel vessel in a sand bed, under a hood to a baghouse (15 kW fan at LV). A batch by a hopper on top (the oxide, aluminium and flux); a sneak-click fires it: **one tonne of alloy in 10 minutes** *(proposed)* and a block of alumina slag. Sparks and fume; a player inside its hood takes fire damage. |
| `bagging_station` | 2 x 4 x 2 | **Existing** (alumina-plant.md). |
| electrode paste | item | **Søderberg paste**, briquettes of calcined coke and coal-tar pitch from the anode plant's mixer (anode-plant.md) as a second product: about **20 kg a tonne** of alloy *(unverified)*. Without paste the electrodes burn short and the furnace's power falls (**ELECTRODES SHORT**). |

## Numbers
- **FeCr:** 8 MW at 3.8 MWh/t is **2.1 t/h**, 50 t a day: a small real furnace.
- **FeMn:** 3.2 t/h. **FeSi:** 0.94 t/h.
- **Current:** 10 MVA on the distribution tier is about **460 A** at 12.47 kV (derived). It needs a substation or a big generator: two
  45 MW aeroderivatives' 60 MVA substation transformer carries it; a single 16.5 MW gas turbine does too.
- **Without the capacitor bank** the furnace's 6 Mvar falls on the generators: their kVA rises and a small island's voltage sags.

## Layout (facing north, plan view; the furnace's main cell at 0,0,0)
Layer y 0:
```
x:  -5-4-3-2-1 0 1 2 3 4 5 6 7 8
z0   X X X X . S S S S S S S . .      S submerged arc furnace (7 x 8 x 7)
z1   X X X X . S S S S S S S . .      X furnace transformer (4 x 5 x 5), against the furnace's west face (its secondary side)
z2   X X X X . S S S S S S S B B      B bagging station against the furnace's east face (taphole side)
z3   X X X X . S S S S S S S B B
z4   X X X X . S S S S S S S . .
z5   . C C . . S S S S S S S . .      C capacitor bank (2 x 3 x 4) on the transformer's primary bus
z6   . C C . . S S S S S S S . .
```
Layers y 1-4: the shell and the transformer; y 5-7: the electrode columns, slipping gear and fume hood above the shell, the charge
hoppers on its top at y 7.

The moly roaster and the aluminothermic vessel stand on their own, each with a bagging station against it.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `S` | `wildcatter:submerged_arc_furnace` |
| `X` | `wildcatter:furnace_transformer` |
| `C` | `wildcatter:capacitor_bank` |
| `B` | `wildcatter:bagging_station` |

## Controller, rotation, forming, removing
- **Controller:** the furnace. It forms with a furnace transformer against its west face; it reads NOT BUILT: NO TRANSFORMER otherwise.
  The capacitor bank works on any bus it is wired to, furnace or not.
- **Rotation:** any of the four.
- **Removing the transformer** stops the furnace: the hearth holds its charge and freezes after **4 hours** without power *(proposed,
  as the potline's pots)*; a frozen furnace must be dug out (its block broken and replaced, losing its hearth). Removing the bagging
  station holds the tap.

## How it runs
- The furnace draws power by its tap step (sneak-click on the transformer steps it, or a PLC 4-20 mA set point) and makes alloy at its
  recipe's MWh a tonne while it has charge and paste.
- **Charge:** it reads NO ORE, NO COKE, NO FLUX, NO PASTE. Its readouts: MW, Mvar, PF, electrode current, tap step, hearth level, the
  last tap.
- **The roaster** runs while it has concentrate and its burner gas has somewhere to go (an acid plant's converter on the pipe, or a
  stack: real roasters must not vent, so with no converter it reads NO ACID PLANT and stops, *proposed*).
- **The vessel** fires a batch on command when it holds a full charge.
- **PLC points:** furnace RUN, TAP, a 4-20 mA power set point and MW, Mvar; roaster RUN, RUNNING.

## Left out
- Low- and medium-carbon ferrochrome (the converter and the Perrin process); silicomanganese.
- Prereduction and closed furnaces with CO gas recovery.
- Arc flicker on the grid (SAFs make little).

## Game tests
- `ferroalloys`: a 13.8 kV gas turbine, its GSU and a 138/12.47 kV substation transformer; the furnace transformer and furnace on
  12.47 kV, chromitite, coke, quartz and paste by hopper.
  - The furnace draws **7.5-8.5 MW at PF 0.75-0.85**; with the capacitor bank closed, **PF above 0.93**.
  - A tap (the hearth brought near full by hook) fills a bag of HC ferrochrome.
  - The recipe set to FeSi with quartz, coke and iron: a bag of ferrosilicon after a tap.
  - The paste hopper emptied and time skipped by hook: ELECTRODES SHORT and the MW fall.
- `alumino`: a vessel with moly oxide, iron and aluminium by hopper fires and drops a bag of ferromolybdenum; the moly roaster on test
  concentrate slurry (a stand-in source with a molybdenite assay) puts burner gas into an acid plant's converter.
- **Screenshot:** the furnace with its transformer and capacitor bank.

## Proposed wave
**17a**: one furnace, its transformer and capacitor bank, the roaster and the vessel; the nickel smelter (17b) reuses the transformer.

## As built (2026-11-10; decisions in `docs/questions/phase17-ferroalloys.md`)
- **Parts:** `FerroalloyPartBlock` (furnace_transformer 4 x 5 x 5, submerged_arc_furnace 7 x 8 x 7, capacitor_bank 2 x 3 x 4,
  moly_roaster 3 x 9 x 3, aluminothermic_vessel 3 x 3 x 3), true size from `tools/models/ferroalloys.py`. Numbers in `Tuning` (Phase 17a)
  and `fluid/Ferroalloys` (unit test `FerroalloysTest`).
- **The grid:** no solver change. The furnace is `circuit/FurnaceLoad`, a floating-star constant-power load at PF 0.80 seen from the
  transformer's primary (its box on the west face of its main cell), ramping 10 %/s, its breaker open below 80 % volts. The bank is
  `circuit/ShuntCapacitor`, an ungrounded wye of -jX legs sized for 3 Mvar at the tier's nominal volts (51.8 ohm a leg at 12.47 kV),
  0.2 W/kvar of losses, X following 1/f; its box on the west face of its main cell. Sneak-click opens or closes it; PLC CLOSE, CLOSED,
  AUTO, MVAR.
- **Tap changer:** 15 steps, 105-150 V; P = 8 MW x (V/150)², so tap 1 is 3.9 MW. 38.5 kA an electrode at full tap.
- **Metering:** the transformer reads the furnace's MW, Mvar, PF and electrode kA, and with banks touching it the feeder's PF; transmitters
  read WATTS, VOLTS, AMPS, POWER_FACTOR. In the scene: 8.00 MW, 6.00 Mvar, PF 0.80; the bank closed gives 2.85 Mvar at 12.17 kV and the
  feeder 8.0 MW, 3.1 Mvar, PF 0.935.
- **Furnace:** bins for chromitite, manganese ore, quartz (100 kg), limestone, coke, iron (100 kg ingots) and paste (green anodes,
  1,275 kg each, into 2,000 kg of electrode columns), any of them from a hopper on top or clicked in. Recipes per tonne as the Products
  table; slag 1.2 / 0.6 / 0.03 t a tonne. Hearth 8 t; taps every 2 hours, when full, or on PLC TAP, into up to 10 t waiting for bags;
  the taphole at local (6, 0, 3), lava, flame and smoke there for 20 s. A recipe change: an hour at tap power, nothing made, then a tap.
  ELECTRODES SHORT: half power. Frozen after 4 hours without power once it holds metal. PLC: RUN, TAP, POWER SP 4-20 mA, MW, MVAR,
  RUNNING, AUTO.
- **Moly roaster:** thickened concentrate on the east face of its bottom cells; its assay's `wildcatter:molybdenite` roasted at 0.25 t/h of
  oxide (0.97 kg of MoS2 a kg, 98 % recovery), the SO2 (0.78 kg a kg) on the pipe out of its south side (local 1, 0, 2) as the burner gas
  that carries it; NO ACID PLANT when its 5 s of gas cannot go. `wildcatter:scheelite` dried to 65 % WO3 concentrate bags. 22 kW at LV,
  box west. PLC RUN, RUNNING, AUTO, RATE.
- **Vessel:** bins for moly oxide bags, scheelite concentrate bags, iron and aluminium ingots; FeMo batch 1 bag + 4 iron + 20 aluminium
  = 860 kg; FeW batch 1 bag + 1 iron + 8 aluminium = 648 kg; 10 minutes, fire damage under its hood; fan 15 kW at LV, box west.
- **Items:** 1 t bulk bags of HC ferrochrome, HC ferromanganese, ferrosilicon 75, technical moly oxide, scheelite concentrate,
  ferromolybdenum and ferrotungsten (icons `*_placeholder`), tags `wildcatter:ferroalloys` and `c:ferroalloys/<alloy>`.
- **Scenes:** NeoForge `ferroalloys` and `alumino` as "Game tests" above (two vessel firings fill the bag); client `ferroalloys`
  (filmed). Chromium metal is not built (no chromic oxide before 17c2).
