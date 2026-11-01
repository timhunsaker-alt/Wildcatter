# Crude distillation units (APPROVED 2026-10-31 with the Phase 5c/5d plan)

Crude (or a gas well's condensate) split into its cuts. An early skid topping unit and a late full crude unit, each built from
distinct true-size parts that form a unit by touching: the column is the controller and names what is missing. Feed, fuel and
products go on steel pipe, one product to a run (lease.md). They replace the Phase 1 one-block refinery and its columns
(refinery-tower.md), which are removed. Values are approximate, drawn from typical equipment of each class.

## Assay (volume)
| Feed | LPG | Gasoline (naphtha) | Kerosene | Diesel | Atmospheric residue |
|---|---|---|---|---|---|
| Crude (light sweet, WTI-like, about 40 API) | 2 % | 30 % | 14 % | 26 % | 28 % |
| Condensate | 5 % | 60 % | 15 % | 15 % | 5 % |

A separator fed only by gas wells gives condensate out of its oil nozzle; with any oil well on it, the condensate goes into the
crude, as real leases blend it.

## Early: 1,000 bbl/day skid topping unit (1.84 L/s at full voltage)
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `topping_column` (controller) | 2 x 12 x 2 | 0.9 m x 12 m column with its kerosene and diesel side strippers mounted on it |
| `topping_heater` | 2 x 6 x 2 | Vertical cylindrical fired heater, 3 MMBtu/h (0.88 MW fired). It burns the unit's own LPG first (2 % of the crude is about 0.9 MW), then gas or residue piped to it |
| `topping_skid` | 3 x 3 x 4 | Crude charge pump (10 hp) and overhead air cooler (5 hp fan) with the reflux drum, one 480 V three-phase feed (11.2 kW) |

Forming: the heater and the skid each touch the column. Nozzles (each part drawn facing north, main cell at its north-west bottom
corner): crude or condensate in on the north face of the skid's front cells (z 0, bottom layer); gasoline out of the south face of
its back cells (z 3, bottom layer); the skid's 480 V terminal box on the west face of its main cell. Kerosene out of the east face,
diesel out of the south face and residue out of the west face of the column's bottom cells. Fuel (any gas, or residue) into the
side faces of the heater's bottom cells.

## Late: 20,000 bbl/day crude unit (36.8 L/s at full voltage)
| Part | Cells | Real-world basis |
|---|---|---|
| `atmospheric_column` (controller) | 3 x 30 x 3 | 3 m x 30 m trayed atmospheric column |
| `crude_heater` | 6 x 14 x 8 | Box (cabin) fired heater, 60 MMBtu/h (17.6 MW fired): 0.46 m3/s of gas or 0.46 L/s of residue at full rate |
| `desalter` | 3 x 4 x 10 | Two-stage electrostatic desalter, 3.6 m x 12 m; its grid transformer draws 40 kW at 480 V |
| `crude_charge_pump` | 2 x 2 x 5 | 250 hp (187 kW) 4.16 kV centrifugal pump, 37 L/s at about 400 psi |
| `side_strippers` | 2 x 10 x 1 | The kerosene and diesel strippers, 1 m x 10 m each |
| `overhead_condenser` | 4 x 4 x 8 | Fin-fan bank, four 25 hp fans (75 kW at 480 V), and the reflux drum |

Forming: the heater, the strippers and the condenser each touch the column; the charge pump touches the heater; the desalter
touches the charge pump. Nozzles: crude in on the north face of the desalter's front cells (z 0, bottom layer); fuel into the north
face of the heater's front cells (z 0, bottom layer); residue out of the south face of the column's bottom cells (the other sides stay free for the parts against it); kerosene out of the
west face of the strippers' west cell, diesel out of the east face of their east cell (bottom layer); gasoline out of the south
face of the condenser's back cells (z 7) in its bottom layer, LPG in the layer above. Terminal boxes on the west face of the main
cell: desalter and condenser 480 V, charge pump 4.16 kV.

## How they run
- The column draws feed at the feed nozzle, as fast as its rate times its motors' output (the skid's motor, or the charge pump),
  while its heater has fuel and every product has room. A part missing or a product backed up stops it and the readout says why.
- Fuel: gasses at 38 MJ/m3, residue at 38 MJ/L. The heater's duty follows the feed rate.
- Products are held a few seconds at their nozzles for whatever draws on them: stock tanks (gasoline, kerosene, diesel, residue,
  condensate, crude), LPG bullets, fireboxes, the steam plant burner (residue), diesel gensets (diesel).

## Game tests
`refining`: the early unit fed crude from a stock tank, its heater on gas, each product filling its tank, a diesel set run on the
diesel. `crudeUnit`: the late unit built part by part, its readout naming each missing part, 4.16 kV to the charge pump and 480 V
to the desalter and the fans, the heater on residue, the products at rate; a part taken away stops it.
