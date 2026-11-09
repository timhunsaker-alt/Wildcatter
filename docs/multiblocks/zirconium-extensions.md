# Zirconium extensions: fumed silica, hafnium metal, Pidgeon magnesium, chromium, scrap, batches

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **17c** (17c1 batches, scrap and
> hafnium; 17c2 Pidgeon magnesium, chromium and fumed silica). Decisions are in `docs/questions/specs-wildcatter.md` (items 72-78).

Seven items the zirconium spec (zirconium.md, approved 2026-11-01) left in ROADMAP Ideas. Each has its real basis and a proposed scope.
Numbers use zirconium.md's: a line of 10 kg of Zircaloy-4 an hour; per kg of Zr 2.01 kg of zircon and 3.11 kg of Cl2, half leaving as
SiCl4; hafnium counted at 2 % of the metal.

## 1. SiCl4 to fumed silica
**Real basis.** Zircon chlorination makes as much silicon tetrachloride as zirconium tetrachloride. Real plants sell it; burned in a
hydrogen-oxygen flame it makes **fumed silica** and HCl: SiCl4 + 2 H2 + O2 -> SiO2 + 4 HCl *(textbook)*.

**Numbers (derived):** 1.86 kg of SiCl4 a kg of Zr: **18.6 kg/h**, making **6.6 kg/h of fumed silica** and **16 kg/h of HCl**; it
takes **4.9 Nm3/h of hydrogen**, about what the small chlor-alkali cell gives (6.7 Nm3/h at 21 kg of Cl2 an hour).

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `fumed_silica_burner` | 2 x 6 x 3 | **New.** A flame-hydrolysis burner, coagulation pipes, a cyclone and a baghouse. Against the chlorinator (its SiCl4 by touching); hydrogen and oxygen (vpsa_oxygen, titanium-pigment.md) on its east face. **15 kW at LV.** Fumed silica in 25 kg sacks *(proposed)* to a hopper under it; **32 % hydrochloric acid** (chlor-alkali-plant.md's product) out of its south face. Without it, SiCl4 is counted as today. |
- **Use:** fumed silica is tagged for other mods; in Wildcatter it has none (questions file).

## 2. Uses for the chlor-alkali caustic
**Already done:** since 10b2 the caustic goes to the Bayer refinery (bayer-refinery.md). This plan adds the lithium plant
(lithium.md), the brine treatment (chlor-alkali-plant.md), the chromate kiln (item 5) and the nickel refinery's purification. **Nothing
to build here**; the item can be struck from the Ideas list (questions file).

## 3. Hafnium metal for control rods
**Real basis.** The hafnium the column takes out (zircon holds 1-3 %) is the raw material for hafnium metal, reduced by the Kroll process
like zirconium and made into control rods for some PWRs and naval reactors *(textbook)*. ROADMAP Phase 8: "hafnium would take about
1,400 h of the 7f line for one set", which is why the reactor uses B4C.

**Proposed scope:**
- The hafnium column's HfCl4 goes out to a hopper under it as **hafnium tetrachloride drums** (50 kg of Hf a drum, *proposed*) instead of
  being counted.
- **A hafnium campaign:** a sneak-click on the Kroll reactor switches it to **HfCl4 by hopper**; the vacuum distillation, VAR furnace and
  pilger mill follow it, the VAR melting it **unalloyed**, the pilger mill's fourth die making **hafnium rodlets** (Hf metal, 9.7 mm,
  3.66 m, about 3.6 kg each, *derived from the B4C rodlet's size and Hf's 13.3 g/cm3*).
- **The RCCA bench** (boron.md) makes a **hafnium RCCA** from 24 rodlets; the reactor's worth for it is in reactor-extensions.md.
- **Pace:** at the line's 2 % hafnium, about **0.2 kg of Hf an hour**: a set of 5 RCCAs (120 rodlets, 430 kg) takes about **2,100 hours**
  of the line's hafnium (derived; the ROADMAP's 1,400 h assumed a different rodlet). The campaign itself is quick; the waiting is the
  hafnium. It is meant as a very late, very slow goal.

## 4. Pidgeon magnesium from dolomite
**Real basis.** The Pidgeon process reduces calcined dolomite with ferrosilicon in horizontal retorts at about **1,200 C** under vacuum;
magnesium vapour condenses as a "crown" at the retort's cold end. Each tonne of Mg takes about **10-12 t of dolomite and about 1 t of 75 %
ferrosilicon** *(textbook, unverified)*. ores.md: "Dolomite waits for a Pidgeon magnesium plant."

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `lime_kiln` | 3 x 14 x 3 | **Existing** (alumina-plant.md): with dolomite in its hopper it makes **dolime** (calcined dolomite) bags *(proposed)*. |
| `pidgeon_furnace` | 4 x 4 x 10 | **New.** A gas-fired furnace holding a bank of alloy-steel retorts with their vacuum pumps and condensers: dolime, FeSi (ferroalloys.md) and 3 % fluorspar briquetted by a hopper on top. **10 kg of Mg an hour** *(proposed)*, about **0.5 MW of gas** *(proposed: about 180 MJ a kg of Mg; real plants are coal-fired and use more, unverified)*, vacuum pumps **30 kW at LV**. A **12-hour cycle** *(proposed; real 10-12 h)*: crowns come out to a hopper under it as **magnesium ingots** (22.7 kg) at the cycle's end. |
- **The Kroll loop** takes magnesium ingots by the magnesium cell's hopper as make-up, alongside or instead of the magnesia chlorinator.
- **Slag:** dicalcium silicate, about 6 t a tonne of Mg, counted.

## 5. The aluminothermic chromium step
**Real basis.** Zircaloy's chromium is added as **chromium metal**, made by reducing **chromic oxide** with aluminium. Chromic oxide comes
from chromite by an **alkaline roast** to sodium chromate, leaching, and reduction (with sulfur) to Cr2O3 *(textbook, unverified)*.
zirconium.md uses a chromite block as a stand-in.

**Proposed scope** (lowest priority here; the VAR furnace needs only about **10 g of chromium an hour**):
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `chromate_kiln` | 3 x 3 x 8 | **New.** A rotary kiln at about 1,100 C roasting chromite with caustic soda in air to sodium chromate, then a leach and a sulfur reduction to **chromic oxide** (25 kg sacks). **0.3 MW** of gas, **15 kW at LV** *(proposed)*. **Hexavalent chromium:** broken while it holds chromate, it leaves a toxic spill that poisons a player standing in it *(proposed)*. |
- The **aluminothermic vessel** (ferroalloys.md) reduces it to **chromium metal**; the VAR furnace takes chromium metal by hopper and uses
  it in place of chromite.

## 6. Scrap recycle and yields
**Real basis.** From sponge to finished tube a zirconium line keeps only part of its metal: crop ends, turnings, rejected tubes. Clean
scrap goes back into the VAR charge *(textbook, unverified fractions)*.

**Proposed scope:**
- **Losses:** extrusion **8 %**, pilgering **6 %**, inspection **2 %** *(proposed)*: about 84 % of the metal reaches a tube.
- **Scrap:** the lost metal goes out to a hopper under the pilger mill as **Zircaloy scrap** (25 kg). The VAR furnace takes it by hopper,
  up to **30 % of its charge** *(proposed)*, saving sponge.
- **The line's rate:** its 10 kg an hour becomes 8.4 kg of tube, or 10 with scrap recycled.

## 7. Kroll and VAR batch timing
**Real basis.** A Kroll retort is a batch of several days; vacuum distillation another; VAR ingots are melted twice in heats of hours
(zirconium.md's Left out).

**Proposed scope:**
- **Kroll:** the reactor holds **two retorts** that alternate (not a second part): each a **50 kg batch** over **5 hours**, then **4 hours**
  in the vacuum distillation *(proposed)*. The average stays 10 kg an hour; metal arrives in 50 kg lumps, and the readout shows each
  retort's step.
- **VAR:** a **100 kg ingot**, melted twice, **1 hour a melt** *(proposed)*.
- Old worlds: a line mid-flow keeps its metal; the first batch starts when the next 50 kg is in.

## Layout (the 7f plant, facing north, plan view; new parts marked)
Layer y 0:
```
 ... C C C U U          C chlorinator (3 x 10 x 3)     U fumed silica burner (new, y 0-5), against its east face
     C C C U U
     C C C U U
 ... K K K            K Kroll reactor   P Pidgeon furnace (new, 4 x 4 x 10), on its own, its ingots to the magnesium cell by hopper
 P P P P P P P P P P   R chromate kiln (new, 3 x 3 x 8), on its own
```
The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `C` | `wildcatter:chlorinator` |
| `U` | `wildcatter:fumed_silica_burner` |
| `K` | `wildcatter:kroll_reactor` |
| `P` | `wildcatter:pidgeon_furnace` |
| `R` | `wildcatter:chromate_kiln` |

## Controller, rotation, forming, removing
The zirconium line has no single controller; each stage takes what the one before it holds (zirconium.md). The fumed silica burner works
against the chlorinator; the Pidgeon furnace and the chromate kiln are their own controllers. Any rotation. Removing a new part returns
its stream to being counted.

## Game tests
- `zirconium` (extended, 17c1): a Kroll batch given by hook finishes as a 50 kg lump into the distillation; the pilger mill's scrap
  drops into a hopper and the VAR takes it; a HfCl4 drum appears under the hafnium column (brought near full by hook); a hafnium
  campaign makes a hafnium rodlet.
- `pidgeon` (17c2): dolime, FeSi and fluorspar by hopper; a cycle (sped by hook) drops magnesium ingots; the magnesium cell takes them.
- `chromium` (17c2): chromite and caustic to chromic oxide; the aluminothermic vessel to chromium metal; the VAR uses it.
- `fumedSilica` (17c2): hydrogen and oxygen from test tanks: a sack of fumed silica and HCl on its pipe.
- **Screenshot:** the Pidgeon furnace.

## Proposed waves
- **17c1:** batches, scrap and hafnium (no new parts; the pilger mill's fourth die and new items).
- **17c2:** Pidgeon magnesium, the chromium step, fumed silica (three new parts; need 17a's FeSi and aluminothermic vessel).
