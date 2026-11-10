# Phase 18, phosphate: decisions taken while Tim was away (2026-11-10)

Specs: `docs/multiblocks/fertilizer.md` (18a) and `docs/multiblocks/uranium-from-phosphate.md` (18b), both DRAFT, decided on recommendation
(`docs/questions/specs-wildcatter.md` items 52-59). Their "As built" sections list every number added. Branch `phase18`. Format as in
`docs/questions/README.md`. Items marked **(needs Tim)** change what the specs promised or wait on another branch.

## Both waves

### 1. Does anything change in an existing world?
- **Chosen:** no. Every part, item, pipe product and recipe is new. Three things existing players can see change only in text: the
  phosphate rock and potash ore tooltips list their new uses (they said "No use yet"); the bagging station's NOT BUILT readout names the
  granulation plant and the potash dryer; and the decant tower's readout adds "of which N t phosphogypsum" (or salt) once a fertilizer
  plant sends it tails. A tower fed only by the uranium mill or Tailings reads as before. No config key was needed.
- **Why:** the hard rule. The sulfuric acid plant is untouched: the reactor is one more consumer on the acid's pipe, as the leach tanks are.
- **Alternatives:** keep the ore tooltips saying "No use yet" until Tim has played it.

### 2. Merge order with Phases 16, 17 and 19
- **Chosen:** the four new pipe products (rock slurry, filter acid, merchant acid, treated acid) are appended after `BIOGAS`, as Phases 16,
  17 and 19 append theirs. Products are saved by name, never by position, so any merge order is safe; the merge is a one-line text conflict.
- **Why:** the branches were cut from the same master.
- **Alternatives:** none needed.

## 18a, fertilizer

### 3. The reactor's sulfuric acid comes in on its south side, not its east (needs Tim)
- **Chosen:** rock slurry on the north face, sulfuric acid on the south face of the back cells; the pan filter against the east face.
- **Why:** the spec puts both the acid and the filter on the east face. The filter (10 long) covers all of the reactor's 8 m east side.
- **Alternatives:** the filter against the south face and the acid on the east (the layout sketch would change).

### 4. Motor boxes on the north face where a part covers the west
- **Chosen:** the pan filter, vacuum crystallizer, potash dryer and DEPA-TOPO circuit carry their terminal box on the north face of their
  main cell. Every other part keeps it on the west face, as every plant part does.
- **Why:** each stands against the part to its west, which covers that face (the spec's layouts), so no cable could reach a west box.
- **Alternatives:** shorten the parts so the box clears; move the spec's layouts.

### 5. Potash: steam and gas on the south faces
- **Chosen:** the dissolver's steam, the crystallizer's ejector steam and the dryer's gas come in on the south face of each part's bottom
  cells. The salt tails leave the dissolver's west face (not its main cell, which has the box). The bagging station goes against the
  dryer's north side.
- **Why:** the spec's line (dissolver, crystallizer, dryer, bagging station) covers each part's east face, and a bagging station against
  the dryer's east end would cover its own west box.
- **Alternatives:** a gap between the parts with the steam piped through it (they must touch).

### 6. The mill's rating
- **Chosen:** 1.16 kg/s of rock (100 t a day): the reactor's 0.82 kg/s plus the granulator's rock on TSP. 200 kW at the medium voltage, as
  the spec gives.
- **Why:** TSP takes rock as well as acid; a mill sized only to the reactor would starve the granulator.
- **Alternatives:** a second mill for the granulator.

### 7. TSP's split between rock and acid
- **Chosen:** 70 % of TSP's P2O5 from the merchant acid, 30 % from the rock: 2.6 t/h of TSP from all the acid.
- **Why:** textbook TSP takes two-thirds to three-quarters of its P2O5 from the acid *(unverified)*.
- **Alternatives:** two-thirds (more rock, less acid a tonne).

### 8. Water to the filter
- **Chosen:** the pan filter takes water off a pump (its washes and the repulping of the gypsum): 1.5 kg a kg of gypsum, so the gypsum
  leaves at the tailings cell's 40 % solids. The decant tower returns three quarters of it to any water user on its water pipe, the filter
  included.
- **Why:** the spec gives water only to the mill, but the gypsum slurry's water has to come from somewhere; a real plant's comes back off
  its gypsum pond, which is what the decant tower does.
- **Alternatives:** no water at the filter, the gypsum's water appearing from nothing.

### 9. The rock slurry
- **Chosen:** 62 % solids, a new pipe product in kg that no stock tank holds; the mill takes 0.61 L of water a kg of rock.
- **Why:** wet grinding runs at 60-65 % solids *(proposed)*.
- **Alternatives:** dry grinding and rock by conveyor.

### 10. Acid densities and the fluorine
- **Chosen:** filter acid 1.30 kg/L (18b's figure), merchant acid 1.68 kg/L *(unverified)*; francolite at 3.7 % F, a quarter of it off
  the evaporator as SiF4, counted on its readout.
- **Why:** the spec counts the fluorine but gives no figure.
- **Alternatives:** count it at the reactor too (real reactors lose 10-30 % there).

### 11. The phosphogypsum and the salt in the cell
- **Chosen:** the filter's tails carry an assay of `wildcatter:phosphogypsum` and the dissolver's `wildcatter:salt_tails`; neither is an
  item. The tower counts them and names them. NO GYPSUM STACK (and NO SALT STACK) when the part's tails hold is full: no pipe, a full cell,
  or a breached one.
- **Why:** the spec asks for a readout that names gypsum. A key of its own keeps a Tailings tails slurry that carries halite from reading
  as potash salt.
- **Alternatives:** a phosphogypsum item and block for a dry stack (a real stack is dry-stacked or wet-ponded).

### 12. The granulation plant
- **Chosen:** everything on its east side (merchant acid, rock slurry, ammonia, gas or LPG); sneak-click switches TSP and DAP; what it
  made of each is held apart for the bags. The dryer burns 0.8 MW at the TSP rating, scaled by output (1.1 MJ a kg).
- **Why:** one face for a part fed only by pipe; the spec names only the ammonia's face.
- **Alternatives:** the rock slurry on the north end and the ammonia on the east, as the pre-neutraliser sits.

### 13. Block masses (the spec said "unverified")
- **Chosen:** the ore's own block masses: phosphate rock 2,600 kg (density 2.6), so the reactor takes about 1.1 blocks an hour; potash
  2,100 kg, so the potash plant takes 3 blocks an hour (the spec guessed 2).
- **Why:** ores.md's densities are the mod's.
- **Alternatives:** none.

### 14. Potash steam and water
- **Chosen:** 0.30 kg of steam a kg of MOP for the dissolver and 0.10 for the ejectors *(proposed)*; make-up water off a pump, what the
  salt tails carry away at 40 % solids (0.375 kg a kg of MOP).
- **Why:** the spec names the steam but gives no figure.
- **Alternatives:** a brine balance with a purge.

### 15. Farmings and `c:fertilizers` (needs Tim)
- **Chosen:** the three bags carry the item tag `c:fertilizers` (a data file, no code, nothing from Farmings needed). Wildcatter gives them
  no use on crops.
- **Why:** the spec: the bags are what Farmings' crops can use; Wildcatter is not a farming mod. A tag is an optional compat by nature.
- **Alternatives:** Farmings does not read the tag yet (checked 2026-11-10): to make it do something, Farmings needs a change in its own
  repository.

### 16. Recipes
- **Chosen:** steel, pipe and vanilla parts, as the other plants (`tools/make_assets.py`).
- **Why:** house style.
- **Alternatives:** none.

## 18b, uranium from phosphoric acid

### 17. Built without Phase 16 (needs Tim)
- **Chosen:** the pretreatment and the DEPA-TOPO circuit's first cycle (extraction and the reductive strip on iron) are built. The second
  cycle (re-extraction, the ammonium carbonate strip, AUC to the uranium calciner) is not: it needs 16a's calciner and 16d1's CO2, both
  on the `phase16` branch. Until it is built the strip holds the uranium it takes, up to 326 kg (a drum's), and then reads FULL and the
  acid goes round it. The plan for the rest is in the spec's "As built".
- **Why:** the brief: build what does not depend on Phase 16. Holding the uranium means nothing is lost when the second cycle comes.
- **Alternatives:** leave 18b unbuilt until Phase 16 merges; or, in the meantime, send the strip to the uranium mill's precipitation
  (not the real route).
- **Tim (2026-11-10):** "build it on `w-next`" (answer 22). Built on branch `w-rules` (cut from `w-next`): the ammonium carbonate strip on ammonia and liquid CO2, AUC to the uranium calciner against the circuit, U3O8 drums (`world-rules.md` 8). Scene `phosphate_auc`.

### 18. Treated acid is its own product
- **Chosen:** the circuit's acid comes out as `treated acid (28 % P2O5)`, which the evaporator takes as it takes filter acid. With no
  circuit, or one that cannot take the acid (filling, no iron, no extractant, full), the pretreatment passes the filter acid straight out
  of its own south side, with no power needed.
- **Why:** the uranium in the acid is worked out from the rock's grade, not carried with it; a second product stops the same acid being
  stripped twice. The spec says the fertilizer plant runs on without the circuit.
- **Alternatives:** carry a uranium assay with the acid (as the slurries carry theirs).

### 19. The extractant drum's recipe (needs Tim)
- **Chosen:** shapeless: an empty drum, phosphate rock, a diesel bucket and an iron ingot. A drum holds 200 kg of DEPA and TOPO.
- **Why:** the spec asks for merchant acid, LPG and iron, but neither acid nor LPG is an item. The rock stands in for the phosphorus and
  the diesel for the organic.
- **Alternatives:** a filling station that fills drums from merchant acid and LPG on pipe (one more part).

### 20. The first charge and the make-up
- **Chosen:** 5,000 L of organic: 4,000 L of kerosene by pipe and 5 extractant drums (counted at a litre a kg). FILLING until both are in.
  Make-up: 0.05 L of organic a m3 of acid, four-fifths kerosene and one-fifth extractant (a sixth drum lasts about 8,000 hours).
- **Why:** the spec's charge and loss; 0.5 M DEPA and 0.125 M TOPO is about 0.2 kg a litre.
- **Alternatives:** charge the organic as one product made elsewhere.

### 21. Iron for the strip
- **Chosen:** iron ingots by the same hopper (the circuit's bin has a slot for drums and one for ingots), 100 kg an ingot (the mod's
  figure), 0.5 kg a kg of U: an ingot lasts about 72 hours.
- **Why:** the spec.
- **Alternatives:** scrap iron.

### 22. What the scenes stand in for
- **Chosen:** in the NeoForge scenes, process steam, gas, the granulator's rock slurry and 18b's filter acid come from the test mod's
  source block; sulfuric acid and ammonia from test-filled tanks; water by hook. The client scene uses a real package boiler.
- **Why:** each is another plant already tested in its own scene; the wiring would double the scenes.
- **Alternatives:** the whole chain in each scene.
