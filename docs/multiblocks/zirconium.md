# The zirconium plant (APPROVED 2026-11-01 with the Phase 7f plan)

Beach-sand zircon becomes Zircaloy-4 cladding tube and guide thimbles for the 17×17 fuel assembly (7g). The steps are:
- mineral separation;
- carbochlorination;
- hafnium removal by extractive distillation;
- Kroll reduction with magnesium, then vacuum distillation;
- vacuum arc remelting with tin, iron and chromium;
- extrusion;
- cold pilgering with vacuum anneals.

Decided with Tim on 2026-11-01:
- **Waves:** fuel fabrication is two waves, this zirconium plant (7f) and then the fuel plant (7g).
- **Chlorine and magnesium:** a **magnesium cell** recycles the Kroll MgCl2 to Mg and Cl2, as real Kroll plants do.
  - Make-up chlorine comes from a **chlor-alkali** membrane cell on rock salt.
  - Make-up magnesium comes from **chlorinating magnesia**.

Sources:
- Carbochlorination: ZrSiO4 + 4C + 4Cl2 -> ZrCl4 + SiCl4 + 4CO in a fluid bed at about 1,200 °C (ScienceDirect, *Zirconium compound*; the ecoinvent dataset for ZrCl4 from zircon).
- Hafnium separation: extractive distillation in molten KCl-AlCl3 at 250-400 °C (US 4,874,475; US 6,929,786). Zircon holds 1-3 % Hf.
- Kroll: ZrCl4 + 2Mg -> Zr + 2MgCl2 in a sealed retort at about 1,123 K. Vacuum distillation of the Mg and MgCl2 at 920-980 °C.
- Magnesium cells: 7-13 kWh/kg Mg (US 5,439,563; METU studies).
- Chlor-alkali: membrane cells at 2,100-2,500 kWh/t Cl2 at about 3.1 V (DOE; Wikipedia); 1.128 kg of NaOH a kg of Cl2.
- Zircaloy-4: 1.2-1.7 % Sn, 0.18-0.24 % Fe, 0.07-0.13 % Cr (US 4,717,434; US 4,584,030).
  - It is melted by repeated vacuum consumable-electrode melting and extruded.
  - Then 2-5 cold pilger passes of 50-85 % each, with anneals at 675-700 °C between them.
- The 17×17 rod: 9.50 mm OD, 0.57 mm wall, 3.85 m long. The guide thimble: 12.24 mm x 0.41 mm (Westinghouse 17×17 standard design).

A figure marked *(unverified)* is textbook, an estimate or a proposal.

```
heavy minerals (hopper) -> MINERAL SEPARATOR -> CHLORINATOR -> HAFNIUM COLUMN -> KROLL REACTOR -> VACUUM DISTILLATION
                                                 ^ Cl2 pipe, coal (hopper)             ^ Mg, MgCl2 back: MAGNESIUM CELL ^ Mg back
  -> VAR FURNACE (tin, iron, chromite by hopper) -> EXTRUSION PRESS -> PILGER MILL -> VACUUM ANNEAL FURNACE -> tubes (hopper under it)
CHLOR-ALKALI CELL (rock salt by hopper, pumped water) against a RECTIFIER: Cl2 and hydrogen out on pipes, NaOH counted
MAGNESIUM CELL against a RECTIFIER, the Kroll reactor, the vacuum distillation and the MAGNESIA CHLORINATOR: Cl2 out on a pipe
MAGNESIA CHLORINATOR (magnesia and coal by hopper, Cl2 pipe): make-up MgCl2 for the magnesium cell
```
The metal passes from part to part by touching, as it moves inside a real plant.

## Size: a small line, 10 kg of Zircaloy-4 an hour *(proposed)*
- **An assembly** takes 264 cladding tubes at **0.404 kg** each (15.99 mm² x 3,850 mm x 6.56 g/cm³), 107 kg. Its 25 guide and instrument thimbles add 0.40 kg each, 10 kg.
- **Pace:** about **12 hours of the line an assembly**. The smallest cascade makes an assembly's uranium in about 29 hours (enrichment.md).
- **Per kg of Zr** (stoichiometry):
  - 2.01 kg of zircon;
  - 0.58 kg of carbon (0.526, plus 10 %);
  - 3.11 kg of Cl2 into the chlorinator, half of it leaving as SiCl4;
  - 0.533 kg of Mg reacted (0.666 kg charged at 25 % excess), which gives 2.09 kg of MgCl2;
  - the magnesium cell turns that back into the Mg and 1.555 kg of Cl2.
- **Items:**
  - a heavy-minerals item (900 kg, a quarter of it zircon) lasts about 11 hours;
  - a magnesia item (1,248 kg of MgO) is 752 kg of Mg for the loop;
  - a rock salt item (2,117 kg) is about 60 hours of the chlor-alkali cell.
- **Coal or charcoal:** a piece counts as 150 kg at 80 % carbon, a ninth of a 1,350 kg coal block *(proposed)*.

## Parts (cells W x H x L, all sizes and watts *unverified*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `mineral_separator` | 3 x 4 x 6 | A dry mineral separation plant: rare-earth magnets take the ilmenite, electrostatic rolls the rutile.<br>• **90 %** of the zircon goes on *(proposed)*.<br>• Ilmenite, rutile and monazite are counted in kg.<br>• Heavy minerals by a hopper on top; **30 kW at 480 V**. |
| `chlorinator` | 3 x 10 x 3 | A fluid-bed carbochlorinator at about 1,200 °C.<br>• Zircon from the separator it touches, carbon from coal by a hopper on top, chlorine off the pipe at the east face of its bottom cells.<br>• The reaction keeps it hot; its fluidizing blower draws **15 kW at 480 V**.<br>• SiCl4 and CO go to its scrubber and are counted. |
| `hafnium_column` | 3 x 12 x 3 | Extractive distillation in molten KAlCl4 at about 350 °C. The salt is a charge that stays in the column.<br>• ZrCl4 goes on with **under 100 ppm Hf**.<br>• HfCl4 is counted: 2 % of the metal *(unverified)*.<br>• Its heaters draw **50 kW at 480 V**. |
| `kroll_reactor` | 3 x 6 x 3 | A sealed steel retort in an electric furnace at about 850 °C: ZrCl4 vapour onto molten magnesium.<br>• It takes Mg from the magnesium cell it touches and taps the MgCl2 back to it.<br>• **100 kW at 480 V**. |
| `vacuum_distillation` | 3 x 6 x 3 | A vacuum furnace at 920-980 °C. It boils the excess Mg out of the sponge and back to the magnesium cell it touches.<br>• **100 kW at 480 V**. |
| `var_furnace` | 3 x 8 x 3 | Vacuum arc remelting, twice, into Zircaloy-4: **1.5 % Sn, 0.21 % Fe, 0.10 % Cr**.<br>• Additions by a hopper on top, drawn down by the kg:<br>&nbsp;&nbsp;– a tin ingot, 271 kg of tin (its cassiterite block);<br>&nbsp;&nbsp;– an iron ingot, 100 kg *(proposed)*;<br>&nbsp;&nbsp;– a chromite, 1,227 kg of chromium (its block). The aluminothermic chromium step is left out.<br>• **1 kWh a kg a melt** *(unverified)*: **20 kW at 480 V**. |
| `extrusion_press` | 3 x 4 x 8 | The ingot is forged, beta-quenched, bored and hot-extruded into tube hollows.<br>• Its hydraulic pumps draw **250 kW at 4.16 kV**. |
| `pilger_mill` | 3 x 3 x 10 | A cold pilger mill: **4 passes** down to the tube.<br>• **75 kW at 480 V**.<br>• Sneak-click it to change its dies between **cladding tube** (9.50 x 0.57 mm, 0.404 kg) and **guide thimble** (12.24 x 0.41 mm, 0.40 kg). A die change scraps the tube on the mill, as re-tooling does. |
| `vacuum_anneal_furnace` | 3 x 3 x 8 | Anneals at 675-700 °C between the passes and a final stress relief, against the pilger mill.<br>• **100 kW at 480 V**.<br>• Finished tubes go out to a hopper under it, or to an empty hand. |
| `magnesium_cell` | 3 x 3 x 6 | A monopolar MgCl2 cell at about 700 °C: **14 kA at 5 V (70 kW DC)** from the rectifier it touches.<br>• 90 % current efficiency (0.453 kg of Mg a kAh): about 12 kWh/kg of Mg *(unverified)*, 5.7 kg of Mg an hour.<br>• Its chlorine goes out on the pipe at the east face of its front cells. |
| `magnesia_chlorinator` | 3 x 6 x 3 | An IG Farben shaft chlorinator, MgO + C + Cl2 -> MgCl2 + CO, at 800-1,000 °C.<br>• Magnesia and coal by a hopper on top; chlorine off the pipe at the north face of its front cells.<br>• **30 kg of MgCl2 an hour** *(proposed)*.<br>• It tops up the magnesium cell it touches while the cell holds less than a quarter of its MgCl2. |
| `chlor_alkali_cell` | 3 x 3 x 6 | A small bipolar membrane electrolyser: **6 cells at 2.8 kA and 3.1 V (52 kW DC)**, 96 % current efficiency.<br>• **21 kg of Cl2 an hour**, 2.4 kWh/kg.<br>• Inputs: rock salt by a hopper on top (1.65 kg a kg of Cl2); water off a pump (**3 L a kg of Cl2**, making 32 % caustic *(unverified)*).<br>• Outputs: chlorine on the pipe at the south face of its back cells; **hydrogen** (0.32 m³ a kg of Cl2) at the north face of its front cells; NaOH counted (1.128 kg a kg of Cl2).<br>• Against a rectifier. |

**Reused:** the conversion plant's `rectifier` (4.16 kV, 12-pulse; conversion.md). Each cell asks the rectifier it touches for its DC. A cell needs its own rectifier, as each real potline has.

## Products and items
- **Chlorine:** a new pipe product (kg, a gas). Hydrogen is the conversion plant's.
- **Zircaloy-4 cladding tube:** 9.50 x 0.57 mm, 3.85 m, 0.404 kg.
- **Guide thimble:** 12.24 x 0.41 mm, 0.40 kg.
- **Carried in kg, not items:** zircon, ZrCl4, sponge, ingot and hollows pass between the parts by touching; Mg and MgCl2 the same way.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **480 V boxes:** the west face of the main cell of every powered part. The extrusion press's box there is **4.16 kV**.
- **Chlorinator:** chlorine in on the east face of its bottom cells.
- **Magnesium cell:** chlorine out of the east face of its front cells (z 0, bottom).
- **Magnesia chlorinator:** chlorine in on the north face of its front cells (z 0, bottom).
- **Chlor-alkali cell:** chlorine out of the south face of its back cells (z 5, bottom); hydrogen out of the north face of its front cells (z 0, bottom).
- **Hoppers:**
  - in from on top: heavy minerals on the separator; coal on the chlorinator; magnesia and coal on the magnesia chlorinator; tin, iron and chromite on the VAR furnace; rock salt on the chlor-alkali cell;
  - out to a hopper under the anneal furnace: finished tubes.
- **Water:** the chlor-alkali cell takes water off a pump.

## How it runs
- **The line:** each stage takes what the one before it holds, at the line rate with a fifth more to catch up, while its motor runs.
  - Each one reads what stops it: NO CHLORINE, NO CARBON, NO MAGNESIUM, NO TIN, NO IRON, NO CHROMIUM, NO ANNEAL FURNACE.
  - A part with nothing set against it reads NOT BUILT and names the part it needs.
- **The cells:** each asks its rectifier for its DC, and its output follows the share it gets.
  - With no rectifier against it: NO DC.
  - The chlor-alkali cell stops without salt or water.
  - The magnesium cell stops without MgCl2.
- **Removing a part** stops what needs it.

## Left out (ROADMAP Ideas)
- SiCl4 to fumed silica.
- Uses for the caustic soda.
- Ilmenite and rutile for a titanium pigment plant.
- Hafnium metal for control rods.
- Pidgeon magnesium from dolomite.
- The aluminothermic chromium step.
- Scrap recycle and yield losses.
- Batch timing of the Kroll retort and the VAR heats (the line runs as a steady flow).

## Game test
`zirconium`:
- **Power:**
  - two diesel sets paralleled on a busway for 480 V;
  - two more paralleled through a backward unit substation to 4.16 kV for the two rectifiers and the extrusion press.
- **Feeds:** heavy minerals, coal, magnesia, rock salt, tin, iron and chromite by hopper; water by hook.
- **Checks:**
  - the chlor-alkali cell makes chlorine and hydrogen, and the magnesium cell makes magnesium and chlorine, on rectifiers that report their kA;
  - the magnesia chlorinator fills the magnesium cell;
  - each stage of the line runs once a hook has given the one before it some metal;
  - a tube that a hook has brought to just short of finished drops into the hopper under the anneal furnace;
  - after a die change, a guide thimble does the same;
  - breaking the hafnium column stops the Kroll reactor.
- **Screenshot:** the plant.

## Added while building (7f)
- **480 V supply:** the line's about 0.5 MW at 480 V is about 600 A. A single cable down the line burns, so the game test runs
  an 800 A busway down it, with a 500 kcmil cable stub to each box. The kit gives busway.
- **Extrusion press:** in the game test it faces south, so its 4.16 kV box is on its east side, away from the 480 V busway.
- **Rectifiers:** both run below their full current on the paralleled diesel sets: the chlor-alkali cell at 2.5 kA, the
  magnesium cell at 12.6 kA.
