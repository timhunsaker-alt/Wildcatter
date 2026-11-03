# The coke ovens and tar distillery (Phase 10b3a, approved with the 10b3 plan 2026-11-03)

The anodes' binder is coal-tar pitch, a by-product of making metallurgical coke:
- coking coal is charged into a battery of slot ovens and heated out of contact with air for about 18 h;
- the coke is pushed out and wet-quenched;
- the raw gas the coal gives off is cooled in the by-product plant, which condenses the tar and sends the cleaned coke oven gas
  on, part of it back to heat the ovens;
- a tar distillery takes the tar down to pitch, its residue, with the lighter tar oils as its overhead.

**Decided with Tim on 2026-11-03:**
- vanilla coal is the coking coal;
- the metallurgical coke leaves as an item that works wherever coal does;
- the coke oven gas goes to a gas line.

**Sources:**
- By-product slot ovens give 0.70-0.78 t of coke a t of dry coal, about 300-400 Nm³ of gas at 17-19 MJ/m³, and 30-40 kg of tar.
  About 40-45 % of the gas goes back to underfire the battery (EU BREF, *Iron and Steel Production*, 2013, chapter 6; Speight,
  *Handbook of Coal Analysis*).
- Tar distillation gives 50-60 % pitch as the residue (Ullmann's, *Tar and Pitch*).
- A wet quench evaporates about 0.4-0.5 t of water a t of coke.

A figure marked *(proposed)* is an estimate or a proposal.

## Size: 240 t of coal a day *(proposed values)*
That is 2.78 kg/s, enough tar for the 4.7 t of pitch a day a 40-pot line's anodes need.

| Out of 1 kg of coal | Amount | Goes to |
|---|---|---|
| Metallurgical coke | 0.75 kg | items of **112.5 kg**, one for each coal item (150 kg); a coal block (1,350 kg) gives 9 |
| Raw coke oven gas | 0.40 m³ | the by-product plant on a pipe ("raw coke oven gas", which carries the tar) |
| of which back to the underfiring | 40 % | taken off inside the by-product plant |
| Coke oven gas, net | 0.24 m³ at **18 MJ/m³** | its own gas line: 0.67 m³/s, about 12 MW, at full rate |
| Coal tar | 35 g | the tar distillery, on a pipe, in kg |
| Quench water | 0.45 L a kg of coke | evaporated in the quench tower |

**Tar distillery:**
- 1 kg of tar gives **0.55 kg of pitch** and 0.45 kg of tar oils;
- the tar oils go onto the residue line, at 1.05 kg/L;
- it fires **1.0 MJ/kg of tar**.

**Metallurgical coke** is 88 % carbon, at 29 MJ/kg, so a piece is 99 kg of carbon or 3.26 GJ. It goes wherever coal goes:
- the chlorinators' and the B4C furnace's carbon;
- the steam plant's burner.

## Parts (cells W x H x L; each a true-size big part; all values *(proposed)*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `coal_tower` | 4 x 12 x 4 | The coal blending and charging bunker, with its conveyor: **15 kW at 480 V**.<br>• Coal or coal blocks by a hopper on top. It holds up to 10 t.<br>• It feeds the battery it touches. |
| `coke_oven_battery` | 10 x 8 x 16 | A battery of **13 by-product slot ovens**, each 0.45 m wide, 6 m tall and 14 m long, taking 15 t charges and coking for 18 h; larry car **30 kW at 480 V**.<br>• The battery is one part with its ovens inside it; they are not tiled blocks.<br>• It must touch the coal tower, the pusher machine and the quench tower.<br>• Raw coke oven gas out of the south face of its back cells (the gas collecting main). |
| `pusher_machine` | 4 x 6 x 16 | The pusher and leveller machine on its rails along the battery's west side: **75 kW at 480 V**. |
| `coke_quench_tower` | 5 x 20 x 5 | The coke guide, quench car and wet quench tower on the battery's east side: quench pump **55 kW at 480 V**.<br>• Water off a pump.<br>• Metallurgical coke goes out to a hopper under it, or to an empty hand. |
| `byproduct_plant` | 5 x 10 x 8 | The primary coolers, tar decanter and exhauster: exhauster **300 kW at 4.16 kV**.<br>• Raw gas in at the north face of its front cells.<br>• Coke oven gas out of the south face of its back cells.<br>• Coal tar out of the east face of its bottom cells. |
| `tar_still` | 3 x 12 x 3 | A continuous tar distillation column with its pipe still: pumps **15 kW at 480 V**.<br>• Tar in at the north face of its front cells.<br>• Gas, coke oven gas or LPG in at the east face of its bottom cells.<br>• Pitch out of the south face of its back cells at x 0, and tar oils (residue) at x 2. |

**New pipe products:**
- raw coke oven gas and coke oven gas, in m³;
- coal tar and pitch, in kg. A stock tank holds both: pitch is kept hot in a heated tank.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Motor boxes:** on the west face of every part's main cell. The by-product plant's is **4.16 kV**; the others are 480 V.
- **Pipes, all at the bottom (y 0):**
  - in at the north face of the front cells;
  - out of the south face of the back cells;
  - the tar out of the by-product plant's east face;
  - the still's fuel in at its east face.
- **Touching:** the battery touches the coal tower (at its north end), the pusher (on its west side) and the quench tower (on
  its east side).
- **Hoppers:**
  - in from on top: coal on the coal tower;
  - out to a hopper under it: coke from the quench tower.
- **Water:** the quench tower takes water off a pump.

## How it runs
- **The battery** cokes coal from the tower at up to the plant rate with a fifth more, while its larry car, the pusher and the
  quench pump run, and while the quench tower has water and room. The quench tower makes a coke item for every 112.5 kg.
- **Its gas:**
  - the battery holds about 5 s of raw gas;
  - when nothing takes it, it stops: its gas has nowhere to go (a real battery would flare it);
  - a battery without a by-product plant cannot run.
- **The by-product plant** takes the raw gas while its exhauster runs. It keeps back the 40 % for underfiring, and holds about
  5 s of coke oven gas and tar at its nozzles.
- **The tar still** distils the tar it is offered, scaled by its fuel.
- **What each part reads when it stops:**
  - NO COAL, NO WATER, NO TAR and NO FUEL;
  - NOT BUILT (set it against the X);
  - FULL: take the coke;
  - "its output has nowhere to go".
- **Removing a part** stops the flow through it.

## Left out (ROADMAP Ideas)
- Ammonia liquor and ammonium sulfate.
- Light oil (benzol) recovery.
- Desulfurizing the gas.
- Dry quenching.
- The battery's heat-up (weeks for a new battery) and its oven-by-oven push schedule.
- Coal blending and coke strength.
- Engines and turbines on coke oven gas.

## Game test
`cokeOvens` (z near −5700):
- **Power:** a diesel set's 480 V on a busway, and a second set through a backward unit substation for the exhauster.
- **Feeds:**
  - coal blocks by hopper;
  - water by hook to the quench tower;
  - an LPG bullet to the tar still;
  - stock tanks on the pitch and tar oil nozzles, and a flare stack on the coke oven gas (the surplus a real plant flares).
- **Checks:**
  1. The battery, by-product plant and still run, and the by-product plant holds coke oven gas at its nozzle.
  2. Coke items drop under the quench tower.
  3. Pitch comes into its tank.
  4. With the by-product plant's exhauster unpowered, the battery stops: its gas has nowhere to go.
- **Screenshot:** the battery.
