# The 45 MW aeroderivative peaker, its GSU and a 60 MVA substation transformer (APPROVED 2026-11-01 with the Phase 6b plan)

A fast-start aeroderivative gas turbine generator of the GE LM6000 class, in simple cycle, built like the 16 MW unit
(gas-turbine.md) from true-size parts that form by touching, with the package as controller. Values are approximate.

## Parts (each drawn facing north; x east, z south; main cell at its north-west bottom corner)
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `aero_turbine` (controller) | 5 x 5 x 14 | The package: enclosure over the gas generator and power turbine, its auxiliary skid, and the 300 hp (224 kW) 4.16 kV motor of its hydraulic start system: the starter's terminal box on the west face of the cell at x 0, z 2, the fuel nozzle on the west face of the cell at x 0, z 6, as on the 16 MW unit |
| `aero_inlet_house` | 6 x 8 x 5 | Filter house with evaporative coolers and silencers, touching the turbine |
| `aero_generator` | 4 x 4 x 7 | 56.25 MVA, 13.8 kV, two-pole (3,600 rpm; 3,000 IEC), driven directly by the power turbine; its terminals the south face of its back bottom cells |
| `aero_exhaust_stack` | 4 x 14 x 4 | Exhaust stack and silencer, touching the turbine |
| `gen_breaker_3000` | 2 x 3 x 2 | 15 kV vacuum generator breaker, 3,000 A, 50 kA (the generator makes 2,350 A), with its bus voltage transformers |
| `gsu_60` | 6 x 6 x 5 | 60 MVA generator step-up, 13.8 kV delta / 138 kV grounded wye, 10 %Z, ONAF |
| `substation_transformer_60` | 6 x 6 x 5 | 60 MVA substation transformer, 138 kV delta / 12.47 kV grounded wye (IEC 132/11 kV), 10 %Z: a standalone yard part. 138 kV comes to the bushings on its cover; 12.47 kV leaves low on its back |

The 13.8 kV bus duct gets a 3,000 A size, crafted like the other conductor sizes.

## Values
- 45 MW at a heat rate of 8,800 kJ/kWh (41 %): 110 MW of fuel at full load, 2.9 m3/s of pipeline gas or 3.1 L/s of diesel or
  kerosene. At no load 25 % of full-load fuel.
- Fast start: purge 30 s, light-off, run-up 60 s to synchronizing; after it closes its breaker it may take load at 25 % of its
  rating a minute, so it is at full load about five minutes after the button.
- The 16 MW unit (6a) gets its own ramp limit now, 10 % a minute, as its spec said.
- Governor 4 % droop, H = 3 s, like the 16 MW unit.

## Added while building (6b)
- The exhaust stack is drawn as an octagon of two crossed boxes: a round stack this wide would sit on a cell corner, where the
  model's rotated faces do not fit. The SCR housing is blank.
- The 3,000 A duct is crafted from three 1,200 A ducts and a copper block.
- In the test the four banks take 37.3 MW, not 40: the generator's voltage sags 3.4 % under load (as the gensets' does) and a
  resistive bank's power follows the voltage squared. Alone on its bus, on 4 % droop, it runs at 59.0 Hz; the test checks over
  57.5 Hz. The 12.47 kV side reads 6.95 kV a phase under that load (7.2 kV nominal).

## Game test
`peaker`: a diesel set gives the starter 4.16 kV and the purge begins (a hook then skips to synchronizing); the turbine closes onto
a dead 13.8 kV bus over 3,000 A bus duct; it holds at its ramp (a hook then skips it); four load banks take 40 MW at 60 Hz; the GSU
puts 138 kV on a line to the 60 MVA substation transformer, whose 12.47 kV side reads 7.2 kV a phase.
