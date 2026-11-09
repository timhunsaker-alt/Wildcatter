# The ammonia plant: ammonia, nitric acid, ammonium nitrate and emulsion (APPROVED by Tim 2026-11-09)

A small **ammonia plant on pipeline gas**, with a **nitric acid** unit (the Ostwald process), an **ammonium nitrate**
neutraliser and an **emulsion matrix** blender. It makes the bulk explosive for Tailings' open pits (Tailings
`docs/multiblocks/open-pit.md`, The blast). Ammonia also serves the purer yellowcake route Wildcatter's ROADMAP already lists
(ADU precipitation). **In Wildcatter, not Tailings (decided with Tim, 2026-11-09):** it is chemistry on Wildcatter's gas, steam
reformer, pipe and tanks, like the acid plant.

Sources: EU BREF *Large Volume Inorganic Chemicals: ammonia, acids and fertilisers* (2007), the ammonia, nitric acid and
ammonium nitrate chapters; the IME and AEISG codes on bulk emulsion explosives. A figure marked *(proposed)* is a textbook or
scaled value; *(unverified)* is from memory, to be checked against the BREF before approval.

```
pipeline gas + water -> STEAM REFORMER (conversion.md, existing) -> hydrogen 30 Nm3/h --+
air -> NITROGEN GENERATOR (PSA) -> nitrogen 10 Nm3/h -----------------------------------+-> SYNGAS COMPRESSOR -> AMMONIA CONVERTER
                                                                                                        | (loop, refrigerated separator)
                                                                                                        v  liquid ammonia 15 kg/h
                                                     +--------------- to the ammonia tank (and the ADU route) --------------+
                                                     v                                                                     v
                         air -> NITRIC ACID UNIT (Pt-Rh gauze, absorption tower) -> 60 % nitric acid -> AN NEUTRALISER <- ammonia
                                                                                                        | ammonium nitrate solution
                                                                                                        v
                                                       diesel + emulsifier -> EMULSION BLENDER -> emulsion matrix -> bulk truck fill
```

## Size: one steam reformer's hydrogen
The plant is sized to **one** of Wildcatter's existing `steam_reformer` parts (30 Nm3/h of hydrogen, conversion.md):
- **Ammonia:** N2 + 3 H2 -> 2 NH3. 30 Nm3/h of hydrogen and 10 of nitrogen make **20 Nm3/h, 15.2 kg/h, 0.36 t/day** of ammonia
  *(proposed: a very small plant; real small modular plants are 1-20 t/day)*.
- **Ammonium nitrate:** NH3 + HNO3 -> NH4NO3. The ammonia is split so the acid made from one share neutralises the other: about
  52 % to the nitric acid unit at 94 % conversion *(BREF: 93-98 %)*, 48 % to the neutraliser. **34.6 kg/h, 0.83 t/day** of ammonium
  nitrate.
- **Emulsion matrix:** about 75 % ammonium nitrate, 18 % water, 6 % diesel and 1 % emulsifier *(proposed; real emulsions 70-80 %
  AN)*. **46 kg/h, 1.1 t/day.**
- **What that blasts:** at Tailings' 0.37 kg/t, about **3,000 t of rock a day**, more than the 50 t/h plant's 1,200 t/day.

## Parts *(sizes proposed; grids come with the models)*
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `steam_reformer` | 3 x 8 x 4 | **Existing** (conversion.md): pipeline gas and water to hydrogen |
| `nitrogen_generator` | 2 x 3 x 3 | A PSA nitrogen generator on carbon molecular sieve: 99.9 % nitrogen from air, about 0.3 kWh/Nm3 *(unverified)*: 3 kW |
| `syngas_compressor` | 3 x 3 x 5 | A reciprocating compressor to **150 bar** *(proposed; real loops 100-250 bar)*, about 40 kW *(proposed)* |
| `ammonia_converter` | 3 x 10 x 3 | An iron-catalyst converter at **400-450 C**, with its loop: recycle circulator, heat exchangers and an ammonia chiller that condenses the product. About 15 % conversion a pass *(unverified)*; the rest recycles. Purge gas back to the reformer's burner |
| `nitric_acid_unit` | 4 x 12 x 4 | A burner with a **platinum-rhodium gauze at 850-900 C** (4 NH3 + 5 O2 -> 4 NO + 6 H2O), a cooler, and an absorption tower making **60 % nitric acid** (BREF: 50-68 %). The air blower is part of it. Its gauze is a **platinum item** from Wildcatter's PGM chain, lost slowly (BREF: 0.05-0.3 g a tonne of acid) |
| `an_neutraliser` | 3 x 5 x 3 | Ammonia and nitric acid to an **85 % AN solution** at about 150 C, by its own heat of reaction (BREF) |
| `emulsion_blender` | 3 x 3 x 4 | A high-shear blender making the emulsion matrix from AN solution, diesel and emulsifier, into an IBC or the bulk truck |
| tanks | Wildcatter's | Ammonia (pressurised), nitric acid (stainless) and AN solution (heated) on Wildcatter's tanks and pipe |

## Products
- **Ammonia** (fluid, m3 of liquid): to the neutraliser, the nitric acid unit and, later, ADU yellowcake.
- **Nitric acid, 60 %** (fluid): also open to other Wildcatter uses.
- **Ammonium nitrate solution** (fluid, kept hot; it sets solid below about 120 C at 85 % *(unverified)*: a cold pipe blocks).
- **Emulsion matrix** (fluid, in IBCs or the bulk truck). It is an oxidiser, **not an explosive until gassed in the hole**, as
  real emulsion is. A spill or a fire does not detonate it.

## Hazards *(proposed)*
- **Ammonia** leaks from a broken pressurised tank or pipe as a gas cloud that hurts nearby players and mobs.
- **AN solution** left hot and contaminated is not modelled to explode. Ammonium nitrate storage explosions are left out on
  purpose: the plant stores solution and matrix, not prill.

## Readouts and PLC points
Each part reads its status, as the acid plant's do (NO HYDROGEN, NO NITROGEN, NO AIR, COLD, FULL). PLC points on the converter
and the nitric acid unit: RUN, RUNNING, a 4-20 mA rate each.

## Left out
- Ammonia from coal or naphtha; the large plant's secondary reformer with process air.
- Prilled ammonium nitrate and ANFO (prill and fuel oil). Emulsion is chosen; prill can follow.
- Urea and fertilisers.
- Nitrous oxide abatement on the nitric acid tail gas (listed as a gap: real plants must abate it).

## Game tests (when built)
- `ammonia_loop`: hydrogen and nitrogen in, ammonia into a tank at about 15 kg/h.
- `nitric_acid`: ammonia and air to 60 % acid; the gauze slowly loses platinum.
- `an_emulsion`: acid and ammonia to AN solution, then emulsion matrix into an IBC.

## As built (2026-11-09; decided while Tim was away, see docs/questions/ammonia-plant.md)
Every rate follows from the reformer's 30 Nm3/h through `fluid/AmmoniaPlant` (unit test `AmmoniaPlantTest`): 15.2 kg/h of ammonia,
27.3 kg/h of HNO3 (45.4 kg/h of 60 % acid), 34.6 kg/h of AN, 46.1 kg/h of matrix, about 3,000 t of rock a day. The split is exact:
51.5 % of the ammonia to the acid unit (1 / 1.94), the rest to the neutraliser.

### Numbers added *(proposed)*
| What | Value | Real-world basis |
|---|---|---|
| Ammonia converter's load | **10 kW** at the industrial voltage *(proposed)* | Its recycle circulator and the ammonia chiller: condensing 15 kg/h takes about 6 kW of refrigeration (1.37 MJ/kg) |
| Nitric acid unit's load | **15 kW** *(proposed)* | Its air blower: about 120 Nm3/h of air to a few bar, 0.1 kWh/Nm3 |
| Emulsion blender's load | **5.5 kW** (7.5 hp) *(proposed)* | Its high-shear mixer and matrix pump at 46 kg/h |
| AN neutraliser's load | none *(proposed)* | It runs on its own heat of reaction; its pumps are left out |
| Liquid ammonia | **0.61 kg/L**, counted in litres | At about 20 C under its own pressure (8.6 bar) |
| 60 % nitric acid | **1.37 kg/L**, counted in litres | At 20 C |
| AN solution, matrix | counted in **kg** *(proposed)* | Blends whose density moves with temperature; charges are reckoned in kg |
| Process water to the absorption tower | **0.35 kg a kg of HNO3** (derived) | The 60 % acid's water less what the burning makes |
| Water boiled off in the neutraliser | **0.44 kg a kg of HNO3** (derived) | The acid's water less the 85 % solution's |
| Make-up water in the blender | **0.048 L a kg of matrix** (derived; 2.2 L/h) | 18 % water less what the solution brings |
| Gauze pack | **20 g** of 90/10 Pt-Rh *(proposed)*, losing **0.15 g a tonne of HNO3** (BREF 0.05-0.3), changed at **25 %** lost *(proposed)*: about 1,200 h | Small plants run gauze campaigns of months |
| Emulsifier pail | **20 kg** *(proposed)*: about 43 h at the plant's rate | A pail of PIBSA-type emulsifier |
| Diesel | **0.84 kg/L** | Road diesel 0.82-0.85 |
| Water held | 200 L in the acid unit and the blender, off a pump | As the reformer |

### Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **480 V boxes:** on the west face of the main cell of the nitrogen generator, syngas compressor, ammonia converter, nitric acid unit
  and emulsion blender.
- **Nitrogen generator:** nitrogen out of the south face of its back cells (z 2, bottom).
- **Syngas compressor:** hydrogen and nitrogen in on the east face of its bottom cells, on two separate runs. The converter takes its
  gas by touching it (no syngas pipe).
- **Ammonia converter:** liquid ammonia out of the south face of its bottom cells (z 2).
- **Nitric acid unit:** ammonia in on the east face of its bottom cells; 60 % acid out of the south face of its back cells (z 3, bottom).
  Gauze packs by a hopper on any top cell, or clicked in. Water from a pump.
- **AN neutraliser:** ammonia and acid in on the east face of its bottom cells; AN solution out of the south face of its bottom cells (z 2).
- **Emulsion blender:** AN solution and diesel in on the east face of its bottom cells; matrix out of the south face of its back cells
  (z 3, bottom). Emulsifier pails by a hopper on top, or clicked in. Water from a pump.
- **Ammonia storage:** the 18,000 gal pressure bullet (the LPG bullet) holds ammonia; a stock tank refuses it. Acid, AN solution and
  matrix go in stock tanks.

### Layout (the `ammonia_loop` scene's, plan view of the bottom layer)
```
            . . . . . . . B B B          B LPG bullet (x 17-19)  R steam reformer (x 13-15)
  C . . . . . . R R R p B B B          N nitrogen generator     K syngas compressor
  C . . . . . . R R R   B B B          A ammonia converter      T the ammonia bullet (x 8-10, z 15-26)
  C . . K K K . . h      ...           C the 480 V cable spine   p LPG pipe   h hydrogen   n nitrogen
  C . . K K K h N N
  C . . K K K n n N N
  C . . A A A
  C . . A A A
          a                            a ammonia pipe to the bullet
        T T T
```
The game scenes (NeoForge, `AmmoniaPlantScenes`) are the layouts of record.

### How it forms and runs
- Each part reads its status: NO HYDROGEN, NO NITROGEN, NO SYNGAS, COLD (the converter's circulator and chiller are off), NO AIR (the
  nitrogen generator's compressor or the acid unit's blower is off), NO AMMONIA, NO ACID, NO WATER, NO GAUZE, NO AN SOLUTION, NO DIESEL,
  NO EMULSIFIER, FULL.
- PLC points on the converter, the acid unit and the neutraliser: RUN, RUNNING, AUTO, RATE 4-20 mA (0 to its rating). Every part also
  has the plant units' HAND / OFF / AUTO.
- Removing a part stops what needs it; nothing is lost but what each part holds.

### Not built (Known gaps)
- The ammonia leak hazard: a broken bullet or pipe does not yet release a gas cloud.
- AN solution does not set in a cold pipe; tanks are not heated.
- No IBC: matrix goes to a stock tank on pipe, for Tailings' bulk truck to load.
- The purge gas back to the reformer's burner is not counted (pure PSA hydrogen and 99.9 % nitrogen make next to none).
- Spent gauze packs are not returned for refining; there is no PGM refinery, so a pack is crafted straight from platinum reef ore.
- Nitrous oxide abatement on the tail gas (as the spec says).
