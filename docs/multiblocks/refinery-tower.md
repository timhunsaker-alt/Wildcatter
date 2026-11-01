# Refinery tower (APPROVED 2026-10-30; REPLACED 2026-10-31 by crude-units.md: the base and columns are removed)

## Real-world counterpart
A **crude distillation unit**: fired heater, atmospheric distillation column with trays, overhead condenser and
product tanks. Products in Phase 5: LPG, gasoline, kerosene, diesel, residue.

## Values (DECISION)
Throughput, yields per cut and power draw are to be proposed with the real crude assay when Phase 5 starts. For
now the as-built values stand: 2 kW at 110 V, 70 percent diesel in a full tower.

## Parts
| Char | Block id | Real part | Notes |
|---|---|---|---|
| `H` | `wildcatter:refinery_heater` | Fired heater | Takes crude; **controller** |
| `C` | `wildcatter:distillation_column` | Column section with trays | Real parts: the column is tall but made of different sections (bottom, trays, top) |
| `O` | `wildcatter:overhead_condenser` | Overhead condenser | Makes the light products |
| `T` | `wildcatter:product_tank` | Product tank | One per product |

## Layout (bottom layer first, NORTH facing)
Layer 1: `H T`
Layer 2: `C .`
Layer 3: `C .`
Layer 4: `O .`

## Controller / rotation / formation / breaking
`refinery_heater` (`H`). Four facings. Forms when the column is complete and touches the heater and a tank.
Removing the condenser stops the light products. Removing the column dissolves it back to a pot still.

## As built
Base plus `REFINERY_COLUMNS` (2) identical `refinery_column` blocks. A pot still without them.

## Game test
Build the full unit, check it forms, check products by cut, remove the condenser and check the light products stop.
