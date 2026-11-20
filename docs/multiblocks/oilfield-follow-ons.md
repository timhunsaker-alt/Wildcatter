# Oilfield follow-ons: the fracture gradient and lost circulation, oil-based mud, the choke manifold, salt caverns and gas storage, Frasch sulfur

> **DRAFT spec (2026-11-19), Wave B step 1: for Tim's review, nothing built.** Questions for Tim are at the end and in
> `docs/questions/oilfield-follow-ons.md`. Everything not asked there is decided on the best recommendation and says so. Proposed
> phase **23** (four waves, 23a-23d); the number is only a placeholder until Tim schedules it.

Phase 15 built salt domes with flank fields (salt-dome-oil.md), the mud plant, weighted mud, kicks, the BOP and blowouts
(drilling-mud.md), and Phase 20a a brine wellhead that solution-mines a dome for the chlor-alkali plant (chlor-alkali-plant.md). Each
left the same things out, now in ROADMAP's Ideas as "Phase 15 follow-ons":
- the **fracture gradient** (a mud too heavy for the casing shoe) and **lost circulation**;
- **oil-based muds**;
- **a choke manifold worked by hand** (today a kill is one minute of circulation; SICP and the choke were not built);
- **brine wells and gas storage caverns** in the salt domes (the brine well exists; the cavern it leaves has no use);
- **Frasch sulfur** from the caprock (today native sulfur is only mined as ore).

They belong together: the fracture gradient sets the **MAASP** the choke operator must stay under; oil-based mud changes how a kick
shows; a gas storage cavern is a brine well taken to its end. The order below follows those links.

## Real counterpart and sources
- **Fracture gradient and the shoe.** The weakest point of an open hole is usually just below the last casing shoe; a **leak-off test
  (LOT)** after drilling out the shoe measures it as an equivalent mud weight; **MAASP = (FG - MW) x 0.052 x shoe TVD** (psi, ppg, ft);
  **ECD** (equivalent circulating density) is the mud weight plus annular friction while pumping, a few tenths of a ppg on a land rig;
  Gulf Coast fracture gradients run about **0.7-0.9 psi/ft** (13.5-17.3 ppg), narrowing the window in geopressure *(textbook,
  unverified; IADC well control practice)*. Intermediate casing is set at the top of geopressure so the shoe will hold kill-weight
  mud *(textbook, unverified)*.
- **Lost circulation** is treated with **LCM** pills (sized calcium carbonate, graphite, fibres), reduced mud weight, or cement; losses
  and a kick at once can become an **underground blowout** *(textbook, unverified)*.
- **Oil-based (invert emulsion) mud:** diesel or mineral oil continuous, an internal **calcium chloride brine** (about 25-30 % CaCl2),
  oil-water ratios of **70/30 to 90/10**, emulsifiers, lime, organophilic clay, barite. It does not dissolve salt, inhibits shale,
  lubricates; **gas is soluble in it**, so a kick shows less pit gain at first and breaks out near the surface *(textbook,
  unverified)*.
- **Well control with a choke:** the **wait-and-weight** method (kill mud pumped in one circulation, drill pipe pressure held from
  **ICP = SIDPP + slow circulating rate pressure** down to **FCP = SCR pressure x KMW / OMW** as the kill mud reaches the bit, then
  held at FCP) and the **driller's method** (two circulations); the choke operator holds drill pipe pressure, not casing pressure; a
  choke move reaches the drill pipe gauge after about **2 s per 1,000 ft** *(textbook, unverified; IADC WellSharp syllabus)*. Gas
  goes through a **mud-gas separator** ("poor boy degasser") to a flare *(textbook, unverified)*.
- **Salt cavern gas storage.** Caverns are leached by fresh water down one string and brine up another, with a **blanket** (diesel or
  nitrogen) to keep the roof from dissolving; shapes are checked by **sonar surveys**; operating pressure at the casing shoe from
  about **0.3-0.35 psi/ft** minimum (the cushion gas holds it) to **0.8-0.85 psi/ft** maximum (Alabama's rule caps it at 0.85)
  ([summary of USPTO 9656807, *Hydrogen cavern pad gas management*](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/9656807);
  [Bérest et al., *Maximum admissible pressure in salt caverns*, OGST 2020](https://ogst.ifpenergiesnouvelles.fr/articles/ogst/abs/2020/01/ogst200180/ogst200180.html);
  [Alabama Admin. Code ch. 400-6-5](https://admincode.legislature.state.al.us/api/chapter/400-6-5)). Withdrawn gas is wet and goes
  through **glycol (TEG) dehydration** *(textbook, unverified)*. A cavern leached too close to a dome's edge failed at Bayou Corne,
  Louisiana, in 2012, opening a sinkhole *(textbook, unverified)*.
- **Frasch process.** Superheated water at **165 C and 2.5-3 MPa** down the outer of **three concentric pipes**, molten sulfur up the
  middle, hot compressed air down the inner to froth it up; **3-38 m3 of water a tonne** of sulfur; **99.5-99.8 %** sulfur; deposits
  **50-800 m** deep; first molten sulfur on 24 December 1894 at Sulphur Mines, Louisiana; made economic by Spindletop's cheap fuel
  from 1901; the last US mine closed in 2000; still used in Mexico, Ukraine and Poland
  ([Wikipedia, *Frasch process*](https://en.wikipedia.org/wiki/Frasch_process)). The injected water has to leave through **bleed
  water wells** *(textbook, unverified)*.

## Numbers *(proposed unless marked)*
The game's holes are short (a deep field at about 100-130 m), so pressures read small, as Phase 15's SIDPP already does (about 80 psi
at 96 m). Gradients are kept as equivalent mud weights (ppg), which do not depend on depth.
- **Fracture gradient (FG), as an equivalent mud weight:**
  - open hole above a deep field, under only the surface casing: **14.0 ppg** (0.73 psi/ft);
  - at an **intermediate casing shoe** set within **12 blocks above a deep field's top** (its transition zone): **15.4 ppg** (0.80
    psi/ft);
  - inside a deep field itself: 15.8 ppg (never the weakest point below a good shoe).
  So kill-weight mud (13.8 ppg) plus drilling ECD (+0.3) is 14.1 ppg: **it breaks down an open hole under surface casing only**. A
  deep field must be drilled from an intermediate shoe, as real geopressured wells are. That is the new requirement this spec adds.
- **ECD:** mud weight **+0.3 ppg** at drilling rate, **+0.1 ppg** at the slow circulating rate (SCR), 0 static.
- **Losses:** pressure at the weakest point over its FG: **partial losses**, up to 1 m3 a minute; more than 0.5 ppg over: **total
  losses**, no returns, the mud column falling 1 block a minute.
- **The kill:** the rig's pumps at SCR (30 strokes a minute, 0.1 bbl a stroke: 3 bbl/min, about 0.48 m3/min). A game hole of 100 m
  holds about 3.4 m3 (drill pipe 0.75, annulus 2.65): **about 7 minutes a circulation** at SCR *(derived)*.
- **Oil-based mud:** 80/20 oil-water ratio, the water phase 28 % CaCl2 (SG 1.27); base about **7.7 ppg** before barite *(derived)*;
  the land rig's 60 m3 system holds **48 m3 of diesel**. Weighted with barite by Phase 15's formula.
- **Caverns:** salt removed at **312 kg a m3 of saturated brine**; at the lease injection pump's 10 L/s (lease.md), **3.1 kg/s, about
  5 rock salt blocks an hour**. Wall: at least **2 blocks of salt** to the dome's flank and **3 blocks** under the caprock.
- **Gas storage** *(question 1)*: the pressure window of **0.3 to 0.85 psi/ft at a nominal shoe depth of 1,000 m** (real Gulf Coast
  caverns lie 600-1,800 m): **68 to 193 bar**. Working gas about **125 Sm3 a m3** of cavern, cushion gas about 70 Sm3 a m3 *(derived,
  ideal gas with z of about 0.9)*. A 2,000 m3 cavern (400 hours of leaching at 1:1) holds about **250,000 Sm3, 9.5 TJ** of working
  gas: a 1 MW gas set for about 40 days of play.
- **Frasch:** **2 t of sulfur an hour** a well; **20 m3/h** of water at 165 C (10 m3 a tonne); heating it from 20 C takes about
  **3.4 MW** of gas *(derived: 620 kJ/kg)*; lift air about 3 Nm3/min *(unverified)*. Each native sulfur block within 6 blocks of the hole
  in the caprock gives **1.7 t** of sulfur (80 % of the mod's 2,160 kg block) and becomes barren calcite.

## The parts
### 23a: casing, the leak-off test, lost circulation, oil-based mud
| Part or item | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `casing_joint` (item) | | **New.** A 9 m joint of 9-5/8 in intermediate casing: steel. One a 9 blocks of casing run. |
| `oilwell_cement` (item, bag) | | **New.** A 1 t bag of class H oilwell cement, crafted from a quicklime bag (alumina-plant.md), clay and phosphogypsum (fertilizer.md) *(stand-in for a cement kiln, Left out)*. Four bags cement a string. |
| `rig_substructure` | | **Existing.** The console gains **SET CASING** (runs and cements casing from surface to the bit: joints and cement by a hopper on the pipe rack; 1 minute a joint, then 10 minutes waiting on cement) and **LOT** (after 3 m of new hole: pumps slowly against the closed annular and reads the leak-off pressure, so the shoe's FG and the MAASP show; until then **FG NOT TESTED**). |
| `lcm_bag` (item) | | **New.** 1 t of sized calcium carbonate: calcite (or the caprock's) ground in the **barite mill** (a second recipe) *(decided: the mill is the real kind of mill for it)*. Through the mud hopper during partial losses: the losses stop within a minute if the excess is under 0.5 ppg, and that shoe holds 0.5 ppg more after *(proposed: wellbore strengthening)*. |
| `calcium_chloride_unit` | 2 x 4 x 3 | **New.** A reactor and steam evaporator: hydrochloric acid (32 %, chlor-alkali-plant.md) on calcite or limestone by hopper, CaCO3 + 2 HCl -> CaCl2 + H2O + CO2, evaporated to **77 % CaCl2 flake**, **0.5 t/h** *(proposed)*. **15 kW at industrial LV**; process steam on a pipe; CO2 counted. Bags through a bagging station against it. |
| `obm_package` (item, bag) | | **New.** 1 t of emulsifier, organophilic clay and lime for oil-based mud: made in the **bentonite dryer** (a second recipe) from bentonite, a quicklime bag and ammonia *(proposed stand-in: real organoclay is bentonite exchanged with quaternary amines made from fatty acids)*. |
| `mud_tanks`, `mud_hopper` | | **Existing.** The tanks take **diesel** (refinery product) on a pipe in place of water once switched to **OIL MUD** (sneak-click on the tanks, with them empty); **DUMP** empties them to the reserve pit (counted, lost). The hopper takes CaCl2 and OBM package bags. Readout: "OIL MUD 80/20, 28 % CaCl2, 13.9 ppg, ES ok". |

### 23b: the choke manifold
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `choke_manifold` | 2 x 2 x 4 | **New.** A 5,000 psi choke manifold: gate valves, a **hydraulic adjustable drilling choke** (remote) and a **manual adjustable choke**, pressure gauges and a buffer chamber. Within 12 blocks of the substructure, joined to the BOP stack's choke line by steel pipe. |
| `remote_choke_panel` | 1 x 2 x 1 | **New.** The choke console on the rig floor: choke position, drill pipe and casing gauges, the stroke counter, the pump rate, and a 0.2 kW hydraulic pump at residential voltage *(proposed)*. Its screen is where the kill is worked. |
| `mud_gas_separator` | 1 x 6 x 1 | **New.** A vertical atmospheric separator, about 0.9 m across and 6 m tall, with a liquid seal: mud back to the tanks, gas up a vent line by steel pipe to a flare (the lease's flare, lease.md). Against the manifold. |

### 23c: salt caverns and gas storage
| Part or item | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `brine_wellhead` | 1 | **Existing** (20a). Gains the cavern's record (volume, height, width, salt to the flank and to the caprock), a **blanket** (diesel or nitrogen on a pipe to its annulus: with it the cavern grows outward first; without, upward) and brine to a **disposal wellhead** (lease.md, which now takes brine) as well as to the chlor-alkali plant. |
| `sonar_tool` (item) | | **New.** A sonar survey tool: used on a brine or storage wellhead for 30 s, it outlines the cavern in particles and reads its volume and its walls. |
| `storage_wellhead` | 1 | **New.** A gas storage wellhead with its subsurface safety valve, set on a brine well's hole when leaching ends. **Debrining:** gas pushed in drives the brine up an inner string to disposal until the cavern is dry; then it stores gas. Readout: pressure (bar, psi), working and cushion gas (Sm3, GJ), cavern volume. |
| `storage_compressor` | 3 x 3 x 6 | **New.** An electric-driven two-stage reciprocating compressor, **5 Sm3/s**, **1.0 MW at industrial MV** *(derived: about 190 kJ a Sm3 at a ratio of about 3, 85 % efficient)*. Takes pipeline gas off steel pipe and puts it in a storage wellhead. |
| `glycol_dehydrator` | 2 x 6 x 3 | **New.** A TEG contactor and its gas-fired reboiler (**0.1 MW** of gas, **15 kW at industrial LV** *(proposed)*): withdrawn gas dried to pipeline spec. Withdrawal, up to 5 Sm3/s, goes only through it; without one the wellhead will not flow (**WET GAS**). Its outlet on steel pipe: to a receipt meter station (sales-gas.md), a gas plant or a set. |

### 23d: Frasch sulfur
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `frasch_water_heater` | 4 x 6 x 8 | **New.** A pressurised hot water plant: water from a water pump, softened, heated to **165 C at 2.8 MPa** by gas burners (**3.4 MW** at full rate), and the charge pump (**75 kW at industrial LV**, as the lease's H-pump). Hot water out on steel pipe. |
| `lift_air_compressor` | 2 x 3 x 4 | **New.** A rotary screw compressor for the lift air, **110 kW at industrial LV** *(proposed)*, joined by steel pipe. |
| `frasch_wellhead` | 1 | **New.** On a hole the rig drilled into a dome's caprock: three concentric strings, hot water down the outer, air down the inner, **molten sulfur** up the middle onto steel pipe (the acid plant's molten sulfur product, acid-plant.md, so it can go straight to the sulfur furnace or melter). |
| `sulfur_vat` | 6 x 2 x 6 | **New.** A pour vat: molten sulfur set solid, broken out as **sulfur** to a hopper under it (the item the acid plant takes). |

**Bleed water:** the water put down has to come out. A disposal wellhead on another hole into the same caprock within 16 blocks takes
it (as a real bleed water well); without one the well's rate falls to a third in 10 minutes and holds there *(proposed: the formation
fills)*.

## Layout
### At the rig (land-rig.md's layout; plan view, the substructure's hole at 0,0)
Layer y 0:
```
 ... T T T H H         T mud tanks    H mud hopper    K BOP stack (under the floor, as built)
 ... T T T H H         C choke manifold, within 12 blocks, piped from K's choke line
     C C . . .         G mud-gas separator against C (1 x 6 x 1); its vent by pipe to a flare F
     C C G             P remote choke panel on the rig floor (y 7, beside the console)
 ... F
```
### A storage cavern (plan view at the surface over a dome)
```
 W . . Q Q Q Q Q Q . D D        W storage wellhead (on the former brine well's hole)
 . . . Q Q Q Q Q Q . D D        Q storage compressor (6 long)   D glycol dehydrator (y 0-5)
                                 the cavern below W, in the salt; a disposal wellhead for brine during debrining
```
### Frasch (plan view over a dome's caprock)
```
 X . . A A A A . . V V V V V V      X Frasch wellhead (on a hole into the caprock)   A lift air compressor
 . . . . . . . . . V V V V V V      V sulfur vat (6 x 6)    H Frasch water heater (8 long)
 H H H H H H H H . . . . . . .      B a disposal wellhead for bleed water within 16 blocks of X
 . . . . . . . . . . . . B . .
```
The grids are sketches; the game tests are the layout of record.

### Legend
| Char | Block id |
|---|---|
| `T` | `wildcatter:mud_tanks` |
| `H` (rig) | `wildcatter:mud_hopper` |
| `K` | `wildcatter:bop_stack` |
| `C` | `wildcatter:choke_manifold` |
| `G` | `wildcatter:mud_gas_separator` |
| `P` | `wildcatter:remote_choke_panel` |
| `F` | `wildcatter:flare_stack` (existing, lease.md) |
| `W` | `wildcatter:storage_wellhead` |
| `Q` | `wildcatter:storage_compressor` |
| `D` | `wildcatter:glycol_dehydrator` |
| `X` | `wildcatter:frasch_wellhead` |
| `A` | `wildcatter:lift_air_compressor` |
| `V` | `wildcatter:sulfur_vat` |
| `H` (Frasch) | `wildcatter:frasch_water_heater` |
| `B` | `wildcatter:disposal_wellhead` (existing) |

## Controllers, rotation, forming, removing
- **The rig:** `rig_substructure` stays the controller. It finds the choke manifold by the pipe from its BOP stack's choke line, the
  separator against the manifold, the panel on its floor. Without a manifold, with the rule `chokeKill` on, the rig reads **NO
  CHOKE** and will not drill into a deep field (as NO BOP). Removing the separator mid-kill sends gas to the floor (below).
- **The cavern:** each wellhead is its own controller; the compressor and dehydrator find it by pipe. Breaking a storage wellhead
  shuts the cavern in (the safety valve closes); its gas stays in the world's saved data for that hole.
- **Frasch:** the wellhead is the controller; it finds the heater and compressor by pipe, the vat by pipe, the bleed well by distance.
- Any rotation, all of them.

## How it runs
### 23a: the shoe, the LOT, losses and oil-based mud
- **Casing:** SET CASING runs casing from the surface to the bit (a joint a 9 blocks, four cement bags) and makes the bit's depth the
  **shoe**. Its FG is 15.4 ppg if the shoe is within 12 blocks above a deep field's top, else 14.0. A rig with no intermediate casing
  has only its surface casing (FG 14.0 below it). The player learns where to set it from the seismograph's DEEP ECHO depth.
- **LOT:** reads the shoe's FG (to 0.1 ppg) and shows **MAASP** in psi for the mud in the hole; recomputed as the mud changes.
- **Losses:** each solve the rig compares the pressure at the shoe (static, ECD, or with choke pressure in a kill) with the FG:
  **LOST CIRCULATION: PARTIAL** (pits falling) or **TOTAL** (no returns, the column falling). The mud lost is gone, oil mud's diesel
  with it. An LCM bag through the hopper cures partial losses as above; total losses need the weight brought down (water or base
  oil added) or the hole cased.
- **Losses and a kick at once** (a deep field kicking while the shoe takes mud): **UNDERGROUND BLOWOUT**: the field drains at the
  blowout rate into the shoe's zone, nothing at surface, no fire; killed by a relief well (drilling-mud.md) *(decided: the same cure as a
  surface blowout; cratering left out)*.
- **Oil-based mud:** no WASHOUT in rock salt (oil does not dissolve it), never LOW GEL (the organoclay), **25 % faster** drilling
  (lubricity) *(proposed)*. Mud losses per metre are whole mud as now, so each metre drilled takes about 64 L of diesel. A kick in
  oil mud shows **a third of the pit gain** until the influx is within 20 blocks of the surface, then all of it at once (the gas
  breaking out) *(question 8)*. Oily cuttings are counted on the rig's readout and go nowhere *(decided; a cuttings dryer is Left out)*.

### 23b: the kill through the choke
With a kick shut in (as Phase 15), the panel reads **SIDPP** and **SICP**, the pit gain, the shoe's MAASP, and builds a **kill sheet**:
kill weight, ICP, FCP, strokes surface to bit, bit to shoe, shoe to surface, and the drill pipe schedule as a line on a chart.
- **Weight up** the tanks to kill weight (the hopper, as now). **Wait and weight** is the default; the **driller's method** is a
  switch (first circulation on the old mud, holding ICP throughout).
- **Bring the pump up** to SCR on the panel while opening the choke so casing pressure holds at SICP; then **hold drill pipe pressure
  on the schedule** by moving the choke (drag or hold the keys; the hydraulic choke moves 10 % a second, the manual one 2 % a second
  by its handwheel) *(proposed)*.
- **The model** *(for the plan)*: bottomhole pressure = annulus hydrostatic (mud and influx) + annular friction + choke back pressure;
  the choke's back pressure goes with flow squared over its opening squared; gas rises at the circulation rate and expands, so casing
  pressure climbs as it nears the surface and drill pipe pressure is what tells the truth. Drill pipe pressure answers a choke move
  after **2 s per 1,000 ft** of hole.
- **Too little back pressure** (BHP under pore pressure): more influx, the pit gains, the kill sheet's schedule no longer holds (a
  second kick, shut in and start again). **Too much** (pressure at the shoe over FG): losses, as 23a; over FG with influx in the
  hole: the underground blowout.
- **Gas at surface** goes through the separator to the flare. Gas over the separator's rating (its liquid seal blown, a few hundred
  psi of casing pressure on a fast choke opening *(proposed)*): **GAS ON THE FLOOR**, ignited as a blowout is (fire, lava or a campfire
  within 16 blocks).
- **Killed:** kill mud back at surface, the choke full open, both gauges at zero, a 1-minute flow check: **WELL DEAD**, and drilling
  resumes (the strike, as now).
- A circulation at SCR takes about 7 minutes; the whole kill about 10.
- **PLC points:** CHOKE POSITION (4-20 mA command and feedback), DRILL PIPE PRESSURE and CASING PRESSURE (4-20 mA, 0-1,000 psi),
  STROKES (an HSC count), PUMP RATE; a PLC program may hold the schedule with a PID, as a real automatic choke does.
- **Rule `chokeKill`** (new world rule): "a kick is killed through a choke manifold by hand or by a PLC". On in new worlds, off in
  existing ones; off, the Phase 15 one-minute circulation stays *(question 7)*.

### 23c: leaching, debrining, storing
- **Leaching:** as 20a, faster with the injection pump's full 10 L/s when the brine goes to disposal; rock salt blocks removed
  outward from the hole's bottom with a blanket, upward without. It stops at the walls (2 blocks to the flank, 3 under the caprock),
  reading **CAVERN AT LIMIT**. The cavern is the removed blocks, filled with brine (water), saved per hole.
- **Breach** *(question 2)*: world rule **`cavernStability`**: leaching past the limits, or a cavern holding gas under its minimum
  pressure (0.3 psi/ft) for over a game week, **breaches**: a flank breach floods it with formation water and the gas is lost; a roof
  breach opens a **sinkhole** at the surface over it (the blocks above the cavern drop to fill it, water pools). The sonar tool warns:
  under 3 blocks of wall it reads **WALL THIN**; a roof heaving shows cracks (particles) at the surface for a game day before a
  collapse. Off: leaching just stops at the limits and pressure has no floor.
- **Debrining:** the storage wellhead on the hole, the compressor on pipeline gas: gas in, brine out to disposal at the same volume,
  until **DRY**. The first gas in is **cushion gas** (to the minimum pressure, not withdrawable); after it, **working gas**.
- **Injection** (compressor running) and **withdrawal** (through the dehydrator) each up to 5 Sm3/s, withdrawal falling as pressure
  nears the minimum *(proposed: in proportion to the pressure above the minimum, full above half the window)*. Over the maximum the
  compressor stops (**HIGH PRESSURE**).
- **PLC points** on the storage wellhead: INJECT, WITHDRAW, SHUT IN (commands); PRESSURE and WORKING GAS (4-20 mA); HIGH PRESSURE, LOW
  PRESSURE, WET GAS (alarms).
- **Only gas** is stored *(question 3)*.

### 23d: Frasch
- The heater's water down, the air on, the wellhead melts sulfur within 6 blocks in the caprock, nearest first, and lifts it as
  molten sulfur at up to 2 t/h; each block taken turns to barren calcite. **NO HOT WATER** under 150 C, **NO AIR**, **NO BLEED** (the
  rate at a third), **CAPROCK SPENT** when no sulfur is in reach.
- A dome's caprock (4 blocks thick, 30 % sulfur) holds about 100-200 sulfur blocks in a well's reach: **170-340 t**, a few days of a
  well at 1:1. More wells, more of the cap.
- Molten sulfur on pipe goes to the acid plant (its melter or furnace) or to a vat for solid sulfur.

## What it adds to the grid
| Wave | Loads | Tier (from the world's standard) |
|---|---|---|
| 23a | calcium chloride unit 15 kW | industrial LV |
| 23b | choke panel 0.2 kW | residential |
| 23c | storage compressor **1.0 MW**; dehydrator 15 kW; injection pump 75 kW (existing) | industrial MV; industrial LV |
| 23d | water heater's charge pump 75 kW; lift air compressor 110 kW | industrial LV |
Gas: the dehydrator's reboiler 0.1 MW; the Frasch heater **3.4 MW** of gas a well at full rate (Frasch was a big gas user, as it
was at Spindletop).

## Recipes and progression
- **23a** sits in the oil-and-gas era with the land rig and the mud plant; oil mud needs the refinery's diesel and the chlor-alkali
  plant's HCl (or rock salt brine) for its CaCl2, so it comes after `chemicals`. Casing joints: steel. Cement: quicklime, clay,
  phosphogypsum (or gypsum, if the player has none) *(decided: a gypsum-free variant with plain clay doubled, so the casing does not
  wait for the fertilizer plant)*.
- **23b:** the manifold and separator from steel, pipe and valves; the panel from the PLC electronics. Available with the BOP stack.
- **23c:** the storage wellhead from steel and a valve; the compressor from steel and an MV motor; the dehydrator from steel, pipe
  and a burner. After the sales line (14b).
- **23d:** the heater and vat from steel, brick and burners; the compressor from steel and a motor. Available with the acid plant.
- **Quest book** (`oil_and_gas`, Wildcatter's chapter 30, new quests after "The lease"; each may require quests in later chapters):
  *A casing point* (milestone `casing_set`), *A leak-off test* (`lot_done`), *Kill a kick through the choke* (`well_killed_choke`),
  *Oil mud* (a land rig drilling on oil mud, `oil_mud_drilling`), *A storage cavern* (`cavern_dry`, requires the sales line quest),
  *Frasch sulfur* (`frasch_flowing`, in `chemicals` after the acid plant).

## Sounds and art
- **Models:** `tools/models/choke.py` (manifold, panel, separator), `tools/models/cavern.py` (storage wellhead, compressor, dehydrator),
  `tools/models/frasch.py` (heater, compressor, wellhead, vat), the CaCl2 unit; items casing joint, cement bag, LCM bag, CaCl2 bag, OBM
  package bag, sonar tool. **Ludo** textures for the items and the vat's sulfur; script-drawn ones named `*_placeholder` until then.
- **Sounds:** the choke's hiss rising with flow, the hydraulic pump, gas at the separator (a roar at the flare), the drill pipe gauge's
  needle tick (subtle); the compressor's reciprocating thump; the dehydrator's reboiler burner; the Frasch heater's burners and steam
  hammer, molten sulfur pouring into the vat; a breach's rumble and the sinkhole's collapse; a sonar ping while surveying.
- **Visuals:** the separator's flare; the sonar outline; surface cracks before a roof breach; yellow molten sulfur in the vat.

## Game tests and client scenes
NeoForge game scenes:
- `casing_lot` (23a): a land rig over a forced deep field: weighted to 13.8 with only surface casing, it reads LOST CIRCULATION when
  drilling (14.1 over 14.0); SET CASING 6 blocks above the field (joints and cement by hook), LOT reads 15.4 and a MAASP; it drills in at
  13.8 without losses; at 16.0 it loses mud (TOTAL); an LCM bag cures a partial loss.
- `oil_mud` (23a): the CaCl2 unit fills a bag; tanks switched to OIL MUD, diesel by pipe, bags by hook: 80/20 and a weight readout; a
  hole through placed rock salt drills with no WASHOUT and no LOW GEL; a kick in oil mud alarms later than in water mud.
- `choke_kill` (23b): a shut-in kick, the kill sheet's ICP and FCP against hand figures; the choke held on schedule by a scripted
  operator (a PLC PID in the scene) kills the well in under 12 minutes; the same with the choke held too far closed: LOST CIRCULATION at
  the shoe; too far open: a second kick; NO CHOKE with the manifold removed (rule on); rule off, the old kill.
- `gas_storage` (23c): a brine well leaches a placed salt column to its limit (by hook for speed); the sonar reads the volume; debrining
  sends brine to a disposal well; the compressor fills cushion then working gas; withdrawal through the dehydrator feeds a gas set;
  WET GAS without it; with `cavernStability` on, leaching past the wall breaches it.
- `frasch` (23d): a placed caprock with sulfur blocks: the wellhead lifts molten sulfur to the acid plant's melter and to a vat; sulfur
  blocks in reach turn to calcite; NO BLEED cuts the rate to a third.
- `world_rules` gains `chokeKill` and `cavernStability`. Unit tests: `WellControlTest` (kill sheet, MAASP, ECD), `CavernTest`
  (pressure window, gas in place).

Client scenes (filmed, per the house rule): **`chokeKill`** (the panel's chart while the choke is worked, gas to the flare),
**`saltCavern`** (a cut through the dome showing the cavern growing, the storage plant), **`frasch`** (sulfur pouring into the vat),
**`lostCirculation`** (the pits falling on the console).

## Proposed waves
- **23a:** casing, the LOT, the fracture gradient, losses and LCM, oil-based mud and the CaCl2 unit.
- **23b:** the choke manifold, panel, separator and the hand kill (the rule `chokeKill`).
- **23c:** leaching to disposal, the blanket, the sonar tool, debrining, gas storage, breaches (the rule `cavernStability`).
- **23d:** Frasch sulfur.

## Code that changes *(for the plan, not a design decision)*
- `RigSubstructureBlockEntity`: shoe depth and FG, the LOT, ECD, losses, oil mud, the kill's hydraulics (a small well model with the
  influx as a gas slug), NO CHOKE. `MudTanks`: a mud type.
- The brine wellhead's cavern record (`CavernData`, saved per hole); `SaltDome` gives the walls.
- `Rules`: `CHOKE_KILL`, `CAVERN_STABILITY`. `Tuning` ("23a-23d"). `OreUses`: native sulfur names the Frasch wellhead.

## Left out
- Liquids storage in caverns (crude, LPG, brine displacement as the strategic reserves do) *(question 3)*.
- A cement kiln; cement bond logs; casing design beyond one intermediate string; liners.
- Cuttings handling for oil mud (a cuttings dryer, thermal desorption); synthetic-base muds; mud rheology beyond the thresholds.
- Hydrates and Joule-Thomson cooling on withdrawal; line pack on the sales line.
- Frasch subsidence and surface caving; bleed water treatment.
- Cratering and broaching in an underground blowout.
- Shale and wellbore stability beyond the fracture gradient.

## Questions for Tim
The same in `docs/questions/oilfield-follow-ons.md`, with why and the alternatives. Recommended option first.
1. **Cavern pressure:** as if at a real storage depth (1,000 m: 68-193 bar, about 125 Sm3 of working gas a m3) / the real gradient at
   the game's depth (about 7-20 bar, a tenth of the gas; no compressor really needed).
2. **Cavern breaches and sinkholes:** a world rule `cavernStability`, on in new worlds / warnings only, never a breach / left out.
3. **What caverns store:** gas only / gas and liquids (crude and LPG by brine displacement, with a brine pond).
4. **Frasch sulfur:** build it with bleed water / build it without bleed water / leave it out (sulfur stays mined as ore).
5. **Casing points:** SET CASING by the player, with joints and cement / casing set by the rig automatically above a deep field /
   no casing, one fixed shoe FG.
6. **Oil mud's cost:** diesel lost with the mud and oily cuttings only counted / a cuttings dryer required to drill on oil mud /
   oil mud as water mud with no salt washout and nothing more.
7. **The choke kill:** required by a new world rule `chokeKill`, on in new worlds / optional, the one-minute kill kept beside it /
   replaces the old kill in every world.
8. **A kick in oil mud:** shows late (a third of the pit gain until the gas breaks out) / shows as in water mud.
