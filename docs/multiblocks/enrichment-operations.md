# Enrichment operations: spin-up, crashes, cooling, sampling, heels, product assay

> **16c1 BUILT (2026-11-10), decided on recommendation while Tim was away; not approved.** See "As built (16c1)" at the end and
> `docs/questions/phase16-uranium.md` (items 17-23). **16c2 BUILT 2026-11-12** (see "As built (16c2)").
> Drafted as proposed phase **16c** (16c1 assay choice, spin-up and cooling; 16c2 crashes, sampling and heels). Decisions are in `docs/questions/specs-wildcatter.md` (items 36-40).

Six items the cascade spec (enrichment.md, approved 2026-11-01) left in ROADMAP Ideas. Each has its real basis and a proposed scope.
Numbers use enrichment.md's: Glaser's separation factor 1.74, a stage factor of its square root, 11.4 SWU an hour and 571 kW a machine
(the written 1000x exception), 1,225 Hz.

Sources: enrichment.md's ([N] NUREG-1827 for the National Enrichment Facility; [G] Glaser; [W] Wikipedia, *Zippe-type centrifuge*;
the SWU literature); ANSI N14.1 for cylinder heels and inspection *(heel limits unverified: 48Y about 22.7 kg, 30B about 11.3 kg)*.

## 1. A choice of product assay
**Real basis.** A cascade is set for its product and tails assays; utilities order LEU at 3-5 % and the 30B is licensed to 5 % ([N]).

**Proposed scope:**
- The feed station's sneak-click sets the **product assay: 3.0, 3.5, 4.0, 4.5, 4.95 %** and the **tails assay: 0.20, 0.25, 0.30 %**.
- `fluid/Enrichment.java` already works from any three assays. With the stage factor 1.319 (derived):

| Product / tails | Feed a kg of product | SWU a kg of product | Stages (machines at least) |
|---|---|---|---|
| 4.95 / 0.25 % | 10.20 | 7.81 | 11 |
| 4.00 / 0.25 % | 8.13 | 5.83 *(derived)* | 11 |
| 3.00 / 0.25 % | 5.97 | 3.81 *(derived)* | 10 |
| 4.95 / 0.20 % | 9.30 | 8.73 *(derived)* | 12 |

- **What it changes downstream:** the 30B, the fuel assembly item and the reactor carry the assay. An assembly's starting reactivity
  falls with it: **+16,000 pcm at 4.95 %, less 4,000 pcm for each percent below** *(proposed)*, so 3 % fuel gives a cycle about half
  as long.

## 2. Spin-up and coast-down
**Real basis.** Machines take hours to reach speed on their small motors and coast for days in their vacuum after the power goes: a real
cascade rides through a short outage *(textbook, unverified hours)*.

**Proposed scope:**
- **Spin-up:** **10 minutes** from rest to full speed *(proposed)*, the converter ramping its frequency; separative work follows the
  speed share as now.
- **Coast-down:** with no drive, a machine loses speed over **60 minutes** *(proposed)*, still separating at its speed share. While the
  machines coast the feed station keeps feeding at the lower rate.
- **Critical speeds:** a supercritical rotor passes its resonances on the way up. Held between **30 % and 60 %** of its speed for more
  than **2 minutes** *(proposed)*, a machine may crash (below). The converter's ramp passes through in under a minute.

## 3. Cooling water to the casings
**Real basis.** The motors and the casings are water-cooled; their heat is the machines' electrical input.

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `cascade_cooling_skid` | 3 x 3 x 6 | **New.** Plate exchangers and pumps on the hall's closed cooling loop: it takes **circulating water** (from a CW pump, as the RHR does, reactor.md) on its north face and returns it on its south face. **55 kW at 480 V** *(proposed)*. It cools the machines along the header it touches, as the converter powers them. |
- Each machine's heat is its electrical power: 571 kW. At a 10 C rise that is **13.7 kg/s** of water a machine, **150 kg/s** for 11
  (derived). A cooling tower takes it.
- **Without cooling:** the machines read **CASING HOT** after 5 minutes and the feed station holds feed; after **15 minutes** each
  may crash *(proposed)*.
- Cascades built before this wave run without a skid until one is placed on their header *(proposed: no old plant stops)*.

## 4. Machine crashes
**Real basis.** A rotor that loses its balance or bearing breaks up in its casing; a crash can shake neighbouring machines on the same
platform. Real failure rates are a fraction of a percent a year *(unverified)*.

**Proposed scope:**
- **No random crashes** *(proposed)*: a machine crashes only for a reason the player can see: lingering at a critical speed; a hot
  casing; **overspeed** (fed over **1,300 Hz** for more than 15 minutes, which a PLC can command through the converter's speed
  reference, plc.md 13e); or the converter's breaker closing onto machines already turning at a different speed.
- **A crash:** a bang, the machine reads **CRASHED** and drops out of the count, and the block becomes a **crashed centrifuge** (it
  breaks to scrap: steel and a carbon-fibre rotor shard). Each machine within one block of it on the header has a **20 %** chance
  *(proposed)* to crash too.
- **UF6 release:** a crash lets a little UF6 out: **0.1 kg** *(proposed)*, a puff of white smoke (UF6 meets moisture as HF and
  UO2F2), harming a player within 3 blocks.

## 5. The product sampling autoclave and blending
**Real basis.** NEF liquefies each product 30B in an autoclave to take a liquid sample for its assay, and blends cylinders to a
customer's assay in a blending station ([N]).

**Proposed scope:**
| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `sampling_autoclave` | 3 x 3 x 5 | **New.** A steam-heated autoclave that liquefies a 30B and draws a sample: **30 minutes** a cylinder *(proposed)*, **50 kW at 480 V** *(proposed)*. A full 30B by a hopper on top comes out **SAMPLED, n.nn %** below. With two 30Bs of different assays and an empty one it **blends** them to a chosen assay between the two. |
- **The fuel plant's autoclave** (fuel.md) takes only **sampled** 30Bs *(proposed)*, as a fabricator accepts nothing without its
  certificate.

## 6. Cylinder heels
**Real basis.** An emptied cylinder keeps a heel of UF6; ANSI N14.1 limits it, and cylinders are re-inspected every five years
*(unverified)*.

**Proposed scope:**
- An emptied 48Y keeps **20 kg** of UF6 and an emptied 30B **10 kg** *(proposed, under the limits)*, carried in the item and counted
  when it is refilled.
- **No washing or re-inspection** *(proposed: a chore with no play in it)*; the heels are only counted.

## Layout (the 7e hall, facing north, plan view; new parts marked)
Layer y 0:
```
 S S S (feed station) == header =========================== P P P (product take-off)   A A A (sampling autoclave, new, stand-alone)
                          c c c c c c c c c c c   (centrifuges, 1 x 6 x 1 each, y 0-5)
 Q Q Q (converter) =======                                 T T T (tails take-off)
 K K K (cooling skid, new, y 0-2), its cells against the header; CW in on its north face, out on its south
```
The grid is a sketch; the game test is the layout of record.

### Legend
| Char | Block id |
|---|---|
| `S` | `wildcatter:feed_station` |
| `c` | `wildcatter:centrifuge` |
| `Q` | `wildcatter:frequency_converter` |
| `P` | `wildcatter:product_takeoff` |
| `T` | `wildcatter:tails_takeoff` |
| `K` | `wildcatter:cascade_cooling_skid` |
| `A` | `wildcatter:sampling_autoclave` |

## Controller, rotation, forming, removing
- The **feed station** stays the cascade's controller (it counts the machines and meters feed); it now also holds the assays and
  counts the cooling skids on its header.
- The sampling autoclave is its own controller, not on the header.
- Any rotation. Removing the cooling skid starts the CASING HOT timer; removing the converter starts coast-down (it no longer reads
  NO DRIVE at once: the machines coast).

## Game tests
- `cascade` (extended, 16c1):
  - set to 3.00 / 0.25 %, ten machines run (TOO FEW STAGES gone) and the product 30B reads 3.00 %;
  - the converter's breaker opened: the machines coast and still make product at a falling rate;
  - the cooling skid removed: CASING HOT after 5 minutes (time sped by hook).
- `cascadeCrash` (16c2): a machine held at 45 % speed by hook for 2 minutes crashes; its neighbour may crash (the test seeds the
  20 % to happen); the crashed block breaks to scrap. A 30B through the sampling autoclave comes out SAMPLED; the fuel plant's
  autoclave refuses an unsampled one.
- **Screenshot:** a crashed machine in the hall.

## Proposed waves
- **16c1:** product assay, spin-up and coast-down, cooling.
- **16c2:** crashes, sampling and blending, heels.

## As built (16c1, 2026-11-10; decisions in `docs/questions/phase16-uranium.md`, items 17-23)
- **Assays:** a sneak-click on the feed station steps its setting round all fifteen: product 3.0, 3.5, 4.0, 4.5, 4.95 %, then tails 0.20,
  0.25, 0.30 %. Until set, 4.95 / 0.25 % (every cascade already built). `fluid/Enrichment` takes any product and tails assay; the spec's
  table is in `UraniumOperationsTest` (3.00 / 0.25 %: 5.97 kg of feed, 3.81 SWU, 10 stages).
- **Downstream:** the product take-off stamps each 30B with the assay of what went into it (a new item component, `wildcatter:assay`;
  4.95 % is left unstamped, so it stacks and reads as every 30B before). The fuel plant carries it by mass through every stage, the rod
  welder and the assembly fixture by rods, onto the assembly; the reactor keeps each assembly's assay: starting reactivity
  +16,000 pcm at 4.95 %, 4,000 pcm less a percent below. Tooltips read it.
- **Spin-up and coast-down** (the rule `cascadeSpinUp`, **off**; since 2026-11-10 a world rule, off even in new worlds, answer 23): 10 minutes from rest to full speed,
  an hour from full speed to rest with no drive, separating at its speed share; a coasting machine reads COASTING. Critical speeds and
  crashes are 16c2.
- **Cascade cooling skid** (`wildcatter:cascade_cooling_skid`, 3 x 3 x 6, 55 kW, box west): its side against the header; circulating water
  in on the north face of its front cells, 13.6 kg/s a machine (571 kW at a 10 C rise), back warm on the south face of its back cells,
  what nothing takes discharged. **A cascade that has had a skid on its header needs one cooling:** 5 minutes without (no skid, no water,
  no power), CASING HOT, feed held. A cascade that never had one runs as before.
- **Tests:** NeoForge scene `cascade_operations` (machines driven by hook: ten run 3.00 / 0.25 %, the 30B reads 3.00 %; with the rule on,
  the machines coast and the cascade feeds; water cut, CASING HOT). Unit test `UraniumOperationsTest` (the table, fresh reactivity,
  cooling water).

### After Tim's answers (2026-11-10, branch `w-rules`)
- **A placed fuel assembly keeps its assay (answer 21):** its main cell's block entity keeps the item's components; the loot table copies
  `wildcatter:assay` and the custom data (handled, bent grid, inspected) back onto the drop, and a click reads the standing assembly's
  assay. A standard 4.95 % assembly drops unstamped. NeoForge scene `assembly_assay`.

## As built (16c2, 2026-11-12, branch `w-17c`; decisions in `docs/questions/phase16-uranium.md`, items 31-35)
- **Rules:** `centrifugeCrashes` (crashes) and `uf6Cylinders` (sampled 30Bs only at the fuel plant; heels), both on in new worlds.
- **Crashes** (`CentrifugeBlockEntity.crashCheck`, `crash`; the feed station looks): critical speed 30-60 % for 2 minutes; overspeed over
  1,300 Hz for 15 minutes (nothing commands it yet); 15 minutes without cooling, then a machine a minute; a converter closing onto a
  machine coasting over 30 % (with `cascadeSpinUp`). A bang, 0.1 kg of UF6 (4 damage and poison within 3 m), a `crashed_centrifuge` in its
  place (4 steel and a `rotor_shard` when broken), 20 % for each machine within 2 blocks. The feed station counts the crashes.
- **Sampling autoclave** (`SamplingAutoclaveBlockEntity`, 3 x 3 x 5, 50 kW): SAMPLE (30 minutes, through `TimeScale`) or BLEND to a
  product assay (two full 30Bs and an empty; a 30B at the target and one at the rest, both sampled). Sneak-click for the mode.
- **Heels:** 48Y 20 kg, 30B 10 kg, on the item (`item/CylinderTags`), counted by the take-offs and the cylinder filler.
- **Tests:** NeoForge scene `cascade_crash` (its rules on); unit tests in `UraniumOperationsTest`; client scene `cascadeCrash`
  (screenshot `wildcatter-cascade-crash`).
- **Placeholders:** `rotor_shard_placeholder` (item icon).
