# The fuel plant (APPROVED 2026-11-01 with the Phase 7g plan)

The 30B cylinders of 4.95 % UF6 from the cascade (enrichment.md) become 17×17 fuel assemblies. The steps are:
- UO2 powder by the dry route;
- granulation and pressing, then sintering in hydrogen and grinding to size;
- rods: Zircaloy-4 tubes from the zirconium plant (zirconium.md), loaded, filled with helium and sealed;
- the assembly: rods in a skeleton of guide thimbles, grids and nozzles.

Decided with Tim on 2026-11-01:
- **Helium** comes from a **helium recovery unit at the gas plant** (gas-plant.md). It fills helium cylinders for the rod welder.
- **Hardware:** steel ingots stand in for the 304 stainless nozzles and the Inconel end grids and springs, until a nickel smelter exists. The 6 mid grids are Zircaloy, made on the pilger mill's third die, "grid strip".
- **The assembly** is a placeable, true-size **1 x 5 x 1 block** (214 mm square, 4.06 m tall), carried as one item, for Phase 8.

Sources:
- **Integrated Dry Route** (Springfields, since 1969): UF6 vapour, steam and hydrogen through a 150-250 °C reaction vessel and a rotary Inconel kiln at 500-800 °C. UF6 + H2 + 2H2O -> UO2 + 6HF (US 5,757,087; Springfields patents). UF6 is vaporized from its cylinder at about 60 °C and superheated to about 290 °C (BATAN design paper).
- **Sintering:** in hydrogen at 1,600-1,800 °C for 2-6 hours, to about 95 % of theoretical density; green pellets about 5.5 g/cm³ (US 4,389,355; CNS proceedings; KAERI pusher furnace papers).
- **The 17×17 rod:** pellet 8.191 mm (EPJ-N table); cladding 9.50 x 0.57 mm, 3.85 m; helium at about 2.0 MPa (arXiv 1709.06937, a Westinghouse 17×17 model).
- **The 17×17 assembly:** 264 rods, 24 guide thimbles and 1 instrument tube, 8 grids, top and bottom nozzles (the Westinghouse standard design). About 665 kg and 461 kg of U *(textbook, unverified)*.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

```
30B of LEU (hopper) -> UF6 AUTOCLAVE -> IDR KILN -> GRANULATOR -> PELLET PRESS -> SINTERING FURNACE -> PELLET GRINDER
                                          ^ H2 pipe, water             ^ H2 pipe
    -> ROD LOADER (cladding tubes, steel by hopper) -> ROD WELDER (helium cylinders by hopper) -> ASSEMBLY FIXTURE
                                                                       (thimbles, spacer grids, steel by hopper) -> FUEL ASSEMBLY
```
Uranium passes from part to part by touching, as UF6, powder, pellets and then rods. It is counted in kg of U.

## Size: a small line, 50 kg of U an hour *(proposed)*
- **The pellet:** 8.19 mm x 9.83 mm at 95 % of 10.96 g/cm³, less 1.5 % for its dish and chamfers: **5.31 g of UO2**.
- **The rod:** **372 pellets** fill the 3.66 m stack, **1.741 kg of U**.
- **The assembly:** **459.6 kg of U**, about 667 kg in all.
- **The line:** 3 pellets a second, 29 rods an hour, an assembly in about **9 hours**.
  - A 30B (1,539 kg of U) is about 31 hours of the line and 3.3 assemblies.
  - The smallest cascade makes 16 kg of U an hour, so the line waits on it.
- **Per kg of U:**
  - 1.479 kg of UF6;
  - **0.141 m³ of hydrogen** (0.094, plus 50 % *(proposed)*);
  - **0.45 L of water** as steam (0.151, plus 200 % *(proposed)*);
  - **0.504 kg of HF** to the kiln's scrubber, counted.
- **Helium:** a rod's free volume of about 20 cm³ at 2 MPa is **0.4 L at STP** *(estimate)*. A cylinder of **8.5 m³** (300 cf at 2,400 psi) fills about 21,000 rods.
- **Steel:** an ingot counts as **100 kg** *(proposed)*. An assembly takes **20 kg** *(unverified)*: 16 kg of nozzles, 2 kg of end grids, 2 kg of springs and plugs (the plugs go on at the rod loader).

## Parts (cells W x H x L, all sizes and watts *unverified*; each against the next)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `uf6_autoclave` | 3 x 3 x 4 | A 30B cylinder of LEU goes in by a hopper on top. It is heated to vaporize the UF6 into the kiln it touches; the empty 30B goes out to a hopper under it. **30 kW at 480 V**. |
| `idr_kiln` | 3 x 4 x 10 | The IDR reaction vessel and rotary Inconel kiln, 500-800 °C.<br>• Hydrogen off the pipe at the east face of its bottom cells; steam from water off a pump.<br>• Its HF is counted at its scrubber.<br>• **150 kW at 480 V**. |
| `granulator` | 3 x 4 x 4 | Pre-compaction, granulation and blending of the powder. **30 kW at 480 V**. |
| `pellet_press` | 3 x 3 x 4 | A rotary press: green pellets at about 5.5 g/cm³, 3 a second. **30 kW at 480 V**. |
| `sintering_furnace` | 3 x 3 x 12 | A continuous pusher furnace, **1,750 °C, about 4 hours** in hydrogen.<br>• **15 m³/h** of hydrogen *(proposed)* off the pipe at the east face of its bottom cells.<br>• **250 kW at 480 V**. |
| `pellet_grinder` | 3 x 3 x 4 | A centreless grinder to 8.19 mm. **0.5 %** is swarf, counted *(proposed)*. **20 kW at 480 V**. |
| `rod_loader` | 3 x 3 x 10 | Cladding tubes and steel by a hopper on top.<br>• It welds on the bottom plug and loads 372 pellets and the plenum spring.<br>• **15 kW at 480 V**. |
| `rod_welder` | 3 x 3 x 6 | A pressurized weld chamber: each rod is filled with helium at 2 MPa, then its top plug and fill hole are TIG-welded.<br>• Helium cylinders by a hopper on top; the empties go out to a hopper under it.<br>• It racks the finished rods, up to 264, for the fixture it touches.<br>• **30 kW at 480 V**. |
| `assembly_fixture` | 3 x 6 x 10 | Builds the skeleton from 25 guide thimbles, 6 spacer grids and steel (end grids and nozzles), all by a hopper on top. It pulls the 264 rods in from the welder's rack.<br>• The finished assembly goes out to a hopper under it, or to an empty hand.<br>• **10 kW at 480 V**. |
| `fuel_assembly` | 1 x 5 x 1 | The finished 17×17 assembly, standing: 214 mm square, 4.06 m tall, drawn with its rods, grids and nozzles.<br>• Its item carries it: 264 rods, 459.6 kg of U at 4.95 %.<br>• It does nothing standing but wait for Phase 8. |

## The helium recovery unit (at the gas plant)
| Part | Cells | Real basis |
|---|---|---|
| `helium_recovery_unit` | 3 x 6 x 4 | A nitrogen-rejection cold box with helium PSA and cylinder filling (Linde; Chart).<br>• It takes pipeline gas off the pipe at the north face of its front cells and puts it back there with its helium taken out: **0.4 %** of the gas *(proposed; Hugoton-Panhandle gas holds 0.3-0.8 %)*, up to 1.0 m³/s. The pipeline gas's other users draw through it.<br>• It fills **8.5 m³** helium cylinders at 99.995 %. Empties go in by a hopper on top; full ones come out to a hopper under it.<br>• **75 kW at 480 V**. |

## The pilger mill's third die (zirconium.md)
A sneak-click on the pilger mill cycles its dies: **cladding tube**, then **guide thimble**, then **grid strip**. Grid strip makes a **spacer grid**: a Zircaloy mid grid, 0.9 kg *(proposed)*.

## Items
- **Spacer grid:** a Zircaloy-4 mid grid, 0.9 kg.
- **Empty helium cylinder**, and **helium cylinder** (8.5 m³, 99.995 %).
- **Fuel assembly:** the block's item.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **480 V boxes:** the west face of each part's main cell.
- **Hydrogen in:** the east face of the bottom cells of the kiln and of the sintering furnace.
- **Water:** the kiln takes water off a pump.
- **Hoppers:**
  - in from on top: a 30B on the autoclave; tubes and steel on the rod loader; helium cylinders on the welder; thimbles, grids and steel on the fixture;
  - out to a hopper under each: the autoclave's empty 30B, the welder's empty cylinders, the fixture's assembly.
- **Helium recovery unit:** pipeline gas in and out on the north face of its front cells; its 480 V box on the west face of its main cell.

## How it runs
- **The line:** each stage takes what the one before it holds, at the line rate with a fifth more, while its motor runs.
- **What stops a stage** (its readout names it):
  - NO HYDROGEN, NO WATER;
  - NO TUBES, NO STEEL, NO HELIUM;
  - NO THIMBLES, NO GRIDS.
- **NOT BUILT:** a stage with nothing against it says so and names what it needs.
- **The welder:** its rack holds 264 rods; FULL when the fixture isn't taking them.
- **The fixture:** it builds an assembly once it has the 264 rods, 25 thimbles, 6 grids and 20 kg of steel.
- **Removing a part** stops what needs it.

## Left out (ROADMAP Ideas)
- HF from the kiln back to the conversion plant.
- Zinc stearate lubricant and pore formers.
- A nickel smelter for real Inconel and stainless parts.
- Pellet inspection, rod X-ray and helium leak testing.
- IFBA and gadolinia burnable absorbers.
- Rejects and scrap recycle.

## Game tests
`fuel`:
- **Power:** two diesel sets paralleled on a busway down the line for 480 V.
- **Hydrogen:** the conversion plant's steam reformer on LPG from a bullet, water by hook.
- **Feeds by hopper:** a 30B of LEU, tubes, steel, thimbles, grids, helium.
- **Checks:**
  - each stage runs once a hook has given the one before it a kilogram of uranium;
  - with the welder's rack topped up to 263 rods, it finishes the 264th and the fixture drops a fuel assembly into the hopper under it;
  - the emptied 30B drops under the autoclave.
- **Screenshot:** the plant, with an assembly standing.

## Added while building (7g)
- In the game test the two diesel sets come on line first and each part's stub is connected after. Closed onto the whole plant at
  once, the first set tripped on frequency (its stator phase B at 519 %).
- The assembly fixture builds its skeleton from its hopper as soon as it has power and a welder against it, then pulls rods as
  they come.

`gasPlant` (extended): a helium recovery unit on the plant's pipeline gas recovers helium and fills a cylinder that a hook has nearly filled.
