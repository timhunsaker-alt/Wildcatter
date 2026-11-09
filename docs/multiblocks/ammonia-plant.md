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
