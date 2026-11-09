# ISR wellfield operations: CO2, monitor wells, the bleed, restoration, satellite IX

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **16d** (16d1 CO2, the bleed
> and satellites; 16d2 monitor rings, excursions and restoration). Decisions are in `docs/questions/specs-wildcatter.md`
> (items 31-35).

Five items the ISR spec (isr-wellfield.md, approved 2026-11-01) left in ROADMAP Ideas. Each has its real basis and a proposed scope.

Sources: as isr-wellfield.md (NRC NUREG-1910 GEIS ch. 2; Lost Creek technical reports; Crow Butte; WNA), plus the lime kiln
(alumina-plant.md) and the lease's disposal well (lease.md). Figures marked *(unverified)* are from memory of those sources and must be
checked against the GEIS before approval.

## 1. CO2 in the lixiviant
**Real basis.** Wyoming ISR adds **oxygen and carbon dioxide** to the injection: the CO2 holds the pH near neutral and adds the
bicarbonate that carries uranium(VI) as its carbonate complex (GEIS ch. 2). isr-wellfield.md left it out because "the mod has no CO2
source, so the native bicarbonate carries the uranium."

**A source:** the lime kiln (alumina-plant.md) gives off **0.785 kg of CO2 a kg of quicklime** (CaCO3 -> CaO + CO2), counted today:
about **327 kg/h** at 10 t a day.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `co2_liquefier` | 3 x 4 x 6 | **New.** Kiln-gas CO2 recovery: a scrubber, a two-stage compressor, a dryer and an ammonia-free refrigerated condenser at about 20 bar and -25 C *(unverified)*. Against the lime kiln; up to **150 kg/h** *(proposed)*, **75 kW at 480 V** *(proposed: about 0.4 kWh a kg, unverified)*. **CO2** (a new pipe product, kg, a liquid in the pipe) out of its south face. |
| `co2_storage_tank` | 3 x 3 x 8 | **New.** A 30 t insulated liquid CO2 tank with its vaporiser, beside the header house; the only tank that holds CO2. |
- **Dose:** **0.5 g/L** of injection *(proposed)*: 25 producers at 2.5 L/s take **113 kg/h**.
- **Effect:** the head grade's oxygen factor (isr-wellfield.md) gains a **CO2 factor of 1.3** at the full dose *(proposed)*: today's
  wellfields keep their grade; with CO2 they make 30 % more.
- The header house reads its CO2 dose; **NO CO2** only when a tank is piped to it and empty.

## 2. The bleed to a disposal well
**Real basis.** The header house holds back 1 % as bleed (isr-wellfield.md) so the wellfield draws water inward; real operations send it
to a deep disposal well or evaporation ponds (GEIS ch. 2).

**Proposed scope:**
- The header house's bleed (0.625 L/s at 25 producers) goes out of a **new nozzle**, the north face of its back cells, as **wellfield
  bleed**, a new pipe product.
- The lease's **injection pump** (lease.md, 10 L/s) takes wellfield bleed as it takes produced water, into a **disposal wellhead**.
- **No disposal:** the bleed is counted, as today; the monitor ring (below) is what makes the bleed matter.

## 3. Monitor-well rings and excursions
**Real basis.** A ring of monitor wells surrounds each wellfield in the ore aquifer, with more in the aquifers above and below. Upper
control limits (UCLs) are set from baseline on chloride, conductivity and alkalinity; two exceeded in one well is an **excursion**,
which the operator must correct by pumping harder (more bleed) or stop injecting *(GEIS ch. 2; ring spacing and distances unverified:
monitor wells about 120-150 m out, about 120 m apart)*.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `monitor_wellhead` | 1 block | **New.** A sampling well: the rig completes a hole that is not an ISR or oil well as a monitor well when one is clicked onto its casing; a 1.5 kW sampling pump at 480 V *(proposed)*, fed along its pipe from the header house as the producers are. |
- **The ring:** a header house counts monitor wells **30 to 60 blocks** from its nearest pattern, and needs **one every 40 blocks** of the
  ring's length, **6 at least** *(proposed, compressed as the fields are)*. Without them it reads **NO MONITOR RING** and will not inject.
- **The lixiviant front:** each header house keeps a front radius beyond its patterns. With bleed at or over **0.5 %** it stays put; under it
  the front grows by **1 block a game hour per 0.1 %** short *(proposed)*.
- **Excursion:** when the front reaches a monitor well, the house reads **EXCURSION** and sounds an alarm. With more bleed the front
  falls back; if it is still out after **3 game days** *(proposed; real 60 days)*, the house **stops injecting** until it is pulled
  back.
- **The bleed matters:** a disposal well that stops (full, broken, unpowered) lets the bleed fall to zero.

## 4. Groundwater restoration
**Real basis.** After mining, a wellfield is restored to its pre-mining use: **groundwater sweep** (pumping without injecting, about one
pore volume, to disposal), **reverse osmosis** (pumping through RO, injecting the permeate, disposing of the brine) and **stability
monitoring**, before the regulator releases it (GEIS ch. 2).

**Proposed scope** (lowest priority here):
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `ro_unit` | 3 x 3 x 8 | **New.** Cartridge filters, high-pressure pumps and spiral-wound membranes: **30 L/s** of feed at **75 % recovery** *(unverified)*, about **1.5 kWh a m3** *(unverified)*, **160 kW at 480 V**. Permeate back to the injectors through the header house; brine to the disposal well as wellfield bleed. |
- **Modes**, by a sneak-click on the header house: **MINE**, then **SWEEP** (producers to disposal, injectors off, **one pore volume**:
  the band's volume under the patterns × 0.3 porosity *(proposed)*), **RO** (two pore volumes through the RO unit), **STABILITY**
  (**2 game days** without excursion *(proposed; real 6-12 months)*), then **RESTORED**.
- **What it is for:** a RESTORED wellfield can be taken up without leaving its front behind; one left unrestored keeps its front, and a
  new header house within its radius starts in EXCURSION *(proposed)*.

## 5. Satellite IX with trucked resin
**Real basis.** Remote wellfields have their own **satellite IX** plants; loaded resin is trucked in trailers to the central plant for
elution, and eluted resin trucked back *(Lost Creek; Crow Butte; trailer size unverified, about 15 m3)*.

**Proposed scope:**
- **Satellite mode:** `ix_columns` gets a mode **SATELLITE** (sneak-click): it loads resin but does not elute; no salt or acid.
- **The trailer:** a **resin trailer** item (a placeable 2 x 3 x 6 block when set down, carried as one item, as the fuel assembly is):
  **10 m3** of resin, **800 kg of U3O8** at 80 g/L *(proposed)*. A satellite fills an empty trailer set against it; the central
  `ix_columns` (mode **ELUTION**) empties a full one set against it and refills it with eluted resin.
- **Why:** a header house is limited to 25 producers and its pipe; a satellite lets a front 500 blocks away pay without a pipeline.

## Layout (the 7c plant, facing north, plan view; new parts marked)
Layer y 0:
```
 W W W W  O O O    H H H H (z 0..9)  X X X X X X X X     W producers' pipe    O oxygen generator   H header house
          T T T    H H H H           X X X X X X X X     T CO2 storage tank (new, y 0-2), against the header house
          T T T    H H H H                               X IX columns (y 0-5)
 R R R             ..                                    R RO unit (new, y 0-2), on the header house's east (injector) side
        m . . . . . . m . . . . . . m                    m monitor wellheads (new) in a ring 30-60 blocks out
```
The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `H` | `wildcatter:header_house` |
| `O` | `wildcatter:oxygen_generator` |
| `X` | `wildcatter:ix_columns` |
| `T` | `wildcatter:co2_storage_tank` |
| `R` | `wildcatter:ro_unit` |
| `m` | `wildcatter:monitor_wellhead` |

## Controller, rotation, forming, removing
- The **header house** stays the wellfield's controller; it finds the CO2 tank and RO unit by touching, and monitor wells by their pipe.
- Any rotation. Removing the CO2 tank drops the CO2 factor; removing the RO unit pauses RO; removing a monitor well can break the ring
  (NO MONITOR RING).

## Game tests
- `isrWellfield` (extended, 16d1):
  - with CO2 from a test-filled tank at full dose, the producer's head grade is **1.3 ×** its oxygen-only value;
  - the bleed reaches a disposal wellhead through an injection pump;
  - a satellite fills a resin trailer that a hook has brought near full; the trailer set against the central columns gives eluate.
- `isrMonitor` (16d2): with five monitor wells, NO MONITOR RING; with six, it injects; bleed cut to 0 and time skipped by hook, the front
  reaches a well: EXCURSION; bleed restored, it falls back. A SWEEP, RO and STABILITY sequence (sped by hook) ends RESTORED.
- **Screenshot:** the header house with its CO2 tank.

## Proposed waves
- **16d1:** CO2, the bleed to disposal, satellite IX.
- **16d2:** monitor rings, excursions, restoration.
