# Drilling mud: barite, bentonite, weighted mud and well control

> **BUILT as waves 15b1 and 15b2 (2026-11-09), decided on recommendation while Tim was away; not approved.** See "As built" at the end
> and `docs/questions/phase15-oilfield.md`. Proposed phase **15b** (two waves, 15b1 and
> 15b2). Decisions are in `docs/questions/specs-wildcatter.md` (items 18-22).

Today the land rig's "mud" is water: 80 L a metre drilled, out of its 60 m3 mud tanks (land-rig.md). The 7a barite and bentonite have
no use. Real mud is water gelled with **bentonite** to carry cuttings and weighted with ground **barite** to hold back the pressure in
the rock. This spec adds a **mud plant** that grinds both into bulk bags, a **mixing hopper** on the rig, a **mud weight** in the
tanks, and what mud weight is for: **pore pressure, kicks, the blowout preventer, and blowouts**. It also closes the known gap "There
is no blowout preventer stack in the cellar yet" (PROGRESS.md).

Sources:
- API Specification 13A: drilling-grade barite at a minimum **SG 4.2**, and since 2010 also a **4.1** grade
  ([AADE paper on barite supply](https://www.aade.org/download_file/2255/424)); API 13A section 9 bentonite at a minimum yield of
  **90 bbl/ton** ([Pochteca data sheet](https://colombia.pochteca.net/wp-content/uploads/2021/09/bentonita-1.pdf); Wyoming product
  sheets give 98 ± 4).
- The weight-up formula, barite at 35 lb/gal: **lb of barite a bbl = 1,470 (W2 − W1) / (35 − W2)** *(textbook, unverified)*.
- Pore pressure: a normal gradient of **0.433-0.465 psi/ft** (fresh to salt water); Gulf Coast geopressure to **0.7-0.9 psi/ft**
  *(textbook, unverified)*.
- A land rig's BOP: a 13-5/8 in, 5,000 psi stack, an annular preventer over pipe and blind-shear rams, closed from an accumulator unit
  *(textbook, unverified)*.

## Numbers *(proposed unless marked)*
- **Mud weight**, in lb/gal (ppg) and kg/m3, kept by the mud tanks: water 8.34 ppg; bentonite gel at **25 lb/bbl (71 kg/m3)** about
  8.6 ppg; barite added by the formula.
- **Pore pressure** of a field, as an equivalent mud weight at its depth:
  - shallow fields and salt-dome flank fields: **normal, 8.34 ppg** (a fresh-water gradient). Plain water still drills them, so
    nothing that works today stops working;
  - **deep fields** (land-rig.md, y -58 to -50): **geopressured, 13.5 ppg** (0.70 psi/ft).
- **To drill into a deep field:** at least **13.8 ppg** (0.3 ppg over the pore pressure, a trip margin). From 8.6 ppg that is
  **360 lb/bbl, about 1,030 kg of barite a m3**; the 60 m3 system takes about **62 t of barite**, and each metre of new hole
  about 80 kg more.
- **Salt:** a hole through rock salt (salt-dome-oil.md) with fresh mud washes out unless the mud is **salt-saturated**: about
  **360 kg of rock salt a m3** (10.0 ppg brine), 22 t for the system *(derived from halite's solubility, unverified)*.
- **Bags:** 1 t bulk bags, as the alumina plant's (alumina-plant.md). A barite block (90 %) is about 3.8 t of barite *(derived from
  ores.md, unverified density)*: about 16 blocks weight up a land rig's system.

## The mud plant
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `barite_mill` | 3 x 6 x 4 | A roller mill with its classifier and baghouse, grinding barite to API 13A (97 % under 75 µm). **150 kW (200 hp) at 480 V** *(unverified)*, **5 t/h** *(proposed)*. Barite by a hopper on top. It is `Bagged`: a bagging station against it fills bags of **barite, API 13A 4.1**. At the game's 90 % ore the mill makes 4.1, not 4.2: the ore's gangue *(proposed)*. |
| `bentonite_dryer` | 3 x 4 x 8 | A rotary dryer and roller mill: bentonite to about 10 % moisture and 200 mesh. The dryer fires pipeline gas or LPG, **1 MW** *(proposed)*; the mill and fan **75 kW at 480 V** *(unverified)*. **3 t/h** *(proposed)*. `Bagged`: bags of **bentonite, API 13A section 9**. |
| `bagging_station` | | **Existing** (alumina-plant.md), against either. |

## At the rig
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `mud_hopper` | 2 x 3 x 3 | **New.** A jet mixing hopper and its centrifugal mixing pump, **37 kW (50 hp) at 480 V** *(unverified)*, against the mud tanks. Bags of barite or bentonite, or rock salt, by a hopper on top: **one bag a minute** *(proposed; real rigs mix 1-2 sacks a minute)*. |
| `bop_stack` | 2 x 4 x 2 (as built; was 2 x 5 x 2) | **New** (15b2). A 13-5/8 in, 5,000 psi stack in the substructure's BOP space under the rotary table: annular, pipe rams, blind-shear rams, the choke and kill lines. |
| `bop_accumulator` | 2 x 2 x 4 | **New** (15b2). The accumulator unit (bottles, a 480 V triplex pump of **15 kW** *(unverified)* and the remote panel), within 20 blocks of the stack, joined by a hydraulic line drawn as steel pipe. It holds pressure for **three closes** without power. |
| `mud_tanks` | 3 x 3 x 12 | **Existing.** Now keeps the mud's weight, its bentonite and salt, and its pit volume (the kick alarm watches it). |

## Layout
### The mud plant (facing north, plan view; the mill's main cell at 0,0,0)
Layer y 0:
```
x:  0 1 2 3 4 5 6 7 8 9 10
z0  M M M S S . D D D D ...      M barite mill (y 0-5)       S bagging station (against M)
z1  M M M S S . D D D D ...      D bentonite dryer (8 long, east; y 0-3), its bagging station on its south face
z2  M M M . . . D D D D ...
z3  M M M . . . . . . . .
```
### The rig (the land rig's layout, land-rig.md; plan view near the substructure)
Layer y 0:
```
 ... T T T H H          T mud tanks (x 6..8)     H mud hopper, against the tanks' east face
 ... T T T H H
 ... T T T . .          K BOP stack, in the substructure's BOP space under the rotary table (y 0-4)
     A A                A accumulator, within 20 blocks, on the ground
```
The grids are sketches; the game tests are the layout of record.

### Legend
| Char | Block id |
|---|---|
| `M` | `wildcatter:barite_mill` |
| `D` | `wildcatter:bentonite_dryer` |
| `S` | `wildcatter:bagging_station` |
| `T` | `wildcatter:mud_tanks` |
| `H` | `wildcatter:mud_hopper` |
| `K` | `wildcatter:bop_stack` |
| `A` | `wildcatter:bop_accumulator` |

## Controllers, rotation, forming, removing
- **Mud plant:** each mill is its own controller; the bagging station is the existing one. Any rotation.
- **Rig:** the land rig's `rig_substructure` stays the controller. It finds the mud hopper through the mud tanks it already finds, the
  BOP stack in its BOP space, and the accumulator by its hydraulic line.
- **Removing:** the hopper stops mixing (the mud keeps its weight); with the BOP stack or accumulator removed the rig reads **NO BOP**
  and, from 15b2, will not drill below the shallow fields' depth.

## How it runs
### 15b1: weight and salt
- The hopper mixes a bag a minute while its pump runs. The tanks' weight follows what is in them; drilling uses mud by the metre as
  today, and the water pump tops up water, which dilutes the weight.
- **Bentonite:** below 15 lb/bbl the cuttings settle when the pumps stop (**LOW GEL**): the rig's rate of penetration is halved
  *(proposed)*. Plain water still drills.
- **Salt:** a land rig drilling rock salt with unsaturated mud: **WASHOUT**. It stops and loses the metres drilled in salt (they fill
  back in) until the mud is saturated. The small rig is not affected *(proposed: its shallow hole is left simple)*.

### 15b2: pore pressure, kicks and blowouts
- **Underbalanced:** a land rig entering a deep field with mud under its pore pressure takes a **KICK**: the pits gain, an alarm
  sounds and the console reads **KICK: PIT GAIN n m3**.
- **Shutting in:** a sneak-click on the console's SHUT IN (or a PLC point) closes the annular and rams **within 30 s of the alarm**.
  The rig reads the shut-in pressures and the **kill weight**; once the mud reaches it, a circulation (one minute *proposed*) kills
  the well and drilling goes on.
- **Not shut in in time, or no BOP:** a **BLOWOUT**. The well flows wild at five times a free-flowing well's rate *(proposed)*:
  oil and gas round the rig floor, counted against the field. Gas within 16 blocks of a fire, lava, lightning or a running engine
  without a spark arrestor ignites: the rig burns. A blowout is killed by closing a BOP that still stands (if its accumulator has a
  close left), or by a **relief well**: a second rig, at least 20 blocks off, drilling into the same field with kill-weight mud.
- **PLC points:** KICK, SHUT IN (command), SHUT IN (state), a 4-20 mA mud weight and pit volume.

## Left out
- Oil-based and synthetic muds; mud rheology beyond the LOW GEL threshold; lost circulation.
- Casing design and the fracture gradient (a mud too heavy for the shoe).
- A choke manifold's manual control: the kill is one circulation.

## Game tests
- `mudPlant`: barite and bentonite by hopper; both mills fill bulk bags through bagging stations on a 480 V set.
- `landRig` (extended, 15b1): the mud hopper weights the tanks from 8.34 to over 13.8 ppg with bags given by hook; a hole into a
  placed rock-salt layer with fresh mud reads WASHOUT; with salt mixed it drills on.
- `wellControl` (15b2): a land rig with a BOP over a forced deep field at 8.6 ppg takes a KICK; a SHUT IN within 30 s holds it; weighted
  to the kill weight by hook, the circulation kills it and the rig strikes oil. The same with the shut-in left: BLOWOUT, the field's
  units fall fast, and a fire block next to the floor ignites the gas.
- **Screenshot:** the mud plant; the rig floor with the BOP stack under it.

## Proposed waves
- **15b1:** the mud plant, the hopper, mud weight, gel and salt.
- **15b2:** pore pressure, the BOP stack and accumulator, kicks, blowouts and relief wells.

## As built (15b1 and 15b2, 2026-11-09)
Decisions taken while building are in `docs/questions/phase15-oilfield.md` (items 11-25).
- **Parts** (`block/MudPartBlock.java`): `barite_mill` 3 x 6 x 4 and `bentonite_dryer` 3 x 4 x 8 (`MudMillBlockEntity`), `mud_hopper` 2 x 3 x 3
  (`MudHopperBlockEntity`), `bop_stack` 2 x 4 x 2 (`BopStackBlockEntity`), `bop_accumulator` 2 x 2 x 4 (`BopAccumulatorBlockEntity`). Motors on
  the world's industrial tier, boxes on the west face of the main cell. Models in `tools/models/mud.py`. Items: `barite_bag`, `bentonite_bag`
  (Ludo icons).
- **Layout of record** (the NeoForge scenes): the mud plant facing north, mill at x 4 z 0, its bagging station against its south side
  (x 4 z 4), the dryer at x 4 z 8 with its gas on the east of its bottom cells (x 7), its station at x 4 z 16; the cable down x 3 to each box.
  At the rig (`PlcPlantScenes.rig`'s layout, hole at 30, 0): the mud hopper's main cell at x 40 z 5 facing south (x 39-40, z 3-5), against the
  tanks' east face; the BOP stack's main cell on the cell above the hole; the accumulator within 20 blocks.
- **The mud:** water, barite, bentonite and salt in kg (decision 11); `mudPpg`, `gelLbBbl`, `lowGel`, `saltSaturated`; the readout "mud water
  N of 60 m3 at 8.34 ppg (999 kg/m3), gel 0 lb/bbl LOW GEL, fresh". 40 m3 of water and 42 bags reach 13.89 ppg; salt-saturated mud with
  25 lb/bbl gel reads 10.3 ppg.
- **15b1 rules:** LOW GEL halves the rate (decision 13); WASHOUT on any rock salt with unsaturated mud, the salt drilled at the bottom
  filling back in (decision 17); the hopper mixes a tonne a minute (decision 15).
- **15b2 rules:** a deep field needs a BOP stack and an accumulator (NO BOP), and kicks with mud under 13.8 ppg (decision 21); shut in by a
  sneak-click on the console or the PLC's SHUT IN within 30 s, else BLOWOUT; SIDPP and kill weight; one minute of circulation at kill
  weight kills it and the rig strikes (decision 22); blowouts drain the field at 7.5 units a tick, light from fire, lava or a lit campfire
  within 16 blocks, and are shut in by a standing BOP with a close left or killed by a relief well (decision 23).
- **PLC points** added to the rig: KICK, SHUT IN (command), SHUT IN (state), MUD WEIGHT 4-20 mA (8-20 ppg), PIT VOLUME 4-20 mA (0-60 m3).
- **Tests:** NeoForge scenes `mud_plant`, `rig_mud` (hopper a bag a minute, 8.34 to 13.89 ppg, LOW GEL, WASHOUT and through salt
  saturated), `well_control` (NO BOP, KICK, shut in, kill weight, kill, strike) and `blowout` (blowout, field drained, ignition, shut in by the
  BOP). The client `landRig` scene now sets a BOP and weights its mud (not run).
- **Not built:** the hydraulic line drawn between accumulator and stack (decision 20); lightning and running engines as ignition; a relief
  well game test (the kill is in code); SICP and the choke.

### After Tim's answers (2026-11-10, branch `w-rules`)
- **The world rule `mudGel` (answer 2):** on in new worlds, off in worlds saved before; off, no mud is LOW GEL and plain water drills at
  full rate (`RigSubstructureBlockEntity.lowGel`).
- **The world rule `wellControl`** (`docs/questions/world-rules.md` 3): on in new worlds, off in worlds saved before; off, a land rig
  drills into a deep field without a BOP and without a kick, and through rock salt without a washout, as before Phase 15.
