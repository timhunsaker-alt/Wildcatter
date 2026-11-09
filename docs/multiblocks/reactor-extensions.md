# Reactor extensions: containment, protection, casks, contamination, rods

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **19** in three waves here:
> **19a** containment and protection; **19b** dry casks, contamination and fresh-fuel inspection; **19d** rod materials and the sodium
> sulfate. (19c is fuel-fabrication-extensions.md.) Decisions are in `docs/questions/specs-wildcatter.md` (items 85-96).

Eleven items the reactor (reactor.md, 8a and 8d) and fuel handling (fuel-handling.md, 8e) specs left in ROADMAP Ideas. Each has its real
basis and a proposed scope. Tim's settled choices hold: **irradiated fuel is never an item**; the core is 9 assemblies at real power
density with 100x burnup; heat-up is compressed 80x; radiation is a real dose with real effects.

Sources: as reactor.md and fuel-handling.md (Westinghouse standard technical specifications, NUREG-1431; textbook kinetics and decay
heat). Figures marked *(unverified)* are from memory of those sources or of NUREG-1431's bracketed values and must be checked before
approval.

---

## Wave 19a: containment and protection

### 1. A containment building with hydrogen recombiners and igniters
**Real basis.** A PWR's island stands in a steel-lined concrete **containment**, a large dry one designed for about **0.4 MPa** gauge.
After a core-damage accident its hydrogen is held below burnable levels by **passive autocatalytic recombiners (PARs)** and, in
ice-condenser and some other plants, by **glow-plug igniters** that burn it in small deliberate burns *(textbook, unverified)*. 8d puts
the hydrogen into "a notional 5,000 m3 of building air" with no containment.

**Proposed scope:**
| Part / block | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `containment_concrete` | 1 block | **New block.** 1 m of reinforced concrete with its steel liner; blast resistance 1,200, as the vessel. A building material, laid like any block (it is a wall, not a machine). |
| `equipment_hatch` | 4 x 4 x 1 | **New.** The 7 m class equipment hatch (here 4 m), set into the wall; open, the building is not closed. |
| `personnel_airlock` | 2 x 3 x 3 | **New.** Two interlocked doors; the way in and out of a closed building. |
| `hydrogen_recombiner` | 1 x 2 x 1 | **New.** A PAR: a box of catalyst plates that starts itself above **1 % hydrogen** and removes up to **1.5 kg/h** at 4 % *(proposed, unverified)*. No power. |
| `hydrogen_igniter` | 1 block | **New.** A glow plug on the wall, on **120 V** (230 V IEC) from the plant's service; above **4 %** in its compartment it burns the hydrogen there in a small deflagration (no blast), **while powered**. |
| `containment_monitor` | 1 x 2 x 1 | **New.** The panel that reads the building: its free volume, pressure, temperature, hydrogen % in the lower compartment and in the dome, and its dose rate. |

- **The building forms** when the vessel's island is enclosed by containment concrete, the hatch shut and the airlock's doors closed: the
  monitor flood-fills the air inside (up to **60,000 m3**, *proposed*) and reads **CLOSED, n m3**. A gap reads **OPEN** and where.
- **Hydrogen** (8d) now goes into the **lower compartment**: the 5,000 m3 nearest the pressurizer, as 8d's notional volume, which mixes
  into the whole building over **1 hour** *(proposed)*. The 4 % blast threshold is checked in the compartment and in the dome. A big
  building with PARs and powered igniters keeps the hydrogen down; an unpowered igniter does nothing.
- **Pressure:** the steam the RCS boils off (8d) and the relief tank's discharge (item 4) raise the building's pressure (ideal gas at its
  temperature, *simplified*). Above **0.4 MPa gauge** the monitor alarms; at **2.5 times design** *(proposed)* the hatch fails and the
  building is OPEN.
- **Containment spray and fan coolers** are left out *(proposed: a later item)*.

### 2. Low-temperature overpressure protection (LTOP)
**Real basis.** A cold vessel is brittle; its pressure must stay under the **pressure-temperature limit curves**. Below an enable
temperature the pressurizer PORVs are set much lower (a few MPa) *(NUREG-1431 LCO 3.4.12; values unverified)*.

**Proposed scope:**
- **The P-T curve** *(proposed, simplified)*: allowed pressure **3.4 MPa** up to **100 C**, rising in a straight line to **15.5 MPa** at
  **290 C**.
- **LTOP:** the console's new switch **LTOP ARMED** (sneak-click on its lower face). Armed below **150 C**, the PORVs lift at **3.2 MPa**.
  Below 150 C with it not armed, the console alarms **LTOP NOT ARMED**.
- **Over the curve:** a **P-T LIMIT** alarm and a latched count of violations on the vessel's readout *(proposed)*. Over **twice** the
  curve's pressure the vessel **fails**: a LOCA (the RCS empties into the building at the PORVs' rate times ten, *proposed*), and 8d's
  boil-off, damage and hydrogen follow.

### 3. The real 55 C/h heat-up limit
**Real basis.** Technical specifications limit RCS heat-up and cool-down to about **55 C (100 F) an hour** for thermal stress
*(NUREG-1431; unverified)*. reactor.md compresses heat-up 80x (1/80 of the heat capacity) so cold to hot standby takes about 30 minutes.

**Proposed scope:**
- The compression stays. The console tracks the heat-up rate **in plant time** (the real-rate equivalent) and alarms **HEAT-UP RATE** over
  55 C/h, as 8d's cool-down already holds 55 C/h.
- Each excursion adds to a **fatigue count** on the vessel's readout *(proposed)*; it does no damage.
- **A world setting** `realHeatup` (off by default) removes the 80x, for players who want the 8-12 hours.

### 4. The pressurizer relief tank
**Real basis.** The PORVs and safety valves discharge into a **pressurizer relief tank** of water with a spray and a rupture disc set at
about **0.7 MPa** *(unverified)*; a burst disc puts the steam into containment.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `pressurizer_relief_tank` | 2 x 2 x 6 | **New.** A horizontal tank holding **30 m3** of water *(proposed)*, against the pressurizer or piped to its top. It condenses what the PORVs let go, warming; at **0.7 MPa** its disc bursts and the rest goes into the building (item 1) or the building air. Its spray from the CVCS's makeup can cool it (a sneak-click). |
- Without it, the PORVs discharge as today.

### 5. Full OT-delta-T and OP-delta-T trips
**Real basis.** NUREG-1431's overtemperature and overpower delta-T trips protect against DNB and fuel melt with a setpoint that falls
with Tavg and pressure. reactor.md simplifies them to "the core outlet over 343 C, or within 3 C of boiling".

**Proposed scope:**
- **OTΔT:** trip when ΔT > ΔT0 [K1 − K2 (1 + τ1 s)/(1 + τ2 s) (Tavg − T') + K3 (P − P')], with the constants of H. B. Robinson 2,
  whose Table 3.3.1-1 Note 1 the NRC aligned with NUREG-1431 Rev. 3: **K1 = 1.1265**, **K2 = 0.01228 /F (0.0221 /C)**,
  **K3 = 0.00089 /psig (0.129 /MPa)** (NRC amendment for H. B. Robinson 2, found through the NRC's ADAMS search; the accession number
  was not recorded and must be added). T' is the full-power Tavg (308.9 C), P' 15.51 MPa; the lead-lag τ1 = 30 s and τ2 = 4 s
  *(unverified)*; the axial flux term f(ΔI) is left out (the model has no axial shape).
- **OPΔT:** trip when ΔT > ΔT0 [K4 − K5 (τ3 s/(1 + τ3 s)) Tavg − K6 (Tavg − T')], with K4, K5 and K6 taken from the same note when it
  is read in full *(not yet found: the wave's plan fills them in)*.
- ΔT0 is the full-power core ΔT, 33 C. The simplified 343 C check stays as a backstop.

---

## Wave 19b: fuel storage, contamination, inspection

### 6. Dry cask storage for the pool's oldest fuel
**Real basis.** Fuel cooled several years in the pool is loaded into a welded steel **canister** in the pool's cask pit, drained, vacuum
dried and backfilled with helium, then moved in a transfer cask to a **concrete storage module** on a pad *(textbook)*. 8e: casks are in
ROADMAP Ideas; irradiated fuel is never an item.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `cask_loading_station` | 3 x 4 x 4 | **New.** The cask pit, the welding machine and the vacuum drying and helium backfill skid, against the pool's controller. **50 kW at LV.** Empty **canisters** (crafted from stainless, nickel-smelter.md, or steel before it) by a hopper on top. |
| `storage_module` | 4 x 6 x 6 | **New.** A horizontal concrete storage module (1 m walls) on its own; linked to a loading station by a sneak-click with the Power Meter, within **64 blocks** *(proposed)*. It holds one canister. |
- **A canister holds 12 assemblies** *(proposed; real 24-37 PWR)* with at most **12 kW** of decay heat in all and **1.5 kW** in any one
  *(proposed)*. The pool's controller has a mode **LOAD CASK**: the coolest assemblies, one move a minute (as REFUEL).
- **Then:** drain, dry and backfill (**20 minutes**, *proposed*) and weld (**10 minutes**); the canister then **transfers** to the
  linked module over **10 minutes**, an abstract transfer cask *(proposed: a vehicle is out of scope)*.
- **The fuel stays records**: the module holds them and goes on decaying them; it radiates through its 1 m walls (×1/1000 a block,
  fuel-handling.md). The module is unbreakable while loaded, as well casing is.
- The pool's 36 racks are freed.

### 7. Contamination and decontamination
**Real basis.** Radioactivity escapes as **contamination**: activated corrosion products and fission products in the coolant, carried out
on whatever touches it. It is a dose source, it sticks to people, and it is cleaned off *(textbook)*. 8e has none ("Dose is a point-source
line-of-sight sum; there is no contamination").

**Proposed scope:**
- **Coolant activity:** the RCS carries an activity that rises with **leaking rods** (fuel-fabrication-extensions.md, item 4) and falls
  with the CVCS demineralizer's resin. The CVCS reads it.
- **Where it gets out:** breaking or opening a primary part with water in it; the relief tank's disc (item 4); core damage (8d); the
  containment failing (item 1); a pool drained. Each puts **contamination** on the blocks within a radius (3 to 32 blocks, by what failed,
  *proposed*), stored sparsely by block position, decaying with a **30-day** half-life in game time *(proposed: Co-60 is 5.3 years)*.
- **The dose:** a contaminated block is a source of **up to 1 mSv/h at 1 m** *(proposed)*, summed as other sources are.
- **On a player:** walking on contaminated blocks transfers some to the player; it doses them until they **decontaminate**.
| Part / item | Cells | Real-world basis and values |
|---|---|---|
| `personnel_monitor` | 1 x 3 x 1 | **New.** A whole-body contamination monitor and shower at the airlock or site exit, **120 V** (230 V IEC): a player stepping in is read and washed clean. |
| `decon_washer` (item) | | **New.** A high-pressure washer; plugged into a receptacle (a real 120 V or 230 V load through a plug, **2 kW**), right-click cleans a block's contamination by **90 %**, using water from a bucket or pipe. |
- The dosimeter reads contamination dose with the rest; the Power Meter on a block reads its contamination.

### 8. Fresh-fuel inspection
**Real basis.** New assemblies are inspected on receipt and before loading (visual, grid and nozzle damage) on a new-fuel elevator or
stand *(textbook)*.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `new_fuel_inspection_stand` | 2 x 6 x 2 | **New.** An inspection stand with lights and a camera, beside the refuelling machine's hopper: an assembly passed through it (by hopper) is marked **INSPECTED**. **5 kW at LV.** |
- **Handling damage:** an assembly moved by hopper or crane takes a **1 %** chance *(proposed)* of a bent grid; inspection finds it and
  the assembly fixture repairs it (a sneak-click with it in its hopper, 10 minutes). Loaded uninspected, a bent grid becomes a **leaking
  rod** (item 7) after a third of its cycle.
- The refuelling machine takes uninspected assemblies too, as a real crew could load them.

---

## Wave 19d: rods and the sodium sulfate

### 9. Silver-indium-cadmium and hafnium rods
**Real basis.** Westinghouse RCCAs use **Ag-In-Cd** (80-15-5); some plants use **hafnium**; CE plants (and Wildcatter) use B4C
(reactor.md). Each has its own worth and life *(textbook)*.

**Proposed scope:**
- **Hafnium RCCAs** from zirconium-extensions.md (item 3): **85 %** of a B4C RCCA's worth *(proposed, unverified)*, and **no helium**
  (item 10): they last.
- **Ag-In-Cd RCCAs:** **90 %** of B4C's worth *(proposed, unverified)*. Silver is mined (ores.md); **indium and cadmium** are by-products
  of a zinc refinery, which Wildcatter does not have (ores.md: "they are by-products of refining zinc, so they come from sphalerite
  later"). **Gated:** AIC waits for a zinc refinery (questions file).
- **Mixed sets:** the vessel takes any mix; its worth is the sum. The shutdown margin check (1,300 pcm with the centre rod stuck) is the
  player's to keep: the console reads the margin and alarms **SDM LOW** below 1,300 pcm.

### 10. Helium in the B4C rodlets
**Real basis.** B-10 + n -> Li-7 + He-4: a B4C rodlet fills with helium and the pellets swell as their boron-10 burns; rods are replaced
for B-10 depletion at the tip and cladding strain *(textbook, unverified limits)*.

**Proposed scope:**
- Each RCCA counts its **B-10 burnup**: the share of the core's neutron absorption it took while inserted (bank D at power takes the
  most). At **30 %** at its tip *(proposed)* its readout reads **REPLACE**; past **40 %**, its worth falls in a straight line to **70 %**
  at 60 % *(proposed)*.
- A replaced RCCA (8e's refuelling machine with REFUEL can swap RCCAs, *proposed*) goes to the pool as a record, as fuel does.

### 11. A buyer for the borax plant's sodium sulfate
**Real basis.** Sodium sulfate (salt cake) goes to kraft pulp, detergents and glass *(textbook)*. The boron plant counts 0.57 kg a kg of
boric acid (boron.md); the lithium plant (lithium.md) and the chromium step (zirconium-extensions.md) make it too.

**Proposed scope:**
- **Bags:** each plant's sodium sulfate goes to a bagging station as **1 t bags** (tagged `c:sodium_sulfate`, *proposed*).
- **A use in Wildcatter:** none is real and in scope (no pulp mill, no glass works). The bag can be dumped in a **lined tailings cell**
  (tailings-cell.md) as solid waste, counted, so plants need not back up (questions file: a glass batch recipe is the alternative).

---

## Layout (the island, facing north, plan view; reactor.md's layout inside a containment; new parts marked)
Layer y 0:
```
x:  -8 ... -3-2-1 0 1 2 3 4 5 ... 10
z-8  W W W W W W W W W W W W W W W W      W containment concrete (walls to y 20, a dome over; free volume flood-filled)
z-5  W . . . . . R R . . . . . . . W      R, Q coolant pumps; A, B steam generators; P pressurizer; V vessel (reactor.md)
z-3  W . . T P P A A A . . . . . . W      T pressurizer relief tank (new), against the pressurizer
z0   W . C C C V V V . . . . h . . W      h hydrogen recombiners (new, on the floor, several)
z3   W . C C C B B B . . . . . . . W      g igniters (new, on the walls, not drawn)
z6   W . . . . Q Q . . . . . . M . W      M containment monitor (new)
z8   W W W W E E E E W W W L L W W W      E equipment hatch (new, 4 wide)   L personnel airlock (new)
```
Outside: the spent fuel pool (fuel-handling.md) with the cask loading station against its controller; storage modules on a pad within
64 blocks.

The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `W` | `wildcatter:containment_concrete` |
| `E` | `wildcatter:equipment_hatch` |
| `L` | `wildcatter:personnel_airlock` |
| `h` | `wildcatter:hydrogen_recombiner` |
| `g` | `wildcatter:hydrogen_igniter` |
| `M` | `wildcatter:containment_monitor` |
| `T` | `wildcatter:pressurizer_relief_tank` |
| `V`, `A`, `B`, `P`, `R`, `Q`, `C` | reactor.md's parts |

## Controllers, rotation, forming, removing
- **The vessel** stays the island's controller; it finds the relief tank by touching or the pressurizer's pipe.
- **The containment monitor** is the building's controller (CLOSED or OPEN). The hatch and airlock are its parts; the concrete is ordinary
  block.
- **The pool's controller** finds the cask loading station by touching; storage modules by link.
- Any rotation.
- **Removing:** a wall block opens the building (OPEN); the igniters off or removed do nothing; the relief tank removed returns the PORVs
  to today's discharge.

## Game tests
- `reactorSafety` (extended, 19a):
  - a closed containment reads its volume; with 8d's loss of cooling by hook, PARs and powered igniters keep the dome under 4 % and there
    is **no blast**; with the igniters' power cut and no PARs, the lower compartment reaches 4 % and blasts as in 8d;
  - LTOP: the RCS cold at 3.0 MPa with LTOP not armed reads LTOP NOT ARMED; pressurised by hook to 3.6 MPa it reads P-T LIMIT; armed,
    the PORVs hold 3.2 MPa;
  - a heat-up faster than 55 C/h plant-time alarms HEAT-UP RATE;
  - the relief tank warms under a PORV lift and bursts its disc at 0.7 MPa;
  - a step that raises Tavg past the OTΔT setpoint trips the reactor naming OT-DELTA-T.
- `casks` (19b): an old pool's records (hooked) load into a canister (moves sped up), dry, weld and transfer to a linked module; the dose
  beside the module is low; the module cannot be broken.
- `contamination` (19b): a primary part broken with water in it contaminates its surroundings; a player walking there carries it; the
  personnel monitor cleans them; the washer cleans a block.
- `rods` (19d): a hafnium RCCA set reads 85 % worth and SDM LOW when below 1,300 pcm; bank D's B-10 count reaches REPLACE (hooked).
- **Screenshot:** the closed containment cut away; the cask pad.

## Proposed waves
- **19a:** containment, PARs and igniters, LTOP, the heat-up limit, the relief tank, OTΔT and OPΔT.
- **19b:** dry casks, contamination and decontamination, fresh-fuel inspection.
- **19d:** hafnium (and later AIC) rods, helium in B4C, the sodium sulfate bags.
