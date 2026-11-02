# The turbine island (APPROVED 2026-11-01 with the Phase 8b plan)

The two steam generators' saturated steam (reactor.md) drives a 51 MW condensing turbine generator onto the 13.8 kV bus. Then the
60 MVA GSU (aeroderivative.md) puts it on 138 kV.

**The steam path:**
1. The main stop and governor valves hold the steam header.
2. The steam runs through a single-flow HP turbine, a moisture separator reheater and a double-flow LP turbine at 3,600 rpm.
3. The LP turbine exhausts into a surface condenser. The condenser is cooled once-through by circulating water pumps at a lake or
   river intake.

**The water path:** condensate pumps, a deaerator with the LP heaters, two motor-driven feed pumps and the HP heaters return the
water to the steam generators at 227 °C.

**Station service:** a unit auxiliary transformer takes 13.8 kV down to the plant's own 4.16 kV.

Decided with Tim on 2026-11-01:
- **Cooling:** once-through cooling in 8b. The hyperbolic natural-draft tower comes in its own wave, 8c, and closes the loop.
- **Generator:** a new 60 MVA, two-pole, hydrogen-cooled generator part.

Sources:
- **The turbine:** a small saturated-steam PWR turbine, about 33 % gross on about 6.1 kg of steam a kWh *(textbook, unverified)*.
- **The condenser:** about 5 kPa absolute with 20 °C water warmed 10 °C *(textbook, unverified)*.
- **Precedent:** Calder Hall's 4 × 50 MWe units.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

## Numbers *(proposed unless marked)*
**Output:**
- **Steam rate:** 6.1 kg/kWh, so 86 kg/s (both steam generators at full power) gives **51 MW** gross.
- **Heat rejected:** 155 − 51 = **104 MW**.

**Vacuum:**
- **Condensing temperature:** the circulating water's inlet (20 °C) plus its rise (10 °C at full flow, more with less), plus 5 °C
  across the tubes. At full flow that is 35 °C, **5.6 kPa**.
- **Power:** the output falls 1 % a kPa over 5.6 kPa.
- **Low-vacuum trip:** at **25 kPa**.

**Circulating water:**
- **Flow:** **2,500 kg/s** for a 10 °C rise.
- **Pumps:** **2 × 1,250 kg/s**, about 15 m of head, **300 kW at 4.16 kV** each.

**Condensate and feed:**
- **Condensate pumps:** **2 × 50 %**, 43 kg/s at 2.5 MPa, **150 kW at 480 V** each.
- **Main feed pumps:** **2 × 50 %**, 43 kg/s from the deaerator at 0.9 MPa to about 8.5 MPa, **500 kW at 4.16 kV** each.
- **Inventory:** the deaerator holds **30 t**. The hotwell holds 10 t. Make-up water comes off a pump into the deaerator.

**Turbine:**
- **Roll-up:** **2 minutes** to 3,600 rpm (3,000 IEC), then the generator synchronizes.
- **Header pressure:** its governor valves hold the steam header at **7.0 MPa**. The steam generators give the turbine steam only
  while they stand above it.
- **Steam dump:** steam the generator cannot use (the bus does not want it) goes to the condenser through the **steam dump**,
  counted.
- **Moisture separator reheater:** takes about 5 % of the main steam, counted in the steam rate.

**Generator:** **60 MVA, 0.85 PF, 51 MW, 13.8 kV**, two-pole, hydrogen-cooled.

**Unit auxiliary transformer:** **13.8/4.16 kV (11/3.3 kV IEC), 7.5 MVA, 7 %Z**, delta/wye.
- **4.16 kV loads:** the plant's own pumps, about 3.1 MW: 2 coolant pumps, 2 feed pumps and 2 CW pumps.
- **480 V loads:** through a unit substation.

**Turbine trip:**
- **Causes:** low vacuum, no feedwater (the deaerator under 10 %), a generator trip, or a turbine stopped on its own.
- **Above P-9 (50 %):** the reactor trips.
- **Below P-9:** the steam generators' dump valves take the steam and the reactor stays up.

## Parts (cells W x H x L, sizes and watts *proposed*; each drawn facing north, its main cell at its north-west bottom corner)
| Part | Cells | Real basis |
|---|---|---|
| `turbine_valve_chest` | 3 x 4 x 3 | Main stop valves and governor valves in one chest. `STEAM` in on its top face. |
| `hp_turbine` | 4 x 4 x 6 | Single-flow HP turbine. **The controller**, against the valve chest, the MSR and the LP turbine. |
| `moisture_separator_reheater` | 3 x 4 x 10 | A horizontal vessel with chevron separators and a live-steam reheat bundle, against the HP turbine. |
| `lp_turbine` | 6 x 5 x 8 | Double-flow LP turbine and its exhaust hood, against the HP turbine, the generator and the condenser. |
| `tg_generator_60` | 4 x 4 x 8 | 60 MVA, 13.8 kV, hydrogen-cooled. Its terminals are on the south face of its back bottom cells. |
| `main_condenser` | 6 x 5 x 8 | Surface condenser with its hotwell and steam dump valves, against the LP turbine.<br>• `COOLING_WATER` in on the north face of its bottom cells.<br>• Out on the south face, discharged to the lake and counted. |
| `condensate_pump` (2) | 2 x 3 x 2 | A vertical can pump, against the condenser. **150 kW at 480 V**. |
| `deaerator` | 3 x 5 x 8 | The deaerator on its storage tank, with the LP heaters, against a condensate pump. Make-up water off a pump. |
| `main_feed_pump` (2) | 2 x 2 x 5 | A motor-driven boiler feed pump, against the deaerator. **500 kW at 4.16 kV**. |
| `hp_feedwater_heater` | 2 x 3 x 8 | HP heaters on extraction steam, against a feed pump. `FEEDWATER` at 227 °C out on the south face of its back bottom cells. |
| `circulating_water_pump` (2) | 3 x 8 x 3 | A vertical intake pump: its bottom 3 cells stand in water, at least 12 water blocks around them.<br>• `COOLING_WATER` out on the east face of its cells at height 3, ground level.<br>• **300 kW at 4.16 kV**. |
| `unit_auxiliary_transformer` | 4 x 4 x 4 | 13.8/4.16 kV, 7.5 MVA, oil-filled. 13.8 kV bushings on top of the front row; 4.16 kV out low on the back of the main cell. |

**Reused:** `gen_breaker_3000`, `bus_duct_gen` (3,000 A), `gsu_60`, `unit_substation`.

## Layout (all facing north; x east, z south; the valve chest's main cell at 0,0,0)
Ground (y 0):
```
x:    0 1 2 3 4 5 6 7 8 9 0 1
z-1   . . . . M M M . . . . .     V  valve chest (y 0-3)          M  moisture separator reheater (y 0-3)
z0    V V V . M M M . . . . .     H  HP turbine (y 0-3)           L  LP turbine (y 0-4)
z3    H H H H M M M . . . . .     C  condenser (y 0-4)            P  condensate pumps (y 0-2)
z8    H H H H M M M w w w w w     w  CW in, on the condenser's north face
z9    L L L L L L C C C C C C
z16   L L L L L L C C C C C C
z17   . G G G G . P P . P P .     G  generator (y 0-3), terminals south at z 25
z19   . G G G G . . F F D D D     F  feed pumps (y 0-1)  D  deaerator (y 0-4)
z24   . G G G G . . F F D D D
z29   . . . . . . . X X . . .     X  HP heaters (y 0-2), south of the second feed pump
z37   . . . . . . . f f . . .     f  feedwater out, south of the HP heaters
```
- **The rows in detail:**
  - V: x 0-2, z 0-2.
  - H: x 0-3, z 3-8.
  - M: x 4-6, z −1..8.
  - L: x 0-5, z 9-16.
  - C: x 6-11, z 9-16.
  - P: x 6-7 and x 9-10, z 17-18.
  - G: x 1-4, z 17-24.
  - D: x 9-11, z 19-26.
  - F: x 7-8, z 19-23 and z 24-28.
  - X: x 7-8, z 29-36.
- **The CW pumps** stand in the intake, piped to the condenser's north face.
- **Electrical:** the generator's breaker, bus duct, GSU and UAT stand south of its terminals. Every motor's box faces west onto a free cell.
- **Rotation:** any of the four. Each part works by touching; every motor's box faces west, away from what it touches.

## Nozzles
- **Steam in:** the valve chest's top face. It is piped from the steam generators' tops.
- **Circulating water:** in on the north face of the condenser's bottom cells, out on its south face (discharged). The CW pumps
  give it on the east face of their cells at height 3.
- **Feedwater:** out on the south face of the HP heaters' back bottom cells, piped to the steam generators' feedwater faces.
- **Make-up water:** the deaerator takes it off a pump.
- **Motor boxes:** the west face of each pump's main cell. The condensate pumps are 480 V; the feed and CW pumps 4.16 kV.

## How it forms and runs
- **Finding the parts:** the HP turbine finds, by touching:
  - the valve chest, the MSR and the LP turbine;
  - from the LP turbine, the generator and the condenser;
  - from the condenser, its condensate pumps;
  - from those, the deaerator;
  - from it, the feed pumps;
  - from them, the HP heaters.
- **NOT BUILT** names what is missing.
- **It runs** once there is steam, vacuum and feedwater.
- **The readout** gives:
  - the steam taken, the vacuum (kPa), MW and the steam dump;
  - CW flow;
  - the deaerator and hotwell levels and the feed flow;
  - and the trip.
- **A turbine trip** stops the steam. The steam generators learn of it through their steam pipe, and the reactor trips above P-9.
- **Removing a part** stops the turbine: a pump stops its flow; the condenser or the CW pumps lose the vacuum.

## Game test
`turbineIsland`:
- **Build:** the 8a island with the turbine island south of it, and two CW pumps standing in a dug pool.
- **Offsite power:** the 16.5 MW gas turbine (the `gasTurbine` setup) on a 13.8 kV bus.
  - The UAT feeds 4.16 kV to all the plant's pumps, and a unit substation feeds the 480 V loads.
  - Load banks on the bus.
- **Startup:** with hooks, the reactor at hot standby and then critical at a 25 % setpoint.
- **Checks:**
  - the CW pumps run and the condenser holds vacuum;
  - the feed pumps feed the steam generators;
  - the turbine rolls up and its generator synchronizes and carries about 12 MW;
  - a CW pump stopped and the other with it: low vacuum trips the turbine. Below P-9 the reactor stays up.
- **Screenshot:** the plant.

## Added while building (8b)
- **The steam rate** is 6.1 kg/kWh (not 5.9), so the full 86 kg/s makes 51 MW, 33 % of 155 MW.
- **The governor** admits the steam its generator can use: on line, what it gave last plus 2 MW; while rolling, 5 % for warming.
  The **steam dump** takes what the steam generators still offer above 7.0 MPa, up to **40 %** of full steam *(proposed)*.
- **Coolant pumps** keep their flow through a voltage sag: only a stopped motor lets the flywheel carry them down (8a's pumps
  coasted when a 20 MW load step sagged the 4.16 kV bus, and the reactor tripped on low flow).
- **Game test:**
  - the load banks take 8 MW until the turbine generator is on line, then 20 MW. They go back to 8 MW before the turbine is
    tripped, so the gas turbine alone can carry the bus;
  - one 4.16 kV tray at up+14, a 480 V busway at up+17 and the 13.8 kV bus duct at up+16 keep the three networks apart over
    the plant.
