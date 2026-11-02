# Fuel handling: refuelling, the spent fuel pool and radiation (APPROVED 2026-11-02 with the Phase 8e plan)

At an outage, with the plant at cold shutdown:
1. A refuelling machine over the vessel moves assemblies one at a time underwater.
2. A fuel transfer system (upender, carriage and tube) carries them to a spent fuel pool.
3. The pool, about 12 m deep, keeps them cool and shields their radiation.

Radiation is a real dose with real effects. An electronic personal dosimeter reads it.

Decided with Tim on 2026-11-02:
- **The machine runs the outage:** REFUEL (3 to the pool, the shuffle, 3 fresh) and DEFUEL (the whole core, a damaged one too).
- **Irradiated fuel is never an item.** It lives in the vessel and the pool as records. Casks are in ROADMAP Ideas.
- **The vessel counts as inside its primary shield.**
- **Real dose and effects** (decided with Tim in the 8d plan).

Sources:
- **The outage:** a manipulator crane over the flooded cavity, the transfer system, and a pool with racks and its own cooling
  (Westinghouse) *(textbook)*.
- **Effects:** acute radiation syndrome from about 1 Gy, LD50/60 about 4-5 Gy, about 8 Gy near-certainly lethal; the
  occupational limit is 20 mSv a year (ICRP) *(textbook)*.
- **Shielding:** half-value layers for about 1 MeV gamma, about 10 cm in water and 5 cm in concrete *(textbook)*.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

## Numbers *(proposed unless marked)*
**Moves:**
- **60 s a move** (compressed; real moves are about 1-2 h).
- **REFUEL** is 9 moves (about 10 minutes): 3 to the pool, the 3-move shuffle, 3 fresh from the hopper.
- **DEFUEL** is 9 moves to the pool. Defuelling a damaged core clears its damage; the RCCAs stay in the vessel.

**The pool:**
- **Size:** 6 × 6 m and 12 m deep, about 430 t of water, with racks for 36 assemblies.
- **Build:** its controller digs it in front of itself, lines it with concrete and fills it.
- **Cooling:** a 75 kW, 480 V pump and an exchanger, UA **80 kW/°C**, on up to **100 kg/s** of circulating water.
  - At about 0.8 MW of decay heat it holds the pool near 35 °C.

**Decay heat in the pool:**
- Each assembly carries the core's decay groups (8d) from its discharge, and goes on decaying in the pool.
- **Without cooling** the pool heats (1.8 GJ/°C) to 100 °C and boils off at 2.26 MJ/kg.
  - As it falls it loses its top layers of water.
  - Make-up water comes off a pump.

**Radiation sources:**
- **An irradiated assembly:** 1,000 Sv/h at 1 m per MW of its decay heat. A day after discharge (about 0.1 MW) that is about
  100 Sv/h.
- **The vessel at power:** 10 mSv/h at 1 m × the power (inside its primary shield).
- **A damaged core:** 1 Sv/h at 1 m until it is defuelled.
- **Ore:** 0.002 µSv/h per scintillometer count a second. Pitchblende at 1 m is about 60 µSv/h.

**Shielding:** along the line from the source to you, water ×1/300 a block and any other full block ×1/1000, with inverse square
from 1 m. 3 m of water over the fuel gives about 2.7×10⁷, so a day-old assembly reads about 4 µSv/h at the surface.

**Dose:**
- **Kept per player:** a lifetime dose, and an acute dose that halves every 24 h of real time *(proposed)*.
- **Acute effects:**
  - **≥ 1 Sv:** bouts of nausea and weakness;
  - **≥ 3 Sv:** also hunger and slowness;
  - **≥ 6 Sv:** wither;
  - **≥ 8 Sv:** death by radiation.
  - Compressed: real deaths take days to weeks.
- **Dying clears the acute dose.**

**The dosimeter:**
- A click reads the rate and the dose.
- In the hotbar it alarms over **1 mSv/h** or **20 mSv**.

## Parts (each drawn facing north)
| Part | Cells | Real basis |
|---|---|---|
| `refuelling_machine` | 4 x 4 x 4 | The manipulator crane's bridge, trolley and mast, against the vessel.<br>• New assemblies by a hopper on top.<br>• Sneak-click cycles REFUEL, DEFUEL, STOP.<br>• It works only with the vessel cold, depressurized and its pumps stopped.<br>• **30 kW at 480 V**. |
| `fuel_transfer_system` | 2 x 2 x 6 | The upender, carriage and transfer tube, touching the machine and the pool's controller. |
| `spent_fuel_pool` | 4 x 3 x 4 | The pool's cooling pump, exchanger and cleanup skid; **the pool's controller**.<br>• The 6 × 6 pool lies just in front of it.<br>• Circulating water in on its west face, out on its east.<br>• Make-up water off a pump.<br>• **75 kW at 480 V**. |
| item `dosimeter` | | An electronic personal dosimeter. |

**The renderer** draws the racks at the pool's floor and each assembly in them, with a blue Cherenkov glow while its decay heat
is high.

## Game test
`fuelHandling`:
- **Setup:** a vessel at cold shutdown with a burned core; the machine, transfer system and pool; a diesel set.
- **REFUEL** (moves sped up): 3 records in the pool; 3 fresh assemblies and 6 burned in the core.
- **Dose:** at the pool's edge it is low. With the pool drained by a hook, the dose over it climbs and the effects apply.
- **DEFUEL:** clears a damaged core.
- **Screenshot:** the pool's racks.
