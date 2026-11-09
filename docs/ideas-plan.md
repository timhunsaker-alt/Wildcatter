# Plan for the unscheduled Ideas (Phase 14 onward)

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Every choice is in
> `docs/questions/specs-wildcatter.md`. Nothing here is CURRENT: ROADMAP.md stays the authority, and a phase starts only when Tim
> picks it.

Every item in ROADMAP.md's "Ideas (not scheduled)" now has a spec, except the ammonia plant (approved 2026-11-09 and being built for
Tailings' wave P3). This file orders them by **value to the game against size**, groups them into proposed phases and waves, and lists
what each needs first.

## How they were ranked
- **Value** (1-5): how much it serves VISION's pillar 1 (fuel, generation, grid), gives an unused thing a use, or closes a known gap in
  PROGRESS.md; and how much new play it makes (decisions, consequences) rather than one more line of parts.
- **Size:** S (no or one new part, a few days), M (2-5 parts or one new mechanic), L (6-9 parts), XL (more, or a new system across the mod).
- **Order:** high value per size first, then dependencies.

## The specs
| # | Spec | Idea it covers | New parts | Size | Value | Proposed wave |
|---|---|---|---|---|---|---|
| 1 | [multiblocks/sales-gas.md](multiblocks/sales-gas.md) | sales gas pipeline and meter station | 5 | S-M | 5: residue gas gets a buyer; gas moves between sites; closes a known gap | **14b** (built) |
| 2 | [multiblocks/acid-plant-steam.md](multiblocks/acid-plant-steam.md) | acid plant waste-heat steam to a turbine | 1 | S | 4: the acid plant exports about 1.3 MW and can island itself | **14c** (built) |
| 3 | [biogas.md](biogas.md) | biogas as an engine fuel | 0 | S | 3: Farmings' digester joins the grid | **14d** (built) |
| 4 | [mekanism-check.md](mekanism-check.md) | check plugs against real Mekanism | 0 | S | 4 when possible: the "power base" claim; **blocked** (Mekanism for 26.3 not released, checked 2026-11-09) | **14e** (when released) |
| 5 | [salt-dome-oil.md](salt-dome-oil.md) | salt domes trap oil on their flanks | worldgen | M | 4: richer early oil, a reason to survey domes | **15a** |
| 6 | [multiblocks/drilling-mud.md](multiblocks/drilling-mud.md) | drilling mud from barite and bentonite | 5 | M (15b1), M (15b2) | 4: two dead ores get a use; kicks, the BOP and blowouts; closes a known gap | **15b1, 15b2** |
| 7 | [multiblocks/adu-yellowcake.md](multiblocks/adu-yellowcake.md) | the purer yellowcake route | 1 | S | 3: cleaner conversion, uses the ammonia plant | **16a** |
| 8 | [multiblocks/conversion-extensions.md](multiblocks/conversion-extensions.md) | conversion: cathode H2, ash bed, KOH, full size | 3 (+6) | M (16b1), L (16b2) | 3 | **16b1, 16b2** |
| 9 | [multiblocks/enrichment-operations.md](multiblocks/enrichment-operations.md) | enrichment: spin-up, crashes, cooling, sampling, heels, assay | 2 | M | 3: product assay is a real choice; crashes are a consequence | **16c1, 16c2** |
| 10 | [multiblocks/isr-operations.md](multiblocks/isr-operations.md) | ISR: CO2, monitor wells, restoration, disposal, satellites | 5 | M-L | 3 | **16d1, 16d2** |
| 11 | [multiblocks/ferroalloys.md](multiblocks/ferroalloys.md) | ferroalloys (Cr, Mn, Mo, W) | 5 | L | 4: a 10 MVA low-PF grid load, capacitor banks, uses for coke and four ores | **17a** |
| 12 | [multiblocks/nickel-smelter.md](multiblocks/nickel-smelter.md) | a nickel smelter (and real stainless and nickel alloy) | 9 | L-XL | 3: nickel ore, real fuel hardware | **17b1, 17b2** |
| 13 | [multiblocks/zirconium-extensions.md](multiblocks/zirconium-extensions.md) | zirconium: SiCl4, caustic, hafnium, Pidgeon Mg, chromium, scrap, batches | 3 | M | 2 | **17c1, 17c2** |
| 14 | [multiblocks/fertilizer.md](multiblocks/fertilizer.md) | fertilizer from phosphate and potash | 8 | L | 3: two dead ores; Farmings' fertilizer | **18a** |
| 15 | [multiblocks/uranium-from-phosphate.md](multiblocks/uranium-from-phosphate.md) | uranium from phosphoric acid | 2 | M | 2: a slow by-product uranium source | **18b** |
| 16 | [multiblocks/reactor-extensions.md](multiblocks/reactor-extensions.md) | reactor: containment, LTOP, 55 C/h, OT/OPΔT, relief tank, casks, contamination, fresh fuel, rods, sodium sulfate | 11 | L (19a), L (19b), M (19d) | 4: end-game consequences; closes 8d's and 8e's known gaps | **19a, 19b, 19d** |
| 17 | [multiblocks/fuel-fabrication-extensions.md](multiblocks/fuel-fabrication-extensions.md) | fuel: HF back, lubricant, real hardware, inspection, IFBA, scrap | 6 | M-L | 2 | **19c1, 19c2** |
| 18 | [multiblocks/chlor-alkali-plant.md](multiblocks/chlor-alkali-plant.md) | chlor-alkali from rock salt (full size) | 8 | L | 2: a 7f cell already does it small | **20a** |
| 19 | [multiblocks/titanium-pigment.md](multiblocks/titanium-pigment.md) | titanium pigment from ilmenite and rutile | 5 | M-L | 2 | **20b** |
| 20 | [multiblocks/lithium.md](multiblocks/lithium.md) | lithium from spodumene | 4 | M | 2 now; 4 if grid batteries follow | **20c** |

## Proposed phases
### Phase 14: gas and power quick wins
Small, high value, few dependencies. Phase 14 is already the ammonia plant (**14a**, built), so these are renumbered: **14b** sales line; **14c** acid plant turbine; **14d** biogas (built with 14b); **14e**
the Mekanism check, whenever Mekanism for 26.3 ships (it can drop into any phase).

### Phase 15: the oilfield
**15a** salt domes; **15b1** the mud plant and weighted mud; **15b2** kicks, the BOP and blowouts. Back to pillar 1's first era with
things a player meets early.

### Phase 16: uranium routes and plant operations
**16a** ADU (once Tailings' P3 has built the ammonia plant); **16b1** conversion's cathode hydrogen, ash bed and KOH; **16c1** product
assay, spin-up and cooling; **16d1** ISR CO2, bleed and satellites. Then **16b2** (full-size conversion), **16c2** (crashes, sampling,
heels), **16d2** (monitor rings, restoration), which can wait.

### Phase 17: metals
**17a** ferroalloys (the furnace transformer, the SAF, the capacitor bank, the moly roaster, the aluminothermic vessel); **17b1** the
nickel smelter and refinery; **17b2** the alloy melt shop; **17c1** zirconium batches, scrap and hafnium; **17c2** Pidgeon magnesium,
chromium and fumed silica.

### Phase 18: phosphate
**18a** fertilizer (phosphoric acid, TSP and DAP, potash); **18b** uranium from phosphoric acid.

### Phase 19: reactor and fuel
**19a** containment, PARs and igniters, LTOP, the heat-up limit, the relief tank, OTΔT and OPΔT; **19b** dry casks, contamination,
fresh-fuel inspection; **19c1** fuel inspection, scrap and lubricant; **19c2** HF recovery, IFBA and real hardware; **19d** hafnium rods,
helium in B4C, sodium sulfate bags.

*Alternative order (questions file, item 97):* 19a and 19b are high value and depend on nothing in 16-18; they could move up to follow
Phase 15 if Tim wants end-game depth before the metals.

### Phase 20: chemicals
**20a** the chlor-alkali plant; **20b** titanium pigment; **20c** lithium.

## Dependencies
```
ammonia plant (approved, Tailings P3) --> 16a ADU --> 18b uranium from phosphate
                                     \--> 18a DAP (TSP needs none)            ^
18a fertilizer (phosphoric acid) ---------------------------------------------+
17a furnace transformer, SAF, aluminothermic vessel --> 17b1 nickel smelter --> 17b2 melt shop --> 19c2 real hardware
                     |                          \--> 17c2 Pidgeon (FeSi), chromium metal
                     \--> 20b Ti slag
17c1 hafnium --> 19d hafnium rods
19c1 inspection (leaking rods) --> 19b contamination (coolant activity)       (either order works; 19b reads leaks if present)
14b sales line <-- biogas refused there (14d)                                 (independent)
15a salt domes --> 15b1 salt washout; 20a brine wells
16d1 CO2 liquefier --> 18b ammonium carbonate; 20a brine carbonate
20a chlor-alkali (chlorine, caustic, rectifier_mv) --> 20b titanium, 20c lithium, 16b1 first KOH (or the small 7f cell), 16b2 rectifier_mv
Tailings concentrate (assayed slurry) --> 17a molybdenum and tungsten; optional for 17b1 nickel
```

## What each phase adds to the electrical grid
| Wave | New loads or sources | Voltage tier (from the world's standard) |
|---|---|---|
| 14b | 2 kW a meter station | residential |
| 14c | **+1.6 MW** generation | generator terminals, through the 13.8/4.16 kV unit auxiliary transformer |
| 14d | gas sets derated to 850 kW | industrial LV |
| 15b | mills 150 kW and 75 kW, mixing hopper 37 kW, accumulator 15 kW | industrial LV |
| 17a | **SAF 8 MW at PF 0.80**; a **3 Mvar** capacitor bank | distribution |
| 17b | furnace 1 MW; converter blower 250 kW; **0.78 MW DC** tankhouse; induction furnace 1.5 MW | distribution, industrial MV, DC |
| 18a | about 1 MW of mills, filter vacuum pump and agitators | industrial MV and LV |
| 19a | igniters | residential (vital service) |
| 20a | **2.4 MW DC** cell room on `rectifier_mv` | distribution, DC |

## New Ideas these specs raised (for ROADMAP, if Tim agrees)
A grid battery storage system; a zinc refinery (indium and cadmium, so Ag-In-Cd rods); a rare-earth plant on monazite (gadolinia); a
niobium ore (Alloy 718); a larger ammonia train; a small biogas set; titanium metal; containment spray; gas storage in salt caverns.
(questions file, item 100.)

## Testing
Per the house rule, each wave runs only its own scenes plus screenshots; the full client suite runs at the end of each phase.
