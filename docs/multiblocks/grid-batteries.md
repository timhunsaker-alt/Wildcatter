> **APPROVED by Tim 2026-11-19** (every question answered with the recommended option, `docs/questions/grid-batteries.md`).
> Phase **22** (waves 22a-22c). See "As built" at the end.

Wildcatter's grid today has only rotating sources: gensets, steam and gas turbines, the reactor. Nothing on it stores energy, and
the lithium plant (20c) makes hydroxide and carbonate with one small use (the potline bath). Graphite schist and cobaltite have no
use at all (`OreUses`). The idea was raised by the lithium spec (`docs/questions/specs-wildcatter.md` item 70) and listed in
`docs/ideas-plan.md` ("New Ideas these specs raised").

This spec adds the real thing: a **containerised lithium iron phosphate (LFP) battery energy storage system (BESS)**. Battery
containers hold the racks, their battery management system (BMS), liquid cooling and fire protection; a bidirectional **power
conversion system (PCS)** inverts DC to AC on the industrial LV tier; an **MV skid** steps it up to distribution; a **plant
controller** runs the site. It charges and discharges through the AC solver as a **grid-following** or a **grid-forming**
source: frequency response by droop, synthetic inertia, and **black start** of a dead island. It has a state of charge, a real
round-trip efficiency, degradation, PLC points, and (as an opt-in world rule) thermal runaway. A small **commercial cabinet** gives
480 V bases the same thing at 125 kW. The cells come from an **LFP cell plant** on the lithium plant's carbonate, the fertilizer
plant's phosphoric acid, iron and purified graphite.

## Real counterpart and sources
- **Containerised BESS.** Utility BESS are built from 20 ft containers of LFP racks, each about **3.7-5 MWh** in current products,
  with a **PCS** (bidirectional inverter) and a **medium-voltage transformer** skid per one or two containers; durations of **1 to
  4 hours** at full power are usual, and storage goes from standby to full power in **under a second**
  ([Wikipedia, *Battery energy storage system*](https://en.wikipedia.org/wiki/Battery_energy_storage_system); a 3.5 GWh project in
  624 containers is about 5.6 MWh a container, from that article's reference 49). The 2025 additions were "mostly LFP" (same).
- **Round-trip efficiency.** NREL's Annual Technology Baseline takes **85 %** AC-to-AC as representative, and assumes about **one
  cycle a day**; the US fleet averaged **82 %** monthly in 2019 (EIA)
  ([NREL ATB 2020, *Utility-scale battery storage*](https://atb-archive.nrel.gov/electricity/2020/index.php?t=st);
  [NREL ATB 2024](https://atb.nrel.gov/electricity/2024/utility-scale_battery_storage);
  [EIA, *Today in Energy* 46756](https://www.eia.gov/todayinEnergy/detail.php?id=46756)).
- **Degradation.** Faster at high charge rates and deep discharge; warranties cap lifetime throughput (Wikipedia, above). LFP cells
  are commonly rated **6,000-8,000 cycles to 70-80 % capacity** *(textbook, unverified)*.
- **Failures.** About **0.04 failures per GWh deployed** by 2025, mostly controls and balance of plant, about **11 % in the cells**
  (Wikipedia, above). Fire codes: NFPA 855 (installation, spacing about **3 ft / 0.9 m** between units unless large-scale tested) and
  the UL 9540A thermal-runaway test method *(textbook, unverified)*.
- **Grid codes.** Grid-following inverters lock to the grid's voltage with a phase-locked loop and inject current; grid-forming
  inverters hold their own voltage and frequency behind an impedance, as a machine does, and can energise a dead network. IEEE
  2800-2022 sets a primary frequency response **deadband of 36 mHz** and droop of **2-5 %** for inverter-based resources in North
  America *(textbook, unverified)*; ENTSO-E's FCR allows up to **10 mHz** of insensitivity *(textbook, unverified)*. Inverter fault
  current is limited to about **1.1-1.5 x rated** *(textbook, unverified)*, far below a machine's 5-6 x.
- **LFP cells.** 3.2 V nominal prismatic cells of **280-314 Ah**; **416 in series** (1,331 V nominal, about 1,160-1,500 V over the
  state of charge) make a container's DC string, in 8 modules of 52 cells a rack *(textbook, unverified)*. Materials, about: **0.55-0.7
  kg lithium carbonate equivalent a kWh**, **1.0-1.2 kg graphite a kWh**, **2.2 kg LiFePO4 a kWh** *(textbook, unverified)*.
- **LFP cathode:** iron phosphate precipitated from iron and phosphoric acid, mixed with lithium carbonate and a carbon source,
  calcined at about **700 C under nitrogen** *(textbook, unverified)*. **Anode graphite:** flake concentrate floated from schist,
  rounded (spheroidised), and purified to **99.95 % C** thermally at **2,400-3,000 C** or with HF *(textbook, unverified)*.

## Sizes *(decided on recommendation; question 1)*
| Size | Power | Energy | AC side | Real basis |
|---|---|---|---|---|
| **Commercial cabinet** | **125 kW**, 138 kVA | **261 kWh** (5 modules) | industrial LV (480Y/277 V NA, 400Y/230 V IEC, from the standard) | the outdoor "C&I" cabinet: racks, PCS, BMS, cooling and suppression in one enclosure, about 2.1 h |
| **Utility block** | **2.5 MW**, 2.75 MVA | **10.0 MWh** (2 containers) or 5.0 MWh (1) | distribution (12.47 kV NA, 11 kV IEC, from the standard), through its MV skid | two 5 MWh containers, one PCS, one MV skid: a 4-hour (or 2-hour) block |
| **Plant** | up to **4 blocks**, 10 MW | up to 40 MWh | distribution | one plant controller over its blocks *(proposed)* |

No voltage is written into code: the PCS's AC side is the world's industrial LV tier and the MV skid's high side its distribution
tier. The PCS's rated current follows from them (2.75 MVA is 3,308 A at 480 V, 3,969 A at 400 V).

## The parts
### Storage (22b)
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `bess_container` | 3 x 3 x 6 | **New.** A 20 ft (6.06 x 2.44 x 2.90 m) battery container: **12 racks** of 8 modules (96 modules, 4,992 cells of 314 Ah in a 416-series string, **5.0 MWh**, 1,331 V nominal), the rack BMS and the system BMS, DC contactors and the DC combiner, a liquid chiller (**15 kW at industrial LV** *(proposed)*, fed by its PCS's auxiliary supply), gas detection, deflagration vent panels on the roof and an aerosol suppression system; a fire-water connection on its end for a sprinkler. One item; the racks are inside it, not tiled. It must **touch its PCS** (real: short DC cables from the combiner to the PCS's DC input); the DC string is modelled inside the PCS, not on the solver *(decided: DC cable between them adds nothing a player sees)*. |
| `bess_pcs` | 3 x 3 x 3 | **New.** The power conversion system skid: a three-level IGBT inverter, **2.5 MW, 2.75 MVA**, DC 1,160-1,500 V in, the world's industrial LV out, its AC breaker, and the auxiliary transformer for the containers' chillers. **98.5 %** efficient each way *(textbook, unverified)*. Touches one or two containers and its MV skid. |
| `bess_mv_skid` | 3 x 3 x 3 | **New.** The medium-voltage skid: a **2,750 kVA** oil-immersed step-up transformer, industrial LV (delta) to distribution (grounded wye), **6 %Z** *(proposed, typical)*, **99.3 %** efficient *(proposed)*, and a ring main unit (a fused load-break switch with a vacuum circuit breaker). Its high-side terminal on the RMU's end face and underside, like the pad-mounts (padmount.md). Takes C57.109 heating as every transformer does. |
| `bess_plant_controller` | 3 x 4 x 3 | **New. Controller.** A prefabricated control house: the power plant controller (PPC), a CT and PT at the point of interconnection (the MV skid's high side, or a meter cabinet on the line), SCADA, a 24 V DC station battery and the HMI. **3 kW at residential voltage** *(proposed)*. It finds its blocks by control cable (as the PLC network does, plc.md) up to 4 PCSs. |
| `bess_cabinet` | 2 x 3 x 2 | **New.** The commercial cabinet: 5 modules (261 kWh), a **125 kW** PCS on the world's industrial LV tier, BMS, a 3 kW chiller, aerosol suppression. Its own controller (the plant controller's modes on its face). AC terminal on its bottom front cell, as a panelboard's feeder. |

### The cell plant (22a)
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `graphite_line` | 3 x 6 x 6 | **New.** Crushing, a flotation bank and spheroidising mills: graphite schist by a hopper on top to **flake concentrate, 95 % C**, rounded to spherical graphite. **160 kW at industrial LV** *(proposed)*. Tails as slurry on a pipe (as the fertilizer plant's flotation tails). |
| `graphite_furnace` | 4 x 5 x 8 | **New.** A thermal purification furnace (electric, resistance-heated, about **2,800 C**): spherical graphite to **99.95 % C**. **1.4 MW at industrial MV** *(proposed: about 4 MWh a tonne, unverified)*, a grid load in its own right. Inert gas from a nitrogen generator (ammonia-plant.md). Purified graphite to a bagging station against it. |
| `lfp_kiln` | 3 x 4 x 12 | **New.** Iron phosphate precipitation (iron ingots by hopper, phosphoric acid off a pipe from the fertilizer plant, air) and a roller-hearth kiln at **700 C** under nitrogen with lithium carbonate bags (20c) by hopper: **LiFePO4/C cathode powder**. **400 kW at industrial LV** *(proposed: electric heating)*. Cathode powder to a bagging station. |
| `cell_line` | 4 x 5 x 16 | **New. Controller of the cell plant.** Electrode mixing and coating, calendering, stacking, electrolyte filling, sealing, and **formation cycling**, and module assembly: bags of cathode powder and purified graphite by hopper, copper and aluminium foil from ingots, electrolyte made in line from lithium carbonate and the anhydrous HF route (conversion.md's HF) with carbonate solvents from the cracker's ethylene *(proposed stand-in for the solvent chain)*. **One LFP module (52 cells, 52.2 kWh) every 10 minutes** *(proposed)*; **300 kW at industrial LV**, dry room included. Modules out to a hopper under it. |
| `bagging_station` | | **Existing** (alumina-plant.md), against the furnace and the kiln. |

**Per module** *(derived from the material figures above, unverified)*: 32 kg of lithium carbonate, 115 kg of cathode powder (about 40
kg of iron and 72 kg of phosphoric acid as H3PO4), 60 kg of purified graphite, 9 kg of copper, 6 kg of aluminium. A container is 96
modules: **3.1 t of lithium carbonate**, 31 hours of the 20c carbonator's 100 kg/h. That is the real bottleneck, and it is the
point: lithium is precious.

## Layout
### A utility block (facing north, plan view; the PCS's main cell at 0,0,0)
Layer y 0:
```
x:  0 1 2 3 4 5 6 7 8 9 10 11
z0  C C C C C C P P P C C C ...      C battery container (6 long, x; y 0-2)   P PCS (y 0-2)
z1  C C C C C C P P P C C C ...      second container on the PCS's east face (optional)
z2  C C C C C C P P P C C C ...
z3  . . . . . . M M M . . . .        M MV skid, against the PCS's south face (y 0-2); its HV terminal south
z4  . . . . . . M M M . . . .
z5  . . . . . . M M M . . . .
z7  . . . K K K . . . . . . .        K plant controller (anywhere within control-cable reach)
```
### The cell plant (facing north; the cell line's main cell at 0,0,0)
```
x:  0 ... 3 4 5 6 7 8 9 ...
z0  G G G . F F F F . L L L     G graphite line (6 long, south)   F graphite furnace (8 long, south)   L LFP kiln (12 long, south)
    ...                          S bagging stations against F and L       Y cell line (16 long, east), 2 blocks south of them
z14 Y Y Y Y Y Y Y Y Y Y Y Y Y Y Y Y
```
The grids are sketches; the game tests are the layout of record.

### Legend
| Char | Block id |
|---|---|
| `C` | `wildcatter:bess_container` |
| `P` | `wildcatter:bess_pcs` |
| `M` | `wildcatter:bess_mv_skid` |
| `K` | `wildcatter:bess_plant_controller` |
| `G` | `wildcatter:graphite_line` |
| `F` | `wildcatter:graphite_furnace` |
| `L` | `wildcatter:lfp_kiln` |
| `Y` | `wildcatter:cell_line` |
| `S` | `wildcatter:bagging_station` |

## Controllers, rotation, forming, removing
- **A block** forms around its PCS: one or two containers touching its east or west face, an MV skid touching its south face. The
  plant controller is optional for one block (the PCS then runs its own default: grid-following at zero setpoint with frequency
  response) and needed for black start and for more than one block. Any rotation.
- **The cabinet** is complete in itself.
- **The cell plant** forms around the cell line; the others feed it by bags and pipe and run on their own.
- **Removing a container** opens its DC contactors (the block keeps running on the other, at half energy). Removing the MV skid
  or the PCS stops the block. A broken container drops itself **with its modules and its state of health kept** (an item that
  remembers, as UF6 cylinders keep their heels, uf6Cylinders), but its charge is lost: it is discharged to 0 % to be moved,
  as real containers ship at a low state of charge *(decided)*.

## How it runs on the grid
### State of charge and losses (22b)
- **Energy:** each container holds its usable energy, **5.0 MWh x state of health**. The plant controller keeps the state of
  charge (SOC) inside a window, **5 % to 95 %** by default, settable to 0-100 % *(proposed)*.
- **Losses, each way:** PCS 98.5 %, MV transformer 99.3 % (its copper and core losses, as every transformer's), DC (cell
  resistance and coulombic) 97 %: about **90 % round trip** at full power before auxiliaries *(derived)*. The chillers' 15 kW a
  container (scaled with cell heat, at least a third of it idle) brings a steady day-cycle to about **85-87 %**, which matches
  NREL's 85 % *(derived)*. Self-discharge is left out (LFP's few percent a month does not show in play).
- **Charging and discharging power** is limited by the PCS rating, by SOC (taper to zero over the last 3 % of the window
  *(proposed)*), and by cell temperature (half power under 0 C or over 45 C; none over 55 C, BMS OVERTEMP *(proposed)*).

### Grid-following (22b)
The default mode. The PCS locks onto a live bus and injects or draws real and reactive power: in the solver a **constant-power
injection** at its terminal (the same element as the drive's constant-power load, with its sign either way), so on its island it
lowers or raises what the generators deliver.
- **Setpoints:** P (discharge positive, charge negative) and Q (or a power factor), from the HMI, the controller's face, or the PLC.
- **Primary frequency response:** outside the deadband (read from the world's standard: **36 mHz** on 60 Hz worlds, **10 mHz** on
  50 Hz worlds, the codes' figures *(unverified)*), P moves by `-(df / f_nominal) / droop x rating`, droop **4 %**
  by default (the gensets' and turbines' figure, so they share a frequency event by rating), settable 1-5 %. Full response within
  **1 s** (the solver steps every 4 ticks; it ramps over 5 steps).
- **Peak shaving:** with the plant controller's CT on a feeder, the block discharges to hold that feeder's demand under a limit and
  charges below it, up to a setpoint *(decided: the one automatic mode that needs no clock; time-of-day and price schedules are the
  PLC's job, as a real site's SCADA does them)*.
- **Needs a grid:** with no generator or grid-forming source on its island it trips **LOSS OF GRID** within 2 s (IEEE 1547's
  anti-islanding, simplified) and recloses 5 minutes after the bus is back in tolerance *(proposed)*.

### Grid-forming (22c)
A switch on the plant controller (or a PLC point) sets the block to **GRID FORMING**. The PCS is then a **voltage source behind
an impedance**, like a generator, and joins its island's frequency model (`FrequencyIsland`) as one:
- **Rating** its PCS rating; **synthetic inertia** H = **2 s** on that rating by default, settable 0-8 s *(proposed)*; **droop 4 %**,
  and its "governor" is its power reference, which may go negative (charging) as a machine's cannot.
- **It may close onto a dead bus** (as the gensets do, genset.md) and then holds nominal voltage and frequency for whatever its
  energy and current allow. A second source synchronises to it as to any machine.
- **Current limit 1.2 x rated** *(proposed)*: in a fault it holds 1.2 pu and its voltage collapses, instead of the 5-6 x a machine
  gives. **Protection on an island fed only by inverters must be set for it:** a 51 pickup above 1.2 x the block's current never
  trips (the relays' readouts already give the fault current they see). After **1 s under 0.5 pu** it trips UNDERVOLTAGE
  *(proposed ride-through)*.
- **Running empty:** at the SOC floor it can supply nothing more; the island's frequency falls as it would with a generator at its
  limit, and under-frequency shedding and the frequency trips act as usual.

### Black start (22c)
A grid-forming block with charge closes its RMU onto a dead feeder and **energises it softly**: voltage ramped from 0 to nominal over
**2 s** *(proposed)*, so transformers take no inrush. The player (or a PLC sequence) then closes loads and lets a genset or turbine
synchronise and close onto the island; once a machine carries the load, the block may go back to grid-following. The BESS's own
chillers and controller run from its station battery and its own output while the bus is dead *(decided)*. Black start is
the quest step "A grid from a battery".

### Degradation (22b) *(question 5)*
- Each container counts **equivalent full cycles** (energy throughput / 2 x rated energy). Its **state of health** (SOH) falls
  **20 % over 6,000 cycles** at 25 C, 0.5C and 90 % depth *(proposed from the textbook figure)*; it falls faster with heat
  (x2 every 10 C over 25 C) and with charge rates over 0.5C *(proposed)*. No calendar fade (Minecraft time is not years).
- SOH scales the container's energy and is shown on its readout and as a PLC point. At under **70 %** the BMS reads **END OF
  LIFE** but the container still runs.
- **Augmentation:** a container whose modules are replaced at the cell line's recipe (a worn container's item plus 96 new modules)
  comes back at 100 %; the worn modules go out as **spent LFP modules** (an item, for a later recycling Idea).

### Thermal runaway, a world rule (22c) *(question 4)*
World rule **`bessThermalRunaway`**: "a damaged or burned battery container goes into thermal runaway". On in new worlds, off in
existing ones, as every rule since `w-rules`.
- **No random defects.** The BMS opens its contactors on over-voltage, under-voltage, over-current and **55 C**, so normal use, even
  careless use, never sets a container off. Runaway comes only from **abuse**: fire or lava touching a container for 30 s, an
  explosion's damage reaching it (a blowout fire, a vessel burst, TNT), or lightning on a container with no ground rod within 4
  blocks *(proposed)*.
- **What happens:** the container reads **THERMAL RUNAWAY**, opens its contactors and its PCS trips; off-gas (hydrogen, carbon monoxide)
  lifts the roof vents (a small explosion with no block damage beyond the container *(proposed)*); then the container **burns for 10
  minutes of play** *(proposed: real fires run hours)* and is destroyed to a **burned container** (scrap steel back, modules lost).
  Its smoke gives Poison and Blindness inside 8 blocks *(proposed: HF and soot)*.
- **Spread:** a container **touching** a burning one catches after 2 minutes; one at least **1 block** apart (NFPA 855's 3 ft) does not
  *(proposed)*. A fire-water pipe on its sprinkler connection, with water, keeps a burning container from spreading and shortens
  its fire to 5 minutes (real: water cools neighbours, it does not stop runaway).
- **Off:** the container is a block that burns like any other and never runs away.

## PLC points (22b, grid-forming ones in 22c)
On the plant controller (and the cabinet), through the PLC network as every device (plc.md):
- **Commands:** RUN / STOP; P SETPOINT and Q SETPOINT (4-20 mA, -rating to +rating); MODE (FOLLOWING, FORMING); BLACK START (close
  onto a dead bus); PEAK SHAVE ENABLE and its limit; FREQUENCY RESPONSE ENABLE.
- **States:** SOC (4-20 mA, 0-100 %); SOH; P and Q (4-20 mA); AVAILABLE CHARGE and DISCHARGE POWER; MODE; ON GRID / ISLANDED.
- **Alarms (ISA-18.2, as the HMI's):** BMS FAULT, OVERTEMP, CHILLER FAULT, LOSS OF GRID, UNDERVOLTAGE, END OF LIFE, GAS DETECTED,
  THERMAL RUNAWAY.

## Recipes and progression
- **The cell plant's parts:** steel, the mod's motors and the PLC modules' electronics, as the other Phase 20 plants. Available once
  the player has the lithium plant (20c) and the fertilizer plant's phosphoric acid (18a); graphite schist stops being "no use yet"
  (cobaltite stays, see Left out).
- **`lfp_module`:** made only by the cell line *(question 2)*.
- **`bess_container`:** 96 LFP modules, steel, copper (bus bars), a chiller (the package chiller recipe: steel, copper, a motor), and
  the PLC electronics for the BMS. **`bess_cabinet`:** 5 modules, steel, copper, electronics.
- **`bess_pcs`:** steel, copper, aluminium, electronics and the rectifier's power semiconductors (as `rectifier_mv`). **`bess_mv_skid`:**
  the pad-mount recipe scaled (steel, copper, transformer oil). **`bess_plant_controller`:** steel, electronics, a PLC CPU.
- **Quest book:** a new chapter **`grid_storage`** (order 75, after `chemicals`), five quests: *Battery graphite* (purified graphite
  bag), *Cathode powder*, *A battery module*, *A battery on the grid* (milestone `bess_discharging`, a block delivering 1 MW),
  *A grid from a battery* (milestone `bess_black_start`: a grid-forming block energises a dead bus and a genset synchronises to it).
  The cabinet gets an earlier, optional quest in `three_phase`: *Peak shaving at 480 V* (a cabinet holding a feeder under its limit).

## What it adds to the grid
| Wave | Loads or sources | Tier (from the world's standard) |
|---|---|---|
| 22a | graphite furnace 1.4 MW; LFP kiln 400 kW; cell line 300 kW; graphite line 160 kW | industrial MV and LV |
| 22b | **±2.5 MW** a block (up to ±10 MW a plant); ±125 kW a cabinet; 15 kW a container of chillers | distribution; industrial LV |
| 22c | the same, grid-forming: the first non-rotating source that can hold an island | distribution |

## Sounds and art
- **Models** (`tools/models/bess.py`, `tools/models/cell_plant.py`): the container (corrugated sides, HVAC unit on its end, vent
  panels on the roof, door with a hazard placard), the PCS skid, the MV skid (tank, radiators, RMU cabinet), the control house, the
  cabinet; the four plant parts. **Ludo** textures for the container, the cabinet and the module, cell and bag items (cathode
  powder, purified graphite, spent module); script-drawn ones named `*_placeholder` until then.
- **Sounds:** the PCS's switching whine (rises with power), the chiller fans, DC contactors closing (a clunk on start), the gas
  alarm and the suppression discharge, the vent panels lifting, and a burning container (existing fire sounds, louder, crackling).
  The graphite furnace's transformer hum, the kiln's roller drive, the cell line's machinery.
- **Visuals:** the container's status light (green running, amber idle, red fault); smoke and flame for runaway.

## Game tests and client scenes
NeoForge game scenes (the house's server tests):
- `cell_plant` (22a): schist by hopper fills a purified graphite bag; iron, acid and a lithium carbonate bag fill a cathode powder
  bag; the cell line makes a module (brought near done by hook); NO NITROGEN and NO ACID stop the kiln.
- `bess_block` (22b): a block on a genset island through a 12.47 kV feeder (11 kV on an IEC world): at P = -2 MW it charges and the
  set carries the load plus 2 MW; at +2 MW the set's delivery falls by 2 MW less losses; SOC rises and falls by the energy over the
  losses (round trip within 85-91 % over a full cycle at full power); at 95 % it tapers to zero; the genset tripped, the block trips
  LOSS OF GRID; a load step on the island: frequency dips less with the block's response on than off.
- `bess_peak_shave` (22b): the cabinet on a 480 V panel holds a feeder's demand at its limit while a load bank steps over it.
- `bess_degradation` (22b): 100 cycles forced by hook take SOH down by 0.33 %; hot (chiller off) by more; the replaced container
  back at 100 %.
- `bess_forming` (22c): a grid-forming block alone on an island holds nominal frequency through a load step; a genset closes in sync
  onto it; a bolted fault holds 1.2 pu and trips UNDERVOLTAGE after 1 s; a 51 relay set under 1.2 pu trips first.
- `bess_black_start` (22c): the dead feeder energised over 2 s with no transformer inrush, loads picked up, a genset synchronises.
- `bess_runaway` (22c): rule on: fire on a container for 30 s, THERMAL RUNAWAY, its neighbour touching catches, the one a block
  apart does not; rule off: nothing. `world_rules` gains the new rule.
- Unit tests: `BessTest` (SOC, losses, taper, droop, SOH).

Client scenes (filmed, per the house rule): **`bessBlock`** (a block discharging into a feeder, the HMI's SOC trend), **`blackStart`**
(the dead feeder coming up and a genset closing onto it), **`cellPlant`** (screenshots of the four parts), **`bessRunaway`** (the
container venting and burning, its neighbour spared by a gap).

## Proposed waves
- **22a:** the cell plant (graphite line, graphite furnace, LFP kiln, cell line) and the module.
- **22b:** the container, PCS, MV skid, plant controller and cabinet; grid-following, SOC, losses, frequency response, peak shaving,
  degradation, PLC points, quests 1-4.
- **22c:** grid-forming, black start, the current limit and ride-through, thermal runaway (the world rule), quest 5.

## Code that changes *(for the plan, not a design decision)*
- `solver/FrequencyIsland`: a storage source's power reference may run from -1 to +1; the island's delivered power is already netted.
- `circuit/`: a signed constant-power element for grid-following (from `DriveLoad`); a `Generator` for grid-forming with a current
  limit; a soft-energise ramp on its source.
- `CircuitManager.stepFrequency`: an island whose only sources are grid-following trips them (LOSS OF GRID).
- `Rules`: `BESS_THERMAL_RUNAWAY`. `Tuning` ("22a-22c"). `OreUses`: graphite schist names the graphite line.

## Left out
- **NMC and other chemistries** (nickel, manganese and cobalt cathodes; sodium-ion; flow batteries) *(question 3)*. Cobaltite keeps
  no use.
- Battery recycling (spent modules are kept as an item for a later Idea).
- Money, prices and an energy market; time-of-day schedules beyond what a PLC program does.
- Solar and wind (the obvious partners of storage; not in Wildcatter).
- Other mods' energy: a BESS stores watt-hours on the grid, not FE; other mods reach it only through the plugs, as before.
- The DC string on the solver (DC cables from container to PCS); DC-coupled sources.
- Cell-level faults and module-by-module state; a cell's voltage curve beyond the DC voltage readout.

## Questions for Tim
The same in `docs/questions/grid-batteries.md`, with why and the alternatives. Recommended option first.
1. **Sizes:** a 125 kW cabinet and a 2.5 MW / 10 MWh utility block / the utility block only / a larger 5 MW, 4-container block too.
2. **Where cells come from:** an LFP cell plant of four parts / a crafting recipe from bags (lithium carbonate, phosphoric acid,
   graphite, iron) / both (crafting for the cabinet, the plant for containers).
3. **Chemistry:** LFP only / LFP and NMC (gives cobaltite and nickel a use) / LFP and sodium-ion.
4. **Thermal runaway:** a world rule on in new worlds, abuse only / the rule on with a rare random cell defect / the rule off by
   default.
5. **Degradation:** real LFP cycle fade (20 % over 6,000 cycles, hotter and faster worse) / ten times faster, so it shows in play /
   none.
6. **Grid-forming:** both modes from the start of the phase, a switch on the controller / grid-following only, grid-forming as a
   later upgrade item / grid-forming only (simpler, less real).
7. **Fault current:** the real inverter limit (1.2 pu, relays must be set for it) / machine-like fault current so protection behaves
   as with gensets.
8. **Where in the progression:** after the lithium plant (late game, as specced) / earlier, with the cabinet's cells crafted from
   vanilla-reachable materials so a 480 V base can have one.
