# The ammonia plant: decisions taken while Tim was away (2026-11-09)

Spec: `docs/multiblocks/ammonia-plant.md` (approved 2026-11-09; its "As built" section lists every number added). Format as in
`docs/questions/README.md`.

### 1. How do the parts pass their products: on pipe or by touching?
- **Chosen:** on steel pipe everywhere (new pipe products: nitrogen, ammonia, nitric acid, AN solution, emulsion matrix), except the
  syngas compressor and the converter, which work by touching (the converter takes the compressor's gas).
- **Why:** the spec draws the plant as separate units joined by pipe and tanks, like the acid plant's. The compressor discharges
  straight into the synthesis loop in a real plant, so it is one machine with the converter.
- **Alternatives:** a SYNGAS pipe product between them (one more product, and nothing else would use it).

### 2. Units: what is counted in litres and what in kg?
- **Chosen:** ammonia and nitric acid in litres (0.61 and 1.37 kg/L); AN solution and emulsion matrix in kg.
- **Why:** the spec counts ammonia as liquid volume, and the mod counts acids in litres (sulfuric acid). AN solution and matrix are hot
  blends whose density moves with temperature, and blast designs reckon charges in kg (Tailings' 0.37 kg/t), as caustic soda and pitch
  are counted in kg already.
- **Alternatives:** everything in litres (needs densities for 85 % AN at 150 C and for matrix, about 1.35-1.4, both unverified).

### 3. Where is the ammonia stored?
- **Chosen:** in the 18,000 gal pressure bullet (the LPG bullet, renamed "Pressure Bullet (18,000 gal; LPG or ammonia)"; its readout
  says "Ammonia bullet" when it holds ammonia). Stock tanks refuse ammonia.
- **Why:** real small ammonia storage is the same 250 psi bullet as propane (ammonia's vapour pressure is 8.6 bar at 20 C).
- **Alternatives:** a new refrigerated ammonia tank (real at large plants; needless at 0.36 t/day).

### 4. Power of the parts the spec gives none for
- **Chosen:** converter loop 10 kW, nitric acid unit 15 kW, emulsion blender 5.5 kW, all at the world's industrial voltage; the
  neutraliser unpowered.
- **Why:** the converter's load is its circulator and the chiller (condensing 15 kg/h of ammonia is about 6 kW of refrigeration). The acid
  unit's blower moves about 120 Nm3/h of air at about 0.1 kWh/Nm3. The blender is a 7.5 hp mixer and pump. A real neutraliser runs on
  its heat of reaction and its pumps are small. (Spec values used as given: nitrogen generator 3 kW, compressor 40 kW.)
- **Alternatives:** larger figures with a tail-gas expander on the acid unit (real medium-pressure plants recover power that way).

### 5. How the ammonia is split between the acid unit and the neutraliser
- **Chosen:** the acid unit has a nameplate of 51.5 % of the plant's ammonia (7.83 kg/h); the neutraliser takes ammonia in proportion to
  the acid it gets. One bullet feeds both.
- **Why:** x x 0.94 = 1 - x gives the spec's 34.6 kg/h of AN exactly ("about 52 %" in the spec). Real neutralisers dose ammonia to the
  acid on pH.
- **Alternatives:** a set split valve on the ammonia line.

### 6. Water to the absorption tower and the blender
- **Chosen:** both take water from a pump (200 L each): 0.35 kg a kg of HNO3 in the tower, 0.048 L a kg of matrix in the blender. The
  neutraliser boils off 0.44 kg a kg of HNO3 as steam (counted).
- **Why:** the mass balance: 60 % acid needs more water than the burning makes, and 18 % water in matrix is more than the 85 % solution
  brings (BREF; real towers take process water at the top).
- **Alternatives:** make 64 % acid with no process water; blend at 85 % solution only (matrix 15 % water).

### 7. The gauze: what it is and how it wears
- **Chosen:** a Platinum-Rhodium Gauze Pack item (20 g of 90/10 Pt-Rh), loaded by a hopper on top or by hand; it loses 0.15 g a tonne of
  HNO3 and is changed when it has lost 25 % (about 1,200 h of running). It is crafted from one Platinum Reef Ore; the worn pack is not
  returned.
- **Why:** the spec asks for a platinum item from a PGM chain, but Wildcatter has only the platinum reef ore (about 195 g of PGM a block)
  and no refinery. 0.15 g/t is the middle of the BREF's 0.05-0.3 g/t. Small plants change gauzes every few months.
- **Alternatives:** a PGM refinery (a new plant, listed in Known gaps); a recipe needing more ore; returning worn packs for recovery.

### 8. The emulsifier
- **Chosen:** an Emulsifier Pail item (20 kg), four from a plastic sheet, a diesel bucket and an iron ingot, fed by a hopper on the
  blender or by hand. A pail lasts about 43 h.
- **Why:** real matrix uses a PIBSA (polyisobutylene succinic anhydride) emulsifier at about 1 %, shipped in pails or drums. The plastic
  sheet stands in for the polymer and the diesel for its carrier oil.
- **Alternatives:** an emulsifier made in a plant from the cracker's C4s; leaving the emulsifier out (matrix would not hold).

### 9. Where the matrix goes (the spec says "into an IBC or the bulk truck")
- **Chosen:** on pipe out of the blender into a stock tank, from which Tailings' emulsion truck (or its own pipe) can load
  `Product.EMULSION_MATRIX` in kg. No IBC item.
- **Why:** a bulk truck loads from a storage tank on a real site; a pipe product is the cleanest hand-over for Tailings. Boosters and
  detonators stay in Tailings (made from vanilla TNT, Tim's decision).
- **Alternatives:** an IBC item filled at the blender (1,000 L, about 1,400 kg of matrix), as the cylinder filler fills 48Y cylinders.

### 10. The converter's COLD and its start-up
- **Chosen:** COLD means its circulator and chiller are off (no power or held); there is no warm-up time and no start-up heater, and
  the 15 % a pass is shown on the readout, not modelled (the loop recycles until all the syngas is ammonia).
- **Why:** the conversion plant left its start-up heaters out for the same reason; the reaction keeps the bed hot once running.
- **Alternatives:** a start-up heater and a bed temperature that must reach 400 C before it converts.

### 11. The purge gas
- **Chosen:** not counted: all the syngas becomes ammonia.
- **Why:** PSA hydrogen and 99.9 % nitrogen carry almost no inerts, so the purge is tiny; the spec's 15.2 kg/h is full conversion.
- **Alternatives:** a 1-2 % purge to the reformer's burner, cutting the ammonia slightly.

### 12. The ammonia leak hazard and hot AN solution (spec "Hazards (proposed)")
- **Chosen:** not built in this wave; listed in Known gaps.
- **Why:** the spec marks them proposed; a gas cloud needs a pipe-break hook the mod does not have yet.
- **Alternatives:** a poison cloud when a bullet holding ammonia is broken (small); a cold-pipe blockage for AN solution.

### 13. PLC points
- **Chosen:** RUN, RUNNING, AUTO and RATE 4-20 mA (0 to the rating) on the converter, the nitric acid unit and also the neutraliser.
- **Why:** the spec names the converter and the acid unit; the neutraliser's rate is the plant's AN output, worth trending too.
- **Alternatives:** only the two the spec names.

### 14. NeoForge game scenes
- **Chosen:** three scenes as the spec names them (`ammonia_loop`, `nitric_acid`, `an_emulsion`), each with its own diesel set, stand-in
  tanks filled by the test, and checks at the spec's rates within 5-10 %, then stopping without feed or power. Each scene force-loads its
  chunks (the framework forces only the one-block test structure's).
- **Why:** each stage proved on its own, in under a minute of game time each. The whole plant chained in one place is the filmed
  client scene `ammoniaPlant` (its ammonia bullet starts with 2,000 L, as if the loop had run a few days).
- **Alternatives:** one scene of the whole plant (slower to reach steady state, harder to read a failure).
