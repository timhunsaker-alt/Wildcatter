# Conversion to UF6 (APPROVED 2026-11-01 with the Phase 7d plan)

Yellowcake is made into uranium hexafluoride by the dry fluoride-volatility process of Metropolis Works, Illinois. The steps are:
- reduce it with hydrogen;
- hydrofluorinate it with HF;
- fluorinate it with fluorine;
- freeze the UF6 out in cold traps, distil it;
- fill it into 48Y cylinders.

Three supporting plants make the reagents:
- a steam methane reformer for the hydrogen;
- a rotary kiln on fluorspar and the 7b acid for the HF;
- fluorine cells on a DC rectifier for the fluorine.

Decided with Tim on 2026-11-01:
- **DC:** the rectifier is an ordinary three-phase AC load. Its DC side (volts, kA, cells in series) is worked out inside it and shown in its readouts, with no DC tier in the solver.
- **Size:** the plant is **quarter size**: 12 fluorine cells, about 0.7 MW at 4.16 kV.

Sources:
- [A] Honeywell Metropolis Works licence application to the NRC (2022);
- [B] Lawroski et al., ANL A/CONF.15/P/1552 (1958), the Metropolis design basis;
- ConverDyn;
- Buss ChemTech's AHF fact sheet;
- patent US 4,511,440 (fluorine cell);
- KAERI / ANSI N14.1 (cylinders);
- packaged-SMR listings (Caloric, Koch-Glitsch).

A figure marked *(unverified)* is textbook, an estimate or a proposal.

```
yellowcake drums (hopper) -> REDUCTION REACTOR -> HYDROFLUORINATOR -> FLUORINATOR -> COLD TRAPS -> DISTILLATION COLUMNS -> CYLINDER FILLER
                              ^ hydrogen (pipe)   ^ anhydrous HF        ^ fluorine                                       ^ empty 48Y (hopper)
STEAM REFORMER (gas or LPG, water) -> hydrogen
HF KILN (fluorspar by hopper, acid, gas or LPG) touching HF COLUMN -> anhydrous HF
RECTIFIER (4.16 kV) touching FLUORINE CELL HOUSE (anhydrous HF, water) -> fluorine
```
The main line passes its solids and its UF6 by touching, the way they move inside a real plant.

## Size
- **Fluorine:** 12 cells at 6 kA and 95 % current efficiency (0.709 kg of F2 a kAh) give **48.5 kg of F2 an hour**.
- **Uranium:** at 0.18 kg of F2 a kg of U, that converts **0.075 kg of U a second**, about 2,350 t a year. Metropolis runs at 7,000 t now (15,000 nameplate).
- **Drums:** a yellowcake drum (271 kg of U) lasts an hour.
- **Cylinders:** a 48Y cylinder (12,501 kg of UF6, 8,453 kg of U) takes about **31 hours**.

## Parts (cells W x H x L, all *unverified* sizes)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `reduction_reactor` | 3 x 10 x 3 | Fluid-bed reducer at 575 C [B].<br>• Yellowcake drums by a hopper on top; the empty drums come out to a hopper under it, ready for the drum filler again.<br>• Hydrogen: **8.47 g a kg of U**, 0.094 m3, since magnesium diuranate is U(VI). The N2 that dilutes it is left out.<br>• The reactions here and downstream give off heat, so start-up heaters are left out. |
| `hydrofluorinator` | 3 x 10 x 6 | Two fluid beds in series, 350 C at the top to 600 C at the bottom [B].<br>• Anhydrous HF: **0.43 kg a kg of U** (0.336 ideal, plus 25-30 % excess [B]), plus **0.084 kg a kg of U** for the magnesia in the yellowcake, which becomes MgF2.<br>• The off-gas (water and the excess HF) goes to its KOH scrubber; counted. |
| `fluorinator` | 3 x 10 x 3 | Fluid bed on an inert CaF2 bed, 400-450 C [A, B].<br>• Fluorine: **0.18 kg a kg of U** (0.160 ideal; estimate).<br>• The magnesium and other impurities stay in the bed as fluorides: "fluorinator ash", counted in kg. Retiring the bed is left out. |
| `cold_traps` | 3 x 4 x 6 | Primary, secondary and tertiary traps, the primary at -29 to -18 C [A]. Refrigeration **30 kW at 480 V** *(unverified)*. They freeze the UF6 out of the fluorinator's gas and melt it out to the columns. |
| `distillation_columns` | 3 x 12 x 3 | The "low boiler" and "high boiler" columns, 75 C at 30 psia [B]. Their reboiler is electric here, **20 kW at 480 V** (Metropolis uses steam). Product UF6 is 99.99 %, under 300 ppm impurities [A]. |
| `cylinder_filler` | 3 x 3 x 6 | 48Y fill station on its scale, **5 kW at 480 V** *(unverified)*.<br>• Empty 48Y cylinders by a hopper on top.<br>• It fills each with **12,501 kg of UF6** as liquid (ANSI N14.1).<br>• Full ones go out to a hopper under it, or to an empty hand. |
| `steam_reformer` | 3 x 8 x 4 | Packaged steam methane reformer with shift and PSA: **30 Nm3/h of hydrogen** *(proposed; the plant needs 25)*.<br>• **0.45 m3 of gas a m3 of hydrogen**, feed and fuel together (Caloric 6,000 Nm3/h, Koch-Glitsch 1,300 Nm3/h). LPG at the same heat also works, as propane reformers run.<br>• Steam:carbon 3 at 850-900 C. Its steam is made from pumped water, **0.015 L a m3 of hydrogen** (derived). |
| `hf_kiln` | 3 x 4 x 10 | Pre-reactor and indirectly fired rotary kiln, gas out at about 205 C (EP 1,167,286). Per kg of anhydrous HF (Buss ChemTech):<br>• **2.165 kg of fluorspar** (CaF2, by a hopper on top);<br>• **2.6 kg of H2SO4** (acid on pipe);<br>• **5 MJ of fuel** (gas or LPG);<br>• its drive **15 kW at 480 V**.<br>• It makes about **0.06 kg of HF a second** *(proposed; the plant needs 0.053)*. Anhydrite is counted: 3.6 kg a kg of HF *(unverified)*. |
| `hf_column` | 2 x 10 x 2 | Scrubbing, condensing and purification against the kiln: 99.98 % anhydrous HF out on pipe. |
| `rectifier` | 3 x 4 x 4 | Rectifier transformer and 12-pulse diode bridge on **4.16 kV**: power factor **0.95**, **97 %** efficient *(both unverified)*. It drives **6 kA** through the cell house it touches, at the cells' total voltage. On the AC side it is a variable three-phase load following the cells. |
| `fluorine_cell_house` | 6 x 4 x 12 | **12 KF·2HF cells in series** at 80-110 C, with carbon anodes and steel cathodes (US 4,511,440).<br>• **9 V a cell** at 6 kA (8-10 V), so **108 V DC, 650 kW**.<br>• **95 %** current efficiency: 13.4 kWh a kg of F2.<br>• Anhydrous HF in, **1.053 kg a kg of F2**; fluorine out on pipe.<br>• The cathode hydrogen goes up its vent; real plants vent it or burn it.<br>• About two thirds of the power is heat. Its cooling tower's make-up water is **0.19 L/s** (433 kW evaporated; derived), from a pump. |

## Products and items
- **New on steel pipe:**
  - hydrogen (m3);
  - anhydrous HF (L, 0.99 kg a litre; no stock tank holds it, because real AHF is stored chilled under pressure);
  - fluorine (kg).
- **Empty 48Y cylinder:** 48 in steel, 2,359 kg tare, crafted from steel.
- **48Y cylinder of UF6:** 12,501 kg of natural UF6 (0.711 % U-235), 8,453 kg of U.
- **Fluorspar:** the 7a mineral, 2,544 kg of CaF2 a block. One block lasts about 9.5 hours at the plant's rate.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Reagents:**
  - the reducer's hydrogen, the hydrofluorinator's HF and the fluorinator's fluorine come in on the east face of their bottom cells;
  - the kiln's acid and fuel also come in on the east face of its bottom cells;
  - the reformer's fuel comes in on the east face of its bottom cells too.
- **Outputs:**
  - the reformer's hydrogen goes out of the south face of its back cells (z 3, bottom);
  - the HF column's anhydrous HF goes out of the south face of its bottom cells (z 1);
  - the cell house's fluorine goes out of the south face of its back cells (z 11, bottom), and its HF comes in on the north face of its front cells (z 0, bottom).
- **480 V boxes:** on the west face of the main cell of each powered part: cold traps, distillation columns, cylinder filler and HF kiln.
- **4.16 kV:** on the rectifier, the west face of its main cell.
- **Water:** the reformer and the cell house take water off a pump.
- **Hoppers:** drums on the reducer, fluorspar on the kiln and empty cylinders on the filler go in from on top. Empty drums and full cylinders come out to a hopper under the part.

## How it runs
- **The main line:** each stage takes what the one before it holds, as fast as its reagent allows:
  - reducer: NO HYDROGEN;
  - hydrofluorinator: NO HF;
  - fluorinator: NO FLUORINE;
  - cold traps and columns: their 480 V.
- **Rates:** each stage is rated at the plant's 0.075 kg of U a second, with a fifth more to catch up.
- **The cell house:** it asks the rectifier for its 6 kA. Its fluorine follows the share of power the rectifier gets.
  - With no rectifier against it: NO DC.
  - Without HF or water it stops.
- **Removing a part** stops what needs it.

## Game test
`conversion`:
- **Power:** three diesel sets. Two are paralleled on a busway through a backward unit substation to 4.16 kV for the rectifier; one runs the 480 V loads.
- **Feeds:**
  - LPG from a bullet for the reformer and the kiln;
  - acid from a test-filled stock tank;
  - fluorspar and yellowcake drums by hopper;
  - water by hook.
- **Checks:**
  - hydrogen, HF and fluorine flow;
  - the rectifier reads about 6 kA and about 0.67 MW;
  - the main line carries uranium through to the filler;
  - a cylinder that a hook has filled to just under full is finished by the plant and drops into the hopper under the filler;
  - empty drums come out under the reducer.
- **Screenshot:** the plant.

## Added while building (7d)
- The rectifier is the land rig's variable drive load, given its own power factor (0.95) and **no frequency floor**: a diode
  rectifier does not back off as the rig's drives do. On a weak island it drags the frequency down with it.
  - At 3,954 V on two paralleled 500 kW sets it gets 603 of its 668 kW, 5.4 kA, and the fluorine follows.
  - Brought onto one set before the second has synced, it trips that set on frequency. The game test starts the cells only
    once both sets are on the bus.
- The rectifier faces south in the test, so its 4.16 kV box is on its east side, away from the cell house its bus bars touch.
