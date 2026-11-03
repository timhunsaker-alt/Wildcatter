# The delayed coker and coke calciner (Phase 10b3a, approved with the 10b3 plan 2026-11-03)

The potline's anodes are mostly calcined petroleum coke. This unit makes it from the crude unit's residue:
- a fired heater takes the residue to about 495 °C;
- the hot residue goes into one of a pair of coke drums, where it cracks and the coke is left behind;
- the drum vapour goes back to a fractionator, which splits it into coker gas, coker naphtha and coker gas oil;
- the full drum is steamed, quenched and cut out with high-pressure water into the coke pit;
- a rotary kiln calcines the green coke at about 1,300 °C, driving off its moisture and volatiles, and a rotary cooler cools it
  for bagging.

**Decided with Tim on 2026-11-03:** a coker sized for a 40-pot line, about 2 L/s of residue.

**Sources:**
- Delayed coking: Ellis and Paul, *Tutorial: Delayed coking fundamentals* (AIChE, 1998). The heater outlet is 480-500 °C, and
  drums run on 12-24 h fill cycles. On a vacuum residue the coke yield is 25-35 %, and on an atmospheric residue it is lower,
  since the gas oil in it distils over.
- Anode-grade (sponge) coke from low-sulfur residue. Calcining in a rotary kiln at 1,200-1,350 °C loses 20-25 % as moisture and
  volatiles (Edwards, *The production and use of calcined petroleum coke*, JOM 2015).
- Hydraulic decoking at about 200-280 bar.

A figure marked *(proposed)* is an estimate or a proposal.

## Size *(proposed values; the size is Tim's)*
**The rate is 169 t of residue a day, 1.96 kg/s (2.06 L/s).** That is enough for the 25.3 t of green coke a 40-pot line's
anodes need. The big crude unit makes about 10 L/s of residue, so the coker takes a fifth of it.

| Out of 1 kg of residue | kg | Goes to |
|---|---|---|
| Green coke | 0.15 | the drums, then the pit as **green petroleum coke**, 1,000 kg pieces |
| Coker gas oil | 0.65 | the diesel line, at 0.90 kg/L |
| Coker naphtha | 0.13 | the gasoline line, at 0.72 kg/L |
| Coker gas | 0.07 | its own gas line, at 1.0 kg/m³ and 45 MJ/m³; the plant's gas burners fire on it |

**Heater duty:** 1.1 MJ/kg of residue, about 2.2 MW of gas, coker gas or LPG at full rate.

**Calciner:**
- 1 kg of green coke gives **0.78 kg of calcined coke**;
- the kiln burns its own volatiles plus **1.5 MJ/kg of calcined coke** of gas;
- at full rate that is 0.23 kg/s of calcined coke (19.7 t a day).

**Cutting water:** 0.3 L/kg of coke is made up; the rest is recirculated through the pit.

## Parts (cells W x H x L; each a true-size big part; all values *(proposed)*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `coker_fractionator` | 3 x 18 x 3 | The coker fractionator, with its pumparound and side-draw pumps: **45 kW at 480 V**.<br>• Residue in at the north face of its front cells.<br>• Coker gas oil out of the south face of its back cells at x 0.<br>• Naphtha out of the same face at x 2.<br>• Coker gas out of the east face of its bottom cells.<br>• It hands its bottoms to the heater it touches, and takes the drum vapour back. |
| `coker_heater` | 4 x 10 x 6 | A two-pass coker charge heater, outlet 495 °C. **1.1 MJ/kg of residue** of gas, coker gas or LPG, in at the east face of its bottom cells.<br>• Charge pump **75 kW at 480 V**.<br>• It must touch the fractionator and the coke drums. |
| `coke_drum` | 3 x 16 x 3 | A coke drum, 3 m across and 15 m tall, with its bottom unheading device: hydraulics **15 kW at 480 V**.<br>• A coker has two. They stand on the coke pit, and both touch the heater.<br>• The two are distinct real vessels, one filling while the other is decoked, not a tiled block. |
| `coke_pit` | 6 x 4 x 8 | The coke pit and its hydraulic decoking system: high-pressure jet pump **400 kW at 4.16 kV**, about 200 bar.<br>• It cuts the coke out of the drum that is not filling, at up to 10 t an hour.<br>• Green petroleum coke goes out in 1,000 kg pieces to a hopper under it, or to an empty hand.<br>• Cutting water off a pump. |
| `coke_kiln` | 3 x 3 x 30 | A rotary calcining kiln, 2.5 m across and 30 m long, at about 1,300 °C; drive **75 kW at 480 V**.<br>• Green coke by a hopper on top.<br>• Gas, coker gas or LPG in at the east face of its bottom cells.<br>• Calcined coke to the cooler it touches. |
| `coke_cooler` | 2 x 3 x 12 | A rotary cooler; drive **30 kW at 480 V**.<br>• It takes the kiln's coke, and a bagging station set against it fills **1 t bags of calcined coke**. |

**Coker gas** is a new pipe product, in m³. Every part that fires through `GasFiring` burns it:
- the plant's kilns and heaters;
- the Bayer package boiler and calciner;
- the lime kiln.

It is not pipeline gas, so engines and turbines do not take it.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Motor boxes:** on the west face of every part's main cell. The coke pit's is **4.16 kV**; the others are 480 V.
- **Pipes, all at the bottom (y 0):**
  - residue in at the north face of the fractionator's front cells;
  - gas oil and naphtha out of the south face of its back cells, at x 0 and x 2, so their runs don't touch;
  - coker gas out of the east face of its bottom cells;
  - fuel in at the east face of the bottom cells of the heater and the kiln.
- **Touching:**
  - the heater touches the fractionator and both drums;
  - the drums stand on the pit;
  - the kiln's back (south) end touches the cooler;
  - a bagging station touches the cooler.
- **Hoppers:**
  - in from on top: green coke on the kiln, empty bulk bags on the bagging station;
  - out to a hopper under it: green coke from the pit, calcined coke bags from the bagging station.
- **Water:** the pit takes water off a pump.

## How it runs
**Flow:**
- The fractionator takes what its inlet run offers, up to the plant rate with a fifth more to catch up, while its pumps run.
- The heater cracks it while its charge pump runs, as far as its fuel allows. It sends the coke into the filling drum and the
  liquids and gas to the fractionator, which holds about 5 s of each at its nozzles.

**The drums:**
- One drum fills while the pit cuts out the other.
- The filling drum switches once it holds a tonne of coke and the other drum is empty. A real drum fills for 12-24 h; that
  wait is not modelled.
- The readout names the filling drum and what each drum holds.
- With one drum, the heater fills it to its capacity (**40 t**) and then stops: ONE DRUM.

**The pit** cuts while its jet pump and the drum's unheading hydraulics run, and while it has water.

**What each part reads when it stops:**
- NO RESIDUE, NO FUEL, NO WATER and NO COKE;
- NOT BUILT (set it against the X);
- FULL: the pit's outlet, or the cooler with no bagging station;
- "its output has nowhere to go".

**Short supply:**
- The heater scales its crack by the fuel it got against what it needs.
- The kiln does the same.
- A product that backs up at the fractionator slows the heater.

**Removing a part** stops the flow through it.

## Left out (ROADMAP Ideas)
- A vacuum unit ahead of the coker, which would raise the coke yield.
- Coker LPG recovery.
- Drum steaming and blowdown, the drum's heat-up and its thermal cycle.
- Shot and needle coke.
- Sulfur and metals in the coke.

## Game test
`coker` (z near −5500):
- **Power:**
  - a diesel set's 480 V on a busway;
  - a second set through a backward unit substation, for the pit's 4.16 kV jet pump.
- **Feeds:**
  - a stock tank of residue to the fractionator;
  - an LPG bullet to the heater and the kiln;
  - water by hook to the pit;
  - stock tanks on the gas oil and naphtha nozzles, and a flare stack on the coker gas.
- **Checks:**
  1. The fractionator, heater, drum, kiln and cooler run.
  2. Diesel and gasoline come into their tanks.
  3. The drum that is not filling is given a little coke and the pit almost a tonne, so the pit cuts and a piece of green coke drops under it.
  4. The cooler is first given almost a bag's worth, so a bag of calcined coke drops under the bagging station.
- **Screenshot:** the coker and the calciner.
