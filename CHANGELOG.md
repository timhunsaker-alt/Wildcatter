# Changelog

## Unreleased (0.7.0, branch `w-next`): Phases 15-19, built 2026-11-10 while Tim was away

### 16d2: monitor wells, excursions, groundwater restoration (built 2026-11-12 while Tim was away; not approved)
- **Monitor wellheads** on a header house's well pipe, 30-60 blocks from its patterns. With the world rule `isrExcursions` (on in new
  worlds) a house needs its ring (one every 40 blocks, 6 at least) to inject; under 0.5 % bleed its lixiviant front spreads to an
  EXCURSION, and after 3 game days injection stops until more bleed pulls it back.
- **Restoration:** sneak-click the header house: SWEEP, RO through a new RO unit (30 L/s), STABILITY, RESTORED.
- Decisions for review: `docs/questions/phase16-uranium.md` (41-45).

### 16c2: centrifuge crashes, the sampling autoclave, heels (built 2026-11-12 while Tim was away; not approved)
- **Crashes** (the world rule `centrifugeCrashes`, on in new worlds): never at random. A machine held at 30-60 % speed for 2 minutes, a
  casing without cooling past 15 minutes, overspeed, or a converter closing onto coasting machines: a bang, a puff of UF6 that hurts
  nearby players, a crashed centrifuge (steel and a rotor shard when broken), and a 20 % chance for each machine beside it.
- **Sampling autoclave:** signs off 30Bs (SAMPLED, n.nn %) in 30 minutes, or blends two to a chosen assay.
- **Heels and sampled 30Bs** (the world rule `uf6Cylinders`): the fuel plant takes only sampled 30Bs; emptied 48Ys keep 20 kg and 30Bs
  10 kg, counted when they are filled again.
- Decisions for review: `docs/questions/phase16-uranium.md` (36-40).

### 17c2: fumed silica, Pidgeon magnesium, chromium (built 2026-11-12 while Tim was away; not approved)
- **Fumed silica burner:** against the chlorinator, it burns the SiCl4 in hydrogen and oxygen to 25 kg sacks of fumed silica
  (`c:fumed_silica`) and 32 % hydrochloric acid on a pipe. Oxygen and hydrochloric acid are new pipe products.
- **Pidgeon magnesium:** a lime kiln fed dolomite burns dolime; the Pidgeon furnace (dolime, ferrosilicon and fluorspar, 0.5 MW of gas)
  makes 10 kg of magnesium an hour, cast into ingots at the end of each 12-hour cycle; a hopper on the magnesium cell takes them.
- **Chromium:** the chromate kiln roasts chromite with caustic soda to chromic oxide (it spills poison if broken while it holds chromate);
  the aluminothermic vessel fires it with aluminium to chromium metal, which the VAR furnace takes in place of chromite.
- Decisions for review: `docs/questions/phase17c-zirconium.md` (9-17).

### 19d: rods and the sodium sulfate (built 2026-11-12 while Tim was away; not approved)
- **Hafnium RCCAs:** 24 hafnium rodlets at the RCCA bench make a hafnium RCCA, worth 85 % of a B4C one, that never wears. The vessel
  takes any mix.
- **Worn B4C rods and the shutdown margin** (the world rule `reactorRodWear`, on in new worlds): a B4C RCCA burns its boron-10 while it
  is in at power, reads REPLACE at 30 % and loses worth past 40 %; the vessel reads the rods' shutdown margin and alarms SDM LOW under
  1,300 pcm. An RCCA taken out keeps its count.
- **Sodium sulfate:** a bagging station against the borax dissolver fills 1 t bags (`c:sodium_sulfate`); dump them in a lined cell
  through its decant tower.
- Decisions for review: `docs/questions/phase19d-rods.md`.

### 17c1: zirconium batches, scrap and hafnium (built 2026-11-12 while Tim was away; not approved)
- **Batches** (the world rule `zirconiumBatches`, on in new worlds): the Kroll reactor's two retorts alternate, 50 kg reduced for 5 hours
  and then 4 hours in the vacuum distillation; the VAR furnace melts 100 kg ingots twice, an hour a melt. The world's time scale shortens
  the waits. Existing worlds keep the steady flow.
- **Scrap** (same rule): 84 % of the metal reaches a tube; the pilger mill bins the rest as 25 kg bundles of Zircaloy scrap into a
  hopper under it, and the VAR furnace takes them back, up to 30 % of an ingot.
- **Hafnium:** HfCl4 drums (50 kg of Hf) drop under the hafnium column. Sneak-click the Kroll reactor for a hafnium campaign: the line
  follows the hafnium, and the pilger mill's fourth dies roll 3.6 kg hafnium rodlets for hafnium RCCAs.
- Decisions for review: `docs/questions/phase17c-zirconium.md`.
### 20c: lithium (built 2026-11-12 while Tim was away; not approved)
- Lithium hydroxide from spodumene by the sulfate route: a decrepitation kiln, an acid roast kiln, the lithium leach, a LiOH crystallizer
  (lithium hydroxide and sodium sulfate in 1 t bags) and a lithium carbonator; 0.5 t of LiOH.H2O an hour. A bag of lithium carbonate in a
  potline pot's hopper runs the pot 0.1 V lower. Spec: `docs/multiblocks/lithium.md`. Existing worlds are unchanged. Decisions:
  `docs/questions/phase20-chemicals.md` (27-34).

### 20b: titanium pigment (built 2026-11-12 while Tim was away; not approved)
- The mineral separator puts its ilmenite and rutile out as items to a hopper under it; the submerged arc furnace smelts ilmenite to
  titanium slag and pig iron; a titanium chlorinator, TiCl4 purification, TiCl4 oxidiser, VPSA oxygen unit and pigment finishing make 0.25 t
  of TiO2 pigment an hour on a chlorine loop. A bag crafts into 64 white dye. Spec: `docs/multiblocks/titanium-pigment.md`. Existing worlds
  are unchanged. Decisions: `docs/questions/phase20-chemicals.md` (15-26).

### 16b2: the full-size conversion plant (built 2026-11-12 while Tim was away; not approved)
- Four fluorine cell houses in series on bus bars from the 12.47 kV rectifier transformer (432 V, 6 kA), and a larger size of the
  reduction reactor, hydrofluorinator, fluorinator, cold traps and distillation columns, each 0.3 kg of U a second: a 48Y about every 8
  hours. Spec: `docs/multiblocks/conversion-extensions.md`. Existing conversion plants are unchanged. Decisions:
  `docs/questions/phase16-uranium.md` (36-40).

### 20a: the chlor-alkali plant (built 2026-11-12 while Tim was away; not approved)
- A full-size chlor-alkali plant, about a tonne of chlorine an hour: a brine saturator, the brine treatment, a membrane cell room of 52
  elements at 15 kA and 161 V (2.4 MW of DC) on a new 12.47 kV rectifier transformer, a chlorine train that dries, compresses and
  liquefies into ton containers, a caustic evaporator (50 % caustic) and an HCl synthesis unit (32 % hydrochloric acid). A brine wellhead
  solution-mines a salt dome. Spec: `docs/multiblocks/chlor-alkali-plant.md`. Existing worlds are unchanged. Decisions for review:
  `docs/questions/phase20-chemicals.md`.

### Time scale (Tim, 2026-11-10, branch `w-timescale`)
- **A world's time scale:** 1:1, 5:1, 10:1, 20:1 or 50:1. It shortens the long waits only: a real reactor heat-up and cooldown
  (`reactorRealHeatup`), the coke oven, digester and precipitator holds (`realResidenceTimes`), and dry cask cooling. Rates, fuel,
  motion and electrics stay real; the old compressed waits stay as they were. An existing world is 1:1.
- **World Rules on the Create World screen:** a button under Grid opens the time scale's five presets and every world rule, each at its
  new-world default. `/wildcatter rule timeScale <preset>` changes the scale later. A dedicated server reads
  `newWorld.timeScale` and `newWorld.<rule>` from `config/wildcatter.properties`. Other mods can add their rules to the screen.

### Tim's answers, grid and pit line (2026-11-10, branch `w-grid`)
- **Firebox:** one real heat rate, 0.36 MW, in every world: piped gas, biogas and liquid fuels last their heating value. A firebox on
  biogas or pipeline gas stays lit; one on a gas well burns far less gas for the same steam. A bucket of crude or diesel burns an hour.
- **Guy wires:** a Guy Anchor and Guy Strand. A pole whose line pulls it past its class load reads UNGUYED on the Power Meter and, if
  placed in this version, leans over time; a guy that holds the pull stands it plumb. Poles never break on their own; built poles never lean.
- **Pole transformer set by command:** in its default facing it turns its back to its pole. Built units keep their terminals.
- **Slurry Pipe (600 mm):** a rated large-bore slurry line, about 1 m3/s. **Air Header (600 mm):** blower air for flotation cells.
- **Decant tower:** a larger cell takes more slurry (20 kg a tick per 256 m2 of floor); built towers keep their rate.
- **Chlorinator:** takes zircon concentrate tagged `#wildcatter:zircon_feed` by hopper, at the kg its data file gives.
- Decisions: `docs/questions/answers-grid.md`.

### Fixes (branch `w-oom`)
- **Circuits no longer load chunks:** finding which circuit parts touch read blocks just past the edge of the loaded area, which loaded
  those chunks on the server thread on every solve. Round an area a server had just let go (a player walking away, a game test batch
  finishing) it kept reloading the chunks as fast as they unloaded, which could stall the tick and run the server out of memory.
  Circuits that are loaded solve exactly as before.

### 19c2 (fuel fabrication: HF recovery, IFBA and the real hardware; built 2026-11-12 while Tim was away; not approved)
- **HF recovery condenser** against the IDR kiln: its HF as 70 % aqueous HF (a new pipe product) for the conversion plant's HF column,
  which distils it over its kiln's sulfuric acid; the HF kiln then roasts less fluorspar.
- **IFBA coater** against the rod loader (Zircaloy tubes and boric acid); sneak-click the loader for 0, 32, 64 or 104 IFBA rods an
  assembly. In the reactor 104 IFBA rods start an assembly 3,000 pcm lower, burning out by 12 GWd/t.
- New world rule **`fuelNickelHardware`** (on in new worlds, off in existing ones): the assembly fixture takes 304 stainless and
  nickel-chromium alloy ingots for its hardware, and the rod loader the alloy for its plugs, instead of steel. Field Manual: HF RECOVERY,
  IFBA AND HARDWARE.

### 19c1 (fuel fabrication: inspection, scrap and lubricant; built 2026-11-12 while Tim was away; not approved)
- **Pellet inspection bench** between the grinder and the rod loader (1 % of pellets rejected) and **rod inspection line** between the
  welder and the fixture (X-ray, helium leak test, gamma scan; one rod in 200 rejected); rejects go out as **cans of UO2 scrap**.
- **Scrap oxidizer** against the granulator: scrap cans to U3O8, blended into the granulator's powder (up to a tenth).
- **Zinc stearate** (a zinc ingot and an animal fat, `c:animal_fats`; rotten flesh stands in) for the pellet press.
- New world rule **`fuelLineDefects`** (on in new worlds, off in existing ones): without zinc stearate the press reads NO LUBRICANT and
  a tenth of its pellets crack to scrap; the grinder's swarf goes out in cans; an assembly made without the inspection parts carries a
  leak chance (0.5 % each), rolled when it is loaded into a core. Field Manual: FUEL INSPECTION AND SCRAP.

### 17b2 (the alloy melt shop, built 2026-11-12 while Tim was away; not approved)
- **Induction furnace** (2 t, 1.5 MW at 4.16 kV or 3.3 kV, PF 0.95): melts a heat of the grade set by sneak-click (304, 316 or the
  nickel-chromium alloy) from bags of HC ferrochrome, cathode nickel, HC ferromanganese and ferromolybdenum and iron ingots by hopper.
- **AOD converter:** blows the ferrochrome's carbon to 0.05 % with oxygen from PSA oxygen generators against it and piped nitrogen.
- **Ingot caster:** twenty 100 kg ingots a heat to a hopper under it. New items: 304 and 316 stainless ingots (`c:ingots/stainless_steel`)
  and nickel-chromium alloy ingots. Field Manual: ALLOY MELT SHOP.

### 17b1 (the nickel smelter and refinery, built 2026-11-12 while Tim was away; not approved)
- **Nickel smelter:** the fluid-bed roaster takes nickel ore by hopper (or Tailings' concentrate carrying pentlandite) at 2 t/h, its SO2
  to an acid plant's converter (NO ACID PLANT otherwise); the six-in-line electric furnace on a 17a furnace transformer smelts the
  calcine at 1 MW into matte; the Peirce-Smith converter blows it to Bessemer matte (50 % Ni) in 2-hour blows, its off-gas to the acid
  plant too. A furnace transformer now feeds either furnace against it.
- **Nickel refinery:** the chlorine leach reactor (Bessemer matte by touching or in 1 t bags), the purification circuit (5,000 L of
  kerosene first; copper cast to copper ingots, cobalt counted) and the tankhouse: 18 chloride cells, 12.3 kA at 63 V DC on a rectifier,
  about 230 kg of cathode nickel an hour into 1 t bags; its anode chlorine goes back to the leach (no DC: NO CHLORINE).
- Nickel ore has a use. Field Manual: NICKEL SMELTER, PEIRCE-SMITH CONVERTER, NICKEL REFINERY. Decisions:
  `docs/questions/phase17b-nickel.md`.

### phosphate (Phase 18, built 2026-11-10 while Tim was away; not approved)
- **Fertilizer (18a):** a phosphate mill, a dihydrate phosphoric reactor with its tilting pan filter, an acid evaporator and a granulation
  plant: 20 t of P2O5 a day from phosphate rock and sulfuric acid, its phosphogypsum to a lined tailings cell, merchant acid, and TSP or
  DAP (on the ammonia plant's ammonia) in 1 t bags. A potash dissolver, vacuum crystallizer and dryer: 5 t/h of muriate of potash in
  bags. The bags carry `c:fertilizers` for another mod's crops. Spec: `docs/multiblocks/fertilizer.md`.
- **Uranium from phosphoric acid (18b):** an acid pretreatment and a DEPA-TOPO circuit take about 2.7 kg/h of uranium out of the filter
  acid into its strip; the treated acid goes on to the evaporator. The second cycle sends it on to the uranium calciner (see below). Spec:
  `docs/multiblocks/uranium-from-phosphate.md`.
- Existing worlds are unchanged. Decisions for review: `docs/questions/phase18-phosphate.md`.

### ferroalloys (Phase 17a, built 2026-11-10 while Tim was away; not approved)
- **Submerged arc furnace:** a 10 MVA furnace transformer on the distribution tier feeds an 8 MW furnace at PF 0.80, the grid's first big
  low power factor load. It smelts HC ferrochrome, HC ferromanganese or ferrosilicon 75 from ore, coke and flux by hopper, taps into 1 t
  bulk bags every 2 hours, burns green anodes as its electrode paste, and freezes after 4 hours without power.
- **Capacitor bank:** 3 Mvar on any 12.47 kV (11 kV) bus; on the furnace's bus it lifts the feeder from PF 0.80 to about 0.94.
- **Moly roaster and aluminothermic vessel:** flotation concentrate (from Tailings) to moly oxide, its SO2 to the acid plant; batches of
  oxide and aluminium fired to ferromolybdenum and ferrotungsten.
- Nothing changes in existing worlds: no solver or existing machine changed. Spec: `docs/multiblocks/ferroalloys.md`. Decisions for
  review: `docs/questions/phase17-ferroalloys.md`.

### Phase 16, uranium routes and plant operations (built 2026-11-10 while Tim was away; not approved)
- **ADU yellowcake (16a):** pipe liquid ammonia to the precipitation tank and set the new URANIUM CALCINER against it and the drum filler:
  U3O8 at 96 % in a new U3O8 Drum, 326 kg of uranium against the magnesia drum's 271. Conversion needs a third less hydrogen for it, no
  HF for magnesia and far less ash. Without ammonia the magnesia route is unchanged. Spec: `docs/multiblocks/adu-yellowcake.md`.
- **Conversion's by-products (16b1):** a CATHODE GAS SCRUBBER sends the fluorine cells' hydrogen to the reducer; the fluorinator's bed
  can be retired into Fluorinator Ash Drums whose uranium the mill's leach tank recovers (BED FULL stops a fluorinator only with
  the world rule `fluorinatorAshBed`); a KOH SCRUBBER cleans the hydrofluorinator's off-gas, charged with caustic
  potash from a chlor-alkali cell run on potash and regenerated with quicklime into Synthetic Fluorspar. Spec:
  `docs/multiblocks/conversion-extensions.md`.
- **Enrichment operations (16c1):** sneak-click the feed station to choose the product (3.0-4.95 %) and tails (0.20-0.30 %) assays; the
  30B, the fuel assembly and the reactor carry the assay (lower assay, shorter cycle). A CASCADE COOLING SKID cools the casings; a cascade
  that has had one reads CASING HOT without it. Spin-up and coast-down are opt-in (the world rule `cascadeSpinUp`, off even in new worlds). Spec:
  `docs/multiblocks/enrichment-operations.md`.
- **ISR operations (16d1):** a CO2 LIQUEFIER on a lime kiln fills a CO2 STORAGE TANK; set against a header house it doses the injection
  and raises the head grade up to 30 %. The header house's bleed can be piped to an injection pump and a disposal well. IX columns can run
  as a SATELLITE, filling a RESIN TRAILER that the central plant's columns elute. Spec: `docs/multiblocks/isr-operations.md`.
- Decisions for review: `docs/questions/phase16-uranium.md`.

### Reactor extensions (Phase 19a and 19b)

- **Reactor extensions 19a** (built 2026-11-10 while Tim was away; not approved; decisions for review: `docs/questions/phase19-reactor.md`):
  - **Containment:** containment concrete, an equipment hatch, a personnel airlock, hydrogen recombiners and igniters, and a
    containment monitor that reads the building CLOSED (and its volume) or OPEN. Closed, it takes the core's hydrogen, burns it with
    powered igniters, and blasts a compartment at 4 % without one; its pressure rises with the steam let in.
  - **The pressurizer relief tank:** the PORVs' discharge condenses in 30 m3 of water until its rupture disc bursts at 0.7 MPa.
  - **New world rules** (on in new worlds, off in existing ones; existing reactors are unchanged): `reactorLtop` (the P-T
    curve, LTOP, the vessel failing over twice the curve), `reactorHeatupLimit` (HEAT-UP RATE over 55 C/h), `reactorRealHeatup` (no 80x
    heat-up), `reactorDeltaTTrips` (the full OT and OP delta-T trips).
- **Reactor extensions 19b** (same; decisions 14-20):
  - **Dry casks:** a cask loading station against the spent fuel pool moves its coolest fuel into welded canisters and sends them to
    horizontal concrete storage modules on a pad, freeing the pool's racks.
  - **Fresh-fuel inspection:** a new-fuel inspection stand marks assemblies INSPECTED; the assembly fixture straightens a bent grid.
  - **Contamination:** a personnel monitor and a decontamination washer.
  - **New world rules:** `reactorContamination` (off even in new worlds) (coolant activity; contamination where primary water gets out, its dose,
    and its pick-up by players), `fuelHandlingDamage` (a 1 % chance of a bent grid on a move, a leaking rod if loaded unseen).

### The oilfield (Phase 15, built 2026-11-09 while Tim was away)
- **Salt domes trap oil (15a):** salt domes now stand on a seed grid in plains, swamps and beaches, each with one to three rich flank
  fields against the salt and sometimes a caprock pool that gushes then fades. A low hill marks them; the seismograph reads SALT DOME and
  its crest. Domes generated before this keep their salt and have no oil. Spec: `docs/salt-dome-oil.md`.
- **Drilling mud (15b1):** a barite mill and a bentonite dryer fill bulk bags; a mud hopper at the land rig mixes them (and rock salt)
  into the tanks, which now keep the mud's weight, gel and salt. Plain water is LOW GEL and drills at half rate; rock salt washes out
  unless the mud is salt-saturated. Barite and bentonite have a use at last. Spec: `docs/multiblocks/drilling-mud.md`.
- **Well control (15b2):** a BOP stack under the rotary table and its accumulator. Deep fields are geopressured: without a BOP the land
  rig stops short; with light mud it kicks, and must be shut in within 30 s, weighted to the kill weight and circulated; otherwise it
  blows out, drains the field and can catch fire. PLC points for the kick, the shut-in and the mud.
- Decisions for review: `docs/questions/phase15-oilfield.md`.

### Tim's answers: world rules, the oilfield's rules, real holds, casks, assays, 18b's second cycle (built 2026-11-10, branch `w-rules`)
- **World rules (answer 23):** every opt-in rule is saved with the world instead of in `config/wildcatter.properties`. A new world
  starts with them on, except `reactorContamination`, `cascadeSpinUp` and `realResidenceTimes`; a world saved before this version loads
  with all of them off. Operators list and set them with `/wildcatter rule <name> [true|false]`. The rules: `fluorinatorAshBed`,
  `cascadeSpinUp`, `reactorLtop`, `reactorHeatupLimit`, `reactorRealHeatup`, `reactorDeltaTTrips`, `reactorContamination`,
  `fuelHandlingDamage`, `saltDomeGrid192`, `mudGel`, `wellControl`, `realResidenceTimes`. `standard` and `wattsPerFe` stay in the config.
- **The oilfield's rules (answer 2):** with `saltDomeGrid192` off, salt domes are placed exactly as before Phase 15 and have no oil
  fields, so an existing world's fields do not move; with `mudGel` off, plain-water mud drills at full rate; with `wellControl` off, a
  land rig drills into deep fields and rock salt as before (no kick, NO BOP or washout).
- **Real residence times (answer 15):** with `realResidenceTimes` on (off by default), coke ovens coke each charge for 18 h, the Bayer
  digester holds its slurry an hour and the precipitators their liquor 36 h before the product moves on; the steady rate is unchanged.
- **Dry casks (answer 20):** a cask's limits count an assembly's cooling time 100x, as burnup is; fuel is eligible days, not months,
  after discharge. The heat it carries stays the real one.
- **A placed fuel assembly (answer 21):** stood up as a block and broken again, it keeps its assay and its inspection record.
- **Uranium from phosphate, the second cycle (answer 22):** the DEPA-TOPO circuit's ammonium carbonate strip, on liquid ammonia and the
  CO2 liquefier's liquid CO2, sends its uranium as AUC to the uranium calciner against it; the drum filler fills U3O8 drums.
- **Field Manual:** WORLD RULES; the ash bed, spin-up and reactor rules pages now name the world rules.
- Decisions for review: `docs/questions/world-rules.md`.
## v0.6.2 (unreleased)

### Fixes
- **No more see-through holes around small blocks:** thirteen blocks whose models are smaller than a whole block still told the game
  they filled it, so it skipped drawing the faces of the blocks beside them and the world showed through: the steam turbine, firebox,
  pumps, day tank, breakers and switches, fuses and cutouts, pole transformers, lamps and heaters, the three-phase heater, motor
  starters, the air compressor and dryer, and crane rail. They no longer hide their neighbours. Only how they draw changes; nothing in a
  world does. The NeoForge `registers` scene now checks every block's models against how it occludes.

## v0.6.1 (released 2026-11-12)

### Fixes
- **The NeoForge client starts again:** NeoForge 26.3.0.101 wants the client-side handler of a clientbound payload registered on the client
  (`RegisterClientPayloadHandlersEvent`). The PLC payload had only its two-way handler, so a NeoForge client refused to start with
  "Some clientbound payloads are missing client-side handlers: [wildcatter:plc]". Found by Farmings' first NeoForge client launch. The
  Fabric client and dedicated servers were not affected.

## v0.6.0 (released 2026-11-10)

### Fixes
- **The circuit manager no longer loads chunks:** working out which circuit parts touch read blocks next to a part even in unloaded
  chunks, loading them on the server thread every few ticks; a circuit at the edge of the loaded area kept its neighbours loaded and
  could slow the server or run it out of memory. It now stops at the loaded area's edge; solved numbers are unchanged.

### Gas and power quick wins (Phase 14b-14d), the PLC gap wave and the protection gap wave
- **The sales line (14b):** a gas plant's pipeline gas goes on an off-map transmission line at a receipt meter station and comes off
  anywhere in the dimension at a delivery station, less 2 % kept as compressor fuel. Five true-size parts: meter building (2 kW,
  residential), filter separator, ultrasonic meter run, regulator skid, pipeline tap. Off-spec gas refused (rich, wet, biogas); 8 hours on
  the RTU battery, then METER FAILED; a run ticket in m3, GJ and MMBtu; PLC points. Spec: `docs/multiblocks/sales-gas.md`.
- **The acid plant's steam (14c):** an acid superheater against the converter sends the waste heat boiler's steam to the existing 5 MW
  turbine, about 1.6 MW; the feed pump returns the condensate; the plant can carry its own blower; LOW STEAM TEMPERATURE trip. Spec:
  `docs/multiblocks/acid-plant-steam.md`.
- **Biogas (14d):** a new pipe product, `biogas, 60 % CH4`, for another mod to offer on Wildcatter's steel pipe through `FluidNode`. Gas
  sets burn it derated to 850 kW; fireboxes burn it; big burners and the sales line refuse it. Spec: `docs/biogas.md`.
- **Solver:** a generator or genset closing onto a live bus takes the bus's angle to the nearest 30 degrees, so machines on either side of
  a delta-wye transformer run in parallel without circulating power.
- Decisions for review: `docs/questions/phase14-gas-power.md`.
- **The PLC gap wave** (decisions for review: `docs/questions/plc-gaps.md`):
  - **Limits:**
    - A control cable is 50-pair: 98 signal cores, a discrete signal a core, a 4-20 mA loop or pulse a pair. A landing on a full cable is
      refused; lay a second cable beside it or use a remote drop.
    - The 24 V supply's 10 A is enforced. Over it, it hiccups (off for 1 s, then on), and its PLCs restart warm. Horns, lamps and
      solenoids draw from it.
    - A remote rack has one owner. A device output wired to two PLCs ORs their contacts; a 4-20 mA loop driven by two fails.
  - **Drawn:** the modules seated are drawn in the cabinet's and the drop's door, with the CPU's RUN lamp and the memory card.
  - **Ladder:** rung labels, jumps and RETURN. A ladder or FBD can be a FUNCTION. FBD wires go around blocks.
  - **Switchgear:** TRIP 50, TRIP 51, TRIP 87B and TRIP 50BF inputs.
  - **Fixed:**
    - a lamp switched off by its relay stayed lit;
    - a firebox held back by its firing rate read BURNING;
    - the rig in AUTO asked for a console click.
- **The protection gap wave** (decisions for review: `docs/questions/protection-gaps.md`):
  - Feeder relays (switchgear, breaker relay cabinet, 15 kV feeder breakers) have settable pickup, curve (MI, VI, EI), time dial and
    50, set from the relay with the Power Meter.
  - The breaker relay cabinet recloses (79): up to three shots, then lockout.
  - Switchgear racking takes 15 s a position and can be done remotely from a PLC; breaker failure (50BF) on the switchgear and the
    13.8 kV feeders; a bus differential (87B) on the 4.16 kV lineup, faulted with the grounding set on a section's bus.
  - 87T on the GSUs and the 10 MVA power transformer, 87G on the big generators, each with a lockout reset by hand; the generator bus
    relay panel transfer-trips a GSU's line-side breaker.
  - Grounding clamps carry an I²t rating and burn off on a fault that lasts; the set also goes on a big generator's terminals.
  - A shock trips a GFCI or RCD: the body is a 1,000 ohm path to earth.
  - ATS: factory timers (30 min retransfer, 5 min cooldown) or the short set; open, in-phase or closed transition.
  - Class J and E-rated fuses limit fault current and read out their let-through; only the fastest device clears a fault; breakers
    and fuses read the device upstream and how far they coordinate.
  - The load centre's and panelboard's main handle shows ON, OFF or TRIPPED.
  - Overhead spans and leads heat on their conductor's rating and burn down; 4.16 kV conductor spools; leads from a line insulator to
    a cutout or pole transformer; a panel with a ground rod is the service, its neutral bonded and earthed; an under-rated insulator's
    arc shatters it; a pole transformer or cutout comes down with its pole.
- **The plant and crane gap wave** (decisions for review: `docs/questions/plant-crane-gaps.md`):
  - **Cranes:** wind stops them (rain stops a crawler on a long boom, a storm stops all three); a crawler needs gravel, rock or concrete
    under its tracks and the ring crane rock, concrete or rail (SOFT GROUND); a lift whose jib or boom would strike a block or another
    tower crane's mast waits (BLOCKED). New block **Crane Rail**: the ring crane travels along a row of it to bring a site within reach.
  - **Plants:** the cracker's quench tower holds 2,000 L of pyrolysis gasoline and of fuel oil, then stops until they are piped away; the
    gas plant's stabilizer fires its reboiler on piped gas or its own LPG; gas engine sets and gas turbines burn coke oven gas and coker
    gas; each pot's auxiliaries take 3 kW at the industrial voltage on its front, and a pot can be cut out of a live line (sneak-right-click);
    a hopper on any cell of the steam plant's burner feeds it.
- **The misc gaps wave** (decisions for review: `docs/questions/misc-gaps.md`):
  - A formed steam plant draws its feedwater line from the feed pump to the boiler, and its main steam line runs flange to flange from
    boiler to superheater to turbine; a formed land rig draws its cable trays from the drive house to the mud pumps and the drawworks.
  - The 1 MW gas set's alternator is drawn at its real 1.6 m (the 500 kW set's stays 1 m); built sets redraw on their own.
  - A gas set on coke oven gas is held to 864 kW; the aeroderivative refuses coke oven gas (its Wobbe index is 40 % off pipeline
    gas's) and still takes coker gas.
  - The brass texture no longer shows a tile line; the conductor spool icons are new art.

### The ammonia plant and the waves before it
- **The ammonia plant (Phase 14):** a nitrogen generator, syngas compressor and ammonia converter on the steam reformer's hydrogen
  (15.2 kg/h of liquid ammonia), a nitric acid unit on a platinum-rhodium gauze (60 % acid), an ammonium nitrate neutraliser and an
  emulsion blender making bulk emulsion matrix (46 kg/h) for open-pit blasting. New pipe products: nitrogen, ammonia, nitric acid,
  AN solution, emulsion matrix. New items: gauze pack, emulsifier pail. The LPG bullet also stores ammonia.
  Spec: `docs/multiblocks/ammonia-plant.md`; decisions for review: `docs/questions/ammonia-plant.md`.

Earlier releases (v0.2.0 to v0.5.1) are described phase by phase in `PROGRESS.md`.
