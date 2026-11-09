# The sales line: receipt and delivery meter stations

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **14a**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 1-4).

The gas plant's residue gas has a buyer besides engines and the flare: a **transmission line** that belongs to someone else, as an
interstate pipeline does. The player builds a **receipt meter station** where processed gas leaves the gas plant and goes into that
line, and can build **delivery meter stations** (city gates) elsewhere in the same dimension that take gas back out of it. Between
the two the gas is "on the system": the transmission company carries it, keeps a share as compressor fuel, and keeps an account of
what each station put in and took out.

This closes the known gap "Gas has no sales pipeline yet" (PROGRESS.md) and lets a gas field run a power plant that is far from it
without a thousand blocks of steel pipe.

Sources:
- AGA Report No. 9 and ISO 17089-1 (multipath ultrasonic gas meters for custody transfer); Emerson, *Custody transfer and check
  metering* (meter runs, flow computers, gas chromatographs, provers).
- Typical interstate tariff quality limits (the General Terms and Conditions of a FERC gas tariff): water about **7 lb per million
  scf**, H2S about **0.25 grain per 100 scf (about 4 ppm)**, heating value about **950-1,150 Btu/scf** *(unverified: from memory;
  each pipeline's own tariff sets them)*.
- Fuel retention on interstate lines: **1-3 %** of receipts *(unverified)*.

A figure marked *(proposed)* is chosen for the game; *(unverified)* is from memory or a typical value not yet checked against a source.

```
GAS PLANT (chiller) -> pipeline gas -> FILTER SEPARATOR -> METER RUN -> PIPELINE TAP  ==>  the transmission line (off the map)
                                                         METER BUILDING (controller: flow computer, chromatograph, RTU)

the transmission line  ==>  PIPELINE TAP -> METER RUN -> REGULATOR SKID -> pipeline gas -> gas sets, turbines, burners
                                            METER BUILDING (controller, set to DELIVERY)
```

## What the line is
- **One system a world** *(proposed)*: every station in a dimension is on the same off-map line. Its state is world saved data: the
  energy received, the energy delivered and the fuel retained, in GJ.
- **Receipts:** a receipt station puts pipeline gas into the line, metered in m3 and GJ at the gas's heating value.
- **Deliveries:** a delivery station takes pipeline gas out, up to its rating, while the line's account holds energy for it.
- **Fuel retention:** the line keeps **2 %** of each receipt *(proposed; real 1-3 %)*, as gas burned in its compressor stations.
- **Gas only in, gas only out.** Nothing is paid; the "buyer" is the line's carriage of the gas to wherever the player takes it. The
  receipt station's run ticket is the record a real producer is paid on.
- **Off-spec gas is refused** at the receipt station, as a real pipeline shuts in a receipt point:
  - pipeline gas: accepted;
  - separator gas, compressed gas or dried gas: **OFF SPEC: RICH** (its NGL makes it over 1,150 Btu/scf and it would drop liquids
    in the line);
  - wet gas: **OFF SPEC: WATER**;
  - anything else: not gas.

## Size
- **Station rating:** **5 m3/s** of standard gas *(proposed)*, about 15 million scf a day, five of the 5b gas plant's 1.0 m3/s.
  A real 4 in multipath meter at 1,000 psi passes far more; the rating is the station's piping and regulators.
- **Heating value:** pipeline gas at **38 MJ/m3** (the mod's `GAS_JOULES_PER_M3`), about 1,020 Btu/scf.
- **What it moves:** a full gas plant's 1.0 m3/s is **38 MW** of fuel, about 37 MW delivered after retention: enough for two
  16.5 MW gas turbines at about 46 % combined cycle, or 37 one-megawatt gas sets *(derived)*.

## Parts
Voltages are the world's standard tiers: "residential" is 120/240 V split-phase (NA) or 230 V (IEC).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `meter_building` | 3 x 3 x 4 | **Controller.** A prefabricated metering shelter: the flow computer (AGA 9 ultrasonic flow, AGA 8 compressibility), a gas chromatograph reading the heating value every few minutes, the RTU and its battery. **2 kW at residential voltage** *(proposed)*: heaters, the chromatograph's oven, lights. Sneak-click sets **RECEIPT** or **DELIVERY**. |
| `filter_separator` | 2 x 4 x 2 | A vertical filter separator: coalescing elements and a liquid sump, catching lube oil, glycol carry-over and line debris before the meter. **Receipt stations need one.** Its sump is counted and drains itself. No power. |
| `ultrasonic_meter_run` | 1 x 2 x 8 | A 4 in, 4-path ultrasonic meter in its run: a flow conditioner, 10 diameters upstream and 5 downstream of straight pipe, the temperature well and a check meter port (AGA 9). No power of its own: the building powers its transducers. |
| `regulator_skid` | 2 x 2 x 5 | **Delivery stations need one.** Two runs of worker and monitor regulators with relief valves, and an indirect-fired line heater ahead of them, since gas cools about 7 F for every 100 psi it drops *(unverified)*. The heater burns **0.2 %** of the gas it passes *(proposed)*. |
| `pipeline_tap` | 1 x 2 x 1 | The tie-in to the transmission line: the hot-tap tee, the mainline block valve with its operator, and a check valve (receipt). The line's side of it is off the map. |

## Layout (facing north, plan view; x east, z south; the building's main cell at 0,0,0)
Layer y 0:
```
x:  0 1 2 3 4 5 6 7 8 9
z0  B B B . . . . . . .      B  meter building (y 0-2)
z1  B B B F F M M M M M      F  filter separator (y 0-3), inlet on its west face
z2  B B B F F M M M M M      M  meter run (y 0-1), 8 long east-west, touching the building's east face
z3  B B B . . . . . . T      T  pipeline tap (y 0-1), touching the meter run's east end
```
Layer y 1: the building, the filter separator and the meter run continue; the tap's valve operator.
Layers y 2-3: the building's roof (y 2) and the filter separator's top head (y 2-3).

A delivery station has the regulator skid (`R`, 2 x 2 x 5) in place of the filter separator, at the meter run's outlet end, and the
tap at its inlet end: the gas flows tap -> meter run -> regulator skid -> pipeline gas out of the skid's south face.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `B` | `wildcatter:meter_building` |
| `F` | `wildcatter:filter_separator` |
| `M` | `wildcatter:ultrasonic_meter_run` |
| `R` | `wildcatter:regulator_skid` |
| `T` | `wildcatter:pipeline_tap` |

## Controller, rotation, forming
- **Controller:** `meter_building`. It holds the station's state (mode, the run ticket, today's and the lifetime totals).
- **Rotation:** any of the four. Each part is placed on its own; they work by touching, as the acid plant's do.
- **Forms** when the building touches a meter run and the meter run touches a tap, plus a filter separator (RECEIPT) or a regulator
  skid (DELIVERY) on the meter run. The readout names what is missing: NOT BUILT: NO METER RUN, NO TAP, NO FILTER, NO REGULATOR.
- **Removing a part** shuts the station in: the tap's block valve closes. Nothing is lost; the line's account is the world's.

## Nozzles
- Filter separator: pipeline gas in on its west face, bottom cell; out to the meter run by touching.
- Regulator skid: pipeline gas out on the south face of its back cells (z 4, bottom).
- Meter building: its residential service box on the west face of its main cell.

## How it runs
- **Receipt:** each interval the station pulls up to its rating of pipeline gas from its inlet run, as a gas plant's buyers do, and
  books it: m3, GJ at the chromatograph's heating value, and 2 % to fuel. It refuses an off-spec run and reads which spec it fails.
- **Delivery:** it offers pipeline gas on its outlet run, up to its rating, while the line holds energy for it. It reads **NO GAS ON
  THE LINE** when the account is empty: the line never delivers more than was received, less fuel.
- **Power:** without its residential supply the chromatograph and flow computer stop after the RTU's battery, **8 hours** of game
  time *(proposed)*, and the station shuts in: METER FAILED. A real station cannot sell unmetered gas.
- **The run ticket:** a click shows the day's and the lifetime m3, GJ and MMBtu, the heating value and the line's balance.
- **PLC points:** RUN (permissive), RUNNING, SHUT IN, a 4-20 mA flow and a 4-20 mA heating value, as the other process plants
  (plc.md, 13g).

## Left out
- Pressure in the pipe: the mod's pipes carry no pressure, so the regulators' set points and line pack are not modelled.
- Hydrogen sulfide and CO2: the mod's gas has none, so there is no amine unit or sour-gas spec.
- Odorizing: a city gate odorizes gas for homes; Wildcatter's homes burn no gas.
- Cathodic protection of the line, pigging and in-line inspection, meter proving.
- Prices, contracts and nominations: there is no money in Wildcatter.

## Game tests
- `salesGas`:
  - the gas plant scene's train makes pipeline gas into a receipt station on a residential service from a pole transformer;
  - the line's balance rises with the energy received, less 2 %;
  - a delivery station 200 blocks away feeds a 1 MW gas set, which starts and carries a load bank;
  - separator gas piped to the receipt station reads OFF SPEC: RICH and nothing is booked;
  - the receipt station's power cut, then 8 game hours skipped by hook: METER FAILED and shut in;
  - with the line's balance drained by hook, the delivery station reads NO GAS ON THE LINE and the set stops on fuel.
- **Screenshot:** the receipt station.

## Proposed wave
**14a**, alone: a small wave of five parts and one world counter. It needs nothing new from the solver.
