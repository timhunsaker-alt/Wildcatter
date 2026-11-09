# Conversion extensions: cathode hydrogen, the ash bed, KOH regeneration, a full-size plant

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **16b** (16b1 the first three
> items; 16b2 the full-size plant). Decisions are in `docs/questions/specs-wildcatter.md` (items 27-30).

Four items the conversion spec (conversion.md, approved 2026-11-01) left in ROADMAP Ideas. Each has its real basis and a proposed
scope. Numbers derive from conversion.md's own: 12 cells at 6 kA, 48.5 kg of F2 an hour, 0.075 kg of U a second.

## 1. The fluorine cells' cathode hydrogen to the reducer
**Real basis.** A KF·2HF cell makes one H2 at its cathode for each F2 at its anode (2 HF -> H2 + F2). conversion.md: "The cathode
hydrogen goes up its vent; real plants vent it or burn it." It carries HF vapour, so using it needs a scrubber (sodium fluoride beds
hold HF and give it back when heated, as at real fluorine plants *(textbook, unverified)*).

**Numbers (derived):**
- 48.5 kg of F2 an hour is 1,276 mol; the same moles of H2 are **2.57 kg, 28.6 Nm3 an hour**.
- The reducer needs **25.4 Nm3/h** on magnesia drums (0.094 m3 a kg of U) and **17 Nm3/h** on ADU drums (adu-yellowcake.md).
- So the cells' hydrogen **covers the reducer**. The steam reformer is then needed only to start (the cells need no hydrogen to start)
  and for the fuel plant.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `cathode_gas_scrubber` | 2 x 4 x 2 | **New.** Twin NaF beds (one adsorbing, one regenerating at about 300 C) and a KOH polishing column. **10 kW at 480 V** *(proposed)*. It takes the cell house's cathode gas on its north face and puts **hydrogen** on the pipe at its south face. It stands against the cell house's back cells. |
- The cell house gets a **cathode gas** nozzle on the west face of its back cells (z 11, bottom), which the scrubber touches; without
  one the hydrogen vents as today.

## 2. Retiring the fluorinator's ash bed
**Real basis.** At Metropolis the fluorinator's bed of inert CaF2 slowly fills with the fluorides of what was not uranium: the
"fluorinator ash", which still holds some uranium, is drummed and sent back to a mill for recovery *(Honeywell's NRC licence
application [A] in conversion.md; the U content is unverified)*.

**Numbers *(proposed unless marked)*:**
- Magnesia drums: 0.084 kg of HF a kg of U makes **0.131 kg of MgF2 a kg of U** (derived): about 1.1 t a 48Y.
- ADU drums: about a tenth of that.
- The bed holds **2 t of ash**; then it reads **BED FULL** and stops.
- The ash holds **3 % uranium** *(proposed)*.

**Proposed scope:**
- A sneak-click on the fluorinator (or a PLC command) **retires the bed**: it stops for **10 minutes** *(proposed)*, dumps the ash into
  **fluorinator ash drums** (empty drums by a hopper on top, 400 kg of ash a drum, out to a hopper under it), and takes **500 kg of
  fluorspar** by its hopper for a new bed.
- **Ash drums go to the uranium mill:** the leach tank takes them by hopper and leaches their uranium with the ore (its acid and
  oxidant as for ore, *proposed*). No new part.

## 3. Regenerating the KOH scrubbers
**Real basis.** The hydrofluorinator's off-gas carries its excess HF to a potassium hydroxide scrubber (KOH + HF -> KF + H2O). Plants
regenerate the KOH with lime, 2 KF + Ca(OH)2 -> CaF2 + 2 KOH, and the CaF2 is a synthetic fluorspar *(textbook, unverified)*.
conversion.md counts the off-gas; nothing scrubs it.

**Numbers (derived):**
- Excess HF: 0.43 − 0.336 = **0.094 kg a kg of U**, **25.4 kg an hour** at the plant's rate.
- Lime: **1.40 kg of quicklime a kg of HF**, **35.6 kg/h** (the alumina plant's lime kiln, alumina-plant.md, in 1 t bags).
- CaF2 made: **1.95 kg a kg of HF**, **49.5 kg/h**: about 2 % of the HF kiln's fluorspar comes back.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `koh_scrubber` | 3 x 8 x 3 | **New.** A packed scrubber, its KOH recirculation tank, and a lime reactor and filter that regenerate the liquor. **15 kW at 480 V** *(proposed)*. It stands against the hydrofluorinator. Quicklime bags by a hopper on top; a first charge of **2,000 kg of KOH** (below); the CaF2 cake goes out to a hopper under it as **synthetic fluorspar**, which the HF kiln takes as fluorspar. |
- **The first KOH:** caustic potash from the chlor-alkali cell run on **potash** instead of rock salt (a membrane cell makes KOH from KCl
  brine as it makes NaOH from NaCl, *textbook*): the cell takes potash by its hopper and puts out **caustic potash** on its caustic
  nozzle. At the zirconium plant's cell's 21 kg/h of Cl2 that is **33 kg/h of KOH** (1.58 kg a kg of Cl2, derived) and **2.1 kg of KCl a kg
  of Cl2**, so 2,000 kg takes about **60 hours**; the chlor-alkali plant (chlor-alkali-plant.md) does it in about 2.
- **Without lime** the scrubber's KOH is spent in about 28 hours *(derived: 2.8 kg of KOH a kg of HF)*: it reads **SPENT** and the
  hydrofluorinator stops. **Without a scrubber** the hydrofluorinator runs and its HF is counted, as today *(proposed: this wave adds the
  part; it does not make old plants stop)*.

## 4. A full-size plant
**Real basis.** The 7d plant is quarter size: 12 cells, about 2,350 t of U a year. Metropolis runs at about 7,000 t (15,000 nameplate)
(conversion.md).

**Proposed scope** (16b2, lowest priority here):
- **The cells:** up to **four cell houses in series** on one DC bus, as the potline puts pots in series (potline.md): 48 cells at 6 kA,
  **432 V DC, about 2.6 MW**. The 4.16 kV rectifier is rated for one house; a **12.47 kV fluorine rectifier** (`rectifier_mv`, 3 x 4 x 4,
  the same 12-pulse bridge, 6 kA to 500 V, 3 MW, PF 0.95, 97 % *(proposed)*) drives four.
- **The main line:** a second, larger size of the reducer, hydrofluorinator, fluorinator, cold traps and columns, each rated
  **0.3 kg of U a second** (four times the 7d parts) at twice their height *(proposed)*. A mixed line runs at its slowest part.
- **What it makes:** a 48Y about every 8 hours, about 9,400 t of U a year *(derived)*.

## Layout (the 7d plant, facing north, plan view; new parts marked)
Layer y 0:
```
 ... H H H S S S ...        H hydrofluorinator (3 x 10 x 6)   S KOH scrubber (new, y 0-7), against it
     H H H S S S
     H H H S S S
 ... F F F F F F (z 0..11) .. G G      F fluorine cell house    G cathode gas scrubber (new, y 0-3), against its back cells
```
The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `H` | `wildcatter:hydrofluorinator` |
| `S` | `wildcatter:koh_scrubber` |
| `F` | `wildcatter:fluorine_cell_house` |
| `G` | `wildcatter:cathode_gas_scrubber` |

## Controller, rotation, forming, removing
The conversion plant has no single controller: each stage takes what the one before it holds (conversion.md). The new parts work by
touching the part they serve; any rotation. Removing one returns that part to today's behaviour (vented hydrogen, counted HF).

## Game tests
- `conversion` (extended, 16b1):
  - the cathode gas scrubber puts **28 Nm3/h ± 10 %** of hydrogen on a pipe to the reducer, and the reducer runs with the reformer off;
  - a fluorinator given 1,990 kg of ash by hook reads BED FULL after a few drums; a retire fills an ash drum and takes 500 kg of
    fluorspar; the ash drum in the mill's leach tank adds uranium to its slurry;
  - the KOH scrubber with a first charge and lime puts CaF2 into the hopper under it; with the lime gone it reads SPENT.
- `conversionLarge` (16b2): four cell houses in series on the 12.47 kV rectifier read about 432 V and 6 kA; the large main line fills a
  48Y that a hook has brought near full.

## Proposed waves
- **16b1:** cathode hydrogen, the ash bed, the KOH scrubber (three new parts, two new items: ash drum, caustic potash).
- **16b2:** the full-size plant.
