# The steam cracker and polyethylene (APPROVED 2026-10-31 with the Phase 5c/5d plan)

LPG or gasoline cracked to ethylene, and ethylene made into plastic sheets: the only source of plastic once the old refinery is
gone. A chain of distinct true-size parts on steel pipe, like the gas plant (gas-plant.md); the polyethylene reactor and its
pelletizer form a unit by touching. Sized small (about 1 L/s of feed), far below a world-scale cracker, so it fits a game base.
Values are approximate.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `cracking_furnace` | 4 x 14 x 6 | Cracking furnace: the radiant box and its coils, the convection section that makes dilution steam, the stack. Takes LPG or gasoline (1 L/s at full rate), fresh water off a water pipe (0.15 L/s on LPG, 0.36 on gasoline) and fuel gas (7.5 MW on LPG, 14 MW on gasoline: 0.2 or 0.38 m3/s) |
| `quench_tower` | 2 x 12 x 2 | Water quench tower: cools the hot cracked gas; pyrolysis gasoline out of its bottom |
| `cracked_gas_compressor` | 4 x 4 x 8 | Cracked gas compressor, 300 hp (224 kW), 4.16 kV |
| `cold_box` | 3 x 10 x 3 | Chilling train, demethanizer and C2 splitter. It needs a refrigeration compressor (gas-plant.md) touching it |
| `polyethylene_reactor` | 3 x 16 x 3 | Small gas-phase fluidized-bed reactor with its cycle gas compressor (75 kW, 480 V). 98 % of the ethylene becomes polyethylene |
| `pelletizer` | 2 x 3 x 6 | Extruder and pelletizer, 250 hp (187 kW), 4.16 kV. It touches the reactor and makes plastic sheets (1 sheet = 1 kg of polyethylene) into its bin of 256: an empty hand or a hopper under its main cell takes them |

## Yields (weight)
| Feed | Ethylene | Fuel gas | LPG (C3/C4) | Pyrolysis gasoline | Residue |
|---|---|---|---|---|---|
| LPG (0.51 kg/L) | 42 % | 28 % | 22 % | 8 % | - |
| Gasoline (0.72 kg/L) | 30 % | 17 % | 25 % | 25 % | 3 % |

Cracked gas is counted in kg on the pipe (1 unit = 1 kg); ethylene in m3 (1.18 kg/m3); fuel gas as pipeline gas in m3 (0.75
kg/m3, methane and hydrogen); LPG, gasoline and residue in litres. On LPG, 0.21 kg/s of ethylene makes about 12 sheets a minute.
The fuel gas roughly covers the furnace's own fuel, as in a real cracker: pipe it back.

## Nozzles (each part drawn facing north, main cell at its north-west bottom corner)
- Furnace: feed on the north face of its front cells (z 0, bottom layer); fuel gas on the east face of its bottom cells; water
  pipe on the west face of its bottom cells; hot cracked gas out of the south face of its back cells (z 5, bottom layer).
- Quench tower: hot cracked gas in on the north face of its bottom cells; cracked gas out of their south face; pyrolysis gasoline
  out of their east face.
- Cracked gas compressor: suction on the north face of its front cells, discharge on the south face of its back cells (z 7),
  both in the bottom layer; its 4.16 kV box on the west face of the cell at x 0, z 4.
- Cold box: cracked gas in on the north face of its front cells (z 0, bottom layer); out of the south face of its back cells
  (z 2): LPG in the bottom layer, ethylene in layer 1, fuel gas in layer 2.
- Polyethylene reactor: ethylene in on the side faces of its bottom cells; its 480 V box on the west face of its main cell.
- Pelletizer: its 4.16 kV box on the west face of its main cell; its bin in its main cell.

## Added while building (5d)
- The furnace also fires on LPG (25 MJ/L) until its own fuel gas comes round: a cracker needs fuel before it makes any. In the test
  the bullet's LPG feeds it and fires it at first.
- The quench tower's fuel oil (residue) leaves its west face.
- The test tops the furnace's water up with a hook instead of a pump: 200 L is over twenty minutes on LPG.

## Game test
`cracker`: LPG from a bullet, water from a pump and fuel gas to the furnace; quench, compressor, cold box with its refrigeration
compressor; ethylene to the reactor; the pelletizer's bin fills with plastic sheets and a hopper under it takes them.
