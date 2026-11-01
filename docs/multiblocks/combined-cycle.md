# Combined cycle: the HRSG and the 5 MW steam turbine (APPROVED 2026-10-31 with the Phase 6a plan)

The gas turbine's exhaust (gas-turbine.md) raises steam in a heat recovery steam generator, and the steam runs a 5 MW condensing
steam turbine generator. Distinct true-size parts that form by touching; the steam turbine is the steam side's controller.
Values are approximate, for an unfired single-pressure plant of this size.

## Parts (each drawn facing north; x east, z south; main cell at its north-west bottom corner)
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `hrsg` | 5 x 12 x 12 | Single-pressure, unfired, drum-type HRSG: inlet duct, superheater, evaporator and economizer sections, the steam drum on top, its stack. It touches the gas turbine in place of the exhaust stack. At full turbine load it takes about 19 MW from the exhaust and makes 6 kg/s of steam at 40 bar and 400 C |
| `steam_turbine_5` (controller of the steam side) | 3 x 3 x 6 | 5 MW condensing steam turbine and its gearbox; it touches the HRSG (its steam line), its generator and the air-cooled condenser |
| `st_generator` | 3 x 3 x 4 | 6.25 MVA, 13.8 kV, four-pole; its terminals the south face of its back bottom cells, against a generator breaker |
| `air_cooled_condenser` | 6 x 8 x 6 | A-frame air-cooled condenser, four 50 hp fans (149 kW, 480 V; terminal box on the west face of its main cell). The turbine exhausts into it |
| `hrsg_feed_pump` | 2 x 2 x 4 | 60 hp (45 kW) 480 V feedwater pump, 6 L/s at 5 MPa, condensate from the condenser's hotwell back to the drum; it touches the HRSG; terminal box on the west face of its main cell |

## How it runs
- The HRSG's steam follows the turbine's exhaust heat: about 65 % of the exhaust's heat above stack temperature, warming up and
  cooling down over two minutes.
- The steam turbine runs while the HRSG makes steam, the fans run and the feed pump runs (it asks for both; without them the
  turbine stops: NO CONDENSER or NO FEEDWATER). Its power is what the steam gives, about 27 % of the HRSG's heat: 5.2 MW at full
  gas turbine load. It synchronizes onto the 13.8 kV bus like any generator and takes a share of the load by its steam.
- At full load: 16.5 + 5.2 = 21.7 MW gross from 46.9 MW of fuel, 46 %. The gas turbine's readout gives the plant's efficiency.

## Game test
In `gasTurbine`, with the HRSG and steam turbine built: the steam turbine synchronizes and carries part of the load bank's 10 MW,
the condenser fans and feed pump run, and the plant reads above 40 % efficient.
