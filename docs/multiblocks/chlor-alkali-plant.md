# The chlor-alkali plant: brine, a full-size membrane cell room, chlorine liquefaction and HCl

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **20a**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 60-63).

**What exists already.** The ROADMAP idea "chlor-alkali from rock salt" was written in 7a. Since then the zirconium plant (7f) built a
small `chlor_alkali_cell`: 6 bipolar membrane cells at 2.8 kA, **21 kg of chlorine an hour**, rock salt by hopper, hydrogen and (from
10b2) 32 % caustic soda on pipes (zirconium.md). That serves the Kroll loop's make-up. It does not serve a big chlorine user: the
titanium pigment plant (titanium-pigment.md) wants about **0.5 t of chlorine an hour**, the nickel refinery chlorine for its leach, the
lithium plant and the Bayer refinery caustic.

So this spec is the **full-size plant**: brine from rock salt (or solution-mined from a salt dome), brine purification to membrane grade,
a cell room of about **1 t of chlorine an hour**, chlorine drying, compression and liquefaction, caustic concentration, and an HCl
burner that turns spare hydrogen into hydrochloric acid. The small cell stays for the zirconium plant.

Sources:
- zirconium.md (approved): membrane cells at 2,100-2,500 kWh/t of Cl2 at about 3.1 V; 1.128 kg of NaOH a kg of Cl2; 96 % current
  efficiency.
- Membrane brine must hold under about **20 ppb of calcium and magnesium**, which takes a chelating ion-exchange step after primary
  precipitation; chlorine is dried with 98 % sulfuric acid; membrane caustic leaves at 32 % and is evaporated to 50 % *(textbook,
  unverified)*.
- Gulf Coast chlor-alkali plants take brine from **solution-mined salt domes** *(textbook, unverified)*.

## Size *(proposed)*: 1 t of chlorine an hour, 24 t a day
- **Electrolyser:** 1.27 kg of Cl2 a kA·h at 96 % (derived from Faraday): **787 kA·cell an hour**: **52 elements at 15 kA, 3.1 V
  each: 161 V DC, 2.4 MW** (2.4 kWh/kg).
- **Salt:** 1.65 kg a kg of Cl2 (zirconium.md): **1.65 t/h**, about one rock salt block (2,117 kg) every 77 minutes.
- **Caustic:** 1.128 t/h of NaOH, as **3.5 t/h of 32 %** or 2.3 t/h of 50 %.
- **Hydrogen:** 28 kg/h, **315 Nm3/h** (derived).
- **Water:** about 3 L a kg of Cl2 for the 32 % caustic, **3 m3/h** (zirconium.md's figure).
- **Drying acid:** about **15 kg of 98 % H2SO4 a tonne of Cl2** *(unverified)*, leaving as spent 75 % acid (counted).

## Parts
Voltages are the world's standard tiers ("distribution" 12.47 kV or 11 kV; "MV" 4.16 kV or 3.3 kV; "LV" 480 V or 400 V).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `brine_saturator` | 4 x 5 x 4 | **New.** A salt dissolver making saturated brine (26 % NaCl) from rock salt by a hopper on top and the cell room's depleted brine, made up with water. **22 kW at LV.** Or it takes **brine** from a brine well on its north face. |
| `brine_treatment` | 4 x 6 x 8 | **New. Controller.** Primary treatment (caustic soda for magnesium, sodium carbonate made in line from caustic and CO2 for calcium, a clarifier and polishing filters) and a pair of chelating ion-exchange columns, regenerated with the plant's own HCl and caustic. **45 kW at LV.** CO2 off a pipe (isr-operations.md's liquefier) *(proposed: without CO2 the brine holds calcium and the cell room's voltage creeps up, 0.1 V an element a day, until the membranes are replaced: proposed)*. |
| `membrane_cell_room` | 6 x 4 x 16 | **New.** **52 bipolar membrane elements** in two electrolysers in series, **15 kA, 161 V DC, 2.4 MW**, on DC bus bars (dc-bus.md). Chlorine, hydrogen and 32 % caustic out on pipes; depleted brine back to the saturator by touching. Its **+ and −** terminals as the existing cells'. |
| `rectifier_mv` | 3 x 4 x 4 | **New, shared** with conversion-extensions.md: a 12-pulse thyristor rectifier transformer on **distribution voltage**, **3.2 MVA, up to 16 kA and 500 V DC**, PF 0.95, 97 % *(proposed)*. |
| `chlorine_train` | 4 x 6 x 10 | **New.** Chlorine cooler, two sulfuric acid drying towers, a compressor (**250 kW at MV**, *proposed*) and a refrigerated liquefier (**110 kW at LV**). Dry chlorine on the pipe; liquid chlorine to the existing **chlorine filler** (1-ton containers, alumina-plant.md) it touches. Acid off a pipe. |
| `caustic_evaporator` | 3 x 10 x 3 | **New.** A double-effect falling-film evaporator taking 32 % caustic to **50 %** (a new pipe product) on process steam (about **0.7 t of steam a tonne of NaOH**, *unverified*); 32 % passes through when it is off. |
| `hcl_synthesis_unit` | 2 x 8 x 2 | **New.** A graphite burner where hydrogen burns in chlorine to HCl, absorbed in water to **32 % hydrochloric acid** (a new pipe product). Up to **100 kg/h of HCl** *(proposed)*. The rest of the hydrogen goes on its pipe. |
| `brine_wellhead` | 1 block | **New** (optional). A solution-mining wellhead on a hole the rig has drilled into a salt dome (salt-dome-oil.md): fresh water down the inner string from an injection pump (lease.md), brine up the annulus. Each **2,117 kg** of salt it brings up removes one **rock salt block** from the bottom of the hole upward and outward, leaving a cavern of water. |

## Layout (facing north, plan view; the brine treatment's main cell at 0,0,0)
Layer y 0:
```
x:  -4-3-2-1 0 1 2 3 4 5 6 7 8 9 ... 21 22 23 24
z0   S S S S T T T T E E E E E E ...  E  Q  Q  Q      S brine saturator   T brine treatment (y 0-5)
z1   S S S S T T T T E E E E E E ...  E  Q  Q  Q      E membrane cell room (6 x 4 x 16, east)   Q rectifier_mv at its east end
...  (T to z7; E to z5)
z6   . . . . . . . . C C C C . . V V V  H H           C chlorine train (south of E)   V caustic evaporator   H HCl unit
```
Layers y 1-5: the treatment's clarifier and IX columns; the cell room's two electrolysers at y 0-3; the chlorine train's drying towers to
y 5; the evaporator and HCl column to y 7-9.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `S` | `wildcatter:brine_saturator` |
| `T` | `wildcatter:brine_treatment` |
| `E` | `wildcatter:membrane_cell_room` |
| `Q` | `wildcatter:rectifier_mv` |
| `C` | `wildcatter:chlorine_train` |
| `V` | `wildcatter:caustic_evaporator` |
| `H` | `wildcatter:hcl_synthesis_unit` |

## Controller, rotation, forming, removing
- **Controller:** the brine treatment. The saturator and cell room touch it; the rest are on pipe. Any rotation.
- **The cell room** is a DC load: its output follows the current through it. NO DC, NO BRINE, NO WATER, and FULL when its chlorine,
  caustic or hydrogen has nowhere to go (as the small cell).
- **Removing the chlorine train:** the cell room's chlorine is wet and the pipe carries it only to a user that takes wet chlorine (none in
  the mod): it stops (WET CHLORINE, *proposed*). Removing the evaporator or HCl unit just passes their stream on.

## Left out
- Sodium hypochlorite, a cooling-water biocide (the mod has no biofouling).
- Mercury and diaphragm cells.
- Gas storage in the salt caverns the brine wells leave (salt-dome-oil.md, Left out).

## Game tests
- `chlorAlkali`: a gas turbine through its GSU and a 138/12.47 kV substation to the rectifier_mv at 15 kA; rock salt by hopper, water and
  acid from test tanks, CO2 from a test tank:
  - the rectifier reads about **161 V and 2.4 MW**; chlorine fills a ton container (brought near full by hook); caustic and hydrogen flow;
  - the evaporator puts out 50 % caustic; the HCl unit 32 % acid;
  - the chlorine train broken: WET CHLORINE.
- `brineWell`: a hole into a placed salt column, a brine wellhead and an injection pump: brine reaches the saturator, and a rock salt block
  at the hole's bottom becomes water.
- **Screenshot:** the cell room.

## Proposed wave
**20a**, before the titanium pigment plant (20b), which needs its chlorine.
