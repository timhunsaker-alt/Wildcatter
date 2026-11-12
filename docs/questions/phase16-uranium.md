# Phase 16, uranium routes and plant operations: decisions taken while Tim was away (2026-11-10)

Plan: `docs/ideas-plan.md`, Phase 16. Built: **16a** ADU yellowcake, **16b1** conversion's cathode hydrogen, ash bed and KOH, **16c1**
product assay, spin-up and cooling, **16d1** ISR CO2, the bleed and satellites. Left as written plans: 16b2, 16c2, 16d2 (ROADMAP).
Specs: `docs/multiblocks/adu-yellowcake.md`, `conversion-extensions.md`, `enrichment-operations.md`, `isr-operations.md`; each has an
"As built" section. Format as in `docs/questions/README.md`. Entries marked **needs Tim** wait on him; the rest only need a yes or a
change.

The rule for this phase: nothing may change or slow what an existing world already does. Anything that would alter a plant already built
is behind a default-off setting, or happens only once the player adds the new part.

## 16a: ADU yellowcake

### 1. Does the drum carry its grade, or is the ADU drum its own item?
- **Chosen:** its own item, **U3O8 Drum (96 %)** (`u3o8_drum`), beside the unchanged Yellowcake Drum (magnesium diuranate, 80 %).
- **Why:** an item component would make two drums of different grades refuse to stack in a hopper and need a tooltip hook; two items
  sort and stack on their own, and every drum already in a world stays exactly what it was. There are only two grades.
- **Alternatives:** a grade component on the one drum item (what the spec's wording suggests); worth it only if more grades come
  (uranyl peroxide, 16a's "left out").

### 2. With both ammonia and magnesia at the tank, which wins?
- **Chosen:** ammonia. The two precipitates are held apart in the tank, so a batch is never mixed: the calciner takes only ADU, the dryer
  only magnesium diuranate.
- **Why:** the player who pipes ammonia to a tank wants the better product; a real mill has one reagent line, and switching is a
  decision. Holding them apart is the spec's "never mixes the two in one batch".
- **Alternatives:** magnesia first while its hopper holds any (the old route wins); a sneak-click selector on the tank.

### 3. The calciner's motor box
- **Chosen:** on the north face of its main cell, not the west as on other mill parts.
- **Why:** set as the spec draws it, the calciner's west face is against the tank: a west box could never be cabled.
- **Alternatives:** keep the west and require a gap (then it would not touch the tank).

### 4. The calciner when idle and when stopped
- **Chosen:** at temperature with nothing to do it holds its hearths hot on a fifth of its full gas; without gas, power or a tank it cools
  at a third of its warm-up rate (15 minutes to cold). Five minutes from cold, as the spec proposes.
- **Why:** real multiple-hearth furnaces are kept hot between campaigns; letting it go cold at once would make every gap in ADU cost five
  minutes. A fifth is a typical holding fire (*proposed*).
- **Alternatives:** no holding fire (it cools at once); hold at full fire.

### 5. The fluorinator's ash on U3O8 drums
- **Chosen:** the spec's own basis, "the 4 % that is not U3O8": 0.049 kg of ash a kg of U, against 0.295 for magnesia drums. That is about
  a sixth, not the "about a tenth" the spec's summary says.
- **Why:** the parenthesis states the basis; the summary was an estimate.
- **Alternatives:** a tenth (0.030 kg a kg of U), if the impurities in a real ADU product are counted as lighter fluorides.

### 6. Off-gas and by-product figures
- **Chosen:** the calciner's off-gas counted at 0.37 kg a kg of U3O8 (the ADU's own ammonia and its water of hydration, *proposed*);
  the tank's ammonium salt at 3.1 kg a kg of NH3 (ammonium chloride on the salt strip, 53.5/17). Both only counted.
- **Why:** the spec asks for them counted; nothing uses them yet.
- **Alternatives:** an off-gas scrubber part (the spec leaves out ammonia recovery).

### 7. The spec's "with a dryer against it, an 80 % drum" check
- **Chosen:** the `adu_yellowcake` scene checks NO AMMONIA and that the tank goes back to the magnesia route with magnesia in its hopper;
  the dryer's 80 % drum is left to the `uraniumMill` client scene, whose code is unchanged.
- **Why:** the dryer and the calciner stand in the same place against the tank; the scene would have to break and rebuild a part.
- **Alternatives:** a second scene with a dryer.

## 16b1: conversion's cathode hydrogen, the ash bed, KOH

### 8. How are rules that would change an existing plant made opt-in? **(needs Tim)**
- **Chosen:** a key in `config/wildcatter.properties`, the file that already holds `standard` and `wattsPerFe`, read when a server starts
  (`dev.wildcatter.Rules`). Each is **off** unless set: `fluorinatorAshBed=true` (16b1) and `cascadeSpinUp=true` (16c1). Off, those
  plants run exactly as before Phase 16.
- **Why:** Tim's rule for this phase is that nothing may change or slow what an existing world already does. The config file is how the
  mod already takes settings, and a missing key is off, so no world changes on update.
- **Alternatives:** on for new worlds and off for worlds made before (a saved flag, as the electrical standard is saved); a game rule;
  per-plant switches. **Tim:** should these rules stay off by default, or be on for new worlds?
- **Tim (2026-11-10):** per-world rules, on in new worlds and off in existing ones, changeable by command (answer 23). `fluorinatorAshBed` is on in new worlds; `cascadeSpinUp` is off even in new worlds. `Rules` now reads the world's rules (`RulesData`), not the config file.

### 9. What the ash bed changes when the rule is off
- **Chosen:** retiring works always (a sneak-click or RETIRE BED), so ash drums and their uranium are there for anyone; only the BED FULL
  stop waits on `fluorinatorAshBed`. The bed counts its ash from what it held before (a fluorinator saved with 5 t of ash reads BED FULL at
  once if the rule is turned on).
- **Why:** retiring is something the player chooses; stopping is what would change a running plant.
- **Alternatives:** start counting from zero when the rule is turned on.

### 10. The ash drums
- **Chosen:** 400 kg a drum, the last part-full; six out slots under the fluorinator (a 2 t bed is five drums); 3 % uranium (14.2 kg of
  U3O8). The leach tank uses up a drum (no empty drum back) and leaches its uranium at up to 0.05 kg/s of U3O8 while it leaches ore.
- **Why:** the spec gives 400 kg and 3 %; a drum of ash is not returned in practice (it goes with the ore). The rate keeps the ash a
  small share of the ore's uranium.
- **Alternatives:** give the empty drum back to a hopper under the leach tank; leach the ash on its own.

### 11. Where the cathode gas scrubber touches and where its box is
- **Chosen:** it takes the hydrogen touching the cell house anywhere, not only at the west face of the back cells; its 480 V box is on the
  north face of its main cell.
- **Why:** the rectifier's DC terminals and the cooling water already crowd the cell house's west side; the box on the west would face
  the cell house when the scrubber stands against its east side.
- **Alternatives:** a fixed cathode gas nozzle on the cell house, as the spec draws it.

### 12. The scene runs the cells by hook
- **Chosen:** `conversion_extensions` sets the cell house's cells to full current by a test hook (`testCells`), not by a rectifier.
- **Why:** the cells at full current take about 0.7 MW of DC, which one 500 kW diesel set cannot carry (the Field Manual says so); the
  client `conversion` scene runs them for real. The hook feeds the same path the cells' own fluorine does.
- **Alternatives:** two synchronized sets and a 4.16 kV unit substation in the server scene.

### 13. The KOH scrubber's first charge
- **Chosen:** on the pipe at the north face of its front cells, as caustic potash at 32 % counted in kg of solution (as caustic soda is);
  6,250 kg of solution make the 2,000 kg charge.
- **Why:** the spec draws it against the hydrofluorinator's east face, so its east face is taken; 32 % is the membrane cell's caustic.
- **Alternatives:** potash bags by hand; KOH counted dry.

### 14. The KOH scrubber's pumps and its regeneration rate
- **Chosen:** the scrubber scrubs only while its pumps run; the lime reactor regenerates up to 1 kg of KOH a second (*proposed*, a batch
  reactor on the recirculation tank).
- **Why:** no circulation, no scrubbing; a fast batch keeps the liquor near full strength whenever lime is there (the plant's HF needs only
  71 kg/h).
- **Alternatives:** scrub unpowered; regenerate at the plant's rate.

### 15. Synthetic fluorspar
- **Chosen:** 1 t bags (the mod's bulk bag), which the HF kiln and a fluorinator's new bed take as fluorspar.
- **Why:** the spec's "the HF kiln takes it as fluorspar"; the quicklime it uses also comes in 1 t bags.
- **Alternatives:** a synthetic fluorspar block.

### 16. Caustic potash from the small chlor-alkali cell
- **Chosen:** the zirconium plant's chlor-alkali cell takes potash (sylvite blocks) in the same hopper as rock salt; on potash its caustic
  nozzle gives caustic potash. It is not covered by a scene (the cell needs DC); its rates are the spec's.
- **Why:** the chlor-alkali plant (20a) is not built; the spec names the small 7f cell as the first source.
- **Alternatives:** a scene with a rectifier for the cell; caustic potash only from 20a.

## 16c1: product assay, spin-up and cooling

### 17. How the assays are set
- **Chosen:** a sneak-click on the feed station steps one setting round fifteen: the five product assays, then the next tails assay.
  The readout shows the stages, feed and SWU a kg at the setting.
- **Why:** one click is how other parts take a setting (the rectifier's setpoint); fifteen steps is short enough to go round.
- **Alternatives:** product on a sneak-click and tails with the Power Meter; a PLC analog setpoint.

### 18. What a 30B carries
- **Chosen:** an item component, `wildcatter:assay`, stamped only when it is not 4.95 %. The 30B's tooltip reads it. Tails 48Ys are not
  stamped (only their take-off's readout shows their assay).
- **Why:** every 30B in a world now has no component and reads 4.95 %, exactly as before. A component (not five items) because there are
  five product assays.
- **Alternatives:** a separate item for each assay; stamp the tails too.

### 19. How the fuel plant carries the assay
- **Chosen:** each stage keeps the assay of what it holds, blended by kg of U as it takes more (the loader by its rods, the welder and
  fixture by rods); the assembly comes out stamped. **Known gap:** an assembly stood up as a block keeps no assay, so a low-assay
  assembly placed and broken again reads 4.95 %.
- **Why:** the line passes uranium by mass between touching parts; a blend is what a real line would make if two assays met in it.
- **Alternatives:** refuse a 30B of another assay while the line holds uranium; keep the assay on the placed block (a block entity).
- **Tim (2026-11-10):** "it keeps its assay" (answer 21). Built: the standing assembly's block entity keeps the item's components and its drop copies the assay and the inspection record (handled, bent grid, inspected) back; a click reads its assay. Scene `assembly_assay`.

### 20. The reactor's fresh reactivity at lower assays
- **Chosen:** the spec's proposed +16,000 pcm at 4.95 %, 4,000 pcm less for each percent below (3 %: +8,200 pcm). Each core position
  keeps its assembly's assay; cores saved before read 4.95 %.
- **Why:** gives 3 % fuel about half the cycle, the spec's intent; linear is close enough over 3-5 %.
- **Alternatives:** a k-infinity curve from the assay (more accurate, harder to read).

### 21. Spin-up and coast-down are opt-in **(needs Tim, with item 8)**
- **Chosen:** behind `cascadeSpinUp=true` (default off). Off, a machine is at its converter's speed at once and stops at once, as before.
- **Why:** Tim's example of what must not change on a running cascade; on, a power cut no longer stops a cascade at once, and a new one
  takes 10 minutes to start.
- **Alternatives:** on for cascades built after this wave (a saved flag on each machine).
- **Tim (2026-11-10):** off even in new worlds (answer 23): the world rule `cascadeSpinUp`, set by `/wildcatter rule`.

### 22. Cooling is opt-in by placing a skid
- **Chosen:** a feed station remembers that a skid has served its header; from then on it needs one cooling (water and power), or after 5
  minutes CASING HOT holds its feed. The skid is found touching any header pipe. Warm water nothing takes back is discharged (counted).
  No crash yet (16c2).
- **Why:** the spec's "cascades built before this wave run without a skid until one is placed on their header": no config needed, since
  only a player who adds the skid takes on the rule.
- **Alternatives:** require cooling of every cascade under a config key.

### 23. The scene drives the machines by hook
- **Chosen:** `cascade_operations` drives the centrifuges directly every second (as the frequency converter does), with a diesel set for
  the 480 V stations and the skid.
- **Why:** a frequency converter takes 12.47 kV and about 6 MW for ten machines (the client `cascade` scene has that chain); the server
  scene tests the feed station, take-offs, skid and machines.
- **Alternatives:** the full power chain in a server scene.

## 16d1: ISR CO2, the bleed, satellites

### 24. Where the CO2 is dosed
- **Chosen:** into the injection header, on what goes to the injectors (0.5 g/L), drawn from a CO2 storage tank that touches the header
  house; each injector carries the share of the full dose it got, and a producer's grade takes the average of its pattern's injectors.
- **Why:** real plants add CO2 at the injection manifold; dosing what is held back first lagged the dose by minutes.
- **Alternatives:** dose the returned barren lixiviant; a CO2 nozzle on the house fed by pipe.

### 25. How the liquefier gets the kiln's CO2
- **Chosen:** by touching the lime kiln, which now keeps a few seconds of its CO2 for a liquefier (the rest, as before, goes up its
  stack); the liquefier takes up to 150 kg/h of the kiln's 327.
- **Why:** the spec stands it "against the lime kiln"; a CO2 pipe from the kiln would be a pipe product the kiln has never had.
- **Alternatives:** a kiln-gas nozzle on the kiln.

### 26. The bleed's nozzle
- **Chosen:** the south face of the header house's back cells, one block up; the bleed goes into the house's hold only when a pipe is
  there, so an unpiped house counts it exactly as before (its PLC level reading unchanged).
- **Why:** the spec's "north face of its back cells" looks into the house itself; the bottom row of the back is the IX lines.
- **Alternatives:** the top of the house.

### 27. The injection pump on the bleed
- **Chosen:** it takes wellfield bleed when it has no produced water, and starts on any bleed (no 2,000 L level switch).
- **Why:** 0.6 L/s of bleed would take an hour to reach the produced-water tank's switch level; real bleed lines run to the pump
  continuously.
- **Alternatives:** a bleed pond (a stock tank) ahead of the pump with the same switch.

### 28. The resin trailer
- **Chosen:** a 2 x 3 x 6 block with its own block entity (kg of U3O8 on its resin, up to 800); broken, its item carries the loading in
  an item component; set down, the block takes it back. It moves resin at up to 4 kg/s of U3O8 (*proposed*, a resin transfer pump).
  It is not a vehicle: carried in the inventory, as the fuel assembly is.
- **Why:** the spec's "a placeable 2 x 3 x 6 block when set down, carried as one item".
- **Alternatives:** a driveable trailer entity (Tailings' trucks); a trailer that keeps its own resin (loaded and eluted) rather than
  swapping it.

### 29. A full trailer at the central plant
- **Chosen:** the central columns in ELUTION take the trailer's resin into their own lead column (up to its 3,520 kg capacity) and elute
  it with their salt and acid as their own; the trailer goes back eluted.
- **Why:** the central plant's elution circuit is what real satellites truck to.
- **Alternatives:** a separate elution vessel for trailers.

### 30. The producer's grade with CO2 in the scene
- **Chosen:** `isr_operations` checks that the injector carries the full dose; the 1.3 x on the producer is checked through
  `Isr.co2Factor` in the unit test, which the producer's grade multiplies by.
- **Why:** a producer needs a roll front from world generation, which the server scene does not have; the client `isrWellfield` scene
  has one.
- **Alternatives:** extend the client scene with a CO2 tank.

There is no `docs/DESIGN.md` in Wildcatter (that file is Tailings'). The design record for each wave is its spec's "As built" section.

## 16b2: the full-size conversion plant (built 2026-11-12, branch `w-20`)

### 31. The larger main line is five new blocks of the same machines
- **Chosen:** `reduction_reactor_large`, `hydrofluorinator_large`, `fluorinator_large`, `cold_traps_large` and `distillation_columns_large`,
  each the 7d part's block entity rated four times (0.3 kg of U a second) and drawn at twice the height (3 x 20 x 3, 3 x 20 x 6,
  3 x 20 x 3, 3 x 8 x 6, 3 x 24 x 3). Everything a part takes goes with its uranium, so its hydrogen, HF, fluorine and ash are four times
  too. They join each other, and the 7d parts, by touching as the small line does; a mixed line runs at its slowest part.
- **Why:** the spec's "a second, larger size ... at twice their height"; one machine class keeps the small and large lines the same.
- **Alternatives:** wider parts rather than taller.

### 32. The large cold traps' and columns' power
- **Chosen:** four times the small ones': 120 kW of refrigeration and 80 kW of reboiler, at 480 V (*proposed*, scaled with the UF6).
- **Why:** the spec gives no rating; refrigeration and reboil duty go with the throughput.
- **Alternatives:** a medium-voltage supply for the large traps.

### 33. No large cylinder filler
- **Chosen:** the existing cylinder filler fills at the rate of the columns it stands against: against the large columns, four times as
  fast (a 48Y about every 8 hours). Against the small columns it is unchanged.
- **Why:** the spec lists five main-line parts, not the filler; a scale and a fill valve are not what limits a real plant.
- **Alternatives:** a large filler block.

### 34. Four cell houses in series
- **Chosen:** on DC bus bars: the rectifier's + to the first house's +, each house's - to the next one's +, the last - back. No cell
  house may touch the rectifier (one that touches is joined to it directly, as in 7d, which would short the string); the rectifier_mv's
  dial (default 6 kA) sets the current. Built and tested at 433 V and 6.00 kA, 2.60 MW.
- **Why:** the potline's precedent (pots in series) and the 10b1 bus bars; nothing new in the solver.
- **Alternatives:** a "series" link part between houses.

### 35. Recipes
- **Chosen:** each large part from four of its small part and four steel ingots (*proposed*).
- **Why:** none are in the spec; it keeps the large part dearer than four small ones' worth of uranium throughput.
- **Alternatives:** any.

