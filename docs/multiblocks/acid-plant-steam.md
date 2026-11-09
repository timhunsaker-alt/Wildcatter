# The acid plant's steam to a turbine

> **BUILT as wave 14c (2026-11-09), decided on recommendation while Tim was away; not approved.** See "As built" at the end and
> `docs/questions/phase14-gas-power.md`. Renumbered 14b-14e on 2026-11-09: Phase 14 was already the ammonia plant (14a). Decisions are in
> `docs/questions/specs-wildcatter.md` (items 5-7).

The sulfuric acid plant (acid-plant.md) burns sulfur, and its waste heat boiler raises about **2.1 kg/s of 40 bar steam**. Today the
melter takes a little of it and the rest goes up the boiler's silencer. Real sulfur-burning plants superheat that steam in the gas
leaving the converter's first bed and run a turbine generator on it: a sulfur-burning acid plant is a power plant that makes acid on
the side, and most of them export power. This spec sends the steam to a turbine.

Sources:
- acid-plant.md (approved): 1.2 t of 40 bar steam a tonne of acid, 2.1 kg/s at 150 t/d; the plant's own use 35-50 kWh/t (BREF).
- A 2,400 t/d Lurgi sulfur-burning plant: about 1.4 t of 60 bar steam a tonne of acid, a 15 MW steam turbine generator, 37.5 kWh/t
  used ([Phoenix Equipment listing](https://www.phxequip.com/Multimedia/documents/plants/sulfuric-acid-plant-2-400-tpd-149.pdf)).
- Stingray Copper's planned 750 t/d plant: superheated high-pressure steam to a turbo-generator of about 10 MW
  ([NS Energy](https://www.nsenergybusiness.com/?p=54058)).
- Large sulfur-burning plants make over 1.3 t of steam a tonne of acid ([CRU, *Maximising energy efficiency in acid plants*,
  2022](https://bcinsight.crugroup.com/2022/11/30/maximising-energy-efficiency-in-acid-plants)).
- combined-cycle.md (approved): the 5 MW steam turbine makes 5.2 MW from 6 kg/s of 40 bar, 400 C steam.

## Numbers
- **Steam:** 2.1 kg/s from the boiler. The melter's coils take about **0.06 kg/s** (100 kW at about 1.7 MJ/kg of condensing steam,
  derived), so **about 2.0 kg/s** is left for the turbine.
- **Power:** the 5 MW turbine makes 0.87 MW per kg/s at its design point. At a third of its flow it loses about a tenth to
  throttling *(proposed)*: **about 1.6 MW** at 2.0 kg/s. That is 0.21 MW per t/d of acid a day, between the Lurgi plant (6 kW per
  t/d, a plant that also sends steam to process) and Stingray (13 kW per t/d).
- **The plant's own use:** about 0.30 MW at 150 t/d (the BREF's 35-50 kWh/t; the 250 kW blower is most of it). So the acid plant
  **exports about 1.3 MW**: enough to run the uranium mill's small motors, not its SAG mill.
- **Black start:** the plant still needs outside power to start its blower. Once it makes steam, the turbine can carry the blower and
  the outside supply can go, as a real plant islands.

## Parts
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `acid_superheater` | 3 x 6 x 3 | **New.** The steam superheater in the gas between the converter's first and second beds, as real DCDA plants have. It takes the boiler's saturated 40 bar steam to about **400 C** and cools the gas by what it takes (about 0.83 MW at 2 kg/s: 2,801 to 3,214 kJ/kg, IAPWS-IF97). It stands against the converter. No power. |
| `steam_turbine_5` | 3 x 3 x 6 | **Existing** (combined-cycle.md): the 5 MW condensing turbine, its steam line on the superheater. |
| `st_generator` | 3 x 3 x 4 | **Existing**: 6.25 MVA, 13.8 kV (11 kV IEC) generator. |
| `air_cooled_condenser` | 6 x 8 x 6 | **Existing**: four 50 hp fans, 149 kW at 480 V. At a third of the steam two fans run *(proposed)*. |
| `hrsg_feed_pump` | 2 x 2 x 4 | **Existing**: 45 kW, 480 V. It returns condensate to the waste heat boiler in place of its water pipe (the boiler's make-up then falls to the 10 % the condenser loses, *proposed* as the steam plant's). |
| `generator_breaker`, bus duct | | **Existing** (gas-turbine.md): the 1,200 A generator breaker on 13.8 kV. |
| `unit_aux_transformer` | | **Existing** (turbine-island.md): 13.8/4.16 kV (11/3.3 kV IEC), 7.5 MVA. It puts the generator on the acid plant's own 4.16 kV bus, and through a unit substation to 480 V. |

## Layout (facing north, plan view; the converter's main cell at 0,0,0, as acid-plant.md)
Layer y 0:
```
x:  -3-2-1 0 1 2 3 4 5 6 7 8
z0   I I I C C C C C A A A .      C converter (5 x 15 x 5)   I interpass absorber   A final absorber
z1   I I I C C C C C A A A .
z2   I I I C C C C C A A A .
z3   . . . C C C C C . . . .
z4   . . . C C C C C . . . .
z5   . . . S S S . . . . . .      S acid superheater (y 0-5), against the converter's south face
z6   . . . S S S T T T . . .      T steam turbine (3 x 3 x 6, running east), against the superheater's east face
z7   . . . S S S T T T . . .
z8   . . . . . . T T T G G G      G generator against the turbine's east end
```
Layers y 1-2: the turbine and generator continue; the superheater continues to y 5, its steam outlet nozzle on top.
The air-cooled condenser stands against the turbine's south face (not drawn); the feed pump touches the waste heat boiler.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `C` | `wildcatter:converter` |
| `I` | `wildcatter:interpass_absorber` |
| `A` | `wildcatter:final_absorber` |
| `S` | `wildcatter:acid_superheater` |
| `T` | `wildcatter:steam_turbine_5` |
| `G` | `wildcatter:st_generator` |

## Controller, rotation, forming
- **Controllers:** the acid plant's parts keep their own roles. The steam side's controller is `steam_turbine_5`, as in combined cycle.
- **Rotation:** any of the four; each part is placed on its own and works by touching.
- **Forms:** the turbine looks for steam from an HRSG **or** an acid superheater it touches; the superheater takes its steam from the
  waste heat boiler through a STEAM pipe (from the boiler's top face) and its heat from the converter it touches.
- **Removing a part:**
  - the superheater: the turbine reads NO STEAM and the boiler's steam goes back up its silencer, as today;
  - the turbine, condenser or feed pump: as in combined cycle (NO CONDENSER, NO FEEDWATER); the acid plant keeps running and vents;
  - the converter stopped: the superheater has no heat; it passes saturated steam and the turbine trips on **LOW STEAM TEMPERATURE**
    *(proposed)*.

## Nozzles
- Waste heat boiler: **new** STEAM out of its top face (main steam at 40 bar); FEEDWATER in on any face, from the feed pump, in place
  of its water pipe.
- Acid superheater: saturated steam in on its west face, bottom cell; superheated STEAM out of its top face.

## How it runs
- The boiler's steam follows the furnace's sulfur rate, as now; the melter takes its share first.
- The turbine takes what the superheater gives, at 0.87 MW per kg/s less the part-load loss, and synchronizes onto its bus like any
  generator.
- With the acid plant islanded on its own turbine, a blower trip takes away the steam: the turbine coasts down and the island goes
  dark, as a real one does. The readout says why.

## Left out
- A back-pressure turbine with process steam extraction (the Lurgi plant's): Wildcatter has process steam only at the Bayer refinery.
- The heat in the absorbers' acid coolers (real heat recovery systems raise low-pressure steam from it).

## Game tests
- `acid_plant` (extended): with the superheater, turbine, generator, condenser and feed pump built, the turbine synchronizes onto the
  plant's bus and reads **1.4 to 1.8 MW**; the genset that started the blower is tripped and the plant carries itself.
- Breaking the superheater: NO STEAM; the acid plant keeps making acid.
- **Screenshot:** the acid plant with its turbine.

## Proposed wave
**14c** (was 14b): one new part, the boiler's two new nozzles, and the turbine's second steam source.

## As built (14c, 2026-11-09)
Decisions taken while building are in `docs/questions/phase14-gas-power.md` (items 9-12).
- **The superheater** (`acid_superheater`, 3 x 6 x 3, part of the acid plant's block family) stands against the converter, takes the
  boiler's steam on a steam pipe at the **west face of its bottom cells**, and passes it to the 5 MW turbine **by touching** (as the HRSG
  does). It is hot while the converter touching it has converted in the last 2 s.
- **The waste heat boiler:** STEAM out of its top face (held 5 s; what nobody takes goes up the silencer); the melter touching it takes
  0.06 kg/s first; a feed pump touching it returns 90 % of the turbine's steam as condensate (`AcidSteam.makeUpKgs`).
- **The turbine:** 0.87 MW a kg/s less a throttle loss of 15 % x (1 - flow/design), a tenth at a third of design (`AcidSteam.turbineWatts`):
  about 1.6 MW at 2.0 kg/s; times its condenser fans' and feed pump's running share, as in combined cycle. It trips its generator on
  LOW STEAM TEMPERATURE and resets once stopped with superheat back. All four condenser fans run (the spec's two at part load is not built).
- **The layout of record** (NeoForge scene `acid_steam`): the acid plant facing west as in the client scene `acidPlant`; the superheater
  (facing north) at x 27-29 z 1-3 against the converter's south face; the turbine x 30-32 z 1-6; generator x 33-35 z 1-4; generator breaker
  x 33-34 z 5-6; condenser x 27-32 z 7-12; feed pump x 22-23 z 1-4 against the boiler; the unit auxiliary transformer x 35-38 z 7-10, its
  4.16 kV onto the plant's bus with the blower and a backward unit substation from two diesel sets.
- **A solver change it needed:** a machine closing onto a bus takes the bus's angle to the nearest 30 degrees (the unit auxiliary
  transformer is delta-wye); without it the turbine and the diesel sets drove about 2 MW round between them.
- **Measured:** steam for 1.44-1.52 MW at 1.9-2.0 kg/s (its 480 V motors at about 95 % voltage on two diesel sets); on its own, it carries
  the plant (about 0.43 MW) at 59.7 Hz.
